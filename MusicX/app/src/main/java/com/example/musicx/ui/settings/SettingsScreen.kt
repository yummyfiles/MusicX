package com.example.musicx.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicx.data.GeneralSettings
import com.example.musicx.ui.navigation.Destination
import com.example.musicx.ui.theme.MusicXTheme
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigate: (Destination) -> Unit,
    generalSettings: GeneralSettings = GeneralSettings(),
    onUpdateGeneralSettings: ((GeneralSettings) -> GeneralSettings) -> Unit = {}
) {
    val context = LocalContext.current
    var feedbackType by remember { mutableStateOf<FeedbackType?>(null) }
    var feedbackText by remember { mutableStateOf("") }
    var updateCheckState by remember { mutableStateOf<UpdateCheckState>(UpdateCheckState.Idle) }
    val scope = rememberCoroutineScope()

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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SettingItem("Appearance", "Theme colors and branding", onClick = { onNavigate(Destination.AppearanceSettings) }) }
            item { SettingItem("Audio", "Equalizer and sound engine", onClick = { onNavigate(Destination.AudioSettings) }) }
            item { SettingItem("Playback", "Queue behavior and controls", onClick = { onNavigate(Destination.PlaybackSettings) }) }
            item { SettingItem("Library", "Scan folders and manage metadata", onClick = { onNavigate(Destination.LibrarySettings) }) }
            item { SettingItem("Video", "Playback and background settings", onClick = { onNavigate(Destination.VideoSettings) }) }
            item { SettingItem("Customize Tabs", "Show/hide bottom navigation tabs", onClick = { onNavigate(Destination.CustomizeTabs) }) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MusicXTheme.colors.cardBackground),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MusicXTheme.colors.outline.copy(alpha = 0.2f)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Logo Animation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MusicXTheme.colors.primaryText)
                            Text("Disable the splash logo on app start", style = MaterialTheme.typography.bodyMedium, color = MusicXTheme.colors.secondaryText)
                        }
                        Switch(
                            checked = generalSettings.showSplash,
                            onCheckedChange = { checked ->
                                onUpdateGeneralSettings { it.copy(showSplash = checked) }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = MusicXTheme.colors.primaryAccent)
                        )
                    }
                }
            }
            item {
                FeedbackButton(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "Check for Updates",
                    subtitle = when (val state = updateCheckState) {
                        is UpdateCheckState.Idle -> "See if a new version is available"
                        is UpdateCheckState.Checking -> "Checking..."
                        is UpdateCheckState.Latest -> "Already up to date (${state.version})"
                        is UpdateCheckState.Available -> "Update available!"
                        is UpdateCheckState.Error -> "Check failed"
                    },
                    enabled = updateCheckState !is UpdateCheckState.Checking,
                    onClick = {
                        scope.launch {
                            updateCheckState = UpdateCheckState.Checking
                            updateCheckState = withContext(Dispatchers.IO) { checkForUpdate() }
                        }
                    }
                )
            }
            item {
                FeedbackButton(
                    icon = Icons.Rounded.BugReport,
                    title = "Bug Report",
                    subtitle = "Report a bug on GitHub",
                    onClick = { feedbackType = FeedbackType.BUG_REPORT; feedbackText = "" }
                )
            }
            item {
                FeedbackButton(
                    icon = Icons.Rounded.Lightbulb,
                    title = "Feature Request",
                    subtitle = "Suggest an idea on GitHub",
                    onClick = { feedbackType = FeedbackType.FEATURE_REQUEST; feedbackText = "" }
                )
            }
            item {
                FeedbackButton(
                    icon = Icons.Rounded.Star,
                    title = "Leave a Review",
                    subtitle = "Share your thoughts on GitHub",
                    onClick = { feedbackType = FeedbackType.REVIEW; feedbackText = "" }
                )
            }
            item {
                SponsorButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://thanks.dev/en/d/6KXMRMNgtnwuJRgeYwZSyVkGp48y2E5C01KVh5kSuT6EilEqPZ0GvlnqtkXE3OEY")
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }
    }

    feedbackType?.let { type ->
        FeedbackDialog(
            title = type.label,
            text = feedbackText,
            onTextChange = { feedbackText = it },
            onConfirm = {
                val encoded = URLEncoder.encode(feedbackText.ifBlank { "(no details)" }, "UTF-8")
                val label = type.label.lowercase().replace(" ", "+")
                val url = "https://github.com/yummyfiles/MusicX/issues/new?labels=$label&body=$encoded"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                feedbackType = null
            },
            onDismiss = { feedbackType = null }
        )
    }

    when (val state = updateCheckState) {
        is UpdateCheckState.Available -> {
            AlertDialog(
                onDismissRequest = { updateCheckState = UpdateCheckState.Idle },
                containerColor = MusicXTheme.colors.cardBackground,
                title = { Text("Update Available", fontWeight = FontWeight.Bold, color = MusicXTheme.colors.primaryText) },
                text = {
                    Text("Version ${state.version} is available!", color = MusicXTheme.colors.secondaryText)
                },
                confirmButton = {
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.url)))
                        updateCheckState = UpdateCheckState.Idle
                    }) {
                        Text("Download", color = MusicXTheme.colors.primaryAccent)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { updateCheckState = UpdateCheckState.Idle }) {
                        Text("Later", color = MusicXTheme.colors.secondaryText)
                    }
                }
            )
        }
        is UpdateCheckState.Error -> {
            AlertDialog(
                onDismissRequest = { updateCheckState = UpdateCheckState.Idle },
                containerColor = MusicXTheme.colors.cardBackground,
                title = { Text("Check Failed", fontWeight = FontWeight.Bold, color = MusicXTheme.colors.primaryText) },
                text = {
                    Text(state.message, color = MusicXTheme.colors.secondaryText)
                },
                confirmButton = {
                    TextButton(onClick = { updateCheckState = UpdateCheckState.Idle }) {
                        Text("OK", color = MusicXTheme.colors.primaryAccent)
                    }
                }
            )
        }
        else -> {}
    }
}

