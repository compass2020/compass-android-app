/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass;


import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.SeekBar;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.test.uiautomator.UiDevice;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import at.univie.compass.activity.MainActivity;
import at.univie.compass.activity.RunActivity;
import at.univie.compass.dto.Answer;
import at.univie.compass.dto.ControlPoint;
import at.univie.compass.dto.Course;
import at.univie.compass.dto.GameModus;
import at.univie.compass.dto.OrienteeringMap;
import at.univie.compass.dto.Question;
import at.univie.compass.dto.QuestionType;
import at.univie.compass.dto.ResultCourse;
import at.univie.compass.dto.SharedCourse;
import at.univie.compass.global.Global;
import at.univie.compass.global.ZipperUtil;
import at.univie.compass.location.LocationUpdateService;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;





@LargeTest
@RunWith(AndroidJUnit4.class)
public class FullRunTestSimple {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION",
                    "android.permission.ACCESS_COARSE_LOCATION",
                    "android.permission.CHANGE_WIFI_STATE",
                    "android.permission.CAMERA",
                    "android.permission.BLUETOOTH"
                    );

    @Before
    public void prepare() {
        Global.DEFAULT_BACKEND_HOST = "78.104.12.139";  // don't use actual server!
        WifiManager wifiManager = (WifiManager) getInstrumentation().getContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);
    }

    @Before
    @After
    public void cleanup() {
        Global.deleteAllStoredViewCodes(getInstrumentation().getTargetContext().getApplicationContext());
        Global.deleteStoredNickname(getInstrumentation().getTargetContext().getApplicationContext());
        Global.deleteAllStoredResults(getInstrumentation().getTargetContext().getApplicationContext());
        Global.deleteStoredCourse(getInstrumentation().getTargetContext().getApplicationContext());
        Global.storeBLEaddress(getInstrumentation().getTargetContext().getApplicationContext(), "");
        Global.storeBLEname(getInstrumentation().getTargetContext().getApplicationContext(),"");
    }

    private Activity getActivityInstance() {
        final Activity[] currentActivity = {null};

        getInstrumentation().runOnMainSync(new Runnable(){
            public void run(){
                Collection<Activity> resumedActivity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
                Iterator<Activity> it = resumedActivity.iterator();
                currentActivity[0] = it.next();
            }
        });

        return currentActivity[0];
    }

    private SharedCourse randomSharedCourse(int n_cps, Integer[] idx_skippable, Integer[] idx_questions) {
        SharedCourse sc = new SharedCourse();
        Course c = new Course();

        Set<ControlPoint> cps = new HashSet<>();

        for (int i = 0; i < n_cps; i++) {
            double randomLongitude = -180 + Math.random() * 360;
            double randomLatitude = -90 + Math.random() * 180;
            ControlPoint cp = new ControlPoint();
            cp.setLongitude(randomLongitude);
            cp.setLatitude(randomLatitude);
            cp.setRadius(1);
            cp.setSequence(i);
            cp.setSkippable(Arrays.asList(idx_skippable).contains(i));
            if (Arrays.asList(idx_questions).contains(i)) {
                Set<Question> questions = new HashSet<>();
                Map<Integer, QuestionType> i2type = new HashMap<>();
                i2type.put(0, QuestionType.SINGLE);
                i2type.put(1, QuestionType.SINGLE);
                i2type.put(2, QuestionType.INPUT);
                i2type.put(3, QuestionType.MULTIPLE);
                for (int j = 0; j < 4; j++) {
                    Question q = new Question();
                    q.setType(i2type.get(j));
                    q.setText("Example question ,v€@²³+~");
                    if (q.getType() == QuestionType.INPUT) {
                        Answer a = new Answer();
                        a.setCorrect(true);
                        a.setText("True Answer");
                        Set<Answer> answers = new HashSet<>();
                        answers.add(a);
                        q.setAnswers(answers);
                    } else {
                        Random random = new Random();
                        Set<Answer> answers = new HashSet<>();
                        for (int k = 0; k < 10; k++) {
                            Answer a = new Answer();
                            a.setCorrect(random.nextBoolean());
                            String answerText = a.getCorrect() ? "True answer" : "False answer";
                            a.setText(answerText);
                            answers.add(a);
                        }
                        q.setAnswers(answers);
                    }
                    questions.add(q);
                }
                cp.setQuestions(questions);
            }
            cps.add(cp);
        }

        c.setControlpoints(cps);
        c.setName("Victoria Park");
        c.setOrienteeringMap(new OrienteeringMap());

        sc.setGameModus(GameModus.GPS);
        sc.setCourse(c);
        sc.setId(1001L);
        return sc;
    }

    private SharedCourse fullyRandomSharedCourse(int n_cps) {
        Random random = new Random();
        List<Integer> idx_skippable = new ArrayList<>();
        List<Integer> idx_questions = new ArrayList<>();

        for (int i = 0; i < n_cps; i++) {
            if (random.nextBoolean()) {
                idx_skippable.add(i);
            }
            if (random.nextBoolean()) {
                idx_questions.add(i);
            }
        }
        Integer[] idx_skippableArray = new Integer[idx_skippable.size()];
        Integer[] idx_questionsArray = new Integer[idx_questions.size()];
        idx_skippableArray = idx_skippable.toArray(idx_skippableArray);
        idx_questionsArray = idx_questions.toArray(idx_questionsArray);

        return randomSharedCourse(n_cps, idx_skippableArray, idx_questionsArray);
    }

    @Test
    public void course_regular() throws Throwable {
        fullRunTestSimple(randomSharedCourse(5, new Integer[]{}, new Integer[]{}), 0, SkipMode.NO, false, false);
    }

    @Test
    public void course_regular_2() throws Throwable {
        fullRunTestSimple(randomSharedCourse(5, new Integer[]{}, new Integer[]{}), 2000, SkipMode.NO, false, false);
    }

    @Test
    public void course_heart() throws Throwable {
        fullRunTestSimple(randomSharedCourse(10, new Integer[]{}, new Integer[]{}), 100, SkipMode.NO, true, false);
    }

    @Test
    public void course_skippable_noskip() throws Throwable {
        fullRunTestSimple(randomSharedCourse(5, new Integer[]{3}, new Integer[]{}), 1, SkipMode.NO, false, false);
    }

    @Test
    public void course_skippable_skip() throws Throwable {
        fullRunTestSimple(randomSharedCourse(5, new Integer[]{3}, new Integer[]{}), 1, SkipMode.YES, false, false);
    }

    @Test
    public void course_skippable_skip2() throws Throwable {
        fullRunTestSimple(randomSharedCourse(5, new Integer[]{2, 3}, new Integer[]{}), 1, SkipMode.YES, false, false);
    }

    @Test
    public void course_skippable_skip3() throws Throwable {
        fullRunTestSimple(randomSharedCourse(6, new Integer[]{2, 3, 4}, new Integer[]{}), 1, SkipMode.YES, false, false);
    }

    @Test
    public void course_skippable_3_noskip() throws Throwable {
        fullRunTestSimple(randomSharedCourse(6, new Integer[]{2, 3, 4}, new Integer[]{}), 1, SkipMode.NO, false, false);
    }

    @Test
    public void course_skippable_skip4() throws Throwable {
        fullRunTestSimple(randomSharedCourse(65, new Integer[]{2, 3, 4, 5, 10, 11, 12, 20, 21, 30, 40, 42, 44, 50, 51, 53, 60, 62, 63}, new Integer[]{}), 1, SkipMode.RANDOM, false, false);
    }

    @Test
    public void course_q1() throws Throwable {
        fullRunTestSimple(randomSharedCourse(3, new Integer[]{}, new Integer[]{1}), 1, SkipMode.NO, false, false);
    }

    @Test
    public void course_random() throws Throwable {
        fullRunTestSimple(fullyRandomSharedCourse(20), 10, SkipMode.RANDOM, false, false);
    }

    public static ViewAction setProgress(final int progress) {
        return new ViewAction() {
            @Override
            public void perform(UiController uiController, View view) {
                SeekBar seekBar = (SeekBar) view;
                seekBar.setProgress(progress);
            }
            @Override
            public String getDescription() {
                return "Set a progress on a SeekBar";
            }
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isAssignableFrom(SeekBar.class);
            }
        };
    }

    enum SkipMode {
        YES, NO, RANDOM
    }

    public void fullRunTestSimple(SharedCourse sc, int n_additional_samples_per_cp, SkipMode skipMode, boolean heart, boolean try_functionality) throws Throwable {
        int n_processed_locations = 0;
        int n_processed_heartrates = 0;

        // 1. Enter nickname
        ViewInteraction editText = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(R.id.custom),
                                0),
                        0),
                        isDisplayed()));
        editText.perform(click());

        ViewInteraction editText2 = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(R.id.custom),
                                0),
                        0),
                        isDisplayed()));
        editText2.perform(replaceText("bla"), closeSoftKeyboard());

        ViewInteraction materialButton = onView(
                allOf(withId(android.R.id.button1), withText(R.string.ok),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3)));
        materialButton.perform(scrollTo(), click());

        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // 2. Set course
        MainActivity mainActivity = (MainActivity) getActivityInstance();
        ControlPoint[] controlPointsExceptStart = new ControlPoint[sc.getCourse().getControlpoints().size()-1];
        ControlPoint startCP = null;
        for (ControlPoint cp : sc.getCourse().getControlpoints()) {
            int seq = cp.getSequence();
            if (seq != 0) {
                controlPointsExceptStart[seq-1] = cp;
            } else {
                startCP = cp;
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.setSelectedCourse(sc);
            }
        });

        // 3. Start RunActivity
        ViewInteraction materialCardView2 = onView(
                allOf(withId(R.id.run_card),
                        childAtPosition(
                                allOf(withId(R.id.main_layout),
                                        childAtPosition(
                                                withId(R.id.linearLayout),
                                                1)),
                                2),
                        isDisplayed()));
        materialCardView2.perform(click());

        RunActivity runActivity = (RunActivity) getActivityInstance();
        runActivity.unbindLocationUpdateService();
        runActivity.locationUpdateService = null;
        if (heart) {
            runActivity.currentHeartRateView = runActivity.findViewById(R.id.currentHeartRateView);
            runActivity.currentHeartRateView.setVisibility(View.VISIBLE);
            runActivity.hrm_ble_address = "BÖA";
        }

        Map<String, Location> filter2location = new HashMap<>();
        Location nextLocation = new Location("");
        nextLocation.setLongitude(startCP.getLongitude());
        nextLocation.setLatitude(startCP.getLatitude());
        filter2location.put(LocationUpdateService.bestFilter.name, nextLocation);

        // 4. Process start location
        n_processed_locations += 1;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                runActivity.processNewLocation(filter2location);
            }
        });

        // 5. Click start
        onView(withId(R.id.buttonStart)).check(matches(isEnabled()));
        ViewInteraction materialButton2 = onView(
                allOf(withId(R.id.buttonStart), withText("Start!"),
                        childAtPosition(
                                allOf(withId(R.id.baseLayout),
                                        childAtPosition(
                                                withId(R.id.coordinatorLayout),
                                                0)),
                                4),
                        isDisplayed()));
        materialButton2.perform(click());

        if (try_functionality) {
            ViewInteraction appCompatImageButton = onView(
                    allOf(withId(R.id.buttonWhereAmI),
                            childAtPosition(
                                    allOf(withId(R.id.baseLayout),
                                            childAtPosition(
                                                    withId(R.id.coordinatorLayout),
                                                    0)),
                                    9),
                            isDisplayed()));
            appCompatImageButton.perform(click());

            ViewInteraction materialButton4 = onView(
                    allOf(withId(android.R.id.button1), withText(R.string.show_my_location),
                            childAtPosition(
                                    childAtPosition(
                                            withId(R.id.buttonPanel),
                                            0),
                                    0)));
            materialButton4.perform(scrollTo(), click());

            ViewInteraction appCompatImageButton3 = onView(
                    allOf(withId(R.id.buttonWhereAmI),
                            childAtPosition(
                                    allOf(withId(R.id.baseLayout),
                                            childAtPosition(
                                                    withId(R.id.coordinatorLayout),
                                                    0)),
                                    9),
                            isDisplayed()));
            appCompatImageButton3.perform(click());

            ViewInteraction materialButton5 = onView(
                    allOf(withId(android.R.id.button3), withText("Skip this control point"),
                            childAtPosition(
                                    childAtPosition(
                                            withId(R.id.buttonPanel),
                                            0),
                                    3)));
            materialButton5.perform(scrollTo(), click());
        }

        // 6. Process control point locations
        for (int j = 0; j < controlPointsExceptStart.length; j++) {
            ControlPoint cp = controlPointsExceptStart[j];
            ControlPoint next_cp = j+1 < controlPointsExceptStart.length ? controlPointsExceptStart[j+1] : null;
            Map<String, Location> f2l_controlpoint = new HashMap<>();
            Location cp_location = new Location("");
            cp_location.setLongitude(cp.getLongitude());
            cp_location.setLatitude(cp.getLatitude());
            f2l_controlpoint.put(LocationUpdateService.bestFilter.name, cp_location);

            for (int i = 0; i < n_additional_samples_per_cp; i++) {
                n_processed_locations += 1;
                if (heart) n_processed_heartrates += 1;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Location dummyLocation = new Location("");
                        dummyLocation.setLongitude(-180 + Math.random() * 360);
                        dummyLocation.setLatitude(-90 + Math.random() * 180);
                        Map<String, Location> f2l_dummy = new HashMap<>();
                        f2l_dummy.put(LocationUpdateService.bestFilter.name, dummyLocation);
                        if (heart) {
                            runActivity.currentHeartRateView.setVisibility(View.VISIBLE);
                            runActivity.displayData(String.valueOf(60 + (int)(Math.random() * ((150 - 60) + 1))));
                        }
                        runActivity.processNewLocation(f2l_dummy);
                    }
                });
            }

            n_processed_locations += 1;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    runActivity.processNewLocation(f2l_controlpoint);
                }
            });

            if (cp.getQuestions().size() > 0) {
                for (Question q: cp.getQuestions()) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ViewInteraction materialButtonQ = onView(
                            allOf(withId(R.id.buttonEnter), withText(R.string.enter),
                                    childAtPosition(
                                            allOf(withId(R.id.buttonsContainer),
                                                    childAtPosition(
                                                            withId(R.id.constraintLayout),
                                                            2)),
                                            0)));
                    materialButtonQ.perform(scrollTo(), click());
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (next_cp != null && next_cp.getSkippable()) {
                boolean makeSkip;
                if (skipMode == SkipMode.YES) {
                    makeSkip = true;
                }
                else if (skipMode == SkipMode.NO) {
                    makeSkip = false;
                }
                else { //if (skipMode == SkipMode.RANDOM) {
                    Random random = new Random();
                    makeSkip = random.nextBoolean();
                }
                ViewInteraction materialButton3 = onView(
                        allOf(withId(R.id.buttonBorg), withText(R.string.ok),
                                childAtPosition(
                                        allOf(withId(R.id.borgdialoglayout),
                                                childAtPosition(
                                                        withId(android.R.id.content),
                                                        0)),
                                        3),
                                isDisplayed()));
                ViewInteraction borgSlideBar = onView(
                        allOf(withId(R.id.seekBarBorg),
                                isDisplayed()));

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                borgSlideBar.perform(setProgress(makeSkip ? 10 : 1));
                materialButton3.perform(click());

                if (makeSkip) {
                    ViewInteraction materialButton4 = onView(
                            allOf(withText(R.string.ok), isDisplayed()));
                    materialButton4.perform(click());
                    j++;  // skip
                }
            }
        }

        pressBack();

        // 7. Check result
        ResultCourse[] rcs = Global.getStoredResults(getInstrumentation().getTargetContext().getApplicationContext());

        assertNotNull(rcs);
        assertEquals(rcs.length, 1);
        assertNotEquals(new String(rcs[0].getResultAdditionalinfo().getGpxTrack()), "");
        int emptyDescs = StringUtils.countOccurrencesOf(new String(rcs[0].getResultAdditionalinfo().getGpxTrack()), "<desc></desc>");
        int allDescs = StringUtils.countOccurrencesOf(new String(rcs[0].getResultAdditionalinfo().getGpxTrack()), "<desc>");
        assertTrue(StringUtils.countOccurrencesOf(new String(ZipperUtil.decompress(rcs[0].getResultAdditionalinfo().getGpxTrack())), "<trkpt ") >= controlPointsExceptStart.length);  // finish wird nicht gezählt... also gesamt - 1
        assertEquals(StringUtils.countOccurrencesOf(new String(ZipperUtil.decompress(rcs[0].getResultAdditionalinfo().getGpxTrack())), "<trkpt "), n_processed_locations-1);  // finish wird nicht gezählt... also gesamt - 1
        assertEquals(StringUtils.countOccurrencesOf(new String(ZipperUtil.decompress(rcs[0].getResultAdditionalinfo().getHeartRate())), "<heartrate>"), n_processed_heartrates);  // finish wird nicht gezählt... also gesamt - 1
//        assertEquals(new String(rcs[0].getResultAdditionalinfo().getHeartRate()), "<heartrate>");  // finish wird nicht gezählt... also gesamt - 1
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
