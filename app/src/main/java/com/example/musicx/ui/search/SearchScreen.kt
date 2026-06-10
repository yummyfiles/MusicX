package com.example.musicx.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicx.model.Song
import com.example.musicx.ui.songs.SongsViewModel
import com.example.musicx.ui.theme.MusicXTheme
import coil.compose.AsyncImage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun SearchScreen(
    viewModel: SongsViewModel,
    onSongClick: (Song) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val songs by viewModel.songs.collectAsState()
    
    val debouncedQuery by produceState("") {
        snapshotFlow { query }
            .debounce(300)
            .collect { value = it }
    }
    
    val filteredSongs = remember(debouncedQuery, songs) {
        if (debouncedQuery.isBlank()) emptyList()
        else {
            songs.filter { 
                it.title.contains(debouncedQuery, ignoreCase = true) || 
                it.artist.contains(debouncedQuery, ignoreCase = true) 
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Search", 
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                placeholder = { Text("Search songs or artists...", color = MusicXTheme.colors.secondaryText.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MusicXTheme.colors.iconPrimary) },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    unfocusedContainerColor = MusicXTheme.colors.surface,
                    focusedContainerColor = MusicXTheme.colors.surfaceVariant,
                    focusedTextColor = MusicXTheme.colors.primaryText,
                    unfocusedTextColor = MusicXTheme.colors.primaryText
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (query.isBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Search for your favorite tracks", color = MusicXTheme.colors.secondaryText)
                }
            } else if (filteredSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No songs found for \"$query\"", color = MusicXTheme.colors.secondaryText)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = filteredSongs,
                        key = { it.mediaUri.toString() },
                        contentType = { "search_song" }
                    ) { song ->
                        val onClick = remember(song, onSongClick) {
                            { onSongClick(song) }
                        }
                        SearchSongItem(song = song, onClick = onClick)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchSongItem(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (song.albumArtUri != null) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MusicXTheme.colors.albumPlaceholder)
                    .border(2.dp, MusicXTheme.colors.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MusicXTheme.colors.albumPlaceholder)
                    .border(2.dp, MusicXTheme.colors.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MusicXTheme.colors.iconPrimary.copy(alpha = 0.5f)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = MusicXTheme.colors.primaryText,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MusicXTheme.colors.secondaryText,
                maxLines = 1
            )
        }
    }
}
