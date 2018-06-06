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
package ilarkesto.tools.his.zeiterfassung;

import ilarkesto.base.Sys;
import ilarkesto.core.base.Parser.ParseException;
import ilarkesto.core.base.Str;
import ilarkesto.core.localization.Localizer;
import ilarkesto.core.time.Date;
import ilarkesto.io.CsvWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Zeiterfassung {

	private static final Date BEGIN_2018_06 = new Date(2017, 11, 16);
	private static final Date BEGIN_2018_12 = new Date(2018, 5, 21);

	private static List<DayAtWork> days = new ArrayList<DayAtWork>();
	private static DayAtWork currentDay;
	private static boolean hiszillaLineReached;

	public static void main(String[] args) throws Exception {
		Localizer.setCurrent(Localizer.DE);
		parse(new File(Sys.getUsersHomePath() + "/myfiles/his/zeiterfassung/zeiterfassung.txt"));
		Collections.sort(days);
		determineAchievoIds();
		writeCsv(new File(Sys.getUsersHomePath() + "/myfiles/his/zeiterfassung/zeiterfassung.csv"));
		System.out.println("-------------------------------------------------------------------------------");
		printForOrganizanto();
		System.out.println("-------------------------------------------------------------------------------");
		printForHiszilla();
	}

	private static void printForOrganizanto() {
		for (DayAtWork day : days) {
			if (day.isAlreadyBookedInHiszilla()) continue;
			Date date = day.getDate();
			System.out.println(date.getWeekday().toShortString("en") + ", " + date.formatDayMonthYear() + " | "
					+ day.getStart().format() + " - " + day.getEnd().format() + " | "
					+ day.getWorkTime().toHoursAndMinutes() + " | " + day.getPauseTime().toMinutes() + " | "
					+ day.getActivitiesText());
		}
	}

	private static void printForHiszilla() {
		Map<String, Long> minutesByHiszillaId = new HashMap<String, Long>();

		for (DayAtWork day : days) {
			for (WorkActivity activity : day.getActivities()) {
				if (activity.isAlreadyBookedInHiszilla()) continue;
				String hiszillaId = activity.getHiszillaId();
				if (hiszillaId == null) continue;
				long time = activity.getWorktimeInMinutes();
				Long total = minutesByHiszillaId.get(hiszillaId);
				if (total == null) {
					total = time;
				} else {
					total = total.longValue() + time;
				}
				minutesByHiszillaId.put(hiszillaId, total);
			}
		}

		for (Entry<String, Long> entry : minutesByHiszillaId.entrySet()) {
			Long minutes = entry.getValue();
			String hiszillaId = entry.getKey();
			String time = String.valueOf(minutes.floatValue() / 60f);
			System.out.println(
				time + " | " + hiszillaId + " | https://hiszilla.his.de/hiszilla/show_bug.cgi?id=" + hiszillaId);
		}
	}

	private static void writeCsv(File file) throws IOException {
		FileWriter out = new FileWriter(file);
		CsvWriter csv = new CsvWriter(out);
		int currentWeek = 0;
		long totalMinutes = 0;
		for (DayAtWork day : days) {
			Date date = day.getDate();
			if (date.isFirstDayOfMonth()) totalMinutes = 0;
			if (!isPreviousMonth(date)) continue;

			if (currentWeek != date.getWeek()) {
				currentWeek = date.getWeek();
				csv.closeRecord();
				csv.writeField("KW " + currentWeek);
				csv.closeRecord();
			}

			List<WorkActivity> activities = day.getActivities();
			int i = 0;
			int count = activities.size();
			for (WorkActivity activity : activities) {
				i++;
				if (!activity.isBookingRequired()) continue;
				activity.appendTo(csv);
				if (i == count) {
					csv.writeField(day.getWorkTime().toHoursAndMinutes());
					totalMinutes += day.getWorkTime().toMinutes();
					csv.writeField(new BigDecimal(totalMinutes / 60f / 8f).setScale(2, BigDecimal.ROUND_HALF_UP));
				}
				csv.closeRecord();
			}

		}
		out.close();
	}

	private static boolean isPreviousMonth(Date date) {
		if (Date.today().getFirstDateOfMonth().equals(date.getFirstDateOfMonth())) return true;
		if (Date.today().getFirstDateOfMonth().addDays(-1).getFirstDateOfMonth().equals(date.getFirstDateOfMonth()))
			return true;
		return false;
	}

	private static void determineAchievoIds() {
		for (DayAtWork day : days) {
			for (WorkActivity activity : day.getActivities()) {
				if (!activity.isBookingRequired()) continue;

				String phaseId = activity.getAchievoPhaseId();
				if (phaseId == null) {
					String hiszillaId = activity.getHiszillaId();
					if (hiszillaId != null) {
						phaseId = determineAchievoPhaseIdByHiszillaId(hiszillaId, activity.getText());
					} else {
						phaseId = determineAchievoPhaseIdByText(activity.getText(), day);
					}
				}
				if (day.getDate().getPeriodToToday().toDays() < 60 && phaseId == null)
					throw new RuntimeException("Missing $-Id in " + day + " " + activity);
				if (phaseId != null) {
					phaseId = convertAchievoPhaseId(phaseId, activity, day);
				}
				activity.setAchievoPhaseId(phaseId);

				activity.setAchievoPackageId(determineAchievoPackageId(activity, day));
				activity.setAchievoProjectId(determineAchievoProjectId(activity, day));
			}
		}
	}

	private static String convertAchievoPhaseId(String id, WorkActivity activity, DayAtWork day) {
		if (day.getDate().isSameOrAfter(BEGIN_2018_12)) {
			if (id.equals("FDU")) return "36036";
			if (id.equals("ERW")) return "36030";
			if (id.equals("QS")) return "36033";
		}

		if (day.getDate().isSameOrAfter(BEGIN_2018_06)) {
			if (id.equals("FDU")) return "33181";
			if (id.equals("ERW")) return "33093";
			if (id.equals("QS")) return "33095";
		}

		if (id.equals("FDU")) return "32056";
		if (id.equals("ERW")) return "32052";
		if (id.equals("QS")) return "32054";
		if (id.equals("QS-17.12")) return "32054";
		if (id.equals("QS-17.06")) return "32053";
		return id;
	}

	private static String determineAchievoProjectId(WorkActivity activity, DayAtWork day) {
		// Projekt Nr

		String text = activity.getText();

		if (day.getDate().isSameOrAfter(BEGIN_2018_06)) {
			if ("Sprint Retrospektive".equals(text)) return "P8535";
		}

		if ("Sprint Retrospektive".equals(text)) return "8444";
		if ("29170".equals(activity.getAchievoPhaseId())) return "8444";

		return "P5461";
	}

	private static String determineAchievoPackageId(WorkActivity activity, DayAtWork day) {
		// Aufgabenpaket Nr

		String text = activity.getText();

		if (day.getDate().isSameOrAfter(BEGIN_2018_06)) {
			if ("Sprint Retrospektive".equals(text)) return "AP11211";
		}

		if ("Sprint Retrospektive".equals(text)) return "10577";
		if ("29170".equals(activity.getAchievoPhaseId())) return "10577";

		if (day.getDate().isBefore(new Date(2017, 7, 24))) return "10282";

		if (day.getDate().isBefore(BEGIN_2018_12)) return "11025";

		return "AP11370";
	}

	private static String determineAchievoPhaseIdByText(String text, DayAtWork day) {
		// Aufgabe Nr

		if (day.getDate().isSameOrAfter(BEGIN_2018_06)) {
			if ("Sprint Retrospektive".equals(text)) return "31890";
		}

		if ("Sprint Retrospektive".equals(text)) return "29170";
		if ("Sprint Planning".equals(text)) return "ERW";
		if ("Sprint Review".equals(text)) return "ERW";
		if ("Daily Scrum".equals(text)) return "ERW";
		if ("Product Backlog Refinement".equals(text)) return "FDU";

		if (!hiszillaLineReached) System.out.println("Achievo-ID fehlt. Text: " + text);
		return null;
	}

	private static String determineAchievoPhaseIdByHiszillaId(String hiszillaId, String text) {
		if (hiszillaId == null) return null;
		for (DayAtWork day : days) {
			for (WorkActivity activity : day.getActivities()) {
				if (activity.getAchievoPhaseId() == null) continue;
				if (hiszillaId.equals(activity.getHiszillaId())) return activity.getAchievoPhaseId();
			}
		}
		if (!hiszillaLineReached) System.out.println("Achievo-ID fehlt. Hiszilla: " + hiszillaId + " Text: " + text);
		return null;
	}

	private static void parse(File file) throws IOException, ParseException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line = null;
		int lineCount = 0;
		while ((line = in.readLine()) != null) {
			lineCount++;
			if (Str.isBlank(line)) continue;
			line = line.trim();
			if (line.startsWith("//")) continue;
			if (line.startsWith("#")) continue;
			if (line.startsWith("---")) {
				hiszillaLineReached = true;
				continue;
			}
			try {
				if (line.startsWith("* ")) {
					currentDay.addActivity(line).setAlreadyBookedInHiszilla(hiszillaLineReached);
					continue;
				}
				currentDay = new DayAtWork(line);
				days.add(currentDay);
			} catch (Exception ex) {
				throw new RuntimeException("Parsing failed at line " + lineCount, ex);
			}
		}
		in.close();
	}

}
