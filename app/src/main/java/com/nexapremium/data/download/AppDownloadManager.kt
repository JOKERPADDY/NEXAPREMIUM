package com.nexapremium.data.download

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.nexapremium.data.db.entity.DownloadEntity
import com.nexapremium.data.repository.DownloadPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

enum class TransferStatus { QUEUED, CONNECTING, DOWNLOADING, WAITING_FOR_WIFI, PAUSED, COMPLETED, FAILED }

data class TransferProgress(
    val status: TransferStatus,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val error: String? = null
)

internal data class SavedDownloadRequest(
    val url: String = "",
    val headers: Map<String, String> = emptyMap(),
    val lastFailure: String? = null,
    val manuallyPaused: Boolean = false
)

@Singleton
class AppDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
    private val downloadPreferences: DownloadPreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val jobs = ConcurrentHashMap<String, Job>()
    private val calls = ConcurrentHashMap<String, okhttp3.Call>()
    private data class PendingTransfer(val entity: DownloadEntity, val request: SavedDownloadRequest)
    private val queueLock = Any()
    private val pendingTransfers = mutableListOf<PendingTransfer>()
    private val activeTransferIds = mutableSetOf<String>()
    private val pausedIds = ConcurrentHashMap.newKeySet<String>()
    private val cancelledIds = ConcurrentHashMap.newKeySet<String>()
    private val mutableProgress = MutableStateFlow<Map<String, TransferProgress>>(emptyMap())
    val progress = mutableProgress.asStateFlow()
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (downloadPreferences.wifiOnly.value && isWifiConnected()) onWifiMayStartDownloads()
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    if (downloadPreferences.wifiOnly.value && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        onWifiMayStartDownloads()
                    }
                }

                override fun onLost(network: Network) {
                    if (downloadPreferences.wifiOnly.value && !isWifiConnected()) pauseActiveForWifiOnly()
                }
            })
        }
        scope.launch {
            downloadPreferences.wifiOnly.collect { wifiOnly ->
                if (wifiOnly && !isWifiConnected()) pauseActiveForWifiOnly()
                else synchronized(queueLock) { startNextQueuedLocked() }
            }
        }
    }

    fun enqueue(entity: DownloadEntity, url: String, headers: Map<String, String>) {
        val request = SavedDownloadRequest(url, headers)
        requestFile(entity).apply {
            parentFile?.mkdirs()
            writeText(gson.toJson(request))
        }
        val intent = Intent(context, AppDownloadService::class.java).apply {
            action = AppDownloadService.ACTION_START
            putExtra(AppDownloadService.EXTRA_ENTITY, gson.toJson(entity))
            putExtra(AppDownloadService.EXTRA_REQUEST, gson.toJson(request))
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun pause(id: String, userInitiated: Boolean = true) {
        synchronized(queueLock) {
            pendingTransfers.removeAll { it.entity.id == id }
        }
        val entityFile = mutableProgress.value[id]
        pausedIds.add(id)
        calls[id]?.cancel()
        jobs[id]?.cancel()
        val entity = progressEntityFiles[id]
        if (entity != null && userInitiated) {
            runCatching {
                val request = gson.fromJson(requestFile(entity).readText(), SavedDownloadRequest::class.java)
                persistRequest(entity, request.copy(manuallyPaused = true))
            }
        }
        val partial = entity?.let { partialFile(it) }
        val bytes = partial?.takeIf(File::exists)?.length() ?: entityFile?.bytesDownloaded ?: 0L
        setProgress(id, TransferProgress(TransferStatus.PAUSED, bytes, entity?.expectedBytes() ?: entityFile?.totalBytes ?: 0L))
        synchronized(queueLock) { startNextQueuedLocked() }
    }

    fun resume(entity: DownloadEntity) {
        val intent = Intent(context, AppDownloadService::class.java).apply {
            action = AppDownloadService.ACTION_RESUME
            putExtra(AppDownloadService.EXTRA_ENTITY, gson.toJson(entity))
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun cancel(entity: DownloadEntity) {
        synchronized(queueLock) {
            pendingTransfers.removeAll { it.entity.id == entity.id }
        }
        cancelledIds.add(entity.id)
        pausedIds.remove(entity.id)
        calls.remove(entity.id)?.cancel()
        jobs.remove(entity.id)?.cancel()
        mutableProgress.update { it - entity.id }
        progressEntityFiles.remove(entity.id)
        partialFile(entity).delete()
        File(entity.filePath).delete()
        requestFile(entity).delete()
        synchronized(queueLock) { startNextQueuedLocked() }
    }

    fun hasResumeData(entity: DownloadEntity): Boolean = requestFile(entity).isFile

    fun failureReason(entity: DownloadEntity): String? = runCatching {
        gson.fromJson(requestFile(entity).readText(), SavedDownloadRequest::class.java).lastFailure
    }.getOrNull()

    fun partialBytes(entity: DownloadEntity): Long = partialFile(entity).takeIf(File::exists)?.length() ?: 0L

    internal fun startTransfer(entity: DownloadEntity, request: SavedDownloadRequest) {
        if (request.url.isBlank()) {
            persistRequest(entity, request.copy(lastFailure = "Missing download URL"))
            setProgress(entity.id, TransferProgress(TransferStatus.FAILED, error = "Missing download URL"))
            return
        }
        synchronized(queueLock) {
            if (entity.id in activeTransferIds || pendingTransfers.any { it.entity.id == entity.id }) return
            pendingTransfers.add(PendingTransfer(entity, request.copy(lastFailure = null)))
            progressEntityFiles[entity.id] = entity
            setProgress(entity.id, TransferProgress(TransferStatus.QUEUED, partialBytes(entity), entity.expectedBytes()))
            startNextQueuedLocked()
        }
    }

    fun reorderQueuedDownloads(idsInOrder: List<String>) {
        synchronized(queueLock) {
            val byId = pendingTransfers.associateBy { it.entity.id }
            val reordered = idsInOrder.mapNotNull(byId::get).toMutableList()
            pendingTransfers.forEach { item ->
                if (item.entity.id !in idsInOrder) reordered.add(item)
            }
            pendingTransfers.clear()
            pendingTransfers.addAll(reordered)
            startNextQueuedLocked()
        }
    }

    private fun startNextQueuedLocked() {
        if (activeTransferIds.isNotEmpty() || pendingTransfers.isEmpty()) return
        if (downloadPreferences.wifiOnly.value && !isWifiConnected()) {
            pendingTransfers.forEach { pending ->
                setProgress(
                    pending.entity.id,
                    TransferProgress(TransferStatus.WAITING_FOR_WIFI, partialBytes(pending.entity), pending.entity.expectedBytes())
                )
            }
            return
        }

        val next = pendingTransfers.removeAt(0)
        activeTransferIds.add(next.entity.id)
        setProgress(next.entity.id, TransferProgress(TransferStatus.CONNECTING, partialBytes(next.entity), next.entity.expectedBytes()))
        launchTransfer(next.entity, next.request)
    }

    private fun launchTransfer(entity: DownloadEntity, request: SavedDownloadRequest) {
        persistRequest(entity, request.copy(lastFailure = null))
        cancelledIds.remove(entity.id)
        pausedIds.remove(entity.id)

        val job = scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            val part = partialFile(entity)
            val destination = File(entity.filePath)
            part.parentFile?.mkdirs()
            var call: okhttp3.Call? = null
            try {
                val offset = part.takeIf(File::exists)?.length() ?: 0L
                val builder = Request.Builder().url(request.url).header("Accept-Encoding", "identity")
                request.headers.forEach { (key, value) -> builder.header(key, value) }
                if (offset > 0L) builder.header("Range", "bytes=$offset-")
                call = client.newCall(builder.build())
                calls[entity.id] = call
                val response = call.execute()
                response.use { res ->
                    if (res.code == 416 && offset > 0L && entity.expectedBytes() == offset) {
                        moveIntoPlace(part, destination)
                        setProgress(entity.id, TransferProgress(TransferStatus.COMPLETED, offset, offset))
                        requestFile(entity).delete()
                        return@use
                    }
                    if (!res.isSuccessful) throw IOException("Server returned HTTP ${res.code}")
                    val body = res.body ?: throw IOException("Empty download response")
                    val append = offset > 0L && res.code == 206
                    val initialBytes = if (append) offset else 0L
                    val contentLength = body.contentLength().coerceAtLeast(0L)
                    val total = if (contentLength > 0L) contentLength + initialBytes else entity.expectedBytes()
                    var downloaded = initialBytes
                    var lastUpdate = 0L
                    setProgress(entity.id, TransferProgress(TransferStatus.DOWNLOADING, downloaded, total))
                    body.byteStream().use { input ->
                        FileOutputStream(part, append).use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 16)
                            while (true) {
                                coroutineContext.ensureActive()
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                val now = System.currentTimeMillis()
                                if (now - lastUpdate >= 250L) {
                                    setProgress(entity.id, TransferProgress(TransferStatus.DOWNLOADING, downloaded, total))
                                    lastUpdate = now
                                }
                            }
                            output.fd.sync()
                        }
                    }
                    moveIntoPlace(part, destination)
                    setProgress(entity.id, TransferProgress(TransferStatus.COMPLETED, downloaded, total.takeIf { it > 0 } ?: downloaded))
                    requestFile(entity).delete()
                }
            } catch (cancelled: CancellationException) {
                if (entity.id in pausedIds) {
                    setProgress(entity.id, TransferProgress(TransferStatus.PAUSED, partialBytes(entity), entity.expectedBytes()))
                }
                throw cancelled
            } catch (error: Exception) {
                when {
                    entity.id in cancelledIds -> Unit
                    entity.id in pausedIds -> setProgress(entity.id, TransferProgress(TransferStatus.PAUSED, partialBytes(entity), entity.expectedBytes()))
                    else -> {
                        persistRequest(entity, request.copy(lastFailure = error.message ?: "Download failed"))
                        setProgress(entity.id, TransferProgress(TransferStatus.FAILED, partialBytes(entity), entity.expectedBytes(), error.message))
                    }
                }
            } finally {
                call?.let { calls.remove(entity.id, it) }
                synchronized(queueLock) {
                    coroutineContext[Job]?.let { jobs.remove(entity.id, it) }
                    progressEntityFiles.remove(entity.id)
                    activeTransferIds.remove(entity.id)
                    startNextQueuedLocked()
                }
            }
        }
        jobs[entity.id] = job
        job.start()
    }

    internal fun pauseTransfer(id: String) = pause(id)

    internal fun resumeTransfer(entity: DownloadEntity, forceResume: Boolean = false) {
        val savedRequest = runCatching {
            gson.fromJson(requestFile(entity).readText(), SavedDownloadRequest::class.java)
        }.getOrNull()
        if (savedRequest == null || (savedRequest.manuallyPaused && !forceResume)) {
            if (savedRequest == null) {
            setProgress(entity.id, TransferProgress(TransferStatus.FAILED, partialBytes(entity), entity.expectedBytes(), "Cannot resume this download"))
            }
        } else {
            val request = savedRequest.copy(manuallyPaused = false)
            persistRequest(entity, request)
            val activeJob = synchronized(queueLock) {
                if (entity.id in activeTransferIds && entity.id in pausedIds) jobs[entity.id]
                else null
            }
            val alreadyRunning = synchronized(queueLock) { entity.id in activeTransferIds && entity.id !in pausedIds }
            if (alreadyRunning) return
            if (activeJob != null) {
                scope.launch {
                    activeJob.join()
                    startTransfer(entity, request)
                }
            } else {
                startTransfer(entity, request)
            }
        }
    }

    internal fun isRunning(status: TransferStatus): Boolean =
        status == TransferStatus.QUEUED || status == TransferStatus.CONNECTING || status == TransferStatus.DOWNLOADING

    private fun DownloadEntity.expectedBytes(): Long = (fileSizeMb * 1024.0 * 1024.0).toLong().coerceAtLeast(0L)

    private fun requestFile(entity: DownloadEntity) = File("${entity.filePath}.request.json")
    private fun partialFile(entity: DownloadEntity) = File("${entity.filePath}.part")

    private fun persistRequest(entity: DownloadEntity, request: SavedDownloadRequest) {
        runCatching {
            requestFile(entity).apply {
                parentFile?.mkdirs()
                writeText(gson.toJson(request))
            }
        }
    }

    private fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun pauseActiveForWifiOnly() {
        val activeIds = synchronized(queueLock) { activeTransferIds.toList() }
        activeIds.forEach { pause(it, userInitiated = false) }
        synchronized(queueLock) { startNextQueuedLocked() }
    }

    private fun onWifiMayStartDownloads() {
        synchronized(queueLock) { startNextQueuedLocked() }
        if (!downloadPreferences.wifiOnly.value) return
        val intent = Intent(context, AppDownloadService::class.java).apply {
            action = AppDownloadService.ACTION_RECOVER_PENDING
        }
        runCatching { ContextCompat.startForegroundService(context, intent) }
    }

    private fun setProgress(id: String, value: TransferProgress) {
        mutableProgress.update { it + (id to value) }
    }

    private fun moveIntoPlace(part: File, destination: File) {
        destination.parentFile?.mkdirs()
        Files.move(part.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    private val progressEntityFiles = ConcurrentHashMap<String, DownloadEntity>()
}
