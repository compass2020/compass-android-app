/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * A DTO representing a position from a player
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PositionDTO {

    private UUID uuid;

    private String nickname;

    private Double latitude;

    private Double longitude;

    // 2020-12-02T13:33:15.216Z -> different format than usual!
    private String timestamp;

    private Long sharedCourseId;

    public PositionDTO() {
    }

    public PositionDTO(UUID uuid, String nickname, Double latitude, Double longitude, String timestamp,
                       Long sharedCourseId) {
        super();
        this.uuid = uuid;
        this.nickname = nickname;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.sharedCourseId = sharedCourseId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Long getSharedCourseId() {
        return sharedCourseId;
    }

    public void setSharedCourseId(Long sharedCourseId) {
        this.sharedCourseId = sharedCourseId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PositionDTO other = (PositionDTO) obj;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PositionDTO [uuid=" + uuid + ", nickname=" + nickname + ", latitude=" + latitude + ", longitude="
                + longitude + ", timestamp=" + timestamp + ", sharedCourseId=" + sharedCourseId + "]";
    }
}
