package com.nexapremium.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapremium.core.ui.theme.MutedGray
import com.nexapremium.core.ui.theme.PrimaryRed
import com.nexapremium.core.ui.theme.PureWhite
import com.nexapremium.data.db.entity.ContinueWatchingEntity

@Composable
fun ContinueWatchingCarousel(
    items: List<ContinueWatchingEntity>,
    onItemClick: (ContinueWatchingEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val configuration = LocalConfiguration.current
    val isLargeScreen = configuration.screenWidthDp >= 840
    val cardWidth = if (isLargeScreen) 160.dp else 107.dp
    val spacing = if (isLargeScreen) 16.dp else 8.dp

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = "Continue Watching",
            color = PureWhite,
            fontSize = if (isLargeScreen) 22.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            items(items, key = { it.playbackKey }) { item ->
                Column(modifier = Modifier.width(cardWidth)) {
                    FocusableCard(
                        imageUrl = item.coverUrl,
                        title = item.title.ifBlank { "Continue episode" },
                        badgeText = if (item.seasonNumber > 0 || item.episodeNumber > 0) {
                            "S${item.seasonNumber} E${item.episodeNumber}"
                        } else {
                            null
                        },
                        onClick = { onItemClick(item) },
                        aspectRatio = 0.71f,
                        showTitle = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (item.durationMs > 0L) {
                        LinearProgressIndicator(
                            progress = {
                                (item.positionMs.toFloat() / item.durationMs).coerceIn(0f, 1f)
                            },
                            color = PrimaryRed,
                            trackColor = MutedGray.copy(alpha = 0.35f),
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
