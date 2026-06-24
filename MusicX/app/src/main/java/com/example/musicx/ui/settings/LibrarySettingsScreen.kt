package com.example.musicx.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun LibrarySettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.generalSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library", color = MusicXTheme.colors.primaryText) },
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
                Text("Library Management", color = MusicXTheme.colors.primaryAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            item {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(100),
                    border = androidx.compose.foundation.BorderStroke(2.dp, MusicXTheme.colors.buttonOutline),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MusicXTheme.colors.buttonBackground,
                        contentColor = MusicXTheme.colors.buttonText
                    )
                ) {
                    Text("Rescan Media")
                }
            }

            item {
                ToggleSetting(
                    title = "Auto-refresh", 
                    subtitle = "Scan for new files on startup", 
                    value = settings.autoRefresh,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(autoRefresh = newVal) } }
                )
            }

            item {
                ToggleSetting(
                    title = "Exclude small files", 
                    subtitle = "Hide audio shorter than 30s", 
                    value = settings.excludeSmallFiles,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(excludeSmallFiles = newVal) } }
                )
            }

            item {
                Text("Metadata", color = MusicXTheme.colors.primaryAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                ToggleSetting(
                    title = "Prefer embedded art", 
                    subtitle = "Use art from inside files first", 
                    value = settings.preferEmbeddedArt,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(preferEmbeddedArt = newVal) } }
                )
            }

            item {
                ToggleSetting(
                    title = "Ignore .nomedia", 
                    subtitle = "Scan hidden folders", 
                    value = settings.ignoreNoMedia,
                    onValueChange = { newVal -> viewModel.updateGeneralSettings { it.copy(ignoreNoMedia = newVal) } }
                )
            }
        }
    }
}
