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
            RecommendedScreen(onItemClick = onItemClick, onSectionClick = onSectionClick)
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
                onItemClick = { detailPath ->
                    navController.navigate(Screen.MovieDetail.createRoute(detailPath))
                }
            )
        }

        composable(Screen.MyList.route) {
            MyListScreen(
                onItemClick = { detailPath ->
                    navController.navigate(Screen.MovieDetail.createRoute(detailPath))
                }
            )
        }

        composable(Screen.ContinueWatching.route) {
            ContinueWatchingScreen(
                onItemClick = { item ->
                    navController.navigate(Screen.Player.createRoute(item.detailPath, item.seasonNumber, item.episodeNumber))
                }
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                onItemClick = { detailPath ->
                    navController.navigate(Screen.Player.createRoute(detailPath, offline = true))
                }
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
                onPlayClick = { path, season, episode ->
                    navController.navigate(Screen.Player.createRoute(path, season, episode))
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
                onPlayClick = { path, season, episode ->
                    navController.navigate(Screen.Player.createRoute(path, season, episode))
                }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("detailPath") { type = NavType.StringType },
                navArgument("season") { type = NavType.IntType; defaultValue = 0 },
                navArgument("episode") { type = NavType.IntType; defaultValue = 0 },
                navArgument("offline") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val encodedDetailPath = backStackEntry.arguments?.getString("detailPath") ?: ""
            val detailPath = Uri.decode(encodedDetailPath)
            val season = backStackEntry.arguments?.getInt("season") ?: 0
            val episode = backStackEntry.arguments?.getInt("episode") ?: 0
            val offline = backStackEntry.arguments?.getBoolean("offline") ?: false

            VideoPlayerScreen(
                detailPath = detailPath,
                season = season,
                episode = episode,
                offlineOnly = offline,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
