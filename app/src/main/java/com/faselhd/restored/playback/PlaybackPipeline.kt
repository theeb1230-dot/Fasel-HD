package com.faselhd.restored.playback

import com.faselhd.restored.core.PlaybackKind
import com.faselhd.restored.core.PlaybackSource
import com.faselhd.restored.network.SafeHttp

data class NativePlaybackRequest(val uri: String, val kind: PlaybackKind)

sealed interface PlaybackDecision {
    data class Native(val request: NativePlaybackRequest) : PlaybackDecision
    data class ResolverRequired(val uri: String) : PlaybackDecision
    data class Rejected(val reason: String) : PlaybackDecision
}

object PlaybackPipeline {
    fun prepare(source: PlaybackSource): PlaybackDecision {
        if (!SafeHttp.isAllowed(source.uri)) return PlaybackDecision.Rejected("unsafe_uri")
        return when (source.kind) {
            PlaybackKind.HLS, PlaybackKind.DASH, PlaybackKind.DIRECT ->
                PlaybackDecision.Native(NativePlaybackRequest(source.uri, source.kind))
            PlaybackKind.WEB_RESOLUTION_REQUIRED -> PlaybackDecision.ResolverRequired(source.uri)
            PlaybackKind.UNSUPPORTED -> PlaybackDecision.Rejected("unsupported_source")
        }
    }
}
