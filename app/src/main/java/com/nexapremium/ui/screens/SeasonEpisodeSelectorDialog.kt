package com.nexapremium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexapremium.ui.components.FocusableButton
import com.nexapremium.data.api.model.EpisodeDto
import com.nexapremium.core.ui.theme.DarkSurface
import com.nexapremium.core.ui.theme.PureWhite
import com.nexapremium.core.ui.theme.Typography

@Composable
fun SeasonEpisodeSelectorDialog(
    episodes: List<EpisodeDto>,
    onEpisodeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .background(DarkSurface)
                .padding(20.dp)
        ) {
            Text(
                text = "Select Episode",
                style = Typography.titleLarge,
                color = PureWhite
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.height(300.dp)) {
                items(episodes) { ep ->
                    FocusableButton(
                        text = "EP ${ep.episodeNumber}: ${ep.title ?: "Episode"}",
                        onClick = { onEpisodeSelected(ep.seasonNumber ?: 1, ep.episodeNumber ?: 1) },
                        isPrimary = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
