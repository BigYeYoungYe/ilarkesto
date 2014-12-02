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

import ilarkesto.core.fp.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class AEntityQuery<T extends AEntity> implements Predicate<T> {

	public Set<T> list() {
		return (Set<T>) AEntityDatabase.get().getTransaction().list(this);
	}

	public T getFirst() {
		return (T) AEntityDatabase.get().getTransaction().getFirst(this);
	}

	@Override
	public abstract boolean test(T entity);

	public Class<T> getType() {
		return null;
	}

	public List<T> filter(Collection<T> entities) {
		ArrayList<T> ret = new ArrayList<T>();
		for (T entity : entities) {
			if (test(entity)) ret.add(entity);
		}
		return ret;
	}

}
