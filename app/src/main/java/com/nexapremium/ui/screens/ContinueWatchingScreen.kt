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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.nexapremium.core.ui.theme.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexapremium.ui.components.FocusableCard
import com.nexapremium.ui.viewmodel.ContinueWatchingViewModel
import com.nexapremium.data.db.entity.ContinueWatchingEntity

@Composable
fun ContinueWatchingScreen(
    onItemClick: (ContinueWatchingEntity) -> Unit,
    viewModel: ContinueWatchingViewModel = hiltViewModel()
) {
    val continueWatchingList by viewModel.continueWatchingList.collectAsState()

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
            columns = GridCells.Adaptive(minSize = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(continueWatchingList, key = { it.detailPath }) { item ->
                Column {
                    Box {
                        FocusableCard(
                            imageUrl = item.coverUrl,
                            title = item.title,
                            badgeText = if (item.seasonNumber > 0) "S${item.seasonNumber} · E${item.episodeNumber}" else null,
                            onClick = { onItemClick(item) }
                        )
                        IconButton(
                            onClick = { viewModel.remove(item.detailPath) },
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
                }
            }
        }
    }
}
