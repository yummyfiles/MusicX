package com.yummyfiles.musicx.ui.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yummyfiles.musicx.data.MusicRepository
import com.yummyfiles.musicx.model.Playlist
import com.yummyfiles.musicx.model.Song
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SongsViewModel(private val repository: MusicRepository) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(value = false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedSongUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedSongUris: StateFlow<Set<String>> = _selectedSongUris.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(value = false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllPlaylists().collect {
                _playlists.value = it
            }
        }
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
            _isLoading.value = true
            try {
                repository.deleteSongs(_selectedSongUris.value.toList())
                _selectedSongUris.value = emptySet()
                _isSelectionMode.value = false
                loadSongs()
            } catch (e: Exception) {
                Log.e("SongsViewModel", "Failed to delete songs", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _songs.value = repository.fetchLocalSongs()
            } catch (e: Exception) {
                Log.e("SongsViewModel", "Failed to load songs", e)
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
                loadSongs()
            } catch (e: Exception) {
                Log.e("SongsViewModel", "Failed to import songs", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateMetadata(uri: String, title: String?, artist: String?, lyrics: String? = null) {
        viewModelScope.launch {
            repository.updateMetadata(uri, title, artist, lyrics)
            loadSongs()
        }
    }
    
    fun autoFetchLyrics(@Suppress("UNUSED_PARAMETER") song: Song) { /* No-op */ }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return@launch
            try {
                repository.createPlaylist(trimmedName)
            } catch (e: Exception) {
                Log.e("SongsViewModel", "Failed to create playlist", e)
            }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            try {
                repository.deletePlaylist(playlist)
            } catch (e: Exception) {
                Log.e("SongsViewModel", "Failed to delete playlist", e)
            }
        }
    }

    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            try {
                repository.updatePlaylist(playlist)
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
            } catch (e: Exception) {
                Log.e("SongsViewModel", "Failed to add songs to playlist", e)
            }
        }
    }

}
