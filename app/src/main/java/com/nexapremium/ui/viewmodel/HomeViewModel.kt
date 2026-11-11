package com.nexapremium.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapremium.data.filter.FamilyContentFilter
import com.nexapremium.data.api.model.HomeBlockDto
import com.nexapremium.data.api.model.SubjectItemDto
import com.nexapremium.data.repository.FamilyModeRepository
import com.nexapremium.data.repository.HomeRepository
import com.nexapremium.data.repository.LocalDatabaseRepository
import com.nexapremium.data.repository.SectionItemsRepository
import com.nexapremium.data.db.entity.ContinueWatchingEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val blocks: List<HomeBlockDto>,
        val continueWatching: List<ContinueWatchingEntity> = emptyList()
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val localDatabaseRepository: LocalDatabaseRepository,
    private val familyModeRepository: FamilyModeRepository,
    private val sectionItemsRepository: SectionItemsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var unfilteredContent: HomeUiState.Success? = null

    init {
        viewModelScope.launch {
            familyModeRepository.enabled.collect { enabled ->
                unfilteredContent?.let { _uiState.value = filterContent(it, enabled) }
            }
        }
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            homeRepository.getHome(1)
                .onSuccess { response ->
                    val blocks = mutableListOf<HomeBlockDto>()

                    if (!response.banners.isNullOrEmpty()) {
                        blocks.add(
                            HomeBlockDto(
                                blockId = "featured_banners",
                                title = "Featured",
                                layout = "banner",
                                items = response.banners
                            )
                        )
                    }

                    response.sections?.forEach { section ->
                        if (!section.items.isNullOrEmpty()) {
                            blocks.add(
                                HomeBlockDto(
                                    blockId = section.title ?: "section",
                                    title = section.title ?: "Recommended",
                                    layout = "carousel",
                                    items = section.items
                                )
                            )
                        }
                    }

                    if (blocks.isEmpty()) {
                        response.data?.blocks?.forEach { block ->
                            if (!block.items.isNullOrEmpty()) {
                                blocks.add(block)
                            }
                        }
                    }

                    if (blocks.isNotEmpty()) {
                        blocks.forEach { block ->
                            if (!block.title.isNullOrEmpty() && !block.items.isNullOrEmpty()) {
                                sectionItemsRepository.cacheSection(block.title!!, block.items!!)
                            }
                        }
                        val content = HomeUiState.Success(blocks = blocks)
                        unfilteredContent = content
                        _uiState.value = filterContent(content, familyModeRepository.enabled.value)
                    } else {
                        _uiState.value = HomeUiState.Error("No content available")
                    }
                }
                .onFailure { error ->
                    _uiState.value = HomeUiState.Error(error.message ?: "Failed to load home content")
                }
        }
    }

    private fun filterContent(content: HomeUiState.Success, familyModeEnabled: Boolean) =
        content.copy(
            blocks = content.blocks.mapNotNull { block ->
                val items = FamilyContentFilter.filter(block.items.orEmpty(), familyModeEnabled)
                if (items.isEmpty()) null else block.copy(items = items)
            }
        )
}
