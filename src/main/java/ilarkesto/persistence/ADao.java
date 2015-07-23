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

import ilarkesto.auth.Auth;
import ilarkesto.auth.AuthUser;
import ilarkesto.auth.Ownable;
import ilarkesto.base.Iconized;
import ilarkesto.base.Reflect;
import ilarkesto.core.base.RuntimeTracker;
import ilarkesto.core.base.Str;
import ilarkesto.core.fp.Predicate;
import ilarkesto.core.logging.Log;
import ilarkesto.core.persistance.AEntityQuery;
import ilarkesto.core.persistance.AllByTypeQuery;
import ilarkesto.core.persistance.EntityDoesNotExistException;
import ilarkesto.core.search.SearchText;
import ilarkesto.core.search.Searchable;
import ilarkesto.di.Context;
import ilarkesto.id.IdentifiableResolver;
import ilarkesto.search.SearchResultsConsumer;
import ilarkesto.search.Searcher;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ADao<E extends AEntity> implements IdentifiableResolver<E>, Searcher, DaoListener, Iconized,
		Comparable<ADao> {

	private final Log log = Log.get(getClass());

	private Predicate<Class> entityTypeFilter;
	private String icon;

	// --- ---

	protected int getOrderIndex() {
		return 0;
	}

	public boolean isSkipLoadingEntityOnFailure() {
		return false;
	}

	// --- basic ---

	public abstract String getEntityName();

	public abstract Class getEntityClass();

	public Map<String, Class> getAliases() {
		return Collections.emptyMap();
	}

	private boolean isPersistent(E entity) {
		return Transaction.get().containsWithId(entity.getId());
	}

	public final Predicate<Class> getEntityTypeFilter() {
		if (entityTypeFilter == null) {
			entityTypeFilter = new Predicate<Class>() {

				@Override
				public boolean test(Class parameter) {
					return parameter.isAssignableFrom(getEntityClass());
				}

			};
		}
		return entityTypeFilter;
	}

	@Override
	public String getIcon() {
		if (icon == null) {
			icon = (String) Reflect.getFieldValue(getEntityClass(), "ICON");
			if (icon == null) icon = Str.lowercaseFirstLetter(getEntityName());
		}
		return icon;
	}

	public E getEntity(final Predicate<E> predicate) {
		return (E) Transaction.get().findFirst(new AEntityQuery<E>() {

			@Override
			public boolean test(E entity) {
				return predicate.test(entity);
			}

			@Override
			public Class<E> getType() {
				return ADao.this.getEntityClass();
			}
		});
	}

	public final Set<E> getEntities(final Predicate<E> filter) {
		return (Set<E>) Transaction.get().findAllAsSet(new AEntityQuery<E>() {

			@Override
			public boolean test(E entity) {
				return filter.test(entity);
			}

			@Override
			public Class<E> getType() {
				return ADao.this.getEntityClass();
			}
		});
	}

	@Override
	public E getById(String id) {
		if (id == null) throw new RuntimeException("id must not be null");
		E entity = (E) Transaction.get().getById(id);
		if (entity == null) throw new EntityDoesNotExistException(id);
		return entity;
	}

	@Deprecated
	public E getEntityById(String id) {
		return getById(id);
	}

	@Override
	public List<E> getByIdsAsList(Collection<String> entitiesIds) {
		Set<String> ids = new HashSet<String>(entitiesIds);
		List<E> result = (List<E>) Transaction.get().getByIdsAsList(entitiesIds);
		if (result.size() != ids.size()) {
			result = new ArrayList<E>();
			for (String id : ids) {
				E entity = (E) Transaction.get().getById(id);
				result.add(entity);
			}
		}
		return result;
	}

	public Set<E> getByIdsAsSet(Collection<String> entitiesIds) {
		return new HashSet<E>(getByIdsAsList(entitiesIds));
	}

	@Deprecated
	public List<E> getEntitiesByIds(Collection<String> entitiesIds) {
		return getByIdsAsList(entitiesIds);
	}

	public Set<E> getEntitiesVisibleForUser(final AuthUser user) {
		return getEntities(new Predicate<E>() {

			@Override
			public boolean test(E e) {
				return Auth.isVisible(e, user);
			}

		});
	}

	public Set<E> getEntities() {
		return (Set<E>) Transaction.get().findAllAsSet(new AllByTypeQuery(getEntityClass()));
	}

	public void persist(E entity) {
		Transaction.get().persist(entity);
		daoService.fireEntitySaved(entity);
	}

	public E newEntityInstance(AuthUser user) {
		E entity = newEntityInstance();
		if (entity instanceof Ownable) ((Ownable) entity).setOwner(user);
		return entity;
	}

	public E newEntityInstance(String id) {
		E entity;
		try {
			entity = (E) getEntityClass().newInstance();
		} catch (InstantiationException ex) {
			throw new RuntimeException(ex);
		} catch (IllegalAccessException ex) {
			throw new RuntimeException(ex);
		}
		if (id != null) entity.setId(id);
		entity.updateLastModified();
		return entity;
	}

	public E newEntityInstance() {
		E entity = newEntityInstance((String) null);
		return entity;
	}

	public void ensureIntegrity() {
		if (!initialized) throw new RuntimeException("Not initialized!");
		Class clazz = getEntityClass();
		log.info("Ensuring integrity:", clazz.getSimpleName());
		for (E entity : getEntities()) {
			try {
				entity.ensureIntegrity();
			} catch (EnsureIntegrityCompletedException ex) {
				continue;
			} catch (Throwable ex) {
				throw new RuntimeException("Ensuring integrity for " + clazz.getSimpleName() + ":" + entity.getId()
						+ " failed.", ex);
			}
		}
	}

	@Override
	public void entityDeleted(EntityEvent event) {
		AEntity entity = event.getEntity();
		for (AEntity e : getEntities()) {
			try {
				e.repairDeadReferences(entity.getId());
			} catch (EnsureIntegrityCompletedException ex) {
				continue;
			}
		}
	}

	@Override
	public void entitySaved(EntityEvent event) {}

	@Override
	public void feed(final SearchResultsConsumer searchBox) {
		if (!Searchable.class.isAssignableFrom(getEntityClass())) return;

		final SearchText searchText = searchBox.getSearchText();
		final AuthUser searcher = searchBox.getSearcher();

		RuntimeTracker rt = new RuntimeTracker();
		Predicate<E> filter = new Predicate<E>() {

			@Override
			public boolean test(E e) {
				if (!e.matches(searchText)) return false;
				if (!Auth.isVisible(e, searcher)) return false;
				return true;
			}

		};
		for (AEntity entity : getEntities(filter)) {
			searchBox.addEntity(entity);
		}
		log.info("Search took", rt);
	}

	// ---

	protected Set<Class> getValueObjectClasses() {
		return Collections.emptySet();
	}

	@Override
	public String toString() {
		String entityName = getEntityName();
		if (entityName == null) return entityName + "Dao";
		return getClass().getName();
	}

	// --------------------
	// --- dependencies ---
	// --------------------

	private volatile boolean initialized;

	public synchronized final void initialize(Context context) {
		if (initialized) throw new RuntimeException("Already initialized!");

		Class entityClass = getEntityClass();
		context.autowireClass(entityClass);
		for (Class c : getValueObjectClasses())
			context.autowireClass(c);
		Field daoField;
		try {
			daoField = entityClass.getDeclaredField("dao");
			boolean accessible = daoField.isAccessible();
			if (!accessible) daoField.setAccessible(true);
			try {
				daoField.set(null, this);
			} catch (IllegalArgumentException ex) {
				throw new RuntimeException(ex);
			} catch (IllegalAccessException ex) {
				throw new RuntimeException(ex);
			} catch (NullPointerException ex) {
				throw new RuntimeException("Setting dao field failed. Is it static?", ex);
			}
			if (!accessible) daoField.setAccessible(false);
		} catch (SecurityException ex) {
			throw new RuntimeException(ex);
		} catch (NoSuchFieldException ex) {
			// nop
		}

		initialized = true;
	}

	private DaoService daoService;

	public final void setDaoService(DaoService daoService) {
		this.daoService = daoService;
	}

	public final DaoService getDaoService() {
		return daoService;
	}

	@Override
	public int compareTo(ADao other) {
		return ilarkesto.core.base.Utl.compare(getOrderIndex(), other.getOrderIndex());
	}

}
