package com.nexapremium.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.nexapremium.BuildConfig
import com.nexapremium.data.repository.DownloadPreferencesRepository
import com.nexapremium.data.repository.FamilyModeRepository
import com.nexapremium.data.repository.PlaybackPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val familyModeRepository: FamilyModeRepository,
    private val downloadPreferencesRepository: DownloadPreferencesRepository,
    private val playbackPreferencesRepository: PlaybackPreferencesRepository
) : ViewModel() {
    val appVersion = BuildConfig.VERSION_NAME
    val familyModeEnabled: StateFlow<Boolean> = familyModeRepository.enabled
    val wifiOnlyDownloads: StateFlow<Boolean> = downloadPreferencesRepository.wifiOnly
    val defaultDownloadQuality: StateFlow<String> = downloadPreferencesRepository.defaultQuality
    val autoDeleteWatchedEpisodes: StateFlow<Boolean> = downloadPreferencesRepository.autoDeleteWatchedEpisodes
    val defaultPlaybackSpeed: StateFlow<Float> = playbackPreferencesRepository.defaultSpeed
    val autoPictureInPicture: StateFlow<Boolean> = playbackPreferencesRepository.autoPictureInPicture

    fun setFamilyModeEnabled(enabled: Boolean) {
        familyModeRepository.setEnabled(enabled)
    }

    fun setWifiOnlyDownloads(enabled: Boolean) = downloadPreferencesRepository.setWifiOnly(enabled)
    fun setDefaultDownloadQuality(quality: String) = downloadPreferencesRepository.setDefaultQuality(quality)
    fun setAutoDeleteWatchedEpisodes(enabled: Boolean) = downloadPreferencesRepository.setAutoDeleteWatchedEpisodes(enabled)
    fun setDefaultPlaybackSpeed(speed: Float) = playbackPreferencesRepository.setDefaultSpeed(speed)
    fun setAutoPictureInPicture(enabled: Boolean) = playbackPreferencesRepository.setAutoPictureInPicture(enabled)
}
