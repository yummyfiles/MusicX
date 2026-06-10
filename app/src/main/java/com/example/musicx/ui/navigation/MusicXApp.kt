package com.example.musicx.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.example.musicx.playback.MusicController
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
import com.example.musicx.ui.nowplaying.NowPlayingScreen
import androidx.media3.common.MediaItem
import com.example.musicx.ui.theme.MusicXTheme

import com.example.musicx.ui.metadata.MetadataEditor

@Composable
fun MusicXApp(
    songsViewModel: SongsViewModel,
    settingsViewModel: SettingsViewModel,
    musicController: MusicController
) {
    val backStack = rememberNavBackStack(Destination.Songs as NavKey)
    val currentDestination = backStack.last()
    
    val mediaController by musicController.mediaController
    
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

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = isMobile && currentDestination !is Destination.NowPlaying,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Box {
                    // Refined Fade Gradient: Solid at the bottom, fades out upwards
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp) // Height of the fade effect
                            .offset(y = (-48).dp) // Position it directly above the MiniPlayer
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        MusicXTheme.colors.primaryBackground
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .background(MusicXTheme.colors.bottomBar)
                    ) {
                        MiniPlayer(
                            mediaController = mediaController,
                            onNavigateToNowPlaying = { backStack.add(Destination.NowPlaying) }
                        )
                        NavigationBar(
                            containerColor = MusicXTheme.colors.bottomBar,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets.navigationBars // Fixed: Respect system nav bar
                        ) {
                            NavigationItem(
                                selected = currentDestination is Destination.Songs,
                                onClick = { 
                                    navigateTopLevel(Destination.Songs)
                                },
                                icon = MusicXIcons.Songs,
                                label = "Songs"
                            )
                            NavigationItem(
                                selected = currentDestination is Destination.Playlists || currentDestination is Destination.PlaylistDetail,
                                onClick = { 
                                    navigateTopLevel(Destination.Playlists)
                                },
                                icon = MusicXIcons.Playlists,
                                label = "Playlists"
                            )
                            NavigationItem(
                                selected = currentDestination is Destination.Import,
                                onClick = { 
                                    navigateTopLevel(Destination.Import)
                                },
                                icon = MusicXIcons.Import,
                                label = "Import"
                            )
                            NavigationItem(
                                selected = currentDestination is Destination.Search,
                                onClick = { 
                                    navigateTopLevel(Destination.Search)
                                },
                                icon = MusicXIcons.Search,
                                label = "Search"
                            )
                            NavigationItem(
                                selected = currentDestination is Destination.Settings,
                                onClick = { 
                                    navigateTopLevel(Destination.Settings)
                                },
                                icon = MusicXIcons.Settings,
                                label = "Settings"
                            )
                        }
                    }
                }
            }
        },
        containerColor = MusicXTheme.colors.primaryBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // Fixed: Remove extra Scaffold insets
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()) // Only bottom padding from Scaffold
                .statusBarsPadding() // Fixed: Manual status bar padding
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
                                onBack = { popBackStack() }
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
                fontSize = 11.sp, // Slightly smaller to prevent wrapping
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
