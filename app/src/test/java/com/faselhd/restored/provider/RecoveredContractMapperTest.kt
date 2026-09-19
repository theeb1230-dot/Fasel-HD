package com.faselhd.restored.provider

import com.faselhd.restored.domain.MediaType
import org.junit.Assert.*
import org.junit.Test

class RecoveredContractMapperTest {
    @Test fun fallsBackToTmdbIdAndName() {
        val media = RecoveredContractMapper.media(
            RecoveredMediaDto(name = "Series", tmdb_id = " 42 "),
            MediaType.SERIES
        )
        assertEquals("42", media?.id)
        assertEquals("Series", media?.title)
    }

    @Test fun rejectsIncompleteEpisode() {
        assertNull(RecoveredContractMapper.episode(RecoveredEpisodeDto(id = "e1", season_number = 1)))
    }

    @Test fun dropsUnsafeArtworkUrls() {
        val media = RecoveredContractMapper.media(
            RecoveredMediaDto(id = "m1", title = "Movie", poster_path = "http://127.0.0.1/a.jpg"),
            MediaType.MOVIE
        )
        assertNull(media?.posterUrl)
    }

    @Test fun paginationUsesLastPageWithoutTrustingExternalNextUrl() {
        val page = RecoveredContractMapper.page(
            RecoveredPageDto(
                data = listOf(RecoveredMediaDto(id = "a", title = "A")),
                current_page = 2,
                last_page = 3,
                next_page_url = "javascript:alert(1)"
            ),
            MediaType.ANIME
        )
        assertEquals(2, page.page)
        assertTrue(page.hasNext)
        assertEquals(1, page.items.size)
    }

    @Test fun unsafeNextUrlCannotCreatePaginationByItself() {
        val page = RecoveredContractMapper.page(
            RecoveredPageDto<RecoveredMediaDto>(current_page = 1, next_page_url = "http://192.168.1.1/next"),
            MediaType.STREAM
        )
        assertFalse(page.hasNext)
    }
}
