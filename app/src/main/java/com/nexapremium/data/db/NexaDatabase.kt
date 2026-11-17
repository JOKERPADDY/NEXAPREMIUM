package com.nexapremium.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 4,
    exportSchema = false
)
abstract class NexaDatabase : RoomDatabase() {
    abstract fun continueWatchingDao(): ContinueWatchingDao
    abstract fun myListDao(): MyListDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS continue_watching_new (
                        playbackKey TEXT NOT NULL,
                        detailPath TEXT NOT NULL,
                        title TEXT NOT NULL,
                        coverUrl TEXT NOT NULL,
                        subjectType TEXT NOT NULL,
                        seasonNumber INTEGER NOT NULL,
                        episodeNumber INTEGER NOT NULL,
                        positionMs INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL,
                        lastUpdated INTEGER NOT NULL,
                        PRIMARY KEY(playbackKey)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO continue_watching_new (
                        playbackKey, detailPath, title, coverUrl, subjectType,
                        seasonNumber, episodeNumber, positionMs, durationMs, lastUpdated
                    )
                    SELECT
                        CASE
                            WHEN seasonNumber > 0 OR episodeNumber > 0
                            THEN detailPath || '#S' || seasonNumber || 'E' || episodeNumber
                            ELSE detailPath
                        END,
                        detailPath, title, coverUrl, subjectType,
                        seasonNumber, episodeNumber, positionMs, durationMs, lastUpdated
                    FROM continue_watching
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE continue_watching")
                db.execSQL("ALTER TABLE continue_watching_new RENAME TO continue_watching")
            }
        }
    }
}
