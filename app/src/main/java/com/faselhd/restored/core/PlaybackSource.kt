package com.faselhd.restored.core

enum class PlaybackKind { HLS, DASH, DIRECT, WEB_RESOLUTION_REQUIRED, UNSUPPORTED }
data class PlaybackSource(val uri: String, val kind: PlaybackKind)

object PlaybackClassifier {
    fun classify(raw: String): PlaybackSource {
        val uri = raw.trim()
        if (uri.isEmpty()) return PlaybackSource(uri, PlaybackKind.UNSUPPORTED)
        val lower = uri.lowercase()
        val kind = when {
            !lower.startsWith("https://") && !lower.startsWith("http://") -> PlaybackKind.UNSUPPORTED
            lower.substringBefore('?').endsWith(".m3u8") -> PlaybackKind.HLS
            lower.substringBefore('?').endsWith(".mpd") -> PlaybackKind.DASH
            lower.substringBefore('?').matches(Regex(".*\\.(mp4|mkv|webm|m4v)$")) -> PlaybackKind.DIRECT
            else -> PlaybackKind.WEB_RESOLUTION_REQUIRED
        }
        return PlaybackSource(uri, kind)
    }
}
