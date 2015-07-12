/*
 * Copyright 2011 Witoslaw Koczewsi <wi@koczewski.de>
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero
 * General Public License as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see <http://www.gnu.org/licenses/>.
 */
package ilarkesto.core.persistance;

import ilarkesto.core.base.Args;
import ilarkesto.core.base.RuntimeTracker;
import ilarkesto.core.base.Str;
import ilarkesto.core.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class Transaction extends ATransaction<AEntity> {

	private static final Log log = Log.get(Transaction.class);
	public static EntitiesBackend<AEntity, Transaction> backend;

	private String name;
	private boolean autoCommit;
	private boolean ignoreModifications;
	private boolean ensureIntegrityOnCommit;
	private boolean ensuringIntegrity;
	private LinkedList<Runnable> runnablesAfterCommit;

	private EntitiesCache<AEntity> modified = new EntitiesCache<AEntity>();
	private Map<String, Map<String, String>> modifiedPropertiesByEntityId = new HashMap<String, Map<String, String>>();
	private Set<String> deleted = new HashSet<String>();

	public Transaction(String name, boolean autoCommit, boolean ensureIntegrityOnCommit) {
		super();
		this.name = name;
		this.autoCommit = autoCommit;
		this.ensureIntegrityOnCommit = ensureIntegrityOnCommit;
	}

	@Override
	protected EntitiesBackend getBackend() {
		return backend;
	}

	@Override
	public void commit() {
		// if (autoCommit) throw new IllegalStateException("Transaction is autoCommit");
		if (!isEmpty()) {
			log.info("commit()", toString());
			if (ensureIntegrityOnCommit) ensureIntegrityUntilUnchanged();
			backend.update(modified.getAll(), deleted, modifiedPropertiesByEntityId, new CommitCallback());
		} else {
			new CommitCallback().run();
		}
		backend.onTransactionFinished(this);
		modified = null;
		deleted = null;
	}

	private void ensureIntegrityUntilUnchanged() {
		String changeHash = createChangeHash();
		ensureIntegrity();
		if (changeHash.equals(createChangeHash())) return;
		ensureIntegrityUntilUnchanged();
	}

	private void ensureIntegrity() {
		ensuringIntegrity = true;
		try {
			for (AEntity entity : new ArrayList<AEntity>(modified.getAll())) {
				entity.ensureIntegrity();
			}
			for (String id : deleted) {
				AEntity deletedEntity;
				try {
					deletedEntity = backend.getById(id);
				} catch (EntityDoesNotExistException ex) {
					continue;
				}

				Set<AEntity> referencedEntities = deletedEntity.getReferencedEntities();
				log.debug("Ensuring integrity for referenced entities of deleted entity:",
					Persistence.toStringWithTypeAndId(deletedEntity), referencedEntities);

				for (AEntity referencedEntity : referencedEntities) {
					if (referencedEntity == null) continue;
					referencedEntity.ensureIntegrity();
				}
			}
		} catch (EntityDeletedWhileEnsureIntegrity ex) {
			// redo (changeHash changed)
		} finally {
			ensuringIntegrity = false;
		}
	}

	private String createChangeHash() {
		StringBuilder sb = new StringBuilder();
		for (AEntity entity : modified.getAll()) {
			sb.append("/").append(entity.getId());
		}
		for (String id : deleted) {
			sb.append("/").append(id);
		}
		return sb.toString();
	}

	@Override
	public void rollback() {
		log.info("rollback()", toString());
		backend.onTransactionFinished(this);
		modified = null;
		deleted = null;
	}

	public void persist(AEntity entity) {
		log.info("persist", toString(entity));
		if (autoCommit) {
			backend.update(Arrays.asList(entity), null, updatePropertiesMap(null, entity), new CommitCallback());
			return;
		}
		if (deleted.contains(entity))
			throw new IllegalStateException("Entity already deleted: " + Persistence.getTypeAndId(entity));
		modified.add(entity);
		updatePropertiesMap(modifiedPropertiesByEntityId, entity);
	}

	public void modified(AEntity entity, String field, String value) {
		if (ignoreModifications) return;
		if (!containsWithId(entity.getId())) return;
		log.info(name, "modified", toString(entity), field, value);
		if (autoCommit) {
			backend.update(Arrays.asList(entity), null, updatePropertiesMap(null, entity, field, value),
				new CommitCallback());
			return;
		}
		modified.add(entity);
		backend.onEntityModified();
		updatePropertiesMap(modifiedPropertiesByEntityId, entity, field, value);
	}

	private String toString(AEntity entity) {
		if (entity == null) return "<null>";
		return Str.getSimpleName(entity.getClass()) + ":" + entity.getId();
	}

	void delete(String entityId) {
		if (autoCommit) {
			backend.update(null, Arrays.asList(entityId), null, new CommitCallback());
			return;
		}
		if (deleted.contains(entityId)) return;
		deleted.add(entityId);
		modified.remove(entityId);
		backend.onEntityModified();
		if (ensuringIntegrity) throw new EntityDeletedWhileEnsureIntegrity();
	}

	@Override
	public boolean containsWithId(String id) {
		if (deleted.contains(id)) return false;
		return modified.containsWithId(id) || backend.containsWithId(id);
	}

	@Override
	public AEntity getById(String id) {
		Args.assertNotNull(id, "id");
		if (deleted.contains(id)) throw new EntityDoesNotExistException(id);
		if (modified.containsWithId(id)) return modified.getById(id);
		return backend.getById(id);
	}

	@Override
	public AEntity findFirst(AEntityQuery query) {
		AEntity entity = modified.findFirst(query);
		if (entity == null) entity = backend.findFirst(query);
		if (entity != null && deleted.contains(entity.getId())) return null;
		return entity;
	}

	@Override
	public <C extends Collection<AEntity>> C findAll(AEntityQuery<AEntity> query, C resultCollection) {
		RuntimeTracker rt = new RuntimeTracker();

		backend.findAll(query, resultCollection);
		modified.findAll(query, resultCollection);

		Iterator<AEntity> iterator = resultCollection.iterator();
		while (iterator.hasNext()) {
			AEntity entity = iterator.next();
			if (deleted.contains(entity.getId())) iterator.remove();
		}

		long time = rt.getRuntime();
		if (time > 100) {
			log.log(time > 1000 ? Log.Level.WARN : Log.Level.DEBUG, "Query provided", resultCollection.size(),
				"elements in", rt.getRuntimeFormated(), query);
		}
		return resultCollection;
	}

	public void setIgnoreModifications(boolean disabled) {
		this.ignoreModifications = disabled;
	}

	public Transaction setAutoCommit(boolean autoCommit) {
		this.autoCommit = autoCommit;
		return this;
	}

	public boolean isAutoCommit() {
		return autoCommit;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		boolean empty = true;
		if (!modified.isEmpty()) {
			sb.append("\n    Modified: ").append(formatIds(modified.getAllIds()));
			empty = false;
		}
		if (!deleted.isEmpty()) {
			sb.append("\n    Deleted: ").append(formatIds(deleted));
			empty = false;
		}
		if (empty) sb.append(" Empty");
		return sb.toString();
	}

	private String formatIds(Collection<String> ids) {
		if (ids.isEmpty()) return "0";
		int size = ids.size();
		if (size <= 7) return Str.format(ids);
		return String.valueOf(size);
	}

	public void runAfterCommit(Runnable runnable) {
		if (autoCommit) {
			runnable.run();
			return;
		}
		if (runnablesAfterCommit == null) runnablesAfterCommit = new LinkedList<Runnable>();
		runnablesAfterCommit.add(runnable);
	}

	public boolean isEmpty() {
		return (modified == null || modified.isEmpty()) && (deleted == null || deleted.isEmpty());
	}

	public String getName() {
		return name;
	}

	private static Map<String, Map<String, String>> updatePropertiesMap(
			Map<String, Map<String, String>> modifiedPropertiesByEntityId, AEntity entity, String field, String value) {
		if (modifiedPropertiesByEntityId == null)
			modifiedPropertiesByEntityId = new HashMap<String, Map<String, String>>();
		String id = entity.getId();
		Map<String, String> properties = modifiedPropertiesByEntityId.get(id);
		if (properties == null) {
			properties = new HashMap<String, String>();
			properties.put("id", id);
			modifiedPropertiesByEntityId.put(id, properties);
		}
		properties.put(field, value);
		return modifiedPropertiesByEntityId;
	}

	private static Map<String, Map<String, String>> updatePropertiesMap(
			Map<String, Map<String, String>> modifiedPropertiesByEntityId, AEntity entity) {
		if (modifiedPropertiesByEntityId == null)
			modifiedPropertiesByEntityId = new HashMap<String, Map<String, String>>();
		String id = entity.getId();
		Map<String, String> properties = entity.createPropertiesMap();
		modifiedPropertiesByEntityId.put(id, properties);
		return modifiedPropertiesByEntityId;
	}

	public static Transaction get() {
		return AEntityDatabase.get().getTransaction();
	}

	class CommitCallback implements Runnable {

		@Override
		public void run() {
			if (runnablesAfterCommit != null) {
				for (Runnable runnable : runnablesAfterCommit) {
					runnable.run();
				}
			}
		}

	}

}
