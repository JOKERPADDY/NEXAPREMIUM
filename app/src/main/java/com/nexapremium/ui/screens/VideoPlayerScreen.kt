package com.nexapremium.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Build
import android.app.PictureInPictureParams
import android.util.Rational
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.nexapremium.ui.components.FocusableButton
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
    coverUrl: String = "",
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val defaultPlaybackSpeed by viewModel.defaultPlaybackSpeed.collectAsState()

    val playerEngine = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PlayerEngineEntryPoint::class.java
        ).playerEngine()
    }
    val player = remember { playerEngine.getOrCreatePlayer(context) }
    val pictureInPictureClick: (() -> Unit)? = remember(context) {
        val activity = context as? Activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) == true
        ) {
            {
                activity.enterPictureInPictureMode(
                    PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .build()
                )
            }
        } else null
    }

    // Lock Orientation to Landscape & Hide System Bars for Immersive Landscape Playback
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val enteredInPortrait = activity?.resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            activity?.requestedOrientation = when {
                originalOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED -> originalOrientation
                enteredInPortrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else -> originalOrientation
            }
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            val stateVal = viewModel.uiState.value
            if (stateVal is PlayerUiState.Success) {
                viewModel.saveProgress(
                    detailPath = detailPath,
                    title = stateVal.playbackData.title ?: "",
                    coverUrl = coverUrl,
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
                            coverUrl = coverUrl,
                            positionMs = pos,
                            durationMs = dur,
                            season = season,
                            episode = episode
                        )
                    },
                    initialPlaybackSpeed = defaultPlaybackSpeed,
                    onPlaybackSpeedChanged = viewModel::setDefaultPlaybackSpeed,
                    onPlaybackEnded = { viewModel.onPlaybackEnded(detailPath, season, episode) },
                    onPictureInPictureClick = pictureInPictureClick
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
                    onBackClick = onBackClick,
                    initialPlaybackSpeed = defaultPlaybackSpeed,
                    onPlaybackSpeedChanged = viewModel::setDefaultPlaybackSpeed,
                    onPlaybackEnded = { viewModel.onPlaybackEnded(detailPath, season, episode) },
                    onPictureInPictureClick = pictureInPictureClick
                )
            }
            is PlayerUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, color = PureWhite)
                    Spacer(modifier = Modifier.height(16.dp))
                    FocusableButton(
                        text = "Retry",
                        onClick = { viewModel.loadPlayback(detailPath, season, episode, offlineOnly) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FocusableButton(text = "Back", onClick = onBackClick, isPrimary = false)
                }
            }
        }
    }
}
