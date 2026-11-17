package com.nexapremium.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "continue_watching")
data class ContinueWatchingEntity(
    @PrimaryKey val playbackKey: String,
    val detailPath: String,
    val title: String,
    val coverUrl: String,
    val subjectType: String, // "movie" or "series"
    val seasonNumber: Int = 0,
    val episodeNumber: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    companion object {
        fun playbackKey(detailPath: String, seasonNumber: Int, episodeNumber: Int): String =
            if (seasonNumber > 0 || episodeNumber > 0) {
                "$detailPath#S${seasonNumber}E${episodeNumber}"
            } else {
                detailPath
            }
    }
}
