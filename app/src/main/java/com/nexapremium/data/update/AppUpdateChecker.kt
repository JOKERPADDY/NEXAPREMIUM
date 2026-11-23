package com.nexapremium.data.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.nexapremium.BuildConfig
import com.nexapremium.ui.components.isTelevisionDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseTitle: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = "https://github.com/JOKERPADDY/NEXAPREMIUM/releases",
    val releasePageUrl: String = "https://github.com/JOKERPADDY/NEXAPREMIUM/releases",
    val checkFailed: Boolean = false
)

object AppUpdateChecker {

    private const val TAG = "AppUpdateChecker"
    private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/JOKERPADDY/NEXAPREMIUM/releases/latest"

    suspend fun checkUpdate(context: Context, client: OkHttpClient = OkHttpClient()): AppUpdateInfo = withContext(Dispatchers.IO) {
        val currentVersion = try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName?.takeIf { it.isNotBlank() } ?: BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            BuildConfig.VERSION_NAME
        }

        try {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "NexaPremium-AndroidApp")
                .header("Cache-Control", "no-cache")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful || body.isBlank()) {
                    Log.w(TAG, "GitHub API returned code ${response.code}")
                    return@withContext AppUpdateInfo(false, currentVersion, currentVersion, checkFailed = true)
                }

                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "").ifBlank { json.optString("name", "") }
                val cleanLatest = tagName.trim().removePrefix("v").removePrefix("V")
                val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")
                if (cleanLatest.isBlank()) {
                    return@withContext AppUpdateInfo(false, currentVersion, currentVersion, checkFailed = true)
                }

                val releaseTitle = json.optString("name", "Version $tagName")
                val releaseNotes = json.optString("body", "Bug fixes and performance improvements.")
                val htmlUrl = json.optString("html_url", "https://github.com/JOKERPADDY/NEXAPREMIUM/releases")

                var apkDownloadUrl = htmlUrl
                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    val isTv = isTelevisionDevice(context)
                    var bestUrl = ""
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "").lowercase()
                        val downloadUrl = asset.optString("browser_download_url", "")
                        if (name.endsWith(".apk") && downloadUrl.isNotBlank()) {
                            if (isTv && (name.contains("tv") || name.contains("leanback") || name.contains("fire"))) {
                                bestUrl = downloadUrl
                                break
                            } else if (!isTv && !name.contains("tv") && !name.contains("leanback")) {
                                if (bestUrl.isBlank()) bestUrl = downloadUrl
                            } else if (bestUrl.isBlank()) {
                                bestUrl = downloadUrl
                            }
                        }
                    }
                    if (bestUrl.isNotBlank()) {
                        apkDownloadUrl = bestUrl
                    }
                }

                val hasNewer = isNewerVersion(cleanLatest, cleanCurrent)

                AppUpdateInfo(
                    hasUpdate = hasNewer,
                    latestVersion = if (tagName.isNotBlank()) tagName else cleanLatest,
                    currentVersion = currentVersion,
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    downloadUrl = apkDownloadUrl,
                    releasePageUrl = htmlUrl
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking update from GitHub: ${e.message}")
            AppUpdateInfo(false, currentVersion, currentVersion, checkFailed = true)
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        if (latest.isBlank() || current.isBlank()) return false
        if (latest.equals(current, ignoreCase = true)) return false

        try {
            val latestParts = latest.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }
            val currentParts = current.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }

            if (latestParts.isEmpty() || currentParts.isEmpty()) {
                return latest.compareTo(current, ignoreCase = true) > 0
            }

            val maxLength = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLength) {
                val latestNum = latestParts.getOrElse(i) { 0 }
                val currentNum = currentParts.getOrElse(i) { 0 }
                if (latestNum > currentNum) return true
                if (latestNum < currentNum) return false
            }
        } catch (e: Exception) {
            return latest.compareTo(current, ignoreCase = true) > 0
        }
        return false
    }
}
