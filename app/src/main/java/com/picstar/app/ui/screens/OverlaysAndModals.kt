package com.picstar.app.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.picstar.app.data.PicStarViewModel
import com.picstar.app.model.ArtistCard
import com.picstar.app.model.ArtistProfileTab
import com.picstar.app.model.ArtistWorkFilter
import com.picstar.app.model.IllustItem
import com.picstar.app.model.SearchDateRange
import com.picstar.app.model.SearchFilters
import com.picstar.app.model.SearchLikeFilter
import com.picstar.app.model.SearchResultKind
import com.picstar.app.model.SearchWorkType
import com.picstar.app.ui.components.ArtistShowcaseCard
import com.picstar.app.ui.components.IllustrationCard
import com.picstar.app.ui.components.OtherArtistsToFollowDrawer
import com.picstar.app.ui.components.PximgNetworkImage
import com.picstar.app.ui.components.UgoiraAnimationPlayer
import com.picstar.app.ui.theme.LocalPicStarColors

/**
 * Floating Search Modal Overlay:
 * - Shows search input at top
 * - Default view (< 2 chars): Recent searches history + Recommended tags (populated from Pixiv API)
 * - Dropdown view (>= 2 chars): Live Quick results for illustrations, artists & autocomplete tags + "See all results"
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchOverlayModal(
    viewModel: PicStarViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalPicStarColors.current
    val query = viewModel.searchQuery
    val trimmed = query.trim()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC02080F))
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
            .padding(horizontal = 14.dp, vertical = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.navy)
                .border(1.dp, colors.line, RoundedCornerShape(22.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* consume clicks inside modal */ }
                .padding(14.dp)
        ) {
            // Top Search Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(colors.panel)
                    .border(1.dp, colors.line, RoundedCornerShape(13.dp))
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⌕",
                    color = colors.coral,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search artists, illustrations or tags…",
                            color = colors.muted.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = colors.text,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(colors.coral),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { viewModel.executeSearch(query) }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }

                if (query.isNotEmpty()) {
                    Text(
                        text = "×",
                        color = colors.muted,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clickable { viewModel.updateSearchQuery("") }
                            .padding(horizontal = 6.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(5.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "ESC",
                        color = colors.muted,
                        fontSize = 9.sp
                    )
                }
            }

            if (trimmed.length < 2) {
                // Default View: Recent Searches + Recommended Tags
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    item(key = "recent_searches_block") {
                        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent searches",
                                    color = colors.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (viewModel.recentSearches.isNotEmpty()) {
                                    Text(
                                        text = "Clear",
                                        color = colors.muted,
                                        fontSize = 10.sp,
                                        modifier = Modifier.clickable {
                                            viewModel.clearAllRecentSearches()
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (viewModel.recentSearches.isEmpty()) {
                                Text(
                                    text = "No recent searches yet.",
                                    color = colors.muted,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                viewModel.recentSearches.forEach { recent ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.updateSearchQuery(recent)
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, colors.line, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "↺",
                                                color = colors.muted,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = recent,
                                            color = colors.text,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "×",
                                            color = colors.muted,
                                            fontSize = 16.sp,
                                            modifier = Modifier
                                                .clickable { viewModel.removeRecentSearch(recent) }
                                                .padding(horizontal = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "recommended_tags_block") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
                        ) {
                            HorizontalDivider(color = colors.line)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recommended tags",
                                    color = colors.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Trending on Pixiv",
                                    color = colors.muted,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            if (viewModel.recommendedTags.isEmpty()) {
                                Text(
                                    text = "Loading trending tags from Pixiv…",
                                    color = colors.muted,
                                    fontSize = 11.sp
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    viewModel.recommendedTags.forEach { tag ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(colors.panel)
                                                .border(1.dp, colors.line, RoundedCornerShape(999.dp))
                                                .clickable {
                                                    viewModel.updateSearchQuery(tag)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                        ) {
                                            Text(
                                                text = tag,
                                                color = colors.muted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Dropdown Results View when >= 2 characters are typed
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, start = 6.dp, end = 6.dp)
                ) {
                    item(key = "quick_results_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        ) {
                            Text(
                                text = "QUICK RESULTS",
                                color = colors.muted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            if (viewModel.isSearchingQuickResults) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(
                                    color = colors.coral,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    items(viewModel.quickSearchResults, key = { it.id }) { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    when (result.kind) {
                                        SearchResultKind.ILLUSTRATION -> {
                                            result.illust?.let {
                                                viewModel.openIllustDetail(it)
                                                onDismiss()
                                            } ?: viewModel.executeSearch(result.title)
                                        }
                                        SearchResultKind.ARTIST -> {
                                            result.artist?.let {
                                                viewModel.openArtistProfile(it)
                                                onDismiss()
                                            } ?: viewModel.executeSearch(result.title)
                                        }
                                        SearchResultKind.TAG -> {
                                            viewModel.executeSearch(result.title)
                                        }
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(
                                        if (result.kind == SearchResultKind.ARTIST) CircleShape
                                        else RoundedCornerShape(9.dp)
                                    )
                            ) {
                                PximgNetworkImage(
                                    imageUrl = result.imageUrl,
                                    fallbackLabel = if (result.kind == SearchResultKind.TAG) "#" else null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.title,
                                    color = colors.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = result.subtitle,
                                    color = colors.muted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "↗",
                                color = colors.muted,
                                fontSize = 14.sp
                            )
                        }
                    }

                    item(key = "see_all_results_btn") {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = colors.line)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.executeSearch(trimmed) }
                                    .padding(vertical = 14.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "See all results for “$trimmed”",
                                    color = colors.coral2,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "→",
                                    color = colors.coral2,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Search Results Filter Modal (Opened via the Search-Results-only Floating Filter Button).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchFiltersModal(
    currentFilters: SearchFilters,
    onApply: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalPicStarColors.current
    var draft by remember(currentFilters) { mutableStateOf(currentFilters) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC02080F))
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.navy)
                .border(1.dp, colors.line, RoundedCornerShape(22.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* consume */ }
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SEARCH REFINEMENT",
                        color = colors.coral,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                    Text(
                        text = "Filter results",
                        color = colors.text,
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.panel)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "×", color = colors.text, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.line)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Work Type
                Column {
                    Text(
                        text = "Work type",
                        color = colors.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SearchWorkType.entries.forEach { wt ->
                            val selected = draft.workType == wt
                            FilterChipPill(
                                label = wt.label,
                                selected = selected,
                                onClick = { draft = draft.copy(workType = wt) }
                            )
                        }
                    }
                }

                // 2. Posting Date
                Column {
                    Text(
                        text = "Posting date",
                        color = colors.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SearchDateRange.entries.forEach { dr ->
                            val selected = draft.dateRange == dr
                            FilterChipPill(
                                label = dr.label,
                                selected = selected,
                                onClick = { draft = draft.copy(dateRange = dr) }
                            )
                        }
                    }
                }

                // 3. Like Counts
                Column {
                    Text(
                        text = "Like counts",
                        color = colors.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SearchLikeFilter.entries.forEach { lf ->
                            val selected = draft.likeFilter == lf
                            FilterChipPill(
                                label = lf.label,
                                selected = selected,
                                onClick = { draft = draft.copy(likeFilter = lf) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = colors.line)

                // 4. AI-generated toggle (moved from Preferences to Search)
                FilterToggleRow(
                    title = "Show AI-generated works",
                    subtitle = "Include illustrations created with AI assistance.",
                    checked = draft.showAiGenerated,
                    onToggle = { draft = draft.copy(showAiGenerated = !draft.showAiGenerated) }
                )

                // 5. Mature content toggle (moved from Preferences to Search)
                FilterToggleRow(
                    title = "Show mature content (XVIII / R-18)",
                    subtitle = "Display sensitive or R-18 tagged works in results.",
                    checked = draft.showMatureContent,
                    onToggle = { draft = draft.copy(showMatureContent = !draft.showMatureContent) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                        .clickable {
                            draft = SearchFilters(sortOption = currentFilters.sortOption)
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Reset", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.coral)
                        .clickable { onApply(draft) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Apply Filters",
                        color = Color(0xFF071423),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalPicStarColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) colors.coral else colors.panel)
            .border(1.dp, if (selected) colors.coral else colors.line, RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF071423) else colors.muted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun FilterToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val colors = LocalPicStarColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = colors.muted, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (checked) colors.coral else Color(0xFF304257))
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (checked) Color(0xFF071423) else Color(0xFFF7F1E8))
            )
        }
    }
}

/**
 * Full "View More Artists" Modal showing all artist cards (beyond the 10-card carousel limit),
 * plus the "Other artists to follow" drawer when following any artist!
 */
@Composable
fun AllArtistsModal(
    viewModel: PicStarViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalPicStarColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ink)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ALL RECOMMENDED CREATORS",
                        color = colors.muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Artists in your orbit (${viewModel.artists.size})",
                        color = colors.text,
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.panel)
                        .border(1.dp, colors.line, CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "×", color = colors.text, fontSize = 20.sp)
                }
            }

            HorizontalDivider(color = colors.line)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 40.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "all_artists_related_drawer") {
                    OtherArtistsToFollowDrawer(
                        seedArtist = viewModel.recentlyFollowedSeedArtist,
                        relatedArtists = viewModel.relatedArtistsToFollow,
                        isLoading = viewModel.isLoadingRelatedArtists,
                        onToggleFollow = { viewModel.toggleFollowArtist(it) },
                        onArtistClick = { viewModel.openArtistProfile(it) },
                        onIllustClick = { viewModel.openIllustDetail(it) },
                        onDismiss = { viewModel.dismissRelatedArtistsBanner() }
                    )
                }

                items(viewModel.artists, key = { "full_artist_${it.id}" }) { artist ->
                    ArtistShowcaseCard(
                        artist = artist,
                        onToggleFollow = { viewModel.toggleFollowArtist(it, artist) },
                        onArtistClick = { viewModel.openArtistProfile(it) },
                        onIllustClick = { viewModel.openIllustDetail(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Full-Screen Illustration Detail Modal:
 * - Navbar & TopBar are hidden!
 * - Displays artwork pages, title, tags, creator caption/description
 * - Creator card (clicking opens Creator Profile directly ON TOP of the post!) + Follow + "Other artists to follow"
 * - User comments (`/v3/illust/comments`)
 * - Recommended posts based on this post (`/v2/illust/related`) with infinite scrolling
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun IllustrationDetailModal(
    illust: IllustItem,
    viewModel: PicStarViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalPicStarColors.current
    val artist = viewModel.artists.find { it.id == illust.artistId }
        ?: ArtistCard(
            id = illust.artistId,
            name = illust.artistName,
            handle = illust.artistHandle,
            avatarUrl = illust.artistAvatarUrl,
            isFollowed = illust.isArtistFollowed
        )
    val relatedPaired = viewModel.relatedIllusts.chunked(2)
    var isCommentsExpanded by remember(illust.id) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ink)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            // Top bar
            item(key = "detail_topbar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.panel)
                            .clickable { onDismiss() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(text = "← Back", color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (illust.isBookmarked) colors.coral else colors.panel
                            )
                            .clickable { viewModel.toggleBookmark(illust.id) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (illust.isBookmarked) "♥ Bookmarked" else "♡ Bookmark",
                            color = if (illust.isBookmarked) Color(0xFF071423) else colors.text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Full Artwork Image(s) or Ugoira Animation
            if (illust.type == "ugoira") {
                item(key = "detail_ugoira_${illust.id}") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .aspectRatio(illust.aspectRatio.coerceIn(0.65f, 1.15f))
                            .clip(RoundedCornerShape(22.dp))
                            .background(colors.panel)
                            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
                            .combinedClickable(
                                onClick = { viewModel.openFullscreenViewer(illust, 0) },
                                onLongClick = { viewModel.downloadIllustrationOnLongPress(illust, 0) }
                            )
                    ) {
                        UgoiraAnimationPlayer(
                            metadata = viewModel.ugoiraMetadata,
                            framesMap = viewModel.ugoiraFramesMap,
                            fallbackUrl = illust.imageUrlLarge ?: illust.imageUrlMedium,
                            isLoading = viewModel.isLoadingUgoira,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xB8071423))
                                .border(1.dp, colors.line, RoundedCornerShape(999.dp))
                                .clickable { viewModel.openFullscreenViewer(illust, 0) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "⛶ Fullscreen",
                                color = colors.text,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                val pages = if (illust.metaPageUrls.isNotEmpty()) {
                    illust.metaPageUrls
                } else {
                    listOfNotNull(illust.imageUrlLarge ?: illust.imageUrlMedium)
                }

                items(pages.size, key = { idx -> "detail_page_${illust.id}_$idx" }) { pageIdx ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .aspectRatio(illust.aspectRatio.coerceIn(0.65f, 1.15f))
                            .clip(RoundedCornerShape(22.dp))
                            .background(colors.panel)
                            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
                            .combinedClickable(
                                onClick = { viewModel.openFullscreenViewer(illust, pageIdx) },
                                onLongClick = { viewModel.downloadIllustrationOnLongPress(illust, pageIdx) }
                            )
                    ) {
                        PximgNetworkImage(
                            imageUrl = pages[pageIdx],
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Metadata, Tags & Creator's Description (Caption)
            item(key = "detail_meta_${illust.id}") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = illust.title,
                        color = colors.text,
                        fontSize = 26.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Pixiv ID #${illust.id} · ${illust.createDateFormatted}",
                            color = colors.muted,
                            fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.panel)
                                .border(1.dp, colors.line, RoundedCornerShape(999.dp))
                                .clickable { viewModel.openBookmarkUsersForIllust(illust.id) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "♡ ${illust.likesFormatted} bookmarks ›",
                                color = colors.coral2,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (illust.caption.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.panel.copy(alpha = 0.55f))
                                .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "CREATOR'S DESCRIPTION",
                                color = colors.muted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.3.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = illust.caption,
                                color = colors.text.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        illust.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(colors.panel)
                                    .border(1.dp, colors.line, RoundedCornerShape(999.dp))
                                    .clickable {
                                        viewModel.detailStack.clear()
                                        viewModel.executeSearch(tag.name)
                                    }
                                    .padding(horizontal = 11.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "#${tag.name}",
                                        color = colors.coral2,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (!tag.translatedName.isNullOrBlank()) {
                                        Text(
                                            text = tag.translatedName,
                                            color = colors.muted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = colors.line)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Artist Row + Follow Button (Profile picture on the right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.panel)
                            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                            .clickable { viewModel.openArtistProfile(artist) }
                            .padding(14.dp),
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
                                text = "${artist.handle} · Tap to view profile",
                                color = colors.muted,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (artist.isFollowed) colors.panel2 else colors.coral
                                )
                                .clickable { viewModel.toggleFollowArtist(artist.id, artist) }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = if (artist.isFollowed) "Following" else "+ Follow",
                                color = if (artist.isFollowed) colors.text else Color(0xFF071423),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .border(2.dp, colors.coral, CircleShape)
                        ) {
                            PximgNetworkImage(
                                imageUrl = artist.avatarUrl,
                                fallbackLabel = artist.name.take(2).uppercase(),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // "Other artists to follow" drawer right below when user follows the artist!
                    OtherArtistsToFollowDrawer(
                        seedArtist = viewModel.recentlyFollowedSeedArtist,
                        relatedArtists = viewModel.relatedArtistsToFollow,
                        isLoading = viewModel.isLoadingRelatedArtists,
                        onToggleFollow = { viewModel.toggleFollowArtist(it) },
                        onArtistClick = { viewModel.openArtistProfile(it) },
                        onIllustClick = { viewModel.openIllustDetail(it) },
                        onDismiss = { viewModel.dismissRelatedArtistsBanner() }
                    )
                }
            }

            // Collapsible User Comments Dropdown
            item(key = "detail_comments_header_${illust.id}") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    HorizontalDivider(color = colors.line)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.panel.copy(alpha = 0.65f))
                            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                            .clickable { isCommentsExpanded = !isCommentsExpanded }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Comments (${viewModel.illustComments.size})",
                                color = colors.text,
                                fontSize = 17.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isCommentsExpanded) "Tap to collapse" else "Tap to expand comments",
                                color = colors.muted,
                                fontSize = 10.sp
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (viewModel.isLoadingComments) {
                                CircularProgressIndicator(
                                    color = colors.coral,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = if (isCommentsExpanded) "▴" else "▾",
                                color = colors.coral2,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isCommentsExpanded && !viewModel.isLoadingComments && viewModel.illustComments.isEmpty()) {
                        Text(
                            text = "No comments on this illustration yet.",
                            color = colors.muted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }
                }
            }

            if (isCommentsExpanded) {
                items(viewModel.illustComments, key = { "comment_${it.id}" }) { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.panel.copy(alpha = 0.5f))
                            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                            .clickable(enabled = c.userId > 0L) {
                                viewModel.openArtistProfile(
                                    ArtistCard(
                                        id = c.userId,
                                        name = c.userName,
                                        handle = c.userAccount,
                                        avatarUrl = c.userAvatarUrl
                                    )
                                )
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .border(1.dp, colors.line, CircleShape)
                        ) {
                            PximgNetworkImage(
                                imageUrl = c.userAvatarUrl,
                                fallbackLabel = c.userName.take(2).uppercase(),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = c.userName,
                                    color = if (c.userId > 0L) colors.coral2 else colors.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = c.dateFormatted,
                                    color = colors.muted,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = c.comment,
                                color = colors.text.copy(alpha = 0.88f),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                if (viewModel.illustCommentsNextUrl != null) {
                    item(key = "load_more_comments_btn") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                                .clickable { viewModel.loadMoreIllustComments() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Load more comments",
                                color = colors.coral2,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Recommended Posts Based on This Post (Infinite Scrolling)
            item(key = "detail_related_header_${illust.id}") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 10.dp)
                ) {
                    HorizontalDivider(color = colors.line)
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "MORE LIKE THIS",
                        color = colors.muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recommended based on this post",
                        color = colors.text,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            items(relatedPaired.size, key = { idx -> "rel_post_row_${relatedPaired[idx].first().id}_$idx" }) { rowIdx ->
                val pair = relatedPaired[rowIdx]
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

            if (viewModel.relatedIllustsNextUrl != null || viewModel.isLoadingRelatedIllusts) {
                item(key = "related_infinite_sentinel") {
                    LaunchedEffect(viewModel.relatedIllusts.size) {
                        if (viewModel.relatedIllustsNextUrl != null) {
                            viewModel.loadMoreRelatedIllusts()
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = colors.coral,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-Screen Artist Profile Modal:
 * - Navbar & TopBar are hidden!
 * - Profile picture on the right + Follow button + "Other artists to follow" when followed
 * - Navigable tabs: Works (posted illustrations) | Likes (liked illustrations) | Following (artists they follow)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArtistProfileModal(
    artist: ArtistCard,
    viewModel: PicStarViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalPicStarColors.current
    val uriHandler = LocalUriHandler.current
    val liveArtist = viewModel.activeArtistProfile ?: artist
    val activeTab = viewModel.artistProfileTab

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ink)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 48.dp)
        ) {
            item(key = "artist_profile_top") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.panel)
                            .clickable { onDismiss() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(text = "← Back", color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "CREATOR PROFILE",
                        color = colors.muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Artist Profile Header (Profile Picture on the RIGHT)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.panel)
                        .border(1.dp, colors.line, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = liveArtist.name,
                            color = colors.text,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = listOfNotNull(
                                liveArtist.handle,
                                liveArtist.followersFormatted.takeIf { it.isNotBlank() && !it.startsWith("@") }?.let { "$it following" },
                                liveArtist.totalFollowUsers.takeIf { it > 0 }?.let { "$it followers" },
                                liveArtist.totalMyPixivUsers.takeIf { it > 0 }?.let { "$it MyPixiv" }
                            ).joinToString(" · "),
                            color = colors.muted,
                            fontSize = 12.sp
                        )
                        if (liveArtist.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = liveArtist.bio,
                                color = colors.text.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        val resolvedTwitterUrl = liveArtist.twitterUrl?.takeIf { it.isNotBlank() }
                            ?: liveArtist.twitterAccount?.takeIf { it.isNotBlank() }?.let { "https://x.com/$it" }
                        val resolvedWebpage = liveArtist.webpage?.takeIf { it.isNotBlank() }
                        if (resolvedWebpage != null || resolvedTwitterUrl != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (resolvedWebpage != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(colors.panel2)
                                            .border(1.dp, colors.line, RoundedCornerShape(999.dp))
                                            .clickable { runCatching { uriHandler.openUri(resolvedWebpage) } }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "🌐 Website",
                                            color = colors.coral2,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                if (resolvedTwitterUrl != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(colors.panel2)
                                            .border(1.dp, colors.line, RoundedCornerShape(999.dp))
                                            .clickable { runCatching { uriHandler.openUri(resolvedTwitterUrl) } }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = liveArtist.twitterAccount?.takeIf { it.isNotBlank() }?.let { "𝕏 @$it" } ?: "𝕏 Twitter",
                                            color = colors.coral2,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (liveArtist.isFollowed) colors.panel2 else colors.coral
                                )
                                .clickable { viewModel.toggleFollowArtist(liveArtist.id, liveArtist) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (liveArtist.isFollowed) "Following ✓" else "+ Follow Artist",
                                color = if (liveArtist.isFollowed) colors.text else Color(0xFF071423),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Profile picture on the right
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .border(3.dp, colors.coral, CircleShape)
                    ) {
                        PximgNetworkImage(
                            imageUrl = liveArtist.avatarUrl,
                            fallbackLabel = liveArtist.name.take(2).uppercase(),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // "Other artists to follow" drawer triggered on follow!
                OtherArtistsToFollowDrawer(
                    seedArtist = viewModel.recentlyFollowedSeedArtist,
                    relatedArtists = viewModel.relatedArtistsToFollow,
                    isLoading = viewModel.isLoadingRelatedArtists,
                    onToggleFollow = { viewModel.toggleFollowArtist(it) },
                    onArtistClick = { viewModel.openArtistProfile(it) },
                    onIllustClick = { viewModel.openIllustDetail(it) },
                    onDismiss = { viewModel.dismissRelatedArtistsBanner() }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Artist Profile Tabs: Works | Likes | Following | MyPixiv | Followers
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.panel.copy(alpha = 0.7f))
                        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(ArtistProfileTab.entries, key = { it.name }) { tab ->
                        val selected = activeTab == tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selected) colors.coral else Color.Transparent)
                                .clickable { viewModel.selectArtistProfileTab(tab) }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.label,
                                color = if (selected) Color(0xFF071423) else colors.muted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            when (activeTab) {
                ArtistProfileTab.WORKS -> {
                    item(key = "works_type_filters") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            items(ArtistWorkFilter.entries, key = { it.name }) { filter ->
                                val selected = viewModel.artistWorkFilter == filter
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (selected) colors.lime else colors.panel.copy(alpha = 0.7f))
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) colors.lime else colors.line,
                                            shape = RoundedCornerShape(999.dp)
                                        )
                                        .clickable { viewModel.selectArtistWorkFilter(filter) }
                                        .padding(horizontal = 13.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = filter.label,
                                        color = if (selected) Color(0xFF071423) else colors.muted,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    if (viewModel.isLoadingArtistProfile && viewModel.artistFullWorks.isEmpty()) {
                        item(key = "works_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = colors.coral)
                            }
                        }
                    } else if (viewModel.artistFullWorks.isEmpty()) {
                        item(key = "works_empty") {
                            Text(
                                text = when (viewModel.artistWorkFilter) {
                                    ArtistWorkFilter.MANGA_SERIES -> "No manga series found."
                                    ArtistWorkFilter.MANGA -> "No manga works posted yet."
                                    ArtistWorkFilter.ILLUSTRATION -> "No illustrations posted in Works yet."
                                    ArtistWorkFilter.ALL -> "No illustrations posted in Works yet."
                                },
                                color = colors.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    } else {
                        val pairedWorks = viewModel.artistFullWorks.chunked(2)
                        items(pairedWorks.size, key = { i -> "artist_work_row_$i" }) { rowIdx ->
                            val pair = pairedWorks[rowIdx]
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
                }

                ArtistProfileTab.LIKES -> {
                    if (viewModel.isLoadingArtistTab && viewModel.artistLikedIllusts.isEmpty()) {
                        item(key = "likes_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = colors.coral)
                            }
                        }
                    } else if (viewModel.artistLikedIllusts.isEmpty()) {
                        item(key = "likes_empty") {
                            Text(
                                text = "No public liked illustrations.",
                                color = colors.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    } else {
                        val pairedLikes = viewModel.artistLikedIllusts.chunked(2)
                        items(pairedLikes.size, key = { i -> "artist_like_row_$i" }) { rowIdx ->
                            val pair = pairedLikes[rowIdx]
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
                }

                ArtistProfileTab.FOLLOWING -> artistUserListTab(
                    keyPrefix = "following",
                    users = viewModel.artistFollowingUsers,
                    isLoading = viewModel.isLoadingArtistTab,
                    emptyText = "This creator isn't following any public users.",
                    coral = colors.coral,
                    muted = colors.muted,
                    viewModel = viewModel
                )

                ArtistProfileTab.MY_PIXIV -> artistUserListTab(
                    keyPrefix = "mypixiv",
                    users = viewModel.artistMyPixivUsers,
                    isLoading = viewModel.isLoadingArtistTab,
                    emptyText = "No MyPixiv users visible.",
                    coral = colors.coral,
                    muted = colors.muted,
                    viewModel = viewModel
                )

                ArtistProfileTab.FOLLOWERS -> artistUserListTab(
                    keyPrefix = "followers",
                    users = viewModel.artistFollowersUsers,
                    isLoading = viewModel.isLoadingArtistTab,
                    emptyText = "No followers visible.",
                    coral = colors.coral,
                    muted = colors.muted,
                    viewModel = viewModel
                )
            }
        }
    }
}

private fun LazyListScope.artistUserListTab(
    keyPrefix: String,
    users: List<ArtistCard>,
    isLoading: Boolean,
    emptyText: String,
    coral: Color,
    muted: Color,
    viewModel: PicStarViewModel
) {
    if (isLoading && users.isEmpty()) {
        item(key = "${keyPrefix}_loading") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = coral)
            }
        }
    } else if (users.isEmpty()) {
        item(key = "${keyPrefix}_empty") {
            Text(
                text = emptyText,
                color = muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }
    } else {
        items(users, key = { "art_${keyPrefix}_${it.id}" }) { user ->
            ArtistShowcaseCard(
                artist = user,
                onToggleFollow = { viewModel.toggleFollowArtist(it, user) },
                onArtistClick = { viewModel.openArtistProfile(it) },
                onIllustClick = { viewModel.openIllustDetail(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }
    }
}

/**
 * PixEz-style OAuth2 PKCE Login WebView Modal that intercepts `pixiv://account/login?code=...`.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PixivOAuthWebViewModal(
    loginUrl: String,
    onCodeIntercepted: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalPicStarColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ink)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.navy)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Pixiv OAuth2 PKCE Login",
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "app-api.pixiv.net · Official OAuth",
                    color = colors.muted,
                    fontSize = 10.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.panel)
                    .clickable { onDismiss() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = "Cancel", color = colors.text, fontSize = 11.sp)
            }
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    webViewClient = object : WebViewClient() {
                        private fun checkIntercept(urlStr: String?): Boolean {
                            if (urlStr.isNullOrBlank()) return false
                            if (urlStr.startsWith("pixiv://")) {
                                val uri = Uri.parse(urlStr)
                                val code = uri.getQueryParameter("code")
                                if (!code.isNullOrBlank()) {
                                    onCodeIntercepted(code)
                                    return true
                                }
                            }
                            return false
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            return checkIntercept(request?.url?.toString())
                        }

                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            return checkIntercept(url)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            if (checkIntercept(url)) {
                                view?.stopLoading()
                            }
                        }
                    }
                    loadUrl(loginUrl)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        )
    }
}

/**
 * Modal showing the list of Pixiv users who bookmarked/liked an illustration.
 * Clicking any user opens their profile.
 */
@Composable
fun BookmarkUsersModal(
    viewModel: PicStarViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalPicStarColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ink)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.panel)
                        .clickable { onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(text = "← Back", color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "BOOKMARKED BY",
                    color = colors.muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            if (viewModel.isLoadingBookmarkUsers && viewModel.bookmarkUsersList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.coral)
                }
            } else if (viewModel.bookmarkUsersList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No public bookmark users returned.",
                        color = colors.muted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 36.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.bookmarkUsersList, key = { "bm_user_${it.id}" }) { user ->
                        ArtistShowcaseCard(
                            artist = user,
                            onToggleFollow = { viewModel.toggleFollowArtist(it, user) },
                            onArtistClick = {
                                onDismiss()
                                viewModel.openArtistProfile(it)
                            },
                            onIllustClick = {
                                onDismiss()
                                viewModel.openIllustDetail(it)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fullscreen Image & Ugoira Viewer opened on single tap in IllustrationDetailModal.
 * Supports multi-page swiping, pinch-to-zoom, and long-press to download (if folder configured).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullscreenImageViewerModal(
    illust: IllustItem,
    initialPageIndex: Int,
    viewModel: PicStarViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalPicStarColors.current
    val pages = remember(illust.id) {
        if (illust.metaPageUrls.isNotEmpty()) {
            illust.metaPageUrls
        } else {
            listOfNotNull(illust.imageUrlOriginal ?: illust.imageUrlLarge ?: illust.imageUrlMedium)
        }
    }
    val safeInitial = initialPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeInitial) { pages.size.coerceAtLeast(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (illust.type == "ugoira") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { viewModel.downloadIllustrationOnLongPress(illust, 0) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                UgoiraAnimationPlayer(
                    metadata = viewModel.ugoiraMetadata,
                    framesMap = viewModel.ugoiraFramesMap,
                    fallbackUrl = illust.imageUrlLarge ?: illust.imageUrlMedium,
                    isLoading = viewModel.isLoadingUgoira,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(illust.aspectRatio.coerceIn(0.4f, 1.8f))
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIdx ->
                var scale by remember(pageIdx) { mutableFloatStateOf(1f) }
                var offsetX by remember(pageIdx) { mutableFloatStateOf(0f) }
                var offsetY by remember(pageIdx) { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pageIdx) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 4f)
                                scale = newScale
                                if (newScale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                        .combinedClickable(
                            onClick = { onDismiss() },
                            onDoubleClick = {
                                if (scale > 1.05f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    scale = 2.2f
                                }
                            },
                            onLongClick = { viewModel.downloadIllustrationOnLongPress(illust, pageIdx) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    PximgNetworkImage(
                        imageUrl = pages.getOrNull(pageIdx),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                }
            }
        }

        // Top bar overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xB8071423))
                    .border(1.dp, colors.line, RoundedCornerShape(999.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(text = "✕ Close", color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (illust.type != "ugoira" && pages.size > 1) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xB8071423))
                        .border(1.dp, colors.line, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${pages.size}",
                        color = colors.text,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
