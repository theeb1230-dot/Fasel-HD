package com.faselhd.restored.provider

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okio.Timeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ProviderTransportCancellationRuntimeTest {
    @Test
    fun coroutineCancellationCancelsTheActiveOkHttpCall() = runBlocking {
        val entered = CountDownLatch(1)
        val calls = TrackingCallFactory(entered)
        val transport = ProviderTransport(callFactory = calls)

        val request: Job = launch(start = CoroutineStart.UNDISPATCHED) {
            transport.get("https://example.org/fixture")
        }
        assertTrue("request should enter OkHttp", entered.await(2, TimeUnit.SECONDS))
        request.cancel()
        request.join()

        assertTrue(
            "coroutine cancellation must cancel the active call",
            calls.last.get()!!.cancelled.get(),
        )
    }

    private class TrackingCallFactory(
        private val entered: CountDownLatch,
    ) : Call.Factory {
        val last = AtomicReference<TrackingCall?>()

        override fun newCall(request: Request): Call = TrackingCall(request, entered).also(last::set)
    }

    private class TrackingCall(
        private val requestValue: Request,
        private val entered: CountDownLatch,
    ) : Call {
        val cancelled = AtomicBoolean(false)

        override fun request(): Request = requestValue

        override fun execute(): Response = throw UnsupportedOperationException("execute is not used")

        override fun enqueue(responseCallback: Callback) {
            entered.countDown()
        }

        override fun cancel() {
            cancelled.set(true)
        }

        override fun isExecuted(): Boolean = false

        override fun isCanceled(): Boolean = cancelled.get()

        override fun timeout(): Timeout = Timeout.NONE

        override fun clone(): Call = TrackingCall(requestValue, entered)
    }
}
