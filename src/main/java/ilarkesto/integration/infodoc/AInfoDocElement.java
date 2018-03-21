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
package ilarkesto.integration.infodoc;

import ilarkesto.json.JsonObject;

public abstract class AInfoDocElement {

	private InfoDocStructure structure;
	private Header header;

	public abstract String toHtml(AHtmlContext context, AReferenceResolver referenceResolver);

	public abstract JsonObject toJson(AReferenceResolver referenceResolver);

	public AInfoDocElement(InfoDocStructure structure) {
		super();
		this.structure = structure;
	}

	public boolean isPrefixed() {
		return false;
	}

	public final int getIndexInDepth() {
		return structure.getIndexInDepth(this);
	}

	AInfoDocElement setHeader(Header header) {
		this.header = header;
		return this;
	}

	public int getDepth() {
		if (header == null) return 0;
		return header.getDepth() + 1;
	}

	public Header getHeader() {
		return header;
	}

	public InfoDocStructure getStructure() {
		return structure;
	}

	public String getPrefix() {
		if (!isPrefixed() || !getStructure().isPrefixingRequired()) return null;
		return AItemCounter.get(getDepth()).getNumber(getIndexInDepth());
	}

}
