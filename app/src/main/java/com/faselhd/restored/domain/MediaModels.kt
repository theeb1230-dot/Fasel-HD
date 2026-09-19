package com.faselhd.restored.domain

enum class MediaType { MOVIE, SERIES, ANIME, STREAM }

data class MediaSummary(
    val id: String,
    val title: String,
    val type: MediaType,
    val posterUrl: String? = null,
    val backdropUrl: String? = null
)

data class Episode(
    val id: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String? = null
)

data class MediaDetails(
    val media: MediaSummary,
    val overview: String? = null,
    val seasons: List<Int> = emptyList(),
    val episodes: List<Episode> = emptyList()
)

data class Page<T>(
    val items: List<T>,
    val page: Int,
    val hasNext: Boolean
)
