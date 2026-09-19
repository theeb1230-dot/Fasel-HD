package com.faselhd.restored.flow

import com.faselhd.restored.domain.MediaSummary
import com.faselhd.restored.domain.Page
import com.faselhd.restored.provider.ProviderLoadException
import kotlinx.coroutines.CancellationException

/** Pure stateful coordinator for catalog/search pagination. UI owns the coroutine lifecycle. */
class PaginatedContentLoader(
    private val loadPage: suspend (Int) -> Page<MediaSummary>
) {
    sealed interface State {
        data object Idle : State
        data class Loading(val previous: List<MediaSummary>) : State
        data class Content(val items: List<MediaSummary>, val page: Int, val hasMore: Boolean) : State
        data object Empty : State
        data class Error(
            val previous: List<MediaSummary>,
            val message: String,
            val retryable: Boolean
        ) : State
    }

    var state: State = State.Idle
        private set

    private var nextPage = 1
    private var lastRequestedPage = 1
    private val accumulated = mutableListOf<MediaSummary>()

    suspend fun refresh(): State {
        accumulated.clear()
        nextPage = 1
        return request(nextPage)
    }

    suspend fun loadMore(): State {
        val current = state
        if (current is State.Loading) return current
        if (current is State.Content && !current.hasMore) return current
        return request(nextPage)
    }

    suspend fun retry(): State = request(lastRequestedPage)

    private suspend fun request(page: Int): State {
        lastRequestedPage = page
        state = State.Loading(accumulated.toList())
        return try {
            val result = loadPage(page)
            if (page == 1) accumulated.clear()
            accumulated += result.items
            nextPage = result.page + 1
            state = if (accumulated.isEmpty()) State.Empty
            else State.Content(accumulated.toList(), result.page, result.hasMore)
            state
        } catch (cancelled: CancellationException) {
            state = if (accumulated.isEmpty()) State.Idle
            else State.Content(accumulated.toList(), (nextPage - 1).coerceAtLeast(1), true)
            throw cancelled
        } catch (error: ProviderLoadException) {
            state = State.Error(accumulated.toList(), error.providerReason, error.retryable)
            state
        } catch (error: Exception) {
            state = State.Error(accumulated.toList(), error.message ?: "Unable to load content", false)
            state
        }
    }
}
