package com.faselhd.restored.provider

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Timeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ProviderTransportCancellationRuntimeTest {
    @Test
    fun coroutineCancellationCancelsTheActiveOkHttpCall() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                entered.countDown()
                release.await(5, TimeUnit.SECONDS)
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("ok")
                    .body("{}".toResponseBody())
                    .build()
            }
            .build()
        val calls = TrackingCallFactory(client)
        val transport = ProviderTransport(client = client, callFactory = calls)

        val request: Job = launch {
            transport.get("https://example.org/fixture")
        }
        assertTrue("request should enter OkHttp", entered.await(2, TimeUnit.SECONDS))
        request.cancel()
        request.join()

        assertTrue("coroutine cancellation must cancel the active call", calls.last.get()!!.cancelled.get())
        release.countDown()
    }

    private class TrackingCallFactory(private val delegate: Call.Factory) : Call.Factory {
        val last = AtomicReference<TrackingCall?>()

        override fun newCall(request: Request): Call {
            return TrackingCall(delegate.newCall(request)).also(last::set)
        }
    }

    private class TrackingCall(private val delegate: Call) : Call {
        val cancelled = AtomicBoolean(false)

        override fun request(): Request = delegate.request()
        override fun execute(): Response = delegate.execute()
        override fun enqueue(responseCallback: Callback) = delegate.enqueue(responseCallback)
        override fun cancel() {
            cancelled.set(true)
            delegate.cancel()
        }
        override fun isExecuted(): Boolean = delegate.isExecuted()
        override fun isCanceled(): Boolean = delegate.isCanceled()
        override fun timeout(): Timeout = delegate.timeout()
        override fun clone(): Call = TrackingCall(delegate.clone())
    }
}
