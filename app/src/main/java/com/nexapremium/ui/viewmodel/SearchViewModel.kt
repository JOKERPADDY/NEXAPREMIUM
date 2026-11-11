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
    data class Success(val results: List<SubjectItemDto>) : SearchUiState
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
                        FamilyContentFilter.filter(unfilteredResults, enabled)
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
                    if (items.isNotEmpty()) {
                        publishResults(items)
                    } else {
                        val cachedAll = sectionItemsRepository.getAllCachedItems()
                        if (cachedAll.isNotEmpty()) {
                            publishResults(cachedAll)
                        } else {
                            _uiState.value = SearchUiState.Idle
                        }
                    }
                }
                .onFailure {
                    val cachedAll = sectionItemsRepository.getAllCachedItems()
                    if (cachedAll.isNotEmpty()) {
                        publishResults(cachedAll)
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
            val cached = sectionItemsRepository.getSectionItems(query)
            if (!cached.isNullOrEmpty()) {
                publishResults(cached)
            } else {
                searchRepository.search(query)
                    .onSuccess { response ->
                        val items = response.items.orEmpty().ifEmpty {
                            response.data?.items.orEmpty()
                        }
                        if (items.isNotEmpty()) {
                            publishResults(items)
                        } else {
                            // Fallback to all cached section items or popular searches if specific query yielded nothing
                            val cachedAll = sectionItemsRepository.getAllCachedItems()
                            if (cachedAll.isNotEmpty()) {
                                publishResults(cachedAll)
                            } else {
                                _uiState.value = SearchUiState.Success(emptyList())
                            }
                        }
                    }
                    .onFailure { error ->
                        val cachedAll = sectionItemsRepository.getAllCachedItems()
                        if (cachedAll.isNotEmpty()) {
                            publishResults(cachedAll)
                        } else {
                            _uiState.value = SearchUiState.Error(error.message ?: "Search failed")
                        }
                    }
            }
        }
    }

    private fun publishResults(items: List<SubjectItemDto>) {
        unfilteredResults = items
        _uiState.value = SearchUiState.Success(
            FamilyContentFilter.filter(items, familyModeRepository.enabled.value)
        )
    }
}
