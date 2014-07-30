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

import ilarkesto.base.Sys;
import ilarkesto.pdf.AImage;
import ilarkesto.pdf.APageExtension;
import ilarkesto.pdf.APageLayer;
import ilarkesto.pdf.AParagraph;
import ilarkesto.pdf.APdfBuilder;
import ilarkesto.pdf.ATable;
import ilarkesto.pdf.FontStyle;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

public class PdfBuilder extends APdfBuilder {

	public static void main(String[] args) throws Throwable {
		PdfBuilder pdf = new PdfBuilder();
		FontStyle fs = new FontStyle();
		fs.setSize(20);
		pdf.paragraph().setHeight(72).text("first", fs);
		pdf.paragraph().setHeight(10).text("second", new FontStyle().setBold(true));
		pdf.paragraph().setHeight(10).text("specials: ä ü ö ß ł 7° m³");
		pdf.paragraph().setHeight(10).text("japanese: " + "日本語の文字");
		pdf.paragraph().setHeight(10).text("chinese: \u5341\u950a\u57cb\u4f0f" + "這是美好一天");
		pdf.paragraph().setHeight(1);
		pdf.paragraph().text("--------------------------");
		ATable table = pdf.table(50, 50);
		table.cell().paragraph().text("1 ABC");
		table.cell().setBorder(Color.RED, 0.5f).paragraph().text("2 ABC\u00DC\u00DC\nABCDEF");
		table.cell().paragraph().text("3 ABC");
		table.cell().paragraph().text("4 ABC");
		String path = "/inbox/test.pdf";
		pdf.write(new FileOutputStream(Sys.getUsersHomePath() + path));
	}

	private Collection<ItextElement> elements = new ArrayList<ItextElement>();
	private boolean newPage = true;

	@Override
	public boolean isNewPage() {
		return newPage;
	}

	public void write(File file) {
		file.getParentFile().mkdirs();
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			write(out);
			out.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void write(OutputStream out) {
		Document document = new Document();
		PdfWriter writer;
		try {
			writer = PdfWriter.getInstance(document, out);
		} catch (DocumentException ex) {
			throw new RuntimeException(ex);
		}
		writer.setPageEvent(new PageEventHandler());
		document.setPageSize(new Rectangle(mmToPoints(pageWidth), mmToPoints(pageHeight)));
		document.setMargins(mmToPoints(marginLeft), mmToPoints(marginRight), mmToPoints(marginTop),
			mmToPoints(marginBottom));
		document.open();
		for (ItextElement element : elements) {
			try {
				if (element instanceof PageBreak) {
					document.newPage();
				} else {
					Element iTextElement = element.getITextElement();
					if (iTextElement != null) document.add(iTextElement);
				}
			} catch (DocumentException ex) {
				throw new RuntimeException(ex);
			}
		}
		document.close();
	}

	@Override
	public APdfBuilder newPage() {
		elements.add(new PageBreak(this));
		newPage = true;
		return this;
	}

	@Override
	public AParagraph paragraph() {
		Paragraph p = new Paragraph(this, fontStyle);
		elements.add(p);
		newPage = false;
		return p;
	}

	@Override
	public ATable table(float... cellWidths) {
		Table t = new Table(this, getFontStyle());
		t.setCellWidths(cellWidths);
		elements.add(t);
		newPage = false;
		return t;
	}

	@Override
	public ATable table(int columnCount) {
		Table t = new Table(this, getFontStyle());
		t.setColumnCount(columnCount);
		elements.add(t);
		newPage = false;
		return t;
	}

	@Override
	public AImage image(byte[] data) {
		Image i = new Image(this, data);
		elements.add(i);
		newPage = false;
		return i;
	}

	@Override
	public AImage image(File file) {
		Image i = new Image(this, file);
		elements.add(i);
		newPage = false;
		return i;
	}

	class PageEventHandler extends PdfPageEventHelper {

		@Override
		public void onEndPage(PdfWriter writer, Document document) {

			for (APageExtension extension : pageExtensions) {
				PageExtensionContainer container = new PageExtensionContainer(writer);
				extension.onPage(container);
				container.apply(extension);
			}

		}

	}

	class PageExtensionContainer extends APageLayer {

		private PdfWriter writer;

		private Collection<ItextElement> elements = new ArrayList<ItextElement>();

		public PageExtensionContainer(PdfWriter writer) {
			this.writer = writer;
		}

		public void apply(APageExtension extension) {
			if (elements.isEmpty()) return;

			PdfContentByte directContent = writer.getDirectContent();
			ColumnText ct = new ColumnText(directContent);

			Rectangle pageSize = writer.getPageSize();
			float pageHeight = pageSize.height();

			float y = mmToPoints(extension.getY(this));
			float height = mmToPoints(extension.getHeight(this));

			float x = mmToPoints(extension.getX(this));
			float width = mmToPoints(extension.getWidth(this));

			y = pageHeight - y;
			ct.setSimpleColumn(x, y, x + width, y - height);

			for (ItextElement element : elements) {
				Element iTextElement = element.getITextElement();
				if (iTextElement != null) ct.addElement(iTextElement);
			}

			try {
				ct.go();
			} catch (DocumentException ex) {
				throw new RuntimeException(ex);
			}
		}

		@Override
		public float getWidth() {
			return pageWidth;
		}

		@Override
		public float getHeight() {
			return pageHeight;
		}

		@Override
		public int getPageNumber() {
			return writer.getPageNumber();
		}

		@Override
		public AParagraph paragraph() {
			Paragraph p = new Paragraph(this, getFontStyle());
			elements.add(p);
			return p;
		}

		@Override
		public ATable table(float... cellWidths) {
			Table t = new Table(this, getFontStyle());
			t.setCellWidths(cellWidths);
			elements.add(t);
			return t;
		}

		@Override
		public ATable table(int columnCount) {
			Table t = new Table(this, getFontStyle());
			t.setColumnCount(columnCount);
			elements.add(t);
			return t;
		}

		@Override
		public AImage image(byte[] data) {
			Image i = new Image(this, data);
			elements.add(i);
			return i;
		}

		@Override
		public AImage image(File file) {
			Image i = new Image(this, file);
			elements.add(i);
			return i;
		}

	}

}
