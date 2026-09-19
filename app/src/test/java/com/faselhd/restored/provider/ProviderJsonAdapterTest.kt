package com.faselhd.restored.provider

import org.junit.Assert.*
import org.junit.Test

class ProviderJsonAdapterTest {
    @Test fun decodesObservedMediaPaginationFields() {
        val result = ProviderJsonAdapter.mediaPage(
            """{"current_page":2,"last_page":3,"next_page_url":"https://example.org/p/3","data":[{"id":"m1","title":"Movie","poster_path":"https://example.org/p.jpg","tmdb_id":"42"}]}"""
        )
        assertTrue(result is ProviderDecodeResult.Success)
        val page = (result as ProviderDecodeResult.Success).value
        assertEquals(2, page.current_page)
        assertEquals(3, page.last_page)
        assertEquals("m1", page.data?.single()?.id)
        assertEquals("Movie", page.data?.single()?.title)
    }

    @Test fun decodesEpisodeFields() {
        val result = ProviderJsonAdapter.episodes(
            """{"episodes":[{"id":"e1","season_number":1,"episode_number":4,"title":"Episode 4"}]}"""
        )
        assertTrue(result is ProviderDecodeResult.Success)
        val episodes = (result as ProviderDecodeResult.Success).value
        assertEquals("e1", episodes.single().id)
        assertEquals(1, episodes.single().season_number)
        assertEquals(4, episodes.single().episode_number)
    }

    @Test fun malformedJsonFailsClosed() {
        assertEquals(ProviderDecodeResult.Invalid("invalid_json"), ProviderJsonAdapter.mediaPage("not-json"))
    }

    @Test fun oversizedPayloadFailsClosedBeforeParsing() {
        val result = ProviderJsonAdapter.mediaPage("x".repeat(2_000_001))
        assertEquals(ProviderDecodeResult.Invalid("body_too_large"), result)
    }
}
