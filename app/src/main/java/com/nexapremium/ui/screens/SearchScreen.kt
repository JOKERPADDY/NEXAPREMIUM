package com.nexapremium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nexapremium.core.ui.theme.DarkSurface
import com.nexapremium.core.ui.theme.MutedGray
import com.nexapremium.core.ui.theme.PrimaryRed
import com.nexapremium.core.ui.theme.PureBlack
import com.nexapremium.core.ui.theme.PureWhite
import com.nexapremium.data.api.model.SubjectItemDto
import com.nexapremium.ui.components.tvFocusOutline
import com.nexapremium.ui.viewmodel.SearchUiState
import com.nexapremium.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    initialQuery: String = "",
    onBackClick: () -> Unit,
    onItemClick: (String, String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(initialQuery) {
        if (initialQuery != searchQuery) viewModel.onQueryChange(initialQuery)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(PureBlack).padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onQueryChange,
                placeholder = {
                    Text("Search by series, film, actor...", maxLines = 1, color = MutedGray)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MutedGray)
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear query", tint = PureWhite)
                        }
                    }
                } else null,
                textStyle = LocalTextStyle.current.copy(color = PureWhite, fontSize = 15.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    viewModel.submitSearch()
                }),
                modifier = Modifier.weight(1f).height(58.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryRed,
                    unfocusedBorderColor = DarkSurface,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    cursorColor = PrimaryRed,
                    focusedContainerColor = DarkSurface.copy(alpha = 0.8f),
                    unfocusedContainerColor = DarkSurface.copy(alpha = 0.8f)
                )
            )
            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.submitSearch()
                },
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
            ) {
                Text("Search", color = PureWhite, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(42.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (searchQuery.isBlank()) "Popular searches" else "Search results",
                    color = PureWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                if (searchQuery.isNotBlank() && uiState is SearchUiState.Success) {
                                val results = uiState as SearchUiState.Success
                                Text(
                                    text = if (results.fromCache) "Saved matches · offline" else "${results.results.size} titles",
                                    color = MutedGray,
                                    fontSize = 12.sp
                                )
                }
            }
            IconButton(onClick = { viewModel.submitSearch() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh results", tint = PureWhite)
            }
        }

        when (val state = uiState) {
            is SearchUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryRed)
                }
            }
            is SearchUiState.Success -> {
                if (state.results.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isBlank()) "No popular titles available" else "No titles found",
                            color = MutedGray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(state.results, key = { it.detailPath ?: it.id ?: it.title.orEmpty() }) { item ->
                            SearchResultRow(item = item, onClick = {
                                item.detailPath?.let { path -> onItemClick(path, item.effectiveSubjectType) }
                            })
                        }
                    }
                }
            }
            is SearchUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MutedGray)
                }
            }
            SearchUiState.Idle -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Search movies, series and people", color = MutedGray)
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(item: SubjectItemDto, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val genres = item.genres.orEmpty().ifEmpty {
        item.tags.orEmpty().mapNotNull { it.name }
    }.take(5).joinToString("/")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !item.detailPath.isNullOrBlank(),
                onClick = onClick
            )
            .tvFocusOutline(interactionSource, RoundedCornerShape(12.dp))
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.effectiveCoverUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 58.dp, height = 82.dp).clip(RoundedCornerShape(9.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = item.title.orEmpty(),
                color = PureWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
            if (genres.isNotBlank()) {
                Text(text = genres, color = MutedGray, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}
