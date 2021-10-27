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
public class ControlpointInfo implements Serializable {
    private Long id;
    private byte[] image;
    private String imageContentType;
    private ControlpointInfoColumn col;
    private String description;
    private String messageKey;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
    }

    public ControlpointInfoColumn getCol() {
        return col;
    }

    public void setCol(ControlpointInfoColumn col) {
        this.col = col;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public Set<ControlPoint> getControlpoints() {
        return controlpoints;
    }

    public void setControlpoints(Set<ControlPoint> controlpoints) {
        this.controlpoints = controlpoints;
    }

    @JsonIgnore
    private Set<ControlPoint> controlpoints = new HashSet<>();

    // prettier-ignore
    @Override
    public String toString() {
        return "ControlpointInfo{" +
                "\nid=" + getId() +
                ", \nimage='" + getImage() + "'" +
                ", \nimageContentType='" + getImageContentType() + "'" +
                ", \ncol='" + getCol() + "'" +
                ", \ndescription='" + getDescription() + "'" +
                ", \nmessageKey='" + getMessageKey() + "'" +
                "\n}";
    }
}
