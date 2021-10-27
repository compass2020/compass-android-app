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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultQuestion implements Serializable {
    private Long id;
    private String text;
    private QuestionType type;
    private Difficulty difficulty;
    private Set<ResultAnswer> resultAnswers = new HashSet<>();
    private Category category;
    private Set<ResultControlpoint> resultControlpoints = new HashSet<>();
    private Boolean answeredCorrectly;

    @JsonIgnore
    public List<ResultAnswer> orderedResultAnswers = new ArrayList<>();  // non-marshalled property to store the order of answers across activities in the app

    // jhipster-needle-entity-add-field - JHipster will add fields here
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public ResultQuestion text(String text) {
        this.text = text;
        return this;
    }

    public Boolean getAnsweredCorrectly() {
        return answeredCorrectly;
    }

    public void setAnsweredCorrectly(Boolean answeredCorrectly) {
        this.answeredCorrectly = answeredCorrectly;
    }

    public void setText(String text) {
        this.text = text;
    }

    public QuestionType getType() {
        return type;
    }

    public ResultQuestion type(QuestionType type) {
        this.type = type;
        return this;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public ResultQuestion difficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        return this;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Set<ResultAnswer> getResultAnswers() {
        return resultAnswers;
    }

    public ResultQuestion resultAnswers(Set<ResultAnswer> resultAnswers) {
        this.resultAnswers = resultAnswers;
        return this;
    }

    public ResultQuestion addResultAnswer(ResultAnswer resultAnswer) {
        this.resultAnswers.add(resultAnswer);
        resultAnswer.setResultQuestion(this);
        return this;
    }

    public ResultQuestion removeResultAnswer(ResultAnswer resultAnswer) {
        this.resultAnswers.remove(resultAnswer);
        resultAnswer.setResultQuestion(null);
        return this;
    }

    public void setResultAnswers(Set<ResultAnswer> resultAnswers) {
        this.resultAnswers = resultAnswers;
    }

    public Category getCategory() {
        return category;
    }

    public ResultQuestion category(Category category) {
        this.category = category;
        return this;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Set<ResultControlpoint> getResultControlpoints() {
        return resultControlpoints;
    }

    public ResultQuestion resultControlpoints(Set<ResultControlpoint> resultControlpoints) {
        this.resultControlpoints = resultControlpoints;
        return this;
    }

    public ResultQuestion addResultControlpoint(ResultControlpoint resultControlpoint) {
        this.resultControlpoints.add(resultControlpoint);
        resultControlpoint.getResultQuestions().add(this);
        return this;
    }

    public ResultQuestion removeResultControlpoint(ResultControlpoint resultControlpoint) {
        this.resultControlpoints.remove(resultControlpoint);
        resultControlpoint.getResultQuestions().remove(this);
        return this;
    }

    public void setResultControlpoints(Set<ResultControlpoint> resultControlpoints) {
        this.resultControlpoints = resultControlpoints;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here


    @Override
    public String toString() {
        return "ResultQuestion{" +
                "id=" + id +
                ", text='" + text + '\'' +
                ", type=" + type +
                ", difficulty=" + difficulty +
                ", resultAnswers=" + resultAnswers +
                ", category=" + category +
                ", resultControlpoints=" + resultControlpoints +
                '}';
    }
}
