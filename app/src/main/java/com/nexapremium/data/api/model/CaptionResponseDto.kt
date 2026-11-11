package com.nexapremium.data.api.model

import com.google.gson.annotations.SerializedName

data class CaptionResponseDto(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<CaptionItemDto>? = emptyList()
)

data class CaptionItemDto(
    @SerializedName("caption_id") val captionId: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("format") val format: String? = "srt"
)
