package com.nexapremium.data.repository

import com.nexapremium.data.api.NexaApiService
import com.nexapremium.data.api.normalizeTitlePath
import com.nexapremium.data.api.model.EpisodeDto
import com.nexapremium.data.api.model.HomeResponseDto
import com.nexapremium.data.api.model.MovieDetailResponseDto
import com.nexapremium.data.api.model.SeasonDto
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetailRepository @Inject constructor(
    private val apiService: NexaApiService
) {
    suspend fun getTitleDetail(detailPath: String): Result<MovieDetailResponseDto> = runCatching {
        val path = normalizeTitlePath(detailPath)
        try {
            apiService.getTitleDetail(path)
        } catch (error: HttpException) {
            if (error.code() != 404) throw error

            // Some backend regions can return a transient 404 for a title that is still
            // present in search and has working episodes/playback endpoints.
            val searchQuery = path
                .replace(Regex("-[A-Za-z0-9]{8,}$"), "")
                .replace('-', ' ')
                .trim()
            val search = apiService.search(searchQuery)
            val item = (search.items.orEmpty() + search.data?.items.orEmpty())
                .distinctBy { it.detailPath }
                .firstOrNull {
                    it.hasResource != false && normalizeTitlePath(it.detailPath.orEmpty()) == path
                } ?: throw error

            val isSeries = item.effectiveSubjectType.equals("series", true) ||
                item.effectiveSubjectType.equals("tv", true)
            MovieDetailResponseDto(
                directTitle = item,
                directSeasons = if (isSeries) listOf(SeasonDto(number = 1)) else emptyList()
            )
        }
    }

    suspend fun getSeriesEpisodes(detailPath: String, season: Int = 1): Result<List<EpisodeDto>> = runCatching {
        apiService.getSeriesEpisodes(normalizeTitlePath(detailPath), season)
    }

    suspend fun getRecommendations(detailPath: String, page: Int = 1): Result<HomeResponseDto> = runCatching {
        apiService.getRecommendations(normalizeTitlePath(detailPath), page)
    }
}
