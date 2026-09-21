package com.faselhd.restored.provider

import kotlinx.coroutines.delay

/** Bounded retry policy for transient provider failures. Cancellation is never swallowed. */
class ProviderRetryPolicy(
    private val maxAttempts: Int = 3,
    private val initialDelayMs: Long = 250,
    private val maxDelayMs: Long = 1_000,
    private val sleeper: suspend (Long) -> Unit = { delay(it) }
) {
    init {
        require(maxAttempts >= 1)
        require(initialDelayMs >= 0)
        require(maxDelayMs >= initialDelayMs)
    }

    suspend fun <T> execute(block: suspend () -> ProviderLoadResult<T>): ProviderLoadResult<T> {
        var attempt = 1
        var backoff = initialDelayMs
        while (true) {
            when (val result = block()) {
                is ProviderLoadResult.Success -> return result
                is ProviderLoadResult.Failure -> {
                    if (!result.retryable || attempt >= maxAttempts) return result
                    if (backoff > 0) sleeper(backoff)
                    attempt += 1
                    backoff = (backoff * 2).coerceAtMost(maxDelayMs)
                }
            }
        }
    }
}
