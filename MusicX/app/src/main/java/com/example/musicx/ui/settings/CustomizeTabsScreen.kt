package com.example.musicx.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicx.data.GeneralSettings
import com.example.musicx.ui.theme.MusicXTheme

private data class TabOption(val id: String, val label: String)

private val ALL_TAB_OPTIONS = listOf(
    TabOption("Songs", "Songs"),
    TabOption("Artists", "Artists"),
    TabOption("Albums", "Albums"),
    TabOption("Genres", "Genres"),
    TabOption("Playlists", "Playlists"),
    TabOption("Import", "Import"),
    TabOption("Search", "Search"),
    TabOption("Settings", "Settings")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeTabsScreen(
    generalSettings: GeneralSettings,
    onSave: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    var visibleTabs by remember(generalSettings) {
        mutableStateOf(generalSettings.visibleTabs.toSet())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Customize Tabs",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MusicXTheme.colors.primaryText
                    )
                },
                navigationIcon = {
                    TextButton(onClick = {
                        onSave(visibleTabs.toList())
                        onBack()
                    }) {
                        Text("< Back", color = MusicXTheme.colors.primaryAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MusicXTheme.colors.topBar,
                    titleContentColor = MusicXTheme.colors.primaryText
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = MusicXTheme.colors.primaryBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Select which tabs appear in the bottom bar",
                style = MaterialTheme.typography.bodyMedium,
                color = MusicXTheme.colors.secondaryText,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(ALL_TAB_OPTIONS.size) { index ->
                    val option = ALL_TAB_OPTIONS[index]
                    val checked = option.id in visibleTabs
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MusicXTheme.colors.cardBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MusicXTheme.colors.primaryText,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = checked,
                                onCheckedChange = { newChecked ->
                                    visibleTabs = if (newChecked) {
                                        visibleTabs + option.id
                                    } else {
                                        visibleTabs - option.id
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MusicXTheme.colors.primaryAccent
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
