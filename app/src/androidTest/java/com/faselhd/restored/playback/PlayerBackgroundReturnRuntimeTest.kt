package com.faselhd.restored.playback

import android.content.Intent
import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.faselhd.restored.core.PlaybackKind
import com.faselhd.restored.ui.PlayerActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerBackgroundReturnRuntimeTest {
    @Test
    fun backgroundAndReturnRestoresRetryablePlaybackSurface() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, PlayerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(PlayerActivity.EXTRA_URI, INVALID_HLS)
            putExtra(PlayerActivity.EXTRA_KIND, PlaybackKind.HLS.name)
        }

        ActivityScenario.launch<PlayerActivity>(intent).use { scenario ->
            await("initial error surface") {
                isVisible(scenario, PlayerActivity.ERROR_VIEW_ID) &&
                    isVisible(scenario, PlayerActivity.RETRY_BUTTON_ID)
            }
            assertTrue("initial error must be non-empty", textOf(scenario, PlayerActivity.ERROR_VIEW_ID).isNotBlank())

            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

            await("returned error surface") {
                isVisible(scenario, PlayerActivity.ERROR_VIEW_ID) &&
                    isVisible(scenario, PlayerActivity.RETRY_BUTTON_ID)
            }
            assertTrue("error message must remain non-empty after background return", textOf(scenario, PlayerActivity.ERROR_VIEW_ID).isNotBlank())

            scenario.onActivity { activity ->
                activity.findViewById<View>(PlayerActivity.RETRY_BUTTON_ID).performClick()
            }
            await("retry surface after background return") {
                isVisible(scenario, PlayerActivity.ERROR_VIEW_ID) &&
                    isVisible(scenario, PlayerActivity.RETRY_BUTTON_ID)
            }
        }
    }

    private fun isVisible(
        scenario: ActivityScenario<PlayerActivity>,
        id: Int,
    ): Boolean {
        var visible = false
        scenario.onActivity { visible = it.findViewById<View>(id).visibility == View.VISIBLE }
        return visible
    }

    private fun textOf(
        scenario: ActivityScenario<PlayerActivity>,
        id: Int,
    ): String {
        var text = ""
        scenario.onActivity { text = it.findViewById<android.widget.TextView>(id).text.toString() }
        return text
    }

    private fun await(label: String, timeoutMs: Long = 20_000L, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Timed out waiting for $label")
    }

    private companion object {
        const val INVALID_HLS = "https://127.0.0.1:9/fasel-hd-invalid.m3u8"
    }
}
