package com.faselhd.restored.ui

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.faselhd.restored.R
import org.hamcrest.Matchers.containsString
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaginationFlowRuntimeTest {
    @Test
    fun catalogLoadMoreKeepsItemsAndReachesSecondDetailsAndPlayer() {
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText("Recovered Series")
            onView(withId(R.id.loadMoreButton)).check(matches(isDisplayed()))
            onView(withId(R.id.loadMoreButton)).perform(click())
            awaitText("Recovered Series 2")
            onView(withText("Recovered Series 2")).perform(click())
            awaitDetails()
            onView(withId(R.id.playButton)).perform(click())
            awaitActivity("com.faselhd.restored/.ui.PlayerActivity")
        }
    }

    private fun awaitText(text: String) {
        awaitAssertion { onView(withText(text)).check(matches(isDisplayed())) }
    }

    private fun awaitDetails() {
        awaitAssertion { onView(withId(R.id.detailsText)).check(matches(withText(containsString("S1E1")))) }
    }

    private fun awaitActivity(component: String, timeoutMs: Long = 10_000) {
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            val descriptor = instrumentation.uiAutomation.executeShellCommand("dumpsys activity activities")
            val text = descriptor.fileDescriptor.let { fd -> java.io.FileInputStream(fd).bufferedReader().use { it.readText() } }
            descriptor.close()
            if (text.lineSequence().any { line -> line.contains("ResumedActivity") && line.contains(component) }) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Timed out waiting for $component")
    }

    private fun awaitAssertion(timeoutMs: Long = 10_000, assertion: () -> Unit) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var last: Throwable? = null
        while (SystemClock.uptimeMillis() < deadline) {
            try {
                assertion()
                return
            } catch (error: Throwable) {
                last = error
                SystemClock.sleep(100)
            }
        }
        throw AssertionError("Timed out waiting for runtime state: ${last?.message}")
    }
}
