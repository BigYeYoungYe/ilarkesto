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
package ilarkesto.integration.max.state;

public class MaxPushButtonConfiguration {

	private MaxPushButtonKeyConfiguration upperKey;
	private MaxPushButtonKeyConfiguration lowerKey;

	public static MaxPushButtonConfiguration createDummy() {
		MaxPushButtonConfiguration dummy = new MaxPushButtonConfiguration();
		dummy.upperKey = MaxPushButtonKeyConfiguration.createDummy(0);
		dummy.lowerKey = MaxPushButtonKeyConfiguration.createDummy(1);
		return dummy;
	}

	public MaxPushButtonKeyConfiguration getLowerKey() {
		return lowerKey;
	}

	public MaxPushButtonKeyConfiguration getUpperKey() {
		return upperKey;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}
