package com.picstar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picstar.app.data.PicStarViewModel
import com.picstar.app.model.ArtistCard
import com.picstar.app.model.FollowingSortMode
import com.picstar.app.model.HighlightCard
import com.picstar.app.model.HomeTab
import com.picstar.app.model.IllustItem
import com.picstar.app.model.SearchSortOption
import com.picstar.app.ui.components.ArtistShowcaseCard
import com.picstar.app.ui.components.IllustrationCard
import com.picstar.app.ui.components.OtherArtistsToFollowDrawer
import com.picstar.app.ui.components.PximgNetworkImage
import com.picstar.app.ui.theme.LocalPicStarColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: PicStarViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalPicStarColors.current
    val followedCount = viewModel.followedArtistsStream.size

    Column(modifier = modifier.fillMaxSize()) {
        // Segmented Home Tabs: "For you" | "Following"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeTabButton(
                title = HomeTab.FOR_YOU.label,
                badgeCount = null,
                isActive = viewModel.currentHomeTab == HomeTab.FOR_YOU,
                onClick = { viewModel.selectHomeTab(HomeTab.FOR_YOU) }
            )
            HomeTabButton(
                title = HomeTab.FOLLOWING.label,
                badgeCount = followedCount.takeIf { it > 0 },
                isActive = viewModel.currentHomeTab == HomeTab.FOLLOWING,
                onClick = { viewModel.selectHomeTab(HomeTab.FOLLOWING) }
            )
        }
        HorizontalDivider(color = colors.line, thickness = 1.dp)

        when (viewModel.currentHomeTab) {
            HomeTab.FOR_YOU -> ForYouTabContent(viewModel = viewModel)
            HomeTab.FOLLOWING -> FollowingTabContent(viewModel = viewModel)
        }
    }
}

