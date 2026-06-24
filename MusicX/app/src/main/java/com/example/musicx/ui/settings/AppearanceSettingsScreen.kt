package com.example.musicx.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Save
import com.example.musicx.ui.theme.CustomTheme
import com.example.musicx.ui.theme.MusicXTheme
import com.example.musicx.ui.theme.ThemeState
import kotlin.reflect.KProperty1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val themeState by viewModel.themeState.collectAsState()
    val savedThemes by viewModel.savedThemes.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    
    val categories = remember {
        listOf(
            "Backgrounds" to listOf(
                ThemeColorItem("Primary", ThemeState::primaryBackground, "Main background for lists and library"),
                ThemeColorItem("Secondary", ThemeState::secondaryBackground, "Background for nested items or sections"),
                ThemeColorItem("Surface", ThemeState::surface, "Background for menus and dialogs"),
                ThemeColorItem("Surface Variant", ThemeState::surfaceVariant, "Alternate background for selected items"),
                ThemeColorItem("Card", ThemeState::cardBackground, "Background for import and info cards"),
                ThemeColorItem("Modal", ThemeState::modalBackground, "Background for overlays and popups"),
                ThemeColorItem("Bottom Bar", ThemeState::bottomBar, "Background for the bottom navigation bar"),
                ThemeColorItem("Top Bar", ThemeState::topBar, "Background for the top application bar"),
                ThemeColorItem("Sidebar", ThemeState::sidebarBackground, "Background for navigation rail on tablets"),
                ThemeColorItem("Drawer", ThemeState::drawerBackground, "Background for side navigation drawers")
            ),
            "Text" to listOf(
                ThemeColorItem("Primary Text", ThemeState::primaryText, "Title and primary information text"),
                ThemeColorItem("Secondary Text", ThemeState::secondaryText, "Subtitle and artist information text"),
                ThemeColorItem("Tertiary Text", ThemeState::tertiaryText, "Small details and metadata text"),
                ThemeColorItem("Disabled Text", ThemeState::disabledText, "Text for inactive or unavailable items"),
                ThemeColorItem("Inverse Text", ThemeState::inverseText, "Text shown on top of accent colors")
            ),
            "Borders & Outlines" to listOf(
                ThemeColorItem("Primary Border", ThemeState::primaryBorder, "Main border for cards and containers"),
                ThemeColorItem("Secondary Border", ThemeState::secondaryBorder, "Subtle borders and separators"),
                ThemeColorItem("Divider", ThemeState::divider, "Line separators between list items"),
                ThemeColorItem("Outline", ThemeState::outline, "Main outline for focusable elements"),
                ThemeColorItem("Outline Variant", ThemeState::outlineVariant, "Subtle variant of the outline color")
            ),
            "Accents" to listOf(
                ThemeColorItem("Primary Accent", ThemeState::primaryAccent, "Main brand and action color"),
                ThemeColorItem("Secondary Accent", ThemeState::secondaryAccent, "Secondary action and highlighting color"),
                ThemeColorItem("Muted Accent", ThemeState::mutedAccent, "Subtle accents for inactive states"),
                ThemeColorItem("Active Accent", ThemeState::activeAccent, "Color for currently active selections"),
                ThemeColorItem("Inactive Accent", ThemeState::inactiveAccent, "Color for background selections")
            ),
            "Buttons" to listOf(
                ThemeColorItem("Button Background", ThemeState::buttonBackground, "Base color for buttons"),
                ThemeColorItem("Button Outline", ThemeState::buttonOutline, "Border color for buttons"),
                ThemeColorItem("Button Hover", ThemeState::buttonHover, "Color when button is pointed at"),
                ThemeColorItem("Button Pressed", ThemeState::buttonPressed, "Color when button is clicked"),
                ThemeColorItem("Button Disabled", ThemeState::buttonDisabled, "Color when button is unavailable"),
                ThemeColorItem("Button Text", ThemeState::buttonText, "Color for text inside buttons")
            ),
            "Icons" to listOf(
                ThemeColorItem("Primary Icon", ThemeState::iconPrimary, "Main icons for navigation and actions"),
                ThemeColorItem("Secondary Icon", ThemeState::iconSecondary, "Subtle icons for info and details"),
                ThemeColorItem("Disabled Icon", ThemeState::iconDisabled, "Icons for unavailable features"),
                ThemeColorItem("Active Icon", ThemeState::iconActive, "Icons currently being interacted with")
            ),
            "Sliders & Progress" to listOf(
                ThemeColorItem("Slider Active", ThemeState::sliderActive, "Color for the filled part of sliders"),
                ThemeColorItem("Slider Inactive", ThemeState::sliderInactive, "Color for the empty part of sliders"),
                ThemeColorItem("Slider Thumb", ThemeState::sliderThumb, "Color for the draggable slider handle"),
                ThemeColorItem("Progress Bar", ThemeState::progressBar, "Color for the filled progress indicator"),
                ThemeColorItem("Progress Background", ThemeState::progressBackground, "Color for the empty progress track")
            ),
            "Toggles" to listOf(
                ThemeColorItem("Toggle Active", ThemeState::toggleActive, "Background when a switch is ON"),
                ThemeColorItem("Toggle Inactive", ThemeState::toggleInactive, "Background when a switch is OFF"),
                ThemeColorItem("Toggle Thumb", ThemeState::toggleThumb, "Color for the round switch handle")
            ),
            "Inputs" to listOf(
                ThemeColorItem("Background", ThemeState::inputBackground, "Inside color for search and text fields"),
                ThemeColorItem("Border", ThemeState::inputBorder, "Outline for inactive text fields"),
                ThemeColorItem("Focused Border", ThemeState::inputFocusedBorder, "Outline when typing in a text field"),
                ThemeColorItem("Input Text", ThemeState::inputText, "Color for text you type into fields"),
                ThemeColorItem("Input Hint", ThemeState::inputHint, "Color for placeholder/hint text")
            ),
            "Player Controls" to listOf(
                ThemeColorItem("Play Button", ThemeState::playButton, "The play icon in the music player"),
                ThemeColorItem("Pause Button", ThemeState::pauseButton, "The pause icon in the music player"),
                ThemeColorItem("Next Button", ThemeState::nextButton, "The skip next icon"),
                ThemeColorItem("Previous Button", ThemeState::previousButton, "The skip previous icon"),
                ThemeColorItem("Shuffle Active", ThemeState::shuffleActive, "Color for active shuffle mode"),
                ThemeColorItem("Repeat Active", ThemeState::repeatActive, "Color for active repeat mode")
            ),
            "Lyrics" to listOf(
                ThemeColorItem("Lyrics Active", ThemeState::lyricsActive, "Color for current singing line"),
                ThemeColorItem("Lyrics Inactive", ThemeState::lyricsInactive, "Color for upcoming or past lyrics")
            ),
            "Navigation" to listOf(
                ThemeColorItem("Nav Active", ThemeState::navActive, "Currently selected tab in bottom bar"),
                ThemeColorItem("Nav Inactive", ThemeState::navInactive, "Other tabs in the bottom bar")
            ),
            "Misc" to listOf(
                ThemeColorItem("Notification Background", ThemeState::notificationBackground, "Background for playback notification"),
                ThemeColorItem("Notification Text", ThemeState::notificationText, "Text color for notification controls"),
                ThemeColorItem("Splash Background", ThemeState::splashBackground, "Initial app loading background"),
                ThemeColorItem("App Icon", ThemeState::appIcon, "Color for the home screen icon"),
                ThemeColorItem("Splash Logo", ThemeState::splashLogo, "Color for the logo during startup"),
                ThemeColorItem("Album Placeholder", ThemeState::albumPlaceholder, "Color used when album art is missing")
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance Settings", color = MusicXTheme.colors.primaryText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MusicXTheme.colors.iconPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Rounded.Save, contentDescription = "Save Current Theme", tint = MusicXTheme.colors.primaryText)
                    }
                    TextButton(onClick = { viewModel.resetToDefault() }) {
                        Text("Reset", color = MusicXTheme.colors.primaryText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MusicXTheme.colors.topBar)
            )
        },
        containerColor = MusicXTheme.colors.primaryBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search colors...", color = MusicXTheme.colors.secondaryText) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MusicXTheme.colors.iconPrimary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MusicXTheme.colors.primaryText,
                    unfocusedTextColor = MusicXTheme.colors.primaryText,
                    focusedBorderColor = MusicXTheme.colors.primaryAccent,
                    unfocusedBorderColor = MusicXTheme.colors.outlineVariant,
                    focusedContainerColor = MusicXTheme.colors.inputBackground,
                    unfocusedContainerColor = MusicXTheme.colors.inputBackground
                ),
                shape = RoundedCornerShape(12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                if (searchQuery.isEmpty() && savedThemes.isNotEmpty()) {
                    item(key = "saved_themes_header", contentType = "header") {
                        Text(
                            text = "Saved Themes",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MusicXTheme.colors.primaryAccent,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            items(savedThemes, key = { it.name }) { theme ->
                                SavedThemeItem(
                                    theme = theme,
                                    onSelect = { viewModel.setTheme(theme.state) },
                                    onDelete = { viewModel.deleteTheme(theme.name) }
                                )
                            }
                        }
                    }
                }

                categories.forEach { (categoryName, items) ->
                    val filteredItems = items.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    if (filteredItems.isNotEmpty()) {
                        item(key = "${categoryName}_header", contentType = "header") {
                            Text(
                                text = categoryName,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = MusicXTheme.colors.primaryAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(filteredItems, key = { it.name }, contentType = { "color_picker" }) { item ->
                            val currentColor = item.property.get(themeState)
                            ColorPickerItem(
                                name = item.name,
                                hint = item.hint,
                                colorLong = currentColor,
                                onColorChange = { newColor ->
                                    viewModel.updateTheme { current ->
                                        updateThemeProperty(current, item.name, newColor)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        SaveThemeDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                viewModel.saveCurrentTheme(name)
                showSaveDialog = false
            }
        )
    }
}

data class ThemeColorItem(
    val name: String,
    val property: KProperty1<ThemeState, Long>,
    val hint: String
)

@Composable
fun ColorPickerItem(
    name: String,
    hint: String,
    colorLong: Long,
    onColorChange: (Long) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val animatedColor by animateColorAsState(targetValue = Color(colorLong), animationSpec = tween(500))

    ListItem(
        modifier = Modifier.clickable { showDialog = true },
        headlineContent = { Text(name, color = MusicXTheme.colors.primaryText) },
        supportingContent = { 
            Column {
                Text(hint, color = MusicXTheme.colors.secondaryText, fontSize = 12.sp)
                Text("#${colorLong.toString(16).uppercase().padStart(8, '0')}", color = MusicXTheme.colors.tertiaryText, fontSize = 11.sp)
            }
        },
        trailingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(animatedColor)
                    .border(1.dp, MusicXTheme.colors.outlineVariant, CircleShape)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )

    if (showDialog) {
        ColorPickerDialog(
            name = name,
            initialColor = colorLong,
            onDismiss = { showDialog = false },
            onColorSelected = {
                onColorChange(it)
                showDialog = false
            }
        )
    }
}

@Composable
fun SavedThemeItem(
    theme: CustomTheme,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MusicXTheme.colors.cardBackground)
            .border(1.dp, MusicXTheme.colors.secondaryBorder, RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(theme.state.primaryBackground))
                .border(2.dp, Color(theme.state.primaryAccent), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(theme.state.primaryAccent))
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = theme.name,
            color = MusicXTheme.colors.primaryText,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = "Delete",
                tint = MusicXTheme.colors.secondaryText,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SaveThemeDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var themeName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Current Theme", color = MusicXTheme.colors.primaryText) },
        text = {
            OutlinedTextField(
                value = themeName,
                onValueChange = { themeName = it },
                label = { Text("Theme Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MusicXTheme.colors.primaryText,
                    unfocusedTextColor = MusicXTheme.colors.primaryText,
                    focusedBorderColor = MusicXTheme.colors.primaryAccent
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { if (themeName.isNotBlank()) onSave(themeName) },
                enabled = themeName.isNotBlank(),
                shape = RoundedCornerShape(100),
                border = androidx.compose.foundation.BorderStroke(2.dp, MusicXTheme.colors.buttonOutline),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MusicXTheme.colors.buttonBackground,
                    contentColor = MusicXTheme.colors.buttonText
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MusicXTheme.colors.secondaryText)
            }
        },
        containerColor = MusicXTheme.colors.modalBackground
    )
}

@Composable
fun ColorPickerDialog(
    name: String,
    initialColor: Long,
    onDismiss: () -> Unit,
    onColorSelected: (Long) -> Unit
) {
    var red by remember { mutableFloatStateOf(((initialColor shr 16) and 0xFF).toFloat() / 255f) }
    var green by remember { mutableFloatStateOf(((initialColor shr 8) and 0xFF).toFloat() / 255f) }
    var blue by remember { mutableFloatStateOf((initialColor and 0xFF).toFloat() / 255f) }
    var alpha by remember { mutableFloatStateOf(((initialColor shr 24) and 0xFF).toFloat() / 255f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit $name", color = MusicXTheme.colors.primaryText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(
                            red = red,
                            green = green,
                            blue = blue,
                            alpha = alpha
                        ))
                        .border(1.dp, MusicXTheme.colors.outline, RoundedCornerShape(12.dp))
                )

                ColorSlider("Red", red) { red = it }
                ColorSlider("Green", green) { green = it }
                ColorSlider("Blue", blue) { blue = it }
                ColorSlider("Alpha", alpha) { alpha = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val r = (red * 255).toInt()
                    val g = (green * 255).toInt()
                    val b = (blue * 255).toInt()
                    val a = (alpha * 255).toInt()
                    val newColor = (a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
                    onColorSelected(newColor)
                },
                shape = RoundedCornerShape(100),
                border = androidx.compose.foundation.BorderStroke(2.dp, MusicXTheme.colors.buttonOutline),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MusicXTheme.colors.buttonBackground,
                    contentColor = MusicXTheme.colors.buttonText
                )
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MusicXTheme.colors.secondaryText)
            }
        },
        containerColor = MusicXTheme.colors.modalBackground
    )
}

