package com.faselhd.restored.playback

import android.content.Intent
import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.faselhd.restored.core.PlaybackKind
import com.faselhd.restored.ui.PlayerActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerErrorRetryRuntimeTest {
    @Test
    fun playbackErrorShowsRetryAndRetryKeepsErrorSurfaceAvailable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, PlayerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(PlayerActivity.EXTRA_URI, INVALID_HLS)
            putExtra(PlayerActivity.EXTRA_KIND, PlaybackKind.HLS.name)
        }

        ActivityScenario.launch<PlayerActivity>(intent).use { scenario ->
            await("error surface") {
                viewVisibility(scenario, PlayerActivity.ERROR_VIEW_ID) == View.VISIBLE &&
                    viewVisibility(scenario, PlayerActivity.RETRY_BUTTON_ID) == View.VISIBLE
            }

            val initialErrorText = viewText(scenario, PlayerActivity.ERROR_VIEW_ID)
            assertTrue("error message should be non-empty", initialErrorText.isNotBlank())

            scenario.onActivity { activity ->
                activity.findViewById<View>(PlayerActivity.RETRY_BUTTON_ID).performClick()
            }

            await("retry keeps error surface available") {
                viewVisibility(scenario, PlayerActivity.ERROR_VIEW_ID) == View.VISIBLE &&
                    viewVisibility(scenario, PlayerActivity.RETRY_BUTTON_ID) == View.VISIBLE
            }
            assertEquals(initialErrorText, viewText(scenario, PlayerActivity.ERROR_VIEW_ID))
        }
    }

    private fun viewVisibility(scenario: ActivityScenario<PlayerActivity>, id: Int): Int {
        var visibility = View.GONE
        scenario.onActivity { visibility = it.findViewById<View>(id).visibility }
        return visibility
    }

    private fun viewText(scenario: ActivityScenario<PlayerActivity>, id: Int): String {
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
