package com.picstar.app.model

enum class AppPage(val label: String, val iconSymbol: String) {
    HOME("Home", "⌂"),
    POPULAR("Popular", "↗"),
    FAVORITES("Favorites", "♡"),
    SETTINGS("Settings", "⚙")
}

enum class HomeTab(val label: String) {
    FOR_YOU("For you"),
    FOLLOWING("Following")
}

enum class FollowingSortMode(val label: String) {
    LATEST("Latest"),
    BY_ARTISTS("By followed artists")
}

enum class RankingMode(
    val displayName: String,
    val pixivApiMode: String,
    val description: String,
    val isAiCategory: Boolean = false,
    val isR18Category: Boolean = false
) {
    DAILY("Daily", "day", "The illustrations capturing imaginations today."),
    FOR_MALE("For Male", "day_male", "The current ranking curated for male audiences."),
    FOR_FEMALE("For Female", "day_female", "The current ranking curated for female audiences."),
    ORIGINAL("Original", "week_original", "Original worlds and characters, ranked by readers."),
    ROOKIE("Rookie", "week_rookie", "Bright new voices finding their first audience."),
    WEEKLY("Weekly", "week", "The week’s most resonant visual stories."),
    MONTHLY("Monthly", "month", "A month of art worth remembering."),
    AI("AI", "day_ai", "Popular AI-assisted illustration, clearly labeled.", isAiCategory = true),
    XVIII_AI("XVIII AI", "day_r18_ai", "Top mature AI-assisted illustrations today.", isAiCategory = true, isR18Category = true),
    XVIII("XVIII", "day_r18", "The daily mature ranking across Pixiv.", isR18Category = true),
    XVIII_WEEKLY("XVIII weekly", "week_r18", "The week’s most bookmarked mature illustrations.", isR18Category = true),
    XVIII_G("XVIII G", "week_r18g", "Weekly extreme / grotesque mature ranking.", isR18Category = true)
}

enum class SettingsSection(val label: String) {
    PROFILE("Profile"),
    PREFERENCES("Preferences"),
    DOWNLOADS("Download settings"),
    THEME("Theme"),
    ABOUT("About")
}

enum class ThemeMode(val label: String, val subtitle: String) {
    MIDNIGHT("Midnight", "Deep and focused"),
    DAYLIGHT("Daylight", "Bright and airy"),
    SYSTEM("System", "Match your device")
}

enum class ArtistProfileTab(val label: String) {
    WORKS("Works"),
    LIKES("Likes"),
    FOLLOWING("Following"),
    MY_PIXIV("MyPixiv"),
    FOLLOWERS("Followers")
}

enum class SearchSortOption(val label: String, val apiSort: String) {
    NEWEST("Newest", "date_desc"),
    POPULAR("Popular", "popular_desc"),
    POPULAR_MALE("Popular · Male", "popular_male_desc"),
    POPULAR_FEMALE("Popular · Female", "popular_female_desc"),
    OLDEST("Oldest", "date_asc")
}

enum class SearchWorkType(val label: String) {
    ALL_ART("Illustrations, Manga & Ugoira"),
    ILLUST_AND_UGOIRA("Illustrations & Ugoira"),
    ILLUST_ONLY("Illustrations only"),
    MANGA_ONLY("Manga only"),
    UGOIRA_ONLY("Ugoira only"),
    NOVELS("Novels")
}

enum class SearchDateRange(val label: String, val daysBack: Int?) {
    ANY_TIME("Any time", null),
    PAST_24_HOURS("Past 24 hours", 1),
    PAST_WEEK("Past week", 7),
    PAST_MONTH("Past month", 30),
    PAST_6_MONTHS("Past 6 months", 180),
    PAST_YEAR("Past year", 365)
}

enum class SearchLikeFilter(val label: String, val minLikes: Int) {
    ANY("Any likes", 0),
    MIN_100("100+ likes", 100),
    MIN_500("500+ likes", 500),
    MIN_1000("1,000+ likes", 1000),
    MIN_5000("5,000+ likes", 5000),
    MIN_10000("10,000+ likes", 10000)
}

