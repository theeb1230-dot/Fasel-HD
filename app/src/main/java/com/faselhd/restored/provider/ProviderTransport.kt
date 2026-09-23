package com.faselhd.restored.provider

import com.faselhd.restored.network.SafeDns
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
 * Coroutine cancellation actively cancels the underlying OkHttp call. SafeDns validates
 * resolved addresses at connection time so a public hostname cannot rebind to local space.
 * Successful response bodies are bounded to prevent an untrusted provider from exhausting
 * application memory, including when Content-Length is missing or dishonest.
 */
class ProviderTransport(
    connectTimeoutSeconds: Long = 10,
    readTimeoutSeconds: Long = 15,
    private val maxResponseBytes: Long = DEFAULT_MAX_RESPONSE_BYTES,
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .dns(SafeDns())
        .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build(),
    private val callFactory: Call.Factory = client,
) {
    init {
        require(maxResponseBytes > 0) { "maxResponseBytes must be positive" }
        require(maxAttempts in 1..3) { "maxAttempts must be between 1 and 3" }
    }

    suspend fun get(url: String): TransportResult {
        val normalized = SafeHttp.normalize(url) ?: return TransportResult.Rejected("unsafe_url")
        var lastResult: TransportResult? = null
        repeat(maxAttempts) { attempt ->
            val result = getOnce(normalized)
            lastResult = result
            val retryable = when (result) {
                is TransportResult.HttpError -> result.code == 408 || result.code == 429 || result.code >= 500
                is TransportResult.NetworkError -> true
                else -> false
            }
            if (!retryable || attempt == maxAttempts - 1) return result
        }
        return lastResult ?: TransportResult.NetworkError("transport_error")
    }

    private suspend fun getOnce(normalized: String): TransportResult =
        suspendCancellableCoroutine { continuation ->
            val request = Request.Builder().url(normalized).get().build()
            val call = callFactory.newCall(request)
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
                        val result = when {
                            !response.isSuccessful -> TransportResult.HttpError(response.code)
                            response.body == null -> TransportResult.Success("", response.code)
                            response.body!!.contentLength() > maxResponseBytes -> TransportResult.Rejected("response_too_large")
                            else -> readBoundedBody(response)
                        }
                        if (continuation.isActive) continuation.resume(result)
                    }
                }
            })
        }

    private fun readBoundedBody(response: Response): TransportResult {
        val body = response.body ?: return TransportResult.Success("", response.code)
        val source = body.source()
        source.request(maxResponseBytes + 1)
        if (source.buffer.size > maxResponseBytes) return TransportResult.Rejected("response_too_large")
        return TransportResult.Success(source.buffer.readUtf8(), response.code)
    }

    companion object {
        const val DEFAULT_MAX_RESPONSE_BYTES: Long = 2L * 1024L * 1024L
        const val DEFAULT_MAX_ATTEMPTS: Int = 2
    }
}
