package com.nexapremium.data.api.model

import com.google.gson.annotations.SerializedName

data class DetailResponseDto(
    @SerializedName("title") val title: SubjectItemDto? = null,
    @SerializedName("cast") val cast: List<PersonDto>? = emptyList(),
    @SerializedName("seasons") val seasons: List<SeasonDto>? = emptyList(),
    @SerializedName("dubs") val dubs: List<DubDto>? = emptyList(),
    @SerializedName("trailer_url") val trailerUrl: String? = null
)

data class ResourceDto(
    @SerializedName("seasons") val seasons: List<SeasonDto>? = emptyList(),
    @SerializedName("source") val source: String? = null
)

data class SeasonDto(
    @SerializedName("se") val se: Int? = null,
    @SerializedName("number") val numberField: Int? = null,
    @SerializedName("maxEp") val maxEp: Int? = null,
    @SerializedName("episode_count") val episodeCountField: Int? = null,
    @SerializedName("resolutions") val resolutionsRaw: List<Any>? = emptyList()
) {
    val number: Int
        get() = se ?: numberField ?: 1

    val episodeCount: Int
        get() = maxEp ?: episodeCountField ?: 0

    val resolutions: List<Int>
        get() {
            if (resolutionsRaw.isNullOrEmpty()) return emptyList()
            val result = mutableListOf<Int>()
            for (item in resolutionsRaw) {
                if (item is Number) {
                    result.add(item.toInt())
                } else if (item is Map<*, *>) {
                    val res = (item["resolution"] as? Number)?.toInt()
                    if (res != null) result.add(res)
                }
            }
            return result.distinct().sorted()
        }
}

data class DubDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("subjectId") val subjectId: String? = null,
    @SerializedName("detail_path") val detailPathSnake: String? = null,
    @SerializedName("detailPath") val detailPathCamel: String? = null,
    @SerializedName("lanName") val lanName: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("lanCode") val lanCode: String? = null,
    @SerializedName("original") val original: Boolean? = false
) {
    val detailPath: String?
        get() = detailPathCamel ?: detailPathSnake ?: id

    val effectiveLabel: String
        get() = lanName ?: label ?: language ?: "Audio"
}

data class TrailerObjectDto(
    @SerializedName("videoAddress") val videoAddress: TrailerVideoAddressDto? = null
)

data class TrailerVideoAddressDto(
    @SerializedName("url") val url: String? = null
)

data class MovieDetailResponseDto(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: DetailResponseDto? = null,
    @SerializedName("subject") val subject: SubjectItemDto? = null,
    @SerializedName("title") val directTitle: SubjectItemDto? = null,
    @SerializedName("stars") val stars: List<PersonDto>? = emptyList(),
    @SerializedName("cast") val directCast: List<PersonDto>? = emptyList(),
    @SerializedName("resource") val resource: ResourceDto? = null,
    @SerializedName("seasons") val directSeasons: List<SeasonDto>? = emptyList(),
    @SerializedName("dubs") val directDubs: List<DubDto>? = emptyList(),
    @SerializedName("trailer_url") val directTrailerUrl: String? = null,
    @SerializedName("trailer") val trailerObject: TrailerObjectDto? = null
) {
    val detailResponse: DetailResponseDto
        get() {
            if (data != null) return data
            val titleItem = subject ?: directTitle
            val castList = if (!stars.isNullOrEmpty()) stars else directCast
            val seasonsList = if (!resource?.seasons.isNullOrEmpty()) resource?.seasons else directSeasons
            val dubsList = if (!subject?.dubs.isNullOrEmpty()) subject?.dubs else directDubs
            val trailer = trailerObject?.videoAddress?.url ?: directTrailerUrl
            return DetailResponseDto(
                title = titleItem,
                cast = castList ?: emptyList(),
                seasons = seasonsList ?: emptyList(),
                dubs = dubsList ?: emptyList(),
                trailerUrl = trailer
            )
        }
}

data class MovieDetailDto(
    @SerializedName("subject_id") val subjectId: String? = null,
    @SerializedName("detail_path") val detailPath: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("original_title") val originalTitle: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("release_year") val releaseYear: Int? = null,
    @SerializedName("duration_minutes") val durationMinutes: Int? = null,
    @SerializedName("score") val score: Double? = null,
    @SerializedName("cover") val cover: CoverDto? = null,
    @SerializedName("backdrop") val backdrop: CoverDto? = null,
    @SerializedName("genres") val genres: List<TagDto>? = emptyList(),
    @SerializedName("actors") val actors: List<PersonDto>? = emptyList(),
    @SerializedName("directors") val directors: List<PersonDto>? = emptyList()
)

data class SeriesDetailResponseDto(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: SeriesDetailDto? = null
)

data class SeriesDetailDto(
    @SerializedName("subject_id") val subjectId: String? = null,
    @SerializedName("detail_path") val detailPath: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("release_year") val releaseYear: Int? = null,
    @SerializedName("score") val score: Double? = null,
    @SerializedName("cover") val cover: CoverDto? = null,
    @SerializedName("backdrop") val backdrop: CoverDto? = null,
    @SerializedName("total_seasons") val totalSeasons: Int? = 1,
    @SerializedName("total_episodes") val totalEpisodes: Int? = 0,
    @SerializedName("genres") val genres: List<TagDto>? = emptyList()
)

data class SeriesEpisodesResponseDto(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: SeriesEpisodesDataDto? = null,
    @SerializedName("season") val directSeason: Int? = 1,
    @SerializedName("episodes") val directEpisodes: List<EpisodeDto>? = emptyList()
) {
    val episodesData: SeriesEpisodesDataDto
        get() = data ?: SeriesEpisodesDataDto(season = directSeason, episodes = directEpisodes)
}

data class SeriesEpisodesDataDto(
    @SerializedName("season") val season: Int? = 1,
    @SerializedName("episodes") val episodes: List<EpisodeDto>? = emptyList()
)

data class EpisodeDto(
    @SerializedName("episode_id") val episodeId: String? = null,
    @SerializedName("episode_number") val episodeNumber: Int? = null,
    @SerializedName("number") val number: Int? = null,
    @SerializedName("season_number") val seasonNumber: Int? = null,
    @SerializedName("season") val season: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("cover") val cover: CoverDto? = null,
    @SerializedName("duration_minutes") val durationMinutes: Int? = null
) {
    val effectiveEpisodeNumber: Int
        get() = episodeNumber ?: number ?: 1
    val effectiveSeasonNumber: Int
        get() = seasonNumber ?: season ?: 1
}

data class PersonDto(
    @SerializedName("person_id") val personIdSnake: String? = null,
    @SerializedName("staffId") val staffId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("avatar_url") val avatarUrlSnake: String? = null,
    @SerializedName("avatarUrl") val avatarUrlCamel: String? = null,
    @SerializedName("character") val character: String? = null,
    @SerializedName("detailPath") val detailPath: String? = null
) {
    val personId: String?
        get() = staffId ?: personIdSnake

    val avatarUrl: String?
        get() = avatarUrlCamel ?: avatarUrlSnake
}
