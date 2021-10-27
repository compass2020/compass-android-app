/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SharedCourse implements Serializable {
    private Long id;
    private String qrCode;
    private GameModus gameModus;
    private Course course;
    private String timeStampShared;
    private String name;
    private Boolean visible;
    private Set<ResultCourse> resultCourses = new HashSet<>();
    private Boolean showCourseBeforeStart;
    private String timeStampStart;
    private String timeStampEnd;
    private Integer numberOfCustomQrCodes;
    private Boolean showPositionAllowed;

    @JsonIgnore
    public Boolean viaOneTimeCode;

    public Boolean getShowPositionAllowed() {
        return showPositionAllowed;
    }

    public void setShowPositionAllowed(Boolean showPositionAllowed) {
        this.showPositionAllowed = showPositionAllowed;
    }

    public String getTimeStampStart() {
        return timeStampStart;
    }

    public void setTimeStampStart(String timeStampStart) {
        this.timeStampStart = timeStampStart;
    }

    public String getTimeStampEnd() {
        return timeStampEnd;
    }

    public void setTimeStampEnd(String timeStampEnd) {
        this.timeStampEnd = timeStampEnd;
    }

    public Integer getNumberOfCustomQrCodes() {
        return numberOfCustomQrCodes;
    }

    public void setNumberOfCustomQrCodes(Integer numberOfCustomQrCodes) {
        this.numberOfCustomQrCodes = numberOfCustomQrCodes;
    }

    public Boolean getShowCourseBeforeStart() {
        return showCourseBeforeStart;
    }

    public void setShowCourseBeforeStart(Boolean showCourseBeforeStart) {
        this.showCourseBeforeStart = showCourseBeforeStart;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Set<ResultCourse> getResultCourses() {
        return resultCourses;
    }

    public void setResultCourses(Set<ResultCourse> resultCourses) {
        this.resultCourses = resultCourses;
    }

    public String getTimeStampShared() {
        return timeStampShared;
    }

    public void setTimeStampShared(String timeStampShared) {
        this.timeStampShared = timeStampShared;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public GameModus getGameModus() {
        return gameModus;
    }

    public void setGameModus(GameModus gameModus) {
        this.gameModus = gameModus;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    @Override
    public String toString() {
        return "SharedCourse{" +
                "id=" + id +
                ", qrCode='" + qrCode + '\'' +
                ", gameModus=" + gameModus +
                ", course=" + course +
                ", timeStampShared='" + timeStampShared + '\'' +
                ", name='" + name + '\'' +
                ", visible=" + visible +
                ", resultCourses=" + resultCourses +
                ", showCourseBeforeStart=" + showCourseBeforeStart +
                ", timeStampStart='" + timeStampStart + '\'' +
                ", timeStampEnd='" + timeStampEnd + '\'' +
                ", numberOfCustomQrCodes=" + numberOfCustomQrCodes +
                ", showPositionAllowed=" + showPositionAllowed +
                '}';
    }
}
