package com.faselhd.restored.ui

import android.os.SystemClock
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.faselhd.restored.R
import org.hamcrest.Matchers.containsString
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream

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
            awaitResumedActivity("com.faselhd.restored/.ui.PlayerActivity")
            awaitAssertion {
                onView(isAssignableFrom(PlayerView::class.java)).check(matches(isDisplayed()))
            }
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

    private fun awaitResumedActivity(component: String, timeoutMs: Long = 10_000) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var lastState = ""
        while (SystemClock.uptimeMillis() < deadline) {
            val descriptor = instrumentation.uiAutomation.executeShellCommand("dumpsys activity activities")
            lastState = FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
            descriptor.close()
            if (lastState.lineSequence().any { line ->
                    (line.contains("mResumedActivity") || line.contains("topResumedActivity") || line.contains("ResumedActivity")) &&
                        line.contains(component)
                }) return
            SystemClock.sleep(100)
        }
        val relevant = lastState.lineSequence()
            .filter { it.contains("ResumedActivity") || it.contains("mResumedActivity") || it.contains("topResumedActivity") }
            .joinToString(" | ")
        fail("Timed out waiting for Android to report $component resumed. Last task state: $relevant")
    }

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
