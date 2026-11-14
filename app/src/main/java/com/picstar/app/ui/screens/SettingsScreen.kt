package com.picstar.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picstar.app.data.PicStarViewModel
import com.picstar.app.model.SettingsSection
import com.picstar.app.model.ThemeMode
import com.picstar.app.ui.components.PximgNetworkImage
import com.picstar.app.ui.theme.LocalPicStarColors

@Composable
fun SettingsScreen(
    viewModel: PicStarViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalPicStarColors.current
    val activeSection = viewModel.selectedSettingsSection

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // Intro Header
        item(key = "settings_intro") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "MAKE IT YOURS",
                    color = colors.muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Settings",
                    color = colors.text,
                    fontSize = 34.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Shape PicStar around the way you discover and collect art.",
                    color = colors.muted,
                    fontSize = 12.sp
                )
            }
        }

        // Settings Section Tabs: Profile | Preferences | Download settings | Theme | About
        item(key = "settings_tabs") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SettingsSection.entries, key = { it.name }) { section ->
                    val selected = section == activeSection
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) colors.coral.copy(alpha = 0.14f) else Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) colors.coral.copy(alpha = 0.5f) else colors.line,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.selectSettingsSection(section) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = section.label,
                            color = if (selected) colors.coral2 else colors.muted,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Active Settings Panel Container
        item(key = "settings_panel_${activeSection.name}") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.panel.copy(alpha = 0.6f))
                    .border(1.dp, colors.line, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                when (activeSection) {
                    SettingsSection.PROFILE -> ProfileSettingsPanel(viewModel)
                    SettingsSection.PREFERENCES -> PreferencesSettingsPanel(viewModel)
                    SettingsSection.DOWNLOADS -> DownloadsSettingsPanel(viewModel)
                    SettingsSection.THEME -> ThemeSettingsPanel(viewModel)
                    SettingsSection.ABOUT -> AboutSettingsPanel()
                }
            }
        }
    }
}

