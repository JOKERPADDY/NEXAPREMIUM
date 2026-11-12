package com.nexapremium.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Requests notification permission only when a user starts a download. Downloads continue if permission is denied. */
@Composable
fun rememberDownloadPermissionRequester(): ((() -> Unit) -> Unit) {
    val context = LocalContext.current
    val pendingAction = remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        pendingAction.value?.invoke()
        pendingAction.value = null
    }

    return remember(context, permissionLauncher) {
        { startDownload: () -> Unit ->
            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (needsPermission) {
                pendingAction.value = startDownload
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startDownload()
            }
        }
    }
}
