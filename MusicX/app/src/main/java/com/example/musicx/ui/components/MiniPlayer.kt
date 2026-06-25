package com.example.musicx.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicx.ui.theme.MusicXTheme
import kotlinx.coroutines.delay

// the little player bar at the bottom - shows whats playing
// tbh this was way harder to make than it looks
@Composable
fun MiniPlayer(
    mediaController: MediaController?,
    modifier: Modifier = Modifier,
    onNavigateToNowPlaying: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(mediaController?.isPlaying ?: false) }
    var songTitle by remember { mutableStateOf(mediaController?.currentMediaItem?.mediaMetadata?.title?.toString() ?: "No Song Playing") }
    var artistName by remember { mutableStateOf(mediaController?.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Tap to select a song") }
    var albumArtUri by remember { mutableStateOf(mediaController?.currentMediaItem?.mediaMetadata?.artworkUri) }
    
    // progress bar stuff - need to track how far into the song we are
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "SmoothProgress"
    )
    
    // gentle polling so the progress bar actually moves during playback
    LaunchedEffect(isPlaying) {
        if (isPlaying && mediaController != null) {
            while (true) {
                delay(1000)
                val dur = mediaController.duration.coerceAtLeast(0L).toFloat()
                if (dur > 0) progress = mediaController.currentPosition.toFloat() / dur
            }
        }
    }

    // sync the initial state when song changes
    LaunchedEffect(mediaController, mediaController?.currentMediaItem?.mediaId) {
        if (mediaController != null) {
            isPlaying = mediaController.isPlaying
            val metadata = mediaController.currentMediaItem?.mediaMetadata
            songTitle = metadata?.title?.toString() ?: "No Song Playing"
            artistName = metadata?.artist?.toString() ?: "Tap to select a song"
            albumArtUri = metadata?.artworkUri
            
            val dur = mediaController.duration.coerceAtLeast(0L).toFloat()
            if (dur > 0) progress = mediaController.currentPosition.toFloat() / dur
        }
    }

    DisposableEffect(mediaController) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onMediaMetadataChanged(metadata: androidx.media3.common.MediaMetadata) {
                songTitle = metadata.title?.toString() ?: "No Song Playing"
                artistName = metadata.artist?.toString() ?: "Tap to select a song"
                albumArtUri = metadata.artworkUri
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                val metadata = mediaItem?.mediaMetadata
                songTitle = metadata?.title?.toString() ?: "No Song Playing"
                artistName = metadata?.artist?.toString() ?: "Tap to select a song"
                albumArtUri = metadata?.artworkUri
                progress = 0f
            }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                val dur = (mediaController?.duration ?: 0L).coerceAtLeast(0L).toFloat()
                if (dur > 0) progress = newPosition.positionMs.toFloat() / dur
            }
        }
        mediaController?.addListener(listener)
        onDispose {
            mediaController?.removeListener(listener)
        }
    }

    val accentColor = MusicXTheme.colors.primaryAccent

    Surface(
        onClick = onNavigateToNowPlaying,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer {
                if (isPlaying) {
                    shadowElevation = 8f
                }
            },
        shape = RoundedCornerShape(16.dp),
        color = MusicXTheme.colors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isPlaying) accentColor.copy(alpha = 0.12f)
                    else MusicXTheme.colors.outline.copy(alpha = 0.2f)
        ),
        tonalElevation = 4.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MusicXTheme.colors.albumPlaceholder),
                    contentAlignment = Alignment.Center
                ) {
                    if (albumArtUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(albumArtUri)
                                .size(96, 96)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MusicXTheme.colors.iconPrimary)
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = songTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MusicXTheme.colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MusicXTheme.colors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { 
                        if (isPlaying) mediaController?.pause() else mediaController?.play()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isPlaying) MusicXTheme.colors.pauseButton else MusicXTheme.colors.playButton,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            // Minimal progress bar at the bottom
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MusicXTheme.colors.primaryAccent,
                trackColor = Color.Transparent
            )
        }
    }
}
