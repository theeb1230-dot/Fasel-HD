package com.faselhd.restored.provider

import com.faselhd.restored.domain.MediaDetails
import com.faselhd.restored.domain.MediaSummary
import com.faselhd.restored.domain.MediaType
import com.faselhd.restored.domain.Page
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class ConfiguredContentProviderTest {
    private fun provider(body: String, code: Int = 200): ConfiguredContentProvider {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("fixture")
                .body(body.toResponseBody())
                .build()
        }.build()
        return ConfiguredContentProvider(
            pageLoader = ProviderPageLoader(ProviderTransport(client = client)),
            catalogUrl = { type, page -> "https://example.org/catalog/${type.name.lowercase()}?page=$page" },
            searchUrl = { query, page -> "https://example.org/search?q=$query&page=$page" },
            detailsLoader = { id, type -> MediaDetails(MediaSummary(id, "fixture", type)) },
            sourcesLoader = { _, _ -> emptyList() }
        )
    }

    @Test fun catalogFlowsThroughTransportDecodeAndMapping() = runBlocking {
        val json = """{"current_page":2,"next_page_url":"https://example.org/page/3","data":[{"id":"m1","title":"Movie One"}]}"""
        val page = provider(json).catalog(MediaType.MOVIE, 2)
        assertEquals(2, page.page)
        assertTrue(page.hasNext)
        assertEquals("m1", page.items.single().id)
        assertEquals(MediaType.MOVIE, page.items.single().type)
    }

    @Test fun invalidPayloadIsNonRetryable() = runBlocking {
        try {
            provider("not-json").catalog(MediaType.MOVIE, 1)
            fail("expected ProviderLoadException")
        } catch (error: ProviderLoadException) {
            assertFalse(error.retryable)
        }
    }

    @Test fun serverFailureIsRetryable() = runBlocking {
        try {
            provider("{}", 503).catalog(MediaType.MOVIE, 1)
            fail("expected ProviderLoadException")
        } catch (error: ProviderLoadException) {
            assertTrue(error.retryable)
            assertEquals("http_503", error.providerReason)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankSearchIsRejectedBeforeTransport() = runBlocking {
        provider("{}").search("   ", 1)
    }
}
