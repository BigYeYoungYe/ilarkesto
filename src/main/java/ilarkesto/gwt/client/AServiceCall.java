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
package ilarkesto.gwt.client;

import ilarkesto.core.base.Str;
import ilarkesto.core.base.Utl;
import ilarkesto.core.logging.Log;
import ilarkesto.core.service.ServiceCall;
import ilarkesto.core.time.Tm;

import java.util.LinkedList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class AServiceCall<D extends ADataTransferObject> implements ServiceCall {

	public static final long MAX_FAILURE_TIME = 30 * Tm.SECOND;

	private static List<ServiceCall> activeServiceCalls = new LinkedList<ServiceCall>();
	private static long lastSuccessfullServiceCallTime;
	public static Runnable listener;

	protected final Log log = Log.get(getClass());

	private Runnable returnHandler;
	private long startTime = -1;
	private long runtime = -1;

	protected abstract void onExecute(int conversationNumber, AsyncCallback<D> callback);

	@Override
	public final void execute() {
		execute(null);
	}

	@Override
	public final void execute(Runnable returnHandler) {
		if (startTime >= 0) throw new IllegalStateException(getName() + " already executed");
		this.returnHandler = returnHandler;

		activeServiceCalls.add(this);
		startTime = Tm.getCurrentTimeMillis();
		onExecute(AGwtApplication.get().getConversationNumber(), new ServiceCallback());
		if (listener != null) listener.run();
	}

	public static final boolean containsServiceCall(Class<? extends ServiceCall> type) {
		String name = Str.getSimpleName(type);
		for (ServiceCall call : activeServiceCalls) {
			String callName = Str.getSimpleName(call.getClass());
			if (callName.equals(name)) return true;
		}
		return false;
	}

	@Override
	public boolean isDispensable() {
		return false;
	}

	public final long getRuntime() {
		return runtime;
	}

	public final String getName() {
		return Str.removeSuffix(Str.getSimpleName(getClass()), "ServiceCall");
	}

	public static List<ServiceCall> getActiveServiceCalls() {
		return activeServiceCalls;
	}

	private void onServiceCallReturned() {}

	private void serviceCallReturned() {
		activeServiceCalls.remove(this);
		runtime = Tm.getCurrentTimeMillis() - startTime;
		onServiceCallReturned();
		if (listener != null) listener.run();
	}

	protected void onCallbackError(List<ErrorWrapper> errors) {}

	private void callbackError(List<ErrorWrapper> errors) {
		log.error("Service call", getName(), "failed:", errors);
		onCallbackError(errors);
		long timeFromLastSuccess = Tm.getCurrentTimeMillis() - lastSuccessfullServiceCallTime;
		if (isDispensable() && timeFromLastSuccess < AServiceCall.MAX_FAILURE_TIME) {
			log.warn("Dispensable service call failed:", getName(), errors);
			return;
		}
		AGwtApplication.get().handleServiceCallError(getName(), errors);
	}

	protected void onCallbackSuccess(D data) {}

	private void callbackSuccess(D data) {
		lastSuccessfullServiceCallTime = Tm.getCurrentTimeMillis();
		AGwtApplication.get().serverDataReceived(data);
		onCallbackSuccess(data);
		if (returnHandler != null) returnHandler.run();
	}

	protected class ServiceCallback implements AsyncCallback<D> {

		@Override
		public void onFailure(Throwable ex) {
			onServiceCallReturned();
			callbackError(Utl.toList(new ErrorWrapper(ex)));
		}

		@Override
		public void onSuccess(D data) {
			serviceCallReturned();

			List<ErrorWrapper> errors = data.getErrors();
			if (errors != null && !errors.isEmpty()) {
				onCallbackError(errors);
				return;
			}

			callbackSuccess(data);
		}

	}

}
