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
import ilarkesto.core.persistance.AEntityDatabase;
import ilarkesto.core.persistance.Transaction;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Image;

public abstract class AAction implements Command, ClickHandler {

	protected final Log log = Log.get(getClass());
	private Updatable updatable = RootWidgetUpdatable.INSTANCE;

	private ClickEvent clickEvent;

	public abstract String getLabel();

	protected abstract void onExecute();

	@Override
	public final void execute() {
		if (!isExecutable()) throw new RuntimeException("Action not executable: " + this);
		if (!isPermitted()) throw new RuntimeException("Action not permitted: " + this);
		if (AEntityDatabase.instance == null) {
			onExecute();
			if (updatable != null) updatable.update();
		} else {
			Transaction transaction = Transaction.get().setAutoCommit(false);
			if (updatable != null) transaction.runAfterCommit(new Runnable() {

				@Override
				public void run() {
					if (updatable != null) updatable.update();
				}
			});
			try {
				onExecute();
				transaction.commit();
			} catch (Exception ex) {
				try {
					handleException(ex);
				} catch (Exception ex1) {
					throw new RuntimeException(Str.getSimpleName(getClass()) + ".onExecute() failed", ex1);
				}
			} finally {
				transaction.setAutoCommit(true);
			}
		}
	}

	protected void handleException(Exception ex) throws Exception {
		throw ex;
	}

	public String getTargetHistoryToken() {
		return null;
	}

	@Override
	public void onClick(ClickEvent event) {
		this.clickEvent = event;
		event.stopPropagation();
		execute();
	}

	public Image getIcon() {
		return null;
	}

	public boolean isExecutable() {
		return true;
	}

	public boolean isPermitted() {
		return true;
	}

	public String getTooltip() {
		return null;
	}

	public String getTooltipAsHtml() {
		return Str.toHtml(getTooltip());
	}

	public String getId() {
		return Str.getSimpleName(getClass()).replace('$', '_');
	}

	@Override
	public String toString() {
		return Str.getSimpleName(getClass());
	}

	public AAction setUpdatable(Updatable updatable) {
		this.updatable = updatable;
		return this;
	}

	protected Updatable getUpdatable() {
		return updatable;
	}

	protected boolean isUpdatableSet() {
		return updatable != null;
	}

	protected ClickEvent getClickEvent() {
		return clickEvent;
	}

	protected boolean isClickEventSet() {
		return clickEvent != null;
	}
}
