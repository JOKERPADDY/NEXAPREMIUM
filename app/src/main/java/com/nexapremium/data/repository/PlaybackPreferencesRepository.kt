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
class PlaybackPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _defaultSpeed = MutableStateFlow(preferences.getFloat(KEY_DEFAULT_SPEED, DEFAULT_SPEED))
    val defaultSpeed: StateFlow<Float> = _defaultSpeed.asStateFlow()

    private val _autoPictureInPicture = MutableStateFlow(preferences.getBoolean(KEY_AUTO_PIP, true))
    val autoPictureInPicture: StateFlow<Boolean> = _autoPictureInPicture.asStateFlow()

    fun setDefaultSpeed(speed: Float) {
        val value = speed.takeIf { it in PLAYBACK_SPEEDS } ?: DEFAULT_SPEED
        preferences.edit { putFloat(KEY_DEFAULT_SPEED, value) }
        _defaultSpeed.value = value
    }

    fun setAutoPictureInPicture(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_AUTO_PIP, enabled) }
        _autoPictureInPicture.value = enabled
    }

    companion object {
        val PLAYBACK_SPEEDS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        const val DEFAULT_SPEED = 1.0f

        private const val PREFERENCES_NAME = "nexapremium_playback_preferences"
        private const val KEY_DEFAULT_SPEED = "default_speed"
        private const val KEY_AUTO_PIP = "auto_picture_in_picture"
    }
}
