package com.faselhd.restored.provider

import com.faselhd.restored.network.SafeHttp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ProviderTransportPolicyTest {
    @Test fun rejectsPrivateAndScriptEndpointsBeforeTransport() {
        assertFalse(SafeHttp.isAllowed("http://127.0.0.1/api"))
        assertFalse(SafeHttp.isAllowed("http://192.168.1.2/api"))
        assertFalse(SafeHttp.isAllowed("javascript:alert(1)"))
        assertFalse(SafeHttp.isAllowed("file:///etc/passwd"))
    }

    @Test fun acceptsPublicHttpsEndpointShape() {
        assertEquals("https://example.org/api/catalog?page=2", SafeHttp.normalize("https://example.org/api/catalog?page=2"))
    }

    @Test fun coroutineCancellationCancelsUnderlyingHttpCall() = runBlocking {
        val entered = CountDownLatch(1)
        val observedCancellation = AtomicBoolean(false)
        val client = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            entered.countDown()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
            while (System.nanoTime() < deadline) {
                if (chain.call().isCanceled()) {
                    observedCancellation.set(true)
                    throw IOException("Canceled")
                }
                Thread.sleep(10)
            }
            throw IOException("test timeout")
        }).build()
        val transport = ProviderTransport(client = client)
        val request = async { transport.get("https://example.org/slow") }

        assertTrue("interceptor should receive the request", entered.await(1, TimeUnit.SECONDS))
        request.cancel()
        try {
            request.await()
            fail("cancelled provider request must not return a transport result")
        } catch (_: CancellationException) {
            // expected
        }

        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)
        while (!observedCancellation.get() && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue("coroutine cancellation must cancel the OkHttp call", observedCancellation.get())
    }
}
