package com.nexapremium.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapremium.data.filter.FamilyContentFilter
import com.nexapremium.data.api.model.HomeBlockDto
import com.nexapremium.data.api.model.HomeResponseDto
import com.nexapremium.data.api.model.SubjectItemDto
import com.nexapremium.data.repository.FamilyModeRepository
import com.nexapremium.data.repository.HomeRepository
import com.nexapremium.data.repository.LocalDatabaseRepository
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
    private val localDatabaseRepository: LocalDatabaseRepository,
    @Assisted private val tabName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<TabUiState>(TabUiState.Loading)
    val uiState: StateFlow<TabUiState> = _uiState.asStateFlow()
    private var unfilteredContent: TabUiState.Success? = null
    private var continueWatchingItems: List<com.nexapremium.data.db.entity.ContinueWatchingEntity> = emptyList()

    init {
        viewModelScope.launch {
            localDatabaseRepository.getContinueWatchingFlow().collect { list ->
                continueWatchingItems = list
                unfilteredContent?.let { currentSuccess ->
                    val enriched = enrichWithSmartRecommendations(currentSuccess)
                    _uiState.value = filterContent(enriched, familyModeRepository.enabled.value)
                }
            }
        }

        viewModelScope.launch {
            familyModeRepository.enabled.collect { enabled ->
                unfilteredContent?.let { currentSuccess ->
                    val enriched = enrichWithSmartRecommendations(currentSuccess)
                    _uiState.value = filterContent(enriched, enabled)
                }
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
        val enriched = enrichWithSmartRecommendations(content)
        _uiState.value = filterContent(enriched, familyModeRepository.enabled.value)
    }

    private fun enrichWithSmartRecommendations(content: TabUiState.Success): TabUiState.Success {
        if (!tabName.equals("recommended", ignoreCase = true) && !tabName.equals("home", ignoreCase = true)) {
            return content
        }

        val baseBlocks = content.blocks
        if (baseBlocks.isEmpty()) return content

        val mutableBlocks = baseBlocks.toMutableList()
        val allItems = baseBlocks.flatMap { it.items.orEmpty() }.distinctBy { it.detailPath ?: it.title }
        if (allItems.isEmpty()) return content

        // 1. "Because You Watched <Title>" Recommendation Row
        val lastWatched = continueWatchingItems.firstOrNull()
        if (lastWatched != null) {
            val cleanTitle = lastWatched.title
                .replace(Regex("[_\\s]+S\\d+E\\d+$", RegexOption.IGNORE_CASE), "")
                .replace(Regex("[_\\s]+Season\\s*\\d+.*$", RegexOption.IGNORE_CASE), "")
                .replace(Regex("[_\\s]+Episode\\s*\\d+.*$", RegexOption.IGNORE_CASE), "")
                .trim()

            val watchedItem = allItems.firstOrNull { 
                it.detailPath == lastWatched.detailPath || 
                it.title?.equals(cleanTitle, ignoreCase = true) == true 
            }
            val watchedGenres = watchedItem?.genres.orEmpty()

            val recommendedItems = allItems.filter { item ->
                item.detailPath != lastWatched.detailPath &&
                item.title?.contains(cleanTitle, ignoreCase = true) != true
            }.sortedByDescending { item ->
                if (watchedGenres.isNotEmpty() && !item.genres.isNullOrEmpty()) {
                    item.genres.count { it in watchedGenres }
                } else 0
            }.take(12)

            if (recommendedItems.isNotEmpty()) {
                val blockTitle = "Because You Watched \"$cleanTitle\" 🎬"
                mutableBlocks.removeAll { it.blockId?.startsWith("because_you_watched") == true }

                val insertIndex = (1).coerceAtMost(mutableBlocks.size)
                mutableBlocks.add(
                    insertIndex,
                    HomeBlockDto(
                        blockId = "because_you_watched_${lastWatched.playbackKey.hashCode()}",
                        title = blockTitle,
                        layout = "carousel",
                        items = recommendedItems
                    )
                )
            }
        }

        // 2. "Weekend Binge Specials 🍿" / "Top Picks For You ✨" Row
        val dayOfWeek = java.time.LocalDate.now().dayOfWeek.value // 5 = Fri, 6 = Sat, 7 = Sun
        val isWeekend = dayOfWeek >= 5
        val smartTitle = if (isWeekend) "Weekend Binge Specials 🍿" else "Top Picks For You ✨"

        val smartItems = allItems.shuffled().take(10)
        if (smartItems.size >= 3) {
            mutableBlocks.removeAll { it.blockId == "smart_weekend_picks" }
            val insertIndex = (0).coerceAtMost(mutableBlocks.size)
            mutableBlocks.add(
                insertIndex,
                HomeBlockDto(
                    blockId = "smart_weekend_picks",
                    title = smartTitle,
                    layout = "carousel",
                    items = smartItems
                )
            )
        }

        return content.copy(blocks = mutableBlocks)
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
