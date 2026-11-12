package com.nexapremium.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapremium.data.api.model.CaptionItemDto
import com.nexapremium.data.api.model.PlaybackDataDto
import com.nexapremium.data.repository.LocalDatabaseRepository
import com.nexapremium.data.repository.PlaybackRepository
import com.nexapremium.data.repository.DownloadPreferencesRepository
import com.nexapremium.data.repository.PlaybackPreferencesRepository
import com.nexapremium.data.download.AppDownloadManager
import com.nexapremium.data.db.entity.DownloadEntity
import java.io.File
import com.nexapremium.data.db.entity.ContinueWatchingEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PlayerUiState {
    object Loading : PlayerUiState
    data class Success(
        val playbackData: PlaybackDataDto,
        val captions: List<CaptionItemDto> = emptyList(),
        val resumePositionMs: Long = 0L
    ) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
    data class Offline(val download: DownloadEntity) : PlayerUiState
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val localDatabaseRepository: LocalDatabaseRepository,
    private val downloadPreferencesRepository: DownloadPreferencesRepository,
    private val playbackPreferencesRepository: PlaybackPreferencesRepository,
    private val appDownloadManager: AppDownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    val defaultPlaybackSpeed: StateFlow<Float> = playbackPreferencesRepository.defaultSpeed

    fun setDefaultPlaybackSpeed(speed: Float) {
        playbackPreferencesRepository.setDefaultSpeed(speed)
    }

    fun onPlaybackEnded(detailPath: String, season: Int, episode: Int) {
        viewModelScope.launch {
            localDatabaseRepository.removeContinueWatching(detailPath)
            if (season <= 0 || episode <= 0 || !downloadPreferencesRepository.autoDeleteWatchedEpisodes.value) return@launch
            val episodeIdSuffix = "_S${season}E${episode}"
            val download = localDatabaseRepository.getAllDownloadsOnce().firstOrNull {
                it.detailPath == detailPath && it.id.endsWith(episodeIdSuffix)
            } ?: return@launch
            appDownloadManager.cancel(download)
            localDatabaseRepository.deleteDownload(download.id)
        }
    }

    fun loadPlayback(detailPath: String, season: Int = 0, episode: Int = 0, offlineOnly: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            if (offlineOnly) {
                val download = localDatabaseRepository.getAllDownloadsOnce()
                    .firstOrNull {
                        it.detailPath == detailPath &&
                            it.id.endsWith("_S${season}E${episode}") &&
                            File(it.filePath).isFile
                    }
                _uiState.value = download?.let(PlayerUiState::Offline)
                    ?: PlayerUiState.Error("This download is missing from the device")
                return@launch
            }
            playbackRepository.getPlayback(detailPath, season, episode)
                .onSuccess { response ->
                    val playbackData = response.playbackData
                    val sources = playbackData.playableSources.ifEmpty { playbackData.effectiveSources }
                    if (sources.isNotEmpty()) {
                        val saved = localDatabaseRepository.getContinueWatching(detailPath)
                        val resumePosition = saved?.positionMs?.takeIf {
                            it > 0L && (saved.durationMs <= 0L || saved.durationMs - it > 30_000L)
                        } ?: 0L
                        _uiState.value = PlayerUiState.Success(playbackData = playbackData, resumePositionMs = resumePosition)
                        loadCaptions(detailPath, playbackData.streamId ?: sources.firstOrNull()?.id)
                    } else {
                        _uiState.value = PlayerUiState.Error("Playback stream not available")
                    }
                }
                .onFailure { error ->
                    _uiState.value = PlayerUiState.Error(error.message ?: "Failed to load stream")
                }
        }
    }

    private fun loadCaptions(detailPath: String, streamId: String?) {
        viewModelScope.launch {
            playbackRepository.getCaptions(detailPath, streamId)
                .onSuccess { response ->
                    val captions = response.data ?: emptyList()
                    val state = _uiState.value
                    if (state is PlayerUiState.Success) {
                        _uiState.value = state.copy(captions = captions)
                    }
                }
        }
    }

    fun saveProgress(detailPath: String, title: String, coverUrl: String, positionMs: Long, durationMs: Long, season: Int = 0, episode: Int = 0) {
        viewModelScope.launch {
            if (durationMs <= 0L) return@launch
            if (positionMs >= durationMs - 30_000L) {
                localDatabaseRepository.removeContinueWatching(detailPath)
                return@launch
            }
            val entity = ContinueWatchingEntity(
                detailPath = detailPath,
                title = title,
                coverUrl = coverUrl,
                subjectType = if (season > 0 || episode > 0) "series" else "movie",
                seasonNumber = season,
                episodeNumber = episode,
                positionMs = positionMs,
                durationMs = durationMs
            )
            localDatabaseRepository.saveContinueWatching(entity)
        }
    }
}
