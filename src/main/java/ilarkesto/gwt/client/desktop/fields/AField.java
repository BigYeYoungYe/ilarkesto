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
package ilarkesto.gwt.client.desktop.fields;

import ilarkesto.core.base.Str;
import ilarkesto.core.logging.Log;
import ilarkesto.gwt.client.Updatable;
import ilarkesto.gwt.client.desktop.Widgets;

import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public abstract class AField implements Updatable {

	protected Log log = Log.get(getClass());

	private FocusPanel focusPanel;
	private IsWidget valueWidget;
	private String warning;

	public abstract String getLabel();

	public abstract IsWidget createDisplayWidget();

	public String getMilestoneLabel() {
		return getLabel();
	}

	public String getTooltip() {
		return null;
	}

	public String getWarning() {
		return warning;
	}

	public AField setWarning(String warning) {
		this.warning = warning;
		return this;
	}

	public AField setWarningIf(boolean condition, String warning) {
		if (!condition) return this;
		return setWarning(warning);
	}

	public Widget getWidget() {
		if (focusPanel == null) {
			focusPanel = new FocusPanel();
			focusPanel.getElement().setId(getId());
			update();
		}
		return focusPanel;
	}

	protected String getId() {
		return Str.getSimpleName(getClass()).replace('$', '_');
	}

	protected void initFocusPanel(FocusPanel focusPanel) {
		Widget label;
		try {
			label = createLabelWidget();
		} catch (Exception ex) {
			throw new RuntimeException(Str.getSimpleName(getClass()) + ".createLabelWidget() failed.", ex);
		}

		try {
			valueWidget = createDisplayWidget();
		} catch (Exception ex) {
			throw new RuntimeException(Str.getSimpleName(getClass()) + ".createDisplayWidget() failed.", ex);
		}

		Widget warningWidget;
		try {
			warningWidget = createWarningWidget();
		} catch (Exception ex) {
			throw new RuntimeException(Str.getSimpleName(getClass()) + ".createWarningWidget() failed.", ex);
		}

		FlowPanel vertical = new FlowPanel();
		if (label != null) vertical.add(label);
		if (valueWidget != null) vertical.add(valueWidget);
		if (warningWidget != null) vertical.add(warningWidget);

		focusPanel.setWidget(vertical);
		focusPanel.setTitle(getTooltip());
		focusPanel.getElement().getStyle().setPadding(Widgets.defaultSpacing, Unit.PX);
		focusPanel.setStyleName("goon-FieldEditor");
		focusPanel.getElement().getStyle().setHeight(100, Unit.PCT);
		focusPanel.getElement().getStyle().setColor(getDisplayValueColor());
		focusPanel.getElement().setAttribute("display", "table-cell");
	}

	protected String getDisplayValueColor() {
		return "#777";
	}

	private Widget createWarningWidget() {
		String warning = getWarning();
		if (warning == null) return null;
		Label warningLabel = Widgets.textWarning(warning);
		warningLabel.getElement().getStyle().setFontSize(80, Unit.PCT);
		if (isLabelAlignRight()) warningLabel.getElement().getStyle().setTextAlign(TextAlign.RIGHT);
		return warningLabel;
	}

	public Widget createLabelWidget() {
		String labelText = getLabel();
		if (labelText == null) labelText = "";
		Widget label = createLabelWidget(labelText);

		String href = getHref();
		if (href == null) return label;

		Widget button = Widgets.gotoHrefButton(href, true, getHrefIcon());
		button.getElement().getStyle().setFloat(Float.RIGHT);
		button.getElement().getStyle().setMarginTop(-Widgets.defaultSpacing, Unit.PX);

		FlowPanel panel = new FlowPanel();
		panel.add(label);
		panel.add(button);
		return panel;
	}

	protected Widget createLabelWidget(String labelText) {
		Widget label = Widgets.textFieldlabel(labelText, isLabelMandatoryMarkerActive());
		if (isLabelAlignRight()) label.getElement().getStyle().setTextAlign(TextAlign.RIGHT);
		return label;
	}

	protected boolean isLabelMandatoryMarkerActive() {
		return false;
	}

	protected String getHrefIcon() {
		return null;
	}

	protected String getHref() {
		return null;
	}

	protected boolean isLabelAlignRight() {
		return false;
	}

	protected boolean isLabelImportant() {
		return false;
	}

	@Override
	public final AField update() {
		// if (valueWidget != null) valueWidget.setText(getValue());
		if (focusPanel == null) return this;
		focusPanel.clear();
		try {
			initFocusPanel(focusPanel);
		} catch (Exception ex) {
			throw new RuntimeException(Str.getSimpleName(getClass()) + ".initFocusPanel() failed.", ex);
		}
		return this;
	}

}
