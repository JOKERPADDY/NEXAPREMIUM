package com.nexapremium.ui.viewmodel

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapremium.data.db.entity.DownloadEntity
import com.nexapremium.data.download.AppDownloadManager
import com.nexapremium.data.download.TransferProgress
import com.nexapremium.data.download.TransferStatus
import com.nexapremium.data.repository.LocalDatabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

data class DownloadItemState(
    val entity: DownloadEntity,
    val progress: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val isDownloading: Boolean = false,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = true,
    val isFailed: Boolean = false,
    val canRetry: Boolean = false,
    val isQueued: Boolean = false,
    val isWaitingForWifi: Boolean = false,
    val statusText: String = "Completed"
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val localDatabaseRepository: LocalDatabaseRepository,
    private val appDownloadManager: AppDownloadManager
) : ViewModel() {

    val downloadStates: StateFlow<List<DownloadItemState>> = combine(
        localDatabaseRepository.getAllDownloads(),
        appDownloadManager.progress
    ) { entities, transfers ->
        entities.map { entity -> getDownloadState(entity, transfers[entity.id]) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun togglePauseDownload(downloadState: DownloadItemState) {
        if (downloadState.isPaused) resumeAtEndOfQueue(downloadState.entity)
        else appDownloadManager.pause(downloadState.entity.id)
    }

    fun retryDownload(downloadState: DownloadItemState) {
        if (downloadState.isFailed && downloadState.canRetry) {
            resumeAtEndOfQueue(downloadState.entity)
        }
    }

    private fun resumeAtEndOfQueue(entity: DownloadEntity) {
        viewModelScope.launch {
            localDatabaseRepository.updateDownloadTimestamp(entity.id, System.currentTimeMillis())
            appDownloadManager.resume(entity)
        }
    }

    fun moveQueuedDownload(id: String, direction: Int) {
        val queue = downloadStates.value.filter { it.isQueued }.sortedByDescending { it.entity.timestamp }.toMutableList()
        val index = queue.indexOfFirst { it.entity.id == id }
        val target = index + direction
        if (index < 0 || target !in queue.indices) return
        val item = queue.removeAt(index)
        queue.add(target, item)
        appDownloadManager.reorderQueuedDownloads(queue.map { it.entity.id })
        val firstTimestamp = System.currentTimeMillis()
        viewModelScope.launch {
            queue.forEachIndexed { order, state ->
                localDatabaseRepository.updateDownloadTimestamp(state.entity.id, firstTimestamp - order)
            }
        }
    }

    private fun getDownloadState(entity: DownloadEntity, transfer: TransferProgress?): DownloadItemState {
        val finalFile = File(entity.filePath)
        val partFile = File("${entity.filePath}.part")
        val expectedBytes = (entity.fileSizeMb * 1024.0 * 1024.0).toLong().coerceAtLeast(0L)
        val partBytes = partFile.takeIf(File::exists)?.length() ?: 0L

        if (transfer == null) {
            if (finalFile.isFile && finalFile.length() > 0L) {
                val bytes = finalFile.length()
                return DownloadItemState(entity, 100, bytes, bytes, isCompleted = true, statusText = "Completed")
            }
            val canResume = appDownloadManager.hasResumeData(entity)
            val bytes = maxOf(partBytes, 0L)
            val progress = if (expectedBytes > 0L) (bytes * 100 / expectedBytes).toInt().coerceIn(0, 99) else 0
            val failure = appDownloadManager.failureReason(entity)
            if (failure != null) {
                return DownloadItemState(
                    entity = entity,
                    progress = progress,
                    bytesDownloaded = bytes,
                    totalBytes = expectedBytes,
                    isCompleted = false,
                    isFailed = true,
                    canRetry = canResume,
                    statusText = "Failed: $failure"
                )
            }
            return DownloadItemState(
                entity = entity,
                progress = progress,
                bytesDownloaded = bytes,
                totalBytes = expectedBytes,
                isPaused = canResume,
                isCompleted = false,
                isFailed = !canResume,
                canRetry = canResume,
                statusText = if (canResume) "Paused" else "Request unavailable · open title to download again"
            )
        }

        val total = transfer.totalBytes.takeIf { it > 0L } ?: expectedBytes
        val bytes = if (transfer.bytesDownloaded > 0L) transfer.bytesDownloaded else partBytes
        val percent = if (total > 0L) (bytes * 100 / total).toInt().coerceIn(0, 100) else 0
        return when (transfer.status) {
            TransferStatus.QUEUED -> DownloadItemState(entity, percent, bytes, total, isCompleted = false, isQueued = true, statusText = "Queued")
            TransferStatus.CONNECTING -> DownloadItemState(entity, percent, bytes, total, isDownloading = true, isCompleted = false, statusText = "Starting download…")
            TransferStatus.DOWNLOADING -> DownloadItemState(entity, percent, bytes, total, isDownloading = true, isCompleted = false, statusText = "Downloading ($percent%)...")
            TransferStatus.WAITING_FOR_WIFI -> DownloadItemState(entity, percent, bytes, total, isCompleted = false, isQueued = true, isWaitingForWifi = true, statusText = "Waiting for Wi-Fi")
            TransferStatus.PAUSED -> DownloadItemState(entity, percent, bytes, total, isPaused = true, isCompleted = false, statusText = "Paused")
            TransferStatus.COMPLETED -> {
                val actual = finalFile.takeIf(File::exists)?.length() ?: bytes
                DownloadItemState(entity, 100, actual, actual, isCompleted = true, statusText = "Completed")
            }
            TransferStatus.FAILED -> DownloadItemState(
                entity = entity,
                progress = percent,
                bytesDownloaded = bytes,
                totalBytes = total,
                isPaused = true,
                isCompleted = false,
                isFailed = true,
                canRetry = appDownloadManager.hasResumeData(entity),
                statusText = transfer.error?.let { "Failed: $it" } ?: "Download failed"
            )
        }
    }

    fun cancelOrDeleteDownload(downloadState: DownloadItemState) {
        appDownloadManager.cancel(downloadState.entity)
        viewModelScope.launch {
            localDatabaseRepository.deleteDownload(downloadState.entity.id)
        }
    }

    fun clearCompletedDownloads(downloadStates: List<DownloadItemState>) {
        viewModelScope.launch {
            downloadStates.filter { it.isCompleted }.forEach { state ->
                appDownloadManager.cancel(state.entity)
                localDatabaseRepository.deleteDownload(state.entity.id)
            }
        }
    }

    fun saveToDevice(context: Context, downloadState: DownloadItemState, onResult: (Boolean, String) -> Unit) {
        if (!downloadState.isCompleted) {
            onResult(false, "Only completed downloads can be saved to device")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val srcFile = File(downloadState.entity.filePath).takeIf { it.exists() && it.length() > 0L }
            if (srcFile == null) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Downloaded file not found on disk")
                }
                return@launch
            }

            try {
                val rawTitle = downloadState.entity.title
                    .replace(Regex("\\s+\\(${Regex.escape(downloadState.entity.quality)}\\)$", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("_S\\d+E\\d+$", RegexOption.IGNORE_CASE), "")
                val (season, episode) = downloadEpisodeNumbers(downloadState.entity.id)
                val nameFormatted = if (episode > 0) "${rawTitle}_S${season}E${episode}" else rawTitle
                val cleanName = nameFormatted.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val fileName = "$cleanName.mp4"

                val success = saveFileToPublicStorage(context, srcFile, fileName)
                withContext(Dispatchers.Main) {
                    if (success) {
                        onResult(true, "Saved to Downloads/NexaPremium/$fileName")
                    } else {
                        onResult(false, "Failed to save file to device storage")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "Failed to save file")
                }
            }
        }
    }

    private fun downloadEpisodeNumbers(id: String): Pair<Int, Int> {
        val match = Regex("_S(\\d+)E(\\d+)$", RegexOption.IGNORE_CASE).find(id)
        return (match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0) to
            (match?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0)
    }

    private fun saveFileToPublicStorage(context: Context, srcFile: File, fileName: String): Boolean {
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/NexaPremium")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            } else {
                resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            }

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { output ->
                    srcFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                true
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val nexaDir = File(downloadsDir, "NexaPremium").apply { mkdirs() }
                val destFile = File(nexaDir, fileName)
                srcFile.copyTo(destFile, overwrite = true)
                MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf("video/mp4"), null)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
