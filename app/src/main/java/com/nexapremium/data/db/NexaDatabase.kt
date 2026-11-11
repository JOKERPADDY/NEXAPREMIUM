package com.nexapremium.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nexapremium.data.db.dao.ContinueWatchingDao
import com.nexapremium.data.db.dao.DownloadDao
import com.nexapremium.data.db.dao.MyListDao
import com.nexapremium.data.db.dao.PlaybackProgressDao
import com.nexapremium.data.db.entity.ContinueWatchingEntity
import com.nexapremium.data.db.entity.DownloadEntity
import com.nexapremium.data.db.entity.MyListEntity
import com.nexapremium.data.db.entity.PlaybackProgressEntity

@Database(
    entities = [
        ContinueWatchingEntity::class,
        MyListEntity::class,
        PlaybackProgressEntity::class,
        DownloadEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NexaDatabase : RoomDatabase() {
    abstract fun continueWatchingDao(): ContinueWatchingDao
    abstract fun myListDao(): MyListDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun downloadDao(): DownloadDao
}
