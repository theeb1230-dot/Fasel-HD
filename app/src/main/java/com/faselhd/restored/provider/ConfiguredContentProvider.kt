package com.faselhd.restored.provider

import com.faselhd.restored.core.PlaybackSource
import com.faselhd.restored.domain.MediaDetails
import com.faselhd.restored.domain.MediaSummary
import com.faselhd.restored.domain.MediaType
import com.faselhd.restored.domain.Page

/**
 * Credential-free provider adapter for catalog/search pages supplied by an authorized configuration.
 * Endpoint construction stays outside this class so no recovered host/token/cookie is embedded.
 */
class ConfiguredContentProvider(
    private val pageLoader: ProviderPageLoader,
    private val catalogUrl: (MediaType, Int) -> String,
    private val searchUrl: (String, Int) -> String,
    private val detailsLoader: suspend (String, MediaType) -> MediaDetails,
    private val sourcesLoader: suspend (String, String?) -> List<PlaybackSource>,
    private val retryPolicy: ProviderRetryPolicy = ProviderRetryPolicy()
) : ContentProvider {
    override suspend fun catalog(type: MediaType, page: Int): Page<MediaSummary> =
        requirePage(retryPolicy.execute { pageLoader.mediaPage(catalogUrl(type, page), type) })

    override suspend fun search(query: String, page: Int): Page<MediaSummary> {
        require(query.isNotBlank()) { "query must not be blank" }
        return requirePage(retryPolicy.execute { pageLoader.mediaPage(searchUrl(query, page), MediaType.MOVIE) })
    }

    override suspend fun details(id: String, type: MediaType): MediaDetails = detailsLoader(id, type)

    override suspend fun sources(mediaId: String, episodeId: String?): List<PlaybackSource> =
        sourcesLoader(mediaId, episodeId)

    private fun <T> requirePage(result: ProviderLoadResult<Page<T>>): Page<T> = when (result) {
        is ProviderLoadResult.Success -> result.value
        is ProviderLoadResult.Failure -> throw ProviderLoadException(result.reason, result.retryable)
    }
}

class ProviderLoadException(
    val providerReason: String,
    val retryable: Boolean
) : IllegalStateException(providerReason)
