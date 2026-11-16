package com.nexapremium.data.api.model

import com.google.gson.annotations.SerializedName

data class CDNChannel(
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String,
    @SerializedName("url") val url: String = "",
    @SerializedName("image") val image: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("viewers") val viewers: Int = 0
)

data class CDNChannelsResponse(
    @SerializedName("total_channels") val totalChannels: Int = 0,
    @SerializedName("channels") val channels: List<CDNChannel> = emptyList()
)

data class CDNSportEvent(
    @SerializedName("id") val id: String = "",
    @SerializedName("home") val home: String = "",
    @SerializedName("away") val away: String = "",
    @SerializedName("league") val league: String = "",
    @SerializedName("time") val time: String = "",
    @SerializedName("status") val status: String = ""
)

data class ExtractedVideo(
    val source: String,
    val headers: Map<String, String> = emptyMap(),
    val quality: String = "LIVE",
    val format: String = "hls"
)
