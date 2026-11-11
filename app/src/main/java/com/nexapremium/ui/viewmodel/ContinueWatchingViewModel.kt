package com.nexapremium.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapremium.data.db.entity.ContinueWatchingEntity
import com.nexapremium.data.repository.LocalDatabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContinueWatchingViewModel @Inject constructor(
    private val localDatabaseRepository: LocalDatabaseRepository
) : ViewModel() {

    val continueWatchingList: StateFlow<List<ContinueWatchingEntity>> = localDatabaseRepository.getContinueWatchingFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun remove(detailPath: String) {
        viewModelScope.launch { localDatabaseRepository.removeContinueWatching(detailPath) }
    }
}
