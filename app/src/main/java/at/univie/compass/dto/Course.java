/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.dto;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Course implements Serializable {
    private Long id;
    private String name;
    private Boolean shared;
    private String location;
    private Double altitudeUp;
    private Double altitudeDown;
    private Double length;
    private OrienteeringMap orienteeringMap;
    public Set<ControlPoint> controlpoints = new HashSet<>();
    private Set<SharedCourse> sharedCourses = new HashSet<>();

    public Boolean getShared() {
        return shared;
    }

    public void setShared(Boolean shared) {
        this.shared = shared;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getAltitudeUp() {
        return altitudeUp;
    }

    public void setAltitudeUp(Double altitudeUp) {
        this.altitudeUp = altitudeUp;
    }

    public Double getAltitudeDown() {
        return altitudeDown;
    }

    public void setAltitudeDown(Double altitudeDown) {
        this.altitudeDown = altitudeDown;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Set<SharedCourse> getSharedCourses() {
        return sharedCourses;
    }

    public void setSharedCourses(Set<SharedCourse> sharedCourses) {
        this.sharedCourses = sharedCourses;
    }

    public OrienteeringMap getOrienteeringMap() {
        return orienteeringMap;
    }

    public void setOrienteeringMap(OrienteeringMap orienteeringMap) {
        this.orienteeringMap = orienteeringMap;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Course name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<ControlPoint> getControlpoints() {
        return controlpoints;
    }

    public Course controlPoints(Set<ControlPoint> controlPoints) {
        this.controlpoints = controlPoints;
        return this;
    }

    public Course addControlPoint(ControlPoint controlPoint) {
        this.controlpoints.add(controlPoint);
        controlPoint.setCourse(this);
        return this;
    }

    public Course removeControlPoint(ControlPoint controlPoint) {
        this.controlpoints.remove(controlPoint);
        controlPoint.setCourse(null);
        return this;
    }

    public void setControlpoints(Set<ControlPoint> controlpoints) {
        this.controlpoints = controlpoints;
    }

    @Override
    public String toString() {
        return "Course{" +
                "\nid=" + id +
                ", \nname='" + name + '\'' +
                ", \nshared=" + shared +
                ", \nlocation='" + location + '\'' +
                ", \naltitudeUp=" + altitudeUp +
                ", \naltitudeDown=" + altitudeDown +
                ", \nlength=" + length +
                ", \norienteeringMap=" + orienteeringMap +
                ", \ncontrolpoints=" + controlpoints +
                ", \nsharedCourses=" + sharedCourses +
                "\n}";
    }
}
