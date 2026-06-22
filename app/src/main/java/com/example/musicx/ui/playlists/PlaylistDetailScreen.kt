package com.example.musicx.ui.playlists

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicx.model.Song
import com.example.musicx.ui.songs.SongItem
import com.example.musicx.ui.songs.SongsViewModel
import com.example.musicx.ui.theme.MusicXTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: SongsViewModel,
    onSongClick: (Song) -> Unit,
    onBack: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val songs by viewModel.songs.collectAsState()
    
    val playlist = remember(playlists, playlistId) {
        playlists.find { it.id == playlistId }
    }
    
    val playlistSongs = remember(playlist, songs) {
        if (playlist == null) emptyList()
        else {
            val uriSet = playlist.songUris.toHashSet()
            songs.filter { uriSet.contains(it.mediaUri.toString()) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        playlist?.name ?: "Playlist", 
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
                )
            )
        },
        containerColor = MusicXTheme.colors.primaryBackground
    ) { innerPadding ->
        if (playlistSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No songs in this playlist", color = MusicXTheme.colors.secondaryText)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(playlistSongs, key = { it.mediaUri.toString() }) { song ->
                    SongItem(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }
    }
}
