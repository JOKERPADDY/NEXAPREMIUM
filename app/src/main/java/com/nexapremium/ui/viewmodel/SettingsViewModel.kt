package com.nexapremium.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.nexapremium.data.repository.FamilyModeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val familyModeRepository: FamilyModeRepository
) : ViewModel() {
    val appVersion = "1.0.0"
    val familyModeEnabled: StateFlow<Boolean> = familyModeRepository.enabled

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _autoMiniplayerEnabled = MutableStateFlow(true)
    val autoMiniplayerEnabled: StateFlow<Boolean> = _autoMiniplayerEnabled.asStateFlow()

    private val _backgroundDownloadEnabled = MutableStateFlow(true)
    val backgroundDownloadEnabled: StateFlow<Boolean> = _backgroundDownloadEnabled.asStateFlow()

    fun setFamilyModeEnabled(enabled: Boolean) {
        familyModeRepository.setEnabled(enabled)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun setAutoMiniplayerEnabled(enabled: Boolean) {
        _autoMiniplayerEnabled.value = enabled
    }

    fun setBackgroundDownloadEnabled(enabled: Boolean) {
        _backgroundDownloadEnabled.value = enabled
    }
}
