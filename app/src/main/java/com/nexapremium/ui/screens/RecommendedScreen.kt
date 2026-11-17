package com.nexapremium.ui.screens

import androidx.compose.runtime.Composable
import com.nexapremium.data.db.entity.ContinueWatchingEntity

@Composable
fun RecommendedScreen(
    onItemClick: (String, String) -> Unit,
    onSectionClick: (String) -> Unit,
    onSportsClick: (String, String) -> Unit,
    onContinueWatchingClick: (ContinueWatchingEntity) -> Unit
) {
    TabContentScreen(
        tabName = "recommended",
        onItemClick = onItemClick,
        onSectionClick = onSectionClick,
        onSportsClick = onSportsClick,
        onContinueWatchingClick = onContinueWatchingClick
    )
}
