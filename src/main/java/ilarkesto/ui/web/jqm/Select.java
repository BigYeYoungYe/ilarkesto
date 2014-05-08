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
package ilarkesto.ui.web.jqm;

import ilarkesto.core.base.Str;
import ilarkesto.ui.web.HtmlBuilder;
import ilarkesto.ui.web.HtmlBuilder.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

public class Select extends AFieldElement {

	private String value;
	private Map<String, String> options = new LinkedHashMap<String, String>();
	private boolean optional = true;
	private String dataRole;

	public Select(JqmHtmlPage htmlPage, String id, String label) {
		super(htmlPage, id, label);
	}

	@Override
	protected void renderField(HtmlBuilder html, String id) {
		Tag select = html.startSELECT(name);
		select.setId(id);
		select.setDataRole(dataRole);

		if (optional) {
			html.OPTION("", "", Str.isBlank(value));
		}

		for (Map.Entry<String, String> option : options.entrySet()) {
			String key = option.getKey();
			html.OPTION(key, option.getValue(), key.equals(value));
		}

		html.endSELECT();
	}

	public Select setValue(String value) {
		this.value = value;
		return this;
	}

	public Select setValue(Boolean value) {
		if (value == null) return setValue((String) null);
		return setValue(value.toString());
	}

	public Select setOptions(Map<String, String> options) {
		this.options = options;
		return this;
	}

	public Select addOption(String key, String label) {
		options.put(key, label);
		return this;
	}

	public Select addBooleanOptions(String trueLabel, String falseLabel) {
		optional = false;
		addOption("false", falseLabel);
		addOption("true", trueLabel);
		return this;
	}

	public Select setDataRole(String dataRole) {
		this.dataRole = dataRole;
		return this;
	}

	public Select setDataRoleToSlider() {
		return setDataRole("slider");
	}

}
