package com.picstar.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picstar.app.api.PixEzApiClient
import com.picstar.app.model.ArtistCard
import com.picstar.app.model.IllustItem
import com.picstar.app.model.UgoiraMetadata
import com.picstar.app.ui.theme.LocalPicStarColors
import kotlinx.coroutines.delay

/**
 * Loads and renders a live Pixiv `i.pximg.net` image with the required `Referer: https://app-api.pixiv.net/` header.
 * Shows a subtle observatory placeholder while the image is downloading.
 */
@Composable
fun PximgNetworkImage(
    imageUrl: String?,
    hostProxy: String = "i.pximg.net",
    fallbackLabel: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier
) {
    val colors = LocalPicStarColors.current
    var networkBitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(imageUrl) { mutableStateOf(!imageUrl.isNullOrBlank()) }

    LaunchedEffect(imageUrl, hostProxy) {
        if (!imageUrl.isNullOrBlank()) {
            isLoading = true
            networkBitmap = PixEzApiClient.loadPximgBitmap(imageUrl, hostProxy)
            isLoading = false
        } else {
            isLoading = false
        }
    }

    val bmp = networkBitmap
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .background(
                    Brush.linearGradient(
                        listOf(colors.panel, colors.panel2)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = colors.coral.copy(alpha = 0.65f),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else if (!fallbackLabel.isNullOrBlank()) {
                Text(
                    text = fallbackLabel,
                    color = colors.coral2,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "✦",
                    color = colors.muted.copy(alpha = 0.35f),
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * Artist Card with:
 * - Profile picture on the RIGHT side of the header
 * - Artist name, handle/followers, and Follow toggle button on the left
 * - Inside the card: the 5 latest illustrations posted by the artist
 */
@Composable
fun ArtistShowcaseCard(
    artist: ArtistCard,
    onToggleFollow: (Long) -> Unit,
    onArtistClick: (ArtistCard) -> Unit,
    onIllustClick: (IllustItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalPicStarColors.current
    val works = artist.recentIllusts.take(5)
    val initials = artist.name.take(2).uppercase()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.panel.copy(alpha = 0.72f))
            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
            .clickable { onArtistClick(artist) }
            .padding(14.dp)
    ) {
        // Card Header: Name + Follow button on the left, Profile Picture on the RIGHT
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist.handle,
                    color = colors.muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Follow / Following Pill Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (artist.isFollowed) colors.panel2.copy(alpha = 0.6f)
                        else colors.coral.copy(alpha = 0.14f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (artist.isFollowed) colors.line else colors.coral.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .clickable { onToggleFollow(artist.id) }
                    .padding(horizontal = 11.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (artist.isFollowed) "Following" else "+ Follow",
                    color = if (artist.isFollowed) colors.muted else colors.coral2,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // PROFILE PICTURE ON THE RIGHT
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(2.dp, colors.coral.copy(alpha = 0.5f), CircleShape)
            ) {
                PximgNetworkImage(
                    imageUrl = artist.avatarUrl,
                    fallbackLabel = initials,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Inside the card: the 5 latest illustrations posted by the artist
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(114.dp)
                .clip(RoundedCornerShape(12.dp)),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            @Composable
            fun WorkSlot(index: Int, slotModifier: Modifier) {
                val work = works.getOrNull(index)
                Box(
                    modifier = slotModifier
                        .background(colors.panel2)
                        .let { m ->
                            if (work != null) m.clickable { onIllustClick(work) } else m
                        }
                ) {
                    PximgNetworkImage(
                        imageUrl = work?.imageUrlSquareMedium ?: work?.imageUrlMedium,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 1st latest work (tall hero tile on left)
            WorkSlot(
                index = 0,
                slotModifier = Modifier
                    .weight(1.45f)
                    .fillMaxHeight()
            )

            // 2nd & 3rd latest works (middle column)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WorkSlot(
                    index = 1,
                    slotModifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                WorkSlot(
                    index = 2,
                    slotModifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }

            // 4th & 5th latest works (right column)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WorkSlot(
                    index = 3,
                    slotModifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                WorkSlot(
                    index = 4,
                    slotModifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

/**
 * CORE PIXIV FEATURE: "Other artists to follow" drawer that expands whenever the user
 * follows an artist, powered by `/v1/user/related?seed_user_id={artistId}`.
 */
@Composable
fun OtherArtistsToFollowDrawer(
    seedArtist: ArtistCard?,
    relatedArtists: List<ArtistCard>,
    isLoading: Boolean = false,
    onToggleFollow: (Long) -> Unit,
    onArtistClick: (ArtistCard) -> Unit,
    onIllustClick: (IllustItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalPicStarColors.current

    AnimatedVisibility(
        visible = seedArtist != null && (isLoading || relatedArtists.isNotEmpty()),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        if (seedArtist != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                colors.panel2.copy(alpha = 0.92f),
                                colors.panel.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .border(1.dp, colors.coral.copy(alpha = 0.38f), RoundedCornerShape(20.dp))
                    .padding(vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "✦ OTHER ARTISTS TO FOLLOW",
                            color = colors.coral,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Because you followed ${seedArtist.name}",
                            color = colors.text,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(colors.ink.copy(alpha = 0.45f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "×",
                            color = colors.muted,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading && relatedArtists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = colors.coral,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Finding artists similar to ${seedArtist.name}…",
                                color = colors.muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(relatedArtists, key = { "rel_${it.id}" }) { relArtist ->
                            ArtistShowcaseCard(
                                artist = relArtist,
                                onToggleFollow = onToggleFollow,
                                onArtistClick = onArtistClick,
                                onIllustClick = onIllustClick,
                                modifier = Modifier.width(276.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Illustration Card used in Recommended, Following, Popular Rankings, and Favorites grids.
 */
@Composable
fun IllustrationCard(
    item: IllustItem,
    onToggleBookmark: (Long) -> Unit,
    onOpenDetail: (IllustItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalPicStarColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenDetail(item) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(item.aspectRatio.coerceIn(0.68f, 1.15f))
                .clip(RoundedCornerShape(15.dp))
                .background(colors.panel)
        ) {
            PximgNetworkImage(
                imageUrl = item.imageUrlMedium ?: item.imageUrlSquareMedium,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom subtle vignette gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xAA030A14))
                        )
                    )
            )

            // Optional Rank Number badge (#01, #02...)
            if (item.rankNumber != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(9.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xC8071423))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "#%02d".format(item.rankNumber),
                        color = Color(0xFFF7F1E8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Badges on Top Right (Ugoira GIF / AI / R-18 / Multi-page)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(9.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (item.type == "ugoira") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xD810243A))
                            .border(1.dp, colors.coral, RoundedCornerShape(999.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "GIF",
                            color = colors.coral2,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                if (item.pageCount > 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xB8071423))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${item.pageCount}p",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (item.isR18 || item.isR18G) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.coral)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (item.isR18G) "XVIII-G" else "XVIII",
                            color = Color(0xFF0A1827),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                if (item.isAiGenerated) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.lime)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "AI",
                            color = Color(0xFF0A1827),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Bottom Action Buttons (Open ↗ and Bookmark ♡/♥)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xB809121D))
                        .clickable { onOpenDetail(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↗",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xB809121D))
                        .clickable { onToggleBookmark(item.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (item.isBookmarked) "♥" else "♡",
                        color = if (item.isBookmarked) Color(0xFFFF8D74) else Color.White,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Meta row: Title + Author on left, Like Count on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = colors.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.artistName,
                    color = colors.muted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "♡ ${item.likesFormatted}",
                color = colors.muted,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Plays Pixiv Ugoira animations frame-by-frame in a loop, respecting each frame's `delayMs`.
 */
@Composable
fun UgoiraAnimationPlayer(
    metadata: UgoiraMetadata?,
    framesMap: Map<String, ImageBitmap>,
    fallbackUrl: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalPicStarColors.current
    val frames = metadata?.frames.orEmpty()
    var currentFrameIndex by remember(metadata, framesMap.size) { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }

    LaunchedEffect(metadata, framesMap.size, isPlaying) {
        if (isPlaying && frames.isNotEmpty() && framesMap.isNotEmpty()) {
            while (true) {
                val frame = frames[currentFrameIndex % frames.size]
                delay(frame.delayMs.toLong())
                currentFrameIndex = (currentFrameIndex + 1) % frames.size
            }
        }
    }

    Box(
        modifier = modifier.clickable { isPlaying = !isPlaying }
    ) {
        val currentBitmap = if (frames.isNotEmpty() && framesMap.isNotEmpty()) {
            val fileName = frames[currentFrameIndex % frames.size].file
            framesMap[fileName]
        } else null

        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PximgNetworkImage(
                imageUrl = fallbackUrl,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Ugoira status pill
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xC8071423))
                .border(1.dp, colors.coral.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = when {
                    isLoading -> "Loading Ugoira…"
                    framesMap.isEmpty() -> "UGOIRA"
                    isPlaying -> "⏸ Playing (${frames.size}f)"
                    else -> "▶ Paused"
                },
                color = colors.coral2,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
