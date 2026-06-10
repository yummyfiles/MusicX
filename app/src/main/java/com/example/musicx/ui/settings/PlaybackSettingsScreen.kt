package com.example.musicx.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musicx.ui.theme.MusicXTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.generalSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playback", color = MusicXTheme.colors.primaryText) },
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
                Text("Queue Behavior", color = MusicXTheme.colors.primaryAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            item {
                ToggleSetting(
                    title = "Autoplay next song", 
                    subtitle = "Automatically play the next track in queue", 
                    value = settings.autoplayNext,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(autoplayNext = newVal) } }
                )
            }
            
            item {
                ToggleSetting(
                    title = "Jump to now playing", 
                    subtitle = "Open player when a song is selected", 
                    value = settings.jumpToNowPlaying,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(jumpToNowPlaying = newVal) } }
                )
            }

            item {
                ToggleSetting(
                    title = "Pause on disconnect", 
                    subtitle = "Stop playback when headphones are removed", 
                    value = settings.pauseOnDisconnect,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(pauseOnDisconnect = newVal) } }
                )
            }

            item {
                Text("Advanced", color = MusicXTheme.colors.primaryAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                ToggleSetting(
                    title = "Gapless playback", 
                    subtitle = "Remove silence between tracks", 
                    value = settings.gaplessPlayback,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(gaplessPlayback = newVal) } }
                )
            }

            item {
                ToggleSetting(
                    title = "Remember position", 
                    subtitle = "Save progress for long tracks", 
                    value = settings.rememberPosition,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(rememberPosition = newVal) } }
                )
            }

            item {
                ToggleSetting(
                    title = "Fade on play/pause", 
                    subtitle = "Smoothly transition volume", 
                    value = settings.fadeOnPlayPause,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(fadeOnPlayPause = newVal) } }
                )
            }
        }
    }
}

@Composable
fun ToggleSetting(title: String, subtitle: String, value: Boolean, onValueChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MusicXTheme.colors.primaryText, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = MusicXTheme.colors.secondaryText, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = value,
            onCheckedChange = onValueChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MusicXTheme.colors.toggleThumb,
                checkedTrackColor = MusicXTheme.colors.toggleActive,
                uncheckedThumbColor = MusicXTheme.colors.toggleThumb,
                uncheckedTrackColor = MusicXTheme.colors.toggleInactive
            )
        )
    }
}
