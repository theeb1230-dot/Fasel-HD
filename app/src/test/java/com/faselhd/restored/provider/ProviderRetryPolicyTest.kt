package com.faselhd.restored.provider

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderRetryPolicyTest {
    @Test
    fun retriesTransientFailureThenSucceeds() = runBlocking {
        var calls = 0
        val sleeps = mutableListOf<Long>()
        val policy = ProviderRetryPolicy(maxAttempts = 3, initialDelayMs = 10, maxDelayMs = 20) { sleeps += it }
        val result = policy.execute {
            calls++
            if (calls < 3) ProviderLoadResult.Failure("network_error", retryable = true)
            else ProviderLoadResult.Success("ok")
        }
        assertTrue(result is ProviderLoadResult.Success)
        assertEquals(3, calls)
        assertEquals(listOf(10L, 20L), sleeps)
    }

    @Test
    fun doesNotRetryPermanentFailure() = runBlocking {
        var calls = 0
        val policy = ProviderRetryPolicy(maxAttempts = 3, initialDelayMs = 0, maxDelayMs = 0)
        val result = policy.execute {
            calls++
            ProviderLoadResult.Failure("unsafe_url", retryable = false)
        }
        assertTrue(result is ProviderLoadResult.Failure)
        assertEquals(1, calls)
    }

    @Test
    fun stopsAtAttemptLimit() = runBlocking {
        var calls = 0
        val policy = ProviderRetryPolicy(maxAttempts = 2, initialDelayMs = 0, maxDelayMs = 0)
        val result = policy.execute {
            calls++
            ProviderLoadResult.Failure("http_503", retryable = true)
        }
        assertTrue(result is ProviderLoadResult.Failure)
        assertEquals(2, calls)
    }

    @Test(expected = CancellationException::class)
    fun cancellationFromAttemptIsNotConvertedToFailure() = runBlocking<Unit> {
        val policy = ProviderRetryPolicy(maxAttempts = 3, initialDelayMs = 0, maxDelayMs = 0)
        policy.execute<String> { throw CancellationException("cancelled") }
    }
}
