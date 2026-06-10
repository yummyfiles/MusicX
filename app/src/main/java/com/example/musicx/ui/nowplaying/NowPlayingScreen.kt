package com.example.musicx.ui.nowplaying

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.example.musicx.data.GeneralSettings
import com.example.musicx.ui.theme.MusicXTheme
import com.example.musicx.ui.songs.SongsViewModel
import com.example.musicx.ui.songs.processSongDisplay
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow

data class LyricsLine(val time: Long, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: SongsViewModel,
    mediaController: MediaController?,
    songs: List<com.example.musicx.model.Song>,
    generalSettings: GeneralSettings = GeneralSettings(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(mediaController?.isPlaying ?: false) }
    var currentPosition by remember { mutableLongStateOf(mediaController?.currentPosition?.coerceAtLeast(0L) ?: 0L) }
    var duration by remember { mutableLongStateOf(mediaController?.duration?.coerceAtLeast(0L) ?: 0L) }
    var songTitle by remember { mutableStateOf(mediaController?.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Unknown Title") }
    var artistName by remember { mutableStateOf(mediaController?.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown Artist") }
    var albumArtUri by remember { mutableStateOf(mediaController?.currentMediaItem?.mediaMetadata?.artworkUri) }
    var repeatMode by remember { mutableIntStateOf(mediaController?.repeatMode ?: Player.REPEAT_MODE_OFF) }
    var shuffleEnabled by remember { mutableStateOf(mediaController?.shuffleModeEnabled ?: false) }

    val currentMediaId = mediaController?.currentMediaItem?.mediaId
    val songsById = remember(songs) {
        songs.associateBy { it.id.toString() }
    }
    val song = remember(currentMediaId, songsById) {
        currentMediaId?.let { songsById[it] }
    }
    val lyricsText = song?.lyrics

    val syncedLyrics = remember(lyricsText) {
        lyricsText?.let { parseLrc(it) } ?: emptyList()
    }
    val hasSyncedLyrics = syncedLyrics.isNotEmpty()
    val showLyricsInPlayer = generalSettings.showLyricsInPlayer

    // Compute activeIndex here so SyncedLyricsView only recomposes when line changes, not every 250ms
    val activeLyricsIndex = remember {
        derivedStateOf {
            val predictionOffset = 150L
            syncedLyrics.indexOfLast { it.time <= currentPosition + predictionOffset }.coerceAtLeast(0)
        }
    }

    var showLyrics by remember { mutableStateOf(false) }

    LaunchedEffect(currentMediaId, showLyricsInPlayer) {
        showLyrics = hasSyncedLyrics && showLyricsInPlayer
    }

    LaunchedEffect(currentMediaId) {
        if (song != null && lyricsText == null) {
            viewModel.autoFetchLyrics(song)
        }
    }

    LaunchedEffect(mediaController, mediaController?.currentMediaItem) {
        if (mediaController != null) {
            isPlaying = mediaController.isPlaying
            currentPosition = mediaController.currentPosition.coerceAtLeast(0L)
            duration = mediaController.duration.coerceAtLeast(0L)
            val metadata = mediaController.currentMediaItem?.mediaMetadata
            songTitle = metadata?.title?.toString() ?: "Unknown Title"
            artistName = metadata?.artist?.toString() ?: "Unknown Artist"
            albumArtUri = metadata?.artworkUri
            repeatMode = mediaController.repeatMode
            shuffleEnabled = mediaController.shuffleModeEnabled
        }
    }

    // Smooth progress polling during playback
    LaunchedEffect(isPlaying) {
        if (isPlaying && mediaController != null) {
            while (true) {
                delay(500)
                currentPosition = mediaController.currentPosition.coerceAtLeast(0L)
                duration = mediaController.duration.coerceAtLeast(0L)
            }
        }
    }

    DisposableEffect(mediaController) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onMediaMetadataChanged(metadata: androidx.media3.common.MediaMetadata) {
                songTitle = metadata.title?.toString() ?: "Unknown Title"
                artistName = metadata.artist?.toString() ?: "Unknown Artist"
                albumArtUri = metadata.artworkUri
                duration = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                currentPosition = (mediaController?.currentPosition ?: 0L).coerceAtLeast(0L)
                duration = (mediaController?.duration ?: 0L).coerceAtLeast(0L)
            }
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                currentPosition = newPosition.positionMs.coerceAtLeast(0L)
                duration = (mediaController?.duration ?: 0L).coerceAtLeast(0L)
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.widget.Toast.makeText(context, "Playback error: ${error.message}", android.widget.Toast.LENGTH_LONG).show()
            }
            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }
            override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                shuffleEnabled = enabled
            }
        }
        mediaController?.addListener(listener)
        onDispose {
            mediaController?.removeListener(listener)
        }
    }

    val progressFraction by remember {
        derivedStateOf {
            if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
        }
    }
    val formattedPosition by remember {
        derivedStateOf { formatTime(currentPosition) }
    }
    val formattedDuration by remember {
        derivedStateOf { formatTime(duration) }
    }

    Scaffold(
        containerColor = MusicXTheme.colors.primaryBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(MusicXTheme.colors.topBar)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Minimize", tint = MusicXTheme.colors.iconPrimary)
                }

                Text(
                    text = "Now Playing",
                    color = MusicXTheme.colors.primaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = com.example.musicx.ui.theme.ShareTechMono
                )

                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        if (showLyrics) Icons.Rounded.Image else Icons.AutoMirrored.Rounded.Notes,
                        contentDescription = "Lyrics",
                        tint = if (showLyrics) MusicXTheme.colors.primaryAccent else MusicXTheme.colors.iconPrimary
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Album Art Area / Lyrics Area with crossfade transition
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = showLyrics,
                    animationSpec = tween(350),
                    label = "LyricsCrossfade"
                ) { showingLyrics ->
                    if (showingLyrics) {
                        SyncedLyricsView(
                            lines = syncedLyrics,
                            plainLyrics = lyricsText,
                            activeIndex = activeLyricsIndex.value,
                            onLineClick = { time -> mediaController?.seekTo(time) },
                            enableSync = generalSettings.syncLyrics,
                            biggerText = generalSettings.biggerLyrics,
                            centerLyrics = generalSettings.centerLyrics
                        )
                    } else {
                        val pulseAlpha by animateFloatAsState(
                            targetValue = if (isPlaying) 0.12f else 0.05f,
                            animationSpec = if (isPlaying) infiniteRepeatable(
                                animation = tween(3000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ) else tween(500),
                            label = "AlbumGlow"
                        )
                        val pulseScale by animateFloatAsState(
                            targetValue = if (isPlaying) 1.05f else 1f,
                            animationSpec = if (isPlaying) infiniteRepeatable(
                                animation = tween(3000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ) else tween(500),
                            label = "AlbumGlowScale"
                        )

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .fillMaxHeight(0.9f)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                    alpha = pulseAlpha
                                }
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(MusicXTheme.colors.primaryAccent, Color.Transparent)
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .fillMaxHeight(0.9f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MusicXTheme.colors.albumPlaceholder)
                                .border(
                                    width = 4.dp,
                                    color = MusicXTheme.colors.outline.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (albumArtUri != null) {
                                AsyncImage(
                                    model = albumArtUri,
                                    contentDescription = "Album Art",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(140.dp),
                                    tint = MusicXTheme.colors.iconSecondary.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // Information & Controls Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val displayInfo = remember(songTitle, artistName) {
                    processSongDisplay(songTitle, artistName)
                }
                val displayTitle = displayInfo.first
                val displayArtist = displayInfo.second

                Spacer(modifier = Modifier.height(28.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    MarqueeText(
                        text = displayTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = MusicXTheme.colors.primaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        )
                    )
                    Text(
                        text = displayArtist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MusicXTheme.colors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Bar (isolated in child composable to limit recomposition scope)
                SeekBarSection(
                    progressFraction = progressFraction,
                    formattedPosition = formattedPosition,
                    formattedDuration = formattedDuration,
                    mediaController = mediaController,
                    duration = duration,
                    onSeek = { pos -> currentPosition = pos }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Controls
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            mediaController?.shuffleModeEnabled = !shuffleEnabled
                        }) {
                            Icon(
                                Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (shuffleEnabled) MusicXTheme.colors.primaryAccent else MusicXTheme.colors.iconPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        IconButton(onClick = {
                            val nextMode = when (repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                                else -> Player.REPEAT_MODE_OFF
                            }
                            mediaController?.repeatMode = nextMode
                        }) {
                            Icon(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                                    else -> Icons.Rounded.Repeat
                                },
                                contentDescription = "Repeat",
                                tint = if (repeatMode != Player.REPEAT_MODE_OFF) MusicXTheme.colors.primaryAccent else MusicXTheme.colors.iconPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { mediaController?.seekToPrevious() }) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp), tint = MusicXTheme.colors.previousButton)
                        }

                        IconButton(onClick = { mediaController?.seekBack() }) {
                            Icon(Icons.Rounded.Replay10, contentDescription = "Rewind", modifier = Modifier.size(32.dp), tint = MusicXTheme.colors.iconSecondary)
                        }

                        IconButton(
                            onClick = { if (isPlaying) mediaController?.pause() else mediaController?.play() },
                            modifier = Modifier.size(80.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(64.dp),
                                tint = if (isPlaying) MusicXTheme.colors.pauseButton else MusicXTheme.colors.playButton
                            )
                        }

                        IconButton(onClick = { mediaController?.seekForward() }) {
                            Icon(Icons.Rounded.Forward10, contentDescription = "Fast Forward", modifier = Modifier.size(32.dp), tint = MusicXTheme.colors.iconSecondary)
                        }

                        IconButton(onClick = { mediaController?.seekToNext() }) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp), tint = MusicXTheme.colors.nextButton)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyncedLyricsView(
    lines: List<LyricsLine>,
    plainLyrics: String?,
    activeIndex: Int,
    onLineClick: (Long) -> Unit,
    enableSync: Boolean = true,
    biggerText: Boolean = false,
    centerLyrics: Boolean = true
) {
    if (lines.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MusicXTheme.colors.surfaceVariant.copy(alpha = 0.5f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = plainLyrics ?: "Looking for lyrics...",
                style = MaterialTheme.typography.bodyLarge,
                color = MusicXTheme.colors.primaryText,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var listHeightPx by remember { mutableIntStateOf(0) }

    // Scroll to center the active line in the container
    LaunchedEffect(activeIndex, lines) {
        if (lines.isNotEmpty() && enableSync) {
            // Wait for LazyColumn to finish its first layout
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.isNotEmpty() }
                .first { it }
            // Now layout is done — listHeightPx is set and items are measured
            val lineHeightPx = with(density) {
                val fontSize = if (biggerText) 22.sp else 18.sp
                fontSize.toPx() * 1.5f + 24.dp.toPx()
            }.toInt()
            val contentPaddingPx = with(density) { 80.dp.toPx() * 2 }
            val visibleHeightPx = (listHeightPx - contentPaddingPx).coerceAtLeast(0f)
            val targetOffset = ((visibleHeightPx - lineHeightPx) / 2).coerceAtLeast(0f)
            listState.scrollToItem(activeIndex, scrollOffset = targetOffset.toInt())
        }
    }

    val lyricsFontSize = if (biggerText) 22.sp else 18.sp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(MusicXTheme.colors.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 16.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { listHeightPx = it.height },
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 80.dp)
        ) {
            itemsIndexed(lines, key = { index, line -> line.time }) { index, line ->
                val isActive = index == activeIndex
                val color by animateColorAsState(
                    targetValue = if (isActive) MusicXTheme.colors.lyricsActive else MusicXTheme.colors.lyricsInactive,
                    animationSpec = tween(200)
                )
                val scale by animateFloatAsState(
                    targetValue = if (isActive && enableSync) 1.15f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
                val alpha by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0.4f,
                    animationSpec = tween(200)
                )

                Text(
                    text = line.text,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = if (isActive && enableSync) FontWeight.Bold else FontWeight.Normal,
                        fontSize = lyricsFontSize
                    ),
                    color = color,
                    textAlign = if (centerLyrics) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .clickable { onLineClick(line.time) }
                )
            }
        }
    }
}

fun parseLrc(lrc: String): List<LyricsLine> {
    val lines = mutableListOf<LyricsLine>()
    val regex = Regex("\\[(\\d+):(\\d+)\\.(\\d+)](.*)")
    val regexAlt = Regex("\\[(\\d+):(\\d+)](.*)")

    lrc.lines().forEach { line ->
        val match = regex.find(line)
        if (match != null) {
            val min = match.groupValues[1].toLong()
            val sec = match.groupValues[2].toLong()
            val msPart = match.groupValues[3]
            val ms = when (msPart.length) {
                1 -> msPart.toLong() * 100
                2 -> msPart.toLong() * 10
                else -> msPart.take(3).toLong()
            }
            val time = (min * 60 * 1000) + (sec * 1000) + ms
            val text = match.groupValues[4].trim()
            if (text.isNotBlank()) {
                lines.add(LyricsLine(time, text))
            }
        } else {
            val matchAlt = regexAlt.find(line)
            if (matchAlt != null) {
                val min = matchAlt.groupValues[1].toLong()
                val sec = matchAlt.groupValues[2].toLong()
                val time = (min * 60 * 1000) + (sec * 1000)
                val text = matchAlt.groupValues[3].trim()
                if (text.isNotBlank()) {
                    lines.add(LyricsLine(time, text))
                }
            }
        }
    }
    return lines.sortedBy { it.time }
}

@Composable
fun MarqueeText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(text) {
        scrollState.scrollTo(0)
    }

    var marqueeCycles by remember { mutableIntStateOf(0) }
    LaunchedEffect(scrollState.maxValue, marqueeCycles) {
        if (scrollState.maxValue > 0 && marqueeCycles < 3) {
            delay(2000)
            scrollState.animateScrollTo(
                value = scrollState.maxValue,
                animationSpec = tween(
                    durationMillis = (scrollState.maxValue * 20).coerceAtLeast(2000),
                    easing = LinearEasing
                )
            )
            delay(1000)
            scrollState.animateScrollTo(
                value = 0,
                animationSpec = tween(
                    durationMillis = (scrollState.maxValue * 20).coerceAtLeast(2000),
                    easing = LinearEasing
                )
            )
            marqueeCycles++
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState, enabled = false)
        ) {
            Text(
                text = text,
                style = style,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeekBarSection(
    progressFraction: Float,
    formattedPosition: String,
    formattedDuration: String,
    mediaController: MediaController?,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    Column(modifier = Modifier.fillMaxWidth()) {
        val sliderValue = if (isDragging) dragPosition else progressFraction
        val displayPosition = if (isDragging) formatTime((dragPosition * duration).toLong()) else formattedPosition

        Slider(
            value = sliderValue,
            onValueChange = {
                isDragging = true
                dragPosition = it
            },
            onValueChangeFinished = {
                mediaController?.seekTo((dragPosition * duration).toLong())
                onSeek((dragPosition * duration).toLong())
                isDragging = false
            },
            colors = SliderDefaults.colors(
                activeTrackColor = MusicXTheme.colors.progressBar,
                inactiveTrackColor = MusicXTheme.colors.progressBackground,
                thumbColor = MusicXTheme.colors.sliderThumb
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(MusicXTheme.colors.sliderThumb, CircleShape)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    modifier = Modifier.height(4.dp),
                    sliderState = sliderState,
                    colors = SliderDefaults.colors(
                        activeTrackColor = MusicXTheme.colors.progressBar,
                        inactiveTrackColor = MusicXTheme.colors.progressBackground
                    ),
                    thumbTrackGapSize = 0.dp,
                    drawStopIndicator = null
                )
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(displayPosition, color = MusicXTheme.colors.secondaryText, fontSize = 12.sp)
            Text(formattedDuration, color = MusicXTheme.colors.secondaryText, fontSize = 12.sp)
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
