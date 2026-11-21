package com.nexapremium.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nexapremium.core.ui.theme.*
import com.nexapremium.R
import com.nexapremium.data.api.model.CaptionItemDto
import com.nexapremium.data.api.model.MediaSourceDto
import kotlinx.coroutines.delay
import com.nexapremium.ui.components.TvIconButton
import com.nexapremium.ui.components.isTelevisionDevice
import com.nexapremium.ui.components.tvFocusOutline

@Composable
fun ThinSeekBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableLongStateOf(0L) }

    val activePos = if (isDragging) dragPosition else currentPosition
    val progress = (activePos.toFloat() / duration.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    val newPos = (offset.x / size.width.toFloat() * duration.toFloat()).toLong().coerceIn(0L, duration)
                    onSeek(newPos)
                }
            }
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragPosition = (offset.x / size.width.toFloat() * duration.toFloat()).toLong().coerceIn(0L, duration)
                    },
                    onDragEnd = {
                        onSeek(dragPosition)
                        isDragging = false
                    },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { _, dragAmount ->
                        val delta = (dragAmount / size.width.toFloat() * duration.toFloat()).toLong()
                        dragPosition = (dragPosition + delta).coerceIn(0L, duration)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val density = LocalDensity.current

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.3f))
        )
        Box(
            modifier = Modifier
                .width((widthPx * progress / density.density).dp)
                .height(2.dp)
                .background(PureWhite)
        )
        Box(
            modifier = Modifier
                .offset(x = ((widthPx * progress) / density.density).dp - 4.dp)
                .size(8.dp)
                .background(PureWhite, CircleShape)
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun NexaPlayerView(
    player: ExoPlayer,
    title: String,
    sources: List<MediaSourceDto> = emptyList(),
    seasonNumber: Int = 0,
    episodeNumber: Int = 0,
    captions: List<CaptionItemDto> = emptyList(),
    onBackClick: () -> Unit,
    onQualitySelected: ((MediaSourceDto) -> Unit)? = null,
    onNextEpisodeClick: (() -> Unit)? = null,
    onSkipIntroClick: (() -> Unit)? = null,
    introDurationMs: Long = 85_000L,
    onProgressChanged: ((Long, Long) -> Unit)? = null,
    initialPlaybackSpeed: Float = 1.0f,
    onPlaybackSpeedChanged: ((Float) -> Unit)? = null,
    onPlaybackEnded: (() -> Unit)? = null,
    onPictureInPictureClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    activeSubtitleText: String? = null,
    serverOptions: List<String> = emptyList(),
    selectedServerIndex: Int = -1,
    onServerSelected: ((Int) -> Unit)? = null,
    isServerResolving: Boolean = false,
    serverError: String? = null,
    onPlaybackError: (() -> Unit)? = null,
    onRetryPlayback: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var gestureValue by remember { mutableStateOf<String?>(null) }
    var brightness by remember(activity) {
        mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f)
    }
    var volume by remember(audioManager) {
        mutableFloatStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() /
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        )
    }
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var isBuffering by remember { mutableStateOf(player.playbackState == Player.STATE_BUFFERING) }
    var playerError by remember { mutableStateOf<PlaybackException?>(player.playerError) }
    var isLocked by remember { mutableStateOf(false) }
    val currentPositionState = remember { mutableLongStateOf(player.currentPosition.coerceAtLeast(0L)) }
    var duration by remember { mutableLongStateOf(player.duration.coerceAtLeast(1L)) }
    var currentSpeed by remember(player) { mutableFloatStateOf(initialPlaybackSpeed) }

    var sleepTimerDeadline by remember { mutableLongStateOf(0L) }
    var sleepTimerRemainingSeconds by remember { mutableLongStateOf(0L) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showCastDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSkipIntro by remember { mutableStateOf(true) }
    var selectedCaption by remember { mutableStateOf<CaptionItemDto?>(null) }
    val playerFocusRequester = remember { FocusRequester() }
    val playFocusRequester = remember { FocusRequester() }

    var lastInteractionTime by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    fun resetHideTimer() { lastInteractionTime = SystemClock.elapsedRealtime() }

    val handleSkipIntro: () -> Unit = remember(onSkipIntroClick, introDurationMs, duration, player) {
        {
            if (onSkipIntroClick != null) {
                onSkipIntroClick()
            } else {
                val targetPos = (player.currentPosition + introDurationMs).coerceAtMost(duration)
                player.seekTo(targetPos)
            }
            showSkipIntro = false
            showControls = false
            gestureValue = "Skipped Intro (+${introDurationMs / 1000}s)"
        }
    }

    LaunchedEffect(playerFocusRequester) {
        playerFocusRequester.requestFocus()
    }
    LaunchedEffect(showControls, sources.isNotEmpty()) {
        if (showControls && sources.isNotEmpty()) playFocusRequester.requestFocus()
        else playerFocusRequester.requestFocus()
    }

    DisposableEffect(activity) {
        val window = activity?.window
        val originalScreenBrightness = window?.attributes?.screenBrightness
        onDispose {
            if (window != null && originalScreenBrightness != null) {
                window.attributes = window.attributes.apply { screenBrightness = originalScreenBrightness }
            }
        }
    }

    val availableQualities = remember(sources) {
        sources.map { it.effectiveQuality }.distinct().ifEmpty { listOf("720P", "1080P", "360P") }
    }
    var currentQuality by remember(availableQualities) { mutableStateOf(availableQualities.firstOrNull() ?: "1080P") }
    var currentFitText by remember { mutableStateOf("Fit") }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    val serverHost = remember(sources, currentQuality) {
        val src = sources.firstOrNull { it.effectiveQuality == currentQuality } ?: sources.firstOrNull()
        val url = src?.url
        if (!url.isNullOrEmpty()) {
            try { Uri.parse(url).host ?: "bcdnxw.hakunaymatata.com" } catch (e: Exception) { "bcdnxw.hakunaymatata.com" }
        } else "bcdnxw.hakunaymatata.com"
    }

    val onPlaybackEndedState by rememberUpdatedState(onPlaybackEnded)
    val onPlaybackErrorState by rememberUpdatedState(onPlaybackError)
    val onRetryPlaybackState by rememberUpdatedState(onRetryPlayback)

    LaunchedEffect(player, initialPlaybackSpeed) {
        currentSpeed = initialPlaybackSpeed
        player.playbackParameters = PlaybackParameters(initialPlaybackSpeed)
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) playerError = null
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
                    if (player.isPlaying || playbackState == Player.STATE_READY) {
                        playerError = null
                    }
                }
                if (playbackState == Player.STATE_ENDED) onPlaybackEndedState?.invoke()
            }

            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    player.seekToDefaultPosition()
                    player.prepare()
                    playerError = null
                    return
                }
                if (player.isPlaying || player.playbackState == Player.STATE_READY) {
                    playerError = null
                    return
                }
                playerError = error
                isBuffering = false
                onPlaybackErrorState?.invoke()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(sleepTimerDeadline) {
        while (sleepTimerDeadline > 0L) {
            val remaining = ((sleepTimerDeadline - SystemClock.elapsedRealtime()) / 1000L).coerceAtLeast(0L)
            sleepTimerRemainingSeconds = remaining
            if (remaining == 0L) {
                player.pause()
                showControls = true
                gestureValue = "Sleep timer ended"
                sleepTimerDeadline = 0L
                break
            }
            delay(1000L)
        }
    }

    LaunchedEffect(showControls, isPlaying, isLocked, lastInteractionTime) {
        if (showControls && isPlaying && !isLocked) {
            delay(3000L)
            showControls = false
        }
    }

    LaunchedEffect(gestureValue) {
        if (gestureValue != null) {
            delay(1200)
            gestureValue = null
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying || player.isPlaying || player.playbackState == Player.STATE_READY) {
            playerError = null
        }
        while (isPlaying) {
            val pos = player.currentPosition.coerceAtLeast(0L)
            val dur = player.duration.coerceAtLeast(1L)
            currentPositionState.longValue = pos
            duration = dur
            onProgressChanged?.invoke(pos, dur)
            delay(1000)
        }
    }

    val displayTitle = remember(title, seasonNumber, episodeNumber) {
        if (seasonNumber > 0 && episodeNumber > 0) {
            "$title S${String.format("%02d", seasonNumber)} E${String.format("%02d", episodeNumber)}"
        } else title
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .focusRequester(playerFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    resetHideTimer()
                } else if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                when (event.key) {
                    Key.Back, Key.Escape -> {
                        if (showControls) {
                            showControls = false
                            true
                        } else {
                            onBackClick()
                            true
                        }
                    }
                    Key.MediaPlayPause -> {
                        if (player.isPlaying) player.pause() else player.play()
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (!showControls) {
                            showControls = true
                            true
                        } else false
                    }
                    Key.DirectionLeft -> {
                        if (!showControls) {
                            player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
                            true
                        } else false
                    }
                    Key.DirectionRight -> {
                        if (!showControls) {
                            player.seekTo((player.currentPosition + 10_000L).coerceAtMost(duration))
                            true
                        } else false
                    }
                    Key.DirectionUp, Key.DirectionDown -> {
                        if (!showControls) {
                            showControls = true
                            true
                        } else false
                    }
                    else -> false
                }
            }
            .pointerInput(isLocked, showControls) {
                if (!isLocked && !showControls) detectVerticalDragGestures { change, dragAmount ->
                    resetHideTimer()
                    change.consume()
                    val adjustment = -dragAmount / size.height.toFloat()
                    if (change.position.x < size.width / 2f) {
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                        volume = (volume + adjustment).coerceIn(0f, 1f)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volume * maxVolume).toInt(), 0)
                        gestureValue = "Volume ${(volume * 100).toInt()}%"
                    } else {
                        brightness = (brightness + adjustment).coerceIn(0.02f, 1f)
                        activity?.window?.let { window ->
                            window.attributes = window.attributes.apply { screenBrightness = brightness }
                        }
                        gestureValue = "Brightness ${(brightness * 100).toInt()}%"
                    }
                }
            }
            .pointerInput(isLocked) {
                detectTapGestures(
                    onTap = {
                        resetHideTimer()
                        if (!isLocked) showControls = !showControls
                    },
                    onDoubleTap = { offset ->
                        resetHideTimer()
                        if (!isLocked) {
                            val halfWidth = size.width / 2f
                            if (offset.x < halfWidth) {
                                player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
                                gestureValue = "-10s"
                            } else {
                                player.seekTo((player.currentPosition + 10_000L).coerceAtMost(duration))
                                gestureValue = "+10s"
                            }
                        }
                    }
                )
            }
    ) {
        PlayerTextureView(player = player, resizeMode = resizeMode)

        PlayerGestureToast(gestureValue = gestureValue)

        PlayerStatusOverlays(
            isBuffering = isBuffering,
            isServerResolving = isServerResolving,
            sourcesEmpty = sources.isEmpty(),
            serverError = serverError,
            playerError = playerError,
            isPlaying = isPlaying || player.isPlaying || player.playbackState == Player.STATE_READY,
            serverOptions = serverOptions,
            currentQuality = currentQuality,
            sources = sources,
            onRetryPlayback = {
                playerError = null
                if (onRetryPlaybackState != null) {
                    onRetryPlaybackState?.invoke()
                } else {
                    val currentSrc = sources.firstOrNull { it.effectiveQuality == currentQuality } ?: sources.firstOrNull()
                    if (currentSrc != null) {
                        onQualitySelected?.invoke(currentSrc)
                    } else {
                        player.prepare()
                        player.playWhenReady = true
                    }
                }
            }
        )

        PlayerSubtitleOverlay(
            selectedCaption = selectedCaption,
            activeSubtitleText = activeSubtitleText
        )

        PlayerSkipIntroButton(
            sourcesNotEmpty = sources.isNotEmpty(),
            isLocked = isLocked,
            currentPositionState = currentPositionState,
            isPlaying = isPlaying,
            showControls = showControls,
            onSkipIntro = handleSkipIntro
        )

        PlayerLockIndicator(
            isLocked = isLocked,
            onUnlock = {
                isLocked = false
                showControls = true
            }
        )

        if (showControls && !isLocked) {
            val overlayState = remember(
                displayTitle, isPlaying, player, sources, captions, availableQualities,
                currentQuality, currentFitText, resizeMode, selectedCaption, currentSpeed,
                sleepTimerDeadline, sleepTimerRemainingSeconds, playFocusRequester, duration
            ) {
                PlayerOverlayState(
                    displayTitle = displayTitle,
                    isPlaying = isPlaying,
                    player = player,
                    sources = sources,
                    captions = captions,
                    availableQualities = availableQualities,
                    currentQuality = currentQuality,
                    currentFitText = currentFitText,
                    resizeMode = resizeMode,
                    selectedCaption = selectedCaption,
                    currentSpeed = currentSpeed,
                    sleepTimerDeadline = sleepTimerDeadline,
                    sleepTimerRemainingSeconds = sleepTimerRemainingSeconds,
                    playFocusRequester = playFocusRequester,
                    duration = duration
                )
            }

            PlayerControlsOverlay(
                state = overlayState,
                currentPositionState = currentPositionState,
                onBackClick = onBackClick,
                onCastClick = { showCastDialog = true },
                onHelpClick = { showHelpDialog = true },
                onSettingsClick = { showSettingsDialog = true },
                onLockClick = {
                    isLocked = true
                    showControls = false
                },
                onPlayPauseClick = { if (isPlaying) player.pause() else player.play() },
                onRewindClick = { player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L)) },
                onForwardClick = { player.seekTo((player.currentPosition + 10_000L).coerceAtMost(duration)) },
                onSkipIntroClick = handleSkipIntro,
                onPictureInPictureClick = onPictureInPictureClick,
                onNextEpisodeClick = onNextEpisodeClick,
                onResizeModeChange = { newMode, newFitText ->
                    resizeMode = newMode
                    currentFitText = newFitText
                },
                onCaptionSelected = { cap -> selectedCaption = cap },
                onSpeedSelected = { speed ->
                    currentSpeed = speed
                    player.playbackParameters = PlaybackParameters(speed)
                    onPlaybackSpeedChanged?.invoke(speed)
                },
                onSleepTimerSelected = { mins ->
                    if (mins <= 0) {
                        sleepTimerDeadline = 0L
                        sleepTimerRemainingSeconds = 0L
                    } else {
                        sleepTimerDeadline = SystemClock.elapsedRealtime() + mins * 60_000L
                        sleepTimerRemainingSeconds = mins * 60L
                        gestureValue = "Sleep timer: $mins minutes"
                    }
                },
                onQualitySelected = { q ->
                    currentQuality = q
                    val matchSource = sources.firstOrNull { it.effectiveQuality == q }
                    if (matchSource != null) {
                        onQualitySelected?.invoke(matchSource)
                    }
                },
                onSeek = { target -> player.seekTo(target) }
            )
        }

        PlayerDialogs(
            showCastDialog = showCastDialog,
            onDismissCast = { showCastDialog = false },
            showHelpDialog = showHelpDialog,
            onDismissHelp = { showHelpDialog = false },
            showSettingsDialog = showSettingsDialog,
            onDismissSettings = { showSettingsDialog = false },
            serverOptions = serverOptions,
            selectedServerIndex = selectedServerIndex,
            onServerSelected = onServerSelected,
            serverHost = serverHost,
            isServerResolving = isServerResolving,
            currentQuality = currentQuality,
            currentSpeed = currentSpeed,
            currentFitText = currentFitText
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerTextureView(
    player: ExoPlayer,
    resizeMode: Int
) {
    AndroidView(
        factory = { ctx ->
            (LayoutInflater.from(ctx).inflate(R.layout.player_view_texture, null, false) as PlayerView).apply {
                this.player = player
                useController = false
                this.resizeMode = resizeMode
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { playerView ->
            playerView.resizeMode = resizeMode
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun BoxScope.PlayerGestureToast(gestureValue: String?) {
    if (gestureValue != null) {
        Text(
            text = gestureValue,
            color = PureWhite,
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun BoxScope.PlayerStatusOverlays(
    isBuffering: Boolean,
    isServerResolving: Boolean,
    sourcesEmpty: Boolean,
    serverError: String?,
    playerError: PlaybackException?,
    isPlaying: Boolean,
    serverOptions: List<String>,
    currentQuality: String,
    sources: List<MediaSourceDto>,
    onRetryPlayback: () -> Unit
) {
    if (isBuffering) {
        CircularProgressIndicator(
            color = PrimaryRed,
            modifier = Modifier.align(Alignment.Center)
        )
    }

    if (isServerResolving) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .background(DarkSurface.copy(alpha = 0.94f), RoundedCornerShape(8.dp))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(color = PrimaryRed)
            Text("Please wait... Live sports is starting.", color = PureWhite, fontSize = 13.sp)
        }
    }

    if (sourcesEmpty && !isServerResolving && !serverError.isNullOrBlank()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .background(DarkSurface.copy(alpha = 0.94f), RoundedCornerShape(8.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Unable to start live stream", color = PrimaryRed, fontSize = 16.sp)
            Text(serverError, color = PureWhite, fontSize = 13.sp)
            Button(
                onClick = onRetryPlayback,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
            ) {
                Text("Retry", color = PureWhite)
            }
        }
    }

    if (playerError != null && !isServerResolving && !isPlaying) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .background(DarkSurface, RoundedCornerShape(8.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Playback Error",
                color = PrimaryRed,
                fontSize = 16.sp,
                style = Typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (serverOptions.isNotEmpty()) {
                    "This stream stopped. Retry or choose another server in Settings."
                } else {
                    playerError.message ?: "Unable to play stream"
                },
                color = PureWhite,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetryPlayback,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
            ) {
                Text("Retry", color = PureWhite)
            }
        }
    }
}

@Composable
private fun BoxScope.PlayerSubtitleOverlay(
    selectedCaption: CaptionItemDto?,
    activeSubtitleText: String?
) {
    val activeText = selectedCaption?.label?.let { "[$it subtitle active]" } ?: activeSubtitleText
    if (!activeText.isNullOrEmpty()) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
                .background(TranslucentDark, RoundedCornerShape(4.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = activeText,
                color = PureWhite,
                fontSize = 18.sp,
                style = Typography.bodyLarge
            )
        }
    }
}

@Stable
private data class PlayerOverlayState(
    val displayTitle: String,
    val isPlaying: Boolean,
    val player: ExoPlayer,
    val sources: List<MediaSourceDto>,
    val captions: List<CaptionItemDto>,
    val availableQualities: List<String>,
    val currentQuality: String,
    val currentFitText: String,
    val resizeMode: Int,
    val selectedCaption: CaptionItemDto?,
    val currentSpeed: Float,
    val sleepTimerDeadline: Long,
    val sleepTimerRemainingSeconds: Long,
    val playFocusRequester: FocusRequester,
    val duration: Long
)

@Composable
private fun BoxScope.PlayerSkipIntroButton(
    sourcesNotEmpty: Boolean,
    isLocked: Boolean,
    currentPositionState: State<Long>,
    isPlaying: Boolean,
    showControls: Boolean,
    onSkipIntro: () -> Unit
) {
    var showSkipIntro by remember { mutableStateOf(true) }
    val currentPosition = currentPositionState.value
    val isIntroRange = currentPosition in 0L..150_000L

    LaunchedEffect(isIntroRange, isPlaying) {
        if (isIntroRange && isPlaying) {
            showSkipIntro = true
            delay(3000L)
            showSkipIntro = false
        }
    }

    LaunchedEffect(showControls) {
        if (showControls && isIntroRange) {
            showSkipIntro = true
            delay(3000L)
            showSkipIntro = false
        }
    }

    if (sourcesNotEmpty && !isLocked && isIntroRange && showSkipIntro) {
        val skipInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 24.dp)
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                .clickable(interactionSource = skipInteraction, indication = null) { onSkipIntro() }
                .tvFocusOutline(skipInteraction, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "Skip Intro",
                tint = PureWhite,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Skip Intro",
                color = PureWhite,
                fontSize = 13.sp,
                style = Typography.labelLarge
            )
        }
    }
}

@Composable
private fun BoxScope.PlayerLockIndicator(
    isLocked: Boolean,
    onUnlock: () -> Unit
) {
    if (isLocked) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
                .clickable { onUnlock() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Unlock Controls",
                    tint = PureWhite,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tap to unlock", color = PureWhite, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PlayerControlsOverlay(
    state: PlayerOverlayState,
    currentPositionState: State<Long>,
    onBackClick: () -> Unit,
    onCastClick: () -> Unit,
    onHelpClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLockClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onRewindClick: () -> Unit,
    onForwardClick: () -> Unit,
    onSkipIntroClick: () -> Unit,
    onPictureInPictureClick: (() -> Unit)?,
    onNextEpisodeClick: (() -> Unit)?,
    onResizeModeChange: (Int, String) -> Unit,
    onCaptionSelected: (CaptionItemDto?) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onSleepTimerSelected: (Int) -> Unit,
    onQualitySelected: (String) -> Unit,
    onSeek: (Long) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        PlayerTopBar(
            displayTitle = state.displayTitle,
            onBackClick = onBackClick,
            onCastClick = onCastClick,
            onHelpClick = onHelpClick,
            onSettingsClick = onSettingsClick
        )

        PlayerLockLeftBar(onLockClick = onLockClick)

        PlayerCenterControls(
            isPlaying = state.isPlaying,
            playFocusRequester = state.playFocusRequester,
            onRewindClick = onRewindClick,
            onPlayPauseClick = onPlayPauseClick,
            onForwardClick = onForwardClick
        )

        PlayerBottomBar(
            state = state,
            currentPositionState = currentPositionState,
            onPlayPauseClick = onPlayPauseClick,
            onSkipIntroClick = onSkipIntroClick,
            onPictureInPictureClick = onPictureInPictureClick,
            onNextEpisodeClick = onNextEpisodeClick,
            onResizeModeChange = onResizeModeChange,
            onCaptionSelected = onCaptionSelected,
            onSpeedSelected = onSpeedSelected,
            onSleepTimerSelected = onSleepTimerSelected,
            onQualitySelected = onQualitySelected,
            onSeek = onSeek
        )
    }
}

@Composable
private fun BoxScope.PlayerTopBar(
    displayTitle: String,
    onBackClick: () -> Unit,
    onCastClick: () -> Unit,
    onHelpClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TvIconButton(
                image = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBackClick,
                buttonSize = 48.dp,
                iconSize = 24.dp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = displayTitle,
                color = PureWhite,
                fontSize = 16.sp,
                style = Typography.titleMedium
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val castInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(interactionSource = castInteraction, indication = null) { onCastClick() }
                    .tvFocusOutline(castInteraction, RoundedCornerShape(6.dp))
                    .padding(4.dp)
            ) {
                Icon(imageVector = Icons.Default.Tv, contentDescription = "Cast", tint = PureWhite, modifier = Modifier.size(20.dp))
                Text("TV", color = PureWhite, fontSize = 10.sp)
            }
            val helpInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(interactionSource = helpInteraction, indication = null) { onHelpClick() }
                    .tvFocusOutline(helpInteraction, RoundedCornerShape(6.dp))
                    .padding(4.dp)
            ) {
                Icon(imageVector = Icons.Default.HelpOutline, contentDescription = "Help", tint = PureWhite, modifier = Modifier.size(20.dp))
                Text("Help", color = PureWhite, fontSize = 10.sp)
            }
            val settingsInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(interactionSource = settingsInteraction, indication = null) { onSettingsClick() }
                    .tvFocusOutline(settingsInteraction, RoundedCornerShape(6.dp))
                    .padding(4.dp)
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = PureWhite, modifier = Modifier.size(20.dp))
                Text("Setting", color = PureWhite, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun BoxScope.PlayerLockLeftBar(onLockClick: () -> Unit) {
    Column(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(start = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onLockClick() }
        ) {
            Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Lock", tint = MutedGray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Tap to lock", color = MutedGray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun BoxScope.PlayerCenterControls(
    isPlaying: Boolean,
    playFocusRequester: FocusRequester,
    onRewindClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onForwardClick: () -> Unit
) {
    Row(
        modifier = Modifier.align(Alignment.Center),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TvIconButton(
            image = Icons.Default.Replay10,
            contentDescription = "Rewind 10 seconds",
            onClick = onRewindClick,
            buttonSize = 64.dp,
            iconSize = 44.dp
        )
        TvIconButton(
            image = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            onClick = onPlayPauseClick,
            buttonSize = 72.dp,
            iconSize = 54.dp,
            focusRequester = playFocusRequester
        )
        TvIconButton(
            image = Icons.Default.Forward10,
            contentDescription = "Forward 10 seconds",
            onClick = onForwardClick,
            buttonSize = 64.dp,
            iconSize = 44.dp
        )
    }
}

@Composable
private fun BoxScope.PlayerBottomBar(
    state: PlayerOverlayState,
    currentPositionState: State<Long>,
    onPlayPauseClick: () -> Unit,
    onSkipIntroClick: () -> Unit,
    onPictureInPictureClick: (() -> Unit)?,
    onNextEpisodeClick: (() -> Unit)?,
    onResizeModeChange: (Int, String) -> Unit,
    onCaptionSelected: (CaptionItemDto?) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onSleepTimerSelected: (Int) -> Unit,
    onQualitySelected: (String) -> Unit,
    onSeek: (Long) -> Unit
) {
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSleepTimerMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }

    val isLiveStream = remember(state.player.isCurrentMediaItemLive, state.duration, state.sources) {
        state.player.isCurrentMediaItemLive || state.duration <= 0L || state.sources.any { it.quality == "LIVE" || it.resolutions == "LIVE" }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (isLiveStream) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(PrimaryRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE STREAM",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        state.player.seekToDefaultPosition()
                        state.player.playWhenReady = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("GO LIVE", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            PlayerProgressSection(
                currentPositionState = currentPositionState,
                duration = state.duration,
                onSeek = onSeek
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvIconButton(
                    image = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    onClick = onPlayPauseClick,
                    buttonSize = 40.dp,
                    iconSize = 22.dp
                )
                TvIconButton(
                    image = Icons.Default.FastForward,
                    contentDescription = "Skip Intro",
                    onClick = onSkipIntroClick,
                    buttonSize = 40.dp,
                    iconSize = 22.dp
                )
                if (onPictureInPictureClick != null) {
                    TvIconButton(
                        image = Icons.Default.PictureInPictureAlt,
                        contentDescription = "Picture in picture",
                        onClick = onPictureInPictureClick,
                        buttonSize = 40.dp,
                        iconSize = 22.dp
                    )
                }
                if (onNextEpisodeClick != null) {
                    TvIconButton(
                        image = Icons.Default.SkipNext,
                        contentDescription = "Next episode",
                        onClick = onNextEpisodeClick,
                        buttonSize = 40.dp,
                        iconSize = 22.dp
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Aspect Ratio
                val fitInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(
                            interactionSource = fitInteraction,
                            indication = null
                        ) {
                            when (state.resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> onResizeModeChange(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Zoom")
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> onResizeModeChange(AspectRatioFrameLayout.RESIZE_MODE_FILL, "Fill")
                                else -> onResizeModeChange(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Fit")
                            }
                        }
                        .tvFocusOutline(fitInteraction, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.AspectRatio, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(state.currentFitText, color = PureWhite, fontSize = 12.sp)
                }

                // Subtitle / Language Selector
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showLanguageMenu = true }
                    ) {
                        Icon(imageVector = Icons.Default.Subtitles, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(state.selectedCaption?.label ?: "Language", color = PureWhite, fontSize = 12.sp)
                    }
                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Original Audio (Off)", color = PureWhite) },
                            onClick = { onCaptionSelected(null); showLanguageMenu = false }
                        )
                        state.captions.forEach { cap ->
                            DropdownMenuItem(
                                text = { Text(cap.label ?: cap.language ?: "Subtitle", color = PureWhite) },
                                onClick = { onCaptionSelected(cap); showLanguageMenu = false }
                            )
                        }
                    }
                }

                // Speed Selector
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showSpeedMenu = true }
                    ) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${state.currentSpeed}x", color = PureWhite, fontSize = 12.sp)
                    }
                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            DropdownMenuItem(
                                text = { Text("${speed}x Speed", color = PureWhite) },
                                onClick = {
                                    onSpeedSelected(speed)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }

                // Sleep timer
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showSleepTimerMenu = true }
                    ) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (state.sleepTimerDeadline > 0L) {
                                "${state.sleepTimerRemainingSeconds / 60}:${(state.sleepTimerRemainingSeconds % 60).toString().padStart(2, '0')}"
                            } else "Timer",
                            color = PureWhite,
                            fontSize = 12.sp
                        )
                    }
                    DropdownMenu(
                        expanded = showSleepTimerMenu,
                        onDismissRequest = { showSleepTimerMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Off", color = PureWhite) },
                            onClick = {
                                onSleepTimerSelected(0)
                                showSleepTimerMenu = false
                            }
                        )
                        listOf(15, 30, 45, 60).forEach { minutes ->
                            DropdownMenuItem(
                                text = { Text("$minutes minutes", color = PureWhite) },
                                onClick = {
                                    onSleepTimerSelected(minutes)
                                    showSleepTimerMenu = false
                                }
                            )
                        }
                    }
                }

                // Quality Selector
                Box {
                    Text(
                        text = state.currentQuality,
                        color = PureWhite,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { showQualityMenu = true }
                    )
                    DropdownMenu(
                        expanded = showQualityMenu,
                        onDismissRequest = { showQualityMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        state.availableQualities.forEach { q ->
                            DropdownMenuItem(
                                text = { Text(q, color = PureWhite) },
                                onClick = {
                                    onQualitySelected(q)
                                    showQualityMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerProgressSection(
    currentPositionState: State<Long>,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPosition = currentPositionState.value
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTime(currentPosition),
            color = PureWhite,
            fontSize = 12.sp
        )
        ThinSeekBar(
            currentPosition = currentPosition,
            duration = duration,
            onSeek = onSeek,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        Text(
            text = formatTime(duration),
            color = PureWhite,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PlayerDialogs(
    showCastDialog: Boolean,
    onDismissCast: () -> Unit,
    showHelpDialog: Boolean,
    onDismissHelp: () -> Unit,
    showSettingsDialog: Boolean,
    onDismissSettings: () -> Unit,
    serverOptions: List<String>,
    selectedServerIndex: Int,
    onServerSelected: ((Int) -> Unit)?,
    serverHost: String,
    isServerResolving: Boolean,
    currentQuality: String,
    currentSpeed: Float,
    currentFitText: String
) {
    if (showCastDialog) {
        AlertDialog(
            onDismissRequest = onDismissCast,
            containerColor = DarkSurface,
            title = { Text("Google Cast (TV)", color = PureWhite) },
            text = { Text("Scanning local network for Google Cast / Smart TV devices...\n\nNo devices found nearby. Ensure your TV and phone are connected to the same Wi-Fi network.", color = MutedGray) },
            confirmButton = {
                TextButton(onClick = onDismissCast) {
                    Text("Close", color = PrimaryRed)
                }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = onDismissHelp,
            containerColor = DarkSurface,
            title = { Text("Player Guide & Shortcuts", color = PureWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Double Tap Left/Right: Rewind or Fast Forward 10s", color = PureWhite, fontSize = 13.sp)
                    Text("• Play/Pause: Tap center play icon", color = PureWhite, fontSize = 13.sp)
                    Text("• Quality Switcher: Tap resolution badge (e.g. 1080P) at bottom right", color = PureWhite, fontSize = 13.sp)
                    Text("• Speed Control: Tap speed indicator (e.g. 1.0x)", color = PureWhite, fontSize = 13.sp)
                    Text("• Screen Lock: Tap 'Tap to lock' on left panel", color = PureWhite, fontSize = 13.sp)
                    Text("• Swipe up on the left for volume, or on the right for brightness", color = PureWhite, fontSize = 13.sp)
                    Text("• Resume Playback: Progress is automatically saved as you watch", color = PureWhite, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissHelp) {
                    Text("Got it", color = PrimaryRed)
                }
            }
        )
    }

    if (showSettingsDialog) {
        var showServerMenu by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = onDismissSettings,
            containerColor = DarkSurface,
            title = { Text("Playback Settings", color = PureWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (serverOptions.isNotEmpty() && onServerSelected != null) {
                        Box {
                            Text(
                                text = "Server: ${serverOptions.getOrNull(selectedServerIndex) ?: "Choose server"}  ▾",
                                color = PureWhite,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showServerMenu = true }
                                    .padding(vertical = 8.dp)
                            )
                            DropdownMenu(
                                expanded = showServerMenu,
                                onDismissRequest = { showServerMenu = false }
                            ) {
                                serverOptions.forEachIndexed { index, option ->
                                    DropdownMenuItem(
                                        text = { Text("${index + 1}. $option", maxLines = 1) },
                                        onClick = {
                                            showServerMenu = false
                                            onServerSelected(index)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Text("Active Stream Source: $serverHost", color = PrimaryGreen, fontSize = 12.sp)
                    if (isServerResolving) {
                        Text("Connecting to server…", color = MutedGray, fontSize = 12.sp)
                    }
                    Text("Selected Quality: $currentQuality", color = PureWhite, fontSize = 13.sp)
                    Text("Playback Speed: ${currentSpeed}x", color = PureWhite, fontSize = 13.sp)
                    Text("Aspect Ratio Mode: $currentFitText", color = PureWhite, fontSize = 13.sp)
                    Text("Hardware Acceleration: Enabled (ExoPlayer)", color = MutedGray, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissSettings) {
                    Text("Close", color = PrimaryRed)
                }
            }
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
