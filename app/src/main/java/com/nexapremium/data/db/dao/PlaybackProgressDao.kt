package com.nexapremium.data.db.dao

import androidx.room.*
import com.nexapremium.data.db.entity.PlaybackProgressEntity

@Dao
interface PlaybackProgressDao {

    @Query("SELECT * FROM playback_progress WHERE streamKey = :key LIMIT 1")
    suspend fun getProgress(key: String): PlaybackProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: PlaybackProgressEntity)
}
