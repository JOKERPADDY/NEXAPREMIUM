package com.nexapremium.data.repository

import com.nexapremium.data.api.NexaApiService
import com.nexapremium.data.api.model.BrowseResponseDto
import com.nexapremium.data.api.model.HomeResponseDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val apiService: NexaApiService
) {
    suspend fun getHome(page: Int = 1): Result<HomeResponseDto> = runCatching {
        apiService.getHome(page)
    }

    suspend fun getCatalog(name: String, page: Int = 1): Result<HomeResponseDto> = runCatching {
        apiService.getCatalog(name, page)
    }

    suspend fun getBrowse(
        page: Int = 1,
        type: String? = null,
        genre: String? = null,
        tab: String? = null
    ): Result<BrowseResponseDto> = runCatching {
        apiService.getBrowse(page = page, kind = type, tab = tab)
    }
}
