package com.example.musicx.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.navigation3.ui.NavDisplay
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import com.example.musicx.playback.MusicController
import com.example.musicx.ui.browse.AlbumsScreen
import com.example.musicx.ui.browse.ArtistsScreen
import com.example.musicx.ui.browse.FilteredSongsList
import com.example.musicx.ui.browse.GenresScreen
import com.example.musicx.ui.components.MiniPlayer
import com.example.musicx.ui.components.MusicXIcons
import com.example.musicx.ui.songs.SongsScreen
import com.example.musicx.ui.songs.SongsViewModel
import com.example.musicx.ui.playlists.PlaylistsScreen
import com.example.musicx.ui.playlists.PlaylistDetailScreen
import com.example.musicx.ui.import.ImportScreen
import com.example.musicx.ui.search.SearchScreen
import com.example.musicx.ui.settings.SettingsViewModel
import com.example.musicx.ui.settings.SettingsScreen
import com.example.musicx.ui.settings.AppearanceSettingsScreen
import com.example.musicx.ui.settings.AudioSettingsScreen
import com.example.musicx.ui.settings.PlaybackSettingsScreen
import com.example.musicx.ui.settings.LibrarySettingsScreen
import com.example.musicx.ui.settings.LyricsSettingsScreen
import com.example.musicx.ui.settings.VideoSettingsScreen
import com.example.musicx.ui.settings.CustomizeTabsScreen
import com.example.musicx.ui.nowplaying.NowPlayingScreen
import androidx.media3.common.MediaItem
import com.example.musicx.ui.theme.MusicXTheme
import com.example.musicx.ui.metadata.MetadataEditor

data class TabDef(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val destination: Destination,
    val isActive: (NavKey) -> Boolean
)

private val ALL_TABS = listOf(
    TabDef("Songs", "Songs", MusicXIcons.Songs, Destination.Songs, { it is Destination.Songs }),
    TabDef("Artists", "Artists", Icons.Rounded.Person, Destination.Artists, { it is Destination.Artists }),
    TabDef("Albums", "Albums", Icons.Rounded.Album, Destination.Albums, { it is Destination.Albums }),
    TabDef("Genres", "Genres", Icons.Rounded.MusicNote, Destination.Genres, { it is Destination.Genres }),
    TabDef("Playlists", "Playlists", MusicXIcons.Playlists, Destination.Playlists, { it is Destination.Playlists || it is Destination.PlaylistDetail }),
    TabDef("Import", "Import", MusicXIcons.Import, Destination.Import, { it is Destination.Import }),
    TabDef("Search", "Search", MusicXIcons.Search, Destination.Search, { it is Destination.Search }),
    TabDef("Settings", "Settings", MusicXIcons.Settings, Destination.Settings, { it is Destination.Settings })
)

