package com.nexapremium.data.sports

import com.nexapremium.data.provider.AkSportsLiveProvider
import javax.inject.Inject
import javax.inject.Singleton

data class SportsMatch(
    val id: String,
    val title: String,
    val category: String,
    val league: String,
    val date: Long,
    val thumbnailUrl: String,
    val sources: List<SportsStream>,
    val live: Boolean = false
) {
    val isLive: Boolean get() = live || sources.any { it.live }
}

data class SportsStream(
    val quality: String,
    val url: String,
    val live: Boolean,
    val broadcast: String?,
    val headers: Map<String, String> = emptyMap(),
    val resolverSource: String = broadcast.orEmpty(),
    val resolverId: String = "",
    val streamNo: Int = 1
)

data class ResolvedSportsStream(
    val url: String,
    val headers: Map<String, String> = emptyMap()
)

@Singleton
class SportsRepository @Inject constructor(
    private val akSportsLiveProvider: AkSportsLiveProvider
) {
    suspend fun getLiveMatches(): List<SportsMatch> {
        val matches = akSportsLiveProvider.getLiveMatches().ifEmpty { akSportsLiveProvider.getAllMatches() }
        return matches.map { match ->
        SportsMatch(
            id = match.id,
            title = match.title,
            category = match.sport,
            league = match.league,
            date = match.date ?: 0L,
            thumbnailUrl = match.poster.orEmpty(),
            live = match.status == "LIVE",
            sources = match.sources.map { source ->
                SportsStream(
                    quality = source.quality,
                    url = source.url,
                    live = match.status == "LIVE",
                    broadcast = match.sport,
                    resolverSource = source.resolverSource.orEmpty(),
                    resolverId = source.resolverId.orEmpty()
                )
            }
        )
        }
    }

    suspend fun getStreams(matchId: String): List<SportsStream> =
        akSportsLiveProvider.getStreams(matchId).map { stream ->
            SportsStream(
                quality = if (stream.hd) "HD" else "SD",
                url = stream.embedUrl,
                live = true,
                broadcast = stream.source,
                headers = stream.headers,
                resolverSource = stream.resolverSource,
                resolverId = stream.resolverId,
                streamNo = stream.streamNo
            )
        }

    suspend fun resolveStream(stream: SportsStream): ResolvedSportsStream {
        val resolved = akSportsLiveProvider.resolveStream(
            source = stream.resolverSource,
            id = stream.resolverId,
            streamNo = stream.streamNo
        )
        return ResolvedSportsStream(resolved.url, resolved.headers)
    }
}
