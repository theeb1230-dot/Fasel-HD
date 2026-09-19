package com.faselhd.restored.provider

import com.faselhd.restored.core.PlaybackSource
import com.faselhd.restored.domain.MediaDetails
import com.faselhd.restored.domain.MediaSummary
import com.faselhd.restored.domain.MediaType
import com.faselhd.restored.domain.Page

interface ContentProvider {
    suspend fun catalog(type: MediaType, page: Int = 1): Page<MediaSummary>
    suspend fun search(query: String, page: Int = 1): Page<MediaSummary>
    suspend fun details(id: String, type: MediaType): MediaDetails
    suspend fun sources(mediaId: String, episodeId: String? = null): List<PlaybackSource>
}
