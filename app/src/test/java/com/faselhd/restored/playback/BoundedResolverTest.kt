package com.faselhd.restored.playback

import com.faselhd.restored.core.PlaybackKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundedResolverTest {
    @Test fun choosesFirstSafeNativeHttpsCandidate() {
        val result = BoundedResolver.resolve(
            listOf(
                "javascript:alert(1)",
                "https://example.org/watch/1",
                "https://cdn.example.org/video.m3u8",
                "https://cdn.example.org/video.mp4"
            )
        )
        assertTrue(result is ResolverResult.Resolved)
        val request = (result as ResolverResult.Resolved).request
        assertEquals("https://cdn.example.org/video.m3u8", request.uri)
        assertEquals(PlaybackKind.HLS, request.kind)
    }

    @Test fun rejectsHttpEvenWhenMediaLooksNative() {
        val result = BoundedResolver.resolve(listOf("http://cdn.example.org/video.mp4"))
        assertTrue(result is ResolverResult.Rejected)
    }

    @Test fun rejectsPrivateAndNonMediaCandidates() {
        val result = BoundedResolver.resolve(
            listOf(
                "https://127.0.0.1/video.m3u8",
                "https://localhost/video.mp4",
                "https://example.org/watch/1"
            )
        )
        assertTrue(result is ResolverResult.Rejected)
    }

    @Test fun emptyCandidatesFailClosed() {
        assertTrue(BoundedResolver.resolve(emptyList()) is ResolverResult.Rejected)
    }

    @Test fun capsInspectionAndDeduplicatesCandidates() {
        val candidates = buildList {
            repeat(40) { add("https://example.org/watch/$it") }
            repeat(40) { add("https://example.org/watch/0") }
            add("https://cdn.example.org/video.mp4")
        }

        assertTrue(BoundedResolver.resolve(candidates) is ResolverResult.Rejected)
    }
}
