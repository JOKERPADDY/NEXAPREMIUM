package com.nexapremium.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CatalogLoadingSkeleton(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            ShimmerPlaceholder(
                modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        items(4) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                ShimmerPlaceholder(
                    modifier = Modifier.width(150.dp).height(20.dp).padding(start = 12.dp, bottom = 4.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(6) {
                        ShimmerPlaceholder(modifier = Modifier.size(width = 108.dp, height = 152.dp))
                    }
                }
            }
        }
    }
}
