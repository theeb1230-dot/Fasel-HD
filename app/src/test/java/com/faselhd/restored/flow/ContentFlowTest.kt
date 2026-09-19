package com.faselhd.restored.flow

import com.faselhd.restored.core.PlaybackClassifier
import com.faselhd.restored.core.PlaybackSource
import com.faselhd.restored.domain.*
import com.faselhd.restored.playback.PlaybackDecision
import com.faselhd.restored.provider.ContentProvider
import com.faselhd.restored.provider.ProviderGateway
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ContentFlowTest {
    private val series = MediaSummary("series-1", "Recovered Series", MediaType.SERIES)
    private val episode = Episode("episode-1", 1, 1, "Episode 1")

    private fun flow(sources: List<PlaybackSource>) = ContentFlow(ProviderGateway(object : ContentProvider {
        override suspend fun catalog(type: MediaType, page: Int) = Page(listOf(series.copy(type = type)), page, false)
        override suspend fun search(query: String, page: Int) = Page(listOf(series.copy(title = query)), page, false)
        override suspend fun details(id: String, type: MediaType) =
            MediaDetails(series.copy(id = id, type = type), seasons = listOf(1), episodes = listOf(episode))
        override suspend fun sources(mediaId: String, episodeId: String?) = sources
    }))

    @Test fun catalogDetailsEpisodeToNativePlaybackIsConnected() = runBlocking {
        val flow = flow(listOf(PlaybackClassifier.classify("https://cdn.example.org/master.m3u8")))
        val item = flow.catalog(MediaType.SERIES).items.single()
        val details = flow.details(item)
        assertEquals(listOf(1), details.seasons)
        assertEquals("episode-1", details.episodes.single().id)
        assertTrue(flow.play(item, details.episodes.single().id) is PlaybackDecision.Native)
    }

    @Test fun searchToDirectPlaybackIsConnected() = runBlocking {
        val flow = flow(listOf(PlaybackClassifier.classify("https://cdn.example.org/movie.mp4")))
        val item = flow.search("Recovered Movie").items.single()
        assertEquals("Recovered Movie", item.title)
        assertTrue(flow.play(item) is PlaybackDecision.Native)
    }

    @Test fun resolverPageIsExplicitInsteadOfPretendingNativePlayback() = runBlocking {
        val flow = flow(listOf(PlaybackClassifier.classify("https://example.org/watch/1")))
        assertTrue(flow.play(series) is PlaybackDecision.ResolverRequired)
    }

    @Test fun noSafeSourcesFailsClosed() = runBlocking {
        val flow = flow(listOf(PlaybackClassifier.classify("http://127.0.0.1/private.m3u8")))
        val decision = flow.play(series)
        assertTrue(decision is PlaybackDecision.Rejected)
        assertEquals("no_safe_sources", (decision as PlaybackDecision.Rejected).reason)
    }

    @Test fun nativeSourceWinsOverResolverFallback() = runBlocking {
        val flow = flow(listOf(
            PlaybackClassifier.classify("https://example.org/watch/1"),
            PlaybackClassifier.classify("https://cdn.example.org/master.m3u8")
        ))
        assertTrue(flow.play(series) is PlaybackDecision.Native)
    }
}
