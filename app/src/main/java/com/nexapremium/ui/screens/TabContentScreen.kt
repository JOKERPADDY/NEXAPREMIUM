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
import com.nexapremium.core.ui.theme.PureBlack
import com.nexapremium.core.ui.theme.PureWhite
import com.nexapremium.ui.components.FocusableButton
import com.nexapremium.ui.components.CatalogLoadingSkeleton
import com.nexapremium.ui.components.HeroBanner
import com.nexapremium.ui.components.HorizontalCarousel
import com.nexapremium.ui.viewmodel.TabContentViewModel
import com.nexapremium.ui.viewmodel.TabUiState

@Composable
fun TabContentScreen(
    tabName: String,
    onItemClick: (String, String) -> Unit,
    onSectionClick: (String) -> Unit,
    onSportsClick: (String, String) -> Unit = { _, _ -> }
) {
    val viewModel: TabContentViewModel = hiltViewModel(
        key = tabName,
        creationCallback = { factory: TabContentViewModel.Factory -> factory.create(tabName) }
    )
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        when (val state = uiState) {
            is TabUiState.Loading -> {
                CatalogLoadingSkeleton()
            }
            is TabUiState.Success -> {
                if (state.banners.isEmpty() && state.blocks.isEmpty()) {
                    Text(
                        text = "No titles available. Try turning off Family Mode in Settings.",
                        color = PureWhite,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Hero Banner at the top of each tab
                    if (state.banners.isNotEmpty()) {
                        item(key = "hero_banner") {
                            HeroBanner(
                                banners = state.banners,
                                onItemClick = onItemClick,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }

                    if (tabName.equals("recommended", ignoreCase = true)) {
                        item(key = "live_sports") {
                            com.nexapremium.ui.components.LiveSportsRow(onMatchClick = { match -> onSportsClick(match.id, match.title) })
                        }
                    }

                    // Content blocks / carousels
                    items(state.blocks, key = { it.blockId ?: it.title ?: java.util.UUID.randomUUID().toString() }) { block ->
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
            is TabUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, color = PureWhite)
                    Spacer(modifier = Modifier.height(16.dp))
                    FocusableButton(
                        text = "Retry",
                        onClick = { viewModel.loadData() }
                    )
                }
            }
        }
    }
}
