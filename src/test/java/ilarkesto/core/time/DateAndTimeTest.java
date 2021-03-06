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
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package ilarkesto.core.time;

import ilarkesto.testng.ATest;

import org.testng.annotations.Test;

public class DateAndTimeTest extends ATest {

	@Test
	public void constructionAndToString() {
		assertEquals(new DateAndTime(2010, 1, 1, 10, 9, 0).toString(), new DateAndTime("2010-01-01 10:09").toString());
	}

	@Test
	public void isAfter() {
		assertTrue(new DateAndTime(2010, 1, 1, 10, 10, 0).isAfter(new DateAndTime(2010, 1, 1, 10, 9, 0)));
		assertFalse(new DateAndTime(2010, 1, 1, 10, 10, 0).isAfter(new DateAndTime(2010, 1, 1, 10, 10, 0)));
	}

	@Test
	public void isBefore() {
		assertTrue(new DateAndTime(2010, 1, 1, 10, 10, 0).isBefore(new DateAndTime(2010, 1, 1, 10, 11, 0)));
		assertFalse(new DateAndTime(2010, 1, 1, 10, 10, 0).isBefore(new DateAndTime(2010, 1, 1, 10, 10, 0)));
	}

	@Test
	public void test24() {
		Time t24 = new Time(24, 0);
		assertTrue(t24.isAfter(new Time(10, 9)));

		assertEquals(new DateAndTime(1979, 8, 2, 24, 0, 0), new DateAndTime(1979, 8, 3, 0, 0, 0));
		assertEquals(new DateAndTime(1979, 8, 2, 24, 0, 0).compareTo(new DateAndTime(1979, 8, 3, 0, 0, 0)), 0);
	}
}