@Composable
private fun ProfileSettingsPanel(viewModel: PicStarViewModel) {
    val colors = LocalPicStarColors.current
    val settings = viewModel.userSettings

    var displayName by remember(settings.displayName) { mutableStateOf(settings.displayName) }
    var username by remember(settings.username) { mutableStateOf(settings.username) }
    var bio by remember(settings.bio) { mutableStateOf(settings.bio) }

    Column {
        Text(
            text = "Profile",
            color = colors.text,
            fontSize = 24.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Authenticated Pixiv Account (ID: ${settings.userId.ifBlank { "—" }})",
            color = colors.muted,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = colors.line)
        Spacer(modifier = Modifier.height(18.dp))

        // Avatar Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val initials = displayName.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .take(2)
                .joinToString("")
                .ifBlank { "PX" }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(3.dp, colors.coral.copy(alpha = 0.6f), CircleShape)
            ) {
                PximgNetworkImage(
                    imageUrl = settings.profileImageUrl,
                    fallbackLabel = initials,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = displayName.ifBlank { "Pixiv User" },
                    color = colors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${username.removePrefix("@")}",
                    color = colors.coral2,
                    fontSize = 12.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.panel2)
                            .border(1.dp, colors.line, RoundedCornerShape(8.dp))
                            .clickable { viewModel.loadAllAuthenticatedFeeds() }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "↻ Reload Feeds",
                            color = colors.text,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, colors.coral.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { viewModel.logoutPixivAccount() }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "Sign out",
                            color = colors.coral2,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Display Name input
        SettingInputField(
            label = "Display name",
            value = displayName,
            onValueChange = { displayName = it }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Username input
        SettingInputField(
            label = "Username",
            prefix = "@",
            value = username,
            onValueChange = { username = it }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Bio input
        SettingInputField(
            label = "Bio",
            value = bio,
            onValueChange = { bio = it },
            singleLine = false
        )

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(colors.coral)
                .clickable {
                    viewModel.updateProfileSettings(displayName, username, bio)
                }
                .padding(horizontal = 16.dp, vertical = 11.dp)
        ) {
            Text(
                text = "Save changes",
                color = Color(0xFF111827),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SettingInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    prefix: String? = null,
    singleLine: Boolean = true
) {
    val colors = LocalPicStarColors.current
    Column {
        Text(
            text = label,
            color = colors.muted,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(colors.ink.copy(alpha = 0.45f))
                .border(1.dp, colors.line, RoundedCornerShape(9.dp))
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (prefix != null) {
                Text(
                    text = prefix,
                    color = colors.muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = TextStyle(
                    color = colors.text,
                    fontSize = 13.sp
                ),
                cursorBrush = SolidColor(colors.coral),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PreferencesSettingsPanel(viewModel: PicStarViewModel) {
    val colors = LocalPicStarColors.current
    val s = viewModel.userSettings

    Column {
        Text(
            text = "Preferences",
            color = colors.text,
            fontSize = 24.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Fine-tune your discovery feed.",
            color = colors.muted,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = colors.line)

        PreferenceToggleRow(
            title = "Autoplay carousel",
            subtitle = "Advance featured artwork automatically.",
            checked = s.autoplayCarousel,
            onToggle = { viewModel.toggleAutoplayCarousel() }
        )
        PreferenceToggleRow(
            title = "High-resolution previews",
            subtitle = "Use more data for sharper imagery.",
            checked = s.highResPreviews,
            onToggle = { viewModel.toggleHighResPreviews() }
        )
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val colors = LocalPicStarColors.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = colors.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = colors.muted,
                    fontSize = 11.sp
                )
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
        HorizontalDivider(color = colors.line)
    }
}

@Composable
private fun DownloadsSettingsPanel(viewModel: PicStarViewModel) {
    val colors = LocalPicStarColors.current
    val context = LocalContext.current
    val s = viewModel.userSettings

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            val label = uri.lastPathSegment?.substringAfterLast(':')?.ifBlank { uri.toString() } ?: uri.toString()
            viewModel.setDownloadFolder(uri.toString(), label)
        }
    }

    val qualities = listOf("Original quality", "Large", "Medium")
    val namingPatterns = listOf("Artist — Title", "Title — ID", "Pixiv_ID_Page")
    val proxies = listOf("i.pximg.net (Direct + Referer)", "i.pixiv.re (Mirror)")

    Column {
        Text(
            text = "Download settings",
            color = colors.text,
            fontSize = 24.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Control how illustrations are saved and proxied.",
            color = colors.muted,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = colors.line)

        // Download Folder (Images & Ugoira) — required before long-press download works
        Column(modifier = Modifier.padding(vertical = 14.dp)) {
            Text(
                text = "Download folder (Images & Ugoira)",
                color = colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (s.downloadFolderUri.isNotBlank()) {
                    "Saving to: ${s.downloadFolderLabel}"
                } else {
                    "Not configured. Set a folder to enable long-press download on posts."
                },
                color = if (s.downloadFolderUri.isNotBlank()) colors.lime else colors.coral2,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.coral)
                        .clickable { folderPicker.launch(null) }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = if (s.downloadFolderUri.isNotBlank()) "Change folder" else "Select folder",
                        color = Color(0xFF071423),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (s.downloadFolderUri.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.panel)
                            .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                            .clickable { viewModel.clearDownloadFolder() }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = "Clear",
                            color = colors.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        OptionSelectorBlock(
            title = "Image quality",
            subtitle = "Choose the default download resolution.",
            options = qualities,
            selectedOption = s.imageQuality,
            onSelect = { viewModel.updateImageQuality(it) }
        )

        OptionSelectorBlock(
            title = "File naming",
            subtitle = "Pattern used for downloaded artwork.",
            options = namingPatterns,
            selectedOption = s.fileNamingPattern,
            onSelect = { viewModel.updateFileNamingPattern(it) }
        )

        OptionSelectorBlock(
            title = "Pximg Image Host",
            subtitle = "Pixiv image server or reverse-proxy mirror.",
            options = proxies,
            selectedOption = s.imageHostProxy,
            onSelect = { viewModel.updateImageHostProxy(it) }
        )
    }
}

@Composable
private fun OptionSelectorBlock(
    title: String,
    subtitle: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit
) {
    val colors = LocalPicStarColors.current
    Column(modifier = Modifier.padding(vertical = 14.dp)) {
        Text(
            text = title,
            color = colors.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = subtitle,
            color = colors.muted,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                val active = option == selectedOption
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (active) colors.coral.copy(alpha = 0.16f) else colors.ink.copy(alpha = 0.4f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (active) colors.coral else colors.line,
                            shape = RoundedCornerShape(9.dp)
                        )
                        .clickable { onSelect(option) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = option,
                        color = if (active) colors.coral2 else colors.muted,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = colors.line)
    }
}

@Composable
private fun ThemeSettingsPanel(viewModel: PicStarViewModel) {
    val colors = LocalPicStarColors.current
    val currentTheme = viewModel.userSettings.themeMode

    Column {
        Text(
            text = "Theme",
            color = colors.text,
            fontSize = 24.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Pick the atmosphere that feels like yours.",
            color = colors.muted,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = colors.line)
        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemeMode.entries.forEach { mode ->
                val active = mode == currentTheme
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.ink.copy(alpha = 0.35f))
                        .border(
                            width = if (active) 2.dp else 1.dp,
                            color = if (active) colors.coral else colors.line,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.updateThemeMode(mode) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val brush = when (mode) {
                        ThemeMode.MIDNIGHT -> Brush.linearGradient(
                            listOf(Color(0xFF091624), Color(0xFFFF795F), Color(0xFF142B44))
                        )
                        ThemeMode.DAYLIGHT -> Brush.linearGradient(
                            listOf(Color(0xFFF7F3EB), Color(0xFFFF795F), Color(0xFFDBE7EF))
                        )
                        ThemeMode.SYSTEM -> Brush.linearGradient(
                            listOf(Color(0xFF091624), Color(0xFFF7F3EB))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(96.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = mode.label,
                            color = colors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = mode.subtitle,
                            color = colors.muted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutSettingsPanel() {
    val colors = LocalPicStarColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "About PicStar",
                color = colors.text,
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Version 1.0.0 · Made for people who love illustration.",
                color = colors.muted,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = colors.line)
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "✦",
            color = colors.coral,
            fontSize = 62.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "PicStar is a modern Kotlin & Jetpack Compose Pixiv client powered by the Pixiv App-API (PixEz architecture), designed for exploring artwork, following creators, discovering similar artists and building a personal visual universe.",
            color = colors.muted,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
