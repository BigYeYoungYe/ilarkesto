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

import java.util.ArrayList;
import java.util.List;

public class MaxHouse {

	private int id;
	private List<MaxDevice> devices;

	public static MaxHouse createDummy() {
		MaxHouse dummy = new MaxHouse();
		dummy.id = 23;
		dummy.devices = new ArrayList<MaxDevice>();
		dummy.devices.add(MaxDevice.createDummyForHouse());
		return dummy;
	}

	public List<MaxDevice> getDevicesWithLowBattery() {
		List<MaxDevice> ret = new ArrayList<MaxDevice>();
		for (MaxDevice device : getDevices()) {
			if (device.getState().isBatteryLow()) ret.add(device);
		}
		return ret;
	}

	public List<MaxDevice> getDevicesWithError(boolean ignorePushButon) {
		List<MaxDevice> ret = new ArrayList<MaxDevice>();
		for (MaxDevice device : getDevices()) {
			if (ignorePushButon && device.isDeviceTypePushButton()) continue;
			if (!device.isRadioOk() || !device.isStateInfoValid()) ret.add(device);
		}
		return ret;
	}

	public List<MaxDevice> getDevicesWithDeviceStateInvalidError(boolean ignorePushButon) {
		List<MaxDevice> ret = new ArrayList<MaxDevice>();
		for (MaxDevice device : getDevices()) {
			if (ignorePushButon && device.isDeviceTypePushButton()) continue;
			if (!device.isStateInfoValid()) ret.add(device);
		}
		return ret;
	}

	public int getId() {
		return id;
	}

	public List<MaxDevice> getDevices() {
		return devices;
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}

}
