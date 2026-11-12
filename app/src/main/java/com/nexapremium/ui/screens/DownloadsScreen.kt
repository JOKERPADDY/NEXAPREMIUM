package com.nexapremium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nexapremium.core.ui.theme.*
import com.nexapremium.data.download.DownloadStorage
import com.nexapremium.ui.components.TvIconButton
import com.nexapremium.ui.viewmodel.DownloadItemState
import com.nexapremium.ui.viewmodel.DownloadsViewModel
import java.util.Locale

private enum class DownloadFilter(val title: String) {
    ALL("All"),
    ACTIVE("Active"),
    DOWNLOADED("Downloaded"),
    FAILED("Failed")
}

@Composable
fun DownloadsScreen(
    onItemClick: (String, Int, Int, String) -> Unit,
    onBrowseClick: () -> Unit,
    onOpenTitle: (String, String) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val downloads by viewModel.downloadStates.collectAsState()
    var selectedFilter by remember { mutableStateOf(DownloadFilter.ALL) }
    var pendingDelete by remember { mutableStateOf<DownloadItemState?>(null) }
    var confirmClearCompleted by remember { mutableStateOf(false) }

    val usedBytes = downloads.sumOf { it.bytesDownloaded }
    val availableBytes = remember(downloads) { DownloadStorage.availableBytes(context) }
    val activeCount = downloads.count { it.isDownloading || it.isQueued || (it.isPaused && !it.isFailed) }
    val downloadedCount = downloads.count { it.isCompleted }
    val failedCount = downloads.count { it.isFailed }
    val visibleDownloads = when (selectedFilter) {
        DownloadFilter.ALL -> downloads
        DownloadFilter.ACTIVE -> downloads.filter { it.isDownloading || it.isQueued || (it.isPaused && !it.isFailed) }
        DownloadFilter.DOWNLOADED -> downloads.filter { it.isCompleted }
        DownloadFilter.FAILED -> downloads.filter { it.isFailed }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete download?", color = PureWhite) },
            text = {
                Text(
                    "Remove ${cleanDownloadTitle(item.entity.title, item.entity.quality)} and its local file from this device?",
                    color = MutedGray
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelOrDeleteDownload(item)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel", color = PureWhite) }
            },
            containerColor = DarkSurface
        )
    }

    if (confirmClearCompleted) {
        AlertDialog(
            onDismissRequest = { confirmClearCompleted = false },
            title = { Text("Clear completed downloads?", color = PureWhite) },
            text = {
                Text(
                    "Remove $downloadedCount completed ${if (downloadedCount == 1) "file" else "files"} from this device? They will no longer be available offline.",
                    color = MutedGray
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCompletedDownloads(downloads.filter { it.isCompleted })
                    confirmClearCompleted = false
                }) { Text("Clear completed", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearCompleted = false }) { Text("Cancel", color = PureWhite) }
            },
            containerColor = DarkSurface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DownloadDone,
                contentDescription = null,
                tint = PrimaryRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Downloads", style = Typography.headlineMedium, color = PureWhite)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${downloads.size} items · $activeCount active · $downloadedCount ready · $failedCount failed",
                        style = Typography.bodyMedium,
                        color = PureWhite,
                        modifier = Modifier.weight(1f)
                    )
                    if (downloadedCount > 0) {
                        TextButton(
                            onClick = { confirmClearCompleted = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Clear completed", color = PrimaryRed, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatBytes(usedBytes)} used by downloads · ${availableBytes?.let(::formatBytes) ?: "Unavailable"} free",
                    style = Typography.bodySmall,
                    color = MutedGray
                )
            }
        }

        if (downloads.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DownloadFilter.values().toList()) { filter ->
                    val count = when (filter) {
                        DownloadFilter.ALL -> downloads.size
                        DownloadFilter.ACTIVE -> activeCount
                        DownloadFilter.DOWNLOADED -> downloadedCount
                        DownloadFilter.FAILED -> failedCount
                    }
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text("${filter.title} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryRed,
                            selectedLabelColor = PureWhite
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            downloads.isEmpty() -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = MutedGray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No downloads yet", style = Typography.titleLarge, color = PureWhite)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Browse movies and series, then choose Download on a title page.",
                            style = Typography.bodyMedium,
                            color = MutedGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onBrowseClick,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                        ) {
                            Text("Browse titles", color = PureWhite)
                        }
                    }
                }
            }
            visibleDownloads.isEmpty() -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No ${selectedFilter.title.lowercase(Locale.getDefault())} downloads", color = MutedGray)
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(visibleDownloads, key = { it.entity.id }) { itemState ->
                        val queue = downloads.filter { it.isQueued }.sortedByDescending { it.entity.timestamp }
                        val queueIndex = queue.indexOfFirst { it.entity.id == itemState.entity.id }
                        DownloadRow(
                            itemState = itemState,
                            onOpen = { onItemClick(itemState.entity.detailPath, itemStateSeason(itemState), itemStateEpisode(itemState), itemState.entity.coverUrl) },
                            onPauseResume = { viewModel.togglePauseDownload(itemState) },
                            onMoveUp = { viewModel.moveQueuedDownload(itemState.entity.id, -1) },
                            onMoveDown = { viewModel.moveQueuedDownload(itemState.entity.id, 1) },
                            canMoveUp = queueIndex > 0,
                            canMoveDown = queueIndex >= 0 && queueIndex < queue.lastIndex,
                            onRetry = { viewModel.retryDownload(itemState) },
                            onDelete = { pendingDelete = itemState },
                            onOpenTitle = {
                                val path = itemState.entity.detailPath
                                onOpenTitle(path, downloadSubjectType(path))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(
    itemState: DownloadItemState,
    onOpen: () -> Unit,
    onPauseResume: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onOpenTitle: () -> Unit
) {
    val item = itemState.entity
    val (seasonNumber, episodeNumber) = downloadEpisodeNumbers(item.id)
    val cleanTitle = cleanDownloadTitle(item.title, item.quality)

    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (itemState.isCompleted) Modifier.clickable(onClick = onOpen) else Modifier)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = cleanTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(70.dp, 100.dp).clip(RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (episodeNumber > 0) "$cleanTitle · S$seasonNumber E$episodeNumber" else cleanTitle,
                    style = Typography.titleMedium,
                    color = PureWhite,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DownloadMetaChip(item.quality, PrimaryRed)
                    DownloadMetaChip(downloadSizeLabel(itemState), MutedGray)
                }
                Spacer(modifier = Modifier.height(7.dp))

                if (itemState.isDownloading) {
                    LinearProgressIndicator(
                        progress = { itemState.progress / 100f },
                        color = PrimaryRed,
                        trackColor = PureWhite.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(itemState.statusText, color = PrimaryRed, fontSize = 11.sp)
                    Text(
                        text = if (itemState.totalBytes > 0L) {
                            "${formatBytes(itemState.bytesDownloaded)} of ${formatBytes(itemState.totalBytes)} downloaded"
                        } else {
                            "${formatBytes(itemState.bytesDownloaded)} downloaded"
                        },
                        color = MutedGray,
                        fontSize = 10.sp
                    )
                } else {
                    Text(
                        text = when {
                            itemState.isCompleted -> "Ready for offline playback"
                            itemState.isFailed -> itemState.statusText
                            else -> "${itemState.statusText} · ${formatBytes(itemState.bytesDownloaded)} downloaded"
                        },
                        color = when {
                            itemState.isCompleted -> PrimaryRed
                            itemState.isFailed -> MaterialTheme.colorScheme.error
                            else -> MutedGray
                        },
                        fontSize = 11.sp,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            when {
                itemState.isCompleted -> TvIconButton(
                    image = Icons.Default.PlayArrow,
                    contentDescription = "Play offline",
                    onClick = onOpen,
                    tint = PrimaryRed,
                    buttonSize = 44.dp,
                    iconSize = 28.dp
                )
                itemState.isFailed && itemState.canRetry -> TvIconButton(
                    image = Icons.Default.Refresh,
                    contentDescription = "Retry download",
                    onClick = onRetry,
                    tint = PrimaryRed,
                    buttonSize = 44.dp,
                    iconSize = 23.dp
                )
                itemState.isFailed -> TvIconButton(
                    image = Icons.Default.Download,
                    contentDescription = "Open title to download again",
                    onClick = onOpenTitle,
                    tint = PrimaryRed,
                    buttonSize = 44.dp,
                    iconSize = 22.dp
                )
                !itemState.isFailed -> TvIconButton(
                    image = if (itemState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (itemState.isPaused) "Resume download" else if (itemState.isQueued) "Pause queued download" else "Pause download",
                    onClick = onPauseResume,
                    buttonSize = 44.dp,
                    iconSize = 22.dp
                )
            }

            if (itemState.isQueued && !itemState.isFailed) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    TvIconButton(
                        image = Icons.Default.ArrowUpward,
                        contentDescription = "Move download up in queue",
                        onClick = { if (canMoveUp) onMoveUp() },
                        tint = PureWhite.copy(alpha = if (canMoveUp) 1f else 0.3f),
                        buttonSize = 30.dp,
                        iconSize = 17.dp
                    )
                    TvIconButton(
                        image = Icons.Default.ArrowDownward,
                        contentDescription = "Move download down in queue",
                        onClick = { if (canMoveDown) onMoveDown() },
                        tint = PureWhite.copy(alpha = if (canMoveDown) 1f else 0.3f),
                        buttonSize = 30.dp,
                        iconSize = 17.dp
                    )
                }
            }

            TvIconButton(
                image = Icons.Default.Delete,
                contentDescription = "Delete download",
                onClick = onDelete,
                tint = MutedGray,
                buttonSize = 44.dp,
                iconSize = 22.dp
            )
        }
    }
}

@Composable
private fun DownloadMetaChip(text: String, textColor: androidx.compose.ui.graphics.Color) {
    Surface(color = SecondarySurface, shape = RoundedCornerShape(4.dp)) {
        Text(text, color = textColor, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

private fun downloadSizeLabel(state: DownloadItemState): String = when {
    state.isCompleted -> formatBytes(state.bytesDownloaded)
    state.totalBytes > 0L -> formatBytes(state.totalBytes)
    state.bytesDownloaded > 0L -> "${formatBytes(state.bytesDownloaded)} so far"
    else -> "Size unknown"
}

private fun downloadEpisodeNumbers(id: String): Pair<Int, Int> {
    val match = Regex("_S(\\d+)E(\\d+)$", RegexOption.IGNORE_CASE).find(id)
    return (match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0) to
        (match?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0)
}

private fun itemStateSeason(state: DownloadItemState): Int = downloadEpisodeNumbers(state.entity.id).first
private fun itemStateEpisode(state: DownloadItemState): Int = downloadEpisodeNumbers(state.entity.id).second

private fun cleanDownloadTitle(title: String, quality: String): String = title
    .replace(Regex("\\s+\\(${Regex.escape(quality)}\\)$", RegexOption.IGNORE_CASE), "")
    .replace(Regex("_S\\d+E\\d+$", RegexOption.IGNORE_CASE), "")

private fun downloadSubjectType(detailPath: String): String =
    if (detailPath.trimStart('/').startsWith("series/", ignoreCase = true)) "series" else "movie"

private fun formatBytes(bytes: Long): String {
    val value = bytes.coerceAtLeast(0L).toDouble()
    return when {
        value < 1024.0 -> "${bytes.coerceAtLeast(0L)} B"
        value < 1024.0 * 1024.0 -> String.format(Locale.getDefault(), "%.1f KB", value / 1024.0)
        value < 1024.0 * 1024.0 * 1024.0 -> String.format(Locale.getDefault(), "%.1f MB", value / (1024.0 * 1024.0))
        else -> String.format(Locale.getDefault(), "%.2f GB", value / (1024.0 * 1024.0 * 1024.0))
    }
}
