package com.faselhd.restored.ui

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
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
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Assert.fail
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
            awaitDisplayedText("Recovered Series")
            onView(withText("Recovered Series")).perform(click())
            awaitTextContaining(R.id.detailsText, "S1E1")
            awaitEnabled(R.id.playButton)
            onView(withId(R.id.playButton)).perform(click())
            awaitPlayerIntent()
        }
    }

    private fun awaitDisplayedText(text: String) = awaitAssertion {
        onView(withText(text)).check(matches(isDisplayed()))
    }

    private fun awaitTextContaining(id: Int, text: String) = awaitAssertion {
        onView(withId(id)).check(matches(withText(containsString(text))))
    }

    private fun awaitEnabled(id: Int) = awaitAssertion {
        onView(withId(id)).check(matches(isEnabled()))
    }

    private fun awaitPlayerIntent() = awaitAssertion {
        intended(hasComponent(PlayerActivity::class.java.name))
    }

    /**
     * MainActivity intentionally performs provider work in lifecycleScope rather than on the UI
     * thread. Espresso cannot infer idleness from arbitrary coroutines, so the smoke must wait for
     * observable acceptance states instead of racing the first frame or using one fixed sleep.
     */
    private fun awaitAssertion(timeoutMs: Long = 10_000, assertion: () -> Unit) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var lastFailure: Throwable? = null
        while (SystemClock.uptimeMillis() < deadline) {
            try {
                assertion()
                return
            } catch (failure: Throwable) {
                if (failure is NoMatchingViewException || failure is AssertionError) {
                    lastFailure = failure
                    SystemClock.sleep(100)
                } else {
                    throw failure
                }
            }
        }
        fail("Timed out waiting for runtime acceptance state: ${lastFailure?.message}")
    }
}