@Composable
fun ColorSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = MusicXTheme.colors.primaryText, style = MaterialTheme.typography.bodySmall)
            Text((value * 255).toInt().toString(), color = MusicXTheme.colors.primaryText, style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = MusicXTheme.colors.sliderThumb,
                activeTrackColor = MusicXTheme.colors.sliderActive,
                inactiveTrackColor = MusicXTheme.colors.sliderInactive
            )
        )
    }
}

// Manual mapping because reflection is slow and restricted on Android
fun updateThemeProperty(current: ThemeState, name: String, color: Long): ThemeState {
    return when (name) {
        "Primary" -> current.copy(primaryBackground = color)
        "Secondary" -> current.copy(secondaryBackground = color)
        "Surface" -> current.copy(surface = color)
        "Surface Variant" -> current.copy(surfaceVariant = color)
        "Card" -> current.copy(cardBackground = color)
        "Modal" -> current.copy(modalBackground = color)
        "Bottom Bar" -> current.copy(bottomBar = color)
        "Top Bar" -> current.copy(topBar = color)
        "Sidebar" -> current.copy(sidebarBackground = color)
        "Drawer" -> current.copy(drawerBackground = color)
        "Primary Text" -> current.copy(primaryText = color)
        "Secondary Text" -> current.copy(secondaryText = color)
        "Tertiary Text" -> current.copy(tertiaryText = color)
        "Disabled Text" -> current.copy(disabledText = color)
        "Inverse Text" -> current.copy(inverseText = color)
        "Primary Border" -> current.copy(primaryBorder = color)
        "Secondary Border" -> current.copy(secondaryBorder = color)
        "Divider" -> current.copy(divider = color)
        "Outline" -> current.copy(outline = color)
        "Outline Variant" -> current.copy(outlineVariant = color)
        "Primary Accent" -> current.copy(primaryAccent = color)
        "Secondary Accent" -> current.copy(secondaryAccent = color)
        "Muted Accent" -> current.copy(mutedAccent = color)
        "Active Accent" -> current.copy(activeAccent = color)
        "Inactive Accent" -> current.copy(inactiveAccent = color)
        "Button Background" -> current.copy(buttonBackground = color)
        "Button Outline" -> current.copy(buttonOutline = color)
        "Button Hover" -> current.copy(buttonHover = color)
        "Button Pressed" -> current.copy(buttonPressed = color)
        "Button Disabled" -> current.copy(buttonDisabled = color)
        "Button Text" -> current.copy(buttonText = color)
        "Primary Icon" -> current.copy(iconPrimary = color)
        "Secondary Icon" -> current.copy(iconSecondary = color)
        "Disabled Icon" -> current.copy(iconDisabled = color)
        "Active Icon" -> current.copy(iconActive = color)
        "Slider Active" -> current.copy(sliderActive = color)
        "Slider Inactive" -> current.copy(sliderInactive = color)
        "Slider Thumb" -> current.copy(sliderThumb = color)
        "Progress Bar" -> current.copy(progressBar = color)
        "Progress Background" -> current.copy(progressBackground = color)
        "Toggle Active" -> current.copy(toggleActive = color)
        "Toggle Inactive" -> current.copy(toggleInactive = color)
        "Toggle Thumb" -> current.copy(toggleThumb = color)
        "Input Background" -> current.copy(inputBackground = color)
        "Input Border" -> current.copy(inputBorder = color)
        "Focused Border" -> current.copy(inputFocusedBorder = color)
        "Input Text" -> current.copy(inputText = color)
        "Input Hint" -> current.copy(inputHint = color)
        "Play Button" -> current.copy(playButton = color)
        "Pause Button" -> current.copy(pauseButton = color)
        "Next Button" -> current.copy(nextButton = color)
        "Previous Button" -> current.copy(previousButton = color)
        "Shuffle Active" -> current.copy(shuffleActive = color)
        "Repeat Active" -> current.copy(repeatActive = color)
        "Lyrics Active" -> current.copy(lyricsActive = color)
        "Lyrics Inactive" -> current.copy(lyricsInactive = color)
        "Nav Active" -> current.copy(navActive = color)
        "Nav Inactive" -> current.copy(navInactive = color)
        "Notification Background" -> current.copy(notificationBackground = color)
        "Notification Text" -> current.copy(notificationText = color)
        "Splash Background" -> current.copy(splashBackground = color)
        "App Icon" -> current.copy(appIcon = color)
        "Splash Logo" -> current.copy(splashLogo = color)
        else -> current
    }
}
