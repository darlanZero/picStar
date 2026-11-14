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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picstar.app.data.PicStarViewModel
import com.picstar.app.model.RankingMode
import com.picstar.app.ui.components.IllustrationCard
import com.picstar.app.ui.theme.LocalPicStarColors

@Composable
fun PopularScreen(
    viewModel: PicStarViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalPicStarColors.current
    val activeMode = viewModel.selectedRankingMode
    val rankedItems = viewModel.rankingIllusts.toList()
    val pairedRows = rankedItems.chunked(2)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // Intro Header
        item(key = "popular_intro") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "WHAT THE WORLD LOVES",
                    color = colors.muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Popular now",
                    color = colors.text,
                    fontSize = 34.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Rankings refreshed every hour, across every corner of the illustration universe.",
                    color = colors.muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // 12 Pixiv Popular Ranking Tabs:
        // Daily, For Male, For Female, Original, Rookie, Weekly, Monthly, AI, XVIII AI, XVIII, XVIII weekly, XVIII G
        item(key = "ranking_tabs_row") {
            Column {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(RankingMode.entries, key = { it.name }) { mode ->
                        val isSelected = mode == activeMode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (isSelected) colors.lime else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) colors.lime else colors.line,
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .clickable { viewModel.selectRankingMode(mode) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = mode.displayName,
                                color = if (isSelected) Color(0xFF071423) else colors.muted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    color = colors.line,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Ranking Lead Section
        item(key = "ranking_lead") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "#01",
                        color = colors.coral,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Trending universe · Pixiv mode: ${activeMode.pixivApiMode}",
                        color = colors.muted,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${activeMode.displayName} ranking",
                    color = colors.text,
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activeMode.description,
                    color = colors.muted,
                    fontSize = 12.sp
                )
            }
        }

        if (viewModel.isLoadingRanking && pairedRows.isEmpty()) {
            item(key = "ranking_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.coral, strokeWidth = 3.dp)
                }
            }
        } else {
            // Ranked Illustrations 2-column Grid
            items(pairedRows.size, key = { idx -> "pop_row_${activeMode.name}_$idx" }) { rowIdx ->
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

            if (viewModel.rankingNextUrl != null) {
                item(key = "ranking_load_more") {
                    LaunchedEffect(rankedItems.size) {
                        viewModel.loadMoreRankingIllusts()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = colors.coral,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Loading more ranked illustrations…",
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
