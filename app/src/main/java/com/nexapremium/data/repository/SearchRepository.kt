package com.nexapremium.data.repository

import com.nexapremium.data.api.NexaApiService
import com.nexapremium.data.api.model.PopularSearchResponseDto
import com.nexapremium.data.api.model.SearchResponseDto
import com.nexapremium.data.api.model.SearchSuggestionResponseDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val apiService: NexaApiService
) {
    suspend fun search(query: String, page: Int = 1): Result<SearchResponseDto> = runCatching {
        apiService.search(query, page)
    }

    suspend fun getSuggestions(query: String): Result<SearchSuggestionResponseDto> = runCatching {
        apiService.getSearchSuggestions(query)
    }

    suspend fun getPopularSearches(): Result<PopularSearchResponseDto> = runCatching {
        apiService.getPopularSearches()
    }
}
