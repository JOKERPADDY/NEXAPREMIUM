package com.nexapremium.data.extractors

import android.util.Log
import com.nexapremium.data.api.model.ExtractedVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class GenericPackedExtractor : Extractor() {
    override val name: String = "GenericPackedSource"
    override val mainUrl: String = ""

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun extract(link: String): ExtractedVideo = withContext(Dispatchers.IO) {
        if (link.endsWith(".m3u8") || link.contains(".m3u8?")) {
            return@withContext ExtractedVideo(source = link)
        }

        val request = Request.Builder()
            .url(link)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .build()

        try {
            val response = client.newCall(request).execute()
            val html = response.body?.string().orEmpty()

            val m3u8Regex = Regex("""["'](https?://[^"']+\.m3u8[^"']*)["']""")
            m3u8Regex.find(html)?.let {
                return@withContext ExtractedVideo(source = it.groupValues[1])
            }

            if (html.contains("eval(function(p,a,c,k,e,d)")) {
                val unpacked = JsUnpacker(html).unpack()
                if (unpacked != null) {
                    m3u8Regex.find(unpacked)?.let {
                        return@withContext ExtractedVideo(source = it.groupValues[1])
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GenericPackedExtractor", "Failed extracting $link: ${e.message}")
        }

        ExtractedVideo(source = link)
    }
}
