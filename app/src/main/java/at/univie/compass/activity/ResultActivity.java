/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import at.univie.compass.R;
import at.univie.compass.dto.ResultAnswer;
import at.univie.compass.dto.ResultCourse;
import at.univie.compass.dto.ResultQuestion;

import static at.univie.compass.dto.QuestionType.MULTIPLE;


public class ResultActivity extends AppCompatActivity implements View.OnClickListener {
    List<ResultQuestion> resultQuestions;
    int currentRQindex = 0;

    // UI
    private ListView listView;
    private TextView editTextAnswer, textViewTime, textViewQuestion, textViewCode;
    private Button buttonNext, buttonPrevious;

    // Data
    private ResultCourse resultCourse;

    // Colors
    @ColorInt
//    int colorRes = R.color.checkboxGreen : ;
    int colorCheckboxRed;
    int colorCheckboxGreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultCourse = (ResultCourse) getIntent().getExtras().get("resultCourse");

        colorCheckboxRed = ContextCompat.getColor(ResultActivity.this, R.color.checkboxRed);
        colorCheckboxGreen = ContextCompat.getColor(ResultActivity.this, R.color.checkboxGreen);

        // UI
        textViewQuestion = findViewById(R.id.textViewQuestion);
        listView = findViewById(R.id.listView);
        textViewTime = findViewById(R.id.textViewTime);
        long millis = resultCourse.getTotalDurationInMillis();
        // https://stackoverflow.com/questions/9027317/how-to-convert-milliseconds-to-hhmmss-format
        String hhmmss = String.format(" %02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
        textViewTime.setText(getString(R.string.your_time, hhmmss));
        textViewCode = findViewById(R.id.textViewYourCode);
        textViewCode.setText(Html.fromHtml( "<u>" + String.format(getString(R.string.click_result_link), getString(R.string.show_results)) + "</u>"));
        textViewCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ResultActivity.this, ResultListActivity.class));
            }
        });
        editTextAnswer = findViewById(R.id.editTextAnswer);
        buttonNext = findViewById(R.id.buttonNext);
        buttonNext.setOnClickListener(this);
        buttonPrevious = findViewById(R.id.buttonPrevious);
        buttonPrevious.setOnClickListener(this);

        resultQuestions = (ArrayList<ResultQuestion>) getIntent().getExtras().get("orderedResultQuestions");
        if (resultQuestions.size() > 0) {
            currentRQindex = 0;
            showResultQuestion(resultQuestions.get(currentRQindex));
        } else {
            buttonNext.setVisibility(View.GONE);
            buttonPrevious.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonNext:
                if (currentRQindex < resultQuestions.size() - 1) {
                    currentRQindex++;
                    showResultQuestion(resultQuestions.get(currentRQindex));
                }
                break;
            case R.id.buttonPrevious:
                if (currentRQindex > 0) {
                    currentRQindex--;
                    showResultQuestion(resultQuestions.get(currentRQindex));
                }
                break;
        }
    }

    public void showResultQuestion(ResultQuestion q) {
        textViewQuestion.setText(q.getText());

        switch (q.getType()) {
            case INPUT:
                listView.setVisibility(View.GONE);
                editTextAnswer.setVisibility(View.VISIBLE);
                String correctAnswer = q.getResultAnswers().iterator().next().getText();  // there is exactly 1 resultAnswer -> get its text
                String userAnswer = q.getResultAnswers().iterator().next().answeredText; // TODO: not implemented yet

                editTextAnswer.setText(getString(R.string.your_answer_correct_answer, userAnswer, correctAnswer));

                if (q.getAnsweredCorrectly()) {
                    editTextAnswer.setTextColor(colorCheckboxGreen);
                } else {
                    editTextAnswer.setTextColor(colorCheckboxRed);
                }
                break;
            case SINGLE:
            case MULTIPLE:
                editTextAnswer.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);

                // Get answer strings for list view
                String[] answerStrings = new String[q.orderedResultAnswers.size()];
                for (int i = 0; i < q.orderedResultAnswers.size(); i++) {
                    answerStrings[i] = q.orderedResultAnswers.get(i).getText();
                }
                if (q.getType() == MULTIPLE) {
                    listView.setAdapter(new StableArrayAdapter(this, R.layout.question_mc_item, q.orderedResultAnswers));
                } else {
                    listView.setAdapter(new StableArrayAdapter(this, R.layout.question_sc_item, q.orderedResultAnswers));
                }
        }
    }

    class StableArrayAdapter extends ArrayAdapter<ResultAnswer> {
        List<String> itemTexts = new ArrayList<>();
        List<Boolean> correct = new ArrayList<>();
        List<Boolean> correctlyAnswered = new ArrayList<>();
        int itemResourceId;

        public StableArrayAdapter(Context context, int textViewResourceId, List<ResultAnswer> resultAnswers) {
            super(context, textViewResourceId, resultAnswers);
            for (ResultAnswer ra : resultAnswers) {
                itemTexts.add(ra.getText());
                correct.add(ra.isCorrect());
                correctlyAnswered.add(ra.getAnsweredCorrectly());
                itemResourceId = textViewResourceId;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(itemResourceId, container, false);
            }
            int states[][] = {{android.R.attr.state_checked}, {}};
            boolean checked = correctlyAnswered.get(position) == correct.get(position);;
            int defaultColor = -16777216;  // apparent default
            int color = defaultColor;
            switch (itemResourceId) {
                case R.layout.question_sc_item:
                    RadioButton radioButton = convertView.findViewById(R.id.checkBox);
                    radioButton.setText(itemTexts.get(position));
                    if (!correctlyAnswered.get(position) || correct.get(position)) {
                        color = correct.get(position) ? colorCheckboxGreen : colorCheckboxRed;
                    }
                    radioButton.setTextColor(color);
                    CompoundButtonCompat.setButtonTintList(radioButton, new ColorStateList(states, new int[]{color, color}));  // wenn man die Farbe nur im If-Case setzt, werden trotzdem ungewollt boxen gefärbt, kA warum

                    ((RadioButton) convertView.findViewById(R.id.checkBox)).setChecked(checked);
                    break;

                case R.layout.question_mc_item:
                    CheckBox checkbox = convertView.findViewById(R.id.checkBox);
                    boolean isCorrect = correct.get(position);
                    boolean isCorrectlyAnswered = correctlyAnswered.get(position);
                    boolean isChecked = isCorrectlyAnswered == isCorrect;

                    checkbox.setText(itemTexts.get(position));
                    checkbox.setChecked(isChecked);

                    if (isChecked && isCorrect) {
                        color = colorCheckboxGreen;
                    } else if (isChecked && !isCorrect) {
                        color = colorCheckboxRed;
                    } else if (!isChecked && !isCorrect) {
                        // don't color at all (when correctly non-checked)
                        color = defaultColor;
                    } else if (!isChecked && isCorrect) {
                        color = colorCheckboxGreen;
                    }
                    CompoundButtonCompat.setButtonTintList(checkbox, new ColorStateList(states, new int[]{color, color}));
                    checkbox.setTextColor(color);

                    break;
            }

            return convertView;
        }
    }
}
