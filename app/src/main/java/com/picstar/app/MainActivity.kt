package com.picstar.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picstar.app.data.PicStarViewModel
import com.picstar.app.model.AppPage
import com.picstar.app.model.DetailRoute
import com.picstar.app.model.HomeTab
import com.picstar.app.ui.components.PximgNetworkImage
import com.picstar.app.ui.screens.AllArtistsModal
import com.picstar.app.ui.screens.ArtistProfileModal
import com.picstar.app.ui.screens.BookmarkUsersModal
import com.picstar.app.ui.screens.FavoritesScreen
import com.picstar.app.ui.screens.FullscreenImageViewerModal
import com.picstar.app.ui.screens.HomeScreen
import com.picstar.app.ui.screens.IllustrationDetailModal
import com.picstar.app.ui.screens.LoginScreen
import com.picstar.app.ui.screens.PopularScreen
import com.picstar.app.ui.screens.SearchFiltersModal
import com.picstar.app.ui.screens.SearchOverlayModal
import com.picstar.app.ui.screens.SettingsScreen
import com.picstar.app.ui.theme.LocalPicStarColors
import com.picstar.app.ui.theme.PicStarTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: PicStarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.initializeSession(this)
        handleOAuthRedirectIntent(intent)

        setContent {
            PicStarTheme(themeMode = viewModel.userSettings.themeMode) {
                PicStarAppShell(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthRedirectIntent(intent)
    }

    private fun handleOAuthRedirectIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "pixiv" && uri.host == "account") {
            val code = uri.getQueryParameter("code")
            if (!code.isNullOrBlank()) {
                viewModel.handlePixivOAuthCallbackCode(code)
            }
        }
    }
}

