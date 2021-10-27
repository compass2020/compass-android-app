/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.task;


import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import at.univie.compass.global.Global;
import at.univie.compass.dto.ResultCourse;

public class UploadResultWorker extends Worker {
    public UploadResultWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        Context applicationContext = getApplicationContext();

        // retrieve stored results
        ResultCourse[] storedResults = Global.getStoredResults(applicationContext);
        if (storedResults == null || storedResults.length == 0)  {
            return Result.success();  // nothing to send
        }

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
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        restTemplate.getMessageConverters().add(converter);

        for (int i = storedResults.length-1; i >= 0; i--) {  // traverse backwards to allow deletion of entries by index
            HttpEntity<ResultCourse> entityUpdate = new HttpEntity<>(storedResults[i], headers);
            try {
                String url = new URL(Global.USE_HTTPS ? "https" : "http", Global.DEFAULT_BACKEND_HOST, Global.BACKEND_PORT, Global.API_COURSE_RESULT).toString();

                ResponseEntity<ResultCourse> responseResult = restTemplate.exchange(url, HttpMethod.POST, entityUpdate, ResultCourse.class);

                switch (responseResult.getStatusCode()) {
                    case CREATED:  // 201
                        Global.deleteStoredResult(applicationContext, i);  // Problem: wenn man die connection am richtigen Punkt unterbricht, sendet er zwar aber löscht nicht das result, vermutlich weil er die response nicht mehr kriegt -> doppeltes Result in der Datenbank
                        if (responseResult.getBody() != null) {
                            ResultCourse rcReturned = responseResult.getBody();
                            rcReturned.setResultControlpoints(null);  // we dont need to store this for the ViewCode
                            rcReturned.setResultAdditionalinfo(null);  // ...
                            Global.storeViewCode(getApplicationContext(), rcReturned);
                        }
                        break;
                    default:
                        break;
                }
            } catch (Exception ignored) {
            }
        }

        ResultCourse[] nowStoredResults = Global.getStoredResults(applicationContext);  // Unnötiger overhead hier nochmal getStoredResults aufzurufen -> besser direkt von deleteStoredResults zurückgeben ob erfolgreich bzw ob noch welche gespeichert sind.
        if (nowStoredResults != null && nowStoredResults.length > 0) {
            // still something to send
            return Result.retry();
        }
        return Result.success();
    }
}
