/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.task;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import at.univie.compass.global.Global;
import at.univie.compass.global.GPXWaypointAnnotations;
import at.univie.compass.dto.ResultAdditionalInfo;
import at.univie.compass.dto.ResultCourse;
import at.univie.compass.dto.SharedCourse;
import at.univie.compass.global.ZipperUtil;

public class ProcessResultsTask extends AsyncTask<Void, Void, Boolean> {
    private final int GPX_MOVING_AVERAGE_WINDOW = 5;  // values on either side
    private final int GPX_MOVING_AVERAGE_WINDOW_SPEED = 5;
    private final int GPX_MOVING_AVERAGE_WINDOW_HEIGHT = 60;

    ResultCourse resultCourse;
    SharedCourse sharedCourse;
    Map<String, List<Double>> gpxLongitudes;
    Map<String, List<Double>> gpxLatitudes;
    Map<String, List<Double>> gpxAltitudes;
    Map<String, List<Double>> gpxSpeeds;
    Map<String, List<GPXWaypointAnnotations>> gpxAnnotations;
    List<Double> heartRateList;
    List<String> heartRateTimeList;
    String nickname;
    Context applicationContext;

    public ProcessResultsTask(
            Context applicationContext,
            ResultCourse resultCourse, SharedCourse sharedCourse,
            Map<String, List<Double>> gpxLongitudes, Map<String, List<Double>> gpxLatitudes, Map<String, List<Double>> gpxAltitudes, Map<String, List<Double>> gpxSpeeds, Map<String, List<GPXWaypointAnnotations>> gpxAnnotations,
            List<Double> heartRateList, List<String> heartRateTimeList,
            String nickname
    ) {
        this.applicationContext = applicationContext;
        this.resultCourse = resultCourse;
        this.sharedCourse = sharedCourse;
        this.gpxLongitudes = gpxLongitudes;
        this.gpxLatitudes = gpxLatitudes;
        this.gpxAltitudes = gpxAltitudes;
        this.gpxSpeeds = gpxSpeeds;
        this.gpxAnnotations = gpxAnnotations;
        this.heartRateList = heartRateList;
        this.heartRateTimeList = heartRateTimeList;
        this.nickname = nickname;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        String heartRateStr = createHeartRateXML();
        for (String filter : gpxLongitudes.keySet()) {
            List<Double> longitudes = this.gpxLongitudes.get(filter);
            List<Double> latitudes = this.gpxLatitudes.get(filter);
            List<Double> altitudes = this.gpxAltitudes.get(filter);
            List<Double> speeds = this.gpxSpeeds.get(filter);
            List<Double> smoothedLongitudes = movingAverage(longitudes, GPX_MOVING_AVERAGE_WINDOW);
            List<Double> smoothedLatitudes = movingAverage(latitudes, GPX_MOVING_AVERAGE_WINDOW);
            List<Double> smoothedAltitudes = movingAverage(altitudes, GPX_MOVING_AVERAGE_WINDOW_HEIGHT);
            List<Double> smoothedSpeeds = movingAverage(speeds, GPX_MOVING_AVERAGE_WINDOW_SPEED);

            String nickname_raw = nickname + " - filter: " + filter;
            String nickname_smooth = nickname + " - filter: " + filter + " (smoothed)";

            if (Global.UPLOAD_INTERMEDIATE_GPX_RESULTS) {
                String gpxStr = gpxFromData(longitudes, latitudes, altitudes, speeds, gpxAnnotations.get(filter), nickname_raw);

                ResultAdditionalInfo resultAdditionalInfoRaw = new ResultAdditionalInfo();
                try {
                    resultAdditionalInfoRaw.setGpxTrack(ZipperUtil.compress(gpxStr.getBytes()));
                    resultAdditionalInfoRaw.setGpxTrackContentType("application/zip");
                    resultAdditionalInfoRaw.setHeartRate(ZipperUtil.compress(heartRateStr.getBytes()));
                    resultAdditionalInfoRaw.setHeartRateContentType("application/zip");
                } catch (IOException exception) {
                    resultAdditionalInfoRaw.setGpxTrack(gpxStr.getBytes());
                    resultAdditionalInfoRaw.setGpxTrackContentType("application/xml");
                    resultAdditionalInfoRaw.setHeartRate(heartRateStr.getBytes());
                    resultAdditionalInfoRaw.setHeartRateContentType("application/xml");
                }

                resultAdditionalInfoRaw.setResultCourse(new ResultCourse());
                resultCourse.setResultAdditionalinfo(resultAdditionalInfoRaw);
                resultCourse.setNickName(nickname_raw);

                Global.storeResult(applicationContext, resultCourse, true);

                gpxStr = gpxFromData(smoothedLongitudes, smoothedLatitudes, smoothedAltitudes, smoothedSpeeds, gpxAnnotations.get(filter), nickname_smooth);

                ResultAdditionalInfo resultAdditionalInfoSmooth = new ResultAdditionalInfo();
                try {
                    resultAdditionalInfoSmooth.setGpxTrack(ZipperUtil.compress(gpxStr.getBytes()));
                    resultAdditionalInfoSmooth.setGpxTrackContentType("application/zip");
                    resultAdditionalInfoSmooth.setHeartRate(ZipperUtil.compress(heartRateStr.getBytes()));
                    resultAdditionalInfoSmooth.setHeartRateContentType("application/zip");
                } catch (IOException exception) {
                    resultAdditionalInfoSmooth.setGpxTrack(gpxStr.getBytes());
                    resultAdditionalInfoSmooth.setGpxTrackContentType("application/xml");
                    resultAdditionalInfoSmooth.setHeartRate(heartRateStr.getBytes());
                    resultAdditionalInfoSmooth.setHeartRateContentType("application/xml");
                }
                resultAdditionalInfoSmooth.setResultCourse(new ResultCourse());
                resultCourse.setResultAdditionalinfo(resultAdditionalInfoSmooth);
                resultCourse.setNickName(nickname_smooth);
                Global.storeResult(applicationContext, resultCourse, true);
            } else {
                String gpxStr = gpxFromData(smoothedLongitudes, smoothedLatitudes, smoothedAltitudes, smoothedSpeeds, gpxAnnotations.get(filter), nickname);

                ResultAdditionalInfo resultAdditionalInfoSmooth = new ResultAdditionalInfo();
                try {
                    resultAdditionalInfoSmooth.setGpxTrack(ZipperUtil.compress(gpxStr.getBytes()));
                    resultAdditionalInfoSmooth.setGpxTrackContentType("application/zip");
                    resultAdditionalInfoSmooth.setHeartRate(ZipperUtil.compress(heartRateStr.getBytes()));
                    resultAdditionalInfoSmooth.setHeartRateContentType("application/zip");
                } catch (IOException exception) {
                    resultAdditionalInfoSmooth.setGpxTrack(gpxStr.getBytes());
                    resultAdditionalInfoSmooth.setGpxTrackContentType("application/xml");
                    resultAdditionalInfoSmooth.setHeartRate(heartRateStr.getBytes());
                    resultAdditionalInfoSmooth.setHeartRateContentType("application/xml");
                }
                resultAdditionalInfoSmooth.setResultCourse(new ResultCourse());
                resultCourse.setResultAdditionalinfo(resultAdditionalInfoSmooth);
                resultCourse.setNickName(nickname);
                Global.storeResult(applicationContext, resultCourse, true);
            }
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);

        // Send the result via UploadWorker.
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(UploadResultWorker.class)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(Global.UPLOAD_WORK_ID, ExistingWorkPolicy.REPLACE, uploadWorkRequest);  // https://developer.android.com/topic/libraries/architecture/workmanager/advanced#unique
    }

