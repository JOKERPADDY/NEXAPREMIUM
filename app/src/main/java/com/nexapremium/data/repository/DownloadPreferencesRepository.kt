package com.nexapremium.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _wifiOnly = MutableStateFlow(preferences.getBoolean(KEY_WIFI_ONLY, false))
    val wifiOnly: StateFlow<Boolean> = _wifiOnly.asStateFlow()

    private val _defaultQuality = MutableStateFlow(preferences.getString(KEY_DEFAULT_QUALITY, QUALITY_AUTO) ?: QUALITY_AUTO)
    val defaultQuality: StateFlow<String> = _defaultQuality.asStateFlow()

    private val _autoDeleteWatchedEpisodes = MutableStateFlow(preferences.getBoolean(KEY_AUTO_DELETE_WATCHED, false))
    val autoDeleteWatchedEpisodes: StateFlow<Boolean> = _autoDeleteWatchedEpisodes.asStateFlow()

    fun setWifiOnly(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_WIFI_ONLY, enabled) }
        _wifiOnly.value = enabled
    }

    fun setDefaultQuality(quality: String) {
        val value = quality.takeIf { it in DOWNLOAD_QUALITIES } ?: QUALITY_AUTO
        preferences.edit { putString(KEY_DEFAULT_QUALITY, value) }
        _defaultQuality.value = value
    }

    fun setAutoDeleteWatchedEpisodes(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_AUTO_DELETE_WATCHED, enabled) }
        _autoDeleteWatchedEpisodes.value = enabled
    }

    companion object {
        const val QUALITY_AUTO = "Auto"
        val DOWNLOAD_QUALITIES = listOf(QUALITY_AUTO, "360P", "480P", "720P", "1080P")

        private const val PREFERENCES_NAME = "nexapremium_download_preferences"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_DEFAULT_QUALITY = "default_quality"
        private const val KEY_AUTO_DELETE_WATCHED = "auto_delete_watched_episodes"
    }
}
