package com.faselhd.restored.provider

import com.faselhd.restored.domain.MediaDetails
import com.faselhd.restored.domain.MediaSummary
import com.faselhd.restored.domain.MediaType
import com.faselhd.restored.core.PlaybackClassifier
import com.faselhd.restored.core.PlaybackKind
import com.faselhd.restored.core.PlaybackSource
import com.faselhd.restored.playback.PlaybackDecision
import com.faselhd.restored.playback.PlaybackDecisionEngine
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class ConfiguredContentProviderTest {
    private fun provider(body: String, code: Int = 200): ConfiguredContentProvider {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("fixture")
                .body(body.toResponseBody())
                .build()
        }.build()
        return ConfiguredContentProvider(
            pageLoader = ProviderPageLoader(ProviderTransport(client = client)),
            catalogUrl = { type, page -> "https://example.org/catalog/${type.name.lowercase()}?page=$page" },
            searchUrl = { query, type, page -> "https://example.org/search/${type.name.lowercase()}?q=$query&page=$page" },
            detailsLoader = { id, type -> MediaDetails(MediaSummary(id, "fixture", type)) },
            sourcesLoader = { _, _ -> emptyList() }
        )
    }

    @Test fun catalogFlowsThroughTransportDecodeAndMapping() = runBlocking {
        val json = """{"current_page":2,"next_page_url":"https://example.org/page/3","data":[{"id":"m1","title":"Movie One"}]}"""
        val page = provider(json).catalog(MediaType.MOVIE, 2)
        assertEquals(2, page.page)
        assertTrue(page.hasNext)
        assertEquals("m1", page.items.single().id)
        assertEquals(MediaType.MOVIE, page.items.single().type)
    }

    @Test fun typedSearchPreservesSeriesType() = runBlocking {
        val json = """{"current_page":1,"next_page_url":null,"data":[{"id":"s1","title":"Series One"}]}"""
        val page = provider(json).search("series", MediaType.SERIES, 1)
        assertEquals(MediaType.SERIES, page.items.single().type)
        assertEquals("s1", page.items.single().id)
    }

    @Test fun typedSearchPreservesAnimeType() = runBlocking {
        val json = """{"current_page":1,"next_page_url":null,"data":[{"id":"a1","title":"Anime One"}]}"""
        val page = provider(json).search("anime", MediaType.ANIME, 1)
        assertEquals(MediaType.ANIME, page.items.single().type)
    }

    @Test fun legacySearchRemainsMovieDefault() = runBlocking {
        val json = """{"current_page":1,"next_page_url":null,"data":[{"id":"m1","title":"Movie One"}]}"""
        val page = provider(json).search("movie", 1)
        assertEquals(MediaType.MOVIE, page.items.single().type)
    }

    @Test fun authorizedFixtureFlowsFromTypedSearchThroughSourcesToNativeDecision() = runBlocking {
        val json = """{"current_page":1,"next_page_url":null,"data":[{"id":"s42","title":"Authorized Series"}]}"""
        val configured = ConfiguredContentProvider(
            pageLoader = ProviderPageLoader(ProviderTransport(client = OkHttpClient.Builder().addInterceptor { chain ->
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                    .message("fixture").body(json.toResponseBody()).build()
            }.build())),
            catalogUrl = { type, page -> "https://example.org/catalog/${type.name.lowercase()}?page=$page" },
            searchUrl = { query, type, page -> "https://example.org/search/${type.name.lowercase()}?q=$query&page=$page" },
            detailsLoader = { id, type -> MediaDetails(MediaSummary(id, "Authorized Series", type)) },
            sourcesLoader = { mediaId, episodeId ->
                assertEquals("s42", mediaId)
                assertEquals("e1", episodeId)
                listOf(
                    PlaybackClassifier.classify("https://media.example.org/owned-fixture.m3u8"),
                    PlaybackSource("javascript:alert(1)", PlaybackKind.UNSUPPORTED)
                )
            }
        )
        val search = configured.search("authorized", MediaType.SERIES, 1)
        val item = search.items.single()
        assertEquals(MediaType.SERIES, item.type)
        val details = configured.details(item.id, item.type)
        assertEquals(MediaType.SERIES, details.media.type)
        val safeSources = ProviderGateway(configured).sources(item.id, "e1")
        assertEquals(1, safeSources.size)
        assertEquals(PlaybackKind.HLS, safeSources.single().kind)
        assertTrue(PlaybackDecisionEngine.decide(safeSources.single()) is PlaybackDecision.Native)
    }

    @Test fun invalidPayloadIsNonRetryable() = runBlocking {
        try {
            provider("not-json").catalog(MediaType.MOVIE, 1)
            fail("expected ProviderLoadException")
        } catch (error: ProviderLoadException) {
            assertFalse(error.retryable)
        }
    }

    @Test fun serverFailureIsRetryable() = runBlocking {
        try {
            provider("{}", 503).catalog(MediaType.MOVIE, 1)
            fail("expected ProviderLoadException")
        } catch (error: ProviderLoadException) {
            assertTrue(error.retryable)
            assertEquals("http_503", error.providerReason)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankSearchIsRejectedBeforeTransport() {
        runBlocking {
            provider("{}").search("   ", 1)
        }
    }
}
