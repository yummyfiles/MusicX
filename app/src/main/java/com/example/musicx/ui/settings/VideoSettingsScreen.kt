package com.example.musicx.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musicx.ui.theme.MusicXTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.generalSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video", color = MusicXTheme.colors.primaryText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MusicXTheme.colors.iconPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MusicXTheme.colors.topBar)
            )
        },
        containerColor = MusicXTheme.colors.primaryBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text("Video Playback", color = MusicXTheme.colors.primaryAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                ToggleSetting(
                    title = "Hardware acceleration", 
                    subtitle = "Use GPU for smoother video", 
                    value = settings.hardwareAcceleration,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(hardwareAcceleration = newVal) } }
                )
            }

            item {
                ToggleSetting(
                    title = "Autoplay videos", 
                    subtitle = "Start videos automatically in lists", 
                    value = settings.autoplayVideos,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(autoplayVideos = newVal) } }
                )
            }

            item {
                ToggleSetting(
                    title = "Loop videos", 
                    subtitle = "Restart video when finished", 
                    value = settings.loopVideos,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(loopVideos = newVal) } }
                )
            }

            item {
                Text("Streaming", color = MusicXTheme.colors.primaryAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                ToggleSetting(
                    title = "High quality only", 
                    subtitle = "Only play 1080p and higher", 
                    value = settings.highQualityOnly,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(highQualityOnly = newVal) } }
                )
            }

            item {
                ToggleSetting(
                    title = "Show subtitles", 
                    subtitle = "Enable closed captions by default", 
                    value = settings.showSubtitles,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(showSubtitles = newVal) } }
                )
            }
        }
    }
}
