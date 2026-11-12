package com.nexapremium.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun RecommendedScreen(
    onItemClick: (String, String) -> Unit,
    onSectionClick: (String) -> Unit,
    onSportsClick: (String, String) -> Unit
) {
    TabContentScreen(
        tabName = "recommended",
        onItemClick = onItemClick,
        onSectionClick = onSectionClick,
        onSportsClick = onSportsClick
    )
}
