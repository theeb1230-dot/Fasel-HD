package com.faselhd.restored.provider

import com.faselhd.restored.domain.MediaSummary
import com.faselhd.restored.domain.MediaType
import com.faselhd.restored.domain.Page

sealed interface ProviderLoadResult<out T> {
    data class Success<T>(val value: T) : ProviderLoadResult<T>
    data class Failure(val reason: String, val retryable: Boolean) : ProviderLoadResult<Nothing>
}

/**
 * Credential-free integration boundary from authorized endpoint -> transport -> decode -> domain.
 * The caller supplies the endpoint; recovered hosts/tokens/cookies never live here.
 */
class ProviderPageLoader(private val transport: ProviderTransport) {
    suspend fun mediaPage(url: String, type: MediaType): ProviderLoadResult<Page<MediaSummary>> =
        when (val response = transport.get(url)) {
            is TransportResult.Success -> when (val decoded = ProviderJsonAdapter.mediaPage(response.body)) {
                is ProviderDecodeResult.Success -> ProviderLoadResult.Success(
                    RecoveredContractMapper.page(decoded.value, type)
                )
                is ProviderDecodeResult.Invalid -> ProviderLoadResult.Failure(decoded.reason, retryable = false)
            }
            is TransportResult.HttpError -> ProviderLoadResult.Failure(
                reason = "http_${response.code}",
                retryable = response.code == 408 || response.code == 429 || response.code >= 500
            )
            is TransportResult.NetworkError -> ProviderLoadResult.Failure("network_error", retryable = true)
            is TransportResult.Rejected -> ProviderLoadResult.Failure(response.reason, retryable = false)
        }
}
