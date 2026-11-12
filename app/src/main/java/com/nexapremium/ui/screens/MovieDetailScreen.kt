package com.nexapremium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nexapremium.core.ui.theme.*
import com.nexapremium.data.api.model.EpisodeDto
import com.nexapremium.data.api.model.MediaSourceDto
import com.nexapremium.ui.components.DownloadBottomSheet
import com.nexapremium.ui.components.rememberDownloadPermissionRequester
import com.nexapremium.ui.components.FocusableButton
import com.nexapremium.ui.components.FocusableCard
import com.nexapremium.ui.components.isTelevisionDevice
import com.nexapremium.ui.viewmodel.DetailUiState
import com.nexapremium.ui.viewmodel.DetailViewModel

@Composable
fun MovieBadgeItem(
    text: String,
    textColor: Color = MutedGray,
    containerColor: Color = DarkSurface,
    border: androidx.compose.foundation.BorderStroke? = null
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(4.dp),
        border = border
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (textColor != MutedGray) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun MovieDetailScreen(
    detailPath: String,
    onPlayClick: (String, Int, Int, String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val requestPermissionThenDownload = rememberDownloadPermissionRequester()
    val isTelevision = remember(context) { isTelevisionDevice(context) }
    val uiState by viewModel.uiState.collectAsState()
    val preferredDownloadQuality by viewModel.defaultDownloadQuality.collectAsState()

    LaunchedEffect(detailPath) {
        viewModel.loadDetail(detailPath)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        when (val state = uiState) {
            is DetailUiState.Loading -> {
                CircularProgressIndicator(
                    color = PrimaryRed,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is DetailUiState.Success -> {
                val data = state.data
                val movie = data.movieDetail
                val isMovie = data.seasons.isEmpty() && data.episodes.isEmpty()

                var showDubDropdown by remember { mutableStateOf(false) }
                var showSeasonDropdown by remember { mutableStateOf(false) }
                var showDownloadBottomSheet by remember { mutableStateOf(false) }
                var downloadSources by remember(detailPath) { mutableStateOf(emptyList<MediaSourceDto>()) }
                LaunchedEffect(showDownloadBottomSheet, detailPath, data.selectedSeason, isMovie) {
                    if (showDownloadBottomSheet && isMovie) {
                        downloadSources = viewModel.getDirectDownloadSources(detailPath, data.selectedSeason, 0)
                    } else if (!showDownloadBottomSheet) {
                        downloadSources = emptyList()
                    }
                }
                var selectedPillIndex by remember { mutableStateOf<Int?>(null) }

                val currentSeasonObj = data.seasons.find { (it.number ?: 1) == data.selectedSeason }
                val availableQualities = currentSeasonObj?.resolutions?.takeIf { it.isNotEmpty() }?.map { "${it}P" }
                    ?: if (isMovie) emptyList() else listOf("360P", "480P", "1080P")

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Video Backdrop Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        AsyncImage(
                            model = movie.backdrop?.url ?: movie.cover?.url,
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, PureBlack),
                                        startY = 100f
                                    )
                                )
                        )
                        // Play Button Overlay
                        IconButton(
                            onClick = { onPlayClick(detailPath, data.selectedSeason, if (data.episodes.isNotEmpty()) 1 else 0, movie.cover?.url.orEmpty()) },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .background(PrimaryRed.copy(alpha = 0.9f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = PureWhite,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        // Title
                        Text(
                            text = movie.title ?: "",
                            style = Typography.headlineMedium,
                            color = PureWhite
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Metadata Badges Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MovieBadgeItem(if (data.seasons.isNotEmpty()) "TV Series" else "Movie")
                            MovieBadgeItem(
                                text = "★ ${String.format("%.1f", movie.score ?: 0.0)}",
                                textColor = Color(0xFFFFC107),
                                containerColor = Color(0xFF2A230B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.6f))
                            )
                            movie.releaseYear?.let { yr -> MovieBadgeItem("$yr") }
                            if (data.seasons.isNotEmpty()) {
                                MovieBadgeItem("${data.seasons.size} Seasons")
                            } else {
                                movie.durationMinutes?.let { dur -> MovieBadgeItem("${dur}m") }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons Row (+ Add to list, Share, Download)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FocusableButton(
                                text = if (data.isBookmarked) "In List" else "+ Add to list",
                                onClick = {
                                    viewModel.toggleBookmark(
                                        detailPath = detailPath,
                                        title = movie.title ?: "",
                                        coverUrl = movie.cover?.url ?: "",
                                        subjectType = if (data.seasons.isNotEmpty()) "series" else "movie"
                                    )
                                },
                                isPrimary = false,
                                modifier = Modifier.weight(1f)
                            )
                            FocusableButton(
                                text = "Share",
                                onClick = { viewModel.shareTitle(context, movie.title ?: "Movie", detailPath) },
                                isPrimary = false,
                                modifier = Modifier.weight(1f)
                            )
                            if (!isTelevision) {
                                FocusableButton(
                                    text = "Download",
                                    onClick = { showDownloadBottomSheet = true },
                                    isPrimary = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Description
                        Text(
                            text = movie.description ?: "",
                            style = Typography.bodyMedium,
                            color = MutedGray
                        )

                        // Resources (Audio & Season Dropdowns) if available
                        if (data.seasons.isNotEmpty() || data.dubs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Resources",
                                style = Typography.titleLarge,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (data.dubs.isNotEmpty()) {
                                    Box {
                                        FilterDropdownChip(
                                            label = data.selectedDub?.label ?: "Original Audio",
                                            onClick = { showDubDropdown = true }
                                        )
                                        DropdownMenu(
                                            expanded = showDubDropdown,
                                            onDismissRequest = { showDubDropdown = false },
                                            modifier = Modifier.background(DarkSurface)
                                        ) {
                                            data.dubs.forEach { dub ->
                                                DropdownMenuItem(
                                                    text = { Text(dub.label ?: "Audio", color = PureWhite) },
                                                    onClick = {
                                                        viewModel.selectDub(dub)
                                                        showDubDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (data.seasons.isNotEmpty()) {
                                    Box {
                                        FilterDropdownChip(
                                            label = "Season ${String.format("%02d", data.selectedSeason)}",
                                            onClick = { showSeasonDropdown = true }
                                        )
                                        DropdownMenu(
                                            expanded = showSeasonDropdown,
                                            onDismissRequest = { showSeasonDropdown = false },
                                            modifier = Modifier.background(DarkSurface)
                                        ) {
                                            data.seasons.forEach { season ->
                                                DropdownMenuItem(
                                                    text = { Text("Season ${String.format("%02d", season.number ?: 1)}", color = PureWhite) },
                                                    onClick = {
                                                        viewModel.selectSeason(detailPath, season.number ?: 1)
                                                        showSeasonDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Episodes section if available
                        if (data.episodes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Episodes (${data.episodes.size})",
                                style = Typography.titleMedium,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    EpisodePill(
                                        numberText = "All",
                                        isSelected = selectedPillIndex == null,
                                        onClick = {
                                            selectedPillIndex = null
                                            onPlayClick(detailPath, data.selectedSeason, 1, movie.cover?.url.orEmpty())
                                        }
                                    )
                                }

                                itemsIndexed(data.episodes) { index, ep ->
                                    val epNum = ep.effectiveEpisodeNumber
                                    EpisodePill(
                                        numberText = String.format("%02d", epNum),
                                        isSelected = selectedPillIndex == index,
                                        onClick = {
                                            selectedPillIndex = index
                                            onPlayClick(detailPath, data.selectedSeason, epNum, movie.cover?.url.orEmpty())
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Recommendations Section ("For You") - strictly below episodes
                        if (data.recommendations.isNotEmpty()) {
                            Text(
                                text = "For You",
                                style = Typography.titleLarge,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(data.recommendations) { item ->
                                    FocusableCard(
                                        imageUrl = item.effectiveCoverUrl,
                                        title = item.title ?: "",
                                        badgeText = item.badgeText,
                                        onClick = {
                                            item.detailPath?.let { path ->
                                                onPlayClick(path, 0, 0, item.effectiveCoverUrl.orEmpty())
                                            }
                                        },
                                        modifier = Modifier.width(115.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (showDownloadBottomSheet && !isTelevision) {
                    DownloadBottomSheet(
                        titleName = movie.title ?: "Movie",
                        seasonNumber = if (data.selectedSeason == 0) 1 else data.selectedSeason,
                        episodes = data.episodes,
                        availableQualities = availableQualities,
                        playbackSources = downloadSources,
                        preferredQuality = preferredDownloadQuality,
                        isMovie = isMovie,
                        onDismiss = { showDownloadBottomSheet = false },
                        onDownloadEpisodes = { selectedEps, resolution, sourceId ->
                            requestPermissionThenDownload {
                                viewModel.downloadTitle(
                                    context = context,
                                    detailPath = detailPath,
                                    titleName = movie.title ?: "Movie",
                                    season = data.selectedSeason,
                                    episode = 0,
                                    quality = resolution,
                                    sourceId = sourceId
                                )
                            }
                        }
                    )
                }
            }
            is DetailUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, color = PureWhite)
                    Spacer(modifier = Modifier.height(12.dp))
                    FocusableButton(text = "Retry", onClick = { viewModel.loadDetail(detailPath) })
                }
            }
        }
    }
}
