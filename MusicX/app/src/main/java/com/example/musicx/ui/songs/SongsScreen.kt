package com.example.musicx.ui.songs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.Person
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.musicx.data.local.entity.Playlist
import com.example.musicx.model.Song
import com.example.musicx.ui.theme.MusicXTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SongsScreen(
    viewModel: SongsViewModel,
    mediaController: androidx.media3.session.MediaController? = null,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onEditMetadata: (Long) -> Unit,
    onBrowseArtists: () -> Unit = {},
    onBrowseAlbums: () -> Unit = {},
    onBrowseGenres: () -> Unit = {}
) {
    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedUris by viewModel.selectedSongUris.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val likedSongs = remember(songs) { songs.filter { it.isLiked } }
    val unlikedSongs = remember(songs) { songs.filter { !it.isLiked } }
    
    // Track current playing song for UI feedback
    var currentMediaId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(mediaController, mediaController?.currentMediaItem) {
        currentMediaId = mediaController?.currentMediaItem?.mediaId
    }

    DisposableEffect(mediaController) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                currentMediaId = mediaItem?.mediaId
            }
        }
        mediaController?.addListener(listener)
        onDispose { mediaController?.removeListener(listener) }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isSelectionMode) "${selectedUris.size} Selected" else "Songs", 
                        fontSize = if (isSelectionMode) 24.sp else 32.sp, 
                        fontWeight = FontWeight.Bold,
                        color = MusicXTheme.colors.primaryText
                    )
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = MusicXTheme.colors.iconPrimary)
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        if (selectedUris.isNotEmpty()) {
                            if (selectedUris.size == 1) {
                                IconButton(onClick = {
                                    val uri = selectedUris.first()
                                    val song = songs.find { it.mediaUri.toString() == uri }
                                    if (song != null) {
                                        viewModel.toggleSelectionMode()
                                        onEditMetadata(song.id)
                                    }
                                }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Metadata", tint = MusicXTheme.colors.iconPrimary)
                                }
                            }
                            IconButton(onClick = { showPlaylistDialog = true }) {
                                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "Add to Playlist", tint = MusicXTheme.colors.iconPrimary)
                            }
                            IconButton(onClick = { viewModel.deleteSelectedSongs() }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MusicXTheme.colors.iconPrimary)
                            }
                        }
                    } else {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Menu", tint = MusicXTheme.colors.iconPrimary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(MusicXTheme.colors.surface)
                                .border(1.dp, MusicXTheme.colors.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Songs", color = MusicXTheme.colors.primaryText) },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleSelectionMode()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MusicXTheme.colors.topBar,
                    titleContentColor = MusicXTheme.colors.primaryText
                ),
                windowInsets = WindowInsets(0, 0, 0, 0) // Fixed: Let Scaffold handle it
            )
        },
        containerColor = MusicXTheme.colors.primaryBackground
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MusicXTheme.colors.primaryAccent)
            }
        } else if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No songs found", color = MusicXTheme.colors.secondaryText)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "browse_sections", contentType = "browse_sections") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Browse Library",
                            style = MaterialTheme.typography.titleMedium,
                            color = MusicXTheme.colors.primaryAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BrowseSectionCard(
                                title = "Artists",
                                icon = Icons.Rounded.Person,
                                onClick = onBrowseArtists,
                                modifier = Modifier.weight(1f)
                            )
                            BrowseSectionCard(
                                title = "Albums",
                                icon = Icons.Rounded.Album,
                                onClick = onBrowseAlbums,
                                modifier = Modifier.weight(1f)
                            )
                            BrowseSectionCard(
                                title = "Genres",
                                icon = Icons.Rounded.MusicNote,
                                onClick = onBrowseGenres,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (likedSongs.isNotEmpty()) {
                    item(key = "liked_header", contentType = "header") {
                        Text(
                            "Liked Songs",
                            style = MaterialTheme.typography.titleMedium,
                            color = MusicXTheme.colors.primaryAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(
                        items = likedSongs,
                        key = { it.mediaUri.toString() },
                        contentType = { "liked_song" }
                    ) { song ->
                        SongItem(
                            song = song,
                            onLikeClick = { viewModel.toggleLike(song) },
                            onClick = {
                                if (isSelectionMode) viewModel.toggleSongSelection(song.mediaUri.toString())
                                else onSongClick(song)
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    viewModel.toggleSelectionMode()
                                    viewModel.toggleSongSelection(song.mediaUri.toString())
                                } else {
                                    onSongLongClick(song)
                                }
                            },
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedUris.contains(song.mediaUri.toString()),
                            isCurrentlyPlaying = currentMediaId == song.id.toString()
                        )
                    }
                }

                if (unlikedSongs.isNotEmpty()) {
                    item(key = "all_songs_header", contentType = "header") {
                        Text(
                            if (likedSongs.isNotEmpty()) "All Songs" else "Songs",
                            style = MaterialTheme.typography.titleMedium,
                            color = MusicXTheme.colors.primaryAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(
                        items = unlikedSongs,
                        key = { it.mediaUri.toString() },
                        contentType = { "song" }
                    ) { song ->
                        SongItem(
                            song = song,
                            onLikeClick = { viewModel.toggleLike(song) },
                            onClick = {
                                if (isSelectionMode) viewModel.toggleSongSelection(song.mediaUri.toString())
                                else onSongClick(song)
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    viewModel.toggleSelectionMode()
                                    viewModel.toggleSongSelection(song.mediaUri.toString())
                                } else {
                                    onSongLongClick(song)
                                }
                            },
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedUris.contains(song.mediaUri.toString()),
                            isCurrentlyPlaying = currentMediaId == song.id.toString()
                        )
                    }
                }
            }
        }
    }

    if (showPlaylistDialog) {
        PlaylistSelectionDialog(
            playlists = playlists,
            onDismiss = { showPlaylistDialog = false },
            onPlaylistSelected = { playlist ->
                viewModel.addSongsToPlaylist(playlist, selectedUris.toList())
                viewModel.toggleSelectionMode()
                showPlaylistDialog = false
            }
        )
    }
}

