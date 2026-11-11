package com.nexapremium.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.nexapremium.core.ui.theme.PrimaryRed
import com.nexapremium.core.ui.theme.PureBlack
import com.nexapremium.core.ui.theme.PureWhite
import com.nexapremium.player.NexaPlayerEngine
import com.nexapremium.player.NexaPlayerView
import com.nexapremium.ui.viewmodel.PlayerUiState
import com.nexapremium.ui.viewmodel.PlayerViewModel

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlayerEngineEntryPoint {
    fun playerEngine(): NexaPlayerEngine
}

@Composable
fun VideoPlayerScreen(
    detailPath: String,
    season: Int = 0,
    episode: Int = 0,
    offlineOnly: Boolean = false,
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val playerEngine = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PlayerEngineEntryPoint::class.java
        ).playerEngine()
    }
    val player = remember { playerEngine.getOrCreatePlayer(context) }

    // Lock Orientation to Landscape & Hide System Bars for Immersive Landscape Playback
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            activity?.requestedOrientation = originalOrientation
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            val stateVal = viewModel.uiState.value
            if (stateVal is PlayerUiState.Success) {
                    viewModel.saveProgress(
                    detailPath = detailPath,
                    title = stateVal.playbackData.title ?: "",
                    coverUrl = "",
                    positionMs = player.currentPosition,
                    durationMs = player.duration.coerceAtLeast(1L),
                    season = season,
                    episode = episode
                )
            }
            playerEngine.release()
        }
    }

    LaunchedEffect(detailPath, season, episode) {
        viewModel.loadPlayback(detailPath, season, episode, offlineOnly)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                CircularProgressIndicator(
                    color = PrimaryRed,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is PlayerUiState.Success -> {
                val sources = state.playbackData.playableSources.ifEmpty { state.playbackData.effectiveSources }
                val title = state.playbackData.title ?: "Playing Title"

                LaunchedEffect(sources, state.resumePositionMs) {
                    if (sources.isNotEmpty()) {
                        playerEngine.prepareMedia(context, detailPath, sources, state.resumePositionMs)
                    }
                }

                NexaPlayerView(
                    player = player,
                    title = title,
                    sources = sources,
                    seasonNumber = season,
                    episodeNumber = episode,
                    captions = state.captions,
                    onBackClick = onBackClick,
                    onQualitySelected = { selectedSource ->
                        val pos = player.currentPosition
                        playerEngine.prepareSource(context, detailPath, selectedSource, pos)
                    },
                    onProgressChanged = { pos, dur ->
                        viewModel.saveProgress(
                            detailPath = detailPath,
                            title = title,
                            coverUrl = "",
                            positionMs = pos,
                            durationMs = dur,
                            season = season,
                            episode = episode
                        )
                    }
                )
            }
            is PlayerUiState.Offline -> {
                val download = state.download
                LaunchedEffect(download.filePath) {
                    player.setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(download.filePath))))
                    player.prepare()
                    player.playWhenReady = true
                }
                NexaPlayerView(
                    player = player,
                    title = download.title,
                    onBackClick = onBackClick
                )
            }
            is PlayerUiState.Error -> {
                Text(
                    text = state.message,
                    color = PureWhite,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
