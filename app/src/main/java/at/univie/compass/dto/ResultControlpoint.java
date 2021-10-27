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
public class ResultControlpoint implements Serializable {
    private Long id;
    private Integer sequence;
    private String timeReached;
    private Double latitude;
    private Double longitude;
    private ResultCourse resultCourse;
    private Set<ResultQuestion> resultQuestions = new HashSet<>();
    private Boolean skipAllowed;
    private Boolean reached;
    private Boolean forceSkipped;
    private Integer borgScale;

    public Boolean getForceSkipped() {
        return forceSkipped;
    }

    public void setForceSkipped(Boolean forceSkipped) {
        this.forceSkipped = forceSkipped;
    }

    public Boolean getSkipAllowed() {
        return skipAllowed;
    }

    public void setSkipAllowed(Boolean skipAllowed) {
        this.skipAllowed = skipAllowed;
    }

    public Boolean getReached() {
        return reached;
    }

    public void setReached(Boolean reached) {
        this.reached = reached;
    }

    public Integer getBorgScale() {
        return borgScale;
    }

    public void setBorgScale(Integer borgScale) {
        this.borgScale = borgScale;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public String getTimeReached() {
        return timeReached;
    }

    public void setTimeReached(String timeReached) {
        this.timeReached = timeReached;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public ResultCourse getResultCourse() {
        return resultCourse;
    }

    public void setResultCourse(ResultCourse resultCourse) {
        this.resultCourse = resultCourse;
    }

    public Set<ResultQuestion> getResultQuestions() {
        return resultQuestions;
    }

    public ResultControlpoint setResultQuestions(Set<ResultQuestion> resultQuestions) {
        this.resultQuestions = resultQuestions;
        return this;
    }

    @Override
    public String toString() {
        return "ResultControlpoint{" +
                "id=" + id +
                ", sequence=" + sequence +
                ", timeReached='" + timeReached + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", resultQuestions=" + resultQuestions +
                ", skipAllowed=" + skipAllowed +
                ", reached=" + reached +
                ", borgScale=" + borgScale +
                '}';
    }
}
