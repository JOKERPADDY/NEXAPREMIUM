package com.nexapremium.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.EntryPointAccessors
import com.nexapremium.data.api.model.MediaSourceDto
import com.nexapremium.player.NexaPlayerView
import com.nexapremium.ui.viewmodel.SportsViewModel

@Composable
fun SportsPlayerScreen(
    matchId: String,
    title: String,
    onBack: () -> Unit,
    viewModel: SportsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val streams by viewModel.streams.collectAsState()
    val selectedStream by viewModel.selectedStream.collectAsState()
    val selectedServerIndex by viewModel.selectedServerIndex.collectAsState()
    val isResolving by viewModel.isResolving.collectAsState()
    val error by viewModel.error.collectAsState()

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
    val player = remember(playerEngine, context) { playerEngine.getOrCreatePlayer(context) }
    val source = remember(selectedStream) {
        selectedStream?.let { resolved ->
            MediaSourceDto(
                id = "sports-live",
                quality = "HD",
                resolutions = "HD",
                format = "HLS",
                url = resolved.url,
                requestHeaders = resolved.headers
            )
        }
    }

    DisposableEffect(playerEngine) {
        onDispose { playerEngine.release() }
    }
    LaunchedEffect(matchId) {
        player.pause()
        player.clearMediaItems()
        viewModel.loadStreams(matchId)
    }
    LaunchedEffect(matchId, source) {
        source?.let { playerEngine.prepareSource(context, "sports/$matchId", it) }
    }

    NexaPlayerView(
        player = player,
        title = title,
        sources = source?.let(::listOf).orEmpty(),
        onBackClick = onBack,
        onQualitySelected = { selectedSource ->
            playerEngine.prepareSource(context, "sports/$matchId", selectedSource, player.currentPosition)
        },
        serverOptions = streams.map { stream ->
            val serverName = stream.broadcast?.takeIf(String::isNotBlank) ?: "Server"
            "$serverName · ${stream.quality} · Stream ${stream.streamNo}"
        },
        selectedServerIndex = selectedServerIndex,
        onServerSelected = viewModel::selectServer,
        isServerResolving = isResolving,
        serverError = error,
        onPlaybackError = viewModel::tryNextServerAfterPlaybackError,
        onRetryPlayback = { viewModel.retry(matchId) }
    )
}
