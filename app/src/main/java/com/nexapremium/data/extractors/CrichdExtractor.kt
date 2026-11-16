package com.nexapremium.data.extractors

import android.util.Log
import com.nexapremium.data.api.model.ExtractedVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class CrichdExtractor : Extractor() {
    override val name: String = "Crichd"
    override val mainUrl: String = "https://crichd.online"
    override val aliasUrls: List<String> = listOf("https://cdn.crichd.com", "https://crichd.tv", "https://crichd.vip")

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun extract(link: String): ExtractedVideo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(link)
            .header("Referer", "https://crichd.online/")
            .header("Origin", "https://crichd.online")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .build()

        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: throw Exception("Empty response from $link")
        val doc = Jsoup.parse(html)

        val m3u8Regex = Regex("""["'](https?://[^"']+\.m3u8[^"']*)["']""")

        // 1. Direct HLS match in script
        m3u8Regex.find(html)?.let {
            return@withContext ExtractedVideo(
                source = it.groupValues[1],
                headers = mapOf("Referer" to "https://crichd.online/", "Origin" to "https://crichd.online")
            )
        }

        // 2. Packed JS unpacker
        if (html.contains("eval(function(p,a,c,k,e,d)")) {
            val unpacked = JsUnpacker(html).unpack()
            if (unpacked != null) {
                m3u8Regex.find(unpacked)?.let {
                    return@withContext ExtractedVideo(
                        source = it.groupValues[1],
                        headers = mapOf("Referer" to "https://crichd.online/", "Origin" to "https://crichd.online")
                    )
                }
            }
        }

        // 3. Iframe inspection
        val iframe = doc.selectFirst("iframe")
        val iframeSrc = iframe?.attr("src")
        if (!iframeSrc.isNullOrBlank() && iframeSrc.startsWith("http") && !iframeSrc.contains("crichd")) {
            val iframeRequest = Request.Builder()
                .url(iframeSrc)
                .header("Referer", "https://crichd.online/")
                .header("Origin", "https://crichd.online")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .build()

            val iframeResponse = client.newCall(iframeRequest).execute()
            val iframeHtml = iframeResponse.body?.string().orEmpty()

            m3u8Regex.find(iframeHtml)?.let {
                return@withContext ExtractedVideo(
                    source = it.groupValues[1],
                    headers = mapOf("Referer" to "https://crichd.online/", "Origin" to "https://crichd.online")
                )
            }

            if (iframeHtml.contains("eval(function(p,a,c,k,e,d)")) {
                val unpacked = JsUnpacker(iframeHtml).unpack()
                if (unpacked != null) {
                    m3u8Regex.find(unpacked)?.let {
                        return@withContext ExtractedVideo(
                            source = it.groupValues[1],
                            headers = mapOf("Referer" to "https://crichd.online/", "Origin" to "https://crichd.online")
                        )
                    }
                }
            }
        }

        Log.w("CrichdExtractor", "Could not resolve stream URL from $link, returning fallback")
        ExtractedVideo(
            source = link,
            headers = mapOf("Referer" to "https://crichd.online/", "Origin" to "https://crichd.online")
        )
    }
}
