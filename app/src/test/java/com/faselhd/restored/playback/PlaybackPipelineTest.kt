package com.faselhd.restored.playback

import com.faselhd.restored.core.PlaybackClassifier
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackPipelineTest {
    @Test fun hlsUsesNativePlayback() {
        assertTrue(PlaybackPipeline.prepare(PlaybackClassifier.classify("https://cdn.example.org/a.m3u8")) is PlaybackDecision.Native)
    }

    @Test fun dashUsesNativePlayback() {
        assertTrue(PlaybackPipeline.prepare(PlaybackClassifier.classify("https://cdn.example.org/a.mpd")) is PlaybackDecision.Native)
    }

    @Test fun directMp4UsesNativePlayback() {
        assertTrue(PlaybackPipeline.prepare(PlaybackClassifier.classify("https://cdn.example.org/a.mp4")) is PlaybackDecision.Native)
    }

    @Test fun ordinaryHttpsPageRequiresResolver() {
        assertTrue(PlaybackPipeline.prepare(PlaybackClassifier.classify("https://example.org/watch/1")) is PlaybackDecision.ResolverRequired)
    }

    @Test fun privateAndScriptUrisAreRejected() {
        assertTrue(PlaybackPipeline.prepare(PlaybackClassifier.classify("http://127.0.0.1/a.m3u8")) is PlaybackDecision.Rejected)
        assertTrue(PlaybackPipeline.prepare(PlaybackClassifier.classify("javascript:alert(1)")) is PlaybackDecision.Rejected)
    }
}
