package com.nexapremium.data.api.model

import com.google.gson.annotations.SerializedName

data class BrowseResponseDto(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("items") val items: List<SubjectItemDto>? = emptyList(),
    @SerializedName("data") val data: SearchDataDto? = null
)
