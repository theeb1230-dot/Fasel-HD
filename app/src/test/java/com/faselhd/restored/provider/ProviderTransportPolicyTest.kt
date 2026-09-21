package com.faselhd.restored.provider

import com.faselhd.restored.network.SafeHttp
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

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
        val capturedCall = AtomicReference<Call>()
        val releaseInterceptor = CountDownLatch(1)
        val client = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            capturedCall.set(chain.call())
            entered.countDown()
            try {
                releaseInterceptor.await(3, TimeUnit.SECONDS)
                throw IOException("test interceptor released")
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("test interrupted", e)
            }
        }).build()
        val transport = ProviderTransport(client = client)
        val request = async { transport.get("https://example.org/slow") }

        try {
            assertTrue("interceptor should receive the request", entered.await(1, TimeUnit.SECONDS))
            request.cancelAndJoin()
            val call = capturedCall.get()
            assertNotNull("test must capture the exact OkHttp call", call)
            assertTrue("coroutine cancellation must cancel the exact OkHttp call", call.isCanceled())
        } finally {
            releaseInterceptor.countDown()
            client.dispatcher.executorService.shutdownNow()
            client.connectionPool.evictAll()
        }
    }
}
