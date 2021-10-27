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
import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultCourse implements Serializable {
    private Long id;
    public boolean sent;
    private String nickName;
    private String timeStampFinished;
    private String timeStampStarted;
    private Long totalDurationInMillis;
    private Set<ResultControlpoint> resultControlpoints = new HashSet<>();
    private SharedCourse sharedCourse;
    private Integer showPositionCounter;
    private Integer switchAppCounter;
    private String viewCode;
    private ResultAdditionalInfo resultAdditionalinfo;
    public boolean pending;  // non-DTO variable indicating if this object has been successfully sent to the server

    public Integer getShowPositionCounter() {
        return showPositionCounter;
    }

    public void setShowPositionCounter(Integer showPositionCounter) {
        this.showPositionCounter = showPositionCounter;
    }

    public Integer getSwitchAppCounter() {
        return switchAppCounter;
    }

    public void setSwitchAppCounter(Integer switchAppCounter) {
        this.switchAppCounter = switchAppCounter;
    }

    public String getViewCode() {
        return viewCode;
    }

    public void setViewCode(String viewCode) {
        this.viewCode = viewCode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getTimeStampFinished() {
        return timeStampFinished;
    }

    public void setTimeStampFinished(String timeStampFinished) {
        this.timeStampFinished = timeStampFinished;
    }

    public String getTimeStampStarted() {
        return timeStampStarted;
    }

    public void setTimeStampStarted(String timeStampStarted) {
        this.timeStampStarted = timeStampStarted;
    }

    public Long getTotalDurationInMillis() {
        return totalDurationInMillis;
    }

    public void setTotalDurationInMillis(Long totalDurationInMillis) {
        this.totalDurationInMillis = totalDurationInMillis;
    }

    public Set<ResultControlpoint> getResultControlpoints() {
        return resultControlpoints;
    }

    public void setResultControlpoints(Set<ResultControlpoint> resultControlpoints) {
        this.resultControlpoints = resultControlpoints;
    }

    public SharedCourse getSharedCourse() {
        return sharedCourse;
    }

    public void setSharedCourse(SharedCourse sharedCourse) {
        this.sharedCourse = sharedCourse;
    }

    public ResultAdditionalInfo getResultAdditionalinfo() {
        return resultAdditionalinfo;
    }

    public void setResultAdditionalinfo(ResultAdditionalInfo resultAdditionalinfo) {
        this.resultAdditionalinfo = resultAdditionalinfo;
    }

    @Override
    public String toString() {
        return "ResultCourse{" +
                "id=" + id +
                ", nickName='" + nickName + '\'' +
                ", timeStampFinished='" + timeStampFinished + '\'' +
                ", timeStampStarted='" + timeStampStarted + '\'' +
                ", totalDurationInMillis=" + totalDurationInMillis +
                ", resultControlpoints=" + resultControlpoints +
                ", sharedCourse=" + sharedCourse +
                ", viewCode='" + viewCode + '\'' +
                ", resultAdditionalinfo=" + resultAdditionalinfo +
                ", pending=" + pending +
                '}';
    }
}
