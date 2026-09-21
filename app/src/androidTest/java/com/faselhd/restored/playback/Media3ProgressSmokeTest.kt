package com.faselhd.restored.playback

import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class Media3ProgressSmokeTest {
    @Test
    fun deterministicOwnedAudioFixtureReachesReadyAndAdvances() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = File(context.cacheDir, "media3-owned-progress-probe.wav")
        writeOwnedPcmWav(fixture)
        val player = ExoPlayer.Builder(context).build()
        try {
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(fixture)))
            player.prepare()
            player.playWhenReady = true

            await("Media3 READY", 10_000) {
                player.playerError == null && player.playbackState == Player.STATE_READY
            }
            val start = player.currentPosition
            await("advancing playback position", 5_000) {
                player.playerError == null && player.currentPosition >= start + 250
            }
            assertTrue("owned fixture must have a positive duration", player.duration > 0)
            assertTrue("playback must advance beyond 250 ms", player.currentPosition >= start + 250)
        } finally {
            player.release()
            fixture.delete()
        }
    }

    private fun await(label: String, timeoutMs: Long, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(50)
        }
        throw AssertionError("Timed out waiting for $label")
    }

    /** Generates our own 2-second mono PCM WAV fixture; no network/provider/copyright dependency. */
    private fun writeOwnedPcmWav(file: File) {
        val sampleRate = 8_000
        val seconds = 2
        val samples = sampleRate * seconds
        val dataSize = samples * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1.toShort())
            putShort(1.toShort())
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2.toShort())
            putShort(16.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
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
