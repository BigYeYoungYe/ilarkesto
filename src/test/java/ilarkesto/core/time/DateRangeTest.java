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

import ilarkesto.testng.ATest;

import org.testng.annotations.Test;

public class DateRangeTest extends ATest {

	private static final DateRange range2014 = new DateRange("2014-01-01 - 2014-12-31");
	private static final DateRange range10 = new DateRange("2014-01-11 - 2014-01-20");

	@Test
	public void containsAny() {
		assertFalse(range2014.containsAny(new DateRange("2013-01-01 - 2013-12-31")));
		assertTrue(range2014.containsAny(range2014));
		assertTrue(range2014.containsAny(range10));
		assertTrue(range2014.containsAny(new DateRange("2014-12-31 - 2015-12-31")));
		assertFalse(range2014.containsAny(new DateRange("2015-01-01 - 2015-12-31")));
	}

	@Test
	public void getOverlappingDays() {
		assertEquals(range2014.getOverlappingDays(new DateRange("2013-01-01 - 2013-12-31")), 0);
		assertEquals(range2014.getOverlappingDays(new DateRange("2015-01-01 - 2015-12-31")), 0);
		assertEquals(range2014.getOverlappingDays(new DateRange("2013-01-01 - 2014-01-01")), 1);
		assertEquals(range2014.getOverlappingDays(new DateRange("2014-12-31 - 2015-12-31")), 1);
		assertEquals(range2014.getOverlappingDays(new DateRange("2014-12-31 - 2014-12-31")), 1);
		assertEquals(range2014.getOverlappingDays(new DateRange("2014-06-01 - 2014-06-31")), 31);

		assertEquals(range10.getOverlappingDays(range10), 10);
	}

	@Test
	public void getOverlappingDaysAsPartial() {
		assertEquals(range10.getOverlappingDaysAsPartial(new DateRange("2014-01-01 - 2014-01-10")), 0, 0);
		assertEquals(range10.getOverlappingDaysAsPartial(new DateRange("2014-01-21 - 2014-01-31")), 0, 0);
		assertEquals(range10.getOverlappingDaysAsPartial(new DateRange("2014-01-01 - 2014-01-11")), 0.1, 0);
		assertEquals(range10.getOverlappingDaysAsPartial(new DateRange("2014-01-20 - 2014-12-31")), 0.1, 0);
		assertEquals(range10.getOverlappingDaysAsPartial(new DateRange("2014-01-11 - 2014-01-20")), 1, 0);
		assertEquals(range10.getOverlappingDaysAsPartial(new DateRange("2014-01-12 - 2014-01-19")), 0.8, 0);
	}

	@Test
	public void contains() {
		assertFalse(range2014.contains(new Date("2013-12-31")));
		assertTrue(range2014.contains(new Date("2014-01-01")));
		assertTrue(range2014.contains(new Date("2014-12-31")));
		assertFalse(range2014.contains(new Date("2015-01-01")));
	}

	@Test
	public void getDayCount() {
		assertEquals(new DateRange("2014-01-01 - 2014-01-01").getDayCount(), 1);
		assertEquals(new DateRange("2014-01-01 - 2014-01-02").getDayCount(), 2);
	}

	@Test
	public void getTimePeriodBetweenStartAndEnd() {
		assertEquals(new DateRange("2014-01-01 - 2014-01-01").getTimePeriodBetweenStartAndEnd(), new TimePeriod(0));
		assertEquals(new DateRange("2014-01-01 - 2014-01-02").getTimePeriodBetweenStartAndEnd(), new TimePeriod(
				Tm.HOUR * 24));
	}

	@Test
	public void constructionAndToString() {
		assertEquals(new DateRange(new Date(2014, 1, 1), new Date(2015, 1, 1)).toString(), new DateRange(
				"2014-01-01 - 2015-01-01").toString());
	}

	@Test
	public void checkFail() {
		assertCheckFail("2000-01-02 - 2000-01-01");
		assertCheckFail("2000-01-01");
	}

	@Test
	public void checkOk() {
		new DateRange("2000-01-01 - 2000-01-01");
	}

	private void assertCheckFail(String s) {
		try {
			new DateRange(s);
			fail("Exception expected: " + s);
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

}
