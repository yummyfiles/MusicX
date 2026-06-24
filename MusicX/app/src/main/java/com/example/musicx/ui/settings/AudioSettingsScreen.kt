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
fun AudioSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.generalSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio", color = MusicXTheme.colors.primaryText) },
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
                Text("Sound Engine", color = MusicXTheme.colors.primaryAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                ToggleSetting(
                    title = "Master Equalizer", 
                    subtitle = "Enable system-wide audio tuning", 
                    value = settings.eqEnabled,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(eqEnabled = newVal) } }
                )
            }

            item {
                ToggleSetting(
                    title = "Bass Boost", 
                    subtitle = "Deepen low-end frequencies", 
                    value = settings.bassBoostEnabled,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(bassBoostEnabled = newVal) } }
                )
            }

            item {
                ToggleSetting(
                    title = "Surround Sound", 
                    subtitle = "Virtual spatial audio", 
                    value = settings.surroundSoundEnabled,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(surroundSoundEnabled = newVal) } }
                )
            }

            item {
                Text("Volume", color = MusicXTheme.colors.primaryAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                ToggleSetting(
                    title = "Normalization", 
                    subtitle = "Equalize volume across all songs", 
                    value = settings.normalizationEnabled,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(normalizationEnabled = newVal) } }
                )
            }

            item {
                ToggleSetting(
                    title = "Smart Gain", 
                    subtitle = "Automatically adjust input levels", 
                    value = settings.smartGainEnabled,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(smartGainEnabled = newVal) } }
                )
            }
        }
    }
}
