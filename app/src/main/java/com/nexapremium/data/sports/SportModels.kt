package com.nexapremium.data.sports

data class MatchSource(
    val quality: String,
    val url: String,
    val id: String = url,
    val resolverSource: String? = null,
    val resolverId: String? = null
)

data class SportMatch(
    val id: String,
    val title: String,
    val homeTeam: String = "",
    val awayTeam: String = "",
    val league: String = "",
    val status: String = "UPCOMING",
    val time: String = "",
    val score: String = "vs",
    val sport: String = "",
    val poster: String? = null,
    val date: Long? = null,
    val sources: List<MatchSource> = emptyList()
) {
    val isLive: Boolean get() = status == "LIVE"
}

data class SportStream(
    val id: String,
    val streamNo: Int,
    val language: String = "Unknown",
    val hd: Boolean = true,
    val embedUrl: String,
    val source: String,
    val thumbnail: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val resolverSource: String = source,
    val resolverId: String = id
)

data class ResolvedSportStream(
    val url: String,
    val headers: Map<String, String> = emptyMap()
)

data class SportCategory(
    val id: String,
    val title: String,
    val poster: String? = null
)
