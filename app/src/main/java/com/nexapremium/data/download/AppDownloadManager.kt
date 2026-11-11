package com.nexapremium.data.download

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.nexapremium.data.db.entity.DownloadEntity
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

enum class TransferStatus { QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED }

data class TransferProgress(
    val status: TransferStatus,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val error: String? = null
)

internal data class SavedDownloadRequest(
    val url: String = "",
    val headers: Map<String, String> = emptyMap()
)

@Singleton
class AppDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val jobs = ConcurrentHashMap<String, Job>()
    private val calls = ConcurrentHashMap<String, okhttp3.Call>()
    private val pausedIds = ConcurrentHashMap.newKeySet<String>()
    private val cancelledIds = ConcurrentHashMap.newKeySet<String>()
    private val mutableProgress = MutableStateFlow<Map<String, TransferProgress>>(emptyMap())
    val progress = mutableProgress.asStateFlow()

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

    fun pause(id: String) {
        val entityFile = mutableProgress.value[id]
        pausedIds.add(id)
        calls[id]?.cancel()
        jobs[id]?.cancel()
        val entity = progressEntityFiles[id]
        val partial = entity?.let { partialFile(it) }
        val bytes = partial?.takeIf(File::exists)?.length() ?: entityFile?.bytesDownloaded ?: 0L
        setProgress(id, TransferProgress(TransferStatus.PAUSED, bytes, entity?.expectedBytes() ?: entityFile?.totalBytes ?: 0L))
    }

    fun resume(entity: DownloadEntity) {
        val intent = Intent(context, AppDownloadService::class.java).apply {
            action = AppDownloadService.ACTION_RESUME
            putExtra(AppDownloadService.EXTRA_ENTITY, gson.toJson(entity))
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun cancel(entity: DownloadEntity) {
        cancelledIds.add(entity.id)
        pausedIds.remove(entity.id)
        calls.remove(entity.id)?.cancel()
        jobs.remove(entity.id)?.cancel()
        mutableProgress.update { it - entity.id }
        progressEntityFiles.remove(entity.id)
        partialFile(entity).delete()
        File(entity.filePath).delete()
        requestFile(entity).delete()
    }

    fun hasResumeData(entity: DownloadEntity): Boolean = requestFile(entity).isFile

    fun partialBytes(entity: DownloadEntity): Long = partialFile(entity).takeIf(File::exists)?.length() ?: 0L

    internal fun startTransfer(entity: DownloadEntity, request: SavedDownloadRequest) {
        if (request.url.isBlank()) {
            setProgress(entity.id, TransferProgress(TransferStatus.FAILED, error = "Missing download URL"))
            return
        }
        if (jobs[entity.id]?.isActive == true) return

        progressEntityFiles[entity.id] = entity
        cancelledIds.remove(entity.id)
        pausedIds.remove(entity.id)
        setProgress(entity.id, TransferProgress(
            TransferStatus.QUEUED,
            partialBytes(entity),
            entity.expectedBytes()
        ))

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
                    else -> setProgress(entity.id, TransferProgress(TransferStatus.FAILED, partialBytes(entity), entity.expectedBytes(), error.message))
                }
            } finally {
                call?.let { calls.remove(entity.id, it) }
                coroutineContext[Job]?.let { jobs.remove(entity.id, it) }
                progressEntityFiles.remove(entity.id)
            }
        }
        jobs[entity.id] = job
        job.start()
    }

    internal fun pauseTransfer(id: String) = pause(id)

    internal fun resumeTransfer(entity: DownloadEntity) {
        val request = runCatching {
            gson.fromJson(requestFile(entity).readText(), SavedDownloadRequest::class.java)
        }.getOrNull()
        if (request == null) {
            setProgress(entity.id, TransferProgress(TransferStatus.FAILED, partialBytes(entity), entity.expectedBytes(), "Cannot resume this download"))
        } else {
            setProgress(entity.id, TransferProgress(TransferStatus.QUEUED, partialBytes(entity), entity.expectedBytes()))
            scope.launch {
                jobs[entity.id]?.join()
                startTransfer(entity, request)
            }
        }
    }

    internal fun isRunning(status: TransferStatus): Boolean =
        status == TransferStatus.QUEUED || status == TransferStatus.DOWNLOADING

    private fun DownloadEntity.expectedBytes(): Long = (fileSizeMb * 1024.0 * 1024.0).toLong().coerceAtLeast(0L)

    private fun requestFile(entity: DownloadEntity) = File("${entity.filePath}.request.json")
    private fun partialFile(entity: DownloadEntity) = File("${entity.filePath}.part")

    private fun setProgress(id: String, value: TransferProgress) {
        mutableProgress.update { it + (id to value) }
    }

    private fun moveIntoPlace(part: File, destination: File) {
        destination.parentFile?.mkdirs()
        Files.move(part.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    private val progressEntityFiles = ConcurrentHashMap<String, DownloadEntity>()
}
