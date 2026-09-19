package com.faselhd.restored.provider

import org.json.JSONArray
import org.json.JSONObject

sealed interface ProviderDecodeResult<out T> {
    data class Success<T>(val value: T) : ProviderDecodeResult<T>
    data class Invalid(val reason: String) : ProviderDecodeResult<Nothing>
}

/**
 * Clean-room JSON adapter for response fields already observed in the reference contract.
 * It deliberately knows nothing about hosts, credentials, cookies, access codes or bypasses.
 */
object ProviderJsonAdapter {
    fun mediaPage(body: String): ProviderDecodeResult<RecoveredPageDto<RecoveredMediaDto>> = decode(body) { root ->
        val data = root.optJSONArray("data") ?: JSONArray()
        val items = buildList {
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                add(
                    RecoveredMediaDto(
                        id = item.stringOrNull("id"),
                        title = item.stringOrNull("title"),
                        name = item.stringOrNull("name"),
                        poster_path = item.stringOrNull("poster_path"),
                        backdrop_path = item.stringOrNull("backdrop_path"),
                        tmdb_id = item.stringOrNull("tmdb_id"),
                        imdb_id = item.stringOrNull("imdb_id")
                    )
                )
            }
        }
        RecoveredPageDto(
            data = items,
            page = root.intOrNull("page"),
            current_page = root.intOrNull("current_page"),
            last_page = root.intOrNull("last_page"),
            next_page_url = root.stringOrNull("next_page_url")
        )
    }

    fun episodes(body: String): ProviderDecodeResult<List<RecoveredEpisodeDto>> = decode(body) { root ->
        val array = root.optJSONArray("episodes") ?: root.optJSONArray("data") ?: JSONArray()
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    RecoveredEpisodeDto(
                        id = item.stringOrNull("id"),
                        season_number = item.intOrNull("season_number"),
                        episode_number = item.intOrNull("episode_number"),
                        title = item.stringOrNull("title")
                    )
                )
            }
        }
    }

    private inline fun <T> decode(body: String, block: (JSONObject) -> T): ProviderDecodeResult<T> {
        if (body.length > MAX_BODY_CHARS) return ProviderDecodeResult.Invalid("body_too_large")
        return try {
            ProviderDecodeResult.Success(block(JSONObject(body)))
        } catch (_: Exception) {
            ProviderDecodeResult.Invalid("invalid_json")
        }
    }

    private fun JSONObject.stringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).trim().takeIf { it.isNotEmpty() }

    private fun JSONObject.intOrNull(key: String): Int? =
        if (!has(key) || isNull(key)) null else runCatching { getInt(key) }.getOrNull()

    private const val MAX_BODY_CHARS = 2_000_000
}