@Composable
private fun HomeTabButton(
    title: String,
    badgeCount: Int?,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalPicStarColors.current
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(top = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 11.dp)
        ) {
            Text(
                text = title,
                color = if (isActive) colors.text else colors.muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (badgeCount != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.panel2)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeCount.toString(),
                        color = colors.text,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(if (badgeCount != null) 92.dp else 58.dp)
                .background(
                    color = if (isActive) colors.coral else Color.Transparent,
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

@Composable
private fun ForYouTabContent(viewModel: PicStarViewModel) {
    val colors = LocalPicStarColors.current
    val recommendedList = viewModel.getDisplayedHomeIllustrations()
    val pairedRows = recommendedList.chunked(2)
    val top10Artists = viewModel.getCarouselArtistsLimitedTo10()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // 1. Pixiv Highlights Carousel
        item(key = "highlights_carousel") {
            HighlightsCarouselSection(
                highlights = viewModel.highlights,
                isLoading = viewModel.isLoadingHighlights,
                autoplay = viewModel.userSettings.autoplayCarousel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }

        // 2. Artists Carousel (limited to 10 + "View more" button + profile pic on right + 5 latest works)
        item(key = "artists_carousel_section") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "HANDPICKED FOR YOU",
                            color = colors.muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Artists in your orbit",
                            color = colors.text,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // View More button to see all artist cards
                    if (viewModel.artists.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { viewModel.showAllArtistsModal = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "View more (${viewModel.artists.size})",
                                color = colors.coral2,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "→",
                                color = colors.coral2,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (viewModel.isLoadingArtists && top10Artists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = colors.coral,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    // Carousel limited to 10 artists + trailing "View more" card
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(top10Artists, key = { "carousel_artist_${it.id}" }) { artist ->
                            ArtistShowcaseCard(
                                artist = artist,
                                onToggleFollow = { viewModel.toggleFollowArtist(it, artist) },
                                onArtistClick = { viewModel.openArtistProfile(it) },
                                onIllustClick = { viewModel.openIllustDetail(it) },
                                modifier = Modifier.width(292.dp)
                            )
                        }

                        if (viewModel.artists.isNotEmpty()) {
                            item(key = "view_more_artists_card") {
                                Box(
                                    modifier = Modifier
                                        .width(156.dp)
                                        .height(198.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(colors.panel.copy(alpha = 0.5f))
                                        .border(1.dp, colors.coral.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                                        .clickable { viewModel.showAllArtistsModal = true }
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(colors.coral.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "→",
                                                color = colors.coral,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "View more",
                                            color = colors.text,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "See all ${viewModel.artists.size} artists",
                                            color = colors.muted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // "Other artists to follow" drawer that expands when following an artist!
                OtherArtistsToFollowDrawer(
                    seedArtist = viewModel.recentlyFollowedSeedArtist,
                    relatedArtists = viewModel.relatedArtistsToFollow,
                    isLoading = viewModel.isLoadingRelatedArtists,
                    onToggleFollow = { viewModel.toggleFollowArtist(it) },
                    onArtistClick = { viewModel.openArtistProfile(it) },
                    onIllustClick = { viewModel.openIllustDetail(it) },
                    onDismiss = { viewModel.dismissRelatedArtistsBanner() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // 3. Recommended Illustrations Header OR Search Results Header + Sort Chips
        item(key = "recommended_header") {
            val activeQuery = viewModel.activeSearchFilterQuery
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 30.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (!activeQuery.isNullOrBlank()) "PIXIV SEARCH RESULTS" else "TUNED TO YOUR TASTE",
                            color = colors.muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (!activeQuery.isNullOrBlank()) "Results for “$activeQuery”" else "Recommended illustrations",
                            color = colors.text,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!activeQuery.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.coral.copy(alpha = 0.18f))
                                .border(1.dp, colors.coral, RoundedCornerShape(10.dp))
                                .clickable { viewModel.clearActiveSearchFilter() }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Clear ×",
                                color = colors.coral2,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Navigable Search Sort Chips: Newest | Popular ⌄ (All / Male / Female) | Oldest
                if (!activeQuery.isNullOrBlank()) {
                    var popularMenuExpanded by remember { mutableStateOf(false) }
                    val currentSort = viewModel.searchFilters.sortOption
                    val isPopularActive = currentSort == SearchSortOption.POPULAR ||
                        currentSort == SearchSortOption.POPULAR_MALE ||
                        currentSort == SearchSortOption.POPULAR_FEMALE
                    val popularLabel = when (currentSort) {
                        SearchSortOption.POPULAR_MALE -> "Popular · Male ⌄"
                        SearchSortOption.POPULAR_FEMALE -> "Popular · Female ⌄"
                        else -> "Popular ⌄"
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .padding(bottom = 10.dp)
                    ) {
                        // Newest chip
                        val newestSelected = currentSort == SearchSortOption.NEWEST
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (newestSelected) colors.lime else colors.panel.copy(alpha = 0.7f))
                                .border(
                                    width = 1.dp,
                                    color = if (newestSelected) colors.lime else colors.line,
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .clickable { viewModel.updateSearchSort(SearchSortOption.NEWEST) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = SearchSortOption.NEWEST.label,
                                color = if (newestSelected) Color(0xFF071423) else colors.muted,
                                fontSize = 11.sp,
                                fontWeight = if (newestSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }

                        // Unified Popular chip with DropdownMenu (All / Male / Female)
                        Box {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (isPopularActive) colors.lime else colors.panel.copy(alpha = 0.7f))
                                    .border(
                                        width = 1.dp,
                                        color = if (isPopularActive) colors.lime else colors.line,
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .clickable { popularMenuExpanded = true }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = popularLabel,
                                    color = if (isPopularActive) Color(0xFF071423) else colors.muted,
                                    fontSize = 11.sp,
                                    fontWeight = if (isPopularActive) FontWeight.Bold else FontWeight.Medium
                                )
                            }

                            DropdownMenu(
                                expanded = popularMenuExpanded,
                                onDismissRequest = { popularMenuExpanded = false },
                                containerColor = colors.panel2
                            ) {
                                listOf(
                                    SearchSortOption.POPULAR to "Popular (All)",
                                    SearchSortOption.POPULAR_MALE to "Popular · Male",
                                    SearchSortOption.POPULAR_FEMALE to "Popular · Female"
                                ).forEach { (opt, label) ->
                                    val optSelected = currentSort == opt
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (optSelected) "✓ $label" else label,
                                                color = if (optSelected) colors.lime else colors.text,
                                                fontSize = 12.sp,
                                                fontWeight = if (optSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        onClick = {
                                            popularMenuExpanded = false
                                            viewModel.updateSearchSort(opt)
                                        }
                                    )
                                }
                            }
                        }

                        // Oldest chip
                        val oldestSelected = currentSort == SearchSortOption.OLDEST
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (oldestSelected) colors.lime else colors.panel.copy(alpha = 0.7f))
                                .border(
                                    width = 1.dp,
                                    color = if (oldestSelected) colors.lime else colors.line,
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .clickable { viewModel.updateSearchSort(SearchSortOption.OLDEST) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = SearchSortOption.OLDEST.label,
                                color = if (oldestSelected) Color(0xFF071423) else colors.muted,
                                fontSize = 11.sp,
                                fontWeight = if (oldestSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        if ((viewModel.isLoadingRecommended || viewModel.isLoadingSearchResults) && recommendedList.isEmpty()) {
            item(key = "recommended_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colors.coral,
                        strokeWidth = 3.dp
                    )
                }
            }
        } else {
            // 4. Recommended Illustrations 2-column Grid with Infinite Scroll
            items(pairedRows.size, key = { idx -> "rec_row_${pairedRows[idx].first().id}_$idx" }) { rowIndex ->
                val pair = pairedRows[rowIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IllustrationCard(
                        item = pair[0],
                        onToggleBookmark = { viewModel.toggleBookmark(it) },
                        onOpenDetail = { viewModel.openIllustDetail(it) },
                        modifier = Modifier.weight(1f)
                    )
                    if (pair.size > 1) {
                        IllustrationCard(
                            item = pair[1],
                            onToggleBookmark = { viewModel.toggleBookmark(it) },
                            onOpenDetail = { viewModel.openIllustDetail(it) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 5. Infinite Scroll Sentinel (Recommended OR Search Results)
            val hasMore = if (viewModel.activeSearchFilterQuery != null) {
                viewModel.searchResultsNextUrl != null
            } else {
                viewModel.recommendedNextUrl != null
            }
            if (hasMore) {
                item(key = "infinite_scroll_sentinel") {
                    LaunchedEffect(recommendedList.size) {
                        if (viewModel.activeSearchFilterQuery != null) {
                            viewModel.loadMoreSearchResults()
                        } else {
                            viewModel.loadMoreRecommendedIllusts()
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = colors.coral,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Reading the constellations… loading more works",
                                color = colors.muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightsCarouselSection(
    highlights: List<HighlightCard>,
    isLoading: Boolean,
    autoplay: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalPicStarColors.current

    if (isLoading && highlights.isEmpty()) {
        Box(
            modifier = modifier
                .height(320.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.panel)
                .border(1.dp, colors.line, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colors.coral, strokeWidth = 2.5.dp)
        }
        return
    }

    if (highlights.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { highlights.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(autoplay, highlights.size) {
        if (autoplay && highlights.size > 1) {
            while (true) {
                delay(5500)
                val next = (pagerState.currentPage + 1) % highlights.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Box(
        modifier = modifier
            .height(340.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, colors.line, RoundedCornerShape(24.dp))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = highlights[page]
            Box(modifier = Modifier.fillMaxSize()) {
                PximgNetworkImage(
                    imageUrl = item.imageUrl,
                    modifier = Modifier.fillMaxSize()
                )

                // Editorial dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x22040A15),
                                    Color(0xBB040A15),
                                    Color(0xEE040A15)
                                )
                            )
                        )
                )

                // Content at bottom-left
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(22.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xE6FFFAEF))
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = item.badge,
                            color = Color(0xFF101827),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = item.kicker,
                        color = Color(0xFFFFBEA9),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.title,
                        color = Color(0xFFF7F1E8),
                        fontSize = 26.sp,
                        lineHeight = 30.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.coral),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.authorInitials,
                                color = Color(0xFF071423),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = item.authorName,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.authorMeta,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Top-right controls: dots + prev/next buttons
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                repeat(highlights.size.coerceAtMost(6)) { index ->
                    val active = pagerState.currentPage % 6 == index
                    Box(
                        modifier = Modifier
                            .height(5.dp)
                            .width(if (active) 18.dp else 5.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (active) colors.coral else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x66030A14))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .clickable {
                        scope.launch {
                            val prev = (pagerState.currentPage - 1 + highlights.size) % highlights.size
                            pagerState.animateScrollToPage(prev)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "←", color = Color.White, fontSize = 14.sp)
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x66030A14))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .clickable {
                        scope.launch {
                            val next = (pagerState.currentPage + 1) % highlights.size
                            pagerState.animateScrollToPage(next)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "→", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

/**
 * Second Tab of Home: "Following"
 * Shows either:
 * 1) Latest illustrations from followed artists (`FollowingSortMode.LATEST`), or
 * 2) Illustrations grouped "By followed artists" (`FollowingSortMode.BY_ARTISTS`)
 */
@Composable
private fun FollowingTabContent(viewModel: PicStarViewModel) {
    val colors = LocalPicStarColors.current
    val followedArtists = viewModel.followedArtistsStream
    val latestFromFollowed = viewModel.followingLatestIllusts
    val pairedLatest = latestFromFollowed.chunked(2)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 110.dp)
    ) {
        item(key = "following_header") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "FROM YOUR CIRCLE",
                    color = colors.muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Fresh from artists you follow",
                    color = colors.text,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Top Sort Switch: "Latest" vs "By followed artists"
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.panel.copy(alpha = 0.7f))
                        .border(1.dp, colors.line, RoundedCornerShape(11.dp))
                        .padding(3.dp)
                ) {
                    FollowingSortMode.entries.forEach { mode ->
                        val active = viewModel.followingSortMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (active) colors.coral else Color.Transparent
                                )
                                .clickable { viewModel.selectFollowingSortMode(mode) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = mode.label,
                                color = if (active) Color(0xFF071423) else colors.muted,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        if (viewModel.isLoadingFollowing && latestFromFollowed.isEmpty() && followedArtists.isEmpty()) {
            item(key = "loading_following") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.coral, strokeWidth = 3.dp)
                }
            }
        } else if (viewModel.followingSortMode == FollowingSortMode.LATEST) {
            if (pairedLatest.isEmpty()) {
                item(key = "empty_following_latest") {
                    EmptyFollowingPlaceholder()
                }
            } else {
                items(pairedLatest.size, key = { i -> "fol_latest_$i" }) { rowIdx ->
                    val pair = pairedLatest[rowIdx]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IllustrationCard(
                            item = pair[0],
                            onToggleBookmark = { viewModel.toggleBookmark(it) },
                            onOpenDetail = { viewModel.openIllustDetail(it) },
                            modifier = Modifier.weight(1f)
                        )
                        if (pair.size > 1) {
                            IllustrationCard(
                                item = pair[1],
                                onToggleBookmark = { viewModel.toggleBookmark(it) },
                                onOpenDetail = { viewModel.openIllustDetail(it) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            // Mode: "By followed artists" — Latest illustrations grouped per followed artist
            if (followedArtists.isEmpty()) {
                item(key = "empty_following_artists") {
                    EmptyFollowingPlaceholder()
                }
            } else {
                items(followedArtists, key = { "stream_artist_${it.id}" }) { artist ->
                    FollowedArtistStreamGroup(
                        artist = artist,
                        onArtistClick = { viewModel.openArtistProfile(it) },
                        onIllustClick = { viewModel.openIllustDetail(it) },
                        onToggleFollow = { viewModel.toggleFollowArtist(it, artist) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFollowingPlaceholder() {
    val colors = LocalPicStarColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, colors.line, RoundedCornerShape(20.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "✦",
                color = colors.coral,
                fontSize = 38.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No works from followed artists yet",
                color = colors.text,
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Follow creators in the For You tab to see their newest works here.",
                color = colors.muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun FollowedArtistStreamGroup(
    artist: ArtistCard,
    onArtistClick: (ArtistCard) -> Unit,
    onIllustClick: (IllustItem) -> Unit,
    onToggleFollow: (Long) -> Unit
) {
    val colors = LocalPicStarColors.current
    val works = artist.recentIllusts.take(5)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.panel.copy(alpha = 0.55f))
            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onArtistClick(artist) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = listOfNotNull(
                        artist.handle,
                        artist.latestPostDate.takeIf { it.isNotBlank() }?.let { "Latest: $it" }
                    ).joinToString(" · "),
                    color = colors.muted,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, colors.line, RoundedCornerShape(999.dp))
                    .clickable { onToggleFollow(artist.id) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (artist.isFollowed) "Following" else "+ Follow",
                    color = colors.muted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Profile picture on the right
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .border(2.dp, colors.coral.copy(alpha = 0.5f), CircleShape)
            ) {
                PximgNetworkImage(
                    imageUrl = artist.avatarUrl,
                    fallbackLabel = artist.name.take(2).uppercase(),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal strip of the artist's latest posted illustrations
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(works, key = { "stream_work_${it.id}" }) { work ->
                Column(
                    modifier = Modifier
                        .width(126.dp)
                        .clickable { onIllustClick(work) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        PximgNetworkImage(
                            imageUrl = work.imageUrlMedium ?: work.imageUrlSquareMedium,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = work.title,
                        color = colors.text,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
