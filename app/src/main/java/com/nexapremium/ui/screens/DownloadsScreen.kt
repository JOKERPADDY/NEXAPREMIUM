package com.nexapremium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nexapremium.core.ui.theme.*
import com.nexapremium.ui.viewmodel.DownloadItemState
import com.nexapremium.ui.viewmodel.DownloadsViewModel

@Composable
fun DownloadsScreen(
    onItemClick: (String) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val downloads by viewModel.downloadStates.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DownloadDone,
                contentDescription = null,
                tint = PrimaryRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Smart Downloads",
                style = Typography.headlineMedium,
                color = PureWhite
            )
        }
        val downloadedBytes = downloads.sumOf { it.bytesDownloaded }
        Text(
            text = "${downloads.count { it.isCompleted }} ready · ${downloads.count { it.isDownloading }} active · ${String.format("%.1f MB", downloadedBytes / (1024.0 * 1024.0))} used",
            style = Typography.bodySmall,
            color = MutedGray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = MutedGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Downloads Yet",
                        style = Typography.titleLarge,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Movies and series you download will appear here with live progress for offline viewing.",
                        style = Typography.bodyMedium,
                        color = MutedGray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(downloads, key = { it.entity.id }) { itemState ->
                    val item = itemState.entity

                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Poster Thumbnail
                            AsyncImage(
                                model = item.coverUrl,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(70.dp, 100.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            // Details & Download Progress Column
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = Typography.titleMedium,
                                    color = PureWhite
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = SecondarySurface,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = item.quality,
                                            color = PrimaryRed,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Surface(
                                        color = SecondarySurface,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                itemState.totalBytes > 0 -> String.format("%.1f MB", itemState.totalBytes / (1024.0 * 1024.0))
                                                item.fileSizeMb > 0.0 -> String.format("%.1f MB", item.fileSizeMb)
                                                else -> "Size unavailable"
                                            },
                                            color = MutedGray,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Active Download Progress Bar & Status Text
                                if (itemState.isDownloading) {
                                    LinearProgressIndicator(
                                        progress = { itemState.progress / 100f },
                                        color = PrimaryRed,
                                        trackColor = PureWhite.copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = itemState.statusText,
                                        color = PrimaryRed,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${String.format("%.1f", itemState.bytesDownloaded / (1024.0 * 1024.0))} MB downloaded" +
                                            if (itemState.totalBytes > 0L) " of ${String.format("%.1f", itemState.totalBytes / (1024.0 * 1024.0))} MB" else "",
                                        color = MutedGray,
                                        fontSize = 10.sp
                                    )
                                } else {
                                    Text(
                                        text = if (itemState.isCompleted) "Ready for Offline Viewing" else itemState.statusText,
                                        color = if (itemState.isCompleted) PrimaryRed else MutedGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Action Buttons
                            if (itemState.isCompleted) {
                                IconButton(
                                    onClick = { onItemClick(item.detailPath) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Offline",
                                        tint = PrimaryRed,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            } else {
                                // Pause / Resume button
                                IconButton(
                                    onClick = { viewModel.togglePauseDownload(itemState) }
                                ) {
                                    Icon(
                                        imageVector = if (itemState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (itemState.isPaused) "Resume" else "Pause",
                                        tint = PureWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.cancelOrDeleteDownload(itemState) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MutedGray,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
