package com.faselhd.restored.playback

import android.content.Intent
import android.os.SystemClock
import androidx.media3.common.Player
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.faselhd.restored.core.PlaybackKind
import com.faselhd.restored.ui.PlayerActivity
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerLifecycleRuntimeTest {
    @Test
    fun pauseResumeRotationBackgroundAndBackReleaseRemainStable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = File(context.cacheDir, "player-lifecycle-owned.wav")
        writeOwnedPcmWav(fixture, seconds = 8)
        val intent = Intent(context, PlayerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(PlayerActivity.EXTRA_URI, fixture.toURI().toString())
            putExtra(PlayerActivity.EXTRA_KIND, PlaybackKind.MP4.name)
        }

        ActivityScenario.launch<PlayerActivity>(intent).use { scenario ->
            await("initial player attachment") { activityPlayer(scenario) != null }
            await("initial READY") { activityPlayer(scenario)?.playbackState == Player.STATE_READY }
            await("initial progress") { (activityPlayer(scenario)?.currentPosition ?: 0L) >= 250L }

            var pausedAt = 0L
            scenario.onActivity { activity ->
                val player = activity.findViewById<androidx.media3.ui.PlayerView>(PlayerActivity.PLAYER_VIEW_ID).player!!
                player.pause()
                pausedAt = player.currentPosition
            }
            SystemClock.sleep(300)
            val afterPause = activityPlayer(scenario)?.currentPosition ?: pausedAt
            assertTrue("pause must stop meaningful advancement", afterPause <= pausedAt + 150L)

            scenario.onActivity { activity ->
                activity.findViewById<androidx.media3.ui.PlayerView>(PlayerActivity.PLAYER_VIEW_ID).player!!.play()
            }
            await("resume progress") { (activityPlayer(scenario)?.currentPosition ?: 0L) >= afterPause + 200L }
            val beforeRotation = activityPlayer(scenario)!!.currentPosition

            scenario.recreate()
            await("player after rotation") { activityPlayer(scenario) != null }
            await("READY after rotation") { activityPlayer(scenario)?.playbackState == Player.STATE_READY }
            val afterRotation = activityPlayer(scenario)!!.currentPosition
            assertTrue("rotation must restore position", afterRotation >= (beforeRotation - 250L).coerceAtLeast(0L))

            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            await("player after background/foreground") { activityPlayer(scenario) != null }
            await("READY after background/foreground") { activityPlayer(scenario)?.playbackState == Player.STATE_READY }

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            await("activity destroyed after back") { scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED }
        }
        fixture.delete()
    }

    private fun activityPlayer(scenario: ActivityScenario<PlayerActivity>): Player? {
        var result: Player? = null
        scenario.onActivity { activity ->
            result = activity.findViewById<androidx.media3.ui.PlayerView>(PlayerActivity.PLAYER_VIEW_ID).player
        }
        return result
    }

    private fun await(label: String, timeoutMs: Long = 10_000L, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(50)
        }
        throw AssertionError("Timed out waiting for $label")
    }

    private fun writeOwnedPcmWav(file: File, seconds: Int) {
        val sampleRate = 8_000
        val samples = sampleRate * seconds
        val dataSize = samples * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII)); putInt(36 + dataSize)
            put("WAVE".toByteArray(Charsets.US_ASCII)); put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16); putShort(1.toShort()); putShort(1.toShort()); putInt(sampleRate)
            putInt(sampleRate * 2); putShort(2.toShort()); putShort(16.toShort())
            put("data".toByteArray(Charsets.US_ASCII)); putInt(dataSize)
        }.array()
        FileOutputStream(file).use { out ->
            out.write(header)
            repeat(samples) { index ->
                val sample = (sin(2.0 * PI * 440.0 * index / sampleRate) * 4_000).toInt().toShort()
                out.write(sample.toInt() and 0xff)
                out.write((sample.toInt() shr 8) and 0xff)
            }
        }
    }
}
