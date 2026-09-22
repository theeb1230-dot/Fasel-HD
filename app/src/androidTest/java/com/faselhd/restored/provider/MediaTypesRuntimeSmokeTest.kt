package com.faselhd.restored.provider

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.faselhd.restored.core.PlaybackClassifier
import com.faselhd.restored.domain.MediaDetails
import com.faselhd.restored.domain.MediaSummary
import com.faselhd.restored.domain.MediaType
import com.faselhd.restored.playback.PlaybackDecision
import com.faselhd.restored.playback.PlaybackPipeline
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
class MediaTypesRuntimeSmokeTest {
    @Test
    fun movieSeriesAnimeAndStreamKeepTheirTypeThroughProviderAndNativeDecision() = runBlocking {
        MediaType.entries.forEach { type ->
            val slug = type.name.lowercase()
            val json = """{"current_page":1,"next_page_url":null,"data":[{"id":"runtime-$slug","title":"Runtime $slug"}]}"""
            val provider = ConfiguredContentProvider(
                pageLoader = ProviderPageLoader(ProviderTransport(client = OkHttpClient.Builder().addInterceptor { chain ->
                    Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                        .message("owned fixture").body(json.toResponseBody()).build()
                }.build())),
                catalogUrl = { requestedType, page -> "https://example.org/catalog/${requestedType.name.lowercase()}?page=$page" },
                searchUrl = { query, requestedType, page -> "https://example.org/search/${requestedType.name.lowercase()}?q=$query&page=$page" },
                detailsLoader = { id, requestedType -> MediaDetails(MediaSummary(id, "Runtime $slug", requestedType)) },
                sourcesLoader = { _, _ -> listOf(PlaybackClassifier.classify("https://media.example.org/$slug.m3u8")) }
            )

            val item = provider.search("runtime", type, 1).items.single()
            assertEquals("search must preserve $type", type, item.type)
            assertEquals("details must preserve $type", type, provider.details(item.id, item.type).media.type)

            val sources = ProviderGateway(provider).sources(item.id, if (type == MediaType.SERIES || type == MediaType.ANIME) "e1" else null)
            assertEquals("safe source must survive for $type", 1, sources.size)
            assertTrue("$type must select internal native playback", PlaybackPipeline.prepare(sources.single()) is PlaybackDecision.Native)
        }
    }
}
