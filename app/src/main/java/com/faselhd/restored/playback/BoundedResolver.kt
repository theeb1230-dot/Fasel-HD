package com.faselhd.restored.playback

import com.faselhd.restored.core.PlaybackClassifier
import com.faselhd.restored.network.SafeHttp

sealed interface ResolverResult {
    data class Resolved(val request: NativePlaybackRequest) : ResolverResult
    data class Rejected(val reason: String) : ResolverResult
}

/**
 * Deliberately bounded resolver boundary.
 *
 * It does not execute JavaScript, scrape arbitrary pages, follow redirects, persist cookies,
 * or bypass DRM/CAPTCHA/paywalls/access controls. An authorized provider may supply candidate
 * media URLs it already obtained legitimately; this boundary accepts only public HTTPS native
 * HLS/DASH/MP4 candidates and returns the first safe playable request.
 */
object BoundedResolver {
    private const val MAX_CANDIDATES = 32

    fun resolve(candidates: Iterable<String>): ResolverResult {
        val seen = LinkedHashSet<String>(MAX_CANDIDATES)
        var inspected = 0

        for (candidate in candidates) {
            if (inspected++ >= MAX_CANDIDATES) break
            val normalized = SafeHttp.normalize(candidate) ?: continue
            if (!seen.add(normalized)) continue
            if (!normalized.startsWith("https://", ignoreCase = true)) continue
            when (val decision = PlaybackPipeline.prepare(PlaybackClassifier.classify(normalized))) {
                is PlaybackDecision.Native -> return ResolverResult.Resolved(decision.request)
                is PlaybackDecision.ResolverRequired, is PlaybackDecision.Rejected -> Unit
            }
        }
        return ResolverResult.Rejected("no_safe_native_candidate")
    }
}
