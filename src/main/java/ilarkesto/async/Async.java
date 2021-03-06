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
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package ilarkesto.async;

public class Async {

	private static AsyncWorker worker;

	public static void start(Job job) {
		getWorker().start(job);
	}

	public static AsyncWorker getWorker() {
		if (worker == null) worker = new ExecutorAsyncWorker();
		return worker;
	}

	public static void setWorker(AsyncWorker worker) {
		Async.worker = worker;
	}

}
