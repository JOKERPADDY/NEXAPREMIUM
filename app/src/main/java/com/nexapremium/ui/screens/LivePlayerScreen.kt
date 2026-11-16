package com.nexapremium.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nexapremium.data.api.model.MediaSourceDto
import com.nexapremium.player.NexaPlayerView
import dagger.hilt.android.EntryPointAccessors

@Composable
fun LivePlayerScreen(
    streamUrl: String,
    channelName: String,
    headersJson: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val headersMap = remember(headersJson) {
        if (headersJson.isNotBlank()) {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                Gson().fromJson<Map<String, String>>(headersJson, type) ?: emptyMap()
            } catch (_: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val prior = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val enteredInPortrait = activity?.resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            activity?.requestedOrientation = if (
                prior == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED && enteredInPortrait
            ) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else prior
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val playerEngine = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PlayerEngineEntryPoint::class.java
        ).playerEngine()
    }

    val player = remember(playerEngine, context) {
        playerEngine.getOrCreatePlayer(context)
    }

    val source = remember(streamUrl, headersMap) {
        MediaSourceDto(
            id = channelName,
            quality = "LIVE",
            resolutions = "LIVE",
            format = "HLS",
            url = streamUrl,
            requestHeaders = headersMap
        )
    }

    DisposableEffect(playerEngine) {
        onDispose {
            playerEngine.release()
        }
    }

    LaunchedEffect(streamUrl) {
        player.pause()
        player.clearMediaItems()
        playerEngine.prepareSource(context, "sports/cdn-live", source)
    }

    NexaPlayerView(
        player = player,
        title = channelName,
        sources = listOf(source),
        onBackClick = onBack
    )
}
