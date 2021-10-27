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
public class OrienteeringMap implements Serializable {
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public byte[] getMapOverlayImage() {
        return mapOverlayImage;
    }

    public void setMapOverlayImage(byte[] mapOverlayImage) {
        this.mapOverlayImage = mapOverlayImage;
    }

    public String getMapOverlayImageContentType() {
        return mapOverlayImageContentType;
    }

    public void setMapOverlayImageContentType(String mapOverlayImageContentType) {
        this.mapOverlayImageContentType = mapOverlayImageContentType;
    }

    public byte[] getMapOverlayKml() {
        return mapOverlayKml;
    }

    public void setMapOverlayKml(byte[] mapOverlayKml) {
        this.mapOverlayKml = mapOverlayKml;
    }

    public String getMapOverlayKmlContentType() {
        return mapOverlayKmlContentType;
    }

    public void setMapOverlayKmlContentType(String mapOverlayKmlContentType) {
        this.mapOverlayKmlContentType = mapOverlayKmlContentType;
    }

    public Double getImageScaleX() {
        return imageScaleX;
    }

    public void setImageScaleX(Double imageScaleX) {
        this.imageScaleX = imageScaleX;
    }

    public Double getImageScaleY() {
        return imageScaleY;
    }

    public void setImageScaleY(Double imageScaleY) {
        this.imageScaleY = imageScaleY;
    }

    public Double getImageCenterX() {
        return imageCenterX;
    }

    public void setImageCenterX(Double imageCenterX) {
        this.imageCenterX = imageCenterX;
    }

    public Double getImageCenterY() {
        return imageCenterY;
    }

    public void setImageCenterY(Double imageCenterY) {
        this.imageCenterY = imageCenterY;
    }

    public Double getImageRotation() {
        return imageRotation;
    }

    public void setImageRotation(Double imageRotation) {
        this.imageRotation = imageRotation;
    }

    public Double getDeclination() {
        return declination;
    }

    public void setDeclination(Double declination) {
        this.declination = declination;
    }

    private Long id;
    private transient byte[] mapOverlayImage;
    private String mapOverlayImageContentType;
    private transient byte[] mapOverlayKml;
    private String mapOverlayKmlContentType;
    private Double imageScaleX;
    private Double imageScaleY;
    private Double imageCenterX;
    private Double imageCenterY;
    private Double imageRotation;
    private Double declination;
    private Course course;

    @Override
    public String toString() {
        return "OrienteeringMap{" + "id=" + getId() + ", mapOverlayImage='" + getMapOverlayImage() + "'"
                + ", mapOverlayImageContentType='" + getMapOverlayImageContentType() + "'" + ", mapOverlayKml='"
                + getMapOverlayKml() + "'" + ", mapOverlayKmlContentType='" + getMapOverlayKmlContentType() + "'"
                + "'"
                + ", imageScaleX=" + getImageScaleX() + ", imageScaleY=" + getImageScaleY() + ", imageCenterX="
                + getImageCenterX() + ", imageCenterY=" + getImageCenterY() + ", imageRotation=" + getImageRotation()
                + ", declination=" + getDeclination() + "}";
    }
}

