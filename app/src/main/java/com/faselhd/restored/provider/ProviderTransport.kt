package com.faselhd.restored.provider

import com.faselhd.restored.network.SafeHttp
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

sealed interface TransportResult {
    data class Success(val body: String, val code: Int) : TransportResult
    data class HttpError(val code: Int) : TransportResult
    data class NetworkError(val message: String) : TransportResult
    data class Rejected(val reason: String) : TransportResult
}

/**
 * Credential-free provider transport boundary. Endpoints are supplied by an authorized
 * provider implementation; this class never embeds recovered hosts, tokens or cookies.
 * Coroutine cancellation actively cancels the underlying OkHttp call.
 */
class ProviderTransport(
    connectTimeoutSeconds: Long = 10,
    readTimeoutSeconds: Long = 15,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
) {
    suspend fun get(url: String): TransportResult {
        val normalized = SafeHttp.normalize(url) ?: return TransportResult.Rejected("unsafe_url")
        val request = Request.Builder().url(normalized).get().build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resume(TransportResult.NetworkError(e.message ?: "network_error"))
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!continuation.isActive) return
                        val result = if (!response.isSuccessful) {
                            TransportResult.HttpError(response.code)
                        } else {
                            TransportResult.Success(response.body?.string().orEmpty(), response.code)
                        }
                        if (continuation.isActive) continuation.resume(result)
                    }
                }
            })
        }
    }
}
