package com.picstar.app.data

import android.content.Context
import com.picstar.app.model.ThemeMode
import com.picstar.app.model.UserSettings
import org.json.JSONArray

/**
 * Persistence manager for the authenticated Pixiv session, user preferences, and search history.
 * Contains NO mock-up data; all content in the app is populated exclusively from the Pixiv App-API.
 */
object PicStarRepository {

    private const val PREFS_NAME = "picstar_session_prefs"

    fun loadUserSettings(context: Context): UserSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val refreshToken = prefs.getString("refresh_token", "") ?: ""
        val accessToken = prefs.getString("access_token", "") ?: ""
        val userId = prefs.getString("user_id", "") ?: ""
        val displayName = prefs.getString("display_name", "") ?: ""
        val username = prefs.getString("username", "") ?: ""
        val profileImg = prefs.getString("profile_image_url", "") ?: ""
        val bio = prefs.getString("bio", "") ?: ""
        val themeStr = prefs.getString("theme_mode", ThemeMode.MIDNIGHT.name) ?: ThemeMode.MIDNIGHT.name
        val theme = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.MIDNIGHT)

        return UserSettings(
            userId = userId,
            displayName = displayName,
            username = username,
            profileImageUrl = profileImg,
            bio = bio,
            showMatureContent = prefs.getBoolean("show_mature", false),
            includeAiWork = prefs.getBoolean("include_ai", true),
            autoplayCarousel = prefs.getBoolean("autoplay_carousel", true),
            highResPreviews = prefs.getBoolean("high_res_previews", false),
            imageQuality = prefs.getString("image_quality", "Original quality") ?: "Original quality",
            fileNamingPattern = prefs.getString("file_naming", "Artist — Title") ?: "Artist — Title",
            imageHostProxy = prefs.getString("image_host_proxy", "i.pximg.net (Direct + Referer)")
                ?: "i.pximg.net (Direct + Referer)",
            downloadFolderUri = prefs.getString("download_folder_uri", "") ?: "",
            downloadFolderLabel = prefs.getString("download_folder_label", "") ?: "",
            themeMode = theme,
            pixivRefreshToken = refreshToken,
            pixivAccessToken = accessToken,
            isPixivConnected = accessToken.isNotBlank() || refreshToken.isNotBlank()
        )
    }

    fun saveUserSettings(context: Context, settings: UserSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("user_id", settings.userId)
            .putString("display_name", settings.displayName)
            .putString("username", settings.username)
            .putString("profile_image_url", settings.profileImageUrl)
            .putString("bio", settings.bio)
            .putBoolean("show_mature", settings.showMatureContent)
            .putBoolean("include_ai", settings.includeAiWork)
            .putBoolean("autoplay_carousel", settings.autoplayCarousel)
            .putBoolean("high_res_previews", settings.highResPreviews)
            .putString("image_quality", settings.imageQuality)
            .putString("file_naming", settings.fileNamingPattern)
            .putString("image_host_proxy", settings.imageHostProxy)
            .putString("download_folder_uri", settings.downloadFolderUri)
            .putString("download_folder_label", settings.downloadFolderLabel)
            .putString("theme_mode", settings.themeMode.name)
            .putString("refresh_token", settings.pixivRefreshToken)
            .putString("access_token", settings.pixivAccessToken)
            .apply()
    }

    fun clearAuthSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("user_id")
            .remove("display_name")
            .remove("username")
            .remove("profile_image_url")
            .remove("refresh_token")
            .remove("access_token")
            .apply()
    }

    fun loadRecentSearches(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("recent_searches_json", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
        }.getOrDefault(emptyList())
    }

    fun saveRecentSearches(context: Context, searches: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        searches.take(15).forEach { arr.put(it) }
        prefs.edit().putString("recent_searches_json", arr.toString()).apply()
    }
}
