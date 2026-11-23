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
    @SerializedName("subject_id") val subjectIdSnake: String? = null,
    @SerializedName("subjectId") val subjectIdCamel: String? = null,
    @SerializedName("detail_path") val detailPathSnake: String? = null,
    @SerializedName("detailPath") val detailPathCamel: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("subject_type") val subjectTypeSnake: String? = null,
    @SerializedName("subjectType") val subjectTypeRaw: Any? = null,
    @SerializedName("poster_url") val posterUrl: String? = null,
    @SerializedName("backdrop_url") val backdropUrl: String? = null,
    @SerializedName("cover") val cover: CoverDto? = null,
    @SerializedName("release_date") val releaseDateSnake: String? = null,
    @SerializedName("releaseDate") val releaseDateCamel: String? = null,
    @SerializedName("release_year") val releaseYearRaw: Int? = null,
    @SerializedName("duration_seconds") val durationSecondsSnake: Int? = null,
    @SerializedName("duration") val durationRaw: Int? = null,
    @SerializedName("score") val score: Double? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("imdbRatingValue") val imdbRatingValue: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("countryName") val countryName: String? = null,
    @SerializedName("genres") val genresList: List<String>? = emptyList(),
    @SerializedName("genre") val genreString: String? = null,
    @SerializedName("tags") val tags: List<TagDto>? = emptyList(),
    @SerializedName("dubs") val dubs: List<DubDto>? = emptyList(),
    @SerializedName("isPornographic") val isPornographic: Boolean? = false,
    @SerializedName("webHighRisk") val webHighRisk: Boolean? = false,
    @SerializedName("has_resource") val hasResourceSnake: Boolean? = null,
    @SerializedName("hasResource") val hasResourceCamel: Boolean? = null
) {
    val subjectId: String?
        get() = subjectIdCamel ?: subjectIdSnake ?: id

    val detailPath: String?
        get() = detailPathCamel ?: detailPathSnake

    val releaseDate: String?
        get() = releaseDateCamel ?: releaseDateSnake

    val releaseYear: Int?
        get() = releaseYearRaw ?: releaseDate?.take(4)?.toIntOrNull()

    val durationSeconds: Int?
        get() = durationSecondsSnake ?: durationRaw

    val effectiveScore: Double?
        get() = score ?: imdbRatingValue?.toDoubleOrNull() ?: rating?.toDoubleOrNull()

    val effectiveId: String
        get() = subjectId ?: id ?: ""

    val effectiveCoverUrl: String?
        get() = posterUrl ?: cover?.url

    val effectiveSubjectType: String
        get() {
            if (!kind.isNullOrEmpty()) return kind
            if (!subjectTypeSnake.isNullOrEmpty()) return subjectTypeSnake
            if (subjectTypeRaw is Double) {
                return if (subjectTypeRaw.toInt() == 2) "series" else "movie"
            }
            if (subjectTypeRaw is Long) {
                return if (subjectTypeRaw.toInt() == 2) "series" else "movie"
            }
            if (subjectTypeRaw != null) {
                val str = subjectTypeRaw.toString()
                return if (str == "2" || str.equals("series", true) || str.equals("tv", true)) "series" else "movie"
            }
            return "movie"
        }

    val effectiveGenres: List<String>
        get() {
            if (!genresList.isNullOrEmpty()) return genresList
            if (!genreString.isNullOrEmpty()) return genreString.split(",").map { it.trim() }
            return emptyList()
        }

    val genres: List<String>?
        get() = effectiveGenres

    val subjectType: String?
        get() = effectiveSubjectType

    val hasResource: Boolean
        get() = hasResourceCamel ?: hasResourceSnake ?: true

    val badgeText: String?
        get() {
            val tagText = tags?.firstOrNull()?.name
            if (!tagText.isNullOrEmpty()) return tagText
            val yr = releaseYear
            if (yr != null && yr > 0) return yr.toString()
            val sc = effectiveScore
            if (sc != null && sc > 0) return String.format(java.util.Locale.US, "%.1f", sc)
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
