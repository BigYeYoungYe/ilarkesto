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
package ilarkesto.integration.selenium;

import java.util.ArrayList;
import java.util.List;

public class SeleniumTestcaseBuilder {

	private String title = "generated-testcase";
	private String baseUrl;
	private List<Command> commands = new ArrayList<Command>();

	public SeleniumTestcaseBuilder(String baseUrl) {
		super();
		this.baseUrl = baseUrl;
	}

	// --- custom ---

	public void clickAndWaitForElementPresent(String clickTarget, String waitForTarget) {
		click(clickTarget);
		waitForElementPresent(waitForTarget);
	}

	// --- basics ---

	public Command select(String target, String optionTarget) {
		return command("select", target, optionTarget);
	}

	public Command click(String target) {
		return command("click", target);
	}

	public Command waitForElementPresent(String target) {
		return command("waitForElementPresent", target);
	}

	public Command clickAndWait(String target) {
		return command("clickAndWait", target);
	}

	public Command type(String target, String value) {
		return command("type", target, value);
	}

	public Command open(String url) {
		return command("open", url);
	}

	public Command command(String command, String target, String value) {
		return command(command, target).setValue(value);
	}

	public Command command(String command, String target) {
		return command(command).setTarget(target);
	}

	public Command command(String command) {
		Command c = new Command(command);
		commands.add(c);
		return c;
	}

	public SeleniumTestcaseBuilder setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
		return this;
	}

	public SeleniumTestcaseBuilder setTitle(String title) {
		this.title = title;
		return this;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
		sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n");
		sb.append("<head profile=\"http://selenium-ide.openqa.org/profiles/test-case\">\n");
		sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n");
		sb.append("<link rel=\"selenium.base\" href=\"").append(baseUrl).append("\" />\n");
		sb.append("<title>").append(title).append("</title>\n");
		sb.append("</head><body>\n");
		sb.append("<table cellpadding=\"1\" cellspacing=\"1\" border=\"1\">\n");
		// thead?
		sb.append("<tbody>\n");

		for (Command command : commands) {
			sb.append(command.toString());
		}

		sb.append("\n</tbody></table></body></html>\n");

		return sb.toString();
	}

	public class Command {

		private String command;
		private String target;
		private String value;

		public Command(String command) {
			super();
			this.command = command;
		}

		public Command setTarget(String target) {
			this.target = target;
			return this;
		}

		public Command setValue(String value) {
			this.value = value;
			return this;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("\n<tr>");
			sb.append("\n  <td>").append(command).append("</td>");
			sb.append("\n  <td>").append(target == null ? "" : target).append("</td>");
			sb.append("\n  <td>").append(value == null ? "" : value).append("</td>");
			sb.append("\n</tr>");
			return sb.toString();
		}
	}

}