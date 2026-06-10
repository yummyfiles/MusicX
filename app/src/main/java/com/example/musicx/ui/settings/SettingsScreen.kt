package com.example.musicx.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicx.ui.navigation.Destination
import com.example.musicx.ui.theme.MusicXTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigate: (Destination) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings", 
                        fontSize = 32.sp, 
                        fontWeight = FontWeight.Bold,
                        color = MusicXTheme.colors.primaryText
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SettingItem("Appearance", "Theme colors and branding", onClick = { onNavigate(Destination.AppearanceSettings) }) }
            item { SettingItem("Audio", "Equalizer and sound engine", onClick = { onNavigate(Destination.AudioSettings) }) }
            item { SettingItem("Playback", "Queue behavior and controls", onClick = { onNavigate(Destination.PlaybackSettings) }) }
            item { SettingItem("Library", "Scan folders and manage metadata", onClick = { onNavigate(Destination.LibrarySettings) }) }
            item { SettingItem("Video", "Playback and background settings", onClick = { onNavigate(Destination.VideoSettings) }) }
        }
    }
}

@Composable
fun SettingItem(title: String, subtitle: String, onClick: () -> Unit) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MusicXTheme.colors.cardBackground),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MusicXTheme.colors.outline.copy(alpha = 0.2f))),
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MusicXTheme.colors.primaryText)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MusicXTheme.colors.secondaryText)
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MusicXTheme.colors.iconPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
