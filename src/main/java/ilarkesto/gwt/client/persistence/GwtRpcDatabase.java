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
package ilarkesto.gwt.client.persistence;

import ilarkesto.core.persistance.ACachingEntityDatabase;
import ilarkesto.core.persistance.AEntity;
import ilarkesto.core.persistance.AEntityDatabase;
import ilarkesto.core.persistance.EntityDoesNotExistException;
import ilarkesto.core.persistance.Transaction;
import ilarkesto.gwt.client.AGwtApplication;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GwtRpcDatabase extends ACachingEntityDatabase {

	private Transaction transaction;
	private AGwtEntityFactory factory;
	private int transactionNumber;

	public GwtRpcDatabase(AGwtEntityFactory factory) {
		super();
		this.factory = factory;
	}

	@Override
	protected void onUpdate(Collection<AEntity> modified, Collection<String> deleted,
			Map<String, Map<String, String>> modifiedProperties, Runnable callback) {
		AGwtApplication.get().sendChangesToServer(modified, deleted, modifiedProperties, callback);
	}

	@Override
	public Transaction getTransaction() {
		if (transaction == null) {
			transaction = new Transaction(this, "GWT-" + (++transactionNumber), true, false);
			log.info("Transaction created:", transaction.getName());
		}
		return transaction;
	}

	@Override
	public void onTransactionFinished(Transaction transaction) {
		log.debug("Transaction finished:", transaction.getName());
		if (this.transaction == transaction) this.transaction = null;
	}

	public void onEntitiesReceived(Collection<HashMap<String, String>> entityDatas) {
		Transaction t = getTransaction();
		t.setIgnoreModifications(true);
		try {
			for (Map<String, String> data : entityDatas) {
				String id = data.get("id");
				AEntity entity;
				try {
					entity = cache.get(id);
				} catch (EntityDoesNotExistException ex) {
					String type = data.get("@type");
					entity = factory.createEntity(type, id);
					cache.add(entity);
				}
				entity.updateProperties(data);
			}
		} finally {
			t.setIgnoreModifications(false);
		}
	}

	public void onEntityDeletionsReceived(Set<String> entityIds) {
		cache.removeAll(entityIds);
	}

	public static void initialize(AGwtEntityFactory factory) {
		AEntityDatabase.instance = new GwtRpcDatabase(factory);
	}

	public static GwtRpcDatabase get() {
		return (GwtRpcDatabase) AEntityDatabase.instance;
	}

}
