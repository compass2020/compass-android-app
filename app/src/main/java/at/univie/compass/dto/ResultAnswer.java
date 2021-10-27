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

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultAnswer implements Serializable {
    private Long id;
    private String text;
    private Boolean correct;
    private Boolean answeredCorrectly;
    private ResultQuestion resultQuestion;

    @JsonIgnore
    public String answeredText;

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

    public ResultAnswer text(String text) {
        this.text = text;
        return this;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean isCorrect() {
        return correct;
    }

    public ResultAnswer correct(Boolean correct) {
        this.correct = correct;
        return this;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public ResultQuestion getResultQuestion() {
        return resultQuestion;
    }

    public ResultAnswer resultQuestion(ResultQuestion resultQuestion) {
        this.resultQuestion = resultQuestion;
        return this;
    }

    public void setResultQuestion(ResultQuestion resultQuestion) {
        this.resultQuestion = resultQuestion;
    }

    public Boolean getAnsweredCorrectly() {
        return answeredCorrectly;
    }

    public void setAnsweredCorrectly(Boolean answeredCorrectly) {
        this.answeredCorrectly = answeredCorrectly;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ResultAnswer{" +
                "id=" + id +
                ", text='" + text + '\'' +
                ", correct=" + correct +
                ", answeredCorrectly=" + answeredCorrectly +
                ", resultQuestion=" + resultQuestion +
                '}';
    }
}
