/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */
// TODO: Lange im Background, dann abschießen, wieder auf und QA starten.
//     java.lang.NullPointerException: Attempt to invoke virtual method 'android.view.View android.view.View.findViewById(int)' on a null object reference
//        at at.univie.compass.activity.QuestionActivity.getAnswerData(QuestionActivity.java:262)
//        at at.univie.compass.activity.QuestionActivity.onClick(QuestionActivity.java:300)
//        at android.view.View.performClick(View.java:6897)
//        at android.widget.TextView.performClick(TextView.java:12693)
//        at com.google.android.material.button.MaterialButton.performClick(MaterialButton.java:992)
//        at android.view.View$PerformClick.run(View.java:26101)
//        at android.os.Handler.handleCallback(Handler.java:789)
//        at android.os.Handler.dispatchMessage(Handler.java:98)
//        at android.os.Looper.loop(Looper.java:164)
//        at android.app.ActivityThread.main(ActivityThread.java:6944)
//        at java.lang.reflect.Method.invoke(Native Method)
//        at com.android.internal.os.Zygote$MethodAndArgsCaller.run(Zygote.java:327)
//        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1374)


package at.univie.compass.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import at.univie.compass.R;
import at.univie.compass.dto.Answer;
import at.univie.compass.dto.Question;
import at.univie.compass.dto.ResultAnswer;
import at.univie.compass.dto.ResultQuestion;
import at.univie.compass.global.Global;

import static at.univie.compass.dto.QuestionType.MULTIPLE;


public class QuestionActivity extends AppCompatActivity implements View.OnClickListener {
    public LinkedList<Question> questions;
    Question currentQuestion;
    List<Answer> currentQuestionOrderedAnswers;

    // UI
    private ListView listView;
    private EditText editTextAnswer;
    private TextView textViewQuestion, textViewMCHint;
    private Button buttonEnter;

    // Snackbar
    CoordinatorLayout coordinatorLayout;
    ConstraintLayout constraintLayout;
    ConstraintSet constraintSet;

    List<ResultQuestion> resultQuestions;
    ResultQuestion currentResultQuestion;

    static boolean isRunning;

    @Override
    protected void onDestroy() {
        isRunning = false;  // TODO: besser in onStop oder onDestroy (s.o.)??
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("isRunning", isRunning);
        outState.putString("questions", Global.gsonInst.toJson(questions));
        outState.putSerializable("currentQuestion", currentQuestion);
        outState.putSerializable("resultQuestions", Global.gsonInst.toJson(resultQuestions));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

        // UI
        textViewQuestion = findViewById(R.id.textViewQuestion);
        textViewMCHint = findViewById(R.id.textView3);
        listView = findViewById(R.id.listView);
        editTextAnswer = findViewById(R.id.editTextAnswer);
        buttonEnter = findViewById(R.id.buttonEnter);
        buttonEnter.setOnClickListener(this);
        constraintLayout = findViewById(R.id.constraintLayout);
        coordinatorLayout = findViewById(R.id.coordinatorLayout);

        constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);
        constraintSet.connect(buttonEnter.getId(), ConstraintSet.LEFT, constraintLayout.getId(), ConstraintSet.RIGHT, 0);
        constraintSet.applyTo(constraintLayout);

