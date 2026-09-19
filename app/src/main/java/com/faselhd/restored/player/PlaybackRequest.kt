package com.faselhd.restored.player

import com.faselhd.restored.core.PlaybackKind
import com.faselhd.restored.core.PlaybackSource
import com.faselhd.restored.network.SafeHttp

/** Ephemeral playback context. Never persist or log headers. */
data class PlaybackRequest(
    val source: PlaybackSource,
    val headers: Map<String, String> = emptyMap()
) {
    init {
        require(SafeHttp.isAllowed(source.uri)) { "Unsafe playback URI" }
        require(source.kind in setOf(PlaybackKind.HLS, PlaybackKind.DASH, PlaybackKind.DIRECT)) {
            "Source requires resolution before native playback"
        }
        require(headers.keys.all { it.lowercase() in ALLOWED_HEADERS }) { "Unsupported playback header" }
        require(headers.values.all { it.length <= MAX_HEADER_VALUE_LENGTH }) { "Playback header too large" }
    }

    fun sanitizedHeaders(): Map<String, String> = headers
        .filterKeys { it.lowercase() in ALLOWED_HEADERS }
        .mapKeys { (key, _) -> when (key.lowercase()) {
            "user-agent" -> "User-Agent"
            "referer" -> "Referer"
            "cookie" -> "Cookie"
            else -> key
        } }

    companion object {
        private val ALLOWED_HEADERS = setOf("user-agent", "referer", "cookie")
        private const val MAX_HEADER_VALUE_LENGTH = 8192
    }
}
