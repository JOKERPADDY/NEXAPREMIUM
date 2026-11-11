package com.nexapremium.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun SeriesScreen(
    onSeriesClick: (String, String) -> Unit,
    onSectionClick: (String) -> Unit
) {
    TabContentScreen(
        tabName = "series",
        onItemClick = onSeriesClick,
        onSectionClick = onSectionClick
    )
}
