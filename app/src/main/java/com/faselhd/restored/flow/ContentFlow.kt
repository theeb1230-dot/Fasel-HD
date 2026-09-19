package com.faselhd.restored.flow

import com.faselhd.restored.domain.MediaDetails
import com.faselhd.restored.domain.MediaSummary
import com.faselhd.restored.domain.MediaType
import com.faselhd.restored.domain.Page
import com.faselhd.restored.playback.PlaybackDecision
import com.faselhd.restored.playback.PlaybackPipeline
import com.faselhd.restored.provider.ProviderGateway

/**
 * Application-level path from discovery to a validated playback decision.
 * Keeps UI independent from provider implementations and makes the recovered flow testable end-to-end.
 */
class ContentFlow(private val gateway: ProviderGateway) {
    suspend fun catalog(type: MediaType, page: Int = 1): Page<MediaSummary> =
        gateway.catalog(type, page)

    suspend fun search(query: String, page: Int = 1): Page<MediaSummary> =
        gateway.search(query, page)

    suspend fun details(item: MediaSummary): MediaDetails =
        gateway.details(item.id, item.type)

    suspend fun play(media: MediaSummary, episodeId: String? = null): PlaybackDecision {
        val sources = gateway.sources(media.id, episodeId)
        if (sources.isEmpty()) return PlaybackDecision.Rejected("no_safe_sources")

        var resolver: PlaybackDecision.ResolverRequired? = null
        for (source in sources) {
            when (val decision = PlaybackPipeline.prepare(source)) {
                is PlaybackDecision.Native -> return decision
                is PlaybackDecision.ResolverRequired -> if (resolver == null) resolver = decision
                is PlaybackDecision.Rejected -> Unit
            }
        }
        return resolver ?: PlaybackDecision.Rejected("no_playable_sources")
    }
}
