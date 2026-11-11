package com.nexapremium.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexapremium.ui.viewmodel.SettingsViewModel
import com.nexapremium.core.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val familyModeEnabled by viewModel.familyModeEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val autoMiniplayerEnabled by viewModel.autoMiniplayerEnabled.collectAsState()
    val backgroundDownloadEnabled by viewModel.backgroundDownloadEnabled.collectAsState()

    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogContent by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    if (infoDialogTitle != null && infoDialogContent != null) {
        AlertDialog(
            onDismissRequest = { infoDialogTitle = null },
            title = {
                Text(text = infoDialogTitle!!, color = PureWhite, style = Typography.titleLarge)
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    Text(text = infoDialogContent!!, color = MutedGray, fontSize = 14.sp, lineHeight = 20.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { infoDialogTitle = null }) {
                    Text(text = "Close", color = PrimaryRed)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        Text(
            text = "Settings",
            style = Typography.headlineLarge,
            color = PureWhite
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Your app and preferences",
            color = MutedGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Preferences Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Family Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Family Mode", color = PureWhite, fontSize = 16.sp)
                        Text(text = "Helps hide potentially mature videos.", color = MutedGray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = familyModeEnabled,
                        onCheckedChange = viewModel::setFamilyModeEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = PrimaryRed)
                    )
                }

                HorizontalDivider(color = SecondarySurface, thickness = 1.dp)

                // Notifications
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Notifications", color = PureWhite, fontSize = 16.sp)
                        Text(text = "Receive alerts for new releases and updates.", color = MutedGray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = viewModel::setNotificationsEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = PrimaryRed)
                    )
                }

                HorizontalDivider(color = SecondarySurface, thickness = 1.dp)

                // Auto activate miniplayer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Auto Activate Miniplayer", color = PureWhite, fontSize = 16.sp)
                        Text(text = "Play video in miniplayer when leaving screen.", color = MutedGray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = autoMiniplayerEnabled,
                        onCheckedChange = viewModel::setAutoMiniplayerEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = PrimaryRed)
                    )
                }

                HorizontalDivider(color = SecondarySurface, thickness = 1.dp)

                // Download in background
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Download in Background", color = PureWhite, fontSize = 16.sp)
                        Text(text = "Continue downloading when app is minimized.", color = MutedGray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = backgroundDownloadEnabled,
                        onCheckedChange = viewModel::setBackgroundDownloadEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = PrimaryRed)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "About & Legal",
            color = MutedGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // About & Legal Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SettingsActionRow(
                    title = "Check Update",
                    subtitle = "Current Version ${viewModel.appVersion}",
                    onClick = { Toast.makeText(context, "You are using the latest version (${viewModel.appVersion})", Toast.LENGTH_SHORT).show() }
                )
                HorizontalDivider(color = SecondarySurface, thickness = 1.dp)
                SettingsActionRow(
                    title = "About Us",
                    subtitle = "NEXAPREMIUM Streaming Service",
                    onClick = {
                        infoDialogTitle = "About Us"
                        infoDialogContent = "Welcome to NEXAPREMIUM, your ultimate destination for high-definition streaming of movies, series, anime, and kids' entertainment.\n\nDesigned with a sleek Netflix-inspired red and black interface, NEXAPREMIUM delivers lightning-fast streaming, smart downloads for offline viewing, multi-audio/dub support, and personalized recommendations.\n\nOur mission is to provide an immersive, ad-free, and cinematic entertainment experience across mobile, tablet, Android TV, and Fire TV devices."
                    }
                )
                HorizontalDivider(color = SecondarySurface, thickness = 1.dp)
                SettingsActionRow(
                    title = "Privacy and Policy",
                    subtitle = "Read our data & privacy policy",
                    onClick = {
                        infoDialogTitle = "Privacy and Policy"
                        infoDialogContent = "Last updated: May 2024\n\nAt NEXAPREMIUM, we respect your privacy and are committed to protecting your personal data.\n\n1. Information Collection: We collect minimal usage data and preference settings (such as Family Mode and download preferences) locally on your device.\n\n2. Data Security: All network requests and streaming connections are encrypted using industry-standard SSL/TLS protocols.\n\n3. Third-Party Services: We do not sell, trade, or rent your personal identification information to third parties.\n\n4. Your Rights: You have full control over your stored data, local downloads, and app settings at all times. You can clear all cached and downloaded content directly from the app."
                    }
                )
                HorizontalDivider(color = SecondarySurface, thickness = 1.dp)
                SettingsActionRow(
                    title = "User Agreement",
                    subtitle = "Terms of service and usage",
                    onClick = {
                        infoDialogTitle = "User Agreement"
                        infoDialogContent = "Last updated: May 2024\n\nBy accessing or using NEXAPREMIUM, you agree to be bound by these User Agreement terms.\n\n1. License & Usage: NEXAPREMIUM is provided for personal, non-commercial entertainment use. Redistribution, reverse engineering, or unauthorized commercial exploitation of app content is strictly prohibited.\n\n2. User Conduct: Users agree not to use the platform for any unlawful activities or in violation of regional copyright and streaming laws.\n\n3. Content Disclaimer: Streaming content is sourced via supported media pipelines. NEXAPREMIUM does not host copyrighted media files directly on its servers.\n\n4. Modifications: We reserve the right to modify, update, or discontinue features or services at any time without prior notice."
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, color = PureWhite, fontSize = 16.sp)
            Text(text = subtitle, color = MutedGray, fontSize = 12.sp)
        }
    }
}
