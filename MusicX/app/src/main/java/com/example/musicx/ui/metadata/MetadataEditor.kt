package com.example.musicx.ui.metadata

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicx.model.Song
import com.example.musicx.ui.theme.MusicXTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataEditor(
    song: Song,
    onSave: (String, String, String?) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var lyrics by remember { mutableStateOf(song.lyrics ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Edit Metadata", 
                        fontSize = 24.sp, 
                        fontWeight = FontWeight.Bold,
                        color = MusicXTheme.colors.primaryText
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MusicXTheme.colors.iconPrimary)
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Song Title", color = MusicXTheme.colors.secondaryText, style = MaterialTheme.typography.labelMedium)
            TextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MusicXTheme.colors.primaryText,
                    unfocusedTextColor = MusicXTheme.colors.primaryText,
                    focusedContainerColor = MusicXTheme.colors.inputBackground,
                    unfocusedContainerColor = MusicXTheme.colors.inputBackground,
                    focusedIndicatorColor = MusicXTheme.colors.primaryAccent,
                    unfocusedIndicatorColor = MusicXTheme.colors.outlineVariant
                )
            )

            Text("Artist Name", color = MusicXTheme.colors.secondaryText, style = MaterialTheme.typography.labelMedium)
            TextField(
                value = artist,
                onValueChange = { artist = it },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MusicXTheme.colors.primaryText,
                    unfocusedTextColor = MusicXTheme.colors.primaryText,
                    focusedContainerColor = MusicXTheme.colors.inputBackground,
                    unfocusedContainerColor = MusicXTheme.colors.inputBackground,
                    focusedIndicatorColor = MusicXTheme.colors.primaryAccent,
                    unfocusedIndicatorColor = MusicXTheme.colors.outlineVariant
                )
            )

            Text("Lyrics (Offline)", color = MusicXTheme.colors.secondaryText, style = MaterialTheme.typography.labelMedium)
            TextField(
                value = lyrics,
                onValueChange = { lyrics = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                placeholder = { Text("Paste lyrics here...", color = MusicXTheme.colors.inputHint) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MusicXTheme.colors.primaryText,
                    unfocusedTextColor = MusicXTheme.colors.primaryText,
                    focusedContainerColor = MusicXTheme.colors.inputBackground,
                    unfocusedContainerColor = MusicXTheme.colors.inputBackground,
                    focusedIndicatorColor = MusicXTheme.colors.primaryAccent,
                    unfocusedIndicatorColor = MusicXTheme.colors.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onSave(title, artist, lyrics.ifBlank { null }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = MusicXTheme.colors.buttonOutline
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MusicXTheme.colors.buttonBackground, 
                    contentColor = MusicXTheme.colors.buttonText
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}
