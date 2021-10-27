/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.dto;

import android.location.Location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ControlPoint implements Serializable {
    private Long id;
    private Integer sequence;
    private Integer controlCode;
    private Double latitude;
    private Double longitude;
    private Double elevation;
    private Integer radius;
    private Boolean skippable;
    private Boolean team;
    private String qrCode;
    private String description;
    private Set<Question> questions = new HashSet<>();
    private Set<ControlpointInfo> controlpointInfos = new HashSet<>();
    private Course course;

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    public Integer getControlCode() {
        return controlCode;
    }

    public void setControlCode(Integer controlCode) {
        this.controlCode = controlCode;
    }

    public Set<ControlpointInfo> getControlpointInfos() {
        return controlpointInfos;
    }

    public void setControlpointInfos(Set<ControlpointInfo> controlpointInfos) {
        this.controlpointInfos = controlpointInfos;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getRadius() {
        return radius;
    }

    public void setRadius(Integer radius) {
        this.radius = radius;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Boolean getSkippable() {
        return skippable;
    }

    public void setSkippable(Boolean skippable) {
        this.skippable = skippable;
    }

    public Set<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(Set<Question> questions) {
        this.questions = questions;
    }

    public Boolean getTeam() {
        return team;
    }

    public void setTeam(Boolean team) {
        this.team = team;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Location getLocation() {
       Location l = new Location("");
       l.setLongitude(longitude);
       l.setLatitude(latitude);
       return l;
    }

    @Override
    public String toString() {
        return "ControlPoint{" +
                "\nid=" + id +
                ", \nsequence=" + sequence +
                ", \ncontrolCode=" + controlCode +
                ", \nlatitude=" + latitude +
                ", \nlongitude=" + longitude +
                ", \nelevation=" + elevation +
                ", \nradius=" + radius +
                ", \nskippable=" + skippable +
                ", \nteam=" + team +
                ", \nqrCode='" + qrCode + '\'' +
                ", \ndescription='" + description + '\'' +
                ", \nquestions=" + questions +
                ", \ncontrolpointInfos=" + controlpointInfos +
                ", \ncourse=" + course +
                "\n}";
    }
}
