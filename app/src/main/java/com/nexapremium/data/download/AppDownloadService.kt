package com.nexapremium.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nexapremium.R
import com.nexapremium.data.db.entity.DownloadEntity
import com.nexapremium.data.repository.LocalDatabaseRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class AppDownloadService : Service() {
    @Inject lateinit var downloadManager: AppDownloadManager
    @Inject lateinit var databaseRepository: LocalDatabaseRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val gson = Gson()
    @Volatile private var commandHandled = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        serviceScope.launch {
            downloadManager.progress.collectLatest { transfers ->
                val activeCount = transfers.values.count {
                    it.status == TransferStatus.CONNECTING || it.status == TransferStatus.DOWNLOADING
                }
                val queuedCount = transfers.values.count { it.status == TransferStatus.QUEUED }
                val waitingForWifi = transfers.values.any { it.status == TransferStatus.WAITING_FOR_WIFI }
                if (activeCount > 0) {
                    updateForegroundNotification("$activeCount downloading${if (queuedCount > 0) " · $queuedCount queued" else ""}")
                } else if (queuedCount > 0) {
                    updateForegroundNotification("$queuedCount queued download${if (queuedCount == 1) "" else "s"}")
                } else if (waitingForWifi) {
                    updateForegroundNotification("Waiting for Wi-Fi to resume downloads")
                } else if (commandHandled) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateForegroundNotification("Managing downloads in NEXAPREMIUM")
        when (intent?.action) {
            ACTION_START -> {
                val entity = parseEntity(intent.getStringExtra(EXTRA_ENTITY))
                val request = runCatching {
                    gson.fromJson(intent.getStringExtra(EXTRA_REQUEST), SavedDownloadRequest::class.java)
                }.getOrNull()
                if (entity != null && request != null) downloadManager.startTransfer(entity, request)
                commandHandled = true
            }
            ACTION_PAUSE -> {
                intent.getStringExtra(EXTRA_ID)?.let(downloadManager::pauseTransfer)
                commandHandled = true
            }
            ACTION_RESUME -> {
                parseEntity(intent.getStringExtra(EXTRA_ENTITY))?.let { downloadManager.resumeTransfer(it, forceResume = true) }
                commandHandled = true
            }
            ACTION_RECOVER_PENDING, null -> recoverIncompleteDownloads()
            else -> recoverIncompleteDownloads()
        }
        return START_STICKY
    }

    private fun recoverIncompleteDownloads() {
        serviceScope.launch {
            val entities = withContext(Dispatchers.IO) { databaseRepository.getAllDownloadsOnce() }
            entities.forEach { entity ->
                if (!java.io.File(entity.filePath).isFile && downloadManager.hasResumeData(entity)) {
                    downloadManager.resumeTransfer(entity)
                }
            }
            commandHandled = true
            if (downloadManager.progress.value.values.none {
                    downloadManager.isRunning(it.status) || it.status == TransferStatus.WAITING_FOR_WIFI
                }) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun parseEntity(json: String?): DownloadEntity? = runCatching {
        if (json.isNullOrBlank()) null else gson.fromJson(json, DownloadEntity::class.java)
    }.getOrNull()

    private fun updateForegroundNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("NEXAPREMIUM Downloads")
            .setContentText(message)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "NEXAPREMIUM Downloads", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.nexapremium.download.START"
        const val ACTION_PAUSE = "com.nexapremium.download.PAUSE"
        const val ACTION_RESUME = "com.nexapremium.download.RESUME"
        const val ACTION_RECOVER_PENDING = "com.nexapremium.download.RECOVER_PENDING"
        const val EXTRA_ENTITY = "download_entity"
        const val EXTRA_REQUEST = "download_request"
        const val EXTRA_ID = "download_id"
        private const val CHANNEL_ID = "nexa_downloads"
        private const val NOTIFICATION_ID = 7401
    }
}
