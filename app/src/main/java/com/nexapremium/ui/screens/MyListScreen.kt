package com.nexapremium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexapremium.ui.components.FocusableCard
import com.nexapremium.ui.viewmodel.MyListViewModel
import com.nexapremium.core.ui.theme.PureBlack
import com.nexapremium.core.ui.theme.Typography

@Composable
fun MyListScreen(
    onItemClick: (String) -> Unit,
    viewModel: MyListViewModel = hiltViewModel()
) {
    val myList by viewModel.myList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
    ) {
        Text(
            text = "My Watchlist",
            style = Typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(myList) { item ->
                FocusableCard(
                    imageUrl = item.coverUrl,
                    title = item.title,
                    onClick = { onItemClick(item.detailPath) }
                )
            }
        }
    }
}
