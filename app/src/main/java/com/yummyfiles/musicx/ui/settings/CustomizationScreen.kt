package com.yummyfiles.musicx.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yummyfiles.musicx.ui.theme.MusicXTheme
import com.yummyfiles.musicx.ui.theme.ThemeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val themeState by viewModel.themeState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customization", color = MusicXTheme.colors.primaryText) },
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
                SettingsHeader("Accent Color")
                AccentColorPicker(
                    selectedColor = themeState.primaryAccent,
                    onColorSelected = { color ->
                        viewModel.updateTheme { 
                            it.copy(
                                primaryAccent = color,
                                activeAccent = color,
                                playButton = color,
                                pauseButton = color,
                                progressBar = color,
                                sliderActive = color,
                                sliderThumb = color,
                                navActive = color,
                                lyricsActive = color,
                                buttonBackground = color,
                                buttonOutline = color,
                                shuffleActive = color,
                                repeatActive = color,
                                nextButton = color,
                                previousButton = color
                            )
                        }
                    }
                )
            }

            item {
                SettingsHeader("UI Style")
                SwitchSetting(
                    title = "Bigger Lyrics",
                    description = "Make the text on the lyrics screen larger",
                    checked = viewModel.generalSettings.collectAsState().value.biggerLyrics,
                    onCheckedChange = { checked ->
                        viewModel.updateGeneralSettings { it.copy(biggerLyrics = checked) }
                    }
                )
                SwitchSetting(
                    title = "Center Lyrics",
                    description = "Align lyrics text to the center",
                    checked = viewModel.generalSettings.collectAsState().value.centerLyrics,
                    onCheckedChange = { checked ->
                        viewModel.updateGeneralSettings { it.copy(centerLyrics = checked) }
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.resetToDefault() },
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
                ) {
                    Text("Reset to Defaults", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AccentColorPicker(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit
) {
    val colors = listOf(
        0xFFFFFFFF, // White
        0xFFBB86FC, // Purple
        0xFF03DAC6, // Teal
        0xFFFF0266, // Pink
        0xFFF44336, // Red
        0xFF4CAF50, // Green
        0xFF2196F3, // Blue
        0xFFFFC107  // Amber
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .clickable { onColorSelected(color) }
                    .border(
                        width = if (selectedColor == color) 3.dp else 0.dp,
                        color = if (selectedColor == color) Color.Gray else Color.Transparent,
                        shape = CircleShape
                    )
            )
        }
    }
}