    private String gpxFromData(List<Double> longitudes, List<Double> latitudes, List<Double> altitudes, List<Double> speeds, List<GPXWaypointAnnotations> annot, String nickname) {
        StringBuilder gpx = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"COMPASS\">\n" +
                "  <metadata>\n" +
                "    <name>" + "COMPASS" + "</name>\n" +
                "    <author>\n" +
                "      <name>" + nickname + "</name>\n" +
                "    </author>\n" +
                "    <device>" + Global.getDeviceName() + "</device>\n" +
                "    <android>Android " + android.os.Build.VERSION.RELEASE + " (SDK " + android.os.Build.VERSION.SDK_INT + ")</android>\n" +
                "    <course>" + sharedCourse.getCourse().getName() + "</course>\n" +
                "    <session>" + sharedCourse.getName() + "</session>\n" +
                "  </metadata>\n" +
                "  <trk>\n" +
                "    <trkseg>\n");

        for (int i = 0; i < longitudes.size(); i++) {
            if (Global.VERBOSE_GPX) {
                gpx.append("      <trkpt lat=\"").append(latitudes.get(i)).append("\" lon=\"").append(longitudes.get(i)).append("\">\n")
                        .append("        <time>").append(annot.get(i).timestamp).append("</time>\n")
                        .append("        <ele>").append(altitudes.get(i)).append("</ele>\n")
                        .append("        <hdop>").append(annot.get(i).accuracy).append("</hdop>\n")
                        .append("        <speed>").append(speeds.get(i)).append("</speed>\n")
                        .append("        <desc>").append(annot.get(i).cpReached).append("</desc>\n")
                        .append("        <bearing>").append(annot.get(i).bearing).append("</bearing>\n")
                        .append("        <provider>").append(annot.get(i).provider).append("</provider>\n");
                for (int j = 0; j < annot.get(i).extrasKeys.size(); j++) {
                    gpx.append("        <extras_").append(annot.get(i).extrasKeys.get(j)).append(">").append(annot.get(i).extrasValues.get(j)).append("</extras_").append(annot.get(i).extrasKeys.get(j)).append(">\n");
                }
            } else {
                gpx.append("      <trkpt lat=\"").append(latitudes.get(i)).append("\" lon=\"").append(longitudes.get(i)).append("\">\n")
                        .append("        <time>").append(annot.get(i).timestamp).append("</time>\n")
                        .append("        <ele>").append(altitudes.get(i)).append("</ele>\n")
                        .append("        <hdop>").append(annot.get(i).accuracy).append("</hdop>\n")
                        .append("        <speed>").append(speeds.get(i)).append("</speed>\n")
                        .append("        <desc>").append(annot.get(i).cpReached).append("</desc>\n");
            }
            gpx.append("      </trkpt>\n");
        }
        gpx.append("    </trkseg>\n" + "  </trk>\n" + "</gpx>\n");
        return gpx.toString();
    }

    private List<Double> movingAverage(List<Double> values, int window) {
        List<Double> filtered = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            int j_first = Math.max(0, i-window);
            int j_last = Math.min(values.size()-1, i+window);
            double sum = 0;
            for (int j = j_first; j <= j_last; j++)
                sum += values.get(j);
            filtered.add(sum / (j_last - j_first + 1));
        }
        return filtered;
    }

    private String createHeartRateXML() {
        StringBuilder stringBuilder = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "  <metadata>\n" +
                "    <name>" + "COMPASS-App" + "</name>\n" +
                "    <author>\n" +
                "      <name>" + Global.getStoredNickname(applicationContext) + "</name>\n" +
                "    </author>\n" +
                "  </metadata>\n" +
                "  <data>\n");

        for (int i = 0; i < heartRateList.size(); i++) {
            stringBuilder.append("      <heartrate>\n")
                    .append("        <time>").append(heartRateTimeList.get(i)).append("</time>\n")
                    .append("        <value>").append(heartRateList.get(i)).append("</value>\n")
                    .append("      </heartrate>\n");
        }
        stringBuilder.append("</data>\n");
        return stringBuilder.toString();
    }
}
