package com.faselhd.restored.provider

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class ProviderTransportRetryRuntimeTest {
    @Test
    fun retriesTransientHttpFailureButStopsAtBoundedAttemptCount() = runBlocking {
        val attempts = AtomicInteger(0)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                if (attempts.incrementAndGet() == 1) {
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(503)
                        .message("transient")
                        .body("".toResponseBody())
                        .build()
                } else {
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("ok")
                        .body("{\"ok\":true}".toResponseBody())
                        .build()
                }
            }
            .build()

        val result = ProviderTransport(
            client = client,
            maxAttempts = 2,
        ).get("https://example.org/fixture")

        assertTrue(result is TransportResult.Success)
        assertEquals(2, attempts.get())
    }
}
