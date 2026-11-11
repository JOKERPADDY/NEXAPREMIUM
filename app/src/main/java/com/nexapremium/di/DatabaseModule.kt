package com.nexapremium.di

import android.content.Context
import androidx.room.Room
import com.nexapremium.data.db.NexaDatabase
import com.nexapremium.data.db.dao.ContinueWatchingDao
import com.nexapremium.data.db.dao.DownloadDao
import com.nexapremium.data.db.dao.MyListDao
import com.nexapremium.data.db.dao.PlaybackProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNexaDatabase(@ApplicationContext context: Context): NexaDatabase {
        return Room.databaseBuilder(
            context,
            NexaDatabase::class.java,
            "nexapremium_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideContinueWatchingDao(db: NexaDatabase): ContinueWatchingDao {
        return db.continueWatchingDao()
    }

    @Provides
    fun provideMyListDao(db: NexaDatabase): MyListDao {
        return db.myListDao()
    }

    @Provides
    fun providePlaybackProgressDao(db: NexaDatabase): PlaybackProgressDao {
        return db.playbackProgressDao()
    }

    @Provides
    fun provideDownloadDao(db: NexaDatabase): DownloadDao {
        return db.downloadDao()
    }
}
