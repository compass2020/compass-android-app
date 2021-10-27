/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.global;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import at.univie.compass.BuildConfig;
import at.univie.compass.R;
import at.univie.compass.activity.RunActivity;
import at.univie.compass.dto.ResultCourse;
import at.univie.compass.dto.SharedCourse;


public class Global {
    // Server info
    public static String DEFAULT_BACKEND_HOST = "compass.schmelz.univie.ac.at";
    public static int BACKEND_PORT = 443;
    public static int WEBSITE_PORT = 443;
    public static boolean USE_HTTPS = true;

    // Use for local testing without https
//    public static String DEFAULT_BACKEND_HOST = "some.local.ip";
//    public static int BACKEND_PORT = 8080;
//    public static int WEBSITE_PORT = 9000;
//    public static boolean USE_HTTPS = false;

    // Continuous position reporting to the server
    public static final int POSITION_REPORTING_INTERVAL = 10000;  // milliseconds!

    // Codes for login return
    public static String LOGIN_UPDATE_REQUIRED = "at.univie.compass.LOGIN_UPDATE_REQUIRED";
    public static String LOGIN_UPDATE_RECOMMENDED = "at.univie.compass.LOGIN_UPDATE_RECOMMENDED";
    public static String BACKEND_KEY_REQUIRED = "LOGIN_UPDATE_REQUIRED";
    public static String BACKEND_HEADER_RECOMMENDED = "LOGIN_UPDATE_RECOMMENDED";
    public static String PLAYSTORE_LINK = "https://play.google.com/store/apps/details?id=at.univie.compass";

    public static String WEBSITE_VIEW = "/view/%s/%s";
    public static String WEBSITE_MANAGE_COURSES_FILE = "/my-courses";
    public static String BACKEND_USER = "android";
    public static String BACKEND_USER_PASSWORD = "hidden";

    public static boolean UPLOAD_INTERMEDIATE_GPX_RESULTS = false;  // upload unsmoothed gpx files + ones with different test filters (instead of only the final best result.) -> false for prodution
    public static boolean VERBOSE_GPX = false;  // false for production

    public static int MAX_TILE_ZOOM_LEVEL = 17;
    public static String[] TILESERVER_URLS = {"https://a.tile.opentopomap.org/", "https://b.tile.opentopomap.org/", "https://c.tile.opentopomap.org/"};
    public static final int NTHREADS_TILE_DOWNLOAD = 7;  // number of simultaneous threads to download + cache tiles

    // API
    public static final String API_LOGIN = "/api/authenticate";
    public static final String API_SHARED_COURSE = "/api/shared-courses/qrcode/%s";
    public static final String API_COURSE_RESULT = "/api/result-courses";
    public static final String API_POSITION = "/api/position";

    // Identifiers
    public static final String SHARED_PREFERENCES_FILE = "at.univie.compass.SHARED_PREFERENCES";
    public static final String SURVIVAL_SHARED_PREFERENCES = "at.univie.compass.SURVIVAL_SHARED_PREFERENCES";
    public static final String NICKNAME_DATA = "at.univie.compass.NICKNAME_DATA";
    public static final String BLE_ADDRESS = "at.univie.compass.BLE_ADDRESS";
    public static final String BLE_NAME = "at.univie.compass.BLE_NAME";
    public static final String GUID_DATA = "at.univie.compass.GUID_DATA";
    public static final String COURSE_DATA = "at.univie.compass.COURSE_DATA";  // to persistently store course in SharedPreferences
    public static final String IMG_OVERLAY_DATA = "at.univie.compass.IMG_OVERLAY_DATA";
    public static final String KML_OVERLAY_DATA = "at.univie.compass.KML_OVERLAY_DATA";
    public static final String RESULT_DATA = "at.univie.compass.RESULT_DATA";  // to persistently store results
    public static final String VIEW_CODES_DATA = "at.univie.compass.VIEW_CODES_DATA";
    public static final String SURVIVAL_DATA = "at.univie.compass.SURVIVAL_DATA.";
    public static final String UPLOAD_WORK_ID = "at.univie.compass.UPLOAD_RESULT_WORK";  // to identify only-once work request
    public static final String REPORT_POSITION_WORK_ID = "at.univie.compass.REPORT_POSITION_WORK_ID";  // to identify only-once work request
    public static final String PROCESS_WORK = "at.univie.compass.PROCESS_RESULT_WORK";  // to identify only-once work request

