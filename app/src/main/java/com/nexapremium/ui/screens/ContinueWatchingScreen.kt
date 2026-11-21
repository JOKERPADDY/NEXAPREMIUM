package com.nexapremium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import com.nexapremium.core.ui.theme.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexapremium.ui.components.FocusableCard
import com.nexapremium.ui.components.RemoveContinueWatchingDialog
import com.nexapremium.ui.viewmodel.ContinueWatchingViewModel
import com.nexapremium.data.db.entity.ContinueWatchingEntity

@Composable
fun ContinueWatchingScreen(
    onItemClick: (ContinueWatchingEntity) -> Unit,
    viewModel: ContinueWatchingViewModel = hiltViewModel()
) {
    val continueWatchingList by viewModel.continueWatchingList.collectAsState()
    var itemToRemove by remember { mutableStateOf<ContinueWatchingEntity?>(null) }

    itemToRemove?.let { item ->
        RemoveContinueWatchingDialog(
            item = item,
            onConfirm = {
                viewModel.remove(item.playbackKey)
            },
            onDismiss = {
                itemToRemove = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
    ) {
        Text(
            text = "Continue Watching",
            style = Typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (continueWatchingList.isEmpty()) {
            Text("Your unfinished movies and episodes will show up here.", color = MutedGray)
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 107.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(continueWatchingList, key = { it.playbackKey }) { item ->
                Column {
                    Box {
                        FocusableCard(
                            imageUrl = item.coverUrl,
                            title = item.title,
                            badgeText = if (item.seasonNumber > 0 || item.episodeNumber > 0) "S${item.seasonNumber} E${item.episodeNumber}" else null,
                            aspectRatio = 0.71f,
                            showTitle = false,
                            onClick = { onItemClick(item) },
                            onLongClick = { itemToRemove = item }
                        )
                        IconButton(
                            onClick = { itemToRemove = item },
                            modifier = Modifier.align(Alignment.TopEnd).size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove from Continue Watching", tint = PureWhite)
                        }
                    }
                    if (item.durationMs > 0L) {
                        LinearProgressIndicator(
                            progress = { (item.positionMs.toFloat() / item.durationMs).coerceIn(0f, 1f) },
                            color = PrimaryRed,
                            trackColor = PureWhite.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth().padding(top = 5.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.title.ifBlank { "Continue episode" },
                        color = PureWhite,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
