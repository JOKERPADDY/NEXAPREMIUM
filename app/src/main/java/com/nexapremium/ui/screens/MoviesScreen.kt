package com.nexapremium.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun MoviesScreen(
    onMovieClick: (String, String) -> Unit,
    onSectionClick: (String) -> Unit
) {
    TabContentScreen(
        tabName = "movies",
        onItemClick = onMovieClick,
        onSectionClick = onSectionClick
    )
}
