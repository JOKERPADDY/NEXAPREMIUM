package com.nexapremium.data.db.dao

import androidx.room.*
import com.nexapremium.data.db.entity.MyListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MyListDao {

    @Query("SELECT * FROM my_list ORDER BY addedTimestamp DESC")
    fun getAllMyList(): Flow<List<MyListEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM my_list WHERE detailPath = :path)")
    fun isBookmarkFlow(path: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MyListEntity)

    @Query("DELETE FROM my_list WHERE detailPath = :path")
    suspend fun deleteByDetailPath(path: String)
}
