package com.nexapremium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapremium.core.ui.theme.*
import com.nexapremium.data.api.model.EpisodeDto
import com.nexapremium.data.api.model.MediaSourceDto
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadBottomSheet(
    titleName: String,
    seasonNumber: Int,
    episodes: List<EpisodeDto>,
    availableQualities: List<String> = listOf("360P", "480P", "1080P"),
    playbackSources: List<MediaSourceDto> = emptyList(),
    episodePlaybackSources: Map<Int, List<MediaSourceDto>> = emptyMap(),
    episodeSourcesLoading: Boolean = false,
    isMovie: Boolean = false,
    onDismiss: () -> Unit,
    onDownloadEpisodes: (List<Int>, String, String?) -> Unit
) {
    val sourceQualities = playbackSources.map { it.effectiveQuality }.distinct()
    val qualities = when {
        sourceQualities.isNotEmpty() -> sourceQualities
        isMovie -> emptyList()
        availableQualities.isNotEmpty() -> availableQualities
        else -> listOf("360P", "480P", "1080P")
    }
    var selectedQuality by remember { mutableStateOf("") }
    var selectedSourceId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(qualities, playbackSources) {
        if (isMovie) {
            if (playbackSources.none { it.id == selectedSourceId }) {
                selectedSourceId = playbackSources.firstOrNull()?.id
            }
            selectedQuality = playbackSources.firstOrNull { it.id == selectedSourceId }?.effectiveQuality.orEmpty()
        } else if (selectedQuality !in qualities) {
            selectedQuality = qualities.firstOrNull().orEmpty()
        }
    }
    val selectedSource = playbackSources.firstOrNull { it.id == selectedSourceId }
        ?: playbackSources.firstOrNull {
            it.effectiveQuality.trim().uppercase().removeSuffix("P") == selectedQuality.trim().uppercase().removeSuffix("P")
        }
    val selectedSizeText = formatDownloadSize(selectedSource?.sizeBytes) ?: "Size unavailable"
    val selectedEpisodes = remember { mutableStateMapOf<Int, Boolean>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Download - $titleName",
                    style = Typography.titleLarge,
                    color = PureWhite
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = PureWhite)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (qualities.isEmpty()) {
                Text("No direct MP4 download sources available", color = MutedGray, fontSize = 13.sp)
            }
            if (!isMovie && episodeSourcesLoading) {
                Text("Loading episode sizes...", color = MutedGray, fontSize = 12.sp)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                qualities.forEach { quality ->
                    val isSelected = selectedQuality == quality
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(
                                color = if (isSelected) Color(0xFF3A1E1E) else Color(0xFF222222),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) PrimaryRed else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedQuality = quality
                                selectedSourceId = playbackSources.firstOrNull { it.effectiveQuality == quality }?.id
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = quality,
                            color = if (isSelected) PrimaryRed else PureWhite,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val displayEpisodes = if (isMovie) {
                listOf(EpisodeDto(episodeNumber = 1, title = titleName))
            } else if (episodes.isEmpty()) {
                listOf(EpisodeDto(episodeNumber = 1, title = "Episode 1"))
            } else episodes

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
            ) {
                items(displayEpisodes) { ep ->
                    val epNum = ep.effectiveEpisodeNumber
                    val isChecked = selectedEpisodes[epNum] ?: true // Default checked
                    val episodeSource = episodePlaybackSources[epNum]?.let { sources ->
                        sourceForQuality(sources, selectedQuality)
                    }
                    val sizeText = if (isMovie) selectedSizeText else if (episodeSourcesLoading && episodeSource == null) {
                        "Checking size..."
                    } else {
                        formatDownloadSize(episodeSource?.sizeBytes) ?: "Size unavailable"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedEpisodes[epNum] = !(selectedEpisodes[epNum] ?: true) }
                            .padding(vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedEpisodes[epNum] != false) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (selectedEpisodes[epNum] != false) PrimaryRed else MutedGray,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isMovie) "$titleName · $selectedQuality" else "S${String.format("%02d", seasonNumber)} E${String.format("%02d", epNum)} · ${ep.title ?: "Episode $epNum"}",
                                color = PureWhite,
                                fontSize = 14.sp
                            )
                            Text(
                                text = sizeText,
                                color = MutedGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val allSelected = displayEpisodes.all { selectedEpisodes[it.effectiveEpisodeNumber] != false }
                TextButton(
                    onClick = {
                        val targetState = !allSelected
                        displayEpisodes.forEach { ep -> selectedEpisodes[ep.effectiveEpisodeNumber] = targetState }
                    }
                ) {
                    Text(
                        text = if (allSelected) "Deselect All" else "Select All",
                        color = PureWhite
                    )
                }

                val selectedEpisodeNumbers = displayEpisodes
                    .filter { selectedEpisodes[it.effectiveEpisodeNumber] != false }
                    .map { it.effectiveEpisodeNumber }
                val count = selectedEpisodeNumbers.size
                val selectedEpisodeSources = selectedEpisodeNumbers.map { episodeNumber ->
                    episodePlaybackSources[episodeNumber]?.let { sourceForQuality(it, selectedQuality) }
                }
                val totalEpisodeBytes = selectedEpisodeSources.mapNotNull { it?.sizeBytes }.sum()
                val allSelectedEpisodeSizesKnown = selectedEpisodeSources.size == count &&
                    selectedEpisodeSources.all { it?.sizeBytes != null }
                val canDownload = count > 0 && selectedQuality.isNotBlank() && when {
                    isMovie -> selectedSource != null
                    episodeSourcesLoading -> false
                    else -> selectedEpisodeSources.all { it != null }
                }
                val buttonText = when {
                    isMovie && selectedSource?.sizeBytes != null -> "Download (${selectedSizeText})"
                    isMovie -> "Download"
                    allSelectedEpisodeSizesKnown -> "Download (${formatDownloadSize(totalEpisodeBytes)})"
                    else -> "Download ($count)"
                }

                Button(
                    onClick = {
                        val epsToDownload = displayEpisodes.filter { selectedEpisodes[it.effectiveEpisodeNumber] != false }.map { it.effectiveEpisodeNumber }
                        if (epsToDownload.isNotEmpty()) {
                            onDownloadEpisodes(epsToDownload, selectedQuality, selectedSource?.id)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    enabled = canDownload,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = buttonText,
                        color = PureWhite,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun sourceForQuality(sources: List<MediaSourceDto>, quality: String): MediaSourceDto? {
    val normalizedQuality = quality.trim().uppercase().removeSuffix("P")
    return sources.firstOrNull {
        it.vipLocked != true &&
            it.effectiveQuality.trim().uppercase().removeSuffix("P") == normalizedQuality &&
            (it.format.equals("MP4", ignoreCase = true) || it.url?.contains(".mp4", ignoreCase = true) == true)
    }
}

private fun formatDownloadSize(sizeBytes: Long?): String? {
    if (sizeBytes == null || sizeBytes <= 0L) return null
    val megabytes = sizeBytes / (1024.0 * 1024.0)
    return if (megabytes >= 1024.0) {
        String.format(Locale.US, "%.1f GB", megabytes / 1024.0)
    } else {
        String.format(Locale.US, "%.1f MB", megabytes)
    }
}
