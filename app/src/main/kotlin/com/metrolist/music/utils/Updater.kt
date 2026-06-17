/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import com.metrolist.music.BuildConfig
import com.metrolist.music.utils.DeviceInfo
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val description: String,
    val releaseDate: String,
    val assets: List<ReleaseAsset>
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
    val architecture: String,
    val variant: String // "foss" or "gms"
)

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set
    
    private var cachedReleaseInfo: ReleaseInfo? = null
    private var cachedAllReleases: List<ReleaseInfo> = emptyList()
    
    private const val CHECK_INTERVAL_MILLIS = 2 * 60 * 60 * 1000L // 2 hours
    private const val GITHUB_API_BASE = "https://api.github.com/repos/AOSP-Silicon/Musik"

    /**
     * Compares two version strings.
     * Handles both numeric segments and non-numeric suffixes (e.g., "1.0.1" vs "1.0.1-beta.1").
     * Returns: 1 if v1 > v2, -1 if v1 < v2, 0 if equal
     */
    fun compareVersions(v1: String, v2: String): Int {
        fun parseVersion(version: String): Pair<List<Int>, String> {
            val parts = version.split("-", limit = 2)
            val versionPart = parts[0].removePrefix("v")
            val numbers = versionPart.split(".").map { it.toIntOrNull() ?: 0 }
            val suffix = if (parts.size > 1) parts[1] else ""
            return numbers to suffix
        }

        val (v1Numbers, v1Suffix) = parseVersion(v1)
        val (v2Numbers, v2Suffix) = parseVersion(v2)

        // Compare number parts
        for (i in 0 until maxOf(v1Numbers.size, v2Numbers.size)) {
            val n1 = v1Numbers.getOrNull(i) ?: 0
            val n2 = v2Numbers.getOrNull(i) ?: 0
            if (n1 != n2) return if (n1 > n2) 1 else -1
        }

        // Numbers are equal, compare suffixes
        return when {
            v1Suffix.isEmpty() && v2Suffix.isEmpty() -> 0
            v1Suffix.isEmpty() -> -1  // No suffix < has suffix
            v2Suffix.isEmpty() -> 1   // Has suffix > no suffix
            else -> v1Suffix.compareTo(v2Suffix) // Both have suffixes, compare lexicographically
        }
    }

    /**
     * Checks if the latest version is newer than the current version.
     * Returns true if an update is available (latestVersion > currentVersion)
     */
    fun isUpdateAvailable(currentVersion: String, latestVersion: String): Boolean {
        return compareVersions(latestVersion, currentVersion) > 0
    }

    /**
     * Parse release assets from GitHub API response
     */
    private fun parseAssets(assetsArray: JSONArray): List<ReleaseAsset> {
        val assets = mutableListOf<ReleaseAsset>()
        
        for (i in 0 until assetsArray.length()) {
            val asset = assetsArray.getJSONObject(i)
            val name = asset.getString("name")
            
            // Skip non-APK files
            if (!name.endsWith(".apk")) continue
            
            val downloadUrl = asset.getString("browser_download_url")
            val size = asset.getLong("size")
            val lower = name.lowercase()

            val variant = when {
                lower.contains("-gms-") || lower.contains("gms") -> "gms"
                lower.contains("-foss-") || lower.contains("foss") -> "foss"
                else -> continue
            }

            val arch = when {
                listOf("arm64-v8a", "arm64", "aarch64").any { lower.contains(it) } -> "arm64-v8a"
                listOf("armeabi-v7a", "armv7", "aarch32", "arm").any { lower.contains(it) } -> "armeabi-v7a"
                lower.contains("universal") -> "universal"
                else -> "unknown"
            }

            assets.add(ReleaseAsset(name, downloadUrl, size, arch, variant))
        }
        
        return assets
    }

    /**
     * Fetch latest release from GitHub API
     */
    suspend fun getLatestRelease(forceRefresh: Boolean = false): Result<ReleaseInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (cachedReleaseInfo != null && !forceRefresh) {
                    return@runCatching cachedReleaseInfo!!
                }
                
                val response = client.get("$GITHUB_API_BASE/releases/latest").bodyAsText()
                val json = JSONObject(response)
                
                val releaseInfo = ReleaseInfo(
                    tagName = json.getString("tag_name"),
                    versionName = json.getString("name").ifEmpty { json.getString("tag_name") },
                    description = json.getString("body"),
                    releaseDate = json.getString("published_at"),
                    assets = parseAssets(json.getJSONArray("assets"))
                )
                
                cachedReleaseInfo = releaseInfo
                lastCheckTime = System.currentTimeMillis()
                releaseInfo
            }
        }

    /**
     * Fetch all releases from GitHub API (paginated)
     */
    suspend fun getAllReleases(forceRefresh: Boolean = false): Result<List<ReleaseInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (cachedAllReleases.isNotEmpty() && !forceRefresh) {
                    return@runCatching cachedAllReleases
                }
                
                val releases = mutableListOf<ReleaseInfo>()
                var page = 1
                var hasMore = true
                
                while (hasMore && page <= 10) { // Limit to 10 pages
                    val response = client.get("$GITHUB_API_BASE/releases?page=$page&per_page=30").bodyAsText()
                    val json = JSONArray(response)
                    
                    if (json.length() == 0) {
                        hasMore = false
                        break
                    }
                    
                    for (i in 0 until json.length()) {
                        val releaseObj = json.getJSONObject(i)
                        releases.add(ReleaseInfo(
                            tagName = releaseObj.getString("tag_name"),
                            versionName = releaseObj.getString("name").ifEmpty { releaseObj.getString("tag_name") },
                            description = releaseObj.getString("body"),
                            releaseDate = releaseObj.getString("published_at"),
                            assets = parseAssets(releaseObj.getJSONArray("assets"))
                        ))
                    }
                    page++
                }
                
                cachedAllReleases = releases
                releases
            }
        }

    /**
     * Get the download URL for the correct app variant
     */
    fun getDownloadUrlForCurrentVariant(releaseInfo: ReleaseInfo): String? {
        val currentVariant = if (BuildConfig.CAST_AVAILABLE) "gms" else "foss"
        val currentArch = DeviceInfo.ARCHITECTURE
        
        // Exact match for ABI and Variant
        var match = releaseInfo.assets.find { it.architecture == currentArch && it.variant == currentVariant }
        
        // Universal with same variant
        if (match == null) {
            match = releaseInfo.assets.find { it.architecture == "universal" && it.variant == currentVariant }
        }
        
        // Any asset with same variant
        if (match == null) {
            match = releaseInfo.assets.find { it.variant == currentVariant }
        }
        
        return match?.downloadUrl
    }

    /**
     * Check if update is needed
     */
    suspend fun checkForUpdate(forceRefresh: Boolean = false, includePreRelease: Boolean = false): Result<Pair<ReleaseInfo?, Boolean>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val shouldFetch = forceRefresh || (System.currentTimeMillis() - lastCheckTime) > CHECK_INTERVAL_MILLIS
                
                if (includePreRelease) {
                    val result = getAllReleases(forceRefresh = shouldFetch)
                    if (result.isSuccess) {
                        val releases = result.getOrThrow()
                        val latestRelease = releases.maxByOrNull { it.releaseDate }
                        if (latestRelease != null) {
                            cachedReleaseInfo = latestRelease
                            val hasUpdate = isUpdateAvailable(BuildConfig.VERSION_NAME, latestRelease.versionName)
                            Pair(latestRelease, hasUpdate)
                        } else throw Exception("No releases found")
                    } else throw result.exceptionOrNull() ?: Exception("Unknown error")
                } else {
                    val result = getLatestRelease(forceRefresh = shouldFetch)
                    if (result.isSuccess) {
                        val releaseInfo = result.getOrThrow()
                        cachedReleaseInfo = releaseInfo
                        val hasUpdate = isUpdateAvailable(BuildConfig.VERSION_NAME, releaseInfo.versionName)
                        Pair(releaseInfo, hasUpdate)
                    } else throw result.exceptionOrNull() ?: Exception("Unknown error")
                }
            }
        }

    fun getCachedLatestRelease(): ReleaseInfo? = cachedReleaseInfo
}
