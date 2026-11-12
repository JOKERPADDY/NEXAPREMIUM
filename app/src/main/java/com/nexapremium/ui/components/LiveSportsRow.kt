package com.nexapremium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.nexapremium.core.ui.theme.DarkSurface
import com.nexapremium.core.ui.theme.MutedGray
import com.nexapremium.core.ui.theme.PureWhite
import com.nexapremium.core.ui.theme.PrimaryRed
import com.nexapremium.data.sports.SportsMatch
import com.nexapremium.ui.viewmodel.SportsViewModel

@Composable
fun LiveSportsRow(
    onMatchClick: (SportsMatch) -> Unit,
    viewModel: SportsViewModel = hiltViewModel()
) {
    val matches by viewModel.matches.collectAsState()
    val isLoading by viewModel.isLoadingMatches.collectAsState()

    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            "Live Sports",
            color = PureWhite,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (matches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(DarkSurface, RoundedCornerShape(10.dp))
                    .clickable { if (!isLoading) viewModel.refresh() }
                    .padding(horizontal = 14.dp, vertical = 16.dp)
            ) {
                Text(
                    text = if (isLoading) "Loading live matches…" else "No live matches available. Tap to retry.",
                    color = MutedGray,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(matches, key = { it.id }) { match ->
                    val interactions = remember(match.id) { MutableInteractionSource() }
                    Column(
                        Modifier.width(220.dp)
                            .background(DarkSurface, RoundedCornerShape(10.dp))
                            .clickable(interactionSource = interactions, indication = null) { onMatchClick(match) }
                            .tvFocusOutline(interactions, RoundedCornerShape(10.dp))
                    ) {
                        Box(
                            Modifier.fillMaxWidth()
                                .height(132.dp)
                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        ) {
                            SubcomposeAsyncImage(
                                model = match.thumbnailUrl,
                                contentDescription = match.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(132.dp),
                                error = {
                                    Box(Modifier.fillMaxWidth().height(132.dp).background(DarkSurface)) {
                                        Text(match.category.uppercase(), color = MutedGray, modifier = Modifier.padding(12.dp))
                                    }
                                }
                            )
                            Box(
                                Modifier.fillMaxWidth().height(132.dp)
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                            )
                            Row(
                                modifier = Modifier.align(Alignment.TopStart).padding(9.dp)
                                    .background(
                                        if (match.isLive) PrimaryRed else Color.Black.copy(alpha = 0.72f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (match.isLive) {
                                    Icon(
                                        Icons.Default.FiberManualRecord,
                                        contentDescription = "Live",
                                        tint = PureWhite,
                                        modifier = Modifier.width(8.dp).height(8.dp)
                                    )
                                }
                                Text(if (match.isLive) "LIVE" else "UPCOMING", color = PureWhite, fontSize = 10.sp)
                            }
                            Text(
                                match.title,
                                color = PureWhite,
                                fontSize = 15.sp,
                                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                            )
                        }
                        Text(
                            if (match.date > 0) {
                                java.text.SimpleDateFormat("EEE, HH:mm", java.util.Locale.getDefault())
                                    .format(java.util.Date(match.date))
                            } else "Live",
                            color = MutedGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }
    }
}
