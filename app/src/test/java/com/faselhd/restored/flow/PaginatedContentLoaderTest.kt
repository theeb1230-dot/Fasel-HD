package com.faselhd.restored.flow

import com.faselhd.restored.domain.MediaSummary
import com.faselhd.restored.domain.MediaType
import com.faselhd.restored.domain.Page
import com.faselhd.restored.provider.ProviderLoadException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PaginatedContentLoaderTest {
    private val one = MediaSummary("1", "One", MediaType.MOVIE)
    private val two = MediaSummary("2", "Two", MediaType.MOVIE)

    @Test fun `refresh then load more accumulates pages`() {
        runBlocking {
            val loader = PaginatedContentLoader { page ->
                if (page == 1) Page(listOf(one), 1, true) else Page(listOf(two), 2, false)
            }
            assertEquals(PaginatedContentLoader.State.Content(listOf(one), 1, true), loader.refresh())
            assertEquals(PaginatedContentLoader.State.Content(listOf(one, two), 2, false), loader.loadMore())
            assertEquals(loader.state, loader.loadMore())
        }
    }

    @Test fun `empty first page becomes explicit empty state`() {
        runBlocking {
            val loader = PaginatedContentLoader { Page(emptyList(), it, false) }
            assertEquals(PaginatedContentLoader.State.Empty, loader.refresh())
        }
    }

    @Test fun `retryable provider failure preserves previous items and retry succeeds`() {
        runBlocking {
            var failSecond = true
            val loader = PaginatedContentLoader { page ->
                if (page == 1) Page(listOf(one), 1, true)
                else if (failSecond) throw ProviderLoadException("HTTP 503", true)
                else Page(listOf(two), 2, false)
            }
            loader.refresh()
            val error = loader.loadMore() as PaginatedContentLoader.State.Error
            assertEquals(listOf(one), error.previous)
            assertTrue(error.retryable)
            failSecond = false
            assertEquals(PaginatedContentLoader.State.Content(listOf(one, two), 2, false), loader.retry())
        }
    }

    @Test fun `cancellation propagates instead of becoming an error`() {
        runBlocking {
            val loader = PaginatedContentLoader { throw CancellationException("cancelled") }
            try {
                loader.refresh()
                fail("expected cancellation")
            } catch (_: CancellationException) {
                assertEquals(PaginatedContentLoader.State.Idle, loader.state)
            }
        }
    }
}
