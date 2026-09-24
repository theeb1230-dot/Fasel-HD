package com.faselhd.restored.provider

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.faselhd.restored.core.PlaybackClassifier
import com.faselhd.restored.domain.MediaDetails
import com.faselhd.restored.domain.MediaSummary
import com.faselhd.restored.domain.MediaType
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProviderNativeSourceRuntimeGuardTest {
    @Test
    fun providerSourcesExposeOnlyNativePlaybackCandidates() = runBlocking {
        val json = """{"current_page":1,"next_page_url":null,"data":[{"id":"guarded-item","title":"Guarded Item"}]}"""
        val provider = ConfiguredContentProvider(
            pageLoader = ProviderPageLoader(ProviderTransport(client = OkHttpClient.Builder().addInterceptor { chain ->
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                    .message("owned fixture").body(json.toResponseBody()).build()
            }.build())),
            catalogUrl = { type, page -> "https://example.org/catalog/${type.name.lowercase()}?page=$page" },
            searchUrl = { query, type, page -> "https://example.org/search/${type.name.lowercase()}?q=$query&page=$page" },
            detailsLoader = { id, type -> MediaDetails(MediaSummary(id, "Guarded Item", type)) },
            sourcesLoader = { _, _ ->
                listOf(
                    PlaybackClassifier.classify("javascript:alert(1)"),
                    PlaybackClassifier.classify("https://example.org/native.m3u8")
                )
            }
        )

        val item = provider.search("guarded", MediaType.MOVIE, 1).items.single()
        val sources = ProviderGateway(provider).sources(item.id, null)

        assertEquals("only one source may survive provider filtering", 1, sources.size)
        assertTrue("the surviving source must be native", sources.single().uri.endsWith("native.m3u8"))
    }

    @Test
    fun providerSourcesReturnEmptyWhenAllCandidatesAreUnsafe() = runBlocking {
        val json = """{"current_page":1,"next_page_url":null,"data":[{"id":"unsafe-only","title":"Unsafe Only"}]}"""
        val provider = ConfiguredContentProvider(
            pageLoader = ProviderPageLoader(ProviderTransport(client = OkHttpClient.Builder().addInterceptor { chain ->
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                    .message("owned fixture").body(json.toResponseBody()).build()
            }.build())),
            catalogUrl = { type, page -> "https://example.org/catalog/${type.name.lowercase()}?page=$page" },
            searchUrl = { query, type, page -> "https://example.org/search/${type.name.lowercase()}?q=$query&page=$page" },
            detailsLoader = { id, type -> MediaDetails(MediaSummary(id, "Unsafe Only", type)) },
            sourcesLoader = { _, _ ->
                listOf(
                    PlaybackClassifier.classify("javascript:alert(1)"),
                    PlaybackClassifier.classify("file:///sdcard/unsafe.mp4"),
                    PlaybackClassifier.classify("http://127.0.0.1:9/loopback.m3u8")
                )
            }
        )

        val item = provider.search("unsafe", MediaType.MOVIE, 1).items.single()
        val sources = ProviderGateway(provider).sources(item.id, null)

        assertTrue("unsafe-only candidates must not reach playback", sources.isEmpty())
    }
}
