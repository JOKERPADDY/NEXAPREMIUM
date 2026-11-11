package com.nexapremium.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapremium.data.filter.FamilyContentFilter
import com.nexapremium.data.api.model.CoverDto
import com.nexapremium.data.api.model.DubDto
import com.nexapremium.data.api.model.EpisodeDto
import com.nexapremium.data.api.model.MovieDetailDto
import com.nexapremium.data.api.model.SeasonDto
import com.nexapremium.data.api.model.SubjectItemDto
import com.nexapremium.data.api.model.TagDto
import com.nexapremium.data.db.entity.DownloadEntity
import com.nexapremium.data.download.AppDownloadManager
import com.nexapremium.data.db.entity.MyListEntity
import com.nexapremium.data.repository.DetailRepository
import com.nexapremium.data.repository.FamilyModeRepository
import com.nexapremium.data.repository.LocalDatabaseRepository
import com.nexapremium.data.repository.PlaybackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import retrofit2.HttpException
import javax.inject.Inject

data class DetailScreenState(
    val movieDetail: MovieDetailDto,
    val dubs: List<DubDto> = emptyList(),
    val seasons: List<SeasonDto> = emptyList(),
    val selectedDub: DubDto? = null,
    val selectedSeason: Int = 1,
    val episodes: List<EpisodeDto> = emptyList(),
    val recommendations: List<SubjectItemDto> = emptyList(),
    val isBookmarked: Boolean = false
)

sealed interface DetailUiState {
    object Loading : DetailUiState
    data class Success(val data: DetailScreenState) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val detailRepository: DetailRepository,
    private val playbackRepository: PlaybackRepository,
    private val appDownloadManager: AppDownloadManager,
    private val localDatabaseRepository: LocalDatabaseRepository,
    private val familyModeRepository: FamilyModeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    private var loadedTitle: SubjectItemDto? = null
    private var loadedDetailPath: String? = null
    private var unfilteredRecommendations: List<SubjectItemDto> = emptyList()

    init {
        viewModelScope.launch {
            familyModeRepository.enabled.collect { enabled ->
                val title = loadedTitle
                if (title != null && !FamilyContentFilter.isAllowed(title)) {
                    if (enabled) {
                        _uiState.value = DetailUiState.Error("This title is hidden by Family Mode")
                    } else if ((_uiState.value as? DetailUiState.Error)?.message ==
                        "This title is hidden by Family Mode"
                    ) {
                        loadedDetailPath?.let(::loadDetail)
                    }
                }
                updateRecommendations(enabled)
            }
        }
    }

