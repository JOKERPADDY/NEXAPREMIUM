package com.nexapremium.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapremium.data.db.entity.DownloadEntity
import com.nexapremium.data.download.AppDownloadManager
import com.nexapremium.data.download.TransferProgress
import com.nexapremium.data.download.TransferStatus
import com.nexapremium.data.repository.LocalDatabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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
}
