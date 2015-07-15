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
package ilarkesto.gwt.client;

import ilarkesto.core.base.Str;
import ilarkesto.core.logging.Log;
import ilarkesto.core.persistance.AEntity;
import ilarkesto.core.persistance.Persistence;
import ilarkesto.gwt.client.persistence.AGwtEntityFactory;
import ilarkesto.gwt.client.persistence.GwtRpcDatabase;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;

public abstract class AGwtApplication<D extends ADataTransferObject> implements EntryPoint {

	protected final Log log = Log.get(getClass());

	private static AGwtApplication singleton;
	protected int conversationNumber = -1;
	protected GwtLogRecordHandler logRecordHandler;
	private String abortMessage;
	private GwtRpcDatabase entitiesBackend;

	protected abstract void init();

	public abstract void handleServiceCallError(String serviceCall, List<ErrorWrapper> errors);

	protected abstract void handleUnexpectedError(Throwable ex);

	protected abstract AGwtEntityFactory getEntityFactory();

	public AGwtApplication() {
		if (singleton != null) throw new RuntimeException("GWT application already instantiated: " + singleton);
		singleton = this;
		GwtSynchronizer.install();
		logRecordHandler = new GwtLogRecordHandler();
		Log.setLogRecordHandler(logRecordHandler);
		GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {

			@Override
			public void onUncaughtException(Throwable ex) {
				handleUnexpectedError(ex);
			}
		});
		History.addValueChangeHandler(new HistoryTokenChangedHandler());
	}

	@Override
	public final void onModuleLoad() {
		AGwtEntityFactory entityFactory = getEntityFactory();
		if (entityFactory != null) {
			entitiesBackend = new GwtRpcDatabase(entityFactory);
			Persistence.initialize(entitiesBackend, new GwtTransactionManager());
		}
		init();
	}

	public GwtRpcDatabase getEntitiesBackend() {
		return entitiesBackend;
	}

	public final boolean isAborted() {
		return abortMessage != null;
	}

	public final void abort(String message) {
		if (Str.isBlank(message)) message = "Unexpected error";
		abortMessage = message;
		onAborted(message);
	}

	protected void onAborted(String message) {}

	protected void onHistoryTokenChanged(String token) {
		log.info("History token changed:", token);
		onHistoryTokenChanged(Gwt.parseHistoryToken(token));
	}

	protected void onHistoryTokenChanged(LinkedHashMap<String, String> parameters) {}

	final void serverDataReceived(D data) {
		if (data.conversationNumber != null) {
			log.info("conversatioNumber received:", data.conversationNumber);
			conversationNumber = data.conversationNumber;
		}
		if (data.containsDeletedEntities()) {
			Set<String> entityIds = data.getDeletedEntities();
			log.debug("entity deletions received:", entityIds);
			getEntitiesBackend().onEntityDeletionsReceived(entityIds);
			onEntityDeletionsReceived(entityIds);
		}
		if (data.containsEntities()) {
			Collection<Map<String, String>> entityDatas = data.getEntities();
			log.debug("entities received:", entityDatas);
			Set<AEntity> entities = getEntitiesBackend().updateFromServer(entityDatas);
			onEntitiesReceived(entities);
		}
		if (data.isUserSet()) {
			String userId = data.getUserId();
			log.info("user-id received:", userId);
			onUserIdReceived(userId);
		}
		onServerDataReceived(data);
	}

	protected void onServiceCallSuccessfullyProcessed(AServiceCall<D> serviceCall) {}

	protected void onEntitiesReceived(Set<AEntity> entities) {}

	protected void onEntityDeletionsReceived(Set<String> entityIds) {}

	protected void onUserIdReceived(String userId) {}

	protected void onServerDataReceived(D data) {}

	public final int getConversationNumber() {
		return conversationNumber;
	}

	public final void resetConversation() {
		conversationNumber = -1;
	}

	@Override
	public String toString() {
		return getClass().getName();
	}

	public static AGwtApplication get() {
		return singleton;
	}

	public abstract void sendChangesToServer(Collection<AEntity> modified, Collection<String> deleted,
			Map<String, Map<String, String>> modifiedProperties, Runnable callback);

	private class HistoryTokenChangedHandler implements ValueChangeHandler<String> {

		@Override
		public void onValueChange(ValueChangeEvent<String> event) {
			onHistoryTokenChanged(event.getValue());
		}

	}

}
