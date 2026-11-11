package com.nexapremium.data.repository

import com.nexapremium.data.api.NexaApiService
import com.nexapremium.data.api.normalizeTitlePath
import com.nexapremium.data.api.model.CaptionResponseDto
import com.nexapremium.data.api.model.PlaybackResponseDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackRepository @Inject constructor(
    private val apiService: NexaApiService
) {
    suspend fun getPlayback(detailPath: String, season: Int = 0, episode: Int = 0): Result<PlaybackResponseDto> = runCatching {
        apiService.getPlayback(normalizeTitlePath(detailPath), season, episode)
    }

    suspend fun getCaptions(detailPath: String, streamId: String? = null): Result<CaptionResponseDto> = runCatching {
        apiService.getCaptions(normalizeTitlePath(detailPath), streamId)
    }
}
