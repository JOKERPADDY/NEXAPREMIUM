package com.nexapremium.data.api.model

import com.google.gson.annotations.SerializedName

data class CatalogFiltersResponseDto(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<FilterOptionDto>? = emptyList()
)

data class FilterOptionDto(
    @SerializedName("key") val key: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("options") val options: List<FilterValueDto>? = emptyList()
)

data class FilterValueDto(
    @SerializedName("value") val value: String? = null,
    @SerializedName("label") val label: String? = null
)
