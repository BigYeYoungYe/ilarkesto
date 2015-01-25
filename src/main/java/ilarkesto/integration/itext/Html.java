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
package ilarkesto.integration.itext;

import ilarkesto.core.base.Utl;
import ilarkesto.pdf.AHtml;
import ilarkesto.pdf.APdfElement;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.html.simpleparser.HTMLWorker;
import com.itextpdf.text.html.simpleparser.StyleSheet;

public class Html extends AHtml implements ItextElement {

	Html(APdfElement parent) {
		super(parent);
	}

	@Override
	public Element[] createITextElements(Document document) {
		HTMLWorker worker = new HTMLWorker(document);
		StringReader reader = new StringReader(code);
		List<Element> elements;
		StyleSheet style = new StyleSheet();
		try {
			elements = worker.parseToList(reader, style);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return Utl.toArray(elements, new Element[elements.size()]);
	}
}
