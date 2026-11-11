package com.nexapremium.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey val streamKey: String, // e.g. "batman-MenTqxvHqCa_S1_E2"
    val detailPath: String,
    val seasonNumber: Int = 0,
    val episodeNumber: Int = 0,
    val lastPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
