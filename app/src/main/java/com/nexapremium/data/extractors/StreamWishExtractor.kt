package com.nexapremium.data.extractors

import android.util.Log
import com.nexapremium.data.api.model.ExtractedVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class StreamWishExtractor : Extractor() {
    override val name: String = "StreamWish"
    override val mainUrl: String = "https://streamwish.com"
    override val aliasUrls: List<String> = listOf(
        "https://cdnwish.com",
        "https://playerwish.com",
        "https://hlswish.com",
        "https://swish.com"
    )

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun extract(link: String): ExtractedVideo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(link)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .build()

        val response = client.newCall(request).execute()
        val html = response.body?.string().orEmpty()

        val fileRegex = Regex("""file\s*:\s*["'](https?://[^"']+\.m3u8[^"']*)["']""")
        fileRegex.find(html)?.let {
            return@withContext ExtractedVideo(
                source = it.groupValues[1],
                headers = mapOf("Referer" to mainUrl)
            )
        }

        if (html.contains("eval(function(p,a,c,k,e,d)")) {
            val unpacked = JsUnpacker(html).unpack()
            if (unpacked != null) {
                fileRegex.find(unpacked)?.let {
                    return@withContext ExtractedVideo(
                        source = it.groupValues[1],
                        headers = mapOf("Referer" to mainUrl)
                    )
                }
            }
        }

        Log.w("StreamWishExtractor", "Direct stream not found in $link, returning fallback")
        ExtractedVideo(source = link, headers = mapOf("Referer" to mainUrl))
    }
}
