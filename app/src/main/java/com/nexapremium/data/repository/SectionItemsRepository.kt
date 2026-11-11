package com.nexapremium.data.repository

import com.nexapremium.data.api.model.SubjectItemDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SectionItemsRepository @Inject constructor() {
    private val sectionMap = mutableMapOf<String, List<SubjectItemDto>>()

    fun cacheSection(title: String, items: List<SubjectItemDto>) {
        if (title.isNotBlank() && items.isNotEmpty()) {
            sectionMap[title.lowercase().trim()] = items
        }
    }

    fun getSectionItems(title: String): List<SubjectItemDto>? {
        val key = title.lowercase().trim()
        sectionMap[key]?.let { return it }
        // Partial or fuzzy match
        sectionMap.entries.firstOrNull { it.key.contains(key) || key.contains(it.key) }?.let { return it.value }
        return null
    }

    fun getSectionItemsExact(title: String): List<SubjectItemDto>? =
        sectionMap[title.lowercase().trim()]

    fun getAllCachedItems(): List<SubjectItemDto> {
        return sectionMap.values.flatten().distinctBy { it.detailPath ?: it.title }
    }
}
