package com.faselhd.restored.provider

import com.faselhd.restored.network.SafeHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed interface TransportResult {
    data class Success(val body: String, val code: Int) : TransportResult
    data class HttpError(val code: Int) : TransportResult
    data class NetworkError(val message: String) : TransportResult
    data class Rejected(val reason: String) : TransportResult
}

/**
 * Credential-free provider transport boundary. Endpoints are supplied by an authorized
 * provider implementation; this class never embeds recovered hosts, tokens or cookies.
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
    suspend fun get(url: String): TransportResult = withContext(Dispatchers.IO) {
        val normalized = SafeHttp.normalize(url) ?: return@withContext TransportResult.Rejected("unsafe_url")
        val request = Request.Builder().url(normalized).get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext TransportResult.HttpError(response.code)
                val body = response.body?.string().orEmpty()
                TransportResult.Success(body, response.code)
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (io: IOException) {
            TransportResult.NetworkError(io.message ?: "network_error")
        }
    }
}
