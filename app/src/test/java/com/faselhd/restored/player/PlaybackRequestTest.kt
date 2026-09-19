package com.faselhd.restored.player

import com.faselhd.restored.core.PlaybackKind
import com.faselhd.restored.core.PlaybackSource
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackRequestTest {
    @Test fun allowsTemporaryPlaybackHeaders() {
        val request = PlaybackRequest(
            PlaybackSource("https://cdn.example.org/master.m3u8", PlaybackKind.HLS),
            mapOf("user-agent" to "FaselRecovery", "Referer" to "https://example.org/", "Cookie" to "session=temporary")
        )
        assertEquals(setOf("User-Agent", "Referer", "Cookie"), request.sanitizedHeaders().keys)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsafeUri() {
        PlaybackRequest(PlaybackSource("http://127.0.0.1/video.mp4", PlaybackKind.DIRECT))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsResolverSource() {
        PlaybackRequest(PlaybackSource("https://example.org/watch/1", PlaybackKind.WEB_RESOLUTION_REQUIRED))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsArbitraryHeaders() {
        PlaybackRequest(
            PlaybackSource("https://cdn.example.org/video.mp4", PlaybackKind.DIRECT),
            mapOf("Authorization" to "must-not-be-forwarded")
        )
    }
}
