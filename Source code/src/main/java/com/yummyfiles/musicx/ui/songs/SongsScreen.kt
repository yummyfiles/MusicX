package com.yummyfiles.musicx.ui.songs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yummyfiles.musicx.model.Playlist
import com.yummyfiles.musicx.model.Song
import com.yummyfiles.musicx.ui.theme.MusicXTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SongsScreen(
    viewModel: SongsViewModel,
    mediaController: androidx.media3.session.MediaController? = null,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onEditMetadata: (Long) -> Unit,
) {
    val songs by viewModel.songs.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedUris by viewModel.selectedSongUris.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val pendingDeleteIntent by viewModel.pendingDeleteIntent.collectAsState()

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onDeletionConfirmed()
        } else {
            viewModel.onDeletionCancelled()
        }
    }

    LaunchedEffect(pendingDeleteIntent) {
        pendingDeleteIntent?.let {
            deleteLauncher.launch(IntentSenderRequest.Builder(it).build())
        }
    }
    
    // Keeping tabs on what's currently bumping.
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

    var showMenu by remember { mutableStateOf(value = false) }
    var showPlaylistDialog by remember { mutableStateOf(value = false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isSelectionMode) "${selectedUris.size} Selected" else "Songs", 
                        fontSize = if (isSelectionMode) 24.sp else 32.sp, 
                        fontWeight = FontWeight.Bold,
                        color = MusicXTheme.colors.primaryText,
                    )
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(
                            onClick = {
                                viewModel.toggleSelectionMode()
                            },
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Cancel",
                                tint = MusicXTheme.colors.iconPrimary,
                            )
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
                            IconButton(
                                onClick = {
                                    showPlaylistDialog = true
                                },
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "Add to Playlist", tint = MusicXTheme.colors.iconPrimary)
                            }
                            IconButton(
                                onClick = {
                                    viewModel.deleteSelectedSongs()
                                },
                            ) {
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
                windowInsets = WindowInsets(0, 0, 0, 0)
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
                // Putting your favorites right where you can see 'em, at the top.
                if (favoriteSongs.isNotEmpty()) {
                    item {
                        FavoritesSection(
                            favoriteSongs = favoriteSongs,
                            onSongClick = onSongClick,
                            viewModel = viewModel,
                            currentMediaId = currentMediaId,
                            isSelectionMode = isSelectionMode,
                            selectedUris = selectedUris,
                            onSongLongClick = onSongLongClick
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                items(
                    items = songs,
                    key = { it.mediaUri.toString() },
                    contentType = { "song" }
                ) { song ->
                    val isSelected = selectedUris.contains(song.mediaUri.toString())
                    val isCurrentlyPlaying = currentMediaId == song.id.toString()
                    val isFavorite = favoriteSongs.any { it.id == song.id }
                    
                    val onClick = remember(song.mediaUri, isSelectionMode, onSongClick) {
                        {
                            if (isSelectionMode) {
                                viewModel.toggleSongSelection(song.mediaUri.toString())
                            } else {
                                onSongClick(song)
                            }
                        }
                    }
                    val onLongClick = remember(song.mediaUri, isSelectionMode, onSongLongClick) {
                        {
                            if (!isSelectionMode) {
                                viewModel.toggleSelectionMode()
                                viewModel.toggleSongSelection(song.mediaUri.toString())
                            } else {
                                onSongLongClick(song)
                            }
                        }
                    }
                    
                    SongItem(
                        song = song, 
                        onClick = onClick,
                        onLongClick = onLongClick,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        isCurrentlyPlaying = isCurrentlyPlaying,
                        isFavorite = isFavorite,
                        onFavoriteClick = { viewModel.toggleFavorite(song.id, !isFavorite) }
                    )
                }
            }
        }
    }

    if (showPlaylistDialog) {
        PlaylistSelectionDialog(
            playlists = playlists,
            onDismiss = { showPlaylistDialog = false }
        ) { playlist ->
            viewModel.addSongsToPlaylist(playlist, selectedUris.toList())
            viewModel.toggleSelectionMode()
            showPlaylistDialog = false
        }
    }
}

