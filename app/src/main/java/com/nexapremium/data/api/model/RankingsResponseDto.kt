package com.nexapremium.data.api.model

import com.google.gson.annotations.SerializedName

data class RankingsResponseDto(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<RankingItemDto>? = emptyList()
)

data class RankingItemDto(
    @SerializedName("ranking_id") val rankingId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("cover") val cover: CoverDto? = null,
    @SerializedName("items") val items: List<SubjectItemDto>? = emptyList()
)
