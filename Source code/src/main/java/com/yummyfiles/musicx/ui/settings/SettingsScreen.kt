package com.yummyfiles.musicx.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yummyfiles.musicx.ui.navigation.Destination
import com.yummyfiles.musicx.ui.theme.MusicXTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigate: (Destination) -> Unit) {
    val settingsItems = remember {
        listOf(
            SettingsCategory("General", Icons.Rounded.Settings, Destination.GeneralSettings),
            SettingsCategory("Customization", Icons.Rounded.Palette, Destination.Customization),
            SettingsCategory("About", Icons.Rounded.Info, Destination.About)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings", 
                        fontSize = 32.sp, 
                        fontWeight = FontWeight.Bold,
                        color = MusicXTheme.colors.primaryText,
                    ) 
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(settingsItems) { item ->
                SettingsItem(item) { onNavigate(item.destination) }
            }
        }
    }
}

data class SettingsCategory(
    val title: String,
    val icon: ImageVector,
    val destination: Destination
)

@Composable
fun SettingsItem(category: SettingsCategory, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { 
            Text(
                category.title, 
                color = MusicXTheme.colors.primaryText,
                fontWeight = FontWeight.Medium
            ) 
        },
        leadingContent = { 
            Icon(
                category.icon, 
                contentDescription = null, 
                tint = MusicXTheme.colors.primaryAccent 
            ) 
        },
        trailingContent = { 
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight, 
                contentDescription = null, 
                tint = MusicXTheme.colors.iconSecondary 
            ) 
        },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}
