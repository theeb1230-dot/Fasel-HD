package com.faselhd.restored.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.faselhd.restored.R
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuntimeFlowSmokeTest {
    @Before fun setUp() = Intents.init()
    @After fun tearDown() = Intents.release()

    @Test
    fun catalogDetailsSourcesDecisionNavigatesToNativePlayer() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Recovered Series")).check(matches(isDisplayed()))
            onView(withText("Recovered Series")).perform(click())
            onView(withId(R.id.detailsText)).check(matches(withText(org.hamcrest.Matchers.containsString("S1E1"))))
            onView(withId(R.id.playButton)).check(matches(isEnabled())).perform(click())
            intended(hasComponent(PlayerActivity::class.java.name))
        }
    }
}
