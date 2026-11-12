package com.nexapremium.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexapremium.core.ui.theme.*
import com.nexapremium.data.repository.DownloadPreferencesRepository
import com.nexapremium.data.repository.PlaybackPreferencesRepository
import com.nexapremium.data.update.AppUpdateChecker
import com.nexapremium.data.update.AppUpdateInfo
import com.nexapremium.ui.components.AppUpdateDialog
import com.nexapremium.ui.components.TelegramPromptDialog
import com.nexapremium.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val familyModeEnabled by viewModel.familyModeEnabled.collectAsState()
    val wifiOnlyDownloads by viewModel.wifiOnlyDownloads.collectAsState()
    val defaultDownloadQuality by viewModel.defaultDownloadQuality.collectAsState()
    val autoDeleteWatchedEpisodes by viewModel.autoDeleteWatchedEpisodes.collectAsState()
    val defaultPlaybackSpeed by viewModel.defaultPlaybackSpeed.collectAsState()
    val autoPictureInPicture by viewModel.autoPictureInPicture.collectAsState()
    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogContent by remember { mutableStateOf<String?>(null) }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var showTelegramDialog by remember { mutableStateOf(false) }
    var showDownloadQualityMenu by remember { mutableStateOf(false) }
    var showPlaybackSpeedMenu by remember { mutableStateOf(false) }

    if (infoDialogTitle != null && infoDialogContent != null) {
        AlertDialog(
            onDismissRequest = { infoDialogTitle = null },
            title = { Text(text = infoDialogTitle!!, color = PureWhite, style = Typography.titleLarge) },
            text = {
                Text(
                    text = infoDialogContent!!,
                    color = MutedGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { infoDialogTitle = null }) {
                    Text(text = "Close", color = PrimaryRed)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    updateInfo?.let { info ->
        AppUpdateDialog(updateInfo = info, onDismiss = { updateInfo = null })
    }

    if (showTelegramDialog) {
        TelegramPromptDialog(onDismiss = { showTelegramDialog = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(text = "Settings", style = Typography.headlineLarge, color = PureWhite)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Preferences", color = MutedGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text("Family Mode", color = PureWhite, fontSize = 16.sp)
                    Text(
                        "Uses available provider flags and tags to filter some mature titles. It may miss content or hide safe titles, so it is not a parental control.",
                        color = MutedGray,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
                Switch(
                    checked = familyModeEnabled,
                    onCheckedChange = viewModel::setFamilyModeEnabled,
                    colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = PrimaryRed)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Downloads & playback", color = MutedGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 10.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                PreferenceSwitchRow(
                    title = "Download over Wi-Fi only",
                    subtitle = "Wait for Wi-Fi before starting or resuming downloads.",
                    checked = wifiOnlyDownloads,
                    onCheckedChange = viewModel::setWifiOnlyDownloads
                )
                HorizontalDivider(color = SecondarySurface, thickness = 1.dp)
                Box {
                    SettingsActionRow(
                        title = "Default download quality",
                        subtitle = defaultDownloadQuality,
                        onClick = { showDownloadQualityMenu = true }
                    )
                    DropdownMenu(
                        expanded = showDownloadQualityMenu,
                        onDismissRequest = { showDownloadQualityMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DownloadPreferencesRepository.DOWNLOAD_QUALITIES.forEach { quality ->
                            DropdownMenuItem(
                                text = { Text(quality, color = PureWhite) },
                                onClick = {
                                    viewModel.setDefaultDownloadQuality(quality)
                                    showDownloadQualityMenu = false
                                }
                            )
                        }
                    }
                }
                HorizontalDivider(color = SecondarySurface, thickness = 1.dp)
                PreferenceSwitchRow(
                    title = "Delete watched episodes",
                    subtitle = "Remove a downloaded episode after playback finishes.",
                    checked = autoDeleteWatchedEpisodes,
                    onCheckedChange = viewModel::setAutoDeleteWatchedEpisodes
                )
                HorizontalDivider(color = SecondarySurface, thickness = 1.dp)
                PreferenceSwitchRow(
                    title = "Enter picture-in-picture automatically",
                    subtitle = "Keep playback in a small window when you leave the player.",
                    checked = autoPictureInPicture,
                    onCheckedChange = viewModel::setAutoPictureInPicture
                )
                HorizontalDivider(color = SecondarySurface, thickness = 1.dp)
                Box {
                    SettingsActionRow(
                        title = "Default playback speed",
                        subtitle = "${defaultPlaybackSpeed}x",
                        onClick = { showPlaybackSpeedMenu = true }
                    )
                    DropdownMenu(
                        expanded = showPlaybackSpeedMenu,
                        onDismissRequest = { showPlaybackSpeedMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        PlaybackPreferencesRepository.PLAYBACK_SPEEDS.forEach { speed ->
                            DropdownMenuItem(
                                text = { Text("${speed}x", color = PureWhite) },
                                onClick = {
                                    viewModel.setDefaultPlaybackSpeed(speed)
                                    showPlaybackSpeedMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("App information", color = MutedGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SettingsActionRow(
                    title = "Check for updates",
                    subtitle = if (isCheckingUpdate) "Checking…" else "Installed version ${viewModel.appVersion}",
                    onClick = {
                        if (!isCheckingUpdate) {
                            scope.launch {
                                isCheckingUpdate = true
                                val result = runCatching { AppUpdateChecker.checkUpdate(context) }
                                isCheckingUpdate = false
                                result.onSuccess { info ->
                                    when {
                                        info.checkFailed -> Toast.makeText(context, "Couldn't check for updates. Try again later.", Toast.LENGTH_LONG).show()
                                        info.hasUpdate -> updateInfo = info
                                        else -> Toast.makeText(context, "You're up to date (${info.currentVersion}).", Toast.LENGTH_SHORT).show()
                                    }
                                }.onFailure {
                                    Toast.makeText(context, "Couldn't check for updates. Try again later.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
                HorizontalDivider(color = SecondarySurface, thickness = 1.dp)
                SettingsActionRow(
                    title = "About NEXAPREMIUM",
                    subtitle = "How the app works",
                    onClick = {
                        infoDialogTitle = "About NEXAPREMIUM"
                        infoDialogContent = "NEXAPREMIUM is an Android app for browsing and playing catalog titles. Catalog and stream availability depends on the remote services configured for the app. Watchlist items, playback progress, and downloads are stored on this device."
                    }
                )
                HorizontalDivider(color = SecondarySurface, thickness = 1.dp)
                SettingsActionRow(
                    title = "Data & privacy",
                    subtitle = "What the app stores and sends",
                    onClick = {
                        infoDialogTitle = "Data & privacy"
                        infoDialogContent = "The app stores your watchlist, playback progress, downloads, and Family Mode preference on this device. There is no sign-in or cross-device sync. To load catalog pages, search, title details, playback links, and captions, the app sends requests to its configured remote services. Video and caption data may be served from remote provider hosts. Removing the app removes its private local data and downloads."
                    }
                )
                HorizontalDivider(color = SecondarySurface, thickness = 1.dp)
                SettingsActionRow(
                    title = "Usage terms",
                    subtitle = "Availability and permitted use",
                    onClick = {
                        infoDialogTitle = "Usage terms"
                        infoDialogContent = "Use this app and its connected services only where you are authorized to access them, and follow the applicable service terms and local rules. Catalog listings, playback links, and offline availability are provided by remote services and may change or stop working. NEXAPREMIUM does not provide an account or cloud backup for your watchlist, progress, or downloads."
                    }
                )
                HorizontalDivider(color = SecondarySurface, thickness = 1.dp)
                SettingsActionRow(
                    title = "Join Telegram Channel",
                    subtitle = "Get updates, movie releases & community support",
                    onClick = { showTelegramDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = PureWhite, fontSize = 16.sp)
            Text(subtitle, color = MutedGray, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = PrimaryRed)
        )
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, color = PureWhite, fontSize = 16.sp)
            Text(text = subtitle, color = MutedGray, fontSize = 12.sp)
        }
    }
}
