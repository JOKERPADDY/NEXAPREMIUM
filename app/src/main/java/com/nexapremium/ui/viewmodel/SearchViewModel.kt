package com.nexapremium.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapremium.data.filter.FamilyContentFilter
import com.nexapremium.data.api.model.SubjectItemDto
import com.nexapremium.data.repository.FamilyModeRepository
import com.nexapremium.data.repository.SearchRepository
import com.nexapremium.data.repository.SectionItemsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val results: List<SubjectItemDto>, val fromCache: Boolean = false) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val familyModeRepository: FamilyModeRepository,
    private val sectionItemsRepository: SectionItemsRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var unfilteredResults: List<SubjectItemDto> = emptyList()

    init {
        familyModeRepository.enabled
            .onEach { enabled ->
                if (_uiState.value is SearchUiState.Success) {
                    _uiState.value = SearchUiState.Success(
                        FamilyContentFilter.filter(unfilteredResults, enabled),
                        fromCache = (_uiState.value as? SearchUiState.Success)?.fromCache == true
                    )
                }
            }
            .launchIn(viewModelScope)

        loadPopularSearches()

        searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.trim().length >= 1) {
                    executeSearch(query)
                } else {
                    loadPopularSearches()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadPopularSearches() {
        viewModelScope.launch {
            searchRepository.getPopularSearches()
                .onSuccess { response ->
                    val items = response.data.orEmpty()
                    publishResults(items)
                }
                .onFailure {
                    val cachedAll = sectionItemsRepository.getAllCachedItems()
                    if (cachedAll.isNotEmpty()) {
                        publishResults(cachedAll, fromCache = true)
                    } else {
                        _uiState.value = SearchUiState.Idle
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun submitSearch() {
        val query = searchQuery.value.trim()
        if (query.isBlank()) loadPopularSearches() else executeSearch(query)
    }

    private fun executeSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            searchRepository.search(query)
                .onSuccess { response ->
                    val items = response.items.orEmpty().ifEmpty {
                        response.data?.items.orEmpty()
                    }
                    publishResults(items)
                }
                .onFailure { error ->
                    val normalizedQuery = query.trim().lowercase()
                    val cachedMatches = sectionItemsRepository.getAllCachedItems().filter { item ->
                        listOfNotNull(
                            item.title,
                            item.description,
                            item.genres?.joinToString(" "),
                            item.tags?.mapNotNull { tag -> tag.name }?.joinToString(" ")
                        ).any { it.contains(normalizedQuery, ignoreCase = true) }
                    }
                    if (cachedMatches.isNotEmpty()) {
                        publishResults(cachedMatches, fromCache = true)
                    } else {
                        _uiState.value = SearchUiState.Error(error.message ?: "Search failed")
                    }
                }
        }
    }

    private fun publishResults(items: List<SubjectItemDto>, fromCache: Boolean = false) {
        unfilteredResults = items
        _uiState.value = SearchUiState.Success(
            FamilyContentFilter.filter(items, familyModeRepository.enabled.value),
            fromCache
        )
    }
}
