package com.nexapremium.data.api.model

import com.google.gson.annotations.SerializedName

data class SearchResponseDto(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("items") val items: List<SubjectItemDto>? = emptyList(),
    @SerializedName("data") val data: SearchDataDto? = null
)

data class SearchDataDto(
    @SerializedName("items") val items: List<SubjectItemDto>? = emptyList(),
    @SerializedName("total") val total: Int? = 0
)

data class SearchSuggestionResponseDto(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<String>? = emptyList()
)

data class PopularSearchResponseDto(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<SubjectItemDto>? = emptyList()
)
