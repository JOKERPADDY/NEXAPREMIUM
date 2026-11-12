package com.nexapremium.data.provider

import android.net.Uri
import android.util.Log
import com.nexapremium.data.sports.MatchSource
import com.nexapremium.data.sports.ResolvedSportStream
import com.nexapremium.data.sports.SportCategory
import com.nexapremium.data.sports.SportMatch
import com.nexapremium.data.sports.SportStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AkSportsLiveProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private var activeHost: String = "https://strmfree.st"
    private val FALLBACK_HOSTS = listOf(
        "https://strmfree.st",
        "https://streamfree.top",
        "https://streamed.pk",
        "https://strmd.st"
    )

    private val client: OkHttpClient = okHttpClient.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    private val resolverClient: OkHttpClient = okHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val sourceCache = ConcurrentHashMap<String, List<MatchSource>>()
    private val persistedMatches = ConcurrentHashMap<String, SportMatch>()
    private data class ResolverStreamCacheEntry(val streams: List<SportStream>, val cachedAtMs: Long)
    private data class ResolverSelection(val source: String, val id: String)
    private val resolverStreamCache = ConcurrentHashMap<String, ResolverStreamCacheEntry>()
    private val resolverStreamLocks = ConcurrentHashMap<String, Mutex>()

    companion object {
        private const val STREAM_RESOLVER_URL = "https://streamed-pk-resolver-5rlc.vercel.app"
        private const val RESOLVER_STREAM_CACHE_TTL_MS = 60_000L
        private const val LIVE_WINDOW_AFTER_START_MS = 6 * 60 * 60 * 1000L
        private const val LIVE_WINDOW_BEFORE_START_MS = 15 * 60 * 1000L
        private val LIVE_STATUSES = setOf(
            "LIVE", "LIVE_NOW", "IN_PLAY", "IN_PROGRESS", "ONGOING", "RUNNING", "STARTED",
            "FIRST_HALF", "SECOND_HALF", "HALF_TIME", "HALFTIME", "EXTRA_TIME", "PENALTIES",
            "1H", "2H", "HT", "ET", "Q1", "Q2", "Q3", "Q4", "OT", "P1", "P2", "P3",
            "1ST_HALF", "2ND_HALF", "FIRST_QUARTER", "SECOND_QUARTER", "THIRD_QUARTER", "FOURTH_QUARTER",
            "PERIOD_1", "PERIOD_2", "PERIOD_3", "INNINGS_1", "INNINGS_2"
        )
        private val FINISHED_STATUSES = setOf(
            "FINISHED", "COMPLETE", "COMPLETED", "ENDED", "END", "FULL_TIME", "FT", "AET",
            "CANCELLED", "CANCELED", "ABANDONED", "POSTPONED"
        )
        private val UPCOMING_STATUSES = setOf("UPCOMING", "SCHEDULED", "NOT_STARTED", "NS", "TBD", "PRE")
    }

    suspend fun getLiveMatches(): List<SportMatch> = withContext(Dispatchers.IO) {
        try {
            val primaryMatches = fetchWithFallback("/api/v1/streams")
                .let { parseMatchesResponse(it, "LIVE") }
                .filter { it.status == "LIVE" }
            if (primaryMatches.isNotEmpty()) {
                return@withContext primaryMatches.also(::cacheMatchSources)
            }
            val streamedMatches = executeRequest("https://streamed.pk/api/matches/live", "https://streamed.pk/")
                .let { parseMatchesResponse(it, "LIVE_ENDPOINT") }
                .filter { it.status == "LIVE" }
            if (streamedMatches.isEmpty()) Log.w("AkSportsLiveProvider", "No live matches returned by the configured sports feeds")
            streamedMatches.also(::cacheMatchSources)
        } catch (e: Exception) {
            Log.e("AkSportsLiveProvider", "Error fetching live matches: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAllMatches(): List<SportMatch> = withContext(Dispatchers.IO) {
        try {
            val primaryMatches = fetchWithFallback("/api/v1/streams")
                .let { parseMatchesResponse(it, "UPCOMING") }
                .filterNot { it.status == "FINISHED" }
            if (primaryMatches.isNotEmpty()) return@withContext primaryMatches.also(::cacheMatchSources)
            executeRequest("https://streamed.pk/api/matches/all", "https://streamed.pk/")
                .let { parseMatchesResponse(it, "UPCOMING") }
                .filterNot { it.status == "FINISHED" }
                .also(::cacheMatchSources)
        } catch (e: Exception) {
            Log.e("AkSportsLiveProvider", "Error fetching all matches: ${e.message}")
            emptyList()
        }
    }

    suspend fun getSportsCategories(): List<SportCategory> = withContext(Dispatchers.IO) {
        val hosts = (listOf(activeHost) + FALLBACK_HOSTS).distinct()
        var body = ""
        for (host in hosts) {
            for (path in listOf("/api/v1/categories", "/api/sports")) {
                try {
                    val url = if (host.endsWith("/")) host + path.removePrefix("/") else "$host$path"
                    val result = executeRequest(url)
                    if (result.isNotBlank()) {
                        body = result
                        break
                    }
                } catch (e: Exception) {}
            }
            if (body.isNotBlank()) break
        }

        val cats = mutableListOf<SportCategory>()
        if (body.isNotBlank()) {
            try {
                val trimmed = body.trim()
                val jsonArray = if (trimmed.startsWith("{")) {
                    val obj = JSONObject(trimmed)
                    obj.optJSONArray("categories") ?: obj.optJSONArray("data") ?: JSONArray()
                } else if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else {
                    JSONArray()
                }
                for (i in 0 until jsonArray.length()) {
                    try {
                        val item = jsonArray.get(i)
                        val name = when (item) {
                            is JSONObject -> item.optString("name").ifBlank { item.optString("id") }
                            is String -> item
                            else -> item.toString()
                        }.replaceFirst("psp_", "").trim()
                        if (name.isBlank()) continue

                        val logo = "$activeHost/api/images/categories/$name.png"
                        cats.add(
                            SportCategory(
                                id = "ak-cat-$name",
                                title = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                poster = logo
                            )
                        )
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {}
        }

        if (cats.isEmpty()) {
            val defaultCategories = listOf("soccer", "basketball", "hockey", "combat", "baseball", "football", "racing", "tennis", "cricket")
            for (name in defaultCategories) {
                val logo = "$activeHost/api/images/categories/$name.png"
                cats.add(
                    SportCategory(
                        id = "ak-cat-$name",
                        title = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        poster = logo
                    )
                )
            }
        }
        cats
    }

    suspend fun getStreams(id: String): List<SportStream> = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext emptyList()
        var match = persistedMatches[id]
        if (match?.sources.isNullOrEmpty()) {
            match = (getLiveMatches() + getAllMatches()).firstOrNull { it.id == id }
        }
        val selections = match?.sources.orEmpty().mapNotNull(::resolverSelectionFor)
            .distinctBy { it.source to it.id }
            .ifEmpty { listOf(ResolverSelection("embed", id)) }

        coroutineScope {
            selections.map { selection ->
                async { getResolverStreams(selection) }
            }.awaitAll().flatten().distinctBy { it.id }
        }
    }

    suspend fun resolveStream(source: String, id: String, streamNo: Int): ResolvedSportStream = withContext(Dispatchers.IO) {
        if (source.isBlank() || id.isBlank() || streamNo < 1) {
            throw IOException("This stream is missing its resolver details")
        }
        val body = JSONObject()
            .put("source", source)
            .put("id", id)
            .put("stream", streamNo)
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("$STREAM_RESOLVER_URL/api/resolve")
            .post(body)
            .header("Accept", "application/json")
            .build()
        val responseBody = resolverClient.newCall(request).execute().use { response ->
            val content = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Resolver returned HTTP ${response.code}: $content")
            content
        }
        val result = JSONObject(responseBody)
        if (!result.optBoolean("ok")) {
            val stage = result.optString("stage").takeIf(String::isNotBlank)
            val error = result.optString("error").ifBlank { "The resolver could not open this stream" }
            throw IOException(if (stage == null) error else "Resolver $stage failed: $error")
        }
        val relay = result.optString("relay")
        if (relay.isBlank()) throw IOException("Resolver response did not include a playable stream")
        ResolvedSportStream(
            url = relay,
            headers = mapOf("Referer" to result.optString("referer").ifBlank { "$STREAM_RESOLVER_URL/" })
        )
    }

    private fun resolverSelectionFor(source: MatchSource): ResolverSelection? {
        val resolverSource = source.resolverSource?.takeIf(String::isNotBlank)
        val resolverId = source.resolverId?.takeIf(String::isNotBlank)
        if (resolverSource != null && resolverId != null) return ResolverSelection(resolverSource, resolverId)

        val url = source.url
        if (!url.startsWith("http", ignoreCase = true)) return null
        val segments = runCatching { Uri.parse(url).pathSegments }.getOrNull().orEmpty()
        if (segments.size < 3 || segments[0] != "embed") return null
        val id = segments.last()
            .replace(Regex("(?:1080p|720p|540p|480p|360p)$", RegexOption.IGNORE_CASE), "")
            .takeIf(String::isNotBlank)
            ?: return null
        return ResolverSelection("embed", id)
    }

    private fun computeMatchStatus(
        rawStatus: String,
        fallbackStatus: String,
        hasLiveSources: Boolean,
        matchDate: Long,
        now: Long
    ): String {
        val normalizedStatus = rawStatus.trim().uppercase(Locale.ROOT)
            .replace(Regex("[^A-Z0-9]+"), "_")
            .trim('_')

        return when {
            normalizedStatus in FINISHED_STATUSES -> "FINISHED"
            fallbackStatus == "LIVE_ENDPOINT" -> "LIVE"
            normalizedStatus in LIVE_STATUSES ||
                normalizedStatus.startsWith("LIVE_") ||
                normalizedStatus.startsWith("IN_PLAY_") ||
                normalizedStatus.startsWith("IN_PROGRESS_") ||
                hasLiveSources -> "LIVE"
            matchDate > 0 && now < matchDate - LIVE_WINDOW_BEFORE_START_MS -> "UPCOMING"
            matchDate > 0 && now <= matchDate + LIVE_WINDOW_AFTER_START_MS -> "LIVE"
            matchDate > 0 -> "FINISHED"
            normalizedStatus in UPCOMING_STATUSES -> "UPCOMING"
            fallbackStatus == "LIVE" -> "LIVE"
            else -> "UPCOMING"
        }
    }

    private suspend fun getResolverStreams(selection: ResolverSelection): List<SportStream> = withContext(Dispatchers.IO) {
        val cacheKey = "${selection.source}\u0000${selection.id}"
        val now = System.currentTimeMillis()
        resolverStreamCache[cacheKey]
            ?.takeIf { now - it.cachedAtMs < RESOLVER_STREAM_CACHE_TTL_MS }
            ?.let { return@withContext it.streams }

        resolverStreamLocks.computeIfAbsent(cacheKey) { Mutex() }.withLock {
            val checkedAt = System.currentTimeMillis()
            resolverStreamCache[cacheKey]
                ?.takeIf { checkedAt - it.cachedAtMs < RESOLVER_STREAM_CACHE_TTL_MS }
                ?.let { return@withLock it.streams }

            val url = Uri.parse("$STREAM_RESOLVER_URL/api/streams")
                .buildUpon()
                .appendQueryParameter("source", selection.source)
                .appendQueryParameter("id", selection.id)
                .build()
            val request = Request.Builder().url(url.toString()).header("Accept", "application/json").get().build()
            val responseBody = resolverClient.newCall(request).execute().use { response ->
                val content = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw IOException("Resolver returned HTTP ${response.code}: $content")
                content
            }
            val root = runCatching { JSONObject(responseBody) }.getOrNull()
            val streamsArray = root?.optJSONArray("streams")
                ?: responseBody.trim().takeIf { it.startsWith("[") }?.let(::JSONArray)
                ?: throw IOException("Resolver response did not contain a streams list")
            val streams = buildList {
                for (index in 0 until streamsArray.length()) {
                    val item = streamsArray.optJSONObject(index) ?: continue
                    val streamId = item.optString("id").takeIf(String::isNotBlank) ?: continue
                    val streamSource = item.optString("source").ifBlank { selection.source }
                    add(
                        SportStream(
                            id = streamId,
                            streamNo = item.optInt("streamNo", index + 1),
                            language = item.optString("language").ifBlank { "Unknown" },
                            hd = item.optBoolean("hd", true),
                            embedUrl = item.optString("embedUrl"),
                            source = streamSource,
                            thumbnail = item.optString("thumbnail").takeIf(String::isNotBlank),
                            resolverSource = streamSource,
                            resolverId = streamId
                        )
                    )
                }
            }
            resolverStreamCache[cacheKey] = ResolverStreamCacheEntry(streams, System.currentTimeMillis())
            streams
        }
    }

    private fun fetchWithFallback(vararg paths: String): String {
        val hosts = (listOf(activeHost) + FALLBACK_HOSTS).distinct()
        for (host in hosts) {
            for (path in paths) {
                repeat(2) {
                    try {
                        val url = if (host.endsWith("/")) host + path.removePrefix("/") else "$host$path"
                        val result = executeRequest(url)
                        if (result.isNotBlank()) {
                            if (activeHost != host) {
                                activeHost = host
                            }
                            return result
                        }
                    } catch (e: Exception) {
                        Log.w("AkSportsLiveProvider", "fetchWithFallback error for $host: ${e.message}")
                    }
                }
            }
        }
        return ""
    }

    private fun executeRequest(url: String, referer: String? = null): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache")
            .header("Referer", referer ?: if (activeHost.endsWith("/")) activeHost else "$activeHost/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
    }

    private suspend fun parseMatchesResponse(body: String, status: String): List<SportMatch> {
        if (body.isBlank()) return emptyList()
        return try {
            val trimmed = body.trim()
            val jsonArray = if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("streams")
                    ?: obj.optJSONArray("matches")
                    ?: obj.optJSONArray("data")
                    ?: JSONArray()
            } else if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                JSONArray()
            }
            parseMatches(jsonArray, status)
        } catch (e: Exception) {
            Log.e("AkSportsLiveProvider", "Error parsing matches response: ${e.message}")
            emptyList()
        }
    }

    private suspend fun parseMatches(jsonArray: JSONArray, status: String): List<SportMatch> = coroutineScope {
        val deferredMatches = (0 until jsonArray.length()).map { i ->
            async {
                val obj = jsonArray.getJSONObject(i)
                val matchId = obj.optString("stream_key").ifBlank { obj.optString("id") }
                val title = obj.optString("name").ifBlank { obj.optString("title") }
                val category = obj.optString("category").ifBlank { obj.optString("sport") }
                val league = obj.optString("league").ifBlank { category }

                val thumbnail = obj.optString("thumbnail_url")
                    .ifBlank { obj.optString("thumbnail") }
                    .ifBlank { obj.optString("poster") }
                    .ifBlank { obj.optString("eventLogo") }

                val poster = when {
                    thumbnail.isNotBlank() -> thumbnail
                    category.isNotBlank() && matchId.isNotBlank() -> "$activeHost/thumbnails/${category}_$matchId"
                    else -> null
                }

                val teams = obj.optJSONObject("teams")
                val homeTeamObj = teams?.optJSONObject("home")
                val awayTeamObj = teams?.optJSONObject("away")
                val homeTeam = homeTeamObj?.optString("name") ?: title.split(" vs ").firstOrNull() ?: title
                val awayTeam = awayTeamObj?.optString("name") ?: title.split(" vs ").lastOrNull() ?: "Opponent"

                var score = "vs"
                val scores = obj.optJSONObject("scores")
                if (scores != null) {
                    val homeScore = scores.optString("home", "")
                    val awayScore = scores.optString("away", "")
                    if (homeScore.isNotBlank() && awayScore.isNotBlank()) {
                        score = "$homeScore - $awayScore"
                    }
                }

                val sources = mutableListOf<MatchSource>()
                val sourcesArray = obj.optJSONArray("sources") ?: obj.optJSONArray("live_sources")
                if (sourcesArray != null && sourcesArray.length() > 0) {
                    for (j in 0 until sourcesArray.length()) {
                        val sObj = sourcesArray.getJSONObject(j)
                        val sourceName = sObj.optString("source")
                        val sourceId = sObj.optString("id")
                        val url = sObj.optString("url").ifBlank { sObj.optString("embedUrl") }.ifBlank { sourceId }
                        if (url.isNotBlank() || (sourceName.isNotBlank() && sourceId.isNotBlank())) {
                            sources.add(
                                MatchSource(
                                    quality = sObj.optString("quality").ifBlank { sourceName.ifBlank { "HD" } },
                                    url = url,
                                    id = "$matchId-$j",
                                    resolverSource = sourceName.takeIf(String::isNotBlank),
                                    resolverId = sourceId.takeIf(String::isNotBlank)
                                )
                            )
                        }
                    }
                }
                if (sources.isEmpty() && matchId.isNotBlank() && category.isNotBlank()) {
                    sources.add(MatchSource("FHD", "$activeHost/embed/$category/${matchId}1080p", "$matchId-1080p"))
                    sources.add(MatchSource("SD", "$activeHost/embed/$category/${matchId}540p", "$matchId-540p"))
                }

                val rawDate = obj.optLong("match_timestamp").takeIf { it > 0 } ?: obj.optLong("date")
                val matchDate = if (rawDate > 0 && rawDate < 1000000000000L) rawDate * 1000 else rawDate

                val now = System.currentTimeMillis()
                val liveSources = obj.optJSONArray("live_sources")
                val hasLiveSources = liveSources != null && liveSources.length() > 0

                val computedStatus = computeMatchStatus(
                    rawStatus = obj.optString("status"),
                    fallbackStatus = status,
                    hasLiveSources = hasLiveSources,
                    matchDate = matchDate,
                    now = now
                )

                val formattedTime = if (computedStatus == "LIVE") {
                    "Live"
                } else if (matchDate > 0) {
                    try {
                        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(matchDate))
                    } catch (e: Exception) {
                        "Upcoming"
                    }
                } else {
                    "Upcoming"
                }

                SportMatch(
                    id = matchId,
                    title = title,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    league = league.uppercase(),
                    status = computedStatus,
                    time = formattedTime,
                    score = score,
                    sport = category,
                    poster = poster,
                    date = matchDate,
                    sources = sources
                )
            }
        }
        deferredMatches.awaitAll()
    }

    private fun cacheMatchSources(matches: List<SportMatch>) {
        matches.forEach { match ->
            if (match.id.isNotBlank() && match.sources.isNotEmpty()) {
                sourceCache[match.id] = match.sources
                persistedMatches[match.id] = match
            }
        }
    }
}
