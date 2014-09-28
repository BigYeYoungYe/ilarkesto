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
package ilarkesto.io;

import ilarkesto.core.base.Str;
import ilarkesto.core.logging.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public abstract class ACsvWriter<R> {

	private Log log = Log.get(getClass());

	private List<AColumn> columns;

	protected abstract void createColumns();

	protected abstract Iterable<R> getRecords();

	public final void write(CsvWriter csv) {

		columns = new ArrayList<AColumn>();
		createColumns();
		List<String> headers = new ArrayList<String>();
		for (AColumn column : columns) {
			headers.add(column.getName());
		}
		csv.writeHeaders(headers);

		Iterable<R> records = getRecords();
		for (R record : records) {
			for (AColumn column : columns) {
				Object value;
				try {
					value = column.getValue(record);
				} catch (Exception ex) {
					handleExceptionOnGetValue(ex);
					csv.writeField("");
					continue;
				}
				csv.writeField(Str.format(value));
			}
			csv.closeRecord();
		}

		csv.close();
	}

	public final void write(Writer out) {
		write(new CsvWriter(out));
	}

	public final void write(File file) throws IOException {
		log.info("Writing", file.getAbsolutePath());
		IO.createDirectory(file.getParentFile());
		write(new BufferedWriter(new FileWriter(file)));
	}

	protected void handleExceptionOnGetValue(Exception ex) {
		throw new RuntimeException(ex);
	}

	protected final void addColumn(AColumn column) {
		columns.add(column);
	}

	public abstract class AColumn {

		public abstract String getName();

		public abstract Object getValue(R record);

	}

}