        if (savedInstanceState != null) {
            isRunning = savedInstanceState.getBoolean("isRunning");
            questions = Global.gsonInst.fromJson(savedInstanceState.getString("questions"), new TypeToken<LinkedList<Question>>(){}.getType());//Queue.class);
            resultQuestions = Global.gsonInst.fromJson(savedInstanceState.getString("resultQuestions"), ArrayList.class);// (List<ResultQuestion>) savedInstanceState.getSerializable("resultQuestions");
            currentQuestion = (Question) savedInstanceState.getSerializable("currentQuestion");
            showQuestion(currentQuestion);
        } else {
            questions = new LinkedList<>((List<Question>) getIntent().getSerializableExtra("questions"));  // see https://stackoverflow.com/questions/12300886/linkedlist-put-into-intent-extra-gets-recast-to-arraylist-when-retrieving-in-nex
            currentQuestion = questions.poll();
            showQuestion(currentQuestion);
            resultQuestions = new ArrayList<>();
            isRunning = true;  // TODO: besser in onStart oder onCreate??
        }
    }

    public void onRadioButtonClicked(View v) {
        // hacky way to connect all radio buttons on single choice questions
        RadioButton clicked = (RadioButton) v;
        ListView lv = (ListView) clicked.getParent();
        int i_clicked = lv.getPositionForView(clicked);

        for (int i = 0; i < listView.getCount(); i++) {
            RadioButton radioButton = (RadioButton) listView.getChildAt(i);
            if (i != i_clicked)
                radioButton.setChecked(false);
        }
    }

    @Override
    public void onBackPressed() {} // disallow

    public void showQuestion(Question q) {
        textViewQuestion.setText(q.getText());
        textViewMCHint.setVisibility(View.GONE);

        currentResultQuestion = new ResultQuestion();
        currentResultQuestion.setResultAnswers(new HashSet<ResultAnswer>());
        currentResultQuestion.setText(q.getText());
        currentResultQuestion.setCategory(q.getCategory());
        currentResultQuestion.setDifficulty(q.getDifficulty());
        currentResultQuestion.setType(q.getType());

        switch (q.getType()) {
            case INPUT:
                listView.setVisibility(View.GONE);
                editTextAnswer.setText("");
                editTextAnswer.setVisibility(View.VISIBLE);

                // re-position enter button - maybe there is a more elegant solution for this
                constraintSet.clone(constraintLayout);
                constraintSet.connect(buttonEnter.getId(), ConstraintSet.TOP, editTextAnswer.getId(), ConstraintSet.BOTTOM, 0);
                constraintSet.applyTo(constraintLayout);

                break;

            case MULTIPLE:
                textViewMCHint.setVisibility(View.VISIBLE);
                textViewMCHint.setText(R.string.mc_hint);
            case SINGLE:
                editTextAnswer.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
                closeKeyboard();

                // reposition enter button
                constraintSet.clone(constraintLayout);
                constraintSet.connect(buttonEnter.getId(), ConstraintSet.TOP, listView.getId(), ConstraintSet.BOTTOM, 0);
                constraintSet.applyTo(constraintLayout);

                // Order answers
                currentQuestionOrderedAnswers = new ArrayList<>();
                currentQuestionOrderedAnswers.addAll(q.getAnswers());

                // Get answer strings for list view
                String[] answerStrings = new String[q.getAnswers().size()];
                for (int i = 0; i < q.getAnswers().size(); i++) {
                    answerStrings[i] = currentQuestionOrderedAnswers.get(i).getText();
                }
                if (q.getType() == MULTIPLE) {
                    listView.setAdapter(new ArrayAdapter<String>(this, R.layout.question_mc_item, answerStrings));
                } else {
                    listView.setAdapter(new ArrayAdapter<String>(this, R.layout.question_sc_item, answerStrings));
                }
                break;
        }
    }

    private void nextQuestion() {
        resultQuestions.add(currentResultQuestion);

        if (!questions.isEmpty()) {
            // there is a next question
            currentQuestion = questions.poll();
            showQuestion(currentQuestion);
            Global.snack(this, getString(R.string.next_question), Snackbar.LENGTH_LONG);
        }
        else {
            // it was the last question
            Intent data = new Intent();
            String dstr =  Global.gsonInst.toJson(resultQuestions);
            data.putExtra("resultsQuestion", dstr);
            setResult(RESULT_OK, data);
            finish();
        }
    }

    public Pair<List<ResultAnswer>, Boolean> getAnswerData() {
        boolean emptyAnswer = true;
        List<Answer> correctAnswers = new ArrayList<>();
        HashSet<ResultAnswer> resultAnswers = new HashSet<>();
        List<ResultAnswer> resultAnswersOrdered = new ArrayList<>();

        for (Answer answer : currentQuestion.getAnswers()) {
            if (answer.getCorrect()) {
                correctAnswers.add(answer);
            }
        }
        switch (currentQuestion.getType()) {
            case INPUT:
                String typedText = editTextAnswer.getText().toString();
                ResultAnswer resultAnswerInput = new ResultAnswer();
                resultAnswerInput.setText(currentQuestion.getAnswers().iterator().next().getText());
                resultAnswerInput.answeredText = typedText;
                emptyAnswer = resultAnswerInput.answeredText.equals("");

                String normTypedText = typedText.trim().toLowerCase();

                boolean correct = false;
                for (Answer validAnswer: correctAnswers) {
                    if (normTypedText.equals(validAnswer.getText().trim().toLowerCase())) {
                        // Typed-in text is the same as the correct text
                        correct = true;
                        break;
                    }
                }

                resultAnswerInput.setCorrect(correct);
                resultAnswerInput.setAnsweredCorrectly(correct);

                resultAnswers.add(resultAnswerInput);
                resultAnswersOrdered.add(resultAnswerInput);
                break;

            case SINGLE:
                // TODO: was wenn kein radio button selektiert wird? Hier: empty string
                for (int i = 0; i < listView.getCount(); i++) {
                    RadioButton cb = listView.getChildAt(i).findViewById(R.id.checkBox);
                    ResultAnswer cbResultAnswer = new ResultAnswer();
                    Answer cbAnswer = currentQuestionOrderedAnswers.get(i);
                    cbResultAnswer.setText(cbAnswer.getText());
                    cbResultAnswer.setCorrect(cbAnswer.getCorrect());
                    cbResultAnswer.setAnsweredCorrectly(cbAnswer.getCorrect() == cb.isChecked());
                    resultAnswers.add(cbResultAnswer);
                    resultAnswersOrdered.add(cbResultAnswer);
                    if (cb.isChecked())
                        emptyAnswer = false;
                }
                break;

            case MULTIPLE:
                for (int i = 0; i < listView.getCount(); i++) {
                    CheckBox cb = listView.getChildAt(i).findViewById(R.id.checkBox);
                    ResultAnswer cbResultAnswer = new ResultAnswer();
                    Answer cbAnswer = currentQuestionOrderedAnswers.get(i);
                    cbResultAnswer.setText(cbAnswer.getText());
                    cbResultAnswer.setCorrect(cbAnswer.getCorrect());
                    cbResultAnswer.setAnsweredCorrectly(cbAnswer.getCorrect() == cb.isChecked());
                    resultAnswers.add(cbResultAnswer);
                    resultAnswersOrdered.add(cbResultAnswer);
                    if (cb.isChecked())
                        emptyAnswer = false;
                }
                break;
        }
        return new Pair<>(resultAnswersOrdered, emptyAnswer);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonEnter:
                Pair<List<ResultAnswer>, Boolean> answerData = getAnswerData();
                List<ResultAnswer> resultAnswers = answerData.first;

                // question is correctly answered when all answers (e.g. MC items) are correct
                boolean questionCorrect = true;
                for (ResultAnswer ra : resultAnswers) {
                    if (!ra.getAnsweredCorrectly()) {
                        questionCorrect = false;
                        break;
                    }
                }
                currentResultQuestion.setAnsweredCorrectly(questionCorrect);
                currentResultQuestion.getResultAnswers().addAll(resultAnswers);
                currentResultQuestion.orderedResultAnswers = resultAnswers;

                nextQuestion();
                break;
        }
    }

    private void closeKeyboard() {
        View view = findViewById(R.id.coordinatorLayout);
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
