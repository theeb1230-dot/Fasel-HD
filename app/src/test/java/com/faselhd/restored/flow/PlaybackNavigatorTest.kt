package com.faselhd.restored.flow

import android.content.Context
import com.faselhd.restored.core.PlaybackKind
import com.faselhd.restored.playback.NativePlaybackRequest
import com.faselhd.restored.playback.PlaybackDecision
import com.faselhd.restored.ui.PlayerActivity
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock

class PlaybackNavigatorTest {
    private val context: Context = mock(Context::class.java)

    @Test fun nativeDecisionCreatesInternalPlayerIntent() {
        val intent = PlaybackNavigator.intent(
            context,
            PlaybackDecision.Native(NativePlaybackRequest("https://cdn.example.org/master.m3u8", PlaybackKind.HLS))
        )
        assertNotNull(intent)
        assertEquals("https://cdn.example.org/master.m3u8", intent?.getStringExtra(PlayerActivity.EXTRA_URI))
        assertEquals(PlaybackKind.HLS.name, intent?.getStringExtra(PlayerActivity.EXTRA_KIND))
    }

    @Test fun resolverDecisionNeverLaunchesPlayer() {
        assertNull(PlaybackNavigator.intent(context, PlaybackDecision.ResolverRequired("https://example.org/watch/1")))
    }

    @Test fun rejectedDecisionNeverLaunchesPlayer() {
        assertNull(PlaybackNavigator.intent(context, PlaybackDecision.Rejected("unsafe_uri")))
    }
}
