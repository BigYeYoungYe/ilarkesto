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
package ilarkesto.tools.enhavo;

import ilarkesto.base.Proc;
import ilarkesto.core.base.Str;
import ilarkesto.core.parsing.ParseException;
import ilarkesto.io.IO;
import ilarkesto.templating.MustacheLikeTemplateParser;
import ilarkesto.templating.Template;
import ilarkesto.templating.TemplateResolver;

import java.io.File;

public class SiteContext extends ABuilder implements TemplateResolver {

	private File dir;
	private File pagesDir;
	private File templatesDir;
	private File contentDir;
	private File resourcesDir;
	private File scriptsDir;

	private File outputDir;
	private ContentProvider contentProvider;

	public SiteContext(CmsContext cms, File dir) {
		super(cms);
		this.dir = dir;

		pagesDir = new File(dir.getPath() + "/pages");
		IO.createDirectory(pagesDir);

		templatesDir = new File(dir.getPath() + "/templates");
		IO.createDirectory(templatesDir);

		contentDir = new File(dir.getPath() + "/content");
		IO.createDirectory(contentDir);

		resourcesDir = new File(dir.getPath() + "/resources");
		IO.createDirectory(resourcesDir);

		scriptsDir = new File(dir.getPath() + "/scripts");
		IO.createDirectory(scriptsDir);

		contentProvider = new FilesContentProvider(contentDir, cms.getContentProvider());
	}

	@Override
	protected void onBuild() {
		outputDir = new File(cms.getSitesOutputDir().getPath() + "/" + dir.getName());

		clean();

		processPagesFiles(pagesDir);
		IO.copyFiles(resourcesDir.listFiles(), outputDir);
		runScripts();
	}

	private void clean() {
		IO.delete(outputDir.listFiles());
	}

	private void runScripts() {
		for (File file : IO.listFiles(scriptsDir)) {
			cms.getProt().pushContext("script> " + file.getName());
			try {
				runScript(file);
			} catch (Exception ex) {
				error(ex);
			} finally {
				cms.getProt().popContext();
			}
		}
	}

	private void runScript(File file) {
		if (file.isDirectory()) return;
		File dst = new File(outputDir + "/" + file.getName());
		IO.copyFile(file, dst);
		String executorCommand = "bash";
		String output = Proc.execute(outputDir, executorCommand, dst.getAbsolutePath());
		info(output);
		// IO.delete(dst);
	}

	private void processPagesFiles(File dir) {
		for (File file : IO.listFiles(dir)) {
			if (file.isDirectory()) {
				processPagesFiles(file);
				continue;
			}
			String name = file.getName();
			if (name.endsWith(".json")) {
				FilePageContext page = new FilePageContext(this, file);
				page.build();
				continue;
			}
			writeOutputFile(getRelativePath(file), file);
		}
	}

	public void writeOutputFile(String path, String text) {
		File file = getOutputFile(path);
		info("output>", file.getPath());
		IO.writeFile(file, text, IO.UTF_8);
	}

	public void writeOutputFile(String path, File source) {
		File file = getOutputFile(path);
		info("output>", file.getPath());
		IO.copyFile(source, file);
	}

	private File getOutputFile(String path) {
		return new File(outputDir.getPath() + "/" + path);
	}

	public File getDir() {
		return dir;
	}

	public File getPagesDir() {
		return pagesDir;
	}

	@Override
	public String toString() {
		return dir.getName();
	}

	@Override
	public Template getTemplate(String templatePath) {
		File file = findTemplateFile(templatePath);
		if (file == null) {
			error("Template not found:", templatePath);
			return null;
		}
		info("template:", templatePath, "->", file.getPath());
		try {
			return MustacheLikeTemplateParser.parseTemplate(file);
		} catch (ParseException ex) {
			throw new RuntimeException(ex);
		}
	}

	public File findTemplateFile(String templatePath) {
		File file = new File(templatesDir.getPath() + "/" + templatePath);
		if (file.exists()) return file;
		return cms.findTemplateFile(templatePath);
	}

	public String getRelativePath(File file) {
		return Str.removePrefix(file.getPath(), getPagesDir().getPath() + "/");
	}

	public ContentProvider getContentProvider() {
		return contentProvider;
	}

}