    fun loadDetail(detailPath: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            detailRepository.getTitleDetail(detailPath)
                .onSuccess { response ->
                    val detailObj = response.detailResponse
                    val titleItem = detailObj.title
                    loadedTitle = titleItem
                    loadedDetailPath = detailPath

                    if (familyModeRepository.enabled.value &&
                        titleItem != null &&
                        !FamilyContentFilter.isAllowed(titleItem)
                    ) {
                        _uiState.value = DetailUiState.Error("This title is hidden by Family Mode")
                        return@onSuccess
                    }

                    val movieDetailDto = if (titleItem != null) {
                        MovieDetailDto(
                            subjectId = titleItem.effectiveId,
                            detailPath = titleItem.detailPath ?: detailPath,
                            title = titleItem.title,
                            originalTitle = titleItem.title,
                            description = titleItem.description,
                            releaseDate = titleItem.releaseDate,
                            releaseYear = titleItem.releaseYear ?: titleItem.releaseDate?.take(4)?.toIntOrNull(),
                            durationMinutes = titleItem.durationSeconds?.let { it / 60 },
                            score = titleItem.score ?: titleItem.rating?.toDoubleOrNull() ?: 0.0,
                            cover = CoverDto(url = titleItem.effectiveCoverUrl),
                            backdrop = CoverDto(url = titleItem.backdropUrl ?: titleItem.effectiveCoverUrl),
                            genres = titleItem.genres?.map { TagDto(name = it) } ?: emptyList(),
                            actors = detailObj.cast ?: emptyList(),
                            directors = emptyList()
                        )
                    } else null

                    if (movieDetailDto != null) {
                        val dubs = detailObj.dubs ?: emptyList()
                        val seasons = detailObj.seasons ?: emptyList()
                        val initialSeason = seasons.firstOrNull()?.number ?: 0

                        val screenState = DetailScreenState(
                            movieDetail = movieDetailDto,
                            dubs = dubs,
                            seasons = seasons,
                            selectedDub = dubs.firstOrNull { it.original == true } ?: dubs.firstOrNull(),
                            selectedSeason = initialSeason
                        )
                        _uiState.value = DetailUiState.Success(screenState)

                        if (seasons.isNotEmpty()) {
                            loadEpisodes(detailPath, initialSeason)
                        }
                        loadRecommendations(detailPath)
                    } else {
                        _uiState.value = DetailUiState.Error("Title detail not found")
                    }
                }
                .onFailure { error ->
                    val message = if (error is HttpException && error.code() == 404) {
                        "This title is no longer available. Try another title."
                    } else {
                        error.message ?: "Failed to load detail"
                    }
                    _uiState.value = DetailUiState.Error(message)
                }
        }
    }

    fun selectSeason(detailPath: String, seasonNumber: Int) {
        val currentState = _uiState.value
        if (currentState is DetailUiState.Success) {
            _uiState.value = DetailUiState.Success(currentState.data.copy(selectedSeason = seasonNumber))
            loadEpisodes(detailPath, seasonNumber)
        }
    }

    fun selectDub(dub: DubDto) {
        val currentState = _uiState.value
        if (currentState is DetailUiState.Success) {
            _uiState.value = DetailUiState.Success(currentState.data.copy(selectedDub = dub))
        }
    }

    private fun loadEpisodes(detailPath: String, season: Int) {
        viewModelScope.launch {
            detailRepository.getSeriesEpisodes(detailPath, season)
                .onSuccess { episodeList ->
                    val currentState = _uiState.value
                    if (currentState is DetailUiState.Success) {
                        _uiState.value = DetailUiState.Success(currentState.data.copy(episodes = episodeList))
                    }
                }
        }
    }

    private fun loadRecommendations(detailPath: String) {
        viewModelScope.launch {
            detailRepository.getRecommendations(detailPath)
                .onSuccess { response ->
                    val recs = mutableListOf<SubjectItemDto>()
                    response.items?.let { recs.addAll(it) }
                    response.banners?.let { recs.addAll(it) }
                    response.sections?.forEach { sec ->
                        sec.items?.let { recs.addAll(it) }
                    }
                    unfilteredRecommendations = recs.distinctBy { it.id ?: it.detailPath ?: it.title }
                    updateRecommendations(familyModeRepository.enabled.value)
                }
        }
    }

    private fun updateRecommendations(familyModeEnabled: Boolean) {
        val currentState = _uiState.value
        if (currentState is DetailUiState.Success) {
            _uiState.value = DetailUiState.Success(
                currentState.data.copy(
                    recommendations = FamilyContentFilter.filter(
                        unfilteredRecommendations,
                        familyModeEnabled
                    )
                )
            )
        }
    }

    suspend fun getDirectDownloadSources(detailPath: String, season: Int = 0, episode: Int = 0) =
        playbackRepository.getPlayback(detailPath, season, episode).getOrNull()
            ?.playbackData?.playableSources
            ?.filter { source ->
                source.vipLocked != true &&
                    (source.format.equals("MP4", ignoreCase = true) || source.url?.contains(".mp4", ignoreCase = true) == true)
            }
            .orEmpty()

    suspend fun getEpisodeDownloadSources(
        detailPath: String,
        season: Int,
        episodeNumbers: List<Int>
    ): Map<Int, List<com.nexapremium.data.api.model.MediaSourceDto>> = coroutineScope {
        val semaphore = Semaphore(4)
        episodeNumbers.distinct().map { episodeNumber ->
            async {
                episodeNumber to semaphore.withPermit {
                    getDirectDownloadSources(detailPath, season, episodeNumber)
                }
            }
        }.awaitAll().toMap()
    }

    fun downloadTitle(context: Context, detailPath: String, titleName: String, season: Int = 0, episode: Int = 0, quality: String? = null, sourceId: String? = null) {
        viewModelScope.launch {
            Toast.makeText(context, "Preparing private download for $titleName...", Toast.LENGTH_SHORT).show()
            playbackRepository.getPlayback(detailPath, season, episode)
                .onSuccess { response ->
                    fun isDirectMp4(source: com.nexapremium.data.api.model.MediaSourceDto) =
                        source.vipLocked != true &&
                            (source.format.equals("MP4", ignoreCase = true) || source.url?.contains(".mp4", ignoreCase = true) == true)
                    fun normalize(value: String) = value.trim().uppercase().removeSuffix("P")
                    val source = when {
                        sourceId != null -> response.playbackData.playableSources.firstOrNull {
                            it.id == sourceId && isDirectMp4(it)
                        }
                        quality != null -> response.playbackData.playableSources.firstOrNull {
                            isDirectMp4(it) && normalize(it.effectiveQuality) == normalize(quality)
                        }
                        else -> response.playbackData.playableSources.firstOrNull { isDirectMp4(it) }
                            ?: response.playbackData.playableSources.firstOrNull()
                            ?: response.playbackData.effectiveSources.firstOrNull()
                    }

                    if (source == null && (quality != null || sourceId != null)) {
                        Toast.makeText(context, "Selected quality is unavailable for download", Toast.LENGTH_SHORT).show()
                        return@onSuccess
                    }

                    val downloadUrl = source?.url
                    if (!downloadUrl.isNullOrEmpty()) {
                        try {
                            val hiddenFolder = File(context.getExternalFilesDir(null), ".downloads").apply {
                                if (!exists()) mkdirs()
                            }
                            File(hiddenFolder, ".nomedia").createNewFile()

                            val fileName = "${detailPath.take(30)}_S${season}E${episode}.mp4"
                            val destinationFile = File(hiddenFolder, fileName)

                            val currentState = _uiState.value
                            val coverUrl = if (currentState is DetailUiState.Success) {
                                currentState.data.movieDetail.cover?.url ?: ""
                            } else ""

                            val cleanPath = detailPath.trimStart('/')
                            val refererPath = if (cleanPath.startsWith("movies/") || cleanPath.startsWith("series/")) cleanPath else "movies/$cleanPath"

                            val requestHeaders = buildMap {
                                put("User-Agent", "okhttp/4.11.0")
                                put("Referer", "https://themoviebox.xyz/$refererPath")
                                put("Origin", "https://themoviebox.xyz")
                                putAll(source.effectiveHeaders)
                            }

                            val downloadEntity = DownloadEntity(
                                id = "${detailPath}_S${season}E${episode}",
                                detailPath = detailPath,
                                title = titleName,
                                coverUrl = coverUrl,
                                filePath = destinationFile.absolutePath,
                                fileSizeMb = (source.sizeBytes ?: 0L) / (1024.0 * 1024.0),
                                quality = source.effectiveQuality
                            )
                            localDatabaseRepository.saveDownload(downloadEntity)
                            try {
                                appDownloadManager.enqueue(downloadEntity, downloadUrl, requestHeaders)
                            } catch (error: Exception) {
                                localDatabaseRepository.deleteDownload(downloadEntity.id)
                                throw error
                            }

                            Toast.makeText(context, "Download added to NEXAPREMIUM Downloads", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "No download link available", Toast.LENGTH_SHORT).show()
                    }
                }
                .onFailure {
                    Toast.makeText(context, "Failed to initiate download", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun shareTitle(context: Context, titleName: String, detailPath: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, titleName)
            putExtra(Intent.EXTRA_TEXT, "Check out $titleName on NEXAPREMIUM!\nhttps://themoviebox.xyz/movies/$detailPath")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share $titleName"))
    }

    fun toggleBookmark(detailPath: String, title: String, coverUrl: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is DetailUiState.Success) {
                val item = MyListEntity(
                    detailPath = detailPath,
                    title = title,
                    coverUrl = coverUrl,
                    subjectType = "movie"
                )
                localDatabaseRepository.toggleBookmark(item, currentState.data.isBookmarked)
                val updatedData = currentState.data.copy(isBookmarked = !currentState.data.isBookmarked)
                _uiState.value = DetailUiState.Success(updatedData)
            }
        }
    }
}
