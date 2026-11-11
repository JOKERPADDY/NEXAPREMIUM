package com.nexapremium.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapremium.data.db.entity.MyListEntity
import com.nexapremium.data.repository.LocalDatabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MyListViewModel @Inject constructor(
    private val localDatabaseRepository: LocalDatabaseRepository
) : ViewModel() {

    val myList: StateFlow<List<MyListEntity>> = localDatabaseRepository.getMyListFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
