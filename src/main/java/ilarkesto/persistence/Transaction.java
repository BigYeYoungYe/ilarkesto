/*
 * Copyright 2011 Witoslaw Koczewsi <wi@koczewski.de>, Artjom Kochtchi
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero
 * General Public License as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package ilarkesto.persistence;

import ilarkesto.base.Utl;
import ilarkesto.core.logging.Log;
import ilarkesto.core.persistance.AEntityQuery;
import ilarkesto.core.persistance.ATransaction;
import ilarkesto.core.persistance.EntitiesBackend;
import ilarkesto.id.IdentifiableResolver;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Transaction extends ATransaction<AEntity> implements IdentifiableResolver<AEntity> {

	private static final Log log = Log.get(Transaction.class);

	static EntitiesBackend<AEntity, Transaction> backend;

	private static int count = 0;
	private int no;

	private String threadName;
	private Set<AEntity> entitiesToSave = new HashSet<AEntity>();
	private Set<AEntity> entitiesToDelete = new HashSet<AEntity>();
	private Set<AEntity> entitiesRegistered = new HashSet<AEntity>();

	public Transaction() {
		synchronized (getClass()) {
			no = ++count;
		}
		threadName = Thread.currentThread().getName();
	}

	@Override
	protected EntitiesBackend getBackend() {
		return backend;
	}

	void saveEntity(AEntity entity) {
		if (entity == null) throw new NullPointerException("entity");
		entity.getId();
		if (entitiesToSave.contains(entity) || entitiesToDelete.contains(entity)) return;
		log.debug("SAVE", toStringWithType(entity), "@", this);
		entitiesToSave.add(entity);
	}

	void deleteEntity(AEntity entity) {
		if (entitiesToDelete.contains(entity)) return;
		log.debug("DELETE", toStringWithType(entity), "@", this);
		entitiesToDelete.add(entity);
		entitiesToSave.remove(entity);
	}

	public void register(AEntity entity) {
		entitiesRegistered.add(entity);
	}

	private boolean committed;

	@Override
	public void commit() {
		if (committed) throw new RuntimeException("Transaction already committed: " + this);
		try {
			doCommit();
		} finally {
			backend.onTransactionFinished(this);
		}
	}

	private void doCommit() {
		committed = true;

		if (entitiesToDelete.isEmpty() && entitiesToSave.isEmpty()) {
			log.debug("Empty Transaction committed:", this);
			return;
		} else {
			log.info("Committing transaction:", this);
		}

		Set<AEntity> integratedEntities = new HashSet<AEntity>(entitiesToSave.size());

		int loopcount = 0;
		while (!integratedEntities.containsAll(entitiesToSave)) {

			if (loopcount > 0) {
				HashSet<AEntity> tmp = new HashSet<AEntity>(entitiesToSave);
				tmp.removeAll(integratedEntities);
				log.debug("  Entities changed after ensuring integrity:", tmp);
			}

			if (loopcount > 1000) throw new RuntimeException("Maximum loops reached while commiting:" + this);

			entitiesToSave.removeAll(entitiesToDelete);
			for (AEntity entity : new HashSet<AEntity>(entitiesToSave)) {
				log.debug("Ensuring integrity for", entity.getClass().getSimpleName(), entity.getId());
				entity.ensureIntegrity();
				integratedEntities.add(entity);
			}
			entitiesToSave.removeAll(entitiesToDelete);

			loopcount++;
		}

		log.debug("Persisting entities:", entitiesToSave, entitiesToDelete);
		backend.update(entitiesToSave, Persist.getIdsAsList(entitiesToDelete), null, null);

		log.debug("Transaction committed:", this);
		entitiesToSave.clear();
		entitiesToDelete.clear();
		entitiesRegistered.clear();
	}

	@Override
	public void rollback() {
		log.debug("Transaction canceled:", this);
		entitiesToSave.clear();
		entitiesToDelete.clear();
		entitiesRegistered.clear();
		backend.onTransactionFinished(this);
	}

	@Override
	public boolean containsWithId(String id) {
		if (id == null) return false;

		if (Persist.getIdsAsList(entitiesToDelete).contains(id)) return false;

		AEntity result = backend.getById(id);
		if (result != null) return true;

		for (AEntity entity : entitiesToSave) {
			if (id.equals(entity.getId())) return true;
		}

		// ignore registeredEntities!
		return false;
	}

	@Override
	public AEntity getById(String id) {
		AEntity result = backend.getById(id);
		if (result == null && !entitiesToSave.isEmpty()) {
			for (AEntity entity : entitiesToSave) {
				if (id.equals(entity.getId())) {
					result = entity;
					break;
				}
			}
		}
		if (result == null && !entitiesRegistered.isEmpty()) {
			for (AEntity entity : entitiesRegistered) {
				if (id.equals(entity.getId())) {
					result = entity;
					break;
				}
			}
		}
		if (result != null && entitiesToDelete.contains(result)) return null;
		return result;
	}

	@Override
	public <C extends Collection<AEntity>> C findAll(AEntityQuery<AEntity> query, C resultCollection) {
		resultCollection.addAll(backend.findAll(query, resultCollection));
		for (AEntity entity : entitiesToSave) {
			if (resultCollection.contains(entity)) continue;
			if (Persist.test(entity, query)) resultCollection.add(entity);
		}
		for (AEntity entity : entitiesRegistered) {
			if (resultCollection.contains(entity)) continue;
			if (Persist.test(entity, query)) resultCollection.add(entity);
		}
		resultCollection.removeAll(entitiesToDelete);
		return resultCollection;
	}

	@Override
	public AEntity findFirst(AEntityQuery query) {
		AEntity result = backend.findFirst(query);
		if (result == null) {
			for (AEntity entity : entitiesToSave) {
				if (entitiesToDelete.contains(entity)) continue;
				if (Persist.test(entity, query)) return entity;
			}
			for (AEntity entity : entitiesRegistered) {
				if (entitiesToDelete.contains(entity)) continue;
				if (Persist.test(entity, query)) return entity;
			}
		} else {
			if (entitiesToDelete.contains(result)) return null;
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("#").append(no);
		sb.append(" (").append(threadName).append(")");
		if (!entitiesToSave.isEmpty()) {
			sb.append("\n    SAVE: ").append(toString(entitiesToSave));
		}
		if (!entitiesRegistered.isEmpty()) {
			sb.append("\n    REGISTERED: ").append(toString(entitiesRegistered));
		}
		if (!entitiesToDelete.isEmpty()) {
			sb.append("\n    DELETE: ").append(toString(entitiesToDelete));
		}
		return sb.toString();
	}

	private String toString(Collection<AEntity> entities) {
		StringBuilder sb = new StringBuilder();
		for (AEntity entity : entities) {
			if (entity == null) {
				sb.append("\n        null");
			} else {
				sb.append("\n        ").append(entity.getClass().getSimpleName()).append(": ")
						.append(entity.toString());
			}
		}
		return sb.toString();
	}

	private String toStringWithType(Object o) {
		try {
			return Utl.toStringWithType(o);
		} catch (Exception ex) {
			return o.getClass().getSimpleName();
		}
	}

	public static Transaction get() {
		return backend.getTransaction();
	}

}
