/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.task;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import at.univie.compass.global.Global;
import at.univie.compass.activity.RunActivity;
import at.univie.compass.dto.PositionDTO;


public class UploadPositionWorker extends Worker {
    static PositionDTO position = null;
    static WeakReference<RunActivity> callerRunActivity = null;

    public UploadPositionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void init(UUID uuid, Location location, String nickname, String timestamp, Long sharedCourseId, RunActivity caller) {
        callerRunActivity = new WeakReference<>(caller);
        position = new PositionDTO(uuid, nickname, location.getLatitude(), location.getLongitude(), timestamp, sharedCourseId);
    }

    @Override
    public Result doWork() {
        // authenticate if necessary
        String authToken;
        try {
            authToken = Global.getAuthenticationToken(Global.BACKEND_USER, Global.BACKEND_USER_PASSWORD, false);
        } catch (IOException e) {
            // Authentication failed
            return Result.retry();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);

        RestTemplate restTemplate = new RestTemplate();
        ((SimpleClientHttpRequestFactory) restTemplate.getRequestFactory()).setConnectTimeout(Global.POSITION_REPORTING_INTERVAL);
        ((SimpleClientHttpRequestFactory) restTemplate.getRequestFactory()).setReadTimeout(Global.POSITION_REPORTING_INTERVAL);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        restTemplate.getMessageConverters().add(converter);

        HttpEntity<PositionDTO> entityPosition = new HttpEntity<>(position, headers);

        try {
            String url = new URL(Global.USE_HTTPS ? "https" : "http", Global.DEFAULT_BACKEND_HOST, Global.BACKEND_PORT, Global.API_POSITION).toString();
            ResponseEntity<String[]> responseResult = restTemplate.exchange(url, HttpMethod.POST, entityPosition, String[].class);
            String[] messages = responseResult.getBody();

            switch (responseResult.getStatusCode()) {
                case OK:  // 200
                    if (callerRunActivity != null) {
                        RunActivity caller = callerRunActivity.get();
                        if (caller != null && !caller.isFinishing()) {
                            caller.onUploadPositionTaskFinished(messages);
                        }
                    }
                    // Position successfully sent but RunActivity gone.
                    return Result.success();
                default:
                    break;
            }
        } catch (Exception ignored) {
        }

        if (callerRunActivity != null) {
            RunActivity caller = callerRunActivity.get();
            if (caller == null || caller.isFinishing()) {
                // caller is gone
                return Result.success();
            }
        }
        // RunActivity is still here but work failed for some other reason
        return Result.retry();
    }
}
