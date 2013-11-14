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

import ilarkesto.core.time.Tm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MaxCubeState {

	private MaxCubeLastPing cubeLastPing;
	private boolean daylightSaving;
	private int firmwareVersion;
	private MaxHouse house;
	private MaxPushButtonConfiguration pushButtonConfiguration;
	private List<MaxRoom> rooms;
	private int NTPCounter;
	private long stateCubeTime;
	private String cubeDate;
	private Integer rfAddress;
	private String serialNumber;

	public static MaxCubeState createDummy() {
		MaxCubeState dummy = new MaxCubeState();
		dummy.cubeLastPing = MaxCubeLastPing.createDummy();
		dummy.daylightSaving = false;
		dummy.firmwareVersion = 7;
		dummy.house = MaxHouse.createDummy();
		dummy.pushButtonConfiguration = MaxPushButtonConfiguration.createDummy();
		dummy.rooms = new ArrayList<MaxRoom>();
		for (int i = 0; i < 5; i++) {
			dummy.rooms.add(MaxRoom.createDummy(i));
		}
		dummy.NTPCounter = 1;
		dummy.stateCubeTime = System.currentTimeMillis() - 123456;
		dummy.cubeDate = "2011-01-02";
		return dummy;
	}

	public boolean isInSync() {
		long lastPingTime = getCubeLastPing().getDate().getTime();
		long lastPingAge = System.currentTimeMillis() - lastPingTime;
		return lastPingAge < (Tm.MINUTE * 5);
	}

	public List<MaxDevice> getAllDevicesWithTransmitError() {
		List<MaxDevice> ret = new ArrayList<MaxDevice>();
		ret.addAll(getHouse().getDevicesWithTransmitError());
		for (MaxRoom room : getRooms()) {
			ret.addAll(room.getDevicesWithTransmitError());
		}
		return ret;
	}

	public List<MaxDevice> getAllDevicesWithLowBattery() {
		List<MaxDevice> ret = new ArrayList<MaxDevice>();
		ret.addAll(getHouse().getDevicesWithLowBattery());
		for (MaxRoom room : getRooms()) {
			ret.addAll(room.getDevicesWithLowBattery());
		}
		return ret;
	}

	public List<MaxDevice> getAllDevices() {
		List<MaxDevice> ret = new ArrayList<MaxDevice>();
		ret.addAll(getHouse().getDevices());
		for (MaxRoom room : getRooms()) {
			ret.addAll(room.getDevices());
		}
		return ret;
	}

	public List<MaxRoom> getRoomsWithOpenWindow() {
		List<MaxRoom> ret = new ArrayList<MaxRoom>();
		for (MaxRoom room : getRooms()) {
			if (room.isWindowOpen()) ret.add(room);
		}
		return ret;
	}

	public List<String> getRoomsWithOpenWindowAsRoomNames() {
		List<MaxRoom> rooms = getRoomsWithOpenWindow();
		List<String> ret = new ArrayList<String>(rooms.size());
		for (MaxRoom room : rooms) {
			ret.add(room.getName());
		}
		return ret;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public Integer getRfAddress() {
		return rfAddress;
	}

	public String getCubeDate() {
		return cubeDate;
	}

	public long getStateCubeTime() {
		return stateCubeTime;
	}

	public int getNTPCounter() {
		return NTPCounter;
	}

	public MaxCubeLastPing getCubeLastPing() {
		return cubeLastPing;
	}

	public boolean isDaylightSaving() {
		return daylightSaving;
	}

	public int getFirmwareVersion() {
		return firmwareVersion;
	}

	public MaxHouse getHouse() {
		return house;
	}

	public MaxPushButtonConfiguration getPushButtonConfiguration() {
		return pushButtonConfiguration;
	}

	public List<MaxRoom> getRooms() {
		return rooms;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(firmwareVersion);
		if (daylightSaving) sb.append(" daylight-saving");
		return sb.toString();
	}

	public void wire() {
		for (MaxRoom room : getRooms()) {
			room.wire(this);
		}
	}

	public float getDefaultEcoTemperature() {
		List<MaxRoom> rooms = getRooms();
		if (rooms.isEmpty()) return 17;
		return rooms.iterator().next().getEcoTemperature();
	}

	public float getDefaultComportTemperature() {
		List<MaxRoom> rooms = getRooms();
		if (rooms.isEmpty()) return 22;
		return rooms.iterator().next().getComfortTemperature();
	}

	public List<Float> getSettableTemperatures() {
		Set<Float> all = new HashSet<Float>();
		for (MaxRoom room : getRooms()) {
			all.addAll(room.getSettableTemperatures());
		}
		List<Float> ret = new ArrayList<Float>(all);
		Collections.sort(ret);
		return ret;
	}

}
