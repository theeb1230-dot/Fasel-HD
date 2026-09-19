package com.faselhd.restored.provider

import com.faselhd.restored.domain.Episode
import com.faselhd.restored.domain.MediaSummary
import com.faselhd.restored.domain.MediaType
import com.faselhd.restored.domain.Page
import com.faselhd.restored.network.SafeHttp

/**
 * Clean-room boundary for response fields observed in the reference application.
 * It intentionally contains no host, access code, credential, token, or cookie.
 */
data class RecoveredMediaDto(
    val id: String? = null,
    val title: String? = null,
    val name: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val tmdb_id: String? = null,
    val imdb_id: String? = null
)

data class RecoveredEpisodeDto(
    val id: String? = null,
    val season_number: Int? = null,
    val episode_number: Int? = null,
    val title: String? = null
)

data class RecoveredPageDto<T>(
    val data: List<T>? = null,
    val page: Int? = null,
    val current_page: Int? = null,
    val last_page: Int? = null,
    val next_page_url: String? = null
)

object RecoveredContractMapper {
    fun media(dto: RecoveredMediaDto, type: MediaType): MediaSummary? {
        val id = sequenceOf(dto.id, dto.tmdb_id, dto.imdb_id)
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
            .firstOrNull() ?: return null
        val title = sequenceOf(dto.title, dto.name)
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
            .firstOrNull() ?: return null
        return MediaSummary(
            id = id,
            title = title,
            type = type,
            posterUrl = safeOptionalUrl(dto.poster_path),
            backdropUrl = safeOptionalUrl(dto.backdrop_path)
        )
    }

    fun episode(dto: RecoveredEpisodeDto): Episode? {
        val id = dto.id?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val season = dto.season_number ?: return null
        val number = dto.episode_number ?: return null
        if (season < 0 || number <= 0) return null
        return Episode(id, season, number, dto.title?.trim()?.takeIf(String::isNotEmpty))
    }

    fun page(dto: RecoveredPageDto<RecoveredMediaDto>, type: MediaType): Page<MediaSummary> {
        val page = (dto.current_page ?: dto.page ?: 1).coerceAtLeast(1)
        val items = dto.data.orEmpty().mapNotNull { media(it, type) }
        val hasNext = when {
            dto.last_page != null -> page < dto.last_page
            dto.next_page_url != null -> SafeHttp.isAllowed(dto.next_page_url)
            else -> false
        }
        return Page(items, page, hasNext)
    }

    private fun safeOptionalUrl(raw: String?): String? = raw?.let(SafeHttp::normalize)
}
