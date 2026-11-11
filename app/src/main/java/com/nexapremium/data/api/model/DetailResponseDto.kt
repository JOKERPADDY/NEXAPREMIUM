package com.nexapremium.data.api.model

import com.google.gson.annotations.SerializedName

data class DetailResponseDto(
    @SerializedName("title") val title: SubjectItemDto? = null,
    @SerializedName("cast") val cast: List<PersonDto>? = emptyList(),
    @SerializedName("seasons") val seasons: List<SeasonDto>? = emptyList(),
    @SerializedName("dubs") val dubs: List<DubDto>? = emptyList(),
    @SerializedName("trailer_url") val trailerUrl: String? = null
)

data class SeasonDto(
    @SerializedName("number") val number: Int? = 1,
    @SerializedName("episode_count") val episodeCount: Int? = 0,
    @SerializedName("resolutions") val resolutions: List<Int>? = emptyList()
)

data class DubDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("detail_path") val detailPath: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("original") val original: Boolean? = false
)

data class MovieDetailResponseDto(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: DetailResponseDto? = null,
    @SerializedName("title") val directTitle: SubjectItemDto? = null,
    @SerializedName("cast") val directCast: List<PersonDto>? = emptyList(),
    @SerializedName("seasons") val directSeasons: List<SeasonDto>? = emptyList(),
    @SerializedName("dubs") val directDubs: List<DubDto>? = emptyList(),
    @SerializedName("trailer_url") val directTrailerUrl: String? = null
) {
    val detailResponse: DetailResponseDto
        get() = data ?: DetailResponseDto(
            title = directTitle,
            cast = directCast,
            seasons = directSeasons,
            dubs = directDubs,
            trailerUrl = directTrailerUrl
        )
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
    @SerializedName("person_id") val personId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)