@Composable
private fun BrowseSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MusicXTheme.colors.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MusicXTheme.colors.primaryAccent,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MusicXTheme.colors.primaryText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SongItem(
    song: Song, 
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onLikeClick: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isCurrentlyPlaying: Boolean = false
) {
    val (displayTitle, displayArtist) = remember(song.title, song.artist) {
        processSongDisplay(song.title, song.artist)
    }

    val bgTint by animateColorAsState(
        targetValue = if (isSelected) MusicXTheme.colors.surfaceVariant else Color.Transparent,
        animationSpec = tween(200), label = "SongItemBg"
    )

    val borderColor = if (isCurrentlyPlaying) {
        MusicXTheme.colors.primaryAccent.copy(alpha = if (isSelected) 0.1f else 0f)
    } else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgTint)
            .then(
                if (isCurrentlyPlaying && isSelected) Modifier.border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.albumArtUri)
                        .size(128)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MusicXTheme.colors.albumPlaceholder)
                        .border(2.dp, MusicXTheme.colors.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MusicXTheme.colors.albumPlaceholder)
                        .border(2.dp, MusicXTheme.colors.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MusicXTheme.colors.iconPrimary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            val titleColor = if (isCurrentlyPlaying) MusicXTheme.colors.primaryAccent else MusicXTheme.colors.primaryText
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val artistColor = if (isCurrentlyPlaying) MusicXTheme.colors.primaryAccent.copy(alpha = 0.7f) else MusicXTheme.colors.secondaryText
                Text(
                    text = displayArtist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = artistColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatDuration(song.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MusicXTheme.colors.secondaryText,
                )
            }
        }

        if (!isSelectionMode) {
            IconButton(
                onClick = { onLikeClick?.invoke() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (song.isLiked) "Unlike" else "Like",
                    tint = if (song.isLiked) MusicXTheme.colors.primaryAccent else MusicXTheme.colors.iconSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MusicXTheme.colors.primaryAccent,
                    uncheckedColor = MusicXTheme.colors.outline
                )
            )
        }
    }
}

@Composable
fun PlaylistSelectionDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist", color = MusicXTheme.colors.primaryText) },
        text = {
            if (playlists.isEmpty()) {
                Text("No playlists created yet.", color = MusicXTheme.colors.secondaryText)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        TextButton(
                            onClick = { onPlaylistSelected(playlist) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(playlist.name, color = MusicXTheme.colors.primaryText)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MusicXTheme.colors.secondaryText)
            }
        },
        containerColor = MusicXTheme.colors.modalBackground,
        modifier = Modifier.border(1.dp, MusicXTheme.colors.outline.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
    )
}

fun processSongDisplay(title: String, artist: String): Pair<String, String> {
    return Pair(title, artist)
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
