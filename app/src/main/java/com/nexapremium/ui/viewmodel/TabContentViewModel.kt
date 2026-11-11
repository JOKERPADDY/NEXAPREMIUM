package com.nexapremium.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapremium.data.filter.FamilyContentFilter
import com.nexapremium.data.api.model.HomeBlockDto
import com.nexapremium.data.api.model.HomeResponseDto
import com.nexapremium.data.api.model.SubjectItemDto
import com.nexapremium.data.repository.FamilyModeRepository
import com.nexapremium.data.repository.HomeRepository
import com.nexapremium.data.repository.SectionItemsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TabUiState {
    object Loading : TabUiState
    data class Success(
        val banners: List<SubjectItemDto> = emptyList(),
        val blocks: List<HomeBlockDto> = emptyList()
    ) : TabUiState
    data class Error(val message: String) : TabUiState
}

@HiltViewModel(assistedFactory = TabContentViewModel.Factory::class)
class TabContentViewModel @AssistedInject constructor(
    private val homeRepository: HomeRepository,
    private val familyModeRepository: FamilyModeRepository,
    private val sectionItemsRepository: SectionItemsRepository,
    @Assisted private val tabName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<TabUiState>(TabUiState.Loading)
    val uiState: StateFlow<TabUiState> = _uiState.asStateFlow()
    private var unfilteredContent: TabUiState.Success? = null

    init {
        viewModelScope.launch {
            familyModeRepository.enabled.collect { enabled ->
                unfilteredContent?.let { _uiState.value = filterContent(it, enabled) }
            }
        }
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = TabUiState.Loading
            when (val tab = tabName.lowercase()) {
                "recommended", "home" -> loadHomeData()
                "movies", "series", "kids", "anime" -> loadCatalogData(tab)
                else -> _uiState.value = TabUiState.Error("Unknown tab: $tabName")
            }
        }
    }

    private suspend fun loadHomeData() {
        homeRepository.getHome(1)
            .onSuccess { response ->
                val blocks = response.sections.orEmpty()
                    .filter { !it.items.isNullOrEmpty() }
                    .map { section ->
                        HomeBlockDto(
                            blockId = section.title ?: "section",
                            title = section.title ?: "Recommended",
                            layout = section.kind ?: "carousel",
                            items = section.items
                        )
                    }
                    .ifEmpty {
                        response.data?.blocks.orEmpty().filter { !it.items.isNullOrEmpty() }
                    }

                val banners = response.banners.orEmpty().ifEmpty {
                    response.sections?.firstOrNull()?.items.orEmpty()
                }
                if (blocks.isNotEmpty() || banners.isNotEmpty()) {
                    publishContent(TabUiState.Success(banners = banners, blocks = blocks))
                } else {
                    // Fallback to cached items
                    val cachedAll = sectionItemsRepository.getAllCachedItems()
                    if (cachedAll.isNotEmpty()) {
                        publishContent(TabUiState.Success(blocks = listOf(HomeBlockDto(blockId = "home", title = "Recommended", items = cachedAll))))
                    } else {
                        _uiState.value = TabUiState.Error("No content available")
                    }
                }
            }
            .onFailure { error ->
                val cachedAll = sectionItemsRepository.getAllCachedItems()
                if (cachedAll.isNotEmpty()) {
                    publishContent(TabUiState.Success(blocks = listOf(HomeBlockDto(blockId = "home", title = "Recommended", items = cachedAll))))
                } else {
                    _uiState.value = TabUiState.Error(error.message ?: "Failed to load Home")
                }
            }
    }

    private suspend fun loadCatalogData(tab: String) {
        val catalogResult = homeRepository.getCatalog(tab)
        if (catalogResult.isSuccess) {
            processCatalogResponse(tab, catalogResult.getOrThrow())
        } else {
            homeRepository.getHome(1)
                .onSuccess { homeResponse ->
                    processCatalogResponse(tab, homeResponse)
                }
                .onFailure {
                    val cachedAll = sectionItemsRepository.getAllCachedItems()
                    if (cachedAll.isNotEmpty()) {
                        publishContent(TabUiState.Success(blocks = listOf(HomeBlockDto(blockId = tab, title = tab.replaceFirstChar { it.uppercase() }, items = cachedAll))))
                    } else {
                        _uiState.value = TabUiState.Error("Failed to load ${tab.replaceFirstChar { it.uppercase() }}")
                    }
                }
        }
    }

    private fun processCatalogResponse(tab: String, response: HomeResponseDto) {
        val expectedKind = when (tab) {
            "movies" -> "movie"
            "series" -> "series"
            else -> null
        }
        val blocks = response.sections.orEmpty().mapNotNull { section ->
            val items = section.items.orEmpty().filter { item ->
                expectedKind == null || item.matchesKind(expectedKind)
            }
            if (items.isEmpty()) {
                null
            } else {
                HomeBlockDto(
                    blockId = section.title ?: "section",
                    title = section.title ?: tab.replaceFirstChar { it.uppercase() },
                    layout = "carousel",
                    items = items
                )
            }
        }

        val flatItems = response.items.orEmpty().filter { item ->
            expectedKind == null || item.matchesKind(expectedKind)
        }
        val allSectionItems = response.sections?.flatMap { it.items.orEmpty() }.orEmpty()

        val catalogBlocks = if (blocks.isNotEmpty()) {
            blocks
        } else if (flatItems.isNotEmpty()) {
            listOf(
                HomeBlockDto(
                    blockId = tab,
                    title = tab.replaceFirstChar { it.uppercase() },
                    layout = "carousel",
                    items = flatItems
                )
            )
        } else if (allSectionItems.isNotEmpty()) {
            listOf(
                HomeBlockDto(
                    blockId = tab,
                    title = tab.replaceFirstChar { it.uppercase() },
                    layout = "carousel",
                    items = allSectionItems
                )
            )
        } else {
            val cachedAll = sectionItemsRepository.getAllCachedItems()
            listOf(
                HomeBlockDto(
                    blockId = tab,
                    title = tab.replaceFirstChar { it.uppercase() },
                    layout = "carousel",
                    items = cachedAll.ifEmpty { response.banners.orEmpty() }
                )
            )
        }

        val banners = response.banners.orEmpty().ifEmpty {
            catalogBlocks.firstOrNull()?.items?.take(5) ?: flatItems.take(5)
        }

        publishContent(TabUiState.Success(banners = banners, blocks = catalogBlocks))
    }

    private fun publishContent(content: TabUiState.Success) {
        content.blocks.forEach { block ->
            if (!block.title.isNullOrEmpty() && !block.items.isNullOrEmpty()) {
                sectionItemsRepository.cacheSection(block.title!!, block.items!!)
            }
        }
        unfilteredContent = content
        _uiState.value = filterContent(content, familyModeRepository.enabled.value)
    }

    private fun filterContent(content: TabUiState.Success, familyModeEnabled: Boolean) =
            content.copy(
                banners = FamilyContentFilter.filter(content.banners, familyModeEnabled),
                blocks = content.blocks.mapNotNull { block ->
                    val items = FamilyContentFilter.filter(block.items.orEmpty(), familyModeEnabled)
                    if (items.isEmpty()) null else block.copy(items = items)
                }
            )

    private fun SubjectItemDto.matchesKind(expectedKind: String): Boolean =
            sequenceOf(kind, subjectType)
                .filterNotNull()
                .any { it.equals(expectedKind, ignoreCase = true) }

    @AssistedFactory
    interface Factory {
        fun create(tabName: String): TabContentViewModel
    }
}
