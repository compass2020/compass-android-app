/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import at.univie.compass.activity.MainActivity;
import at.univie.compass.dto.Course;
import at.univie.compass.dto.GameModus;
import at.univie.compass.dto.ResultCourse;
import at.univie.compass.dto.SharedCourse;
import at.univie.compass.global.Global;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ResultListActivityTest {

    ResultCourse r101_null, r101_test, r101_2;

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void initResultCourseData() {
        cleanup();

        r101_null = new ResultCourse();
        r101_null.setResultAdditionalinfo(null);
        r101_null.setResultControlpoints(null);
        r101_null.setViewCode(null);
        r101_null.setId(null);
        r101_null.setNickName(null);
        r101_null.setSharedCourse(null);
        r101_null.setTimeStampFinished(null);
        r101_null.setTimeStampStarted(null);
        r101_null.setTotalDurationInMillis(null);

        r101_test = new ResultCourse();
        r101_test.setResultAdditionalinfo(null);
        r101_test.setResultControlpoints(null);
        r101_test.setViewCode("SomeViewCode");
        r101_test.setId(null);
        r101_test.setNickName("SomeNickname");
        r101_test.setSharedCourse(null);
        r101_test.setTimeStampFinished(null);
        r101_test.setTimeStampStarted(null);
        r101_test.setTotalDurationInMillis(null);

        r101_2 = new ResultCourse();
        SharedCourse sc = new SharedCourse();
        Course c = new Course();
        c.setName("Test Course");
        sc.setCourse(c);
        sc.setId(1234L);
        sc.setGameModus(GameModus.GPS);
        r101_2.setResultAdditionalinfo(null);
        r101_2.setResultControlpoints(null);
        r101_2.setViewCode("SomeViewCode");
        r101_2.setId(null);
        r101_2.setNickName("SomeNickname");
        r101_2.setSharedCourse(sc);
        r101_2.setTimeStampStarted(Global.dateToString(Global.now()));
        r101_2.setTimeStampFinished(Global.dateToString(Global.now()));
        r101_2.setTotalDurationInMillis(null);

        Global.storeViewCode(getInstrumentation().getTargetContext().getApplicationContext(), r101_null);
        Global.storeViewCode(getInstrumentation().getTargetContext().getApplicationContext(), r101_test);
        Global.storeViewCode(getInstrumentation().getTargetContext().getApplicationContext(), r101_2);
    }

    @After
    public void cleanup() {
        Global.deleteAllStoredViewCodes(getInstrumentation().getTargetContext().getApplicationContext());
        Global.deleteStoredNickname(getInstrumentation().getTargetContext().getApplicationContext());
        Global.deleteAllStoredResults(getInstrumentation().getTargetContext().getApplicationContext());
        Global.deleteStoredCourse(getInstrumentation().getTargetContext().getApplicationContext());
    }

    @Test
    public void resultActivityTest() {
        Context applicationContext = getInstrumentation().getTargetContext().getApplicationContext();

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
        editText2.perform(replaceText("Tester"), closeSoftKeyboard());

        ViewInteraction materialButton = onView(
                allOf(withId(android.R.id.button1), withText(applicationContext.getString(R.string.ok)),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3)));
        materialButton.perform(scrollTo(), click());

        ViewInteraction actionMenuItemView = onView(
                allOf(withId(R.id.menu_toggle),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.toolbar),
                                        1),
                                0),
                        isDisplayed()));
        actionMenuItemView.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction materialTextView = onView(
                allOf(withId(R.id.title), withText(applicationContext.getString(R.string.show_results)),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.content),
                                        1),
                                0),
                        isDisplayed()));
        materialTextView.perform(click());
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
