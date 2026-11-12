package com.nexapremium.app

import android.os.Bundle
import android.os.Build
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.app.PictureInPictureParams
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nexapremium.app.navigation.NexaNavHost
import com.nexapremium.app.navigation.Screen
import com.nexapremium.core.ui.theme.NexaTheme
import com.nexapremium.core.ui.theme.PureBlack
import com.nexapremium.data.update.AppUpdateChecker
import com.nexapremium.data.update.AppUpdateInfo
import com.nexapremium.ui.components.AppUpdateDialog
import com.nexapremium.ui.components.BottomNavigationBar
import com.nexapremium.ui.components.TopNavigationBar
import com.nexapremium.ui.components.isTelevisionDevice
import dagger.hilt.android.AndroidEntryPoint

import com.nexapremium.ui.components.TelegramPromptDialog
import android.content.Context
import kotlinx.coroutines.delay
import com.nexapremium.player.NexaPlayerEngine
import com.nexapremium.data.repository.PlaybackPreferencesRepository
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var playerEngine: NexaPlayerEngine
    @Inject lateinit var playbackPreferencesRepository: PlaybackPreferencesRepository

    @Volatile private var playerScreenActive = false
    private var inPipMode by mutableStateOf(false)

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
                    playerScreenActive = currentRoute == Screen.Player.route
                    val isTelevision = isTelevisionDevice(this@MainActivity)

                    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
                    var showTelegramPrompt by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        val prefs = getSharedPreferences("nexapremium_preferences", Context.MODE_PRIVATE)
                        if (!prefs.getBoolean("has_shown_telegram_prompt", false)) {
                            prefs.edit().putBoolean("has_shown_telegram_prompt", true).apply()
                            delay(1200)
                            showTelegramPrompt = true
                        }

                        val info = AppUpdateChecker.checkUpdate(this@MainActivity)
                        if (info.hasUpdate) {
                            updateInfo = info
                        }
                    }

                    val showTopCategoryBar = currentRoute == Screen.Recommended.route ||
                            currentRoute == Screen.Movies.route ||
                            currentRoute == Screen.Series.route ||
                            currentRoute == Screen.Kids.route ||
                            currentRoute == Screen.Anime.route

                    val showBottomNav = !inPipMode && currentRoute != Screen.Player.route &&
                            currentRoute != Screen.SportsPlayer.route &&
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
                                    showDownloads = !isTelevision,
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
                            if (showTopCategoryBar && !inPipMode) {
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

                        updateInfo?.let { info ->
                            AppUpdateDialog(
                                updateInfo = info,
                                onDismiss = { updateInfo = null }
                            )
                        }

                        if (showTelegramPrompt && updateInfo == null) {
                            TelegramPromptDialog(
                                onDismiss = { showTelegramPrompt = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            !playerScreenActive ||
            !playbackPreferencesRepository.autoPictureInPicture.value ||
            !packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) return
        val player = playerEngine.currentPlayer() ?: return
        if (!player.isPlaying) return
        enterPictureInPictureMode(
            PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
        )
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        this.inPipMode = isInPictureInPictureMode
    }
}
