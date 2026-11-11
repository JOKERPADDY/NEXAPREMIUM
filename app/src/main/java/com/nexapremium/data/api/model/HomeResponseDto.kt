package com.nexapremium.data.api.model

import com.google.gson.annotations.SerializedName

data class HomeResponseDto(
    @SerializedName("banners") val banners: List<SubjectItemDto>? = emptyList(),
    @SerializedName("sections") val sections: List<HomeSectionDto>? = emptyList(),
    @SerializedName("items") val items: List<SubjectItemDto>? = emptyList(),
    @SerializedName("page") val page: Int? = null,
    @SerializedName("next_page") val nextPage: Int? = null,
    @SerializedName("has_more") val hasMore: Boolean? = null,
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: HomeDataDto? = null
)

data class HomeSectionDto(
    @SerializedName("title") val title: String? = null,
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("items") val items: List<SubjectItemDto>? = emptyList()
)

data class HomeDataDto(
    @SerializedName("blocks") val blocks: List<HomeBlockDto>? = emptyList(),
    @SerializedName("pagination") val pagination: PaginationDto? = null
)

data class HomeBlockDto(
    @SerializedName("block_id") val blockId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("layout") val layout: String? = null,
    @SerializedName("items") val items: List<SubjectItemDto>? = emptyList()
)

data class SubjectItemDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("subject_id") val subjectId: String? = null,
    @SerializedName("detail_path") val detailPath: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("subject_type") val subjectType: String? = null,
    @SerializedName("poster_url") val posterUrl: String? = null,
    @SerializedName("backdrop_url") val backdropUrl: String? = null,
    @SerializedName("cover") val cover: CoverDto? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("release_year") val releaseYear: Int? = null,
    @SerializedName("duration_seconds") val durationSeconds: Int? = null,
    @SerializedName("score") val score: Double? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("genres") val genres: List<String>? = emptyList(),
    @SerializedName("tags") val tags: List<TagDto>? = emptyList(),
    @SerializedName("isPornographic") val isPornographic: Boolean? = false,
    @SerializedName("webHighRisk") val webHighRisk: Boolean? = false,
    @SerializedName("has_resource") val hasResource: Boolean? = true
) {
    val effectiveId: String
        get() = id ?: subjectId ?: ""

    val effectiveCoverUrl: String?
        get() = posterUrl ?: cover?.url

    val effectiveSubjectType: String
        get() = kind ?: subjectType ?: "movie"

    val badgeText: String?
        get() {
            val tagText = tags?.firstOrNull()?.name
            if (!tagText.isNullOrEmpty()) return tagText
            if (releaseYear != null && releaseYear > 0) return releaseYear.toString()
            if (score != null && score > 0) return String.format(java.util.Locale.US, "%.1f", score)
            return null
        }
}

data class CoverDto(
    @SerializedName("url") val url: String? = null,
    @SerializedName("aspect_ratio") val aspectRatio: Double? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null
)

data class TagDto(
    @SerializedName("tag_id") val tagId: String? = null,
    @SerializedName("name") val name: String? = null
)

data class PaginationDto(
    @SerializedName("page") val page: Int? = 1,
    @SerializedName("has_next") val hasNext: Boolean? = false
)
