package com.picstar.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.picstar.app.api.PixEzApiClient
import com.picstar.app.model.AppPage
import com.picstar.app.model.ArtistCard
import com.picstar.app.model.ArtistProfileTab
import com.picstar.app.model.ArtistWorkFilter
import com.picstar.app.model.DetailRoute
import com.picstar.app.model.FollowingSortMode
import com.picstar.app.model.HighlightCard
import com.picstar.app.model.HomeTab
import com.picstar.app.model.IllustComment
import com.picstar.app.model.IllustItem
import com.picstar.app.model.RankingMode
import com.picstar.app.model.SearchFilters
import com.picstar.app.model.SearchQuickResult
import com.picstar.app.model.SearchResultKind
import com.picstar.app.model.SearchSortOption
import com.picstar.app.model.SearchWorkType
import com.picstar.app.model.SettingsSection
import com.picstar.app.model.ThemeMode
import com.picstar.app.model.UgoiraMetadata
import com.picstar.app.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PicStarViewModel : ViewModel() {

    private var appContext: Context? = null

    // Navigation state
    var currentPage by mutableStateOf(AppPage.HOME)
        private set

    var currentHomeTab by mutableStateOf(HomeTab.FOR_YOU)
        private set

    var followingSortMode by mutableStateOf(FollowingSortMode.LATEST)
        private set

    var selectedRankingMode by mutableStateOf(RankingMode.DAILY)
        private set

    var selectedFavoriteTag by mutableStateOf("All")
        private set

    var selectedSettingsSection by mutableStateOf(SettingsSection.PROFILE)
        private set

    // Back-stack for full-screen Post Detail & Creator Profile navigation
    val detailStack = mutableStateListOf<DetailRoute>()
    val currentDetailRoute: DetailRoute?
        get() = detailStack.lastOrNull()

    // Live API Data Collections (NO mock data)
    val highlights = mutableStateListOf<HighlightCard>()
    var isLoadingHighlights by mutableStateOf(false)
        private set

    val artists = mutableStateListOf<ArtistCard>()
    var artistsNextUrl by mutableStateOf<String?>(null)
    var isLoadingArtists by mutableStateOf(false)
        private set
    var showAllArtistsModal by mutableStateOf(false)

    // KEY FEATURE: "Other artists to follow" state triggered when user follows an artist
    var recentlyFollowedSeedArtist by mutableStateOf<ArtistCard?>(null)
        private set
    val relatedArtistsToFollow = mutableStateListOf<ArtistCard>()
    var isLoadingRelatedArtists by mutableStateOf(false)
        private set

    // Recommended Illustrations (with infinite scrolling via next_url)
    val recommendedIllusts = mutableStateListOf<IllustItem>()
    var recommendedNextUrl by mutableStateOf<String?>(null)
    var isLoadingRecommended by mutableStateOf(false)
        private set
    var isLoadingMoreRecommended by mutableStateOf(false)
        private set

    // Following tab data (Latest & By followed artists)
    val followingLatestIllusts = mutableStateListOf<IllustItem>()
    var followingLatestNextUrl by mutableStateOf<String?>(null)
    val followedArtistsStream = mutableStateListOf<ArtistCard>()
    var isLoadingFollowing by mutableStateOf(false)
        private set

    // Popular / Ranking illustrations
    val rankingIllusts = mutableStateListOf<IllustItem>()
    var rankingNextUrl by mutableStateOf<String?>(null)
    var isLoadingRanking by mutableStateOf(false)
        private set

    // Favorites / Bookmarked illustrations
    val favoriteIllusts = mutableStateListOf<IllustItem>()
    var favoritesNextUrl by mutableStateOf<String?>(null)
    var isLoadingFavorites by mutableStateOf(false)
        private set

    // Post Detail State (Comments + Related Recommended Posts + Ugoira + Bookmark Users)
    val illustComments = mutableStateListOf<IllustComment>()
    var illustCommentsNextUrl by mutableStateOf<String?>(null)
    var isLoadingComments by mutableStateOf(false)
        private set

    val relatedIllusts = mutableStateListOf<IllustItem>()
    var relatedIllustsNextUrl by mutableStateOf<String?>(null)
    var isLoadingRelatedIllusts by mutableStateOf(false)
        private set

    var ugoiraMetadata by mutableStateOf<UgoiraMetadata?>(null)
        private set
    val ugoiraFramesMap = mutableStateMapOf<String, ImageBitmap>()
    var isLoadingUgoira by mutableStateOf(false)
        private set

    var isBookmarkUsersModalOpen by mutableStateOf(false)
    val bookmarkUsersList = mutableStateListOf<ArtistCard>()
    var isLoadingBookmarkUsers by mutableStateOf(false)
        private set

    // Creator Profile State (Works + Likes + Following + MyPixiv + Followers)
    var activeArtistProfile by mutableStateOf<ArtistCard?>(null)
        private set
    var artistProfileTab by mutableStateOf(ArtistProfileTab.WORKS)
        private set
    val artistFullWorks = mutableStateListOf<IllustItem>()
    val artistLikedIllusts = mutableStateListOf<IllustItem>()
    var artistLikedNextUrl by mutableStateOf<String?>(null)
    val artistFollowingUsers = mutableStateListOf<ArtistCard>()
    var artistFollowingNextUrl by mutableStateOf<String?>(null)
    val artistMyPixivUsers = mutableStateListOf<ArtistCard>()
    val artistFollowersUsers = mutableStateListOf<ArtistCard>()
    var isLoadingArtistProfile by mutableStateOf(false)
        private set
    var isLoadingArtistTab by mutableStateOf(false)
        private set
    var artistWorkFilter by mutableStateOf(ArtistWorkFilter.ALL)
        private set

    // Fullscreen image viewer (item + pageIndex)
    var fullscreenViewerState by mutableStateOf<Pair<IllustItem, Int>?>(null)
        private set

    // Floating Search & Search Results Filters state
    var isSearchOverlayOpen by mutableStateOf(false)
    var isSearchFiltersModalOpen by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
        private set
    val recentSearches = mutableStateListOf<String>()
    val recommendedTags = mutableStateListOf<String>()
    val quickSearchResults = mutableStateListOf<SearchQuickResult>()
    var isSearchingQuickResults by mutableStateOf(false)
        private set

    var activeSearchFilterQuery by mutableStateOf<String?>(null)
    var searchFilters by mutableStateOf(SearchFilters())
        private set
    val searchResultsIllusts = mutableStateListOf<IllustItem>()
    var searchResultsNextUrl by mutableStateOf<String?>(null)
    var isLoadingSearchResults by mutableStateOf(false)
        private set
    private var searchDebounceJob: Job? = null

    // User Settings & Pixiv OAuth2 PKCE Login state
    var userSettings by mutableStateOf(UserSettings())
        private set
    var isAuthenticating by mutableStateOf(false)
        private set
    var authErrorMessage by mutableStateOf<String?>(null)
        private set

    var pkceSession by mutableStateOf<PixEzApiClient.PkceSession?>(null)
        private set
    var isOAuthWebViewOpen by mutableStateOf(false)
    var isOAuthCreateAccountMode by mutableStateOf(false)

    var toastMessage by mutableStateOf<String?>(null)
        private set

    fun initializeSession(context: Context) {
        if (appContext != null) return
        val appCtx = context.applicationContext
        appContext = appCtx
        userSettings = PicStarRepository.loadUserSettings(appCtx)
        recentSearches.clear()
        recentSearches.addAll(PicStarRepository.loadRecentSearches(appCtx))

        if (userSettings.pixivRefreshToken.isNotBlank()) {
            refreshSessionAndLoadFeeds(userSettings.pixivRefreshToken)
        } else if (userSettings.pixivAccessToken.isNotBlank()) {
            loadAllAuthenticatedFeeds()
        }
    }

    private fun persistSettings() {
        appContext?.let { PicStarRepository.saveUserSettings(it, userSettings) }
    }

    private fun persistRecentSearches() {
        appContext?.let { PicStarRepository.saveRecentSearches(it, recentSearches) }
    }

    fun selectPage(page: AppPage) {
        currentPage = page
        if (page == AppPage.POPULAR && rankingIllusts.isEmpty() && !isLoadingRanking) {
            refreshRankingList(selectedRankingMode)
        } else if (page == AppPage.FAVORITES && favoriteIllusts.isEmpty() && !isLoadingFavorites) {
            loadUserBookmarks()
        }
    }

    fun selectHomeTab(tab: HomeTab) {
        currentHomeTab = tab
        if (tab == HomeTab.FOLLOWING && followingLatestIllusts.isEmpty() && followedArtistsStream.isEmpty()) {
            loadFollowingFeeds()
        }
    }

    fun selectFollowingSortMode(mode: FollowingSortMode) {
        followingSortMode = mode
        if (mode == FollowingSortMode.BY_ARTISTS && followedArtistsStream.isEmpty()) {
            loadFollowingFeeds()
        }
    }

    fun selectRankingMode(mode: RankingMode) {
        selectedRankingMode = mode
        refreshRankingList(mode)
    }

    fun selectFavoriteTag(tag: String) {
        selectedFavoriteTag = tag
    }

    fun selectSettingsSection(section: SettingsSection) {
        selectedSettingsSection = section
    }

    // Detail Stack Navigation (Post <-> Creator Profile)
    fun openIllustDetail(item: IllustItem) {
        detailStack.add(DetailRoute.Illust(item))
        loadIllustDetailExtras(item)
    }

    fun openArtistProfile(artist: ArtistCard) {
        detailStack.add(DetailRoute.Artist(artist))
        loadArtistProfileData(artist)
    }

    fun popDetail() {
        if (detailStack.isNotEmpty()) {
            detailStack.removeAt(detailStack.lastIndex)
            when (val top = detailStack.lastOrNull()) {
                is DetailRoute.Illust -> loadIllustDetailExtras(top.item)
                is DetailRoute.Artist -> loadArtistProfileData(top.artist)
                null -> Unit
            }
        }
    }

    private fun loadIllustDetailExtras(item: IllustItem) {
        val token = userSettings.pixivAccessToken
        if (token.isBlank()) return

        illustComments.clear()
        illustCommentsNextUrl = null
        isLoadingComments = true

        relatedIllusts.clear()
        relatedIllustsNextUrl = null
        isLoadingRelatedIllusts = true

        ugoiraMetadata = null
        ugoiraFramesMap.clear()
        isLoadingUgoira = item.type == "ugoira"

        viewModelScope.launch {
            PixEzApiClient.fetchIllustComments(token, item.id)
                .onSuccess { (comments, next) ->
                    illustComments.clear()
                    illustComments.addAll(comments)
                    illustCommentsNextUrl = next
                }
            isLoadingComments = false
        }

        viewModelScope.launch {
            PixEzApiClient.fetchRelatedIllusts(token, item.id)
                .onSuccess { (related, next) ->
                    relatedIllusts.clear()
                    relatedIllusts.addAll(related)
                    relatedIllustsNextUrl = next
                }
            isLoadingRelatedIllusts = false
        }

        if (item.type == "ugoira") {
            viewModelScope.launch {
                PixEzApiClient.fetchUgoiraMetadata(token, item.id)
                    .onSuccess { meta ->
                        ugoiraMetadata = meta
                        val frames = PixEzApiClient.downloadAndDecodeUgoiraFrames(
                            meta.zipUrl,
                            userSettings.imageHostProxy
                        )
                        ugoiraFramesMap.clear()
                        ugoiraFramesMap.putAll(frames)
                    }
                isLoadingUgoira = false
            }
        }
    }

    fun openBookmarkUsersForIllust(illustId: Long) {
        val token = userSettings.pixivAccessToken
        if (token.isBlank()) return
        isBookmarkUsersModalOpen = true
        bookmarkUsersList.clear()
        isLoadingBookmarkUsers = true
        viewModelScope.launch {
            PixEzApiClient.fetchBookmarkUsers(token, illustId)
                .onSuccess { (users, _) ->
                    bookmarkUsersList.clear()
                    bookmarkUsersList.addAll(users)
                }
            isLoadingBookmarkUsers = false
        }
    }

    fun loadMoreRelatedIllusts() {
        val token = userSettings.pixivAccessToken
        val next = relatedIllustsNextUrl
        val currentIllust = (currentDetailRoute as? DetailRoute.Illust)?.item ?: return
        if (token.isBlank() || next.isNullOrBlank() || isLoadingRelatedIllusts) return

        isLoadingRelatedIllusts = true
        viewModelScope.launch {
            PixEzApiClient.fetchRelatedIllusts(token, currentIllust.id, next)
                .onSuccess { (more, nextUrl) ->
                    val existing = relatedIllusts.map { it.id }.toSet()
                    relatedIllusts.addAll(more.filter { it.id !in existing })
                    relatedIllustsNextUrl = nextUrl
                }
            isLoadingRelatedIllusts = false
        }
    }

    fun loadMoreIllustComments() {
        val token = userSettings.pixivAccessToken
        val next = illustCommentsNextUrl
        val currentIllust = (currentDetailRoute as? DetailRoute.Illust)?.item ?: return
        if (token.isBlank() || next.isNullOrBlank() || isLoadingComments) return

        isLoadingComments = true
        viewModelScope.launch {
            PixEzApiClient.fetchIllustComments(token, currentIllust.id, next)
                .onSuccess { (more, nextUrl) ->
                    illustComments.addAll(more)
                    illustCommentsNextUrl = nextUrl
                }
            isLoadingComments = false
        }
    }

    private fun loadArtistProfileData(artist: ArtistCard) {
        activeArtistProfile = artist
        artistProfileTab = ArtistProfileTab.WORKS
        artistWorkFilter = ArtistWorkFilter.ALL
        artistFullWorks.clear()
        artistFullWorks.addAll(artist.recentIllusts)
        artistLikedIllusts.clear()
        artistLikedNextUrl = null
        artistFollowingUsers.clear()
        artistFollowingNextUrl = null
        artistMyPixivUsers.clear()
        artistFollowersUsers.clear()

        val token = userSettings.pixivAccessToken
        if (token.isBlank()) return

        isLoadingArtistProfile = true
        viewModelScope.launch {
            PixEzApiClient.fetchArtistDetail(token, artist.id)
                .onSuccess { detail ->
                    activeArtistProfile = detail.copy(recentIllusts = artistFullWorks.take(5))
                }
            PixEzApiClient.fetchArtistIllusts(token, artist.id, artistWorkFilter.apiType)
                .onSuccess { works ->
                    artistFullWorks.clear()
                    artistFullWorks.addAll(works)
                    activeArtistProfile = activeArtistProfile?.copy(recentIllusts = works.take(5))
                }
            isLoadingArtistProfile = false
        }
    }

    fun selectArtistWorkFilter(filter: ArtistWorkFilter) {
        artistWorkFilter = filter
        val artistId = activeArtistProfile?.id ?: return
        val token = userSettings.pixivAccessToken
        if (token.isBlank()) return

        isLoadingArtistProfile = true
        viewModelScope.launch {
            PixEzApiClient.fetchArtistIllusts(token, artistId, filter.apiType)
                .onSuccess { works ->
                    val filtered = if (filter == ArtistWorkFilter.MANGA_SERIES) {
                        works.filter { it.seriesId != null || !it.seriesTitle.isNullOrBlank() }
                    } else {
                        works
                    }
                    artistFullWorks.clear()
                    artistFullWorks.addAll(filtered)
                }
                .onFailure {
                    showToast("Failed to load ${filter.label}: ${it.message}")
                }
            isLoadingArtistProfile = false
        }
    }

    fun openFullscreenViewer(item: IllustItem, pageIndex: Int = 0) {
        fullscreenViewerState = item to pageIndex
    }

    fun closeFullscreenViewer() {
        fullscreenViewerState = null
    }

    fun selectArtistProfileTab(tab: ArtistProfileTab) {
        artistProfileTab = tab
        val artistId = activeArtistProfile?.id ?: return
        val token = userSettings.pixivAccessToken
        if (token.isBlank()) return

        when (tab) {
            ArtistProfileTab.WORKS -> Unit
            ArtistProfileTab.LIKES -> {
                if (artistLikedIllusts.isEmpty() && !isLoadingArtistTab) {
                    isLoadingArtistTab = true
                    viewModelScope.launch {
                        PixEzApiClient.fetchUserBookmarks(token, artistId.toString())
                            .onSuccess { (likes, next) ->
                                artistLikedIllusts.clear()
                                artistLikedIllusts.addAll(likes)
                                artistLikedNextUrl = next
                            }
                        isLoadingArtistTab = false
                    }
                }
            }
            ArtistProfileTab.FOLLOWING -> {
                if (artistFollowingUsers.isEmpty() && !isLoadingArtistTab) {
                    isLoadingArtistTab = true
                    viewModelScope.launch {
                        PixEzApiClient.fetchFollowedArtistsStream(token, artistId.toString())
                            .onSuccess { (users, next) ->
                                artistFollowingUsers.clear()
                                artistFollowingUsers.addAll(users)
                                artistFollowingNextUrl = next
                            }
                        isLoadingArtistTab = false
                    }
                }
            }
            ArtistProfileTab.MY_PIXIV -> {
                if (artistMyPixivUsers.isEmpty() && !isLoadingArtistTab) {
                    isLoadingArtistTab = true
                    viewModelScope.launch {
                        PixEzApiClient.fetchArtistMyPixiv(token, artistId)
                            .onSuccess { (users, _) ->
                                artistMyPixivUsers.clear()
                                artistMyPixivUsers.addAll(users)
                            }
                        isLoadingArtistTab = false
                    }
                }
            }
            ArtistProfileTab.FOLLOWERS -> {
                if (artistFollowersUsers.isEmpty() && !isLoadingArtistTab) {
                    isLoadingArtistTab = true
                    viewModelScope.launch {
                        PixEzApiClient.fetchArtistFollowers(token, artistId)
                            .onSuccess { (users, _) ->
                                artistFollowersUsers.clear()
                                artistFollowersUsers.addAll(users)
                            }
                        isLoadingArtistTab = false
                    }
                }
            }
        }
    }

    // OAuth2 PKCE WebView flow
    fun startPixivOAuthWebView(createAccount: Boolean = false) {
        authErrorMessage = null
        pkceSession = PixEzApiClient.createPkceLoginSession()
        isOAuthCreateAccountMode = createAccount
        isOAuthWebViewOpen = true
    }

    fun handlePixivOAuthCallbackCode(code: String) {
        val session = pkceSession ?: return
        isOAuthWebViewOpen = false
        isAuthenticating = true
        authErrorMessage = null

        viewModelScope.launch {
            PixEzApiClient.exchangeAuthorizationCode(code, session.codeVerifier)
                .onSuccess { tokenResult ->
                    isAuthenticating = false
                    applyConnectedPixivSession(tokenResult)
                }
                .onFailure { err ->
                    isAuthenticating = false
                    authErrorMessage = "OAuth login failed: ${err.message}"
                }
        }
    }

    fun connectWithRefreshToken(refreshToken: String) {
        val cleanToken = refreshToken.trim()
        if (cleanToken.isBlank()) {
            authErrorMessage = "Please enter your Pixiv refresh_token or sign in via WebView."
            return
        }
        isAuthenticating = true
        authErrorMessage = null

        viewModelScope.launch {
            PixEzApiClient.loginWithRefreshToken(cleanToken)
                .onSuccess { tokenResult ->
                    isAuthenticating = false
                    applyConnectedPixivSession(tokenResult)
                }
                .onFailure { err ->
                    isAuthenticating = false
                    authErrorMessage = "Authentication failed: ${err.message}"
                }
        }
    }

    private fun refreshSessionAndLoadFeeds(refreshToken: String) {
        isAuthenticating = true
        viewModelScope.launch {
            PixEzApiClient.loginWithRefreshToken(refreshToken)
                .onSuccess { tokenResult ->
                    isAuthenticating = false
                    applyConnectedPixivSession(tokenResult)
                }
                .onFailure {
                    isAuthenticating = false
                    if (userSettings.pixivAccessToken.isNotBlank()) {
                        loadAllAuthenticatedFeeds()
                    } else {
                        userSettings = userSettings.copy(isPixivConnected = false)
                    }
                }
        }
    }

    private fun applyConnectedPixivSession(tokenResult: PixEzApiClient.TokenResult) {
        userSettings = userSettings.copy(
            userId = tokenResult.userId,
            displayName = tokenResult.userName,
            username = tokenResult.userAccount,
            profileImageUrl = tokenResult.profileImageUrl,
            pixivAccessToken = tokenResult.accessToken,
            pixivRefreshToken = tokenResult.refreshToken.ifBlank { userSettings.pixivRefreshToken },
            isPixivConnected = true
        )
        persistSettings()
        showToast("Welcome, ${tokenResult.userName} ✦")
        loadAllAuthenticatedFeeds()
    }

    fun logoutPixivAccount() {
        appContext?.let { PicStarRepository.clearAuthSession(it) }
        userSettings = userSettings.copy(
            userId = "",
            displayName = "",
            username = "",
            profileImageUrl = "",
            pixivAccessToken = "",
            pixivRefreshToken = "",
            isPixivConnected = false
        )
        detailStack.clear()
        highlights.clear()
        artists.clear()
        recommendedIllusts.clear()
        followingLatestIllusts.clear()
        followedArtistsStream.clear()
        rankingIllusts.clear()
        favoriteIllusts.clear()
        relatedArtistsToFollow.clear()
        recentlyFollowedSeedArtist = null
        currentPage = AppPage.HOME
    }

    fun loadAllAuthenticatedFeeds() {
        val token = userSettings.pixivAccessToken
        if (token.isBlank()) return

        isLoadingHighlights = true
        viewModelScope.launch {
            PixEzApiClient.fetchSpotlightHighlights(token)
                .onSuccess { list ->
                    highlights.clear()
                    highlights.addAll(list)
                }
            isLoadingHighlights = false
        }

        isLoadingArtists = true
        viewModelScope.launch {
            PixEzApiClient.fetchRecommendedArtists(token)
                .onSuccess { (list, next) ->
                    artists.clear()
                    artists.addAll(list)
                    artistsNextUrl = next
                    enrichArtistsWithFiveIllusts(token, list.take(10))
                }
            isLoadingArtists = false
        }

        isLoadingRecommended = true
        viewModelScope.launch {
            PixEzApiClient.fetchRecommendedIllusts(token)
                .onSuccess { (list, next) ->
                    recommendedIllusts.clear()
                    recommendedIllusts.addAll(list)
                    recommendedNextUrl = next
                }
            isLoadingRecommended = false
        }

        viewModelScope.launch {
            PixEzApiClient.fetchTrendingTags(token)
                .onSuccess { tags ->
                    recommendedTags.clear()
                    recommendedTags.addAll(tags.take(16))
                }
        }

        loadFollowingFeeds()
        refreshRankingList(selectedRankingMode)
        loadUserBookmarks()
    }

    private fun enrichArtistsWithFiveIllusts(token: String, targetArtists: List<ArtistCard>) {
        viewModelScope.launch {
            for (artist in targetArtists) {
                if (artist.recentIllusts.size < 5) {
                    PixEzApiClient.fetchArtistIllusts(token, artist.id)
                        .onSuccess { works ->
                            if (works.isNotEmpty()) {
                                val idx = artists.indexOfFirst { it.id == artist.id }
                                if (idx != -1) {
                                    artists[idx] = artists[idx].copy(recentIllusts = works.take(5))
                                }
                            }
                        }
                }
            }
        }
    }

    fun loadFollowingFeeds() {
        val token = userSettings.pixivAccessToken
        val uid = userSettings.userId
        if (token.isBlank()) return

        isLoadingFollowing = true
        viewModelScope.launch {
            PixEzApiClient.fetchFollowingLatestIllusts(token)
                .onSuccess { (latest, next) ->
                    followingLatestIllusts.clear()
                    followingLatestIllusts.addAll(latest)
                    followingLatestNextUrl = next
                }
            if (uid.isNotBlank()) {
                PixEzApiClient.fetchFollowedArtistsStream(token, uid)
                    .onSuccess { (artistList, _) ->
                        followedArtistsStream.clear()
                        followedArtistsStream.addAll(artistList)
                    }
            }
            isLoadingFollowing = false
        }
    }

    fun loadUserBookmarks() {
        val token = userSettings.pixivAccessToken
        val uid = userSettings.userId
        if (token.isBlank() || uid.isBlank()) return

        isLoadingFavorites = true
        viewModelScope.launch {
            PixEzApiClient.fetchUserBookmarks(token, uid)
                .onSuccess { (bookmarks, next) ->
                    favoriteIllusts.clear()
                    favoriteIllusts.addAll(bookmarks)
                    favoritesNextUrl = next
                }
            isLoadingFavorites = false
        }
    }

    fun loadMoreUserBookmarks() {
        val token = userSettings.pixivAccessToken
        val next = favoritesNextUrl
        if (token.isBlank() || next.isNullOrBlank() || isLoadingFavorites) return

        isLoadingFavorites = true
        viewModelScope.launch {
            PixEzApiClient.fetchUserBookmarks(token, userSettings.userId, next)
                .onSuccess { (more, nextUrl) ->
                    favoriteIllusts.addAll(more)
                    favoritesNextUrl = nextUrl
                }
            isLoadingFavorites = false
        }
    }

    fun loadMoreRecommendedIllusts() {
        val token = userSettings.pixivAccessToken
        val next = recommendedNextUrl
        if (token.isBlank() || next.isNullOrBlank() || isLoadingMoreRecommended) return

        isLoadingMoreRecommended = true
        viewModelScope.launch {
            PixEzApiClient.fetchRecommendedIllusts(token, next)
                .onSuccess { (newItems, nextUrl) ->
                    val existingIds = recommendedIllusts.map { it.id }.toSet()
                    recommendedIllusts.addAll(newItems.filter { it.id !in existingIds })
                    recommendedNextUrl = nextUrl
                }
            isLoadingMoreRecommended = false
        }
    }

    fun refreshRankingList(mode: RankingMode) {
        val token = userSettings.pixivAccessToken
        if (token.isBlank()) return

        isLoadingRanking = true
        rankingIllusts.clear()
        viewModelScope.launch {
            PixEzApiClient.fetchRankingIllusts(token, mode.pixivApiMode)
                .onSuccess { (ranked, next) ->
                    rankingIllusts.clear()
                    rankingIllusts.addAll(ranked)
                    rankingNextUrl = next
                }
                .onFailure { err ->
                    showToast("Could not load ${mode.displayName}: ${err.message?.take(35)}")
                }
            isLoadingRanking = false
        }
    }

    fun loadMoreRankingIllusts() {
        val token = userSettings.pixivAccessToken
        val next = rankingNextUrl
        if (token.isBlank() || next.isNullOrBlank() || isLoadingRanking) return

        isLoadingRanking = true
        viewModelScope.launch {
            PixEzApiClient.fetchRankingIllusts(token, selectedRankingMode.pixivApiMode, next)
                .onSuccess { (more, nextUrl) ->
                    val startRank = rankingIllusts.size
                    val numbered = more.mapIndexed { idx, item ->
                        item.copy(rankNumber = startRank + idx + 1)
                    }
                    rankingIllusts.addAll(numbered)
                    rankingNextUrl = nextUrl
                }
            isLoadingRanking = false
        }
    }

    fun toggleBookmark(illustId: Long) {
        val token = userSettings.pixivAccessToken
        if (token.isBlank()) return

        val topDetailIllust = (currentDetailRoute as? DetailRoute.Illust)?.item?.takeIf { it.id == illustId }
        val targetItem = recommendedIllusts.find { it.id == illustId }
            ?: rankingIllusts.find { it.id == illustId }
            ?: followingLatestIllusts.find { it.id == illustId }
            ?: favoriteIllusts.find { it.id == illustId }
            ?: searchResultsIllusts.find { it.id == illustId }
            ?: relatedIllusts.find { it.id == illustId }
            ?: artistFullWorks.find { it.id == illustId }
            ?: topDetailIllust

        val newBookmarked = !(targetItem?.isBookmarked ?: false)

        fun updateList(list: MutableList<IllustItem>) {
            for (i in list.indices) {
                if (list[i].id == illustId) {
                    list[i] = list[i].copy(isBookmarked = newBookmarked)
                }
            }
        }

        updateList(recommendedIllusts)
        updateList(rankingIllusts)
        updateList(followingLatestIllusts)
        updateList(searchResultsIllusts)
        updateList(relatedIllusts)
        updateList(artistFullWorks)
        updateList(artistLikedIllusts)

        for (i in detailStack.indices) {
            val route = detailStack[i]
            if (route is DetailRoute.Illust && route.item.id == illustId) {
                detailStack[i] = DetailRoute.Illust(route.item.copy(isBookmarked = newBookmarked))
            }
        }

        if (newBookmarked) {
            if (targetItem != null && favoriteIllusts.none { it.id == illustId }) {
                favoriteIllusts.add(0, targetItem.copy(isBookmarked = true))
            }
        } else {
            favoriteIllusts.removeAll { it.id == illustId }
        }

        viewModelScope.launch {
            PixEzApiClient.setBookmarkIllust(token, illustId, newBookmarked)
        }
    }

    fun toggleFollowArtist(artistId: Long, fallbackArtist: ArtistCard? = null) {
        val token = userSettings.pixivAccessToken
        if (token.isBlank()) return

        val existing = artists.find { it.id == artistId }
            ?: relatedArtistsToFollow.find { it.id == artistId }
            ?: followedArtistsStream.find { it.id == artistId }
            ?: artistFollowingUsers.find { it.id == artistId }
            ?: activeArtistProfile?.takeIf { it.id == artistId }
            ?: fallbackArtist

        val currentlyFollowed = existing?.isFollowed ?: false
        val nowFollowed = !currentlyFollowed

        val updatedArtist = (existing ?: ArtistCard(id = artistId, name = "Artist", handle = "@$artistId"))
            .copy(isFollowed = nowFollowed)

        val mainIdx = artists.indexOfFirst { it.id == artistId }
        if (mainIdx != -1) artists[mainIdx] = updatedArtist

        val relIdx = relatedArtistsToFollow.indexOfFirst { it.id == artistId }
        if (relIdx != -1) relatedArtistsToFollow[relIdx] = updatedArtist

        val folIdx = followedArtistsStream.indexOfFirst { it.id == artistId }
        if (folIdx != -1) {
            followedArtistsStream[folIdx] = updatedArtist
        } else if (nowFollowed) {
            followedArtistsStream.add(0, updatedArtist)
        }

        val artFolIdx = artistFollowingUsers.indexOfFirst { it.id == artistId }
        if (artFolIdx != -1) artistFollowingUsers[artFolIdx] = updatedArtist

        if (activeArtistProfile?.id == artistId) {
            activeArtistProfile = updatedArtist
        }

        for (i in detailStack.indices) {
            val r = detailStack[i]
            if (r is DetailRoute.Illust && r.item.artistId == artistId) {
                detailStack[i] = DetailRoute.Illust(r.item.copy(isArtistFollowed = nowFollowed))
            } else if (r is DetailRoute.Artist && r.artist.id == artistId) {
                detailStack[i] = DetailRoute.Artist(updatedArtist)
            }
        }

        if (nowFollowed) {
            recentlyFollowedSeedArtist = updatedArtist
            isLoadingRelatedArtists = true
            viewModelScope.launch {
                PixEzApiClient.setFollowArtist(token, artistId, true)
                PixEzApiClient.fetchRelatedArtistsOnFollow(token, artistId)
                    .onSuccess { relatedList ->
                        relatedArtistsToFollow.clear()
                        relatedArtistsToFollow.addAll(relatedList)
                        enrichRelatedArtistsWithFiveIllusts(token, relatedList.take(6))
                    }
                isLoadingRelatedArtists = false
            }
            showToast("Following ${updatedArtist.name} · Discovering similar artists ✦")
        } else {
            if (recentlyFollowedSeedArtist?.id == artistId) {
                recentlyFollowedSeedArtist = null
                relatedArtistsToFollow.clear()
            }
            viewModelScope.launch {
                PixEzApiClient.setFollowArtist(token, artistId, false)
            }
        }
    }

    private fun enrichRelatedArtistsWithFiveIllusts(token: String, list: List<ArtistCard>) {
        viewModelScope.launch {
            for (artist in list) {
                if (artist.recentIllusts.size < 5) {
                    PixEzApiClient.fetchArtistIllusts(token, artist.id)
                        .onSuccess { works ->
                            if (works.isNotEmpty()) {
                                val idx = relatedArtistsToFollow.indexOfFirst { it.id == artist.id }
                                if (idx != -1) {
                                    relatedArtistsToFollow[idx] = relatedArtistsToFollow[idx].copy(
                                        recentIllusts = works.take(5)
                                    )
                                }
                            }
                        }
                }
            }
        }
    }

    fun dismissRelatedArtistsBanner() {
        recentlyFollowedSeedArtist = null
        relatedArtistsToFollow.clear()
    }

    // Live Search & Search Filters
    fun updateSearchQuery(newQuery: String) {
        searchQuery = newQuery
        val trimmed = newQuery.trim()
        searchDebounceJob?.cancel()

        if (trimmed.length < 2) {
            quickSearchResults.clear()
            isSearchingQuickResults = false
            return
        }

        val token = userSettings.pixivAccessToken
        if (token.isBlank()) return

        isSearchingQuickResults = true
        searchDebounceJob = viewModelScope.launch {
            delay(280)
            val results = mutableListOf<SearchQuickResult>()

            PixEzApiClient.fetchAutocompleteSuggestions(token, trimmed)
                .onSuccess { tags ->
                    tags.take(4).forEach { tag ->
                        results.add(
                            SearchQuickResult(
                                id = "tag_$tag",
                                title = "#$tag",
                                subtitle = "Tag · Pixiv Autocomplete",
                                kind = SearchResultKind.TAG
                            )
                        )
                    }
                }

            PixEzApiClient.searchArtists(token, trimmed)
                .onSuccess { foundArtists ->
                    foundArtists.take(3).forEach { artist ->
                        results.add(
                            SearchQuickResult(
                                id = "artist_${artist.id}",
                                title = artist.name,
                                subtitle = "Artist · ${artist.handle}",
                                kind = SearchResultKind.ARTIST,
                                imageUrl = artist.avatarUrl,
                                artist = artist
                            )
                        )
                    }
                }

            PixEzApiClient.searchIllusts(token, trimmed, searchFilters)
                .onSuccess { (foundIllusts, _) ->
                    foundIllusts.take(4).forEach { illust ->
                        results.add(
                            SearchQuickResult(
                                id = "illust_${illust.id}",
                                title = illust.title,
                                subtitle = "by ${illust.artistName} · ♡ ${illust.likesFormatted}",
                                kind = SearchResultKind.ILLUSTRATION,
                                imageUrl = illust.imageUrlSquareMedium ?: illust.imageUrlMedium,
                                illust = illust
                            )
                        )
                    }
                }

            quickSearchResults.clear()
            quickSearchResults.addAll(results)
            isSearchingQuickResults = false
        }
    }

    fun executeSearch(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        val token = userSettings.pixivAccessToken
        if (token.isBlank()) return

        recentSearches.remove(clean)
        recentSearches.add(0, clean)
        persistRecentSearches()

        activeSearchFilterQuery = clean
        isSearchOverlayOpen = false
        currentPage = AppPage.HOME
        currentHomeTab = HomeTab.FOR_YOU
        runFilteredSearch()
    }

    fun updateSearchSort(sortOption: SearchSortOption) {
        searchFilters = searchFilters.copy(sortOption = sortOption)
        runFilteredSearch()
    }

    fun applySearchFilters(newFilters: SearchFilters) {
        searchFilters = newFilters
        isSearchFiltersModalOpen = false
        runFilteredSearch()
    }

    private fun runFilteredSearch() {
        val query = activeSearchFilterQuery ?: return
        val token = userSettings.pixivAccessToken
        if (token.isBlank()) return

        isLoadingSearchResults = true
        searchResultsIllusts.clear()
        searchResultsNextUrl = null

        viewModelScope.launch {
            PixEzApiClient.searchIllusts(token, query, searchFilters)
                .onSuccess { (found, next) ->
                    searchResultsIllusts.clear()
                    searchResultsIllusts.addAll(found)
                    searchResultsNextUrl = next
                }
            isLoadingSearchResults = false
        }
    }

    fun loadMoreSearchResults() {
        val query = activeSearchFilterQuery ?: return
        val token = userSettings.pixivAccessToken
        val next = searchResultsNextUrl
        if (token.isBlank() || next.isNullOrBlank() || isLoadingSearchResults) return

        isLoadingSearchResults = true
        viewModelScope.launch {
            PixEzApiClient.searchIllusts(token, query, searchFilters, next)
                .onSuccess { (more, nextUrl) ->
                    val existing = searchResultsIllusts.map { it.id }.toSet()
                    searchResultsIllusts.addAll(more.filter { it.id !in existing })
                    searchResultsNextUrl = nextUrl
                }
            isLoadingSearchResults = false
        }
    }

    fun clearActiveSearchFilter() {
        activeSearchFilterQuery = null
        searchResultsIllusts.clear()
        searchResultsNextUrl = null
    }

    fun removeRecentSearch(item: String) {
        recentSearches.remove(item)
        persistRecentSearches()
    }

    fun clearAllRecentSearches() {
        recentSearches.clear()
        persistRecentSearches()
    }

    // Settings updates
    fun updateProfileSettings(displayName: String, username: String, bio: String) {
        userSettings = userSettings.copy(
            displayName = displayName,
            username = username.removePrefix("@"),
            bio = bio
        )
        persistSettings()
        showToast("Settings saved to your universe ✦")
    }

    fun toggleAutoplayCarousel() {
        userSettings = userSettings.copy(autoplayCarousel = !userSettings.autoplayCarousel)
        persistSettings()
    }

    fun toggleHighResPreviews() {
        userSettings = userSettings.copy(highResPreviews = !userSettings.highResPreviews)
        persistSettings()
    }

    fun updateImageQuality(quality: String) {
        userSettings = userSettings.copy(imageQuality = quality)
        persistSettings()
    }

    fun updateFileNamingPattern(pattern: String) {
        userSettings = userSettings.copy(fileNamingPattern = pattern)
        persistSettings()
    }

    fun updateImageHostProxy(proxy: String) {
        userSettings = userSettings.copy(imageHostProxy = proxy)
        persistSettings()
    }

    fun updateThemeMode(mode: ThemeMode) {
        userSettings = userSettings.copy(themeMode = mode)
        persistSettings()
    }

    fun setDownloadFolder(uriString: String, label: String) {
        userSettings = userSettings.copy(
            downloadFolderUri = uriString,
            downloadFolderLabel = label
        )
        persistSettings()
        showToast("Download folder set: $label")
    }

    fun clearDownloadFolder() {
        userSettings = userSettings.copy(
            downloadFolderUri = "",
            downloadFolderLabel = ""
        )
        persistSettings()
        showToast("Download folder cleared")
    }

    fun downloadIllustrationOnLongPress(illust: IllustItem, pageIndex: Int = 0) {
        val folderUriStr = userSettings.downloadFolderUri
        if (folderUriStr.isBlank()) {
            showToast("Configure the download folder in Settings > Download settings first.")
            return
        }
        val ctx = appContext ?: return
        val token = userSettings.pixivAccessToken

        showToast("Downloading ${illust.title}...")
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val isUgoira = illust.type == "ugoira"
                    val rawUrl = if (isUgoira) {
                        val meta = ugoiraMetadata ?: PixEzApiClient.fetchUgoiraMetadata(token, illust.id).getOrThrow()
                        meta.zipUrl
                    } else {
                        when (userSettings.imageQuality) {
                            "Medium" -> illust.imageUrlMedium ?: illust.imageUrlLarge ?: illust.metaPageUrls.getOrNull(pageIndex)
                            "Large" -> illust.imageUrlLarge ?: illust.metaPageUrls.getOrNull(pageIndex) ?: illust.imageUrlMedium
                            else -> illust.metaPageUrls.getOrNull(pageIndex) ?: illust.imageUrlOriginal ?: illust.imageUrlLarge ?: illust.imageUrlMedium
                        }
                    } ?: throw IllegalStateException("No image URL available")

                    val downloadUrl = if (userSettings.imageHostProxy.contains("pixiv.re")) {
                        rawUrl.replace("https://i.pximg.net", "https://i.pixiv.re")
                    } else {
                        rawUrl
                    }

                    val bytes = PixEzApiClient.downloadPximgBytes(downloadUrl).getOrThrow()
                    val ext = if (isUgoira) "zip" else downloadUrl.substringAfterLast('.', "jpg").substringBefore('?').lowercase()
                    val mimeType = when (ext) {
                        "png" -> "image/png"
                        "webp" -> "image/webp"
                        "gif" -> "image/gif"
                        "zip" -> "application/zip"
                        else -> "image/jpeg"
                    }

                    val safeArtist = illust.artistName.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(30)
                    val safeTitle = illust.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(40)
                    val pageSuffix = if (!isUgoira && illust.pageCount > 1) "_p$pageIndex" else ""
                    val baseName = when (userSettings.fileNamingPattern) {
                        "Title — ID" -> "${safeTitle}_${illust.id}$pageSuffix"
                        "Pixiv_ID_Page" -> "${illust.id}$pageSuffix"
                        else -> "${safeArtist}_${safeTitle}_${illust.id}$pageSuffix"
                    }
                    val fileName = "$baseName.$ext"

                    val treeUri = Uri.parse(folderUriStr)
                    val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        DocumentsContract.getTreeDocumentId(treeUri)
                    )
                    val createdUri = DocumentsContract.createDocument(
                        ctx.contentResolver,
                        parentDocUri,
                        mimeType,
                        fileName
                    ) ?: throw IllegalStateException("Could not create file in selected folder")

                    ctx.contentResolver.openOutputStream(createdUri)?.use { out ->
                        out.write(bytes)
                    } ?: throw IllegalStateException("Unable to open output stream")

                    fileName
                }
            }

            result.onSuccess { savedName ->
                showToast("Saved: $savedName")
            }.onFailure { err ->
                showToast("Download failed: ${err.message}")
            }
        }
    }

    fun showToast(message: String) {
        toastMessage = message
    }

    fun consumeToast() {
        toastMessage = null
    }

    // Derived lists
    fun getCarouselArtistsLimitedTo10(): List<ArtistCard> = artists.take(10)

    fun getFavoriteTagList(): List<String> {
        val dynamicTags = favoriteIllusts
            .flatMap { it.tags }
            .map { it.name }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(10)
        return listOf("All") + dynamicTags
    }

    fun getFilteredFavoriteIllustrations(): List<IllustItem> {
        if (selectedFavoriteTag.equals("All", ignoreCase = true)) {
            return favoriteIllusts
        }
        return favoriteIllusts.filter { item ->
            item.tags.any { it.name.equals(selectedFavoriteTag, ignoreCase = true) }
        }
    }

    fun getDisplayedHomeIllustrations(): List<IllustItem> {
        if (activeSearchFilterQuery.isNullOrBlank()) {
            return recommendedIllusts.toList()
        }

        // Apply Search Filters (Work type, Like count, AI generated, Mature content)
        return searchResultsIllusts.filter { item ->
            val workTypeOk = when (searchFilters.workType) {
                SearchWorkType.ALL_ART -> item.type != "novel"
                SearchWorkType.ILLUST_AND_UGOIRA -> item.type == "illust" || item.type == "ugoira"
                SearchWorkType.ILLUST_ONLY -> item.type == "illust"
                SearchWorkType.MANGA_ONLY -> item.type == "manga"
                SearchWorkType.UGOIRA_ONLY -> item.type == "ugoira"
                SearchWorkType.NOVELS -> item.type == "novel"
            }
            val likesOk = item.bookmarkCount >= searchFilters.likeFilter.minLikes
            val aiOk = searchFilters.showAiGenerated || !item.isAiGenerated
            val matureOk = searchFilters.showMatureContent || (!item.isR18 && !item.isR18G)
            workTypeOk && likesOk && aiOk && matureOk
        }
    }
}
