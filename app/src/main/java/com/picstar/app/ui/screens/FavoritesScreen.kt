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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picstar.app.data.PicStarViewModel
import com.picstar.app.ui.components.IllustrationCard
import com.picstar.app.ui.theme.LocalPicStarColors

@Composable
fun FavoritesScreen(
    viewModel: PicStarViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalPicStarColors.current
    val favorites = viewModel.getFilteredFavoriteIllustrations()
    val pairedRows = favorites.chunked(2)
    val filterTags = viewModel.getFavoriteTagList()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // Header
        item(key = "favorites_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "YOUR PERSONAL CONSTELLATION",
                        color = colors.muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Favorites",
                        color = colors.text,
                        fontSize = 34.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${favorites.size} illustrations saved in your Pixiv bookmarks.",
                        color = colors.muted,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.panel.copy(alpha = 0.6f))
                        .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                        .clickable { viewModel.loadUserBookmarks() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "↻ Refresh",
                        color = colors.muted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Dynamic Tag Filter Row from user's bookmarked illustrations
        item(key = "favorites_tags") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterTags, key = { it }) { tag ->
                    val active = viewModel.selectedFavoriteTag.equals(tag, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (active) colors.coral else Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = if (active) colors.coral else colors.line,
                                shape = RoundedCornerShape(999.dp)
                            )
                            .clickable { viewModel.selectFavoriteTag(tag) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = tag,
                            color = if (active) Color(0xFF071423) else colors.muted,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (viewModel.isLoadingFavorites && favorites.isEmpty()) {
            item(key = "favorites_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colors.coral,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        } else if (favorites.isEmpty()) {
            item(key = "favorites_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 32.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(22.dp))
                        .padding(vertical = 64.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "♡",
                            color = colors.coral,
                            fontSize = 48.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Your constellation is waiting",
                            color = colors.text,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the heart on any illustration to save it to your Pixiv bookmarks.",
                            color = colors.muted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(pairedRows.size, key = { idx -> "fav_row_${pairedRows[idx].first().id}_$idx" }) { rowIdx ->
                val pair = pairedRows[rowIdx]
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

            if (viewModel.favoritesNextUrl != null && viewModel.selectedFavoriteTag.equals("All", ignoreCase = true)) {
                item(key = "favorites_load_more") {
                    LaunchedEffect(favorites.size) {
                        viewModel.loadMoreUserBookmarks()
                    }
                }
            }
        }
    }
}
