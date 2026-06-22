package com.example.musicx.ui.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicx.data.MusicRepository
import com.example.musicx.data.local.entity.Playlist
import com.example.musicx.model.Song
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SongsViewModel(private val repository: MusicRepository) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private var hasLoadedSongs = false
    private var lyricsSyncJob: Job? = null

    private val _selectedSongUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedSongUris: StateFlow<Set<String>> = _selectedSongUris.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init {
        refreshPlaylists()
    }

    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedSongUris.value = emptySet()
        }
    }

    fun toggleSongSelection(uri: String) {
        val current = _selectedSongUris.value.toMutableSet()
        if (current.contains(uri)) {
            current.remove(uri)
        } else {
            current.add(uri)
        }
        _selectedSongUris.value = current
    }

    fun deleteSelectedSongs() {
        viewModelScope.launch {
            val uris = _selectedSongUris.value.toList()
            if (uris.isEmpty()) return@launch
            try {
                repository.deleteSongs(uris)
                _selectedSongUris.value = emptySet()
                _isSelectionMode.value = false
                loadSongs(forceRefresh = true)
            } catch (e: Exception) {
                Log.e("SongsViewModel", "Failed to delete songs", e)
            }
        }
    }

    fun loadSongs(forceRefresh: Boolean = false) {
        if (hasLoadedSongs && !forceRefresh) return
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _songs.value = repository.fetchLocalSongs()
                hasLoadedSongs = true
                lyricsSyncJob?.cancel()
                lyricsSyncJob = launch { repository.syncAllLyrics() }
            } catch (e: Exception) {
                android.util.Log.e("SongsViewModel", "Failed to load songs", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importSongs(uris: List<android.net.Uri>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.importSongs(uris)
                _songs.value = repository.fetchLocalSongs()
                hasLoadedSongs = true
                lyricsSyncJob?.cancel()
                lyricsSyncJob = launch { repository.syncAllLyrics() }
            } catch (e: Exception) {
                android.util.Log.e("SongsViewModel", "Failed to import songs", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateMetadata(uri: String, title: String?, artist: String?, lyrics: String? = null) {
        viewModelScope.launch {
            repository.updateMetadata(uri, title, artist, lyrics)
            loadSongs(forceRefresh = true)
        }
    }
    
    fun autoFetchLyrics(song: Song) {
        viewModelScope.launch {
            val fetchedLyrics = repository.autoFetchLyrics(song)
            if (fetchedLyrics != null) {
                _songs.value = _songs.value.map {
                    if (it.mediaUri == song.mediaUri) it.copy(lyrics = fetchedLyrics) else it
                }
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return@launch
            try {
                repository.createPlaylist(trimmedName)
                refreshPlaylists()
            } catch (e: Exception) {
                Log.e("SongsViewModel", "Failed to create playlist", e)
            }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            try {
                repository.deletePlaylist(playlist)
                refreshPlaylists()
            } catch (e: Exception) {
                Log.e("SongsViewModel", "Failed to delete playlist", e)
            }
        }
    }

    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            try {
                repository.updatePlaylist(playlist)
                refreshPlaylists()
            } catch (e: Exception) {
                Log.e("SongsViewModel", "Failed to update playlist", e)
            }
        }
    }

    fun addSongsToPlaylist(playlist: Playlist, songUris: List<String>) {
        viewModelScope.launch {
            try {
                val updatedUris = (playlist.songUris + songUris).distinct()
                repository.updatePlaylist(playlist.copy(songUris = updatedUris))
                refreshPlaylists()
            } catch (e: Exception) {
                Log.e("SongsViewModel", "Failed to add songs to playlist", e)
            }
        }
    }

    fun refreshPlaylists() {
        viewModelScope.launch {
            try {
                _playlists.value = repository.getAllPlaylists().firstOrNull().orEmpty()
            } catch (e: Exception) {
                Log.e("SongsViewModel", "Failed to refresh playlists", e)
            }
        }
    }
}
