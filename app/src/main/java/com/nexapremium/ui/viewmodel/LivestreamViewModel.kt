package com.nexapremium.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapremium.data.api.model.CDNChannel
import com.nexapremium.data.api.model.ExtractedVideo
import com.nexapremium.data.provider.CdnLiveTvProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LivestreamViewModel @Inject constructor(
    private val cdnLiveTvProvider: CdnLiveTvProvider
) : ViewModel() {

    private val _channels = MutableStateFlow<List<CDNChannel>>(emptyList())
    val channels: StateFlow<List<CDNChannel>> = _channels.asStateFlow()

    private val _filteredChannels = MutableStateFlow<List<CDNChannel>>(emptyList())
    val filteredChannels: StateFlow<List<CDNChannel>> = _filteredChannels.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _resolvingChannelCode = MutableStateFlow<String?>(null)
    val resolvingChannelCode: StateFlow<String?> = _resolvingChannelCode.asStateFlow()

    val categories = listOf("All", "Sports", "Movies", "Entertainment", "News")

    init {
        loadChannels()
    }

    fun loadChannels(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val list = cdnLiveTvProvider.getChannels(forceRefresh)
                _channels.value = list
                applyFilters()
            } catch (e: Exception) {
                _error.value = "Failed to load live channels: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        applyFilters()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    private fun applyFilters() {
        var list = _channels.value
        val category = _selectedCategory.value
        val query = _searchQuery.value.trim().lowercase()

        if (category != "All") {
            val sportsKeywords = listOf("sport", "sky", "dazn", "espn", "bein", "premier", "foot", "nba", "nfl", "f1", "bt")
            val movieKeywords = listOf("movie", "cinema", "hbo", "action", "starz", "film")
            val newsKeywords = listOf("news", "bbc", "cnn", "sky news", "al jazeera", "fox news")

            list = list.filter { ch ->
                val lowerName = ch.name.lowercase()
                when (category) {
                    "Sports" -> sportsKeywords.any { lowerName.contains(it) }
                    "Movies" -> movieKeywords.any { lowerName.contains(it) }
                    "News" -> newsKeywords.any { lowerName.contains(it) }
                    "Entertainment" -> !sportsKeywords.any { lowerName.contains(it) } &&
                            !movieKeywords.any { lowerName.contains(it) } &&
                            !newsKeywords.any { lowerName.contains(it) }
                    else -> true
                }
            }
        }

        if (query.isNotEmpty()) {
            list = list.filter { it.name.lowercase().contains(query) }
        }

        _filteredChannels.value = list
    }

    fun playChannel(channel: CDNChannel, onResolved: (ExtractedVideo) -> Unit) {
        viewModelScope.launch {
            _resolvingChannelCode.value = channel.code
            try {
                val video = cdnLiveTvProvider.resolveStreamUrl(channel)
                onResolved(video)
            } catch (e: Exception) {
                _error.value = "Could not play channel ${channel.name}: ${e.message}"
            } finally {
                _resolvingChannelCode.value = null
            }
        }
    }
}
