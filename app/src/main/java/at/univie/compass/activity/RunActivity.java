/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.activity;


// INFOS
// How to survive process being killed in background: https://medium.com/@wesleym/youve-handled-activity-recreation-what-about-process-recreation-f8f8dc80fa4a

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textview.MaterialTextView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import at.univie.compass.bluetooth.BluetoothLeService;
import at.univie.compass.dto.GameModus;
import at.univie.compass.global.GPXWaypointAnnotations;
import at.univie.compass.global.Global;
import at.univie.compass.location.LiveLocationFilter;
import at.univie.compass.location.LocationUpdateService;
import at.univie.compass.task.ProcessResultsTask;
import at.univie.compass.R;
import at.univie.compass.bluetooth.SampleGattAttributes;
import at.univie.compass.location.ServiceCallbacks;
import at.univie.compass.task.UploadPositionWorker;
import at.univie.compass.dto.ControlPoint;
import at.univie.compass.dto.ControlpointInfo;
import at.univie.compass.dto.Course;
import at.univie.compass.dto.Question;
import at.univie.compass.dto.ResultControlpoint;
import at.univie.compass.dto.ResultCourse;
import at.univie.compass.dto.ResultQuestion;
import at.univie.compass.dto.SharedCourse;


public class RunActivity extends AppCompatActivity implements View.OnClickListener, ServiceCallbacks, SensorEventListener  {  //, CompoundButton.OnCheckedChangeListener {
    public static WeakReference<MainActivity> callerMainActivity;

    // request id
    private static final int REQUEST_RESULT_QUESTION = 5555;
    private static final int REQUEST_CODE_ENABLE_GPS_WINDOW = 91;

    // UI
    Button buttonStart, buttonScanControlpointQRCode;
    ImageButton buttonInfo, buttonWhereAmI;
    SwitchMaterial switchGPS;
    private Chronometer chronometer;
    WebView webView;
    ImageView imageViewCompass;

    // Permissions
    private static final int REQUEST_LOCATION_ENABLING = 1234; // arbitrary
    private static final int REQUEST_LOCATION_PERMISSION = 12; // ...
    private boolean permissionsAlreadyAsked = false;

    // To persist data that changes while in background (protection against process kill + restore)
    private boolean isStopped;

    // Borg scale
    final Map<Integer, Integer> borgLevel2Description = new HashMap<Integer, Integer>() {{
        put(1, R.string.borgLevel1);
        put(2, R.string.borgLevel2);
        put(3, R.string.borgLevel3);
        put(4, R.string.borgLevel4);
        put(5, R.string.borgLevel5);
        put(6, R.string.borgLevel6);
        put(7, R.string.borgLevel7);
        put(8, R.string.borgLevel8);
        put(9, R.string.borgLevel9);
        put(10, R.string.borgLevel10);
    }};
    final static int INITIAL_BORG_LEVEL = 5;
    final static int MIN_SKIP_LEVEL = 7;
    private boolean borgDialogisOpen;
    private boolean tellUserToSkipDialogIsOpen;

    // Course logic
    public static SharedCourse currentSharedCourse;
    private String nickname;
    Course currentCourse;
    GameModus currentCourseGameModus;
    private ControlPoint[] controlPoints;
    private ArrayList<ArrayList<ControlpointInfo>> controlpointInfos;
    private Integer idx_currentCP;  // index of the current control point
    private Integer idx_CPCurrentQuestion;  // index of the control point of the current question
    private Date timestamp_started;  // needs to be stored to later compute time needed
    private Location startingLocation;  // needs to be stored for GPX
    private boolean running;
    private boolean reachedLastCP;
    private boolean lastCPHasQuestions;
    private boolean isAtStart;  // to make reaching the start a one shot event
    boolean locationRequestDialogIsOpen;  // to make opening the location request dialog a one shot event
    private Integer idxLastControlpoint;
    private Date lastUnskippedCPTimestamp;
    private Integer idxLastUnskippedControlpoint;
    private boolean hasDescriptions;
    private boolean hasSymbolTable;

    // Time when a location was sent the last time (to know when to send next one)
    Date lastSent, lastLocationDate;
    Location lastSentLocation, lastLocation;
    Handler positionUploadHandler;  // for periodic upload
    Runnable positionUploadRunnable;  // for periodic upload

    final static int MESSAGE_RETRIEVAL_INTERVAL_WITHOUT_GPS = 10000;  // milliseconds

    // Data for GPX
    Map<String, List<Double>> gpxLatitudes;  // static to survive onCreate -> onDestroy -> onCreate!
    Map<String, List<Double>> gpxLongitudes;  // static to survive onCreate -> onDestroy -> onCreate!
    Map<String, List<Double>> gpxAltitudes;  // static to survive onCreate -> onDestroy -> onCreate!
    Map<String, List<Double>> gpxSpeeds;  // static to survive onCreate -> onDestroy -> onCreate!
    Map<String, List<GPXWaypointAnnotations>> gpxAnnotations;  // static to survive onCreate -> onDestroy -> onCreate!
    private Location currentCPLocation;
    private Date currentCPTimestamp;

    // Result
    private ResultCourse resultCourse = new ResultCourse();  // static to survive onCreate -> onDestroy -> onCreate!
    private ArrayList<ResultQuestion> orderedResultQuestions;

    // WakeLock
    protected PowerManager.WakeLock wakeLock;

    // QR
    private IntentIntegrator qrScan;
    private boolean scanning;

    // Vibration + sound effect
    private final static int VIBRATION_DURATION_IN_MILLIS = 750;  // in milliseconds, how long to vibrate when controlpoint is reached, -1 turns vibration off
    private Vibrator vibrator;
    MediaPlayer mpCPReached;
    MediaPlayer mpQuestions;
    MediaPlayer mpFinish;

    // Compass
    private SensorManager sensorManager;
    float[] mGravity;
    float[] mGeomagnetic;

    // Heart rate
    public String hrm_ble_address;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothLeService mBluetoothLeService;
    private boolean ble_service_is_bound;
    public MaterialTextView currentHeartRateView;
    List<Double> heartRateList;
    List<String> heartRateTimeList;

    // to create this activity in non-visible mode = for caching the map
    private boolean dummy = false;

    // Show my location
    static final Long SHOW_MY_LOCATION_MILLIS = 30000L;
    boolean showingMyLocation;
    Handler hideMyLocationHandler;
    Runnable hideMyLocationRunnable;
    Date timestampRevealedLocation;

    // force skip
    boolean currentCPIsForceSkipped;

    // Draw only start if required
    boolean showEntireCourse;

    // User leaving
    boolean leavingOk;

    // For connection to LocationUpdateService
    public LocationUpdateService locationUpdateService;
    private boolean boundToGPSService = false;
    private ServiceConnection locationServiceConnection;

    // Values that are stored during onSaveInstanceState AND also during background
    public static String[] storableValuesForSurvival = {
            "idx_currentCP",
            "idx_CPCurrentQuestion",
            "idxLastControlpoint",
            "idxLastUnskippedControlpoint",
            "lastUnskippedCPTimestamp",
            "currentCPTimestamp",
            "currentCPLocation",
            "gpxLongitudes",
            "gpxLatitudes",
            "gpxAltitudes",
            "gpxSpeeds",
            "gpxAnnotations",
            "heartRateList",
            "heartRateTimeList",
            "lastLocation",
    };
    public static final String[] survivingListKeys = {
            "gpxLongitudes",
            "gpxLatitudes",
            "gpxAltitudes",
            "gpxSpeeds",
            "gpxAnnotations",
            "heartRateList",
            "heartRateTimeList",
    };

    private void storeValueWhileInBackground(String key, Object o, boolean onlyInBackground) {
        if ((!onlyInBackground || isStopped) && o != null) {
            if (key.contains("ocation")) {
                // because https://stackoverflow.com/questions/17840521/android-fatal-signal-11-sigsegv-at-0x636f7d89-code-1-how-can-it-be-tracked#comment54489618_31866823 -> weird SIGSEGV errors otherwise.
                Location l = (Location) o;
                Global.storeLocationForSurvival(getApplicationContext(), key, l);
            } else if (key.contains("gpx") || key.contains("heart")) {
                // device gets warm when you alawys store the entire lists of coordinates etc. -> use extra function that stores sublists instead.
                if (key.contains("gpx")) {
                    Map<String, ArrayList<Object>> f2l = (Map<String, ArrayList<Object>>) o;
                    ArrayList<Object> bestList = f2l.get(LocationUpdateService.bestFilter.name);
                    Global.storeListForSurvival(getApplicationContext(), key, bestList);
                } else {
                    ArrayList<Object> l = (ArrayList<Object>) o;
                    Global.storeListForSurvival(getApplicationContext(), key, l);
                }
            }
            else {
                // normal case
                Global.storeValueForSurvival(getApplicationContext(), key, o);
            }
        }
    }

