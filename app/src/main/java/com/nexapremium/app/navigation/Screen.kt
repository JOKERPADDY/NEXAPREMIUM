package com.nexapremium.app.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Recommended : Screen("recommended")
    object Movies : Screen("movies")
    object Series : Screen("series")
    object Kids : Screen("kids")
    object Anime : Screen("anime")
    object Search : Screen("search?query={query}") {
        fun createRoute(query: String? = null): String {
            return if (!query.isNullOrEmpty()) {
                val encoded = Uri.encode(query)
                "search?query=$encoded"
            } else {
                "search"
            }
        }
    }
    object SectionItems : Screen("section/{title}") {
        fun createRoute(title: String): String = "section/${Uri.encode(title)}"
    }
    object MovieDetail : Screen("movie_detail/{detailPath}") {
        fun createRoute(detailPath: String): String {
            val encoded = Uri.encode(detailPath)
            return "movie_detail/$encoded"
        }
    }
    object SeriesDetail : Screen("series_detail/{detailPath}") {
        fun createRoute(detailPath: String): String {
            val encoded = Uri.encode(detailPath)
            return "series_detail/$encoded"
        }
    }
    object Player : Screen("player/{detailPath}?season={season}&episode={episode}&offline={offline}") {
        fun createRoute(detailPath: String, season: Int = 0, episode: Int = 0, offline: Boolean = false): String {
            val encoded = Uri.encode(detailPath)
            return "player/$encoded?season=$season&episode=$episode&offline=$offline"
        }
    }
    object MyList : Screen("mylist")
    object ContinueWatching : Screen("continuewatching")
    object Downloads : Screen("downloads")
    object Settings : Screen("settings")
}
