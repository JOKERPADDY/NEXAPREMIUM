package com.nexapremium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexapremium.core.ui.theme.MutedGray
import com.nexapremium.core.ui.theme.PureBlack
import com.nexapremium.core.ui.theme.PureWhite
import com.nexapremium.core.ui.theme.Typography
import com.nexapremium.ui.components.FocusableCard
import com.nexapremium.ui.viewmodel.SectionItemsViewModel

@Composable
fun SectionItemsScreen(
    title: String,
    onBackClick: () -> Unit,
    onItemClick: (String, String) -> Unit,
    viewModel: SectionItemsViewModel = hiltViewModel()
) {
    val screenTitle by viewModel.title.collectAsState()
    val items by viewModel.items.collectAsState()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val gridColumns = if (screenWidthDp < 600) GridCells.Fixed(3) else GridCells.Adaptive(minSize = 170.dp)

    LaunchedEffect(title) { viewModel.load(title) }

    Column(
        modifier = Modifier.fillMaxSize().background(PureBlack)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
            }
            Text(
                text = screenTitle.ifBlank { title },
                style = Typography.headlineMedium,
                color = PureWhite
            )
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No titles are available in this row right now.", color = MutedGray)
            }
        } else {
            LazyVerticalGrid(
                columns = gridColumns,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.detailPath ?: it.id ?: it.title.orEmpty() }) { item ->
                    FocusableCard(
                        imageUrl = item.effectiveCoverUrl,
                        title = item.title.orEmpty(),
                        badgeText = item.badgeText,
                        aspectRatio = 0.71f,
                        onClick = {
                            item.detailPath?.let { path -> onItemClick(path, item.effectiveSubjectType) }
                        }
                    )
                }
            }
        }
    }
}
