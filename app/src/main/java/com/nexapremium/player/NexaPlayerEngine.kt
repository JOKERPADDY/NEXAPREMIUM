package com.nexapremium.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.nexapremium.data.api.NexaHeaderInterceptor
import com.nexapremium.data.api.model.MediaSourceDto
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NexaPlayerEngine @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private var exoPlayer: ExoPlayer? = null

    /**
     * Specialized OkHttpClient instance for media streaming.
     * Strips NexaHeaderInterceptor which injects "Accept: application/json",
     * allowing video CDNs to serve binary MP4 streams without HTTP 406/403 errors.
     */
    private val mediaOkHttpClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .apply {
                interceptors().removeAll { it is NexaHeaderInterceptor }
            }
            .build()
    }

    @OptIn(UnstableApi::class)
    fun getOrCreatePlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
        }
        return exoPlayer!!
    }

    @OptIn(UnstableApi::class)
    fun prepareMedia(
        context: Context,
        detailPath: String,
        sources: List<MediaSourceDto>,
        startPositionMs: Long = 0L
    ): String? {
        // Filter out DASH manifests (which return 403 Forbidden on CDN) and select valid MP4 direct sources
        val mp4Sources = sources.filter { source ->
            val isDash = source.format.equals("DASH", ignoreCase = true) || source.url?.endsWith(".mpd") == true
            !isDash && !source.url.isNullOrBlank()
        }

        // Prefer higher resolution MP4 sources by default (e.g. 720P > 480P > 360P)
        val selectedSource = mp4Sources.maxByOrNull { source ->
            source.resolutions?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        } ?: mp4Sources.firstOrNull() ?: sources.firstOrNull()

        if (selectedSource != null) {
            prepareSource(context, detailPath, selectedSource, startPositionMs)
        }
        return selectedSource?.effectiveQuality
    }

    @OptIn(UnstableApi::class)
    fun prepareSource(
        context: Context,
        detailPath: String,
        source: MediaSourceDto,
        startPositionMs: Long = 0L
    ) {
        val player = getOrCreatePlayer(context)
        val mediaUrl = source.url ?: return

        val cleanPath = detailPath.trimStart('/')
        val refererPath = if (cleanPath.startsWith("movies/") || cleanPath.startsWith("series/")) cleanPath else "movies/$cleanPath"

        val headers = mutableMapOf(
            "User-Agent" to "okhttp/4.11.0",
            "Accept" to "*/*",
            "Referer" to "https://themoviebox.xyz/$refererPath",
            "Origin" to "https://themoviebox.xyz"
        )
        source.effectiveHeaders.let { headers.putAll(it) }

        // Guarantee User-Agent header is okhttp/4.11.0 as required by CDN bcdnxw.hakunaymatata.com
        if (!headers.containsKey("User-Agent") || headers["User-Agent"].isNullOrBlank()) {
            headers["User-Agent"] = "okhttp/4.11.0"
        }

        val dataSourceFactory = OkHttpDataSource.Factory(mediaOkHttpClient)
            .setDefaultRequestProperties(headers)

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(mediaUrl))

        player.setMediaSource(mediaSource)
        player.prepare()
        if (startPositionMs > 0) {
            player.seekTo(startPositionMs)
        }
        player.playWhenReady = true
    }

    fun release() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
    }
}