    public static final int LIST_SURVIVAL_CHUNK_SIZE = 5;

    // For backend access
    public static String authToken = null;

    // Location
    private final static double EARTH_RADIUS_IN_METERS = 6378137;

    // GSON, required bc Android may yield NaN for gps speed (undocumented behaviour) https://stackoverflow.com/a/59679002
    public static Gson gsonInst = new GsonBuilder().serializeSpecialFloatingPointValues().create();

    //region Device name
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
    //endregion

    public static Integer getVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    public static String getVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    public static void snack(AppCompatActivity caller, String msg, int length) {
        CoordinatorLayout coordinatorLayout = caller.findViewById(R.id.coordinatorLayout);
        Snackbar snack = Snackbar.make(coordinatorLayout, msg, length);
        View view = snack.getView();
        CoordinatorLayout.LayoutParams params =(CoordinatorLayout.LayoutParams)view.getLayoutParams();
        params.gravity = Gravity.TOP;
        view.setLayoutParams(params);
        snack.show();
        TextView tv = (TextView) view.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setMaxLines(10);
    }

    public static void authFailedDialog(Activity caller, String result, boolean alsoRecommend) {
        if (result.equals(Global.LOGIN_UPDATE_REQUIRED)) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(caller);
            builder.setMessage(caller.getString(R.string.update_required))
                    .setPositiveButton(caller.getString(R.string.update), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            caller.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Global.PLAYSTORE_LINK)));
                            caller.finish();
                        }
                    })
                    .setNegativeButton(caller.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            caller.finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
        } else if (result.equals(Global.LOGIN_UPDATE_RECOMMENDED) && alsoRecommend) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(caller);
            builder.setMessage(caller.getString(R.string.update_recommended))
                    .setPositiveButton(caller.getString(R.string.update), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            caller.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Global.PLAYSTORE_LINK)));
                        }
                    })
                    .setNegativeButton(caller.getString(R.string.cancel), null)
                    .show();
        }
    }

    //region Location
    // https://stackoverflow.com/a/7478827
    public static double longitude_to_metres(double longitudes, double latitude) {
        return (2*Math.PI/360) * EARTH_RADIUS_IN_METERS * Math.cos(latitude);
    }

    // derived from https://stackoverflow.com/a/7478827
    public static double meters_to_longitude(double meters_in_lon_direction, double latitude) {
        return meters_in_lon_direction / ((2*Math.PI/360) * EARTH_RADIUS_IN_METERS * Math.cos(latitude));
    }

    // https://stackoverflow.com/a/7478827
    public static double latitude_to_meters(double latitudes) {
        return (2*Math.PI/360) * EARTH_RADIUS_IN_METERS;
    }

    // derived from https://stackoverflow.com/a/7478827
    public static double meters_to_latitude(double meters_in_lat_direction) {
        return meters_in_lat_direction / ((2*Math.PI/360) * EARTH_RADIUS_IN_METERS);
    }
    //endregion

    //region Date and time
    public static String locationToTimeString(Location location) {
        final DateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.ENGLISH); // new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSZ"); // need: "2020-06-25T09:17:10.663+02:00"
        Date date = new Date(location.getTime());
        return DATETIME_FORMAT.format(date);
    }

    public static String dateToString(Date date) {
        // 2020-12-03T07:39:01.108+01:00
        final DateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.ENGLISH); // new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSZ"); // need: "2020-06-25T09:17:10.663+02:00"
        return DATETIME_FORMAT.format(date);
    }

    public static Date stringToDateNoMillis(String dateStr) throws ParseException {
        // 2020-12-03T07:39:01.108+01:00
        final DateFormat DATETIME_FORMAT =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.ENGLISH);
        return DATETIME_FORMAT.parse(dateStr);
    }

    public static String dateToStringForPositionDTO(Date date) {
        // 2020-12-02T13:33:15.216Z
        DateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH); // new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSZ"); // need: "2020-06-25T09:17:10.663+02:00"
        DATETIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        return DATETIME_FORMAT.format(date);
    }

    public static Date now() {
        return Calendar.getInstance().getTime();
    }
    //endregion

    class LoginResponse implements Serializable {
        String type;
        String title;
        String message;
        String detail;
        Integer status;
    }

    public static String getAuthenticationToken(String user, String password, Boolean onStartup) throws IOException {
        if (authToken != null)
            return authToken;

        // Uses HttpURLConnection to recreate the POST request that is sent to the JHipster backend if you login via browser.
        // https://stackoverflow.com/a/2938787
        HttpURLConnection con = null;// = (HttpURLConnection) auth_url.openConnection();
        if (Global.USE_HTTPS) {
            URL auth_url = new URL("https", Global.DEFAULT_BACKEND_HOST, Global.BACKEND_PORT, Global.API_LOGIN);
            con = (HttpsURLConnection) auth_url.openConnection();
        } else {
            URL auth_url = new URL("http", Global.DEFAULT_BACKEND_HOST, Global.BACKEND_PORT, Global.API_LOGIN);
            con = (HttpURLConnection) auth_url.openConnection();
        }

        con.setRequestMethod("POST");

        // Set headers and body
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");

        final String androidVersion = Global.getDeviceName() + "/" + "Android " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")";
        con.setRequestProperty("User-Agent", androidVersion);
        String data = "{\"username\":\"" + user + "\",\"password\":\"" + password + "\",\"rememberMe\":false,\"version\":\"" + androidVersion + "\",\"appVersion\":\"" + Global.getVersionName() + "(" + Global.getVersionCode() + ")\"}";

        // Write body
        con.setDoOutput(true);
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = data.getBytes("utf-8");
            os.write(input, 0, input.length);
        } catch (IOException ignored) {
        }

        // Connect + retrieve token
        authToken = con.getHeaderField("Authorization");

        // Extract login info
        if (onStartup) {
            int responseCode = con.getResponseCode();

            if (responseCode == 400) {

                if (onStartup && con != null && con.getResponseCode() == 400) {
                    BufferedReader br = new BufferedReader(new InputStreamReader((con.getErrorStream())));
                    StringBuilder sb = new StringBuilder();
                    String output;
                    while ((output = br.readLine()) != null) {
                        sb.append(output);
                    }
                    LoginResponse r = Global.gsonInst.fromJson(sb.toString(), LoginResponse.class);


                    if (r.title.equals(Global.BACKEND_KEY_REQUIRED)) {
                        return Global.LOGIN_UPDATE_REQUIRED;
                    }
                    return null;
                }
            }
            if (con.getHeaderField(Global.BACKEND_HEADER_RECOMMENDED) != null) {
                return Global.LOGIN_UPDATE_RECOMMENDED;
            }
        }

        return authToken;

    }

    // https://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences
    public static SharedPreferences.Editor putDouble(final SharedPreferences.Editor edit, final String key, final double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }
    public static double getDouble(final SharedPreferences prefs, final String key, final double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }

    public static void storeListForSurvival(Context applicationContext, String key, ArrayList<Object> list) {
        if (list.size() == 0) {
            return;
        }
        // Obtain next file number
        SharedPreferences sharedPrefGlobal = applicationContext.getSharedPreferences(Global.SURVIVAL_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editorGlobal = sharedPrefGlobal.edit();
        int file_number = sharedPrefGlobal.getInt(Global.SURVIVAL_DATA + key + "file_number", -1);  // default case: start the first chunk with file_number 0
        if (file_number == -1) {
            file_number = 0;
            editorGlobal.putInt(Global.SURVIVAL_DATA + key + "file_number", 0);
        }

        // Open file
        SharedPreferences sharedPrefChunk = applicationContext.getSharedPreferences(Global.SURVIVAL_SHARED_PREFERENCES + file_number, Context.MODE_PRIVATE);
        SharedPreferences.Editor editorChunk = sharedPrefChunk.edit();

        // Store relevant slice of data (= slice which has not been saved yet)
        int first_index = sharedPrefGlobal.getInt(Global.SURVIVAL_DATA + key + "last_stored_index", 0);  // default case: store entire list (first_index = 0)
        int last_index = list.size();
        int chunk_length = sharedPrefChunk.getInt(Global.SURVIVAL_DATA + key + "chunk_length", 0);  // default case: nothing stored yet so chunk_length is 0
        String json = Global.gsonInst.toJson(list.subList(first_index, last_index));
        editorChunk.putString(Global.SURVIVAL_DATA + key + "*" + first_index, json);

        int new_chunk_length = chunk_length + (last_index - first_index);

        // Increment file counter if stored data exceeds threshold
        if (new_chunk_length > Global.LIST_SURVIVAL_CHUNK_SIZE) {
            editorGlobal.putInt(Global.SURVIVAL_DATA + key + "file_number", file_number+1);
        }

        // Update remaining bookkeeping data
        editorChunk.putInt(Global.SURVIVAL_DATA + key + "chunk_length", new_chunk_length);
        editorGlobal.putInt(Global.SURVIVAL_DATA + key + "last_stored_index", last_index);

        editorChunk.apply();
        editorGlobal.apply();
    }

    public static ArrayList<Object> getListForSurvival(Context applicationContext, String key, Type c) {
        SharedPreferences sharedPrefGlobal = applicationContext.getSharedPreferences(Global.SURVIVAL_SHARED_PREFERENCES, Context.MODE_PRIVATE);

        // Retrieve bookkeeping info to figure out where to look for the sublists
        int last_stored_index = sharedPrefGlobal.getInt(Global.SURVIVAL_DATA + key + "last_stored_index", -1);  // default case: store entire list (first_index = 0)
        if (last_stored_index == -1) {
            // nothing stored
            return null;
        }
        int last_stored_file_number = sharedPrefGlobal.getInt(Global.SURVIVAL_DATA + key + "file_number", -1);
        if (last_stored_file_number == -1) {
            // nothing stored
            return null;
        }

        // Retrieve sublists and compose result list
        ArrayList<Object> result = new ArrayList<>();
        int current_index = 0;
        for (int file_nr = 0; file_nr <= last_stored_file_number; file_nr++) {
            // Read data from next chunk
            SharedPreferences sharedPrefChunk = applicationContext.getSharedPreferences(Global.SURVIVAL_SHARED_PREFERENCES + file_nr, Context.MODE_PRIVATE);

            while (true) {
                String list_segment_json = sharedPrefChunk.getString(Global.SURVIVAL_DATA + key + "*" + current_index, "");
                if (list_segment_json.equals("")) {
                    break;
                }
                ArrayList<Object> segment = Global.gsonInst.fromJson(list_segment_json, c);
                if (segment.size() == 0) {
                    break;
                }
                result.addAll(segment);
                current_index = current_index + segment.size();
            }
        }
        return result;
    }

    public static Object getLocationForSurvival(Context applicationContext, String key) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SURVIVAL_SHARED_PREFERENCES, Context.MODE_PRIVATE);

        if (sharedPref.contains(Global.SURVIVAL_DATA + key + ".getAccuracy")) {
            Location l = new Location("");
            l.setAccuracy(sharedPref.getFloat(Global.SURVIVAL_DATA + key + ".getAccuracy", 0));
            l.setBearing(sharedPref.getFloat(Global.SURVIVAL_DATA + key + ".getBearing", 0));
            l.setElapsedRealtimeNanos(sharedPref.getLong(Global.SURVIVAL_DATA + key + ".getElapsedRealtimeNanos", 0));
            l.setTime(sharedPref.getLong(Global.SURVIVAL_DATA + key + ".getTime", 0));
            l.setProvider(sharedPref.getString(Global.SURVIVAL_DATA + key + ".getProvider", ""));
            l.setSpeed(sharedPref.getFloat(Global.SURVIVAL_DATA + key + ".getSpeed", 0));
            l.setLongitude(Global.getDouble(sharedPref, Global.SURVIVAL_DATA + key + ".getLongitude", 0));
            l.setLatitude(Global.getDouble(sharedPref, Global.SURVIVAL_DATA + key + ".getLatitude", 0));
            l.setAltitude(Global.getDouble(sharedPref, Global.SURVIVAL_DATA + key + ".getAltitude", 0));
            return l;
        }
        return null;
    }

    public static void storeLocationForSurvival(Context applicationContext, String key, Location l) {
        synchronized (Global.class) {
            try {
                SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SURVIVAL_SHARED_PREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                editor.putFloat(Global.SURVIVAL_DATA + key + ".getAccuracy", l.getAccuracy());
                editor.putFloat(Global.SURVIVAL_DATA + key + ".getBearing", l.getBearing());
                editor.putLong(Global.SURVIVAL_DATA + key + ".getElapsedRealtimeNanos", l.getElapsedRealtimeNanos());
                editor.putLong(Global.SURVIVAL_DATA + key + ".getTime", l.getTime());
                Global.putDouble(editor, Global.SURVIVAL_DATA + key + ".getAltitude", l.getAltitude());
                Global.putDouble(editor, Global.SURVIVAL_DATA + key + ".getLongitude", l.getLongitude());
                Global.putDouble(editor, Global.SURVIVAL_DATA + key + ".getLatitude", l.getLatitude());
                editor.putString(Global.SURVIVAL_DATA + key + ".getProvider", l.getProvider());
                editor.putFloat(Global.SURVIVAL_DATA + key + ".getSpeed", l.getSpeed());

                editor.apply();
            } catch (Exception ignored) {
            }
        }
    }

    //region Access to SharedPreferences
    public static void storeValueForSurvival(Context applicationContext, String key, Object s) {
        synchronized (Global.class) {
            try {
                SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SURVIVAL_SHARED_PREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                String json = Global.gsonInst.toJson(s);
                editor.putString(Global.SURVIVAL_DATA + key, json);
                editor.apply();
            } catch (Exception ignored) {
            }
        }
    }

    public static Object getValueForSurvival(Context applicationContext, String key, Type c) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SURVIVAL_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        String result_json = sharedPref.getString(Global.SURVIVAL_DATA + key, "");
        return Global.gsonInst.fromJson(result_json, c);
    }

    public static String[] storableMembersOfLocation = {
            ".getAccuracy",
            ".getBearing",
            ".getElapsedRealtimeNanos",
            ".getTime",
            ".getAltitude",
            ".getLongitude",
            ".getLatitude",
            ".getProvider",
            ".getSpeed"
    };

    public static void clearValuesForSurvival(Context applicationContext) {
        SharedPreferences sharedPrefGlobal = applicationContext.getSharedPreferences(Global.SURVIVAL_SHARED_PREFERENCES, Context.MODE_PRIVATE);

        // Clear list chunks
        for (String key: RunActivity.survivingListKeys) {
            int last_stored_file_number = sharedPrefGlobal.getInt(Global.SURVIVAL_DATA + key + "file_number", 0);
            for (int file_nr = 0; file_nr <= last_stored_file_number; file_nr++) {
                SharedPreferences sharedPrefChunk = applicationContext.getSharedPreferences(Global.SURVIVAL_SHARED_PREFERENCES + file_nr, Context.MODE_PRIVATE);
                SharedPreferences.Editor editorChunk = sharedPrefChunk.edit();
                editorChunk.clear();
                editorChunk.apply();
            }
        }

        // Clear everything else
        SharedPreferences.Editor editorGlobal = sharedPrefGlobal.edit();
        editorGlobal.clear();
        editorGlobal.apply();
    }

    public static void storeResult(Context applicationContext, ResultCourse resultCourse, boolean pending) {
        resultCourse.pending = pending;
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        Gson gson = Global.gsonInst;
        String course_json;
        if (getStoredResults(applicationContext) == null) {
            course_json = gson.toJson(new ResultCourse[]{resultCourse});
        } else {
            ArrayList<ResultCourse> rc = new ArrayList<>(Arrays.asList(getStoredResults(applicationContext)));
            rc.add(resultCourse);
            course_json = gson.toJson(rc.toArray());
        }
        editor.putString(Global.RESULT_DATA, course_json);
        editor.apply();
    }

    public static ResultCourse[] getStoredResults(Context applicationContext) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        Gson gson = Global.gsonInst;
        String result_json = sharedPref.getString(Global.RESULT_DATA, "");

        return gson.fromJson(result_json, ResultCourse[].class);
    }

    public static void deleteAllStoredResults(Context applicationContext) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(Global.RESULT_DATA);
        editor.apply();
    }

    public static void deleteStoredResult(Context applicationContext, int index) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);

        ResultCourse[] rcs = getStoredResults(applicationContext);
        if (rcs != null) {
            SharedPreferences.Editor editor = sharedPref.edit();
            if (rcs.length == 1 && index == 0) {
                editor.remove(Global.RESULT_DATA);
            } else {
                ArrayList<ResultCourse> rc = new ArrayList<>(Arrays.asList(rcs));
                rc.remove(index);
                Gson gson = Global.gsonInst;
                String course_json = gson.toJson(rc.toArray());
                editor.putString(Global.RESULT_DATA, course_json);
            }
            editor.apply();
        }
    }

    public static void storeNickname(Context applicationContext, String nickname) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Global.NICKNAME_DATA, nickname);
        editor.apply();
    }

    public static String getStoredNickname(Context applicationContext) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        return sharedPref.getString(Global.NICKNAME_DATA, "");
    }

    public static void deleteStoredNickname(Context applicationContext) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(Global.NICKNAME_DATA);
        editor.apply();
    }

    public static String getGuid(Context applicationContext) {
        String guid = getStoredGuid(applicationContext);
        if (guid.equals("")) {
            guid = UUID.randomUUID().toString();
            storeGuid(applicationContext, guid);
            return guid;
        }
        return guid;
    }

    public static String getStoredGuid(Context applicationContext) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        return sharedPref.getString(Global.GUID_DATA, "");
    }

    public static void storeGuid(Context applicationContext, String guid) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Global.GUID_DATA, guid);
        editor.apply();
    }

    public static void storeCourse(Context applicationContext, SharedCourse course) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        Gson gson = Global.gsonInst;

        byte[] imgOverlayBytes = course.getCourse().getOrienteeringMap().getMapOverlayImage();
        if (imgOverlayBytes != null) {
            String overlayStr = Base64.encodeToString(imgOverlayBytes, Base64.NO_WRAP);
            editor.putString(Global.IMG_OVERLAY_DATA, overlayStr);
        } else {
            editor.putString(Global.IMG_OVERLAY_DATA, "");
        }
        byte[] kmlOverlayBytes = course.getCourse().getOrienteeringMap().getMapOverlayKml();
        if (kmlOverlayBytes != null) {
            String kmlOverlayStr = Base64.encodeToString(kmlOverlayBytes, Base64.NO_WRAP);
            editor.putString(Global.KML_OVERLAY_DATA, kmlOverlayStr);
        } else {
            editor.putString(Global.KML_OVERLAY_DATA, "");
        }

        String course_json = gson.toJson(course);
        editor.putString(Global.COURSE_DATA, course_json);
        editor.apply();
    }

    public static void deleteStoredCourse(Context applicationContext) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(Global.COURSE_DATA);
        editor.remove(Global.IMG_OVERLAY_DATA);
        editor.remove(Global.KML_OVERLAY_DATA);
        editor.apply();
    }

    private static byte[] getStoredImgOverlay(Context applicationContext) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        String imgOverlayStr = sharedPref.getString(Global.IMG_OVERLAY_DATA, "");
        if (!imgOverlayStr.equals("")) {
            return Base64.decode(imgOverlayStr, Base64.NO_WRAP);
        }
        return null;
    }

    private static byte[] getStoredKmlOverlay(Context applicationContext) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        String kmlOverlayStr = sharedPref.getString(Global.KML_OVERLAY_DATA, "");
        if (!kmlOverlayStr.equals("")) {
            return Base64.decode(kmlOverlayStr, Base64.NO_WRAP);
        }
        return null;
    }

    public static SharedCourse getStoredCourse(Context applicationContext) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        Gson gson = Global.gsonInst;
        String course_json = sharedPref.getString(Global.COURSE_DATA, "");
        SharedCourse retrievedCourse = gson.fromJson(course_json, SharedCourse.class);
        if (retrievedCourse != null && retrievedCourse.getCourse() != null && retrievedCourse.getCourse().getOrienteeringMap() != null) {
            retrievedCourse.getCourse().getOrienteeringMap().setMapOverlayImage(getStoredImgOverlay(applicationContext));
            retrievedCourse.getCourse().getOrienteeringMap().setMapOverlayKml(getStoredKmlOverlay(applicationContext));
        }
        return retrievedCourse;
    }

    public static String getStoredBLEaddress(Context applicationContext) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        return sharedPref.getString(Global.BLE_ADDRESS, "");
    }

    public static void storeBLEaddress(Context applicationContext, String bleAddress) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Global.BLE_ADDRESS, bleAddress);
        editor.apply();
    }

    public static void storeBLEname(Context applicationContext, String bleName) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Global.BLE_NAME, bleName);
        editor.apply();
    }

    public static String getStoredBLEname(Context applicationContext) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        return sharedPref.getString(Global.BLE_NAME, "");
    }

    public static void storeViewCode(Context applicationContext, ResultCourse resultCourse) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        Gson gson = Global.gsonInst;
        String course_json;
        ResultCourse[] storedViewCodes = getStoredViewCodes(applicationContext);
        if (storedViewCodes == null) {
            course_json = gson.toJson(new ResultCourse[]{resultCourse});
        } else {
            ArrayList<ResultCourse> rc = new ArrayList<>(Arrays.asList(storedViewCodes));
            rc.add(resultCourse);
            course_json = gson.toJson(rc.toArray());
        }
        editor.putString(Global.VIEW_CODES_DATA, course_json);
        editor.apply();
    }

    public static ResultCourse[] getStoredViewCodes(Context applicationContext) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        Gson gson = Global.gsonInst;
        String result_json = sharedPref.getString(Global.VIEW_CODES_DATA, "");
        return gson.fromJson(result_json, ResultCourse[].class);
    }

    public static void deleteAllStoredViewCodes(Context applicationContext) {
        SharedPreferences sharedPref = applicationContext.getSharedPreferences(Global.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(Global.VIEW_CODES_DATA);
        editor.apply();
    }

    //endregion

}
