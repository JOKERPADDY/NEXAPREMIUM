package com.nexapremium.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun KidsScreen(
    onItemClick: (String, String) -> Unit,
    onSectionClick: (String) -> Unit
) {
    TabContentScreen(
        tabName = "kids",
        onItemClick = onItemClick,
        onSectionClick = onSectionClick
    )
}
