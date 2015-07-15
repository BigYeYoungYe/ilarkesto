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

import ilarkesto.core.persistance.ATransaction;
import ilarkesto.core.persistance.EntitiesBackend;
import ilarkesto.core.persistance.Persistence;
import ilarkesto.id.IdentifiableResolver;

public class Transaction extends ATransaction<AEntity> implements IdentifiableResolver<AEntity> {

	public Transaction(String name, boolean writable, boolean autoCommit, boolean ensureIntegrityOnCommit) {
		super(name, writable, autoCommit, ensureIntegrityOnCommit);
	}

	@Override
	protected EntitiesBackend getBackend() {
		return Persistence.backend;
	}

	public static Transaction get() {
		return (Transaction) Persistence.transactionManager.getCurrentTransaction();
	}

}
