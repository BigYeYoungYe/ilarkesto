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
package ilarkesto.tools.cms;

import ilarkesto.core.logging.Log;
import ilarkesto.io.IO;

import java.io.File;

public class CmsContext {

	protected final Log log = Log.get(getClass());

	private File dir;
	private File inputDir;
	private File sitesDir;
	private File templatesDir;
	private File outputDir;

	private BuildProtocol prot;

	public CmsContext(File dir) {
		this.dir = dir;

		outputDir = new File(dir.getPath() + "/output");
		IO.createDirectory(outputDir);

		inputDir = new File(dir.getPath() + "/input");
		sitesDir = new File(inputDir.getPath() + "/sites");
		IO.createDirectory(sitesDir);
		templatesDir = new File(inputDir.getPath() + "/templates");
		IO.createDirectory(templatesDir);
	}

	public void build() {
		prot = new BuildProtocol();
		prot.pushContext(dir.getAbsolutePath());
		buildSites();
		prot.popContext();
	}

	private void buildSites() {
		for (File siteDir : IO.listFiles(sitesDir)) {
			if (!siteDir.isDirectory()) continue;
			SiteContext siteContext = new SiteContext(this, siteDir);
			siteContext.build();
		}
	}

	public File getDir() {
		return dir;
	}

	public BuildProtocol getProt() {
		return prot;
	}

	public File getOutputDir() {
		return outputDir;
	}

	public File getInputDir() {
		return inputDir;
	}

	public File findTemplateFile(String templatePath) {
		File file = new File(templatesDir.getPath() + "/" + templatePath);
		if (file.exists()) return file;
		return null;
	}

}
