package com.nexapremium.data.db.dao

import androidx.room.*
import com.nexapremium.data.db.entity.ContinueWatchingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContinueWatchingDao {

    @Query("SELECT * FROM continue_watching ORDER BY lastUpdated DESC")
    fun getAllContinueWatching(): Flow<List<ContinueWatchingEntity>>

    @Query("SELECT * FROM continue_watching WHERE detailPath = :path LIMIT 1")
    suspend fun getByDetailPath(path: String): ContinueWatchingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ContinueWatchingEntity)

    @Query("DELETE FROM continue_watching WHERE detailPath = :path")
    suspend fun deleteByDetailPath(path: String)
}