private enum class FeedbackType(val label: String) {
    BUG_REPORT("Bug Report"),
    FEATURE_REQUEST("Feature Request"),
    REVIEW("Review")
}

@Composable
private fun FeedbackDialog(
    title: String,
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MusicXTheme.colors.cardBackground,
        titleContentColor = MusicXTheme.colors.primaryText,
        textContentColor = MusicXTheme.colors.secondaryText,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Write your feedback...") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MusicXTheme.colors.primaryText,
                    unfocusedTextColor = MusicXTheme.colors.primaryText,
                    cursorColor = MusicXTheme.colors.primaryAccent,
                    focusedBorderColor = MusicXTheme.colors.primaryAccent,
                    unfocusedBorderColor = MusicXTheme.colors.outline
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Submit", color = MusicXTheme.colors.primaryAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MusicXTheme.colors.secondaryText)
            }
        }
    )
}

@Composable
fun FeedbackButton(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit, enabled: Boolean = true) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.5f
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MusicXTheme.colors.cardBackground),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MusicXTheme.colors.outline.copy(alpha = 0.2f))),
        onClick = { if (enabled) onClick() },
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MusicXTheme.colors.iconPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
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

@Composable
fun SponsorButton(onClick: () -> Unit) {
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
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))),
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Sponsor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MusicXTheme.colors.primaryText)
                Text("Support the developer", style = MaterialTheme.typography.bodyMedium, color = MusicXTheme.colors.secondaryText)
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

private sealed class UpdateCheckState {
    data object Idle : UpdateCheckState()
    data object Checking : UpdateCheckState()
    data class Latest(val version: String) : UpdateCheckState()
    data class Available(val version: String, val url: String) : UpdateCheckState()
    data class Error(val message: String) : UpdateCheckState()
}

private fun checkForUpdate(): UpdateCheckState {
    return try {
        val client = OkHttpClient()
        val request = okhttp3.Request.Builder()
            .url("https://api.github.com/repos/yummyfiles/MusicX/releases/latest")
            .header("Accept", "application/vnd.github.v3+json")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            return UpdateCheckState.Error("GitHub API returned ${response.code}")
        }
        val body = response.body?.string() ?: return UpdateCheckState.Error("Empty response")
        val json = JSONObject(body)
        val latestTag = json.optString("tag_name", "").removePrefix("v")
        val htmlUrl = json.optString("html_url", "")
        if (latestTag.isBlank()) return UpdateCheckState.Error("No version found")

        val currentVersion = "1.6.0"
        val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latestTag.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            when {
                l > c -> return UpdateCheckState.Available(latestTag, htmlUrl)
                l < c -> return UpdateCheckState.Latest(currentVersion)
            }
        }
        UpdateCheckState.Latest(currentVersion)
    } catch (e: Exception) {
        UpdateCheckState.Error(e.message ?: "Unknown error")
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
