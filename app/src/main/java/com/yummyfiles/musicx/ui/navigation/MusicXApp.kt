package com.yummyfiles.musicx.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.navigation3.ui.NavDisplay
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.yummyfiles.musicx.playback.MusicController
import com.yummyfiles.musicx.ui.components.MiniPlayer
import com.yummyfiles.musicx.ui.components.MusicXIcons
import com.yummyfiles.musicx.ui.songs.SongsScreen
import com.yummyfiles.musicx.ui.songs.SongsViewModel
import com.yummyfiles.musicx.ui.playlists.PlaylistsScreen
import com.yummyfiles.musicx.ui.playlists.PlaylistDetailScreen
import com.yummyfiles.musicx.ui.import.ImportScreen
import com.yummyfiles.musicx.ui.search.SearchScreen
import com.yummyfiles.musicx.ui.settings.SettingsViewModel
import com.yummyfiles.musicx.ui.settings.SettingsScreen
import com.yummyfiles.musicx.ui.settings.AboutScreen
import com.yummyfiles.musicx.ui.nowplaying.NowPlayingScreen
import androidx.media3.common.MediaItem
import com.yummyfiles.musicx.ui.theme.MusicXTheme
import com.yummyfiles.musicx.ui.metadata.MetadataEditor
import com.yummyfiles.musicx.ui.splash.SplashScreen

@Composable
fun MusicXApp(
    songsViewModel: SongsViewModel,
    settingsViewModel: SettingsViewModel,
    musicController: MusicController?,
    onRequestPermissions: () -> Unit,
) {
    // Start permission check and data loading in parallel with splash
    LaunchedEffect(Unit) {
        onRequestPermissions()
    }
    val backStack = rememberNavBackStack(Destination.Songs as NavKey)
    val currentDestination = backStack.last()
    val mediaController = musicController?.mediaController?.value
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

    val playSong = remember(mediaController, songsViewModel) {
        { song: com.yummyfiles.musicx.model.Song ->
            val songs = songsViewModel.songs.value
            val mediaItems = songs.map { s ->
                MediaItem.Builder()
                    .setMediaId(s.id.toString())
                    .setUri(s.mediaUri)
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(s.title)
                            .setArtist(s.artist)
                            .setArtworkUri(s.albumArtUri)
                            .build(),
                    )
                    .build()
            }
            val startIndex = songs.indexOf(song).coerceAtLeast(0)
            mediaController?.setMediaItems(mediaItems, startIndex, 0L)
            mediaController?.prepare()
            mediaController?.play()
            Unit
        }
    }

    Scaffold(
        bottomBar = {
            val showBottomBar = isMobile && currentDestination !is Destination.NowPlaying
            
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                Column(modifier = Modifier.background(MusicXTheme.colors.bottomBar)) {
                    MiniPlayer(
                        mediaController = mediaController,
                        onNavigateToNowPlaying = { backStack.add(Destination.NowPlaying) }
                    )
                    NavigationBar(
                        containerColor = MusicXTheme.colors.bottomBar,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavigationItem(
                            selected = currentDestination is Destination.Songs,
                            onClick = { navigateTopLevel(Destination.Songs) },
                            icon = MusicXIcons.Songs,
                            label = "Songs"
                        )
                        NavigationItem(
                            selected = currentDestination is Destination.Playlists || currentDestination is Destination.PlaylistDetail,
                            onClick = { navigateTopLevel(Destination.Playlists) },
                            icon = MusicXIcons.Playlists,
                            label = "Playlists"
                        )
                        NavigationItem(
                            selected = currentDestination is Destination.Import,
                            onClick = { navigateTopLevel(Destination.Import) },
                            icon = MusicXIcons.Import,
                            label = "Import"
                        )
                        NavigationItem(
                            selected = currentDestination is Destination.Search,
                            onClick = { navigateTopLevel(Destination.Search) },
                            icon = MusicXIcons.Search,
                            label = "Search"
                        )
                        NavigationItem(
                            selected = currentDestination is Destination.Settings,
                            onClick = { navigateTopLevel(Destination.Settings) },
                            icon = MusicXIcons.Settings,
                            label = "Settings"
                        )
                    }
                }
            }
        },
        containerColor = MusicXTheme.colors.primaryBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
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
                            SettingsScreen(onNavigate = { backStack.add(it) }) 
                        }
                        is Destination.About -> NavEntry(destination) {
                            AboutScreen(onBack = { popBackStack() })
                        }
                        is Destination.NowPlaying -> NavEntry(destination) {
                            val songs by songsViewModel.songs.collectAsState()
                            val generalSettings by settingsViewModel.generalSettings.collectAsState()
                            NowPlayingScreen(
                                viewModel = songsViewModel,
                                mediaController = mediaController,
                                songs = songs,
                                generalSettings = generalSettings,
                                onBack = { popBackStack() }
                            )
                        }
                        is Destination.EditMetadata -> NavEntry(destination) {
                            val songs by songsViewModel.songs.collectAsState()
                            val song = songs.find { it.id == destination.songId }
                            song?.let {
                                MetadataEditor(
                                    song = it,
                                    onSave = { title, artist, lyrics ->
                                        songsViewModel.updateMetadata(it.mediaUri.toString(), title, artist, lyrics)
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
        alwaysShowLabel = true, // need to see the labels
        icon = { Icon(icon, contentDescription = label) },
        label = { 
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                softWrap = false,
                fontSize = 10.sp, // small text to fit
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
