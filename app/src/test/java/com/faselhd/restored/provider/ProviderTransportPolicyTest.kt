package com.faselhd.restored.provider

import com.faselhd.restored.network.SafeHttp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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
        val started = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        val capturedCall = AtomicReference<Call>()
        val callFactory = Call.Factory { request: Request ->
            RecordingPendingCall(request, started, cancelled).also(capturedCall::set)
        }
        val client = OkHttpClient.Builder().build()
        val transport = ProviderTransport(client = client, callFactory = callFactory)
        // UNDISPATCHED guarantees ProviderTransport reaches enqueue() before this test thread waits.
        // The fake call never invokes its callback, so the coroutine remains suspended until cancelled.
        val request = async(start = CoroutineStart.UNDISPATCHED) {
            transport.get("https://example.org/slow")
        }

        try {
            assertTrue("transport must enqueue the HTTP call", started.await(1, TimeUnit.SECONDS))
            assertNotNull("transport must create a call before cancellation", capturedCall.get())
            request.cancelAndJoin()
            assertTrue("coroutine cancellation must invoke Call.cancel", cancelled.await(1, TimeUnit.SECONDS))
            assertTrue("the exact transport call must be cancelled", capturedCall.get().isCanceled())
        } finally {
            client.dispatcher.executorService.shutdownNow()
            client.connectionPool.evictAll()
        }
    }

    private class RecordingPendingCall(
        private val request: Request,
        private val started: CountDownLatch,
        private val cancelled: CountDownLatch,
    ) : Call {
        private val executed = AtomicBoolean(false)
        private val canceled = AtomicBoolean(false)

        override fun request(): Request = request
        override fun execute(): Response = error("ProviderTransport must use enqueue, not execute")
        override fun enqueue(responseCallback: Callback) {
            check(executed.compareAndSet(false, true)) { "Already Executed" }
            started.countDown()
        }
        override fun cancel() {
            canceled.set(true)
            cancelled.countDown()
        }
        override fun isExecuted(): Boolean = executed.get()
        override fun isCanceled(): Boolean = canceled.get()
        override fun timeout() = okio.Timeout.NONE
        override fun clone(): Call = RecordingPendingCall(request, started, cancelled)
    }
}
