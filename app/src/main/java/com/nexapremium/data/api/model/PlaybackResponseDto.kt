package com.nexapremium.data.api.model

import com.google.gson.annotations.SerializedName

data class PlaybackResponseDto(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: PlaybackDataDto? = null,
    @SerializedName("title_id") val titleId: String? = null,
    @SerializedName("season") val season: Int? = null,
    @SerializedName("episode") val episode: Int? = null,
    @SerializedName("streams") val directStreams: List<MediaSourceDto>? = emptyList(),
    @SerializedName("limited") val limited: Boolean? = false,
    @SerializedName("vip_locked") val vipLocked: Boolean? = false
) {
    val playbackData: PlaybackDataDto
        get() = data ?: PlaybackDataDto(
            streamId = directStreams?.firstOrNull()?.id,
            title = null,
            sources = directStreams
        )
}

data class PlaybackDataDto(
    @SerializedName("stream_id") val streamId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("sources") val sources: List<MediaSourceDto>? = emptyList(),
    @SerializedName("streams") val directStreams: List<MediaSourceDto>? = emptyList()
) {
    val effectiveSources: List<MediaSourceDto>
        get() = sources.takeIf { !it.isNullOrEmpty() } ?: directStreams ?: emptyList()

    val playableSources: List<MediaSourceDto>
        get() = effectiveSources.filter { source ->
            val isDash = source.format.equals("DASH", ignoreCase = true) || source.url?.endsWith(".mpd") == true
            !isDash && !source.url.isNullOrBlank()
        }
}

data class MediaSourceDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("resolutions") val resolutions: String? = null,
    @SerializedName("format") val format: String? = null, // "MP4" or "DASH"
    @SerializedName("url") val url: String? = null,
    @SerializedName("size_bytes") val sizeBytes: Long? = null,
    @SerializedName("request_headers") val requestHeaders: Map<String, String>? = emptyMap(),
    @SerializedName("headers") val headers: Map<String, String>? = emptyMap(),
    @SerializedName("vip_locked") val vipLocked: Boolean? = false
) {
    val effectiveQuality: String
        get() {
            val q = quality ?: resolutions ?: "HD"
            return if (q.all { it.isDigit() }) "${q}P" else q
        }
    val effectiveHeaders: Map<String, String>
        get() = requestHeaders.takeIf { !it.isNullOrEmpty() } ?: headers ?: emptyMap()
}