@Composable
fun FavoritesSection(
    favoriteSongs: List<Song>,
    onSongClick: (Song) -> Unit,
    viewModel: SongsViewModel,
    currentMediaId: String?,
    isSelectionMode: Boolean,
    selectedUris: Set<String>,
    onSongLongClick: (Song) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // The title for this part.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Favorites",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MusicXTheme.colors.primaryText
                )
            )
            Text(
                text = "${favoriteSongs.size} songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MusicXTheme.colors.secondaryText
            )
        }
        
        // A row of favorites you can swipe through.
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(favoriteSongs, key = { it.id }) { song ->
                val isCurrentlyPlaying = currentMediaId == song.id.toString()
                val isSelected = selectedUris.contains(song.mediaUri.toString())
                
                val onClick = remember(song.mediaUri, isSelectionMode, onSongClick) {
                    {
                        if (isSelectionMode) {
                            viewModel.toggleSongSelection(song.mediaUri.toString())
                        } else {
                            onSongClick(song)
                        }
                    }
                }
                val onLongClick = remember(song.mediaUri, isSelectionMode, onSongLongClick) {
                    {
                        if (!isSelectionMode) {
                            viewModel.toggleSelectionMode()
                            viewModel.toggleSongSelection(song.mediaUri.toString())
                        } else {
                            onSongLongClick(song)
                        }
                    }
                }
                
                FavoriteSongCard(
                    song = song,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected,
                    isCurrentlyPlaying = isCurrentlyPlaying,
                    onFavoriteClick = { viewModel.toggleFavorite(song.id, false) }
                )
            }
        }
    }
}

@Composable
fun FavoriteSongCard(
    song: Song,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isCurrentlyPlaying: Boolean = false,
    onFavoriteClick: () -> Unit
) {
    val (displayTitle, displayArtist) = remember(song.title, song.artist) {
        processSongDisplay(song.title, song.artist)
    }
    
    val bgTint by animateColorAsState(
        targetValue = if (isSelected) MusicXTheme.colors.surfaceVariant else MusicXTheme.colors.cardBackground,
        animationSpec = tween(200), label = "FavoriteSongCardBg"
    )

    Surface(
        modifier = Modifier
            .width(240.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = bgTint,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isCurrentlyPlaying) 2.dp else 1.dp,
            color = if (isCurrentlyPlaying) MusicXTheme.colors.primaryAccent else MusicXTheme.colors.outline.copy(alpha = 0.1f)
        )
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // The left side has the album cover or a placeholder.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(MusicXTheme.colors.mutedAccent.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MusicXTheme.colors.iconPrimary
                    )
                }
            }

            // The right side is for the song info.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayArtist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MusicXTheme.colors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // The heart button.
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = "Remove from favorites",
                        tint = MusicXTheme.colors.primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-8).dp)
                            .scale(0.8f),
                        colors = CheckboxDefaults.colors(
                            checkedColor = MusicXTheme.colors.primaryAccent
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SongItem(
    song: Song, 
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isCurrentlyPlaying: Boolean = false,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit
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
                    model = song.albumArtUri,
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

        // Favorite button
        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) MusicXTheme.colors.primaryAccent else MusicXTheme.colors.iconSecondary
            )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        Surface(
                            onClick = { onPlaylistSelected(playlist) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MusicXTheme.colors.cardBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = MusicXTheme.colors.outline.copy(alpha = 0.15f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    contentDescription = null,
                                    tint = MusicXTheme.colors.primaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = playlist.name,
                                    color = MusicXTheme.colors.primaryText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
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

private val featRegex = Regex("(?i)(?:ft\\.|feat\\.|featuring)\\s*(.*)")
private val officialTagRegex = Regex("(?i)(?:\\(official[^)]*\\)|\\[official[^]]*])")
private val charsToRemove = setOf('|', '\\', '(', ')', '"', '[', ']')

fun processSongDisplay(title: String, artist: String): Pair<String, String> {
    var cleanTitle = title
    var cleanArtist = artist

    val featMatch = featRegex.find(cleanTitle)
    if (featMatch != null) {
        cleanTitle = cleanTitle.substring(0, featMatch.range.first).trim()
        val feature = "ft. ${featMatch.groupValues[1].trim()}"
        cleanArtist = "$cleanArtist $feature"
    }

    cleanTitle = cleanTitle.filter { it !in charsToRemove }.trim()
    cleanTitle = cleanTitle.replace(officialTagRegex, "").trim()

    return Pair(cleanTitle, cleanArtist)
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