@Composable
fun PicStarAppShell(viewModel: PicStarViewModel) {
    val colors = LocalPicStarColors.current

    // AUTHENTICATION GATE: While not logged in, lock the user on the LoginScreen
    if (!viewModel.userSettings.isPixivConnected) {
        BackHandler(enabled = viewModel.isOAuthWebViewOpen) {
            viewModel.isOAuthWebViewOpen = false
        }
        LoginScreen(viewModel = viewModel)
        return
    }

    val activeDetail = viewModel.currentDetailRoute
    val isShowingFullscreenDetail = activeDetail != null || viewModel.showAllArtistsModal

    // Handle system back button for modals/overlays when logged in
    val hasActiveOverlay = viewModel.isSearchFiltersModalOpen ||
        viewModel.isSearchOverlayOpen ||
        viewModel.isBookmarkUsersModalOpen ||
        viewModel.fullscreenViewerState != null ||
        activeDetail != null ||
        viewModel.showAllArtistsModal ||
        viewModel.isOAuthWebViewOpen

    BackHandler(enabled = hasActiveOverlay) {
        when {
            viewModel.isOAuthWebViewOpen -> viewModel.isOAuthWebViewOpen = false
            viewModel.fullscreenViewerState != null -> viewModel.closeFullscreenViewer()
            viewModel.isSearchFiltersModalOpen -> viewModel.isSearchFiltersModalOpen = false
            viewModel.isSearchOverlayOpen -> viewModel.isSearchOverlayOpen = false
            viewModel.isBookmarkUsersModalOpen -> viewModel.isBookmarkUsersModalOpen = false
            activeDetail != null -> viewModel.popDetail()
            viewModel.showAllArtistsModal -> viewModel.showAllArtistsModal = false
        }
    }

    // Auto-dismiss Toast
    val currentToast = viewModel.toastMessage
    LaunchedEffect(currentToast) {
        if (currentToast != null) {
            delay(2600)
            viewModel.consumeToast()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.ink)) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.ink),
            containerColor = colors.ink,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (!isShowingFullscreenDetail) {
                    PicStarTopBar(
                        viewModel = viewModel,
                        modifier = Modifier.statusBarsPadding()
                    )
                }
            },
            bottomBar = {
                if (!isShowingFullscreenDetail) {
                    PicStarBottomNav(
                        currentPage = viewModel.currentPage,
                        onSelectPage = { viewModel.selectPage(it) },
                        modifier = Modifier.navigationBarsPadding()
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main Tab Content
                when (viewModel.currentPage) {
                    AppPage.HOME -> HomeScreen(viewModel = viewModel)
                    AppPage.POPULAR -> PopularScreen(viewModel = viewModel)
                    AppPage.FAVORITES -> FavoritesScreen(viewModel = viewModel)
                    AppPage.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }

                // Floating Buttons at Bottom-End
                val isSearchResultsMode = viewModel.activeSearchFilterQuery != null &&
                    viewModel.currentPage == AppPage.HOME &&
                    viewModel.currentHomeTab == HomeTab.FOR_YOU

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // FLOATING FILTERS BUTTON — Visible ONLY when viewing search results
                    if (isSearchResultsMode) {
                        Row(
                            modifier = Modifier
                                .shadow(14.dp, RoundedCornerShape(999.dp))
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.lime)
                                .clickable { viewModel.isSearchFiltersModalOpen = true }
                                .padding(horizontal = 15.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text(
                                text = "⚙",
                                color = Color(0xFF071423),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Filters",
                                color = Color(0xFF071423),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // FLOATING SEARCH BUTTON (FAB) — Available across ALL main tabs
                    Row(
                        modifier = Modifier
                            .shadow(16.dp, RoundedCornerShape(999.dp))
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.coral)
                            .clickable { viewModel.isSearchOverlayOpen = true }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⌕",
                            color = Color(0xFF111927),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Search",
                            color = Color(0xFF111927),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Fullscreen Layers ABOVE Scaffold (so TopBar & BottomNav are completely hidden!)
        if (viewModel.showAllArtistsModal) {
            AllArtistsModal(
                viewModel = viewModel,
                onDismiss = { viewModel.showAllArtistsModal = false }
            )
        }

        when (activeDetail) {
            is DetailRoute.Illust -> {
                IllustrationDetailModal(
                    illust = activeDetail.item,
                    viewModel = viewModel,
                    onDismiss = { viewModel.popDetail() }
                )
            }
            is DetailRoute.Artist -> {
                ArtistProfileModal(
                    artist = activeDetail.artist,
                    viewModel = viewModel,
                    onDismiss = { viewModel.popDetail() }
                )
            }
            null -> Unit
        }

        if (viewModel.isBookmarkUsersModalOpen) {
            BookmarkUsersModal(
                viewModel = viewModel,
                onDismiss = { viewModel.isBookmarkUsersModalOpen = false }
            )
        }

        viewModel.fullscreenViewerState?.let { (fsIllust, fsPage) ->
            FullscreenImageViewerModal(
                illust = fsIllust,
                initialPageIndex = fsPage,
                viewModel = viewModel,
                onDismiss = { viewModel.closeFullscreenViewer() }
            )
        }

        if (viewModel.isSearchOverlayOpen) {
            SearchOverlayModal(
                viewModel = viewModel,
                onDismiss = { viewModel.isSearchOverlayOpen = false }
            )
        }

        if (viewModel.isSearchFiltersModalOpen) {
            SearchFiltersModal(
                currentFilters = viewModel.searchFilters,
                onApply = { viewModel.applySearchFilters(it) },
                onDismiss = { viewModel.isSearchFiltersModalOpen = false }
            )
        }

        // Toast Notification Pill
        AnimatedVisibility(
            visible = currentToast != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 86.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .shadow(14.dp, RoundedCornerShape(999.dp))
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.cream)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "✦",
                    color = colors.coral,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentToast ?: "",
                    color = colors.ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PicStarTopBar(
    viewModel: PicStarViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalPicStarColors.current
    val firstName = viewModel.userSettings.displayName
        .split(" ")
        .firstOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: viewModel.userSettings.username.ifBlank { "Creator" }

    val pageTitle = when (viewModel.currentPage) {
        AppPage.HOME -> "Good evening, $firstName"
        AppPage.POPULAR -> "Popular constellations"
        AppPage.FAVORITES -> "Your saved universe"
        AppPage.SETTINGS -> "Your PicStar"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.coral.copy(alpha = 0.1f))
                    .border(1.dp, colors.coral.copy(alpha = 0.5f), CircleShape)
                    .clickable { viewModel.selectPage(AppPage.HOME) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✦",
                    color = colors.coral,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column {
                Text(
                    text = "PIXIV OBSERVATORY",
                    color = colors.muted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                )
                Text(
                    text = buildAnnotatedString {
                        append(pageTitle)
                        append(" ")
                        withStyle(SpanStyle(color = colors.coral)) {
                            append("✦")
                        }
                    },
                    color = colors.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Reload Feeds Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.panel.copy(alpha = 0.6f))
                    .border(1.dp, colors.line, CircleShape)
                    .clickable {
                        viewModel.showToast("Refreshing Pixiv feeds… ✦")
                        viewModel.loadAllAuthenticatedFeeds()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "↻", color = colors.text, fontSize = 15.sp)
            }

            // Authenticated User Avatar Chip
            val initials = viewModel.userSettings.displayName
                .split(" ")
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .take(2)
                .joinToString("")
                .ifBlank { "PX" }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, colors.coral.copy(alpha = 0.6f), CircleShape)
                    .clickable { viewModel.selectPage(AppPage.SETTINGS) }
            ) {
                PximgNetworkImage(
                    imageUrl = viewModel.userSettings.profileImageUrl,
                    fallbackLabel = initials,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun PicStarBottomNav(
    currentPage: AppPage,
    onSelectPage: (AppPage) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalPicStarColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.ink.copy(alpha = 0.95f))
    ) {
        HorizontalDivider(color = colors.line, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppPage.entries.forEach { page ->
                val active = page == currentPage
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectPage(page) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = page.iconSymbol,
                        color = if (active) colors.coral else colors.muted,
                        fontSize = 20.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = page.label,
                        color = if (active) colors.coral else colors.muted,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
