package com.nexapremium.data.provider

import android.util.Base64
import android.util.Log
import com.nexapremium.data.api.model.CDNChannel
import com.nexapremium.data.api.model.CDNChannelsResponse
import com.nexapremium.data.api.model.ExtractedVideo
import com.nexapremium.data.extractors.JsUnpacker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CdnLiveTvProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val BASE_URL = "https://api.cdnlivetv.tv/api/v1/"
        private const val USER = "cdnlivetv"
        private const val PLAN = "free"
        private const val REFERER_URL = "https://cdnlivetv.tv/"
        private const val ORIGIN_URL = "https://cdnlivetv.tv"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        private const val CACHE_EXPIRY_MS = 5 * 60 * 1000L
    }

    private val client: OkHttpClient = okHttpClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val service: Service = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(Service::class.java)

    private var cachedChannels: List<CDNChannel> = emptyList()
    private var lastFetchTime: Long = 0L

    interface Service {
        @GET("channels/")
        suspend fun getChannels(
            @Query("user") user: String = USER,
            @Query("plan") plan: String = PLAN
        ): CDNChannelsResponse
    }

    suspend fun getChannels(forceRefresh: Boolean = false): List<CDNChannel> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedChannels.isNotEmpty() && (now - lastFetchTime < CACHE_EXPIRY_MS)) {
            return@withContext cachedChannels
        }

        try {
            val response = service.getChannels()
            if (response.channels.isNotEmpty()) {
                cachedChannels = response.channels
                lastFetchTime = now
            }
            cachedChannels
        } catch (e: Exception) {
            Log.e("CdnLiveTvProvider", "Error fetching CDN channels: ${e.message}")
            cachedChannels
        }
    }

    suspend fun resolveStreamUrl(channel: CDNChannel): ExtractedVideo = withContext(Dispatchers.IO) {
        var playerUrl = channel.url.takeIf { it.isNotBlank() } ?: "https://cdnlivetv.tv/channels/player/${channel.code}"

        if (playerUrl.endsWith(".m3u8") || playerUrl.contains(".m3u8?")) {
            return@withContext ExtractedVideo(
                source = playerUrl,
                headers = defaultHeaders()
            )
        }

        try {
            val request = Request.Builder()
                .url(playerUrl)
                .header("Referer", REFERER_URL)
                .header("Origin", ORIGIN_URL)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string().orEmpty()

            val m3u8Regex = Regex("""["'](https?[:\\]+[^"']+\.m3u8[^"']*)["']""")
            val fileRegex = Regex("""file\s*[:\s]+["']([^"']+)["']""")
            val sourceRegex = Regex("""source\s*[:\s]+["']([^"']+)["']""")

            var streamUrl = m3u8Regex.find(html)?.groupValues?.get(1)
                ?: fileRegex.find(html)?.groupValues?.get(1)
                ?: sourceRegex.find(html)?.groupValues?.get(1)

            // 1. Try atob Base64 patterns
            if (streamUrl == null) {
                Regex("""atob\s*\(\s*["']([^"']+)["']\s*\)""").findAll(html).forEach { match ->
                    try {
                        val decoded = String(Base64.decode(match.groupValues[1], Base64.DEFAULT))
                        if (decoded.contains(".m3u8")) {
                            streamUrl = decoded
                        }
                    } catch (_: Exception) {}
                }
            }

            // 2. Try iframe inspection
            if (streamUrl == null) {
                val doc = Jsoup.parse(html)
                for (iframe in doc.select("iframe")) {
                    val src = iframe.attr("src")
                    if (src.isNotEmpty() && src.startsWith("http")) {
                        try {
                            val iframeReq = Request.Builder().url(src).header("Referer", playerUrl).build()
                            val iframeHtml = client.newCall(iframeReq).execute().body?.string().orEmpty()
                            val foundInIframe = m3u8Regex.find(iframeHtml)?.groupValues?.get(1)
                                ?: extractObfuscatedStreamUrl(iframeHtml)
                            if (foundInIframe != null) {
                                streamUrl = foundInIframe
                                break
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            // 3. Try JS Unpacker
            if (streamUrl == null && html.contains("eval(function(p,a,c,k,e,d)")) {
                val unpacked = JsUnpacker(html).unpack()
                if (unpacked != null) {
                    streamUrl = m3u8Regex.find(unpacked)?.groupValues?.get(1)
                        ?: fileRegex.find(unpacked)?.groupValues?.get(1)
                }
            }

            // 4. Try custom obfuscated stream URL
            if (streamUrl == null) {
                streamUrl = extractObfuscatedStreamUrl(html)
            }

            val currentUrl = streamUrl
            if (currentUrl != null) {
                val finalStreamUrl = currentUrl.replace("\\/", "/")
                    .let { if (it.startsWith("//")) "https:$it" else it }

                return@withContext ExtractedVideo(
                    source = finalStreamUrl,
                    headers = defaultHeaders()
                )
            }
        } catch (e: Exception) {
            Log.e("CdnLiveTvProvider", "Error resolving stream for channel ${channel.name}: ${e.message}")
        }

        ExtractedVideo(
            source = playerUrl,
            headers = defaultHeaders()
        )
    }

    private fun extractObfuscatedStreamUrl(html: String): String? {
        try {
            val funcNamePattern = Regex("""function\s+([a-zA-Z0-9_]+)\s*\(\s*[a-z]\s*\)\s*\{\s*[a-z]\s*=\s*[a-z]\.replace\(""")
            val funcNameMatch = funcNamePattern.find(html) ?: return null
            val funcName = funcNameMatch.groupValues[1]

            val varAssignments = mutableMapOf<String, String>()
            val varPattern = Regex("""var\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*["']([^"']*)["']""")
            for (match in varPattern.findAll(html)) {
                val varName = match.groupValues[1]
                val varValue = match.groupValues[2]
                varAssignments[varName] = varValue
            }

            val urlPattern = Regex("""var\s+[a-zA-Z_][a-zA-Z0-9_]*\s*=\s*((?:$funcName\s*\(\s*[a-zA-Z_][a-zA-Z0-9_]*\s*\)\s*(?:\+\s*)?)+)""")
            val urlMatch = urlPattern.find(html) ?: return null

            val callSequence = urlMatch.groupValues[1]
            val functionCalls = callSequence.split("+").map { it.trim() }

            val decodedParts = StringBuilder()
            val callPattern = Regex("""$funcName\s*\(\s*([a-zA-Z_][a-zA-Z0-9_]*)\s*\)""")

            for (call in functionCalls) {
                val varMatch = callPattern.find(call)
                if (varMatch != null) {
                    val varName = varMatch.groupValues[1]
                    val varValue = varAssignments[varName]
                    if (varValue != null) {
                        decodedParts.append(obfuscatedBase64Decode(varValue))
                    }
                }
            }

            return decodedParts.toString().takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e("CdnLiveTvProvider", "Error extracting obfuscated stream URL: ${e.message}")
            return null
        }
    }

    private fun obfuscatedBase64Decode(input: String): String {
        return try {
            var s = input.replace("-", "+").replace("_", "/")
            while (s.length % 4 != 0) s += "="
            val decodedBytes = Base64.decode(s, Base64.DEFAULT)
            String(decodedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            input
        }
    }

    private fun defaultHeaders(): Map<String, String> = mapOf(
        "Referer" to REFERER_URL,
        "Origin" to ORIGIN_URL,
        "User-Agent" to USER_AGENT
    )
}
