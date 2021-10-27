/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.global;

import android.location.Location;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class GPXWaypointAnnotations implements Serializable {
    public String timestamp;
    public String time;
    public double accuracy;
    public double bearing;
    public String provider;
    public List<String> extrasValues;
    public List<String> extrasKeys;
    public String cpReached;

    public GPXWaypointAnnotations(Location location, String cpReached, Date timestamp) {
        this.timestamp = Global.dateToString(timestamp);
        time = Global.locationToTimeString(location);
        accuracy = location.getAccuracy();
        bearing = location.getBearing();
        provider = location.getProvider();
        extrasValues = new ArrayList<>();
        extrasKeys = new ArrayList<>();
        this.cpReached = cpReached;

        if (location.getExtras() != null) {
            for (String key : location.getExtras().keySet()) {
                Object value = location.getExtras().get(key);
                if (value != null) {
                    extrasKeys.add(key);
                    extrasValues.add(value.toString());
                }
            }
        }
    }
}
