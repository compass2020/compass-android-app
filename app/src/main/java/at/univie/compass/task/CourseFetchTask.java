/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.task;

// TODO: manchmal geht das Kurs scannen einfach nicht und dann doch beim 2. versuch obwohl man nix geändert hat, wahrscheinlich hatte man eine falsche ip und da läuft dann noch der alte CourseFetchTask -> gscheites finishen des CFT bei Netzwerk-Fehlern zB falsche IP

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import at.univie.compass.global.Global;
import at.univie.compass.R;
import at.univie.compass.activity.MainActivity;
import at.univie.compass.dto.SharedCourse;

public class CourseFetchTask extends AsyncTask<String, Void, SharedCourse> {
    /* Take SharedCourse.Id as String, return SharedCourse from server */
    private RestTemplate restTemplate = new RestTemplate();
    private WeakReference<MainActivity> callerMainActivity;
    private Exception exception;
    private Integer specialErrorMsgStringId;
    private String specialErrorMsgStringArgument;

    public CourseFetchTask(MainActivity caller) {
        callerMainActivity = new WeakReference<>(caller);
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
    }

    class CourseFetchResponse400 implements Serializable {
        String type;
        String title;
        String message;
        String detail;
        Integer status;
    }

    @Override
    protected SharedCourse doInBackground(String... sharedCourseQRCodes) {
        if (isCancelled()) {
            return null;
        }

        String sharedCourseQRCode = sharedCourseQRCodes[0];  // only 1 call per task instance

        specialErrorMsgStringId = null;
        specialErrorMsgStringArgument = null;

        // Set authorization header
        String authToken;
        try {
            authToken = Global.getAuthenticationToken(Global.BACKEND_USER, Global.BACKEND_USER_PASSWORD, false);
        } catch (IOException e) {
            exception = e;
            return null;
        }

        if (isCancelled()) {
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        HttpEntity entity = new HttpEntity(headers);

        // fetch course from server
        ResponseEntity<SharedCourse> responseSharedCourse = null;
        try {
            responseSharedCourse = restTemplate.exchange(new URL(Global.USE_HTTPS ? "https" : "http", Global.DEFAULT_BACKEND_HOST, Global.BACKEND_PORT, String.format(Global.API_SHARED_COURSE, sharedCourseQRCode)).toString(), HttpMethod.GET, entity, SharedCourse.class);
        } catch (HttpStatusCodeException e) {
            int statusCode = e.getStatusCode().value();
            String body = e.getResponseBodyAsString(); // TODO: use body to detect QrCode already scanned
            exception = e;
            if (statusCode == 400) {
                CourseFetchResponse400 r = Global.gsonInst.fromJson(body, CourseFetchResponse400.class);
                if (r.title.equals("QRCODE_ALREADY_SCANNED")) {
                    specialErrorMsgStringId = R.string.error_double_scan_unique_qr_code;
                    specialErrorMsgStringArgument = null;
                } else if (r.title.equals("QRCODE_TIMESTAMP_EXPIRED") || r.title.equals("QRCODE_TIMESTAMP_NOT_REACHED")) {
                    specialErrorMsgStringId = r.title.equals("QRCODE_TIMESTAMP_EXPIRED") ? R.string.session_has_already_ended : R.string.session_has_not_startet_yet;

                    // "detail": "2021-01-29T07:07:19Z[UTC]" -> convention (same in backend)
                    DateTime x = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ'[UTC]'").parseDateTime(r.detail).withZone(DateTimeZone.UTC);
                    final DateFormat DATETIME_FORMAT_OUT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
                    specialErrorMsgStringArgument = DATETIME_FORMAT_OUT.format(x.toDate());
                }
            } else if (statusCode == 404) {
                specialErrorMsgStringId = R.string.error_course_not_found;
            }
            return null;
        }
        catch (Exception e) {
            exception = e;
            specialErrorMsgStringId = null;  // no specific error msg
            specialErrorMsgStringArgument = null;
            e.printStackTrace();
            return null;  // return null if some network exception occured
        }
        return responseSharedCourse.getBody();
    }

    @Override
    protected void onPostExecute(SharedCourse course) {
        // https://stackoverflow.com/questions/44309241/warning-this-asynctask-class-should-be-static-or-leaks-might-occur
        MainActivity caller = callerMainActivity.get();
        if (caller == null || caller.isFinishing())
            // caller is gone
            return;

        caller.onCourseFetchTaskFinished(course, exception, specialErrorMsgStringId, specialErrorMsgStringArgument);
    }
}
