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
fun LyricsSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.generalSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lyrics", color = MusicXTheme.colors.primaryText) },
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
                Text("Lyrics Display", color = MusicXTheme.colors.primaryAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            item {
                ToggleSetting(
                    title = "Show lyrics in player", 
                    subtitle = "Automatically find and show lyrics", 
                    value = settings.showLyricsInPlayer,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(showLyricsInPlayer = newVal) } }
                )
            }

            item {
                ToggleSetting(
                    title = "Sync lyrics", 
                    subtitle = "Highlight lines as they are sung", 
                    value = settings.syncLyrics,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(syncLyrics = newVal) } }
                )
            }

            item {
                ToggleSetting(
                    title = "Romanized lyrics", 
                    subtitle = "Show pronunciation for other languages", 
                    value = settings.romanizedLyrics,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(romanizedLyrics = newVal) } }
                )
            }

            item {
                Text("Appearance", color = MusicXTheme.colors.primaryAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                ToggleSetting(
                    title = "Bigger text", 
                    subtitle = "Make lyrics easier to read", 
                    value = settings.biggerLyrics,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(biggerLyrics = newVal) } }
                )
            }

            item {
                ToggleSetting(
                    title = "Center lyrics", 
                    subtitle = "Align text to the middle of screen", 
                    value = settings.centerLyrics,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(centerLyrics = newVal) } }
                )
            }
        }
    }
}
