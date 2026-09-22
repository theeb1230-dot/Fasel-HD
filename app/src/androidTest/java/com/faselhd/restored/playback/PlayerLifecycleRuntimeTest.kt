package com.faselhd.restored.playback

import android.content.Intent
import android.os.SystemClock
import androidx.media3.common.Player
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.faselhd.restored.core.PlaybackKind
import com.faselhd.restored.ui.PlayerActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerLifecycleRuntimeTest {
    @Test
    fun pauseResumeRotationBackgroundAndBackReleaseRemainStable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, PlayerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Public Shaka demo media is already used by the provider runtime smoke. Using HTTPS here
            // keeps this test on the exact production PlaybackRequest/SafeHttp path instead of adding
            // a test-only file:// bypass that would weaken the boundary we are trying to prove.
            putExtra(PlayerActivity.EXTRA_URI, SHAKA_DEMO_HLS)
            putExtra(PlayerActivity.EXTRA_KIND, PlaybackKind.HLS.name)
        }

        ActivityScenario.launch<PlayerActivity>(intent).use { scenario ->
            await("initial player attachment") { playerSnapshot(scenario).attached }
            await("initial READY", 20_000L) { playerSnapshot(scenario).playbackState == Player.STATE_READY }
            await("initial progress") { playerSnapshot(scenario).currentPosition >= 250L }

            var pausedAt = 0L
            scenario.onActivity { activity ->
                val player = activity.findViewById<androidx.media3.ui.PlayerView>(PlayerActivity.PLAYER_VIEW_ID).player!!
                player.pause()
                pausedAt = player.currentPosition
            }
            SystemClock.sleep(300)
            val afterPause = playerSnapshot(scenario).currentPosition
            assertTrue("pause must stop meaningful advancement", afterPause <= pausedAt + 150L)

            scenario.onActivity { activity ->
                activity.findViewById<androidx.media3.ui.PlayerView>(PlayerActivity.PLAYER_VIEW_ID).player!!.play()
            }
            await("resume progress") { playerSnapshot(scenario).currentPosition >= afterPause + 200L }
            val beforeRotation = playerSnapshot(scenario).currentPosition

            scenario.recreate()
            await("player after rotation") { playerSnapshot(scenario).attached }
            await("READY after rotation", 20_000L) { playerSnapshot(scenario).playbackState == Player.STATE_READY }
            val afterRotation = playerSnapshot(scenario).currentPosition
            assertTrue("rotation must restore position", afterRotation >= (beforeRotation - 250L).coerceAtLeast(0L))

            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            await("player after background/foreground") { playerSnapshot(scenario).attached }
            await("READY after background/foreground", 20_000L) { playerSnapshot(scenario).playbackState == Player.STATE_READY }

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            await("activity destroyed after back") { scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED }
        }
    }

    /** Media3 requires every player read to happen on its application looper (main for PlayerActivity). */
    private fun playerSnapshot(scenario: ActivityScenario<PlayerActivity>): PlayerSnapshot {
        var snapshot = PlayerSnapshot()
        scenario.onActivity { activity ->
            val player = activity.findViewById<androidx.media3.ui.PlayerView>(PlayerActivity.PLAYER_VIEW_ID).player
            snapshot = if (player == null) {
                PlayerSnapshot()
            } else {
                PlayerSnapshot(
                    attached = true,
                    playbackState = player.playbackState,
                    currentPosition = player.currentPosition,
                )
            }
        }
        return snapshot
    }

    private fun await(label: String, timeoutMs: Long = 10_000L, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(50)
        }
        throw AssertionError("Timed out waiting for $label")
    }

    private data class PlayerSnapshot(
        val attached: Boolean = false,
        val playbackState: Int = Player.STATE_IDLE,
        val currentPosition: Long = 0L,
    )

    private companion object {
        const val SHAKA_DEMO_HLS = "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8"
    }
}
