package com.sai.decisiongraveyard.ui.add;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sai.decisiongraveyard.R;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AddDecisionFragmentTest {

    @Test
    public void launchFragment_displaysAllViews() {
        FragmentScenario.launchInContainer(AddDecisionFragment.class);

        onView(withId(R.id.etTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.etDescription)).check(matches(isDisplayed()));
        onView(withId(R.id.actCategory)).check(matches(isDisplayed()));
        onView(withId(R.id.btnSaveDecision)).check(matches(isDisplayed()));
    }

    @Test
    public void emptyTitle_showsErrorOnSave() {
        FragmentScenario.launchInContainer(AddDecisionFragment.class);

        onView(withId(R.id.btnSaveDecision)).perform(click());

        // Check if TextInputLayout error is set (this is tricky with Espresso, usually we check if error text is displayed)
        onView(withText("Title is required.")).check(matches(isDisplayed()));
    }

    @Test
    public void typingTitle_updatesField() {
        FragmentScenario.launchInContainer(AddDecisionFragment.class);

        String testTitle = "New Car Decision";
        onView(withId(R.id.etTitle)).perform(typeText(testTitle), closeSoftKeyboard());

        onView(withId(R.id.etTitle)).check(matches(withText(testTitle)));
    }
}
