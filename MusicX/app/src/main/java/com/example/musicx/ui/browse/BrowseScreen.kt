package com.example.musicx.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musicx.model.Song
import com.example.musicx.ui.theme.MusicXTheme


enum class BrowseSort(val label: String) {
    A_Z("A-Z"),
    Z_A("Z-A"),
    COUNT("Most Songs")
}

@Composable
fun ArtistsScreen(
    songs: List<Song>,
    onArtistClick: (String) -> Unit
) {
    BrowseListScreen(
        title = "Artists",
        songs = songs,
        extractKey = { it.artist },
        icon = Icons.Rounded.Person,
        onItemClick = onArtistClick
    )
}

@Composable
fun AlbumsScreen(
    songs: List<Song>,
    onAlbumClick: (String) -> Unit
) {
    BrowseListScreen(
        title = "Albums",
        songs = songs,
        extractKey = { it.album },
        icon = Icons.Rounded.Album,
        onItemClick = onAlbumClick
    )
}

@Composable
fun GenresScreen(
    songs: List<Song>,
    onGenreClick: (String) -> Unit
) {
    BrowseListScreen(
        title = "Genres",
        songs = songs,
        extractKey = { it.genre },
        icon = Icons.Rounded.MusicNote,
        onItemClick = onGenreClick
    )
}

@Composable
private fun BrowseListScreen(
    title: String,
    songs: List<Song>,
    extractKey: (Song) -> String,
    icon: ImageVector,
    onItemClick: (String) -> Unit
) {
    var sort by remember { mutableStateOf(BrowseSort.A_Z) }

    val groups = remember(songs, sort) {
        val grouped = songs
            .filter { extractKey(it).isNotBlank() }
            .groupBy { extractKey(it) }
            .mapValues { it.value.size }
            .toList()

        when (sort) {
            BrowseSort.A_Z -> grouped.sortedBy { it.first.lowercase() }
            BrowseSort.Z_A -> grouped.sortedByDescending { it.first.lowercase() }
            BrowseSort.COUNT -> grouped.sortedByDescending { it.second }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$title (${groups.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MusicXTheme.colors.primaryAccent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            SortDropdown(sort = sort, onSortChange = { sort = it })
        }

        if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No $title found",
                    color = MusicXTheme.colors.iconSecondary
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = groups,
                    key = { it.first }
                ) { (name, count) ->
                    BrowseItem(
                        name = name,
                        count = count,
                        icon = icon,
                        onClick = { onItemClick(name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseItem(
    name: String,
    count: Int,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MusicXTheme.colors.primaryAccent,
            modifier = Modifier.size(28.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MusicXTheme.colors.primaryText
            )
            Text(
                text = "$count song${if (count != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MusicXTheme.colors.iconSecondary
            )
        }
    }
}

@Composable
private fun SortDropdown(
    sort: BrowseSort,
    onSortChange: (BrowseSort) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(sort.label, color = MusicXTheme.colors.primaryAccent)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            BrowseSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option.label,
                            fontWeight = if (option == sort) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun FilteredSongsList(
    songs: List<Song>,
    title: String,
    filterKey: (Song) -> String,
    filterValue: String,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit = {}
) {
    val filtered = remember(songs, filterValue) {
        songs.filter { filterKey(it) == filterValue }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onBack) {
                Text("< Back", color = MusicXTheme.colors.primaryAccent)
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MusicXTheme.colors.primaryText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${filtered.size} song${if (filtered.size != 1) "s" else ""}",
                color = MusicXTheme.colors.iconSecondary
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = filtered,
                key = { it.mediaUri.toString() }
            ) { song ->
                SongRow(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MusicXTheme.colors.primaryAccent,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MusicXTheme.colors.primaryText,
                maxLines = 1
            )
            song.artist.takeIf { it.isNotBlank() }?.let { artist ->
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MusicXTheme.colors.iconSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
