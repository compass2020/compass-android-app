/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.location;


import android.location.Location;
import android.util.Pair;

import java.util.ArrayList;

import at.univie.compass.global.Global;

public abstract class LiveLocationFilter {
    public String name;
    public ArrayList<Pair<float[], Long>> currentAccelerationValues;

    LiveLocationFilter(String name) {
        this.name = name;
        currentAccelerationValues = new ArrayList<>();
    }

    abstract Location initializeFilter(Location firstLocation);

    abstract Location filterLocation(Location currentLocation, Location previousLocation);

    void addAccelerationValue(float[] values, long timestamp) {
        currentAccelerationValues.add(new Pair<>(values, timestamp));
    }
}

class NoOpFilter extends LiveLocationFilter {
    NoOpFilter(String name) {
        super(name);
    }

    @Override
    Location initializeFilter(Location firstLocation) {
        return firstLocation;
    }

    @Override
    Location filterLocation(Location currentLocation, Location previousLocation) {
        return currentLocation;
    }
}

class SimpleKalmanLike extends LiveLocationFilter {
    Location lastGoodLocation, lastPredictedLocation;
    double lastGoodEstimatedVerticalSpeed;
    int numConsecutiveOffPredictions;
    boolean onlyImu;

    private double requiredAccuracyInMeters;
    private double maxBadDurationInSeconds;
    private double allowedKalmanDeviationInMetersPerSecond;
    SimpleKalmanLike(String name, double requiredAccuracyInMeters, double allowedKalmanDeviationInMetersPerSecond, double maxBadDurationInSeconds, boolean onlyImu) {
        super(name);
        this.requiredAccuracyInMeters = requiredAccuracyInMeters;
        this.allowedKalmanDeviationInMetersPerSecond = allowedKalmanDeviationInMetersPerSecond;
        this.maxBadDurationInSeconds = maxBadDurationInSeconds;
        this.onlyImu = onlyImu;
    }

    @Override
    Location initializeFilter(Location firstLocation) {
        lastGoodLocation = firstLocation;
        lastPredictedLocation = lastGoodLocation;
        lastGoodEstimatedVerticalSpeed = 0.0;
        return firstLocation;
    }

    @Override
    Location filterLocation(Location currentLocation, Location previousLocation) {
        // ultra-simpler Kalman filter (reicht vllt schon)
        double dt_since_last_good_location_in_seconds = (currentLocation.getElapsedRealtimeNanos() - lastGoodLocation.getElapsedRealtimeNanos()) / 1000000000.0;

        double dir_lon = Math.sin(lastGoodLocation.getBearing());
        double dir_lat = Math.cos(lastGoodLocation.getBearing());

        double dLon = 0.0;
        double dLat = 0.0;
        double dAlt = 0.0;
        double dSpeed = 0.0;

        // iterate over all accelerations and sum them. Maybe better to use some filtering
        for (int i = 0; i < currentAccelerationValues.size() - 1; i++) {
            Pair<float[], Long> element = currentAccelerationValues.get(i);
            Pair<float[], Long> nextElement = currentAccelerationValues.get(i+1);
            double aEast = (element.first[0] + nextElement.first[0]) / 2.0;  // m/s^2 (?)
            double aNorth = (element.first[1] + nextElement.first[1]) / 2.0;  // m/s^2 (?)
            double aUp = (element.first[2] + nextElement.first[2]) / 2.0;
            double dt_in_seconds = (nextElement.second - element.second) / 1000000000.0;
            dLon += Global.meters_to_longitude(lastPredictedLocation.getSpeed() * dir_lon * dt_in_seconds + 0.5 * aEast * dir_lon * dt_in_seconds*dt_in_seconds, lastPredictedLocation.getLatitude());
            dLat += Global.meters_to_latitude(lastPredictedLocation.getSpeed() * dir_lat * dt_in_seconds + 0.5 * aNorth * dir_lon * dt_in_seconds*dt_in_seconds);
            dAlt += lastGoodEstimatedVerticalSpeed * dt_in_seconds + 0.5 * aUp * dt_in_seconds*dt_in_seconds;
            dSpeed += aEast * dir_lon * dt_in_seconds + aNorth * dir_lon * dt_in_seconds;
        }
        currentAccelerationValues = new ArrayList<Pair<float[], Long>>();  // reset acceleration values each time

        double predicted_lon = lastGoodLocation.getLongitude() + dLon;
        double predicted_lat = lastGoodLocation.getLatitude() + dLat;
        double predicted_alt = lastGoodLocation.getAltitude() + dAlt;
        double predicted_speed = java.lang.Math.abs(lastGoodLocation.getSpeed() + dSpeed);

        Location predictedLocation = new Location("");
        predictedLocation.setLatitude(predicted_lat);
        predictedLocation.setLongitude(predicted_lon);
        predictedLocation.setAltitude(predicted_alt);
        predictedLocation.setSpeed((float) predicted_speed);
        predictedLocation.setTime(currentLocation.getTime());
        predictedLocation.setElapsedRealtimeNanos(currentLocation.getElapsedRealtimeNanos());
        double predictedGroundDeltaInMeters = predictedLocation.distanceTo(currentLocation);
        double predictedHeightDeltaInMeters = predictedLocation.getAltitude() - currentLocation.getAltitude();
        double predictedDistance = Math.sqrt(predictedGroundDeltaInMeters * predictedGroundDeltaInMeters + predictedHeightDeltaInMeters * predictedHeightDeltaInMeters);

        if (onlyImu) {
            lastPredictedLocation = predictedLocation;
            return predictedLocation;
        }

        if (currentLocation.getAccuracy() > requiredAccuracyInMeters) {
            // Location is bad due to bad accuracy
            lastPredictedLocation = predictedLocation;
            return predictedLocation;
        }

        double kalman_deviation = predictedDistance / dt_since_last_good_location_in_seconds;
        if (kalman_deviation > allowedKalmanDeviationInMetersPerSecond) {
            numConsecutiveOffPredictions += 1;
            if (dt_since_last_good_location_in_seconds < maxBadDurationInSeconds) {
                // Location is bad because it is far off from the Kalman prediction
                lastGoodEstimatedVerticalSpeed = (predictedLocation.getAltitude() - lastGoodLocation.getAltitude()) / (predictedLocation.getTime() - lastGoodLocation.getTime()) * 1000;
                lastPredictedLocation = predictedLocation;
                return predictedLocation;
            } else {
                // The last few locations were discarded but they seem to have been good. This should only be possible at the beginning.
                numConsecutiveOffPredictions = 0;
                lastGoodEstimatedVerticalSpeed = (currentLocation.getAltitude() - lastGoodLocation.getAltitude()) / (currentLocation.getTime() - lastGoodLocation.getTime()) * 1000;
                lastGoodLocation = currentLocation;
                lastPredictedLocation = currentLocation;
                return currentLocation;
            }
        }
        numConsecutiveOffPredictions = 0;
        lastGoodEstimatedVerticalSpeed = (currentLocation.getAltitude() - lastGoodLocation.getAltitude()) / (currentLocation.getTime() - lastGoodLocation.getTime()) * 1000;
        lastGoodLocation = currentLocation;
        lastPredictedLocation = currentLocation;
        return currentLocation;
    }
}
