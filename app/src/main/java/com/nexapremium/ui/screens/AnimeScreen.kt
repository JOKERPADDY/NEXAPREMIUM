package com.nexapremium.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun AnimeScreen(
    onItemClick: (String, String) -> Unit,
    onSectionClick: (String) -> Unit
) {
    TabContentScreen(
        tabName = "anime",
        onItemClick = onItemClick,
        onSectionClick = onSectionClick
    )
}
