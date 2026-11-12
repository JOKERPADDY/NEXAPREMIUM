package com.nexapremium.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapremium.data.sports.SportsMatch
import com.nexapremium.data.sports.SportsRepository
import com.nexapremium.data.sports.SportsStream
import com.nexapremium.data.sports.ResolvedSportsStream
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SportsViewModel @Inject constructor(private val repository: SportsRepository) : ViewModel() {
    private val _matches = MutableStateFlow<List<SportsMatch>>(emptyList())
    val matches = _matches.asStateFlow()
    private val _isLoadingMatches = MutableStateFlow(true)
    val isLoadingMatches = _isLoadingMatches.asStateFlow()
    private val _streams = MutableStateFlow<List<SportsStream>>(emptyList())
    val streams = _streams.asStateFlow()
    private val _selectedStream = MutableStateFlow<ResolvedSportsStream?>(null)
    val selectedStream = _selectedStream.asStateFlow()
    private val _selectedServerIndex = MutableStateFlow(-1)
    val selectedServerIndex = _selectedServerIndex.asStateFlow()
    private val _isResolving = MutableStateFlow(true)
    val isResolving = _isResolving.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val attemptedServerIndices = mutableSetOf<Int>()
    private val playbackRecoveryAttempts = mutableMapOf<Int, Int>()
    private var resolutionGeneration = 0

    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _isLoadingMatches.value = true
        runCatching { repository.getLiveMatches() }
            .onSuccess {
                _matches.value = it
                _error.value = if (it.isEmpty()) "Live sports are unavailable right now." else null
            }
            .onFailure { _error.value = it.message ?: "Live sports unavailable" }
        _isLoadingMatches.value = false
    }
    fun loadStreams(matchId: String) = viewModelScope.launch {
        val generation = ++resolutionGeneration
        _streams.value = emptyList()
        _selectedStream.value = null
        _selectedServerIndex.value = -1
        _error.value = null
        _isResolving.value = true
        attemptedServerIndices.clear()
        playbackRecoveryAttempts.clear()

        runCatching { repository.getStreams(matchId) }
            .onSuccess { available ->
                if (generation != resolutionGeneration) return@onSuccess
                _streams.value = available
                if (available.none { it.live }) {
                    _error.value = "No live streams are available for this match yet."
                    _isResolving.value = false
                } else {
                    resolveServers(startIndex = 0, generation = generation)
                }
            }
            .onFailure {
                if (generation == resolutionGeneration) {
                    _error.value = friendlySportsError(it)
                    _isResolving.value = false
                }
            }
    }

    /** Called by the player when a resolved source fails during playback. */
    fun tryNextServerAfterPlaybackError() {
        if (_isResolving.value || _streams.value.isEmpty()) return
        val failedIndex = _selectedServerIndex.value
        if (failedIndex >= 0) attemptedServerIndices += failedIndex
        _error.value = null
        val generation = ++resolutionGeneration
        val hasUntriedAlternative = _streams.value.indices.any { index ->
            index != failedIndex && _streams.value[index].live && index !in attemptedServerIndices
        }
        val canRefreshCurrentServer = failedIndex >= 0 &&
            !hasUntriedAlternative && playbackRecoveryAttempts.getOrDefault(failedIndex, 0) < 1
        val retryCurrentIfAlternativesFail = failedIndex.takeIf { hasUntriedAlternative }
        val nextIndex = if (canRefreshCurrentServer) {
            playbackRecoveryAttempts[failedIndex] = playbackRecoveryAttempts.getOrDefault(failedIndex, 0) + 1
            attemptedServerIndices.remove(failedIndex)
            failedIndex
        } else if (failedIndex >= 0) {
            failedIndex + 1
        } else {
            0
        }
        viewModelScope.launch {
            resolveServers(nextIndex, generation, retryIndexIfExhausted = retryCurrentIfAlternativesFail)
        }
    }

    /** Allows the viewer to choose or retry a server from the player's Settings menu. */
    fun selectServer(index: Int) {
        if (index !in _streams.value.indices || !_streams.value[index].live) return
        attemptedServerIndices.remove(index)
        playbackRecoveryAttempts.remove(index)
        _error.value = null
        val generation = ++resolutionGeneration
        viewModelScope.launch { resolveServers(index, generation) }
    }

    fun retry(matchId: String) {
        if (_streams.value.none { it.live }) {
            loadStreams(matchId)
            return
        }
        attemptedServerIndices.clear()
        playbackRecoveryAttempts.clear()
        _error.value = null
        val generation = ++resolutionGeneration
        viewModelScope.launch { resolveServers(startIndex = 0, generation = generation) }
    }

    private suspend fun resolveServers(
        startIndex: Int,
        generation: Int,
        retryIndexIfExhausted: Int? = null
    ) {
        val available = _streams.value
        if (available.isEmpty() || generation != resolutionGeneration) return

        _isResolving.value = true
        val start = startIndex.coerceIn(0, available.lastIndex)
        val order = (start..available.lastIndex).toList() + (0 until start).toList()
        var lastFailure: Throwable? = null

        for (index in order) {
            if (generation != resolutionGeneration) return
            val stream = available[index]
            if (!stream.live || !attemptedServerIndices.add(index)) continue

            val result = runCatching { repository.resolveStream(stream) }
            if (generation != resolutionGeneration) return
            result.onSuccess {
                _selectedServerIndex.value = index
                _selectedStream.value = it
                _error.value = null
                _isResolving.value = false
                return
            }.onFailure { lastFailure = it }
        }

        if (generation == resolutionGeneration) {
            if (retryIndexIfExhausted != null &&
                playbackRecoveryAttempts.getOrDefault(retryIndexIfExhausted, 0) < 1
            ) {
                playbackRecoveryAttempts[retryIndexIfExhausted] = 1
                attemptedServerIndices.remove(retryIndexIfExhausted)
                resolveServers(retryIndexIfExhausted, generation)
                return
            }
            _error.value = lastFailure?.let(::friendlySportsError)
                ?: "No other live servers are available. Choose a server in player Settings to retry."
            _isResolving.value = false
        }
    }

    private fun friendlySportsError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("violation", ignoreCase = true) || message.contains("HTTP 403") ->
                "The sports provider is blocking requests right now. Try another stream or check back later."
            message.contains("Resolver returned", ignoreCase = true) ->
                "Could not load a playable stream from the resolver. Try another stream."
            else -> message.ifBlank { "Could not load sports streams. Try again later." }
        }
    }
}
