/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.location;


import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.univie.compass.global.Global;
import at.univie.compass.R;
import at.univie.compass.activity.RunActivity;

public class LocationUpdateService extends Service implements SensorEventListener { //, LocationListener {
    public static WeakReference<RunActivity> runActivityWeakReference;

    // GPS constants
    private final int GPS_REQUEST_INTERVAL_IN_MILLIS = 0; // in milliseconds, 0 = as fast as possible, in practice the update interval seems to be limited by the GPS hardware to 1s
    Location previousLocation = null;

    // Filters
    public static final LiveLocationFilter bestFilter = new SimpleKalmanLike("simple-kalman-like(acc-35, dev-5, dur-25)", 35.0, 5.0, 25.0, false);
    static final List<LiveLocationFilter> testFilters = Arrays.asList(
            bestFilter,  // bestFilter must be in testFilters!
            new NoOpFilter("none"),
            new SimpleKalmanLike("simple-kalman-like(only-imu)", 35.0, 2.0, 25.0, true)
    );
    public static final List<LiveLocationFilter> usedFilters = Global.UPLOAD_INTERMEDIATE_GPX_RESULTS ? testFilters : Collections.singletonList(bestFilter);  // depends on debug vs release mode

    // GPS
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationClient;

    // IMU sensors
    SensorManager sensorManager;
    Sensor sensorAccelerometer;
    Sensor sensorGravity;
    Sensor sensorMagneticField;
    private float[] gravityValues = null;
    private float[] magneticValues = null;

    // Notification
    private NotificationManager notificationManager;
    static final int NOTIFICATION_ID = 222;
    static final String NOTIFICATION_CHANNEL_ID = "at.univie.compass";
    static final String NOTIFICATION_CHANNEL_NAME = "Compass";

    // Binder given to clients
    private final IBinder binder = new LocalBinder();
    // Registered callbacks
    private ServiceCallbacks serviceCallbacks;

    class CustomLocationCallback extends LocationCallback {
        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability) {
            if (runActivityWeakReference == null || runActivityWeakReference.get() == null || runActivityWeakReference.get().isFinishing()) {
                // caller is gone
                stopSelf();
                return;
            }
            runActivityWeakReference.get().onLocationAvailability();
        }

        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (runActivityWeakReference == null || runActivityWeakReference.get() == null || runActivityWeakReference.get().isFinishing()) {
               //  caller is gone
                stopSelf();
                return;
            }

            Location location = locationResult.getLastLocation();
            if (location != null && location.getAccuracy() > 0.0) {
                if (serviceCallbacks != null) {
                    Map<String, Location> filter2location = filterLocation(location);
                    serviceCallbacks.processNewLocation(filter2location);
                }
            }
        }
    }


    @Override
    public final void onSensorChanged(SensorEvent event) {
        // https://stackoverflow.com/questions/11578636/acceleration-from-devices-coordinate-system-into-absolute-coordinate-system
        if ((gravityValues != null) && (magneticValues != null) && (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {

            float[] deviceRelativeAcceleration = new float[4];
            deviceRelativeAcceleration[0] = event.values[0];
            deviceRelativeAcceleration[1] = event.values[1];
            deviceRelativeAcceleration[2] = event.values[2];
            deviceRelativeAcceleration[3] = 0;

            // Change the device relative acceleration values to earth relative values
            // X axis -> East
            // Y axis -> North Pole
            // Z axis -> Sky

            float[] R = new float[16], I = new float[16], earthAcc = new float[16];

            SensorManager.getRotationMatrix(R, I, gravityValues, magneticValues);

            float[] inv = new float[16];

            android.opengl.Matrix.invertM(inv, 0, R, 0);
            android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);

            for (LiveLocationFilter filter : usedFilters) {
                if (filter.name.contains("simple-kalman-")) {
                    filter.addAccelerationValue(earthAcc, event.timestamp);
                }
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            gravityValues = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticValues = event.values;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}


    public class LocalBinder extends Binder {
        public LocationUpdateService getService() {
            // Return this instance of LocationUpdateService so clients can call public methods
            return LocationUpdateService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        requestLocationUpdates();
        return binder;
    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

    public void onCreate() {
        // Acceleration sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);  // TODO: unregister on pause https://developer.android.com/reference/android/hardware/SensorManager
        sensorManager.registerListener(this, sensorMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorGravity, SensorManager.SENSOR_DELAY_NORMAL);

        // Prepare GPS request
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(GPS_REQUEST_INTERVAL_IN_MILLIS)
                .setFastestInterval(GPS_REQUEST_INTERVAL_IN_MILLIS);
        locationCallback = new CustomLocationCallback();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Notification ID cannot be 0.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0 and higher uses notification channels https://developer.android.com/training/notify-user/channels
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(chan);
            Notification notification8 = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setOngoing(true)
                    .setContentTitle(getString(R.string.notification_title))
                    .setSmallIcon(R.mipmap.compass_icon)
                    .setContentText(getString(R.string.notification_text))
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setTicker(getString(R.string.notification_text))
                    .build();
            startForeground(NOTIFICATION_ID, notification8);
        } else {
            Notification notification = new NotificationCompat.Builder(this, String.valueOf(NOTIFICATION_ID))  // unsicher ob das stimmt mit der notification id
                    .setOngoing(true)
                    .setContentTitle(getString(R.string.notification_title))
                    .setSmallIcon(R.mipmap.compass_icon)
                    .setContentText(getString(R.string.notification_text))
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setTicker(getString(R.string.notification_text))
                    .build();
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Map<String, Location> filterLocation(Location location) {
        Map<String, Location> filterResults = new HashMap<>();

        if (previousLocation == null) {
            // Our first location -> initialise filter. TODO: how do filters make sure first location is fine??
            for (LiveLocationFilter filter : usedFilters) {
                filterResults.put(filter.name, filter.initializeFilter(location));
            }
        } else {
            // We already had a previousLocation -> run normal filter step
            for (LiveLocationFilter filter : usedFilters) {
                filterResults.put(filter.name, filter.filterLocation(location, previousLocation));
            }
        }
        previousLocation = location;
        return filterResults;
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationCallback.onLocationAvailability(null);
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    @Override
    public boolean onUnbind(Intent intent) {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        previousLocation = null;
        return super.onUnbind(intent);
    }
}