data class SearchFilters(
    val sortOption: SearchSortOption = SearchSortOption.NEWEST,
    val workType: SearchWorkType = SearchWorkType.ALL_ART,
    val dateRange: SearchDateRange = SearchDateRange.ANY_TIME,
    val likeFilter: SearchLikeFilter = SearchLikeFilter.ANY,
    val showAiGenerated: Boolean = true,
    val showMatureContent: Boolean = false
)

sealed class DetailRoute {
    data class Illust(val item: IllustItem) : DetailRoute()
    data class Artist(val artist: ArtistCard) : DetailRoute()
}

data class IllustTag(
    val name: String,
    val translatedName: String? = null
)

data class IllustComment(
    val id: Long,
    val userId: Long,
    val userName: String,
    val userAccount: String,
    val userAvatarUrl: String?,
    val comment: String,
    val dateFormatted: String
)

data class UgoiraFrame(
    val file: String,
    val delayMs: Int
)

data class UgoiraMetadata(
    val zipUrl: String,
    val frames: List<UgoiraFrame>
)

data class IllustItem(
    val id: Long,
    val title: String,
    val type: String = "illust", // illust, manga, ugoira, novel
    val artistId: Long,
    val artistName: String,
    val artistHandle: String,
    val artistAvatarUrl: String? = null,
    val isArtistFollowed: Boolean = false,
    val likesFormatted: String,
    val bookmarkCount: Int,
    val totalViewCount: Int = 0,
    val aspectRatio: Float = 0.75f,
    val tags: List<IllustTag> = emptyList(),
    val isBookmarked: Boolean = false,
    val isAiGenerated: Boolean = false,
    val isR18: Boolean = false,
    val isR18G: Boolean = false,
    val rankNumber: Int? = null,
    val imageUrlSquareMedium: String? = null,
    val imageUrlMedium: String? = null,
    val imageUrlLarge: String? = null,
    val imageUrlOriginal: String? = null,
    val metaPageUrls: List<String> = emptyList(),
    val caption: String = "",
    val createDateFormatted: String = "",
    val pageCount: Int = 1,
    val seriesId: Long? = null,
    val seriesTitle: String? = null
)

enum class ArtistWorkFilter(val label: String, val apiType: String?) {
    ALL("All", null),
    ILLUSTRATION("Illustrations", "illust"),
    MANGA("Manga", "manga"),
    MANGA_SERIES("Manga Series", "manga")
}

data class HighlightCard(
    val id: Long,
    val badge: String,
    val kicker: String,
    val title: String,
    val authorName: String,
    val authorInitials: String,
    val authorMeta: String,
    val imageUrl: String? = null,
    val articleUrl: String? = null
)

data class ArtistCard(
    val id: Long,
    val name: String,
    val handle: String,
    val avatarUrl: String? = null,
    val isFollowed: Boolean = false,
    val followersFormatted: String = "",
    val latestPostDate: String = "",
    val bio: String = "",
    val webpage: String = "",
    val twitterUrl: String = "",
    val twitterAccount: String = "",
    val totalMyPixivUsers: Int = 0,
    val totalFollowUsers: Int = 0,
    val recentIllusts: List<IllustItem> = emptyList()
)

enum class SearchResultKind {
    TAG, ILLUSTRATION, ARTIST
}

data class SearchQuickResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: SearchResultKind,
    val imageUrl: String? = null,
    val illust: IllustItem? = null,
    val artist: ArtistCard? = null
)

data class UserSettings(
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val profileImageUrl: String = "",
    val bio: String = "",
    val showMatureContent: Boolean = false,
    val includeAiWork: Boolean = true,
    val autoplayCarousel: Boolean = true,
    val highResPreviews: Boolean = false,
    val imageQuality: String = "Original quality",
    val fileNamingPattern: String = "Artist — Title",
    val imageHostProxy: String = "i.pximg.net (Direct + Referer)",
    val downloadFolderUri: String = "",
    val downloadFolderLabel: String = "",
    val themeMode: ThemeMode = ThemeMode.MIDNIGHT,
    val pixivRefreshToken: String = "",
    val pixivAccessToken: String = "",
    val isPixivConnected: Boolean = false
)
