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
public class Question implements Serializable {
    private Long id;
    private String text;
    private QuestionType type;
    private Difficulty difficulty;
    private Set<Answer> answers = new HashSet<>();
    private Category category;
    private Set<ControlPoint> controlpoints = new HashSet<>();
    private Boolean personal;

    public Long getId() {
        return id;
    }

    public Set<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(Set<Answer> answers) {
        this.answers = answers;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Set<ControlPoint> getControlpoints() {
        return controlpoints;
    }

    public void setControlpoints(Set<ControlPoint> controlpoints) {
        this.controlpoints = controlpoints;
    }

    public Boolean isPersonal() {
        return personal;
    }

    public void setPersonal(Boolean personal) {
        this.personal = personal;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", text='" + text + '\'' +
                ", type=" + type +
                ", difficulty=" + difficulty +
                ", answers=" + answers +
                ", category=" + category +
                ", controlpoints=" + controlpoints +
                ", personal=" + personal +
                '}';
    }
}
