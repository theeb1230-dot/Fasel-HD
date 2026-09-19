package com.faselhd.restored.flow

import com.faselhd.restored.core.PlaybackKind
import com.faselhd.restored.playback.NativePlaybackRequest
import com.faselhd.restored.playback.PlaybackDecision
import org.junit.Assert.*
import org.junit.Test

class PlaybackNavigatorTest {
    @Test fun nativeDecisionCreatesPlayerLaunchPlan() {
        val launch = PlaybackNavigator.plan(
            PlaybackDecision.Native(NativePlaybackRequest("https://cdn.example.org/master.m3u8", PlaybackKind.HLS))
        )
        assertNotNull(launch)
        assertEquals("https://cdn.example.org/master.m3u8", launch?.uri)
        assertEquals(PlaybackKind.HLS.name, launch?.kind)
    }

    @Test fun resolverDecisionNeverCreatesPlayerLaunch() {
        assertNull(PlaybackNavigator.plan(PlaybackDecision.ResolverRequired("https://example.org/watch/1")))
    }

    @Test fun rejectedDecisionNeverCreatesPlayerLaunch() {
        assertNull(PlaybackNavigator.plan(PlaybackDecision.Rejected("unsafe_uri")))
    }
}
