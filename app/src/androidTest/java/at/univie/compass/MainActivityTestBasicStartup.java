/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass;


import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
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
import at.univie.compass.global.Global;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MainActivityTestBasicStartup {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.CAMERA");

    @Before
    @After
    public void cleanup() {
        Global.deleteAllStoredViewCodes(getInstrumentation().getTargetContext().getApplicationContext());
        Global.deleteStoredNickname(getInstrumentation().getTargetContext().getApplicationContext());
        Global.deleteAllStoredResults(getInstrumentation().getTargetContext().getApplicationContext());
        Global.deleteStoredCourse(getInstrumentation().getTargetContext().getApplicationContext());
    }

    @Test
    public void mainActivityTestBasicStartup() {
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
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3)));
        materialButton.perform(scrollTo(), click());

        ViewInteraction cardView = onView(
                allOf(withId(R.id.scan_card),
                        withParent(allOf(withId(R.id.main_layout),
                                withParent(withId(R.id.linearLayout)))),
                        isDisplayed()));
        cardView.check(matches(isDisplayed()));

        ViewInteraction textView = onView(
                allOf(withId(R.id.versionTextview),
                        withParent(allOf(withId(R.id.main_layout),
                                withParent(withId(R.id.linearLayout)))),
                        isDisplayed()));
        textView.check(matches(isDisplayed()));

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.welcomeTextview),
                        withParent(allOf(withId(R.id.main_layout),
                                withParent(withId(R.id.linearLayout)))),
                        isDisplayed()));
        textView2.check(matches(isDisplayed()));

        ViewInteraction materialCardView = onView(
                allOf(withId(R.id.scan_card),
                        childAtPosition(
                                allOf(withId(R.id.main_layout),
                                        childAtPosition(
                                                withId(R.id.linearLayout),
                                                1)),
                                1),
                        isDisplayed()));
        materialCardView.perform(click());
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
