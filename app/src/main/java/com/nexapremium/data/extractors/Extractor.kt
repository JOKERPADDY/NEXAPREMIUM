package com.nexapremium.data.extractors

import android.util.Log
import com.nexapremium.data.api.model.ExtractedVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class Extractor {
    abstract val name: String
    abstract val mainUrl: String
    open val aliasUrls: List<String> = emptyList()

    abstract suspend fun extract(link: String): ExtractedVideo

    companion object {
        private val extractors by lazy {
            listOf(
                CrichdExtractor(),
                StreamWishExtractor(),
                GenericPackedExtractor()
            )
        }

        suspend fun extract(link: String): ExtractedVideo = withContext(Dispatchers.IO) {
            val matchingExtractor = extractors.find { extractor ->
                link.contains(extractor.mainUrl, ignoreCase = true) ||
                        extractor.aliasUrls.any { alias -> link.contains(alias, ignoreCase = true) }
            }

            if (matchingExtractor != null) {
                try {
                    return@withContext matchingExtractor.extract(link)
                } catch (e: Exception) {
                    Log.e("ExtractorRegistry", "Error in ${matchingExtractor.name} extractor: ${e.message}")
                }
            }

            // Fallback to GenericPackedExtractor
            try {
                GenericPackedExtractor().extract(link)
            } catch (e: Exception) {
                Log.e("ExtractorRegistry", "Generic fallback failed for $link: ${e.message}")
                ExtractedVideo(source = link)
            }
        }
    }
}
