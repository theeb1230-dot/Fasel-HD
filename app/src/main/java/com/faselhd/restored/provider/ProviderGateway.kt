package com.faselhd.restored.provider

import com.faselhd.restored.core.PlaybackSource
import com.faselhd.restored.domain.MediaDetails
import com.faselhd.restored.domain.MediaSummary
import com.faselhd.restored.domain.MediaType
import com.faselhd.restored.domain.Page
import com.faselhd.restored.network.SafeHttp

class ProviderGateway(private val provider: ContentProvider) {
    suspend fun catalog(type: MediaType, page: Int = 1): Page<MediaSummary> =
        provider.catalog(type, page.coerceAtLeast(1))

    suspend fun search(query: String, page: Int = 1): Page<MediaSummary> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return Page(emptyList(), page.coerceAtLeast(1), false)
        return provider.search(normalized, page.coerceAtLeast(1))
    }

    suspend fun details(id: String, type: MediaType): MediaDetails =
        provider.details(id.trim().requireNotEmpty(), type)

    suspend fun sources(mediaId: String, episodeId: String? = null): List<PlaybackSource> =
        provider.sources(mediaId.trim().requireNotEmpty(), episodeId?.trim())
            .filter { SafeHttp.isAllowed(it.uri) }

    private fun String.requireNotEmpty(): String {
        require(isNotEmpty()) { "id must not be empty" }
        return this
    }
}
