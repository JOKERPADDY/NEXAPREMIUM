package com.nexapremium.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentSearchesRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _recentSearches = MutableStateFlow(readSearches())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    fun add(query: String) {
        val normalized = query.trim().replace(Regex("\\s+"), " ").take(MAX_QUERY_LENGTH)
        if (normalized.isBlank()) return

        save(
            listOf(normalized) + _recentSearches.value.filterNot {
                it.equals(normalized, ignoreCase = true)
            }
        )
    }

    fun remove(query: String) {
        save(_recentSearches.value.filterNot { it.equals(query, ignoreCase = true) })
    }

    fun clear() {
        save(emptyList())
    }

    private fun readSearches(): List<String> =
        preferences.getString(KEY_RECENT_SEARCHES, null)
            ?.split(SEARCH_SEPARATOR)
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.distinctBy { it.lowercase() }
            ?.take(MAX_RECENT_SEARCHES)
            .orEmpty()

    private fun save(searches: List<String>) {
        val updated = searches.take(MAX_RECENT_SEARCHES)
        preferences.edit {
            if (updated.isEmpty()) {
                remove(KEY_RECENT_SEARCHES)
            } else {
                putString(KEY_RECENT_SEARCHES, updated.joinToString(SEARCH_SEPARATOR))
            }
        }
        _recentSearches.value = updated
    }

    private companion object {
        const val PREFERENCES_NAME = "nexapremium_search_preferences"
        const val KEY_RECENT_SEARCHES = "recent_searches"
        const val SEARCH_SEPARATOR = "\n"
        const val MAX_RECENT_SEARCHES = 8
        const val MAX_QUERY_LENGTH = 120
    }
}
