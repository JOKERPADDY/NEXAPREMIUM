package com.nexapremium.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nexapremium.ui.screens.*

@Composable
fun NexaNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Recommended.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        val onItemClick: (String, String) -> Unit = { detailPath, subjectType ->
            if (subjectType.equals("series", ignoreCase = true) || subjectType.equals("tv", ignoreCase = true)) {
                navController.navigate(Screen.SeriesDetail.createRoute(detailPath))
            } else {
                navController.navigate(Screen.MovieDetail.createRoute(detailPath))
            }
        }

        val onSectionClick: (String) -> Unit = { title ->
            navController.navigate(Screen.SectionItems.createRoute(title))
        }

        composable(Screen.Recommended.route) {
            RecommendedScreen(
                onItemClick = onItemClick,
                onSectionClick = onSectionClick,
                onSportsClick = { id, title -> navController.navigate(Screen.SportsPlayer.createRoute(id, title)) }
            )
        }

        composable(
            route = Screen.SportsPlayer.route,
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { entry ->
            SportsPlayerScreen(
                matchId = Uri.decode(entry.arguments?.getString("matchId").orEmpty()),
                title = Uri.decode(entry.arguments?.getString("title").orEmpty()),
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SectionItems.route,
            arguments = listOf(navArgument("title") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedTitle = backStackEntry.arguments?.getString("title").orEmpty()
            SectionItemsScreen(
                title = Uri.decode(encodedTitle),
                onBackClick = { navController.popBackStack() },
                onItemClick = onItemClick
            )
        }

        composable(Screen.Movies.route) {
            MoviesScreen(onMovieClick = onItemClick, onSectionClick = onSectionClick)
        }

        composable(Screen.Series.route) {
            SeriesScreen(onSeriesClick = onItemClick, onSectionClick = onSectionClick)
        }

        composable(Screen.Kids.route) {
            KidsScreen(onItemClick = onItemClick, onSectionClick = onSectionClick)
        }

        composable(Screen.Anime.route) {
            AnimeScreen(onItemClick = onItemClick, onSectionClick = onSectionClick)
        }

        composable(
            route = Screen.Search.route,
            arguments = listOf(navArgument("query") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val encodedQuery = backStackEntry.arguments?.getString("query")
            val initialQuery = encodedQuery?.let { Uri.decode(it) } ?: ""
            SearchScreen(
                initialQuery = initialQuery,
                onBackClick = { navController.popBackStack() },
                onItemClick = onItemClick
            )
        }

        composable(Screen.MyList.route) {
            MyListScreen(
                onItemClick = onItemClick
            )
        }

        composable(Screen.ContinueWatching.route) {
            ContinueWatchingScreen(
                onItemClick = { item ->
                    navController.navigate(Screen.Player.createRoute(item.detailPath, item.seasonNumber, item.episodeNumber, coverUrl = item.coverUrl))
                }
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                onItemClick = { detailPath, season, episode, coverUrl ->
                    navController.navigate(Screen.Player.createRoute(detailPath, season, episode, offline = true, coverUrl = coverUrl))
                },
                onBrowseClick = {
                    navController.navigate(Screen.Recommended.route) {
                        popUpTo(Screen.Recommended.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenTitle = onItemClick
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        composable(
            route = Screen.MovieDetail.route,
            arguments = listOf(navArgument("detailPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedDetailPath = backStackEntry.arguments?.getString("detailPath") ?: ""
            val detailPath = Uri.decode(encodedDetailPath)
            MovieDetailScreen(
                detailPath = detailPath,
                onPlayClick = { path, season, episode, coverUrl ->
                    navController.navigate(Screen.Player.createRoute(path, season, episode, coverUrl = coverUrl))
                }
            )
        }

        composable(
            route = Screen.SeriesDetail.route,
            arguments = listOf(navArgument("detailPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedDetailPath = backStackEntry.arguments?.getString("detailPath") ?: ""
            val detailPath = Uri.decode(encodedDetailPath)
            SeriesDetailScreen(
                detailPath = detailPath,
                onPlayClick = { path, season, episode, coverUrl ->
                    navController.navigate(Screen.Player.createRoute(path, season, episode, coverUrl = coverUrl))
                }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("detailPath") { type = NavType.StringType },
                navArgument("season") { type = NavType.IntType; defaultValue = 0 },
                navArgument("episode") { type = NavType.IntType; defaultValue = 0 },
                navArgument("offline") { type = NavType.BoolType; defaultValue = false },
                navArgument("coverUrl") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val encodedDetailPath = backStackEntry.arguments?.getString("detailPath") ?: ""
            val detailPath = Uri.decode(encodedDetailPath)
            val season = backStackEntry.arguments?.getInt("season") ?: 0
            val episode = backStackEntry.arguments?.getInt("episode") ?: 0
            val offline = backStackEntry.arguments?.getBoolean("offline") ?: false
            val coverUrl = backStackEntry.arguments?.getString("coverUrl").orEmpty()

            VideoPlayerScreen(
                detailPath = detailPath,
                season = season,
                episode = episode,
                offlineOnly = offline,
                coverUrl = coverUrl,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
