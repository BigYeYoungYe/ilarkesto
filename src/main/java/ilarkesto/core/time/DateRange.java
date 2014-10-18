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
package ilarkesto.core.time;

import ilarkesto.core.base.Args;
import ilarkesto.core.base.Str.Formatable;

import java.io.Serializable;

public class DateRange implements Comparable<DateRange>, Serializable, Formatable {

	protected Date start;
	protected Date end;

	private transient int hashCode;

	public DateRange(String s) {
		Args.assertNotNull(s, "s");
		int separatorIdx = s.indexOf(" - ");
		if (separatorIdx < 0) throw new IllegalArgumentException("Illegal DateRange: " + s);
		start = new Date(s.substring(0, separatorIdx));
		end = new Date(s.substring(separatorIdx + 3));
		check();
	}

	public DateRange(Date start, Date end) {
		Args.assertNotNull(start, "start", end, "end");
		this.start = start;
		this.end = end;
		check();
	}

	public DateRange(java.util.Date start, java.util.Date end) {
		this(new Date(start), new Date(end));
		check();
	}

	private void check() {
		if (start.isAfter(end))
			throw new IllegalArgumentException("Illegal date range. Start is after end: " + toString());
	}

	public boolean isWholeMonth() {
		if (!isSameMonthAndYear()) return false;
		return start.isFirstDayOfMonth() && end.isLastDateOfMonth();
	}

	public boolean isSameYear() {
		return start.year == end.year;
	}

	public boolean isSameMonthAndYear() {
		return start.month == end.month && isSameYear();
	}

	public boolean isOneDay() {
		return start.equals(end);
	}

	public int getDayCount() {
		return Tm.getDaysBetweenDates(start.toJavaDate(), end.toJavaDate()) + 1;
	}

	public TimePeriod getTimePeriodBetweenStartAndEnd() {
		return start.getPeriodTo(end);
	}

	public Date getStart() {
		return start;
	}

	public Date getEnd() {
		return end;
	}

	@Override
	public String format() {
		if (isOneDay()) return start.format();
		return start.format() + " - " + end.format();
	}

	public String formatStartLongMonthYear() {
		return start.formatLongMonthYear();
	}

	public String formatShortest() {
		if (isOneDay()) return start.format();
		if (isSameMonthAndYear()) return formatStartLongMonthYear();
		return format();
	}

	@Override
	public int compareTo(DateRange o) {
		return start.compareTo(o.start);
	}

	@Override
	public String toString() {
		return start.toString() + " - " + end.toString();
	}

	@Override
	public final int hashCode() {
		if (hashCode == 0) {
			hashCode = 23;
			hashCode = hashCode * 37 + start.hashCode();
			hashCode = hashCode * 37 + end.hashCode();
		}
		return hashCode;
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof DateRange)) return false;
		return start.equals(((DateRange) obj).start) && end.equals(((DateRange) obj).end);
	}
}
