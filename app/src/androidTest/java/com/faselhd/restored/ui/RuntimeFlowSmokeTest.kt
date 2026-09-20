package com.faselhd.restored.ui

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.faselhd.restored.R
import org.hamcrest.Matchers.containsString
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuntimeFlowSmokeTest {
    @Test
    fun catalogDetailsSourcesDecisionNavigatesToNativePlayer() {
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitDisplayedText("Recovered Series")
            onView(withText("Recovered Series")).perform(click())
            awaitTextContaining(R.id.detailsText, "S1E1")
            awaitEnabled(R.id.playButton)
            onView(withId(R.id.playButton)).perform(click())
            awaitPlayerActivityResumed()
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

    /**
     * Verify the actual internal activity lifecycle rather than relying on Espresso-Intents' intent
     * recorder. The latter produced a false negative in CI even though the click completed; a
     * RESUMED PlayerActivity is stronger runtime evidence that Android resolved and launched the
     * internal native player route.
     */
    private fun awaitPlayerActivityResumed() = awaitAssertion {
        var resumed = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            resumed = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .any { it is PlayerActivity }
        }
        if (!resumed) throw AssertionError("PlayerActivity is not RESUMED")
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
