package com.nexapremium.data.download

import android.content.Context
import android.os.StatFs
import java.io.File

object DownloadStorage {
    fun directory(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, ".downloads")

    fun availableBytes(context: Context): Long? {
        val storageRoot = directory(context).parentFile ?: context.filesDir
        return runCatching { StatFs(storageRoot.absolutePath).availableBytes }.getOrNull()
    }
}
