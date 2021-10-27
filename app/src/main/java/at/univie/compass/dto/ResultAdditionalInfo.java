/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultAdditionalInfo implements Serializable {
    private Long id;
    private byte[] gpxTrack;
    private String gpxTrackContentType;
    private byte[] heartRate;
    private String heartRateContentType;

    public ResultCourse getResultCourse() {
        return resultCourse;
    }

    public void setResultCourse(ResultCourse resultCourse) {
        this.resultCourse = resultCourse;
    }

    private ResultCourse resultCourse;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public byte[] getGpxTrack() {
        return gpxTrack;
    }

    public void setGpxTrack(byte[] gpxTrack) {
        this.gpxTrack = gpxTrack;
    }

    public String getGpxTrackContentType() {
        return gpxTrackContentType;
    }

    public void setGpxTrackContentType(String gpxTrackContentType) {
        this.gpxTrackContentType = gpxTrackContentType;
    }

    public byte[] getHeartRate() { if(heartRate != null)
        return heartRate; else  return new byte[1];}

    public void setHeartRate(byte[] heartRate) { this.heartRate = heartRate; }

    public String getHeartRateContentType() { if(heartRateContentType != null) return heartRateContentType; else return "";}

    public void setHeartRateContentType(String heartRateContentType) { this.heartRateContentType = heartRateContentType; }

    public String toString() {
        return "ResultAdditionalInfo{" +
                "id=" + getId() +
                ", gpxTrack='" + getGpxTrack().toString() + "'" +
                ", gpxTrackContentType='" + getGpxTrackContentType() + "'" +
                ", heartRate='" + getHeartRate().toString() + "'" +
                ", heartRateContentType='" + getHeartRateContentType() + "'" +
                "}";
    }

}
