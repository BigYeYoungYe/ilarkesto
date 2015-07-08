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
package ilarkesto.core.search;

import java.util.ArrayList;
import java.util.List;

public class SearchText {

	private List<String> words;

	public SearchText(String text) {
		words = new ArrayList<String>();
		text = text.toLowerCase().trim();
		int idx = text.indexOf(' ');
		while (idx > 0) {
			words.add(text.substring(0, idx));
			text = text.substring(idx + 1).trim();
			idx = text.indexOf(' ');
		}
		words.add(text.trim());
	}

	public boolean matches(Object... values) {
		for (String word : words) {
			if (!matchesAny(word, values)) return false;
		}
		return true;
	}

	private boolean matchesAny(String word, Object[] values) {
		for (Object value : values) {
			if (value == null) continue;
			String s = value.toString().toLowerCase();
			if (s.contains(word)) return true;
		}
		return false;
	}

}