    private Object restoreValue(String key, Type c) {
        if (key.contains("ocation")) {
            return Global.getLocationForSurvival(getApplicationContext(), key);
        } else if (key.contains("gpx") || key.contains("heart")) {
            if (key.contains("gpx")) {
                Map<String, ArrayList<Object>> f2l = new HashMap<>();
                f2l.put(LocationUpdateService.bestFilter.name, Global.getListForSurvival(getApplicationContext(), key, c));
                return f2l;
            } else {
                return Global.getListForSurvival(getApplicationContext(), key, c);
            }
        }
        return Global.getValueForSurvival(getApplicationContext(), key, c);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;

        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            if (SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)) {
                // orientation contains azimut, pitch and roll
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                float azimut = orientation[0];
                float rotation = -azimut * 360 / (2 * 3.14159f);

                imageViewCompass.setRotation(rotation);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public boolean showPositionAllowed() {
        boolean showPositionAllowed;
        try { // Compatibility with old server data
            showPositionAllowed = currentSharedCourse.getShowPositionAllowed();
        } catch (NullPointerException e) {
            showPositionAllowed = true;
        }
        return showPositionAllowed;
    }

    //region Activity Lifecycle Methods
    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boundToGPSService = false;
        permissionsAlreadyAsked = false;

        Bundle extras = getIntent().getExtras();
        nickname = (String) extras.get("nickname");

        dummy = (Boolean) extras.get("dummy");

        setContentView(R.layout.activity_run);

        // Restore formerly static variables on kill. TODO: Interference with "normal" reconstruction?
        if (savedInstanceState != null) {
            currentSharedCourse = (SharedCourse) savedInstanceState.getSerializable("currentSharedCourse");
            dummy = savedInstanceState.getBoolean("dummy");
            nickname = savedInstanceState.getString("nickname");
            resultCourse = (ResultCourse) savedInstanceState.getSerializable("resultCourse");
            orderedResultQuestions = (ArrayList<ResultQuestion>) savedInstanceState.getSerializable("orderedResultQuestions");

            gpxLongitudes = (Map<String, List<Double>>) restoreValue("gpxLongitudes", new TypeToken<List<Double>>(){}.getType());
            gpxLatitudes = (Map<String, List<Double>>) restoreValue("gpxLatitudes", new TypeToken<List<Double>>(){}.getType());
            gpxSpeeds = (Map<String, List<Double>>) restoreValue("gpxSpeeds", new TypeToken<List<Double>>(){}.getType());
            gpxAltitudes = (Map<String, List<Double>>) restoreValue("gpxAltitudes", new TypeToken<List<Double>>(){}.getType());
            gpxAnnotations = (Map<String, List<GPXWaypointAnnotations>>) restoreValue("gpxAnnotations", new TypeToken<ArrayList<GPXWaypointAnnotations>>(){}.getType());
            heartRateList = (ArrayList<Double>) restoreValue("heartRateList", new TypeToken<ArrayList<Double>>(){}.getType());
            heartRateTimeList = (ArrayList<String>) restoreValue("heartRateTimeList", new TypeToken<ArrayList<String>>(){}.getType());
            if (gpxLongitudes == null) {
                gpxLongitudes = new HashMap<>();
            }
            if (gpxLatitudes == null) {
                gpxLatitudes = new HashMap<>();
            }
            if (gpxSpeeds == null) {
                gpxSpeeds = new HashMap<>();
            }
            if (gpxAltitudes == null) {
                gpxAltitudes = new HashMap<>();
            }
            if (gpxAnnotations == null) {
                gpxAnnotations = new HashMap<>();
            }
            if (heartRateList == null) {
                heartRateList = new ArrayList<>();
            }
            if (heartRateTimeList == null) {
                heartRateTimeList = new ArrayList<>();
            }
            for (LiveLocationFilter filter: LocationUpdateService.usedFilters) {
                if (gpxLongitudes.get(filter.name) == null) gpxLongitudes.put(filter.name, new ArrayList<Double>());
                if (gpxLatitudes.get(filter.name) == null) gpxLatitudes.put(filter.name, new ArrayList<Double>());
                if (gpxAltitudes.get(filter.name) == null) gpxAltitudes.put(filter.name, new ArrayList<Double>());
                if (gpxSpeeds.get(filter.name) == null) gpxSpeeds.put(filter.name, new ArrayList<Double>());
                if (gpxAnnotations.get(filter.name) == null) gpxAnnotations.put(filter.name, new ArrayList<GPXWaypointAnnotations>());
            }
        }

        // Course data
        currentCourse = currentSharedCourse.getCourse();
        if (dummy) {
            showEntireCourse = true;
        } else if (savedInstanceState == null) {
            // first initialization
            try { // Compatibility with old server data
                showEntireCourse = currentSharedCourse.getShowCourseBeforeStart();
            } catch (NullPointerException e) {
                showEntireCourse = true;
            }
        } else {
            // re-initialization
            showEntireCourse = savedInstanceState.getBoolean("showEntireCourse");
        }
        currentCourseGameModus = currentSharedCourse.getGameModus();
        controlPoints = new ControlPoint[currentCourse.getControlpoints().size()];
        for (ControlPoint cp : currentCourse.getControlpoints()) {
            controlPoints[cp.getSequence()] = cp;
        }
        lastCPHasQuestions = controlPoints[controlPoints.length - 1].getQuestions().size() != 0;

        hasDescriptions = false;
        hasSymbolTable = false;
        for (ControlPoint cp : controlPoints) {
            if (cp.getDescription() != null && !cp.getDescription().equals("")) {
                hasDescriptions = true;
            }
            if (cp.getControlpointInfos() != null && cp.getControlpointInfos().size() != 0) {
                hasSymbolTable = true;
            }
        }

        // WebView
        webView = findViewById(R.id.map);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);

        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                setupMap(!showEntireCourse);  // initialize map with CP data after the page is loaded TODO showEntireCourse-Variable brauch ich nimmer
                repaintControlpoints();
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            }
        });

        // Test to get return value
        webView.addJavascriptInterface(this, "android");
        webView.loadUrl("file:///android_asset/map.html");

        // We only need the webview + course data in dummy mode (to get tile info) -> end initialization here
        if (dummy) {
            // immediately bring MainActivity back to foreground (hack to make RunActivity invisible)
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            startActivity(intent);
            return;
        }

        // UI
        buttonStart = findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(this);
        buttonInfo = findViewById(R.id.buttonInfo);
        buttonInfo.setOnClickListener(this);
        buttonWhereAmI = findViewById(R.id.buttonWhereAmI);
        buttonWhereAmI.setOnClickListener(this);
        findViewById(R.id.buttonInfoTable).setOnClickListener(this);
        buttonScanControlpointQRCode = findViewById(R.id.buttonScanControlpointQRCode);
        buttonScanControlpointQRCode.setOnClickListener(this);
        switchGPS = findViewById(R.id.switchGPS);
        switchGPS.setOnClickListener(this);
        imageViewCompass = findViewById(R.id.imageViewCompass);
        chronometer = findViewById(R.id.chronometer);
        if (!hasDescriptions) {
            buttonInfo.setVisibility(View.INVISIBLE);
        }
        if (!hasSymbolTable) {
            findViewById(R.id.buttonInfoTable).setVisibility(View.INVISIBLE);
        }

        // GPS service connection
        LocationUpdateService.runActivityWeakReference = new WeakReference<>(this);
        locationServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                // cast the IBinder and get MyService instance
                LocationUpdateService.LocalBinder binder = (LocationUpdateService.LocalBinder) service;
                locationUpdateService = binder.getService();
                boundToGPSService = true;
                locationUpdateService.setCallbacks(RunActivity.this); // register
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                boundToGPSService = false;
            }
        };

        //disable Display OFF
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "powermanager:wakelock");
        wakeLock.acquire();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // To invoke barcode scanning
        qrScan = new IntentIntegrator(this).setBeepEnabled(false);

        // Vibration and sound effect
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        mpCPReached = MediaPlayer.create(getApplicationContext(), R.raw.cpdetected);
        mpQuestions = MediaPlayer.create(getApplicationContext(), R.raw.cpquestions);
        mpFinish = MediaPlayer.create(getApplicationContext(), R.raw.finish);

        // Controlpoint Expert-Descriptions
        arrangeControlpointInfos();

        // Periodic message retrieval without GPS
        positionUploadHandler = new Handler();
        positionUploadRunnable = new Runnable() {
            public void run() {
                // Report position if we dont get GPS positions anyway and we reported at least 1 location so far
                if (!boundToGPSService && lastSent != null && lastSentLocation != null) {
                    tryToReportPosition(lastSentLocation, lastSent);
                }
                positionUploadHandler.postDelayed(this, MESSAGE_RETRIEVAL_INTERVAL_WITHOUT_GPS);
            }
        };
        positionUploadHandler.postDelayed(positionUploadRunnable, MESSAGE_RETRIEVAL_INTERVAL_WITHOUT_GPS);

        // Show my location
        hideMyLocationHandler = new Handler();
        hideMyLocationRunnable = new Runnable() {
            @Override
            public void run() {
                showingMyLocation = false;
                timestampRevealedLocation = null;
                undrawLocationOnMap();
            }
        };

        // Compass image
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        Sensor magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (magneticSensor == null || accelerometer == null) {
            imageViewCompass.setVisibility(View.GONE);
        } else {
            sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Connect to Heart Rate Monitor if one was selected earlier
        ble_service_is_bound = false;
        hrm_ble_address = Global.getStoredBLEaddress(getApplicationContext());
        if(hrm_ble_address != "") {
            currentHeartRateView = findViewById(R.id.currentHeartRateView);
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            ble_service_is_bound = true;
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            if (mBluetoothLeService != null) {
                final boolean result = mBluetoothLeService.connect(hrm_ble_address);
                Log.d("BLE", "Connect request result=" + result);
            }
        }
        else {
            findViewById(R.id.heartRateLayout).setVisibility(View.GONE);
        }

        // mode specific setup
        switch (currentCourseGameModus) {
            case GPS:
                buttonScanControlpointQRCode.setEnabled(false);  // TODO: sollte besser defaultmäßig schon false sein (?)
                buttonScanControlpointQRCode.setVisibility(View.GONE);  // we do not need this button in GPS mode
                switchGPS.setVisibility(View.GONE);
                break;
            case QRCODE:
                buttonScanControlpointQRCode.setEnabled(true);
                buttonStart.setVisibility(View.GONE);  // we do not need this button in QR mode
                break;
        }

        permissionsAlreadyAsked = false;

        // Differentiate between first init and re-init
        if (savedInstanceState == null) {
            // first initialization
            if (currentCourseGameModus == GameModus.QRCODE) {
                // In QR mode on first initialization: ask for GPS
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
                builder.setMessage(getString(R.string.want_to_activate_gps))
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                // "Yes" button was clicked
                                if (!areLocationPermissionsGranted()) {
                                    disableGPSInQRMode();
                                    requestLocationPermissions();
                                } else if (!isLocationEnabled()) {
                                    disableGPSInQRMode();
                                    makeQRLocationEnablementDialog();
                                } else {
                                    enableGPSInQRMode();
                                }
                            }
                        })
                        .setNegativeButton(getString(R.string.no), null)
                        .show();
            }

            // Initialize some variables...
            idxLastControlpoint = -1;
            idxLastUnskippedControlpoint = -999;
            idx_currentCP = 0;  // index of the current control point
            running = false;
            reachedLastCP = false;
            isAtStart = false;  // to make reaching the start a one shot event
            locationRequestDialogIsOpen = false;  // to make opening the location request dialog a one shot event
            lastSentLocation = null;
            lastLocation = null;
            lastLocationDate = null;
            lastSent = null;
            currentCPIsForceSkipped = false;

            showingMyLocation = false;
            timestampRevealedLocation = null;
            leavingOk = false;
            scanning = false;

            borgDialogisOpen = false;
            tellUserToSkipDialogIsOpen = false;
        } else {
            // re-initialization
            // Just check if we still have location in QR mode (if activated) instead of asking again...
            if (savedInstanceState.getBoolean("GPSActivatedInQRMode")) {
                onLocationAvailability();
            }
            // Restore instance state
            idxLastControlpoint = (Integer) restoreValue("idxLastControlpoint", Integer.class);
            idx_currentCP = (Integer) restoreValue("idx_currentCP", Integer.class);
            idx_CPCurrentQuestion = (Integer) restoreValue("idx_CPCurrentQuestion", Integer.class);
            idxLastUnskippedControlpoint = (Integer) restoreValue("idxLastUnskippedControlpoint", Integer.class);
            lastUnskippedCPTimestamp = (Date) restoreValue("lastUnskippedCPTimestamp", Date.class);
            currentCPTimestamp = (Date) restoreValue("currentCPTimestamp", Date.class);
            currentCPLocation = (Location) restoreValue("currentCPLocation", Location.class);
            lastLocation = (Location) restoreValue("lastLocation", Location.class);
            running = savedInstanceState.getBoolean("running");
            reachedLastCP = savedInstanceState.getBoolean("reachedLastCP");
            isAtStart = savedInstanceState.getBoolean("isAtStart");
            locationRequestDialogIsOpen = false;  // DON'T restore because we don't restore the dialog directly but via onLocationAvailability
            tellUserToSkipDialogIsOpen = savedInstanceState.getBoolean("tellUserToSkipDialogIsOpen");
            borgDialogisOpen = savedInstanceState.getBoolean("borgDialogisOpen");
            showingMyLocation = savedInstanceState.getBoolean("showingMyLocation");
            leavingOk = savedInstanceState.getBoolean("leavingOk");
            scanning = savedInstanceState.getBoolean("scanning");
            currentCPIsForceSkipped = savedInstanceState.getBoolean("currentCPIsForceSkipped");
            timestamp_started = (Date) savedInstanceState.getSerializable("timestamp_started");
            startingLocation = savedInstanceState.getParcelable("startingLocation");
            lastSent = (Date) savedInstanceState.getSerializable("lastSent");
            lastSentLocation = savedInstanceState.getParcelable("lastSentLocation");
            lastLocationDate = savedInstanceState.getParcelable("lastLocationDate");
            timestampRevealedLocation = (Date) savedInstanceState.getSerializable("timestampRevealedLocation");
            boolean switchGPS_wasChecked = savedInstanceState.getBoolean("switchGPS_wasChecked");

            if (borgDialogisOpen) {
                makeBorgDialog(lastLocationDate); // TODO: lastLocationDate nicht nehmen! (oder vorher befüllen)
            }

            if (showingMyLocation) {
                long millis_passed = Global.now().getTime() - timestampRevealedLocation.getTime();
                long millis_remaining = SHOW_MY_LOCATION_MILLIS - millis_passed;
                if (millis_remaining > 0) {
                    drawLocationOnMap(lastLocation, true);
                    hideMyLocationHandler.postDelayed(hideMyLocationRunnable, millis_remaining);
                } else {
                    showingMyLocation = false;
                    timestampRevealedLocation = null;
                    undrawLocationOnMap();
                }
            }

            if (running) {
                chronometer.setBase(savedInstanceState.getLong("chronometer.getBase()"));
                chronometer.start();
            }

            // Otherwise "locked" into isAtStart and we cannot start the run
            if (!running) {
                isAtStart = false;
            }

            if (currentCourseGameModus == GameModus.QRCODE && switchGPS_wasChecked && areLocationPermissionsGranted()) {  // when permissions were withdrawn, they seem to be asked for somewhere else TODO check permission withdrawal in GPS mode
                tryToActivateGPSInQRMode();   // TODO:  SIGSEGV Service greift auf neu gebaute RunActivity zu (die jetzt ja woanders ist!!) wenn diese ZEile einkommentiert ist passiert er sofort weil der service neugestartet wird
            }
        }

        if (currentCourseGameModus == GameModus.GPS) {
            onLocationAvailability();  // may open a dialog
        }
    }

    private void makeQRLocationEnablementDialog() {
        locationRequestDialogIsOpen = true;
        final AlertDialog.Builder builder = new MaterialAlertDialogBuilder(RunActivity.this)
                .setMessage(getString(R.string.turn_on_location_qr))
                .setCancelable(true)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        disableGPSInQRMode();
                    }
                })
                .setPositiveButton(getString(R.string.location_settings), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_CODE_ENABLE_GPS_WINDOW);
                        locationRequestDialogIsOpen = false;
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        locationRequestDialogIsOpen = false;
                        disableGPSInQRMode();
                        if (currentCourseGameModus == GameModus.GPS && !isLocationEnabled()) {
                            finish();
                        }
                    }
                });
        alert = builder.create();
        alert.show();
    }

    private boolean isScreenOn() {
        return ((PowerManager) getSystemService(Context.POWER_SERVICE)).isInteractive();
    }

    @Override
    protected void onUserLeaveHint() {
        if (!leavingOk && running) {
            resultCourse.setSwitchAppCounter(resultCourse.getSwitchAppCounter() + 1);
        }
        super.onUserLeaveHint();
    }

    @Override
    protected void onStop() {
        isStopped = true;
        if (!isScreenOn()) {
            leavingOk = true;
        }
        super.onStop();
    }

    @Override
    protected void onStart() {
        isStopped = false;
        leavingOk = false;
        if (scanning) {
            leavingOk = true;
        }
        scanning = false;
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        if (!dummy) {
            unbindLocationUpdateService();
            wakeLock.release();
            positionUploadHandler.removeCallbacks(positionUploadRunnable);
            hideMyLocationHandler.removeCallbacks(hideMyLocationRunnable);
            if (ble_service_is_bound) {
                unbindService(mServiceConnection);
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (!dummy) {
            storeValueWhileInBackground("idx_currentCP", idx_currentCP, false);
            storeValueWhileInBackground("idx_CPCurrentQuestion", idx_CPCurrentQuestion, false);
            outState.putBoolean("running", running);
            outState.putBoolean("reachedLastCP", reachedLastCP);
            outState.putBoolean("isAtStart", isAtStart);
            outState.putBoolean("locationRequestDialogIsOpen", locationRequestDialogIsOpen);
            outState.putBoolean("tellUserToSkipDialogIsOpen", tellUserToSkipDialogIsOpen);
            outState.putBoolean("borgDialogisOpen", borgDialogisOpen);
            outState.putBoolean("showingMyLocation", showingMyLocation);
            outState.putBoolean("leavingOk", leavingOk);
            outState.putBoolean("scanning", scanning);
            outState.putBoolean("showEntireCourse", showEntireCourse);
            outState.putBoolean("switchGPS_wasChecked", switchGPS.isChecked());
            outState.putBoolean("currentCPIsForceSkipped", currentCPIsForceSkipped);
            outState.putSerializable("timestampRevealedLocation", timestampRevealedLocation);
            storeValueWhileInBackground("idxLastControlpoint", idxLastControlpoint, false);
            storeValueWhileInBackground("idxLastUnskippedControlpoint", idxLastUnskippedControlpoint, false);
            outState.putSerializable("timestamp_started", timestamp_started);
            storeValueWhileInBackground("lastUnskippedCPTimestamp", lastUnskippedCPTimestamp, false);
            storeValueWhileInBackground("currentCPTimestamp", currentCPTimestamp, false);
            storeValueWhileInBackground("currentCPLocation", currentCPLocation, false);
            outState.putParcelable("startingLocation", startingLocation);
            outState.putSerializable("lastSent", lastSent);
            outState.putParcelable("lastSentLocation", lastSentLocation);
            outState.putParcelable("lastLocation", lastLocation);

            // chronometer state
            outState.putLong("chronometer.getBase()", chronometer.getBase());

            // to re-ask for GPS
            outState.putBoolean("GPSActivatedInQRMode", currentCourseGameModus == GameModus.QRCODE && switchGPS.isChecked());

            // Formerly static variables
            outState.putSerializable("currentSharedCourse", currentSharedCourse);
            outState.putString("nickname", nickname);
            outState.putBoolean("dummy", dummy);
            outState.putSerializable("resultCourse", resultCourse);
            outState.putSerializable("orderedResultQuestions", orderedResultQuestions);

            storeValueWhileInBackground("gpxLongitudes", gpxLongitudes, false);
            storeValueWhileInBackground("gpxLatitudes", gpxLatitudes, false);
            storeValueWhileInBackground("gpxAltitudes", gpxAltitudes, false);
            storeValueWhileInBackground("gpxSpeeds", gpxSpeeds, false);
            storeValueWhileInBackground("gpxAnnotations", gpxAnnotations, false);
            storeValueWhileInBackground("heartRateList", heartRateList, false);
            storeValueWhileInBackground("heartRateTimeList", heartRateTimeList, false);
        }

        super.onSaveInstanceState(outState);
    }
    //endregion

    AlertDialog alert;
    void disableGPSInQRMode() {
        switchGPS.setChecked(false);
        unbindLocationUpdateService();
    }
    void enableGPSInQRMode() {
        switchGPS.setChecked(true);
        bindLocationUpdateService();
    }
    public void onLocationAvailability() {
        boolean locationEnabled = isLocationEnabled();
        boolean permissionsGranted = areLocationPermissionsGranted();

        switch (currentCourseGameModus) {
            case GPS:
                if (!permissionsGranted && !permissionsAlreadyAsked) {
                    permissionsAlreadyAsked = true;
                    requestLocationPermissions();
                } else if (!locationEnabled && !locationRequestDialogIsOpen) {
                    locationRequestDialogIsOpen = true;

                    final AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this)
                            .setMessage(getString(R.string.turn_on_location_gps))
                            .setCancelable(false)
                            .setPositiveButton(R.string.location_settings, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int id) {
                                    startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_CODE_ENABLE_GPS_WINDOW);
                                    locationRequestDialogIsOpen = false;
                                }
                            })
                            .setNegativeButton(getString(R.string.quit_run), new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int id) {
                                    dialog.cancel();
                                    locationRequestDialogIsOpen = false;
                                    if (currentCourseGameModus == GameModus.GPS && !isLocationEnabled()) {
                                        finish();
                                    }
                                }
                            });
                    alert = builder.create();
                    alert.show();
                } else if (locationEnabled && locationRequestDialogIsOpen) {
                    locationRequestDialogIsOpen = false;
                    alert.dismiss();  // user turned on location manually while dialog is open -> close dialog
                    bindLocationUpdateService();
                } else {
                    bindLocationUpdateService();
                }
                break;

            case QRCODE:
                if (!permissionsGranted) {
                    disableGPSInQRMode();
                    requestLocationPermissions();
                } else if (!locationEnabled) {
                    disableGPSInQRMode();  // location wird disablet -> schalte GPS aus
                }
        }
    }

    // called by LocationUpdateService when it has a new location, see also https://stackoverflow.com/a/23587641
    @Override
    public void processNewLocation(Map<String, Location> filter2location) {
        Date timestamp = Global.now();
        if (running) {
            String cpReached = idxLastControlpoint == idx_currentCP ? "" : String.valueOf(idx_currentCP);
            addGPXWaypoint(filter2location, cpReached, timestamp);
        }

        Location bestLocation = filter2location.get(LocationUpdateService.bestFilter.name);

        // Show my location
        lastLocation = bestLocation;
        storeValueWhileInBackground("lastLocation", lastLocation, true);
        if (showingMyLocation) {
            drawLocationOnMap(lastLocation, false);
        }

        if (lastSent == null || timestamp.getTime() - lastSent.getTime() > Global.POSITION_REPORTING_INTERVAL) {
            lastSent = timestamp;  // do this first because we dont want it to matter for the reporting frequency if the sending was successful or not
            lastSentLocation = bestLocation;
            tryToReportPosition(bestLocation, timestamp);
        }

        checkForControlpoint(bestLocation);  // use location from best filter to check for control points

        if (running) {
            idxLastControlpoint = idx_currentCP;
            storeValueWhileInBackground("idxLastControlpoint", idxLastControlpoint, true);
        }
    }

    private void tryToReportPosition(Location location, Date timestamp) {
        UploadPositionWorker.init(UUID.fromString(Global.getGuid(getApplicationContext())), location, nickname, Global.dateToStringForPositionDTO(timestamp), currentSharedCourse.getId(), this);
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest uploadPositionWorkRequest = new OneTimeWorkRequest.Builder(UploadPositionWorker.class)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(Global.REPORT_POSITION_WORK_ID, ExistingWorkPolicy.REPLACE, uploadPositionWorkRequest);  // https://developer.android.com/topic/libraries/architecture/workmanager/advanced#unique
    }

    public void onUploadPositionTaskFinished(String[] messages) {
        if (messages.length > 0) {
            mpQuestions.seekTo(0);
            mpQuestions.start();

            String allMessages = "";
            for(int i = 0; i < messages.length; i++) {
                allMessages += messages[i];
                if(i != messages.length - 1)
                    allMessages+="\n";
            }

            Global.snack(this, allMessages, Snackbar.LENGTH_INDEFINITE);
        }
    }

    // "start" location service
    public void unbindLocationUpdateService() {
        if (boundToGPSService) {
            unbindService(locationServiceConnection);
            boundToGPSService = false;
        }
    }

    // "stop" location service
    protected void bindLocationUpdateService() {
        if (!boundToGPSService) {
            Intent intent = new Intent(this, LocationUpdateService.class);
            bindService(intent, locationServiceConnection, Context.BIND_AUTO_CREATE);
            boundToGPSService = true;
        }
    }

    private void arrangeControlpointInfos() {
        int tempColIndex;
        controlpointInfos = new ArrayList<>();
        ArrayList<ControlpointInfo> tempInfos;
        ArrayList<ControlpointInfo> cpInfos;
        int nrOfInfosInserted;
        for (int i = 0; i < controlPoints.length; i++) {
            tempInfos = new ArrayList<>();
            cpInfos = new ArrayList<>();
            nrOfInfosInserted = 0;
            for (ControlpointInfo cpInfo : controlPoints[i].getControlpointInfos()) {
                cpInfos.add(cpInfo);
            }
            Collections.sort(cpInfos, new Comparator<ControlpointInfo>() {
                @Override
                public int compare(ControlpointInfo o1, ControlpointInfo o2) {
                    return o1.getCol().compareTo(o2.getCol());
                }
            });
            for (int j = 0; j < cpInfos.size(); j++) {
                tempColIndex = cpInfos.get(j).getCol().toString().toCharArray()[0] - 67 - nrOfInfosInserted;
                for (int k = 0; k < tempColIndex; k++) {
                    tempInfos.add(null);
                    nrOfInfosInserted++;
                }
                tempInfos.add(cpInfos.get(j));
                nrOfInfosInserted++;
            }
            for (int j = tempInfos.size(); j < 6; j++) tempInfos.add(null);
            controlpointInfos.add(tempInfos);
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                findViewById(R.id.heartRateLayout).setVisibility(View.GONE);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    public void displayData(String data) {
        if (data != null) {
            currentHeartRateView.setText(data);
            if (tryParseDouble(data)) {
                if(running) {
                    Double heartRate = Double.parseDouble(data);
                    heartRateList.add(heartRate);
                    heartRateTimeList.add(Global.dateToString(Global.now()));
                    storeValueWhileInBackground("heartRateList", heartRateList, true);
                    storeValueWhileInBackground("heartRateTimeList", heartRateTimeList, true);
                }
            }
        }
    }

    private boolean tryParseDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        /*ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();*/

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            // HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
           /* currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);*/

            // ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            // ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                // charas.add(gattCharacteristic);
                // HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();

                if(uuid.equals(SampleGattAttributes.HEART_RATE_MEASUREMENT)) {
                    final int charaProp = gattCharacteristic.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                        // If there is an active notification on a characteristic, clear
                        // it first so it doesn't update the data field on the user interface.
                        if (mNotifyCharacteristic != null) {
                            mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                            mNotifyCharacteristic = null;
                        }
                        mBluetoothLeService.readCharacteristic(gattCharacteristic);
                    }
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        mNotifyCharacteristic = gattCharacteristic;
                        mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                    }
                }
            }
        }
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("BLE", "Unable to initialize Bluetooth");
                // no further action
            }
            else {
                // Automatically connects to the device upon successful start-up initialization.
                mBluetoothLeService.connect(hrm_ble_address);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentHeartRateView.setText(resourceId);
            }
        });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @JavascriptInterface
    public void onData(String value) {
        if (dummy && value.contains(";") && value.contains(",")) {
            // format of "value" is defined in javascript
            int i_sep = value.indexOf(";");

            String[] zoomLimits = value.substring(0, i_sep).split(",");
            int minZoomToCache = Integer.parseInt(zoomLimits[0]);
            int maxZoomToCache = Integer.parseInt(zoomLimits[1]);

            String[] extents = value.substring(i_sep+1).split(",");
            double minx = Double.parseDouble(extents[0]);
            double miny = Double.parseDouble(extents[1]);
            double maxx = Double.parseDouble(extents[2]);
            double maxy = Double.parseDouble(extents[3]);

            if (callerMainActivity != null) {
                MainActivity caller = callerMainActivity.get();
                if (caller != null && !caller.isFinishing()) {
                    // caller is not gone
                    caller.startTileDownload(minZoomToCache, maxZoomToCache, minx, miny, maxx, maxy);
                }
            }
            finish();
        }
    }

    protected void setupMap(boolean onlyStart) {
        try {
            String jsToInject =
                    "var lat = [];\n" +
                    "var lon = [];\n";

            int j = controlPoints.length;
            for(int i = 0; i < j; i++) {
                jsToInject +=
                        "lat[" + i + "] = " + controlPoints[i].getLatitude() + ";\n" +
                        "lon[" + i + "] = " + controlPoints[i].getLongitude() + ";\n";
            }

            boolean initTileLayers = !dummy;  // dont draw tile layers in dummy mode
            jsToInject +=
                    "initControlPoints(lon, lat);\n" +
                    "drawControlPoints(0, " + onlyStart + ");\n" +
                    "fitMapToControlPoints(" + initTileLayers + ");\n";

            webView.evaluateJavascript(jsToInject, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    addImageOverlay();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addKmlOverlay() {
        if(!dummy && currentCourse.getOrienteeringMap().getMapOverlayKml() != null) {
            String img = Base64.encodeToString(currentCourse.getOrienteeringMap().getMapOverlayKml(), Base64.NO_WRAP);
            String imgBase64 = "data:" + currentCourse.getOrienteeringMap().getMapOverlayKmlContentType() + ";base64," + img;
            String jsToInject = "addKMLOverlay(\"" + imgBase64 + "\");\n";
            webView.evaluateJavascript(jsToInject, null);
        }
    }

    private void addImageOverlay() {
        if(!dummy && currentCourse.getOrienteeringMap().getMapOverlayImage() != null) {
            String img = Base64.encodeToString(currentCourse.getOrienteeringMap().getMapOverlayImage(), Base64.NO_WRAP);
            String imgBase64 = "data:" + currentCourse.getOrienteeringMap().getMapOverlayImageContentType() + ";base64," + img;
            String jsToInject =
                    "var center = [" + currentCourse.getOrienteeringMap().getImageCenterX() + ", " + currentCourse.getOrienteeringMap().getImageCenterY() + "];\n" +
                            "var scale = [" + currentCourse.getOrienteeringMap().getImageScaleX() + ", " + currentCourse.getOrienteeringMap().getImageScaleY() + "];\n" +
                            "var rotation = " + currentCourse.getOrienteeringMap().getImageRotation() + ";\n" +
                            "addImageOverlay(\"" + imgBase64 + "\", center, scale, rotation);\n";
            webView.evaluateJavascript(jsToInject, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    addKmlOverlay();
                }
            });
        }
    }

    protected void repaintControlpoints() {
        webView.evaluateJavascript("drawControlPoints(" + idx_currentCP + ", " + !showEntireCourse + ");", null);
    }

    private void drawLocationOnMap(Location location, boolean rezoom) {
        String jsToInject =
                "addSinglePoint(" + location.getLongitude() + ", " + location.getLatitude() + ");\n";
        if (rezoom) {
            jsToInject += "rezoom();\n";
        }

        webView.evaluateJavascript(jsToInject, null);
    }

    private void undrawLocationOnMap() {
        String jsToInject =
                "removeSinglePoint();\n" +
                "setDefaultView();\n";
        webView.evaluateJavascript(jsToInject, null);
    }

    protected void showMyLocationOnMap(Long howLongMillis) {
        if (!showingMyLocation) {
            timestampRevealedLocation = Global.now();
            showingMyLocation = true;
            drawLocationOnMap(lastLocation, true);
            if (running) {
                resultCourse.setShowPositionCounter(resultCourse.getShowPositionCounter() + 1);
            }
            hideMyLocationHandler.postDelayed(hideMyLocationRunnable, howLongMillis);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonStart:
                reached(controlPoints[idx_currentCP],  startingLocation, Global.now(), true, false, false);
                break;
            case R.id.buttonScanControlpointQRCode:
                leavingOk = true;
                scanning = true;
                qrScan.initiateScan();
                break;
            case R.id.buttonInfo:
                String[] data = new String[controlPoints.length];
                for (int i = 0; i < controlPoints.length; i++) {
                    String nr;
                    if (i == 0)
                        nr = getString(R.string.cp_name_first);
                    else if (i == controlPoints.length-1)
                        nr = getString(R.string.cp_name_last);
                    else
                        nr = getString(R.string.cp_name_regular, i);
                    String description = controlPoints[i].getDescription() == null ? "" : controlPoints[i].getDescription();
                    data[i] = nr + ": " + description;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.control_point_descriptions))
                        .setItems(data, null)
                        .show();
                break;
            case R.id.buttonWhereAmI:
                    MaterialAlertDialogBuilder whereAmIDialogBuilder = new MaterialAlertDialogBuilder(RunActivity.this);
                    if (showPositionAllowed() && !showingMyLocation) {
                        whereAmIDialogBuilder.setMessage(getString(R.string.where_am_i))
                                .setPositiveButton(getString(R.string.show_my_location), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        if (lastLocation == null || ((currentCourseGameModus == GameModus.QRCODE) && !switchGPS.isChecked())) {
                                            Global.snack(RunActivity.this, getString(R.string.no_location_available), Snackbar.LENGTH_LONG);
                                        } else {
                                            showMyLocationOnMap(SHOW_MY_LOCATION_MILLIS);
                                        }
                                    }
                                })
                                .setNeutralButton(getString(R.string.skip_this_controlpoint), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        MaterialAlertDialogBuilder areYouSureDialogBuilder = new MaterialAlertDialogBuilder(RunActivity.this);
                                        areYouSureDialogBuilder.setMessage(getString(R.string.skip_this_controlpoint) + "?")
                                                .setNegativeButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int i) {
                                                        reached(controlPoints[idx_currentCP], controlPoints[idx_currentCP].getLocation(), Global.now(), idx_currentCP==0, idx_currentCP==controlPoints.length-1, true);
                                                    }
                                                })
                                                .setPositiveButton(getString(R.string.no), null)
                                                .show();
                                    }
                                })
                                .setNegativeButton(getString(R.string.cancel), null)
                                .show();
                    } else {
                        whereAmIDialogBuilder.setMessage(getString(R.string.where_am_i))
                                .setPositiveButton(getString(R.string.skip_this_controlpoint), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        MaterialAlertDialogBuilder areYouSureDialogBuilder = new MaterialAlertDialogBuilder(RunActivity.this);
                                        areYouSureDialogBuilder.setMessage(getString(R.string.skip_this_controlpoint) + "?")
                                                .setNegativeButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int i) {
                                                        reached(controlPoints[idx_currentCP], controlPoints[idx_currentCP].getLocation(), Global.now(), idx_currentCP == 0, idx_currentCP == controlPoints.length - 1, true);
                                                    }
                                                })
                                                .setPositiveButton(getString(R.string.no), null)
                                                .show();
                                    }
                                })
                                .setNegativeButton(getString(R.string.cancel), null)
                                .show();
                    }
                break;
            case R.id.buttonInfoTable:
                showDescriptionTable();
                break;
            case R.id.switchGPS:
                // https://stackoverflow.com/a/38208967
                // Reset
                if (switchGPS.isChecked()) {
                    // has been checked
                    tryToActivateGPSInQRMode();
                } else {
                    // has been unchecked
                    disableGPSInQRMode();
                }
                break;
        }
    }

    private void tryToActivateGPSInQRMode() {
        if (!areLocationPermissionsGranted()) {
            disableGPSInQRMode();
            requestLocationPermissions();
        } else if (!isLocationEnabled()) {
            disableGPSInQRMode();
            makeQRLocationEnablementDialog();
        } else {
            enableGPSInQRMode();
        }
    }

    private void styleTextViewForDescriptionTable(TextView textView, LinearLayout.LayoutParams params) {
        textView.setTextColor(getColor(R.color.black));
        textView.setLayoutParams(params);
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        textView.setTextSize(20);
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        textView.setBackgroundResource(R.drawable.expert_table_border);
    }

    private void showDescriptionTable( ) {
        LayoutInflater inflater = getLayoutInflater();
        ScrollView scrollView = (ScrollView) inflater.inflate(R.layout.expert_desc_table, null);
        LinearLayout tableLayout = scrollView.findViewById(R.id.expert_desc_table);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.weight = 1;
        params.width = 0;
        params.gravity = Gravity.CENTER;
        params.height = 120;

        LinearLayout tableRow;
        TextView textView;
        ImageView imageView;
        byte[] imageByteArray;

        tableRow = (LinearLayout) inflater.inflate(R.layout.expert_desc_table_row, null);
        textView = new TextView(getApplicationContext());
        styleTextViewForDescriptionTable(textView, params);
        textView.setText(currentCourse.getName());
        tableRow.addView(textView);
        tableLayout.addView(tableRow);

        tableRow = (LinearLayout) inflater.inflate(R.layout.expert_desc_table_row, null);

        params.weight = 3;
        textView = new TextView(getApplicationContext());
        styleTextViewForDescriptionTable(textView, params);
        textView.setText(controlPoints.length + "");
        tableRow.addView(textView);

        textView = new TextView(getApplicationContext());
        styleTextViewForDescriptionTable(textView, params);
        textView.setText(String.format("%.2f", currentCourse.getLength() / 1000) + " km");
        tableRow.addView(textView);

        params.weight = 2;
        textView = new TextView(getApplicationContext());
        styleTextViewForDescriptionTable(textView, params);
        textView.setText(currentCourse.getAltitudeUp() + " m");
        tableRow.addView(textView);

        tableLayout.addView(tableRow);

        params.weight = 1;
        for(int i = 0; i < controlpointInfos.size(); i++) {
            tableRow = (LinearLayout) inflater.inflate(R.layout.expert_desc_table_row, null);

            textView = new TextView(getApplicationContext());
            styleTextViewForDescriptionTable(textView, params);
            textView.setText(i + "");
            tableRow.addView(textView);

            textView = new TextView(getApplicationContext());
            styleTextViewForDescriptionTable(textView, params);

            if(controlPoints[i].getControlCode() != null)
                textView.setText(controlPoints[i].getControlCode().toString());
            else
                textView.setText("");

            tableRow.addView(textView);

            for (int j = 0; j <controlpointInfos.get(i).size(); j++) {
                imageView = new ImageView(getApplicationContext());
                imageView.setBackgroundResource(R.drawable.expert_table_border);
                imageView.setLayoutParams(params);
                imageView.setAdjustViewBounds(true);
                if(controlpointInfos.get(i).get(j) != null) {
                    imageByteArray = controlpointInfos.get(i).get(j).getImage();
                    imageView.setImageBitmap(BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length));
                } else {
                    imageView.setImageResource(0);
                }
                tableRow.addView(imageView);

            }
            tableLayout.addView(tableRow);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(scrollView);
        builder.show();
    }

    private void addGPXWaypoint(Map<String, Location> filter2Location, String cpReached, Date timestamp) {
        for (Map.Entry<String, Location> entry : filter2Location.entrySet()) {
            gpxLongitudes.get(entry.getKey()).add(entry.getValue().getLongitude());
            gpxLatitudes.get(entry.getKey()).add(entry.getValue().getLatitude());
            gpxAltitudes.get(entry.getKey()).add(entry.getValue().getAltitude());
            gpxSpeeds.get(entry.getKey()).add((double) entry.getValue().getSpeed());
            gpxAnnotations.get(entry.getKey()).add(new GPXWaypointAnnotations(entry.getValue(), cpReached, timestamp));
        }
        storeValueWhileInBackground("gpxLongitudes", gpxLongitudes, true);
        storeValueWhileInBackground("gpxLatitudes", gpxLatitudes, true);
        storeValueWhileInBackground("gpxAltitudes", gpxAltitudes, true);
        storeValueWhileInBackground("gpxSpeeds", gpxSpeeds, true);
        storeValueWhileInBackground("gpxAnnotations", gpxAnnotations, true);
    }

    private void startTrack(GameModus gameModus, Date timestamp, Location startLocation) {
        Global.clearValuesForSurvival(getApplicationContext());

        if (showingMyLocation) {
            // Hide position again if shown before run.
            hideMyLocationHandler.removeCallbacksAndMessages(null);
            hideMyLocationRunnable.run();
        }

        idxLastControlpoint = 0;
        idxLastUnskippedControlpoint = -999;

        timestamp_started = timestamp;
        chronometer.setBase(SystemClock.elapsedRealtime());

        // Init resultCourse
        resultCourse = new ResultCourse();
        resultCourse.setId(null);
        resultCourse.setViewCode(null);
        resultCourse.setNickName(nickname);
        resultCourse.setSwitchAppCounter(0);
        resultCourse.setShowPositionCounter(0);
        // only keep the ID of the SharedCourse in the Result to avoid sending the entire SC
        resultCourse.setSharedCourse(new SharedCourse());
        resultCourse.getSharedCourse().setId(currentSharedCourse.getId());
        resultCourse.getSharedCourse().setCourse(new Course());
        resultCourse.getSharedCourse().getCourse().setName(currentCourse.getName());  // useful for viewCode later
        resultCourse.getSharedCourse().getCourse().setLocation(currentCourse.getLocation());  // useful for viewCode later
        resultCourse.getSharedCourse().getCourse().setLength(currentCourse.getLength());  // useful for viewCode later
        resultCourse.setTimeStampStarted(Global.dateToString(timestamp));
        for (int i = 0; i < controlPoints.length; i++) {
            setResultControlpoint(null, null, null, i, false, null, false, false);  // initialize
        }

        // Reset questions
        orderedResultQuestions = new ArrayList<ResultQuestion>();

        // Reset GPX data
        gpxLongitudes = new HashMap<>(); //new ArrayList<>();
        gpxLatitudes = new HashMap<>(); //new ArrayList<>();
        gpxAltitudes = new HashMap<>(); //new ArrayList<>();
        gpxSpeeds = new HashMap<>(); //new ArrayList<>();
        gpxAnnotations = new HashMap<>(); //new ArrayList<>();

        // Reset HR data
        heartRateList = new ArrayList<>();
        heartRateTimeList = new ArrayList<>();

        for (LiveLocationFilter filter: LocationUpdateService.usedFilters) {
            gpxLongitudes.put(filter.name, new ArrayList<Double>());
            gpxLatitudes.put(filter.name, new ArrayList<Double>());
            gpxAltitudes.put(filter.name, new ArrayList<Double>());
            gpxSpeeds.put(filter.name, new ArrayList<Double>());
            gpxAnnotations.put(filter.name, new ArrayList<GPXWaypointAnnotations>());
        }

        switch (gameModus) {
            case GPS:
                buttonStart.setEnabled(false);
                break;
            case QRCODE:
                break;
        }

        showEntireCourse = true;
        chronometer.start();
        running = true;
    }

    private void finishTrack(GameModus gameModus, Date timestamp) {
        chronometer.stop();

        long millis_passed = timestamp.getTime() - timestamp_started.getTime();
        resultCourse.setTimeStampFinished(Global.dateToString(timestamp));
        resultCourse.setTotalDurationInMillis(millis_passed);

        ProcessResultsTask rt = new ProcessResultsTask(getApplicationContext(), resultCourse, currentSharedCourse,
                gpxLongitudes, gpxLatitudes,  gpxAltitudes,  gpxSpeeds, gpxAnnotations,
                heartRateList, heartRateTimeList,
                nickname
        );
        rt.execute();

        // Allow only 1 run if unique QR code is used
        Boolean onlyOneRunAllowed;
        try { // Compatibility with old smartphone data
             onlyOneRunAllowed = currentSharedCourse.viaOneTimeCode;
            if (onlyOneRunAllowed) {
                Global.deleteStoredCourse(getApplicationContext());

                if (callerMainActivity != null) {
                    MainActivity caller = callerMainActivity.get();
                    if (caller != null && !caller.isFinishing()) {
                        // caller is not gone
                        caller.setSelectedCourse(null);
                    }
                }
            }
        } catch (NullPointerException e) {
            // old data -> do nothing
        }

        Intent intent = new Intent(RunActivity.this, ResultActivity.class);
        intent.putExtra("resultCourse", resultCourse);
        intent.putExtra("orderedResultQuestions", orderedResultQuestions);
        RunActivity.this.startActivity(intent);

        Global.clearValuesForSurvival(getApplicationContext());
        finish();
    }

    private void makeBorgDialog(Date timestamp) {
        borgDialogisOpen = true;
        final Dialog borgDialog = new Dialog(this, R.style.Theme_AppCompat_Light_Dialog_Alert);
        borgDialog.setCancelable(false);
        borgDialog.setCanceledOnTouchOutside(false);

        LayoutInflater inflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_borg, (ViewGroup)findViewById(R.id.borgdialoglayout));
        borgDialog.setContentView(layout);
        final TextView textViewBorgDescription = (TextView) layout.findViewById(R.id.textViewBorgDescription);
        Button buttonBorg = (Button) layout.findViewById(R.id.buttonBorg);
        final SeekBar seekBar = (SeekBar) layout.findViewById(R.id.seekBarBorg);
        seekBar.setProgress(INITIAL_BORG_LEVEL-1);
        textViewBorgDescription.setText(INITIAL_BORG_LEVEL+"/10: " + getString(borgLevel2Description.get(INITIAL_BORG_LEVEL)));

        SeekBar.OnSeekBarChangeListener yourSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBark, int progress, boolean fromUser) {
                //add code here
                textViewBorgDescription.setText(String.valueOf(progress+1) + "/10: " + getString(borgLevel2Description.get(progress+1)));
            }
        };
        buttonBorg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int level = seekBar.getProgress()+1;
                setResultControlpoint(null, null, null, idx_currentCP, null, level, null, null);  // add level to current CP
                if (level >= MIN_SKIP_LEVEL) {
                    // skip next CP = add skipped ResultControlpoint and increase index by 2 instead of 1
                    ControlPoint skippedCP = controlPoints[idx_currentCP+1];
                    Set<ResultQuestion> fakeRQs = skippedCP.getQuestions() != null && skippedCP.getQuestions().size() != 0 ? new HashSet<ResultQuestion>() : null;
                    for (Question q: skippedCP.getQuestions()) {
                        ResultQuestion rq = new ResultQuestion();
                        rq.setCategory(q.getCategory());
                        rq.setDifficulty(q.getDifficulty());
                        rq.setText(q.getText());
                        rq.setCategory(q.getCategory());
                        rq.setType(q.getType());
                        rq.setResultAnswers(null);
                        rq.setAnsweredCorrectly(null);
                        fakeRQs.add(rq);
                    }
                    setResultControlpoint(skippedCP.getLocation(), timestamp, fakeRQs, idx_currentCP+1, false, null, true, false);  // next CP is skippable (can be overwritten if its still reached)
                    idxLastUnskippedControlpoint = idx_currentCP;
                    idx_currentCP += 2;

                    // tell user that he can skip
                    tellUserToSkipDialogIsOpen = true;
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(RunActivity.this);
                    builder.setMessage(idx_currentCP == controlPoints.length-1 ? getString(R.string.you_can_skip_finish) : getString(R.string.you_can_skip, idx_currentCP))
                            .setPositiveButton(getString(R.string.ok), null)
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    tellUserToSkipDialogIsOpen = false;
                                }
                            })
                            .show();
                } else {
                    idxLastUnskippedControlpoint = idx_currentCP;
                    idx_currentCP++;
                }
                repaintControlpoints();
                lastUnskippedCPTimestamp = timestamp;
                borgDialogisOpen = false;
                borgDialog.dismiss();
            }
        });
        seekBar.setOnSeekBarChangeListener(yourSeekBarListener);
        borgDialog.show();
    }

    private void reached(ControlPoint cp, final Location location, final Date timestamp, boolean startTrack, boolean finishTrack, boolean forced) {
        boolean openQuestionActivity = cp.getQuestions().size() != 0;
        boolean nextCPIsSkippable = !finishTrack && !startTrack && idx_currentCP < controlPoints.length - 2 && controlPoints[idx_currentCP+1].getSkippable();

        // stop cp detection when we reached the finish
        if (finishTrack) {
            running = false;
        }
        reachedLastCP = finishTrack;

        if (!boundToGPSService) {
            // upload location of CP when we reached it IF we don't get GPS locations anyway
            tryToReportPosition(controlPoints[idx_currentCP].getLocation(), timestamp);
            lastSent = timestamp;
            lastSentLocation = controlPoints[idx_currentCP].getLocation();
        }

        if (startTrack) {
            startTrack(currentCourseGameModus, timestamp, location);
        }

        if (forced){  // must be after startTrack() because startTrack initializes ResultControlpoints (which resets currentCPIsForceSkipped)
            currentCPIsForceSkipped = true;
        }

        vibrator.vibrate(VIBRATION_DURATION_IN_MILLIS);
        if (openQuestionActivity) {
            mpQuestions.seekTo(0);
            mpQuestions.start();

            leavingOk = true;
            Intent intent = new Intent(this, QuestionActivity.class);

            LinkedList<Question> llq = new LinkedList<>(cp.getQuestions());
            intent.putExtra("questions", llq);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // keep QuestionActivity if it already exists (not sure if necessary). TODO: Was wenn man in "blöden" Momenten die QA beendet?
            idx_CPCurrentQuestion = idx_currentCP;
            startActivityForResult(intent, REQUEST_RESULT_QUESTION);
            currentCPLocation = location;
            currentCPTimestamp = timestamp;
            storeValueWhileInBackground("idx_CPCurrentQuestion", idx_CPCurrentQuestion, true);
            storeValueWhileInBackground("currentCPLocation", currentCPLocation, true);
            storeValueWhileInBackground("currentCPTimestamp", currentCPTimestamp, true);
        } else {
            if (!finishTrack) {
                mpCPReached.seekTo(0);
                mpCPReached.start();
            } else {
                mpFinish.seekTo(0);
                mpFinish.start();
            }
            setResultControlpoint(location, timestamp, null, idx_currentCP, true, null, null, currentCPIsForceSkipped);  // (if questions are asked, add result after the answers are given)
            currentCPIsForceSkipped = false;
        }

        if (finishTrack) {
            if (!lastCPHasQuestions) {
                // if no questions remain: finish the track. (else this is done in onActivityResult)
                finishTrack(currentCourseGameModus, timestamp);
            }
        }
        // Skipping is possible if (a) we are neither at start nor at finish, nor at finish-1 (b) the next CP is skippable and (c) skipping conditions are met.
        if (nextCPIsSkippable) { // && Skipping.shouldSkip(controlPoints[idx_currentCP], controlPoints[idxLastUnskippedControlpoint], lastUnskippedCPTimestamp, timestamp)) {
            // Open borg dialog
            makeBorgDialog(timestamp);
        } else {
            // no skipping is possible
            idxLastUnskippedControlpoint = idx_currentCP;
            idx_currentCP++;
            storeValueWhileInBackground("idxLastUnskippedControlpoint", idxLastUnskippedControlpoint, true);
            storeValueWhileInBackground("idx_currentCP", idx_currentCP, true);
        }

        repaintControlpoints();
        lastUnskippedCPTimestamp = timestamp;
        storeValueWhileInBackground("lastUnskippedCPTimestamp", lastUnskippedCPTimestamp, true);
    }

    @Override
    public void onBackPressed() {
        if (running) {
            // are-you-sure-dialog if the user wants to quit while making a run
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setMessage(getString(R.string.do_you_really_want_to_stop_run))
                    .setPositiveButton(getString(R.string.continue_run), null)
                    .setNegativeButton(getString(R.string.cancel_run), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            // "Yes" button was clicked
                            finish();
                        }
                    })
                    .show();
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_ENABLE_GPS_WINDOW:
                switch (currentCourseGameModus) {
                    case GPS:
                        onLocationAvailability();  // re-check and repeat dialog in GPS case if user does not enable GPS
                        break;

                    case QRCODE:
                        if (isLocationEnabled()) {
                            // user enabled GPS upon request -> enable GPS
                            enableGPSInQRMode();
                        }
                        // if user didnt enable GPS, do nothing
                        break;
                }
                break;

            case REQUEST_RESULT_QUESTION:
                leavingOk = false;
                List<ResultQuestion> resultsQuestion = new ArrayList<>();
                if (data != null) {  // does this case occur (pressing back on QA is disallowed)
                    resultsQuestion = Global.gsonInst.fromJson(data.getStringExtra("resultsQuestion"), new TypeToken<ArrayList<ResultQuestion>>() {}.getType());
                }

                orderedResultQuestions.addAll(resultsQuestion);

                setResultControlpoint(currentCPLocation, currentCPTimestamp, new HashSet(resultsQuestion), idx_CPCurrentQuestion, true, null, null, currentCPIsForceSkipped);
                currentCPIsForceSkipped = false;
                if (reachedLastCP)
                    // finish the track after last questions (on  finish CP) are answered
                    finishTrack(currentCourseGameModus, Global.now());
                break;

            case IntentIntegrator.REQUEST_CODE:
                // Process the QR code scan results, see https://zxing.github.io/zxing/apidocs/com/google/zxing/integration/android/IntentIntegrator.html
                Date timestamp = Global.now();
                IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                if (result != null && result.getContents() != null) {
                    boolean previousCPIsSkippable = idxLastUnskippedControlpoint == idx_currentCP - 2;
                    boolean cpReached = result.getContents().equals(controlPoints[idx_currentCP].getQrCode());
                    boolean previousCPReached = idx_currentCP >= 1 ? result.getContents().equals(controlPoints[idx_currentCP-1].getQrCode()) : false;
                    if (cpReached || (previousCPIsSkippable && previousCPReached)) {
                        if (!cpReached) {
                            idx_currentCP = idx_currentCP-1;
                        }
                        if (idx_currentCP == 0) {
                            // start CP
                            Global.snack(this, getString(R.string.start_your_run), Snackbar.LENGTH_LONG);
                            reached(controlPoints[idx_currentCP], controlPoints[idx_currentCP].getLocation(), timestamp, true, false, false);
                        } else if (idx_currentCP == controlPoints.length - 1) {
                            // finish CP
                            reached(controlPoints[idx_currentCP], controlPoints[idx_currentCP].getLocation(), timestamp, false, true, false);
//                            text = "Congratulation! You completed the run!";
                        } else {
                            // regular CP
                            Global.snack(this, getString(R.string.regular_cp_reached, idx_currentCP), Snackbar.LENGTH_LONG);
                            reached(controlPoints[idx_currentCP], controlPoints[idx_currentCP].getLocation(), timestamp, false, false, false);
                        }
                    }
                    else
                    {
                        Global.snack(this, getString(R.string.wrong_cp), Snackbar.LENGTH_LONG);
                    }
                } else {
                    Global.snack(this, getString(R.string.no_cp_qr_code_scanned), Snackbar.LENGTH_LONG);
                }
                break;
        }
    }

    //Neue Position abgerufen --> Check ob Checkpunkt erreicht ist und Updaten der Texte
    private void checkForControlpoint(Location location) {
        Date timestamp = Global.now();

        if (currentCourseGameModus != GameModus.GPS) {
            return;
        }

        if (idx_currentCP >= controlPoints.length) {
            return;
        }

        float distance = location.distanceTo(controlPoints[idx_currentCP].getLocation());
        boolean cpReached = distance < controlPoints[idx_currentCP].getRadius();

        boolean previousCPIsSkippable = idxLastUnskippedControlpoint == idx_currentCP - 2;
        boolean previousCPReached = previousCPIsSkippable ? location.distanceTo(controlPoints[idx_currentCP-1].getLocation()) < controlPoints[idx_currentCP-1].getRadius() : false;

        if (currentCourseGameModus == GameModus.GPS && !QuestionActivity.isRunning && !borgDialogisOpen && !tellUserToSkipDialogIsOpen) {  // only recognize control points while not answering questions. TODO: ist das gut?
            if (cpReached || (previousCPIsSkippable && previousCPReached)) {
                if (!cpReached) {
                    // the skippable CP has been reached
                    idx_currentCP = idx_currentCP-1;
                    storeValueWhileInBackground("idx_currentCP", idx_currentCP, true);
                }

                // we are at SOME control point
                if (idx_currentCP == 0) {
                    // we are at the starting point
                    startingLocation = location;

                    if (!isAtStart) {
                        // we have not already been at the starting point during the previous call of this function (= we "reached" the starting point)
                        vibrator.vibrate(VIBRATION_DURATION_IN_MILLIS);
                        mpCPReached.seekTo(0);
                        mpCPReached.start();

                        isAtStart = true;
                        buttonStart.setEnabled(true);

                        Global.snack(this, getString(R.string.at_start), Snackbar.LENGTH_LONG);
                    }
                }
                else {
                    // we are at a control point other than the starting point
                    isAtStart = false;
                    buttonStart.setEnabled(false);

                    if (running && (idx_currentCP == controlPoints.length-1)) {
                        // we are at the finish point
                        reached(controlPoints[idx_currentCP], location, timestamp, false, true, false);  // timestamp umwandeln in location.gettime oder so
                    }
                    else if (running) {
                        // we are at a regular checkpoint
                        Global.snack(this, getString(R.string.regular_cp_reached, idx_currentCP), Snackbar.LENGTH_LONG);
                        reached(controlPoints[idx_currentCP], location, timestamp, false, false, false);
                    }
                }
            } else {
                // we are at no checkpoint
                isAtStart = false;
                buttonStart.setEnabled(false);
            }
        }
    }

    void setResultControlpoint(Location location, Date timestamp, Set<ResultQuestion> resultQuestions, Integer index, Boolean reached, Integer borgScale, Boolean skipAllowed, Boolean forceSkipped) {
        ResultControlpoint res = new ResultControlpoint();
        for (ResultControlpoint rc : resultCourse.getResultControlpoints()) {
            if (rc.getSequence().equals(index)) {
                res = rc;
                resultCourse.getResultControlpoints().remove(rc);
                break;
            }
        }

        res.setId(null);
        if (location != null) {
            res.setLatitude(location.getLatitude());
            res.setLongitude(location.getLongitude());
        }
        if (index != null) res.setSequence(index);
        if (timestamp != null) res.setTimeReached(Global.dateToString(timestamp));  // TODO: set to location.gettime
        if (resultQuestions != null) res.setResultQuestions(resultQuestions);
        if (reached != null) res.setReached(reached);
        if (borgScale != null) res.setBorgScale(borgScale);
        if (skipAllowed != null) res.setSkipAllowed(skipAllowed);
        if (forceSkipped != null) res.setForceSkipped(forceSkipped);

        resultCourse.getResultControlpoints().add(res);
    }

    //region Methods for Permissions
    public void requestLocationPermissions() {
        ActivityCompat.requestPermissions(
                RunActivity.this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                RunActivity.REQUEST_LOCATION_PERMISSION
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for (int g : grantResults) {
            if (g == PackageManager.PERMISSION_DENIED) {
                // Location permission denied by user
                if (currentCourseGameModus == GameModus.GPS) {
                    // Quit RunActivity as GPS is mandatory
                    finish();
                }
                disableGPSInQRMode();
                return;
            }
        }

        // Permissions granted but location access may still be missing
        if (!isLocationEnabled()) {
            switchGPS.setChecked(false);
            if (currentCourseGameModus == GameModus.GPS) {
                onLocationAvailability();
            } else {
                makeQRLocationEnablementDialog();
            }
        } else {
            enableGPSInQRMode();
        }
    }

    boolean areLocationPermissionsGranted() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    //endregion
}
