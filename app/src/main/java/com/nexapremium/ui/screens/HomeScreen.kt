package com.nexapremium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexapremium.ui.components.FocusableButton
import com.nexapremium.ui.components.HorizontalCarousel
import com.nexapremium.ui.components.CatalogLoadingSkeleton
import com.nexapremium.ui.viewmodel.HomeUiState
import com.nexapremium.ui.viewmodel.HomeViewModel
import com.nexapremium.core.ui.theme.PureBlack
import com.nexapremium.core.ui.theme.PureWhite

@Composable
fun HomeScreen(
    onItemClick: (String, String) -> Unit,
    onSectionClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                CatalogLoadingSkeleton()
            }
            is HomeUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(state.blocks) { block ->
                        val isLandscape = block.layout?.contains("landscape", ignoreCase = true) == true ||
                                block.layout?.contains("banner", ignoreCase = true) == true ||
                                (block.title?.contains("Recommended", ignoreCase = true) == true && block == state.blocks.firstOrNull())

                        HorizontalCarousel(
                            title = block.title ?: "Recommended",
                            items = block.items ?: emptyList(),
                            onItemClick = onItemClick,
                            onSectionClick = onSectionClick,
                            isLandscape = isLandscape
                        )
                    }
                }
            }
            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, color = PureWhite)
                    Spacer(modifier = Modifier.height(16.dp))
                    FocusableButton(
                        text = "Retry",
                        onClick = { viewModel.loadHomeData() }
                    )
                }
            }
        }
    }
}
