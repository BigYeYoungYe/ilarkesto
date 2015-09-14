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
package ilarkesto.io;

import ilarkesto.testng.ATest;

import org.apache.tools.ant.filters.StringInputStream;
import org.testng.annotations.Test;

public class IOTest extends ATest {

	@Test
	public void stringInputStream() {
		assertEquals(IO.readToString(new StringInputStream("täst", IO.UTF_8), IO.UTF_8), "täst");
		assertEquals(IO.readToString(new StringInputStream("täst", IO.ISO_LATIN_1), IO.ISO_LATIN_1), "täst");
	}
}