@Composable
fun MusicXApp(
    songsViewModel: SongsViewModel,
    settingsViewModel: SettingsViewModel,
    musicController: MusicController
) {
    val backStack = rememberNavBackStack(Destination.Songs as NavKey)
    val currentDestination = backStack.last()
    
    val mediaController by musicController.mediaController
    
    val generalSettings by settingsViewModel.generalSettings.collectAsState()
    val songs by songsViewModel.songs.collectAsState()
    
    val isMobile = true

    fun popBackStack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }

    fun navigateTopLevel(destination: Destination) {
        if (currentDestination == destination) return
        backStack.clear()
        backStack.add(destination)
    }

    val playSong = remember(mediaController) {
        { song: com.example.musicx.model.Song ->
            val mediaItem = MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.mediaUri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkUri(song.albumArtUri)
                        .build()
                )
                .build()
            mediaController?.setMediaItem(mediaItem)
            mediaController?.prepare()
            mediaController?.play()
            Unit
        }
    }

    val visibleTabs = remember(generalSettings.visibleTabs) {
        ALL_TABS.filter { it.id in generalSettings.visibleTabs }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = isMobile && currentDestination !is Destination.NowPlaying && currentDestination !is Destination.ArtistSongs && currentDestination !is Destination.AlbumSongs && currentDestination !is Destination.GenreSongs,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Column {
                    Box(modifier = Modifier.background(MusicXTheme.colors.bottomBar)) {
                        MiniPlayer(
                            mediaController = mediaController,
                            onNavigateToNowPlaying = { backStack.add(Destination.NowPlaying) }
                        )
                    }
                    NavigationBar(
                        containerColor = MusicXTheme.colors.bottomBar,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        visibleTabs.forEach { tab ->
                            NavigationItem(
                                selected = tab.isActive(currentDestination),
                                onClick = { navigateTopLevel(tab.destination) },
                                icon = tab.icon,
                                label = tab.label
                            )
                        }
                    }
                }
            }
        },
        containerColor = MusicXTheme.colors.primaryBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                NavDisplay(
                    backStack = backStack,
                    modifier = Modifier.fillMaxSize(),
                    onBack = { popBackStack() },
                    entryProvider = { key ->
                        when (val destination = key as Destination) {
                            is Destination.Songs -> NavEntry(destination) {
                                SongsScreen(
                                    viewModel = songsViewModel,
                                    mediaController = mediaController,
                                    onSongClick = playSong,
                                    onSongLongClick = { song ->
                                        backStack.add(Destination.EditMetadata(song.id))
                                    },
                                    onEditMetadata = { songId ->
                                        backStack.add(Destination.EditMetadata(songId))
                                    }
                                )
                            }
                            is Destination.Artists -> NavEntry(destination) {
                                ArtistsScreen(
                                    songs = songs,
                                    onArtistClick = { artist ->
                                        backStack.add(Destination.ArtistSongs(artist))
                                    }
                                )
                            }
                            is Destination.Albums -> NavEntry(destination) {
                                AlbumsScreen(
                                    songs = songs,
                                    onAlbumClick = { album ->
                                        backStack.add(Destination.AlbumSongs(album))
                                    }
                                )
                            }
                            is Destination.Genres -> NavEntry(destination) {
                                GenresScreen(
                                    songs = songs,
                                    onGenreClick = { genre ->
                                        backStack.add(Destination.GenreSongs(genre))
                                    }
                                )
                            }
                            is Destination.ArtistSongs -> NavEntry(destination) {
                                FilteredSongsList(
                                    songs = songs,
                                    title = destination.artist,
                                    filterKey = { it.artist },
                                    filterValue = destination.artist,
                                    onBack = { popBackStack() },
                                    onSongClick = playSong
                                )
                            }
                            is Destination.AlbumSongs -> NavEntry(destination) {
                                FilteredSongsList(
                                    songs = songs,
                                    title = destination.album,
                                    filterKey = { it.album },
                                    filterValue = destination.album,
                                    onBack = { popBackStack() },
                                    onSongClick = playSong
                                )
                            }
                            is Destination.GenreSongs -> NavEntry(destination) {
                                FilteredSongsList(
                                    songs = songs,
                                    title = destination.genre,
                                    filterKey = { it.genre },
                                    filterValue = destination.genre,
                                    onBack = { popBackStack() },
                                    onSongClick = playSong
                                )
                            }
                            is Destination.Playlists -> NavEntry(destination) { 
                                PlaylistsScreen(
                                    viewModel = songsViewModel,
                                    onPlaylistClick = { playlistId ->
                                        backStack.add(Destination.PlaylistDetail(playlistId))
                                    }
                                ) 
                            }
                            is Destination.PlaylistDetail -> NavEntry(destination) {
                                PlaylistDetailScreen(
                                    playlistId = destination.playlistId,
                                    viewModel = songsViewModel,
                                    onSongClick = playSong,
                                    onBack = { popBackStack() }
                                )
                            }
                            is Destination.Import -> NavEntry(destination) { ImportScreen(songsViewModel) }
                            is Destination.Search -> NavEntry(destination) { 
                                SearchScreen(
                                    viewModel = songsViewModel,
                                    onSongClick = playSong
                                )
                            }
                            is Destination.Settings -> NavEntry(destination) { 
                                SettingsScreen(
                                    onNavigate = { backStack.add(it) },
                                    generalSettings = generalSettings,
                                    onUpdateGeneralSettings = { update -> settingsViewModel.updateGeneralSettings(update) }
                                ) 
                            }
                            is Destination.AppearanceSettings -> NavEntry(destination) {
                                AppearanceSettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = { popBackStack() }
                                )
                            }
                            is Destination.AudioSettings -> NavEntry(destination) {
                                AudioSettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = { popBackStack() }
                                )
                            }
                            is Destination.PlaybackSettings -> NavEntry(destination) {
                                PlaybackSettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = { popBackStack() }
                                )
                            }
                            is Destination.LibrarySettings -> NavEntry(destination) {
                                LibrarySettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = { popBackStack() },
                                    onRescan = { songsViewModel.loadSongs(forceRefresh = true) }
                                )
                            }
                            is Destination.LyricsSettings -> NavEntry(destination) {
                                LyricsSettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = { popBackStack() }
                                )
                            }
                            is Destination.VideoSettings -> NavEntry(destination) {
                                VideoSettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = { popBackStack() }
                                )
                            }
                            is Destination.CustomizeTabs -> NavEntry(destination) {
                                CustomizeTabsScreen(
                                    generalSettings = generalSettings,
                                    onSave = { visibleTabs ->
                                        settingsViewModel.updateGeneralSettings { it.copy(visibleTabs = visibleTabs) }
                                    },
                                    onBack = { popBackStack() }
                                )
                            }
                            is Destination.NowPlaying -> NavEntry(destination) {
                                val currentMediaId = mediaController?.currentMediaItem?.mediaId
                                val currentSong = remember(songs, currentMediaId) {
                                    currentMediaId?.let { id -> songs.find { it.id.toString() == id } }
                                }
                                NowPlayingScreen(
                                    viewModel = songsViewModel,
                                    mediaController = mediaController,
                                    currentSong = currentSong,
                                    generalSettings = generalSettings,
                                    onBack = { popBackStack() }
                                )
                            }
                            is Destination.EditMetadata -> NavEntry(destination) {
                                val song = remember(songs, destination.songId) {
                                    songs.find { it.id == destination.songId }
                                }
                                if (song != null) {
                                    MetadataEditor(
                                        song = song,
                                        onSave = { title, artist, lyrics ->
                                            songsViewModel.updateMetadata(song.mediaUri.toString(), title, artist, lyrics)
                                            popBackStack()
                                        },
                                        onBack = { popBackStack() }
                                    )
                                }
                            }
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MusicXTheme.colors.bottomBar)
                        )
                    )
            )
        }
    }
}

@Composable
fun RowScope.NavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { 
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                softWrap = false,
                fontSize = 11.sp,
                color = if (selected) MusicXTheme.colors.navActive else MusicXTheme.colors.navInactive
            ) 
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MusicXTheme.colors.navActive,
            unselectedIconColor = MusicXTheme.colors.navInactive,
            selectedTextColor = MusicXTheme.colors.navActive,
            unselectedTextColor = MusicXTheme.colors.navInactive,
            indicatorColor = MusicXTheme.colors.bottomBar.copy(alpha = 0.5f)
        )
    )
}
