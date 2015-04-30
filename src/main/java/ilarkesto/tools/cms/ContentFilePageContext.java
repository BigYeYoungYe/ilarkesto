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

import ilarkesto.core.base.Str;
import ilarkesto.json.JsonObject;
import ilarkesto.templating.Context;
import ilarkesto.templating.Template;
import ilarkesto.templating.TemplateResolver;

import java.io.File;

public class ContentFilePageContext extends ABuilder implements TemplateResolver {

	private SiteContext site;
	private File contentFile;
	private JsonObject content;
	private JsonObject data;
	private String templatePath;
	private Template template;

	public ContentFilePageContext(SiteContext site, File contentFile) {
		super(site.cms);
		this.site = site;
		this.contentFile = contentFile;
	}

	@Override
	protected void onBuild() {
		content = JsonObject.loadFile(contentFile, false);

		templatePath = content.getString("template");
		template = site.getTemplate(templatePath);
		if (template == null) {
			error("ABORTED");
			return;
		}

		data = content.getObject("data");

		String outputPath = getContentFilePath().replace(".page.json", ".html");
		Context templateContext = creaeTemplateContext();
		template.process(templateContext);
		site.writeOutputFile(outputPath, templateContext.popOutput());
	}

	private Context creaeTemplateContext() {
		Context context = new Context();
		context.setTemplateResolver(this);
		context.setScope(data);
		return context;
	}

	@Override
	public Template getTemplate(String path) {
		int idx = templatePath.lastIndexOf('/');
		if (idx > 1) {
			path = templatePath.substring(0, idx) + "/" + path;
		}
		return site.getTemplate(path);
	}

	@Override
	public String toString() {
		return getContentFilePath();
	}

	private String getContentFilePath() {
		return Str.removePrefix(contentFile.getPath(), site.getContentDir().getPath() + "/");
	}

}
