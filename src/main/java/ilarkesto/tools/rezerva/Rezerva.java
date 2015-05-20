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
package ilarkesto.tools.rezerva;

import ilarkesto.io.IO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Rezerva {

	private File dir;

	public Rezerva(File dir) {
		super();
		this.dir = dir;
	}

	private List<BackupConfig> loadBackupConfigs() {
		ArrayList<BackupConfig> ret = new ArrayList<BackupConfig>();
		for (File file : IO.listFiles(dir)) {
			if (!file.isDirectory()) continue;
			if (!new File(file.getPath() + "/rezerva.json").exists()) continue;
			ret.add(new BackupConfig(this, file));
		}
		return ret;
	}

}
