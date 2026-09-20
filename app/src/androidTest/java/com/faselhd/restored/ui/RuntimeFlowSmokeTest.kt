package com.faselhd.restored.ui

import android.app.Activity
import android.app.Application
import android.os.Bundle
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
import com.faselhd.restored.R
import org.hamcrest.Matchers.containsString
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class RuntimeFlowSmokeTest {
    @Test
    fun catalogDetailsSourcesDecisionNavigatesToNativePlayer() {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as Application
        val playerResumed = CountDownLatch(1)
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity is PlayerActivity) playerResumed.countDown()
            }
            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        }
        application.registerActivityLifecycleCallbacks(callbacks)
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                awaitDisplayedText("Recovered Series")
                onView(withText("Recovered Series")).perform(click())
                awaitTextContaining(R.id.detailsText, "S1E1")
                awaitEnabled(R.id.playButton)
                onView(withId(R.id.playButton)).perform(click())
                if (!playerResumed.await(10, TimeUnit.SECONDS)) {
                    fail("Timed out waiting for PlayerActivity.onResume")
                }
            }
        } finally {
            application.unregisterActivityLifecycleCallbacks(callbacks)
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
