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
package ilarkesto.testng;

import ilarkesto.core.base.OperationObserver;
import ilarkesto.core.logging.Log;
import ilarkesto.core.time.Date;
import ilarkesto.io.AFileStorage;
import ilarkesto.io.SimpleFileStorage;

import java.io.File;
import java.util.Collection;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;

public class ATest extends Assert {

	public static void main(String[] args) {
		System.out.println(new Date(2100, 12, 31).toMillis());
	}

	public static final String OUTPUT_DIR = "test-output";
	public static final String INPUT_DIR = "test-input";

	protected Log log = Log.get(getClass());
	protected static final OperationObserver observer = OperationObserver.DUMMY;

	@BeforeSuite
	public void enableDebugLogging() {
		Log.setDebugEnabled(true);
	}

	// --- files ---

	protected File getTestOutputFile(String name) {
		return new File(OUTPUT_DIR + "/" + getClass().getSimpleName() + "/" + name);
	}

	protected AFileStorage getTestFileStorage(String name) {
		return new SimpleFileStorage(getTestOutputFile(name));
	}

	// --- asserts ---

	public static void assertInstanceOf(Object object, Class type) {
		assertNotNull(object);
		assertTrue(type.isAssignableFrom(object.getClass()), "object class " + object.getClass().getName()
				+ " is not instance of " + type.getName());
	}

	public static void failExceptionExpected(Class<? extends Throwable> exceptionType) {
		fail(exceptionType.getSimpleName() + " expected");
	}

	public static void assertNotEquals(Object a, Object b) {
		assertFalse(a.equals(b), "Objects expected not to be equal: <" + a + "> and <" + b + ">");
	}

	public static void assertEmpty(Collection collection) {
		assertNotNull(collection);
		assertTrue(collection.isEmpty(), "Collection expected not be empty, but is not: <" + collection + ">.");
	}

	public static void assertSize(Collection collection, int expectedSize) {
		assertNotNull(collection, "Collection expected to be not null");
		assertEquals(collection.size(), expectedSize, "Collection size expected to be <" + expectedSize + ">, but is <"
				+ collection.size() + ">: <" + collection + ">");
	}

	public static void assertContainsNot(String string, String substring) {
		assertNotNull(string);
		assertTrue(!string.contains(substring), "<" + string + "> expected not to contain <" + substring
				+ ">, but it does.");
	}

	public static void assertContains(String string, String substring) {
		assertNotNull(string);
		assertTrue(string.contains(substring), "<" + string + "> expected to contain <" + substring + ">.");
	}

	public static <T> void assertContains(Collection<T> collection, T element) {
		assertTrue(collection.contains(element), "Collection expected to contain <" + element + ">.");
	}

	public static <T> void assertContainsNot(Collection<T> collection, T element) {
		assertFalse(collection.contains(element), "Collection expected not to contain <" + element + ">.");
	}

	public static void assertNotEmpty(Collection collection) {
		assertFalse(collection.isEmpty(), "Collection expected to be not empty, but it is.");
	}

	public static void assertStartsWith(String actual, String expectedPrefix) {
		assertTrue(actual.startsWith(expectedPrefix), "<" + actual + "> expected to start with <" + expectedPrefix
				+ "> |");
	}

}
