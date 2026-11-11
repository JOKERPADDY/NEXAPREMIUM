package com.nexapremium.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexapremium.data.api.model.SubjectItemDto
import com.nexapremium.data.filter.FamilyContentFilter
import com.nexapremium.data.repository.FamilyModeRepository
import com.nexapremium.data.repository.SectionItemsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SectionItemsViewModel @Inject constructor(
    private val sectionItemsRepository: SectionItemsRepository,
    private val familyModeRepository: FamilyModeRepository
) : ViewModel() {
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _items = MutableStateFlow<List<SubjectItemDto>>(emptyList())
    val items: StateFlow<List<SubjectItemDto>> = _items.asStateFlow()

    init {
        viewModelScope.launch {
            familyModeRepository.enabled.collect { enabled ->
                val cached = sectionItemsRepository.getSectionItemsExact(_title.value).orEmpty()
                _items.value = FamilyContentFilter.filter(cached, enabled)
            }
        }
    }

    fun load(title: String) {
        _title.value = title
        _items.value = FamilyContentFilter.filter(
            sectionItemsRepository.getSectionItemsExact(title).orEmpty(),
            familyModeRepository.enabled.value
        )
    }
}
