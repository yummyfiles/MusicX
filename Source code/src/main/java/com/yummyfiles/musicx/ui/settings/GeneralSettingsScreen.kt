package com.yummyfiles.musicx.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yummyfiles.musicx.ui.theme.MusicXTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.generalSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("General Settings", color = MusicXTheme.colors.primaryText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MusicXTheme.colors.iconPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MusicXTheme.colors.topBar)
            )
        },
        containerColor = MusicXTheme.colors.primaryBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SettingsHeader("Playback")
                SwitchSetting(
                    title = "Autoplay Next",
                    description = "Automatically play the next song in the list",
                    checked = settings.autoplayNext,
                    onCheckedChange = { checked -> viewModel.updateGeneralSettings { it.copy(autoplayNext = checked) } }
                )
                SwitchSetting(
                    title = "Jump to Now Playing",
                    description = "Open the player screen when a song starts",
                    checked = settings.jumpToNowPlaying,
                    onCheckedChange = { checked -> viewModel.updateGeneralSettings { it.copy(jumpToNowPlaying = checked) } }
                )
                SwitchSetting(
                    title = "Pause on Disconnect",
                    description = "Pause playback when headphones are disconnected",
                    checked = settings.pauseOnDisconnect,
                    onCheckedChange = { checked -> viewModel.updateGeneralSettings { it.copy(pauseOnDisconnect = checked) } }
                )
            }

            item {
                SettingsHeader("Library")
                SwitchSetting(
                    title = "Auto Refresh",
                    description = "Scan for new files when the app opens",
                    checked = settings.autoRefresh,
                    onCheckedChange = { checked -> viewModel.updateGeneralSettings { it.copy(autoRefresh = checked) } }
                )
                SwitchSetting(
                    title = "Exclude Small Files",
                    description = "Don't show audio files shorter than 30 seconds",
                    checked = settings.excludeSmallFiles,
                    onCheckedChange = { checked -> viewModel.updateGeneralSettings { it.copy(excludeSmallFiles = checked) } }
                )
            }

            item {
                SettingsHeader("Lyrics")
                SwitchSetting(
                    title = "Show Lyrics in Player",
                    description = "Display lyrics on the Now Playing screen",
                    checked = settings.showLyricsInPlayer,
                    onCheckedChange = { checked -> viewModel.updateGeneralSettings { it.copy(showLyricsInPlayer = checked) } }
                )
                SwitchSetting(
                    title = "Sync Lyrics",
                    description = "Scroll lyrics in time with the music",
                    checked = settings.syncLyrics,
                    onCheckedChange = { checked -> viewModel.updateGeneralSettings { it.copy(syncLyrics = checked) } }
                )
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        color = MusicXTheme.colors.primaryAccent,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, color = MusicXTheme.colors.primaryText) },
        supportingContent = { Text(description, color = MusicXTheme.colors.secondaryText) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MusicXTheme.colors.primaryAccent,
                    checkedTrackColor = MusicXTheme.colors.primaryAccent.copy(alpha = 0.5f)
                )
            )
        },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}
