package com.faselhd.restored.provider

import com.faselhd.restored.core.PlaybackKind
import com.faselhd.restored.core.PlaybackSource
import com.faselhd.restored.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ProviderGatewayTest {
    private val media = MediaSummary("m1", "Recovered title", MediaType.SERIES)

    private val provider = object : ContentProvider {
        override suspend fun catalog(type: MediaType, page: Int) = Page(listOf(media), page, false)
        override suspend fun search(query: String, page: Int) = Page(listOf(media.copy(title = query)), page, false)
        override suspend fun details(id: String, type: MediaType) = MediaDetails(media)
        override suspend fun sources(mediaId: String, episodeId: String?) = listOf(
            PlaybackSource("https://cdn.example.org/master.m3u8", PlaybackKind.HLS),
            PlaybackSource("http://127.0.0.1/private.m3u8", PlaybackKind.HLS),
            PlaybackSource("javascript:alert(1)", PlaybackKind.UNSUPPORTED)
        )
    }

    @Test fun blankSearchDoesNotHitProvider() = runBlocking {
        assertTrue(ProviderGateway(provider).search("   ").items.isEmpty())
    }

    @Test fun pageIsNeverBelowOne() = runBlocking {
        assertEquals(1, ProviderGateway(provider).catalog(MediaType.SERIES, 0).page)
    }

    @Test fun unsafeSourcesAreRemovedAtGatewayBoundary() = runBlocking {
        val sources = ProviderGateway(provider).sources("m1")
        assertEquals(1, sources.size)
        assertEquals(PlaybackKind.HLS, sources.single().kind)
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyIdIsRejected() = runBlocking {
        ProviderGateway(provider).details(" ", MediaType.SERIES)
    }
}
