package com.nexapremium.data.api

import com.nexapremium.data.api.model.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NexaApiService {

    @GET("health")
    suspend fun getHealth(): Map<String, Any>

    @GET("v1/home")
    suspend fun getHome(
        @Query("page") page: Int = 1
    ): HomeResponseDto

    @GET("v1/catalog/{name}")
    suspend fun getCatalog(
        @Path("name") name: String,
        @Query("page") page: Int = 1
    ): HomeResponseDto

    @GET("v1/browse")
    suspend fun getBrowse(
        @Query("page") page: Int = 1,
        @Query("kind") kind: String? = null,
        @Query("tab") tab: String? = null
    ): BrowseResponseDto

    @GET("v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 1
    ): SearchResponseDto

    @GET("v1/search/suggestions")
    suspend fun getSearchSuggestions(
        @Query("q") query: String
    ): SearchSuggestionResponseDto

    @GET("v1/search/popular")
    suspend fun getPopularSearches(): PopularSearchResponseDto

    @GET("v1/rankings")
    suspend fun getRankings(): RankingsResponseDto

    @GET("v1/rankings/{id}")
    suspend fun getRankingDetail(
        @Path("id") rankingId: String
    ): RankingsResponseDto

    @GET("v1/catalog/{type}/filters")
    suspend fun getCatalogFilters(
        @Path("type") type: String
    ): CatalogFiltersResponseDto

    @GET("v1/titles/{path}")
    suspend fun getTitleDetail(
        @Path(value = "path", encoded = true) detailPath: String
    ): MovieDetailResponseDto

    @GET("v1/titles/{path}/episodes")
    suspend fun getSeriesEpisodes(
        @Path(value = "path", encoded = true) detailPath: String,
        @Query("season") season: Int = 1
    ): List<EpisodeDto>

    @GET("v1/titles/{path}/recommendations")
    suspend fun getRecommendations(
        @Path(value = "path", encoded = true) detailPath: String,
        @Query("page") page: Int = 1
    ): HomeResponseDto

    @GET("v1/titles/{path}/playback")
    suspend fun getPlayback(
        @Path(value = "path", encoded = true) detailPath: String,
        @Query("season") season: Int = 0,
        @Query("episode") episode: Int = 0
    ): PlaybackResponseDto

    @GET("v1/titles/{path}/captions")
    suspend fun getCaptions(
        @Path(value = "path", encoded = true) detailPath: String,
        @Query("stream_id") streamId: String? = null
    ): CaptionResponseDto
}
