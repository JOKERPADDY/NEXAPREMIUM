package com.nexapremium.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexapremium.app.navigation.NexaNavHost
import com.nexapremium.app.navigation.Screen
import com.nexapremium.core.ui.theme.NexaTheme
import com.nexapremium.core.ui.theme.PureBlack
import com.nexapremium.ui.components.BottomNavigationBar
import com.nexapremium.ui.components.TopNavigationBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PureBlack
                ) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ ->
                        // Notification permission result handled
                    }

                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    val showTopCategoryBar = currentRoute == Screen.Recommended.route ||
                            currentRoute == Screen.Movies.route ||
                            currentRoute == Screen.Series.route ||
                            currentRoute == Screen.Kids.route ||
                            currentRoute == Screen.Anime.route

                    val showBottomNav = currentRoute != Screen.Player.route &&
                            currentRoute != Screen.Splash.route &&
                            currentRoute?.startsWith("movie_detail") != true &&
                            currentRoute?.startsWith("series_detail") != true

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = PureBlack,
                        bottomBar = {
                            if (showBottomNav) {
                                BottomNavigationBar(
                                    currentRoute = currentRoute,
                                    onItemSelected = { route ->
                                        navController.navigate(route) {
                                            popUpTo(Screen.Recommended.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (showTopCategoryBar) {
                                TopNavigationBar(
                                    currentRoute = currentRoute,
                                    onTabSelected = { route ->
                                        navController.navigate(route) {
                                            popUpTo(Screen.Recommended.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onSearchClick = {
                                        navController.navigate(Screen.Search.createRoute())
                                    }
                                )
                            }

                            NexaNavHost(
                                navController = navController,
                                modifier = Modifier.weight(1f),
                                startDestination = Screen.Splash.route
                            )
                        }
                    }
                }
            }
        }
    }
}
