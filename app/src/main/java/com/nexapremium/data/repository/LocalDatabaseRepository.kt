package com.nexapremium.data.repository

import com.nexapremium.data.db.dao.ContinueWatchingDao
import com.nexapremium.data.db.dao.DownloadDao
import com.nexapremium.data.db.dao.MyListDao
import com.nexapremium.data.db.dao.PlaybackProgressDao
import com.nexapremium.data.db.entity.ContinueWatchingEntity
import com.nexapremium.data.db.entity.DownloadEntity
import com.nexapremium.data.db.entity.MyListEntity
import com.nexapremium.data.db.entity.PlaybackProgressEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDatabaseRepository @Inject constructor(
    private val continueWatchingDao: ContinueWatchingDao,
    private val myListDao: MyListDao,
    private val playbackProgressDao: PlaybackProgressDao,
    private val downloadDao: DownloadDao
) {
    fun getContinueWatchingFlow(): Flow<List<ContinueWatchingEntity>> =
        continueWatchingDao.getAllContinueWatching()

    suspend fun saveContinueWatching(item: ContinueWatchingEntity) =
        continueWatchingDao.upsert(item)

    suspend fun removeContinueWatching(detailPath: String) =
        continueWatchingDao.deleteByDetailPath(detailPath)

    suspend fun getContinueWatching(detailPath: String): ContinueWatchingEntity? =
        continueWatchingDao.getByDetailPath(detailPath)

    fun getMyListFlow(): Flow<List<MyListEntity>> =
        myListDao.getAllMyList()

    fun isBookmarkedFlow(detailPath: String): Flow<Boolean> =
        myListDao.isBookmarkFlow(detailPath)

    suspend fun toggleBookmark(item: MyListEntity, isBookmarked: Boolean) {
        if (isBookmarked) {
            myListDao.deleteByDetailPath(item.detailPath)
        } else {
            myListDao.insert(item)
        }
    }

    suspend fun getProgress(streamKey: String): PlaybackProgressEntity? =
        playbackProgressDao.getProgress(streamKey)

    suspend fun saveProgress(progress: PlaybackProgressEntity) =
        playbackProgressDao.saveProgress(progress)

    fun getAllDownloads(): Flow<List<DownloadEntity>> =
        downloadDao.getAllDownloads()

    suspend fun getAllDownloadsOnce(): List<DownloadEntity> =
        downloadDao.getAllDownloadsOnce()

    suspend fun saveDownload(download: DownloadEntity) =
        downloadDao.insertDownload(download)

    suspend fun updateDownloadTimestamp(id: String, timestamp: Long) =
        downloadDao.updateTimestamp(id, timestamp)

    suspend fun deleteDownload(id: String) =
        downloadDao.deleteDownload(id)
}
