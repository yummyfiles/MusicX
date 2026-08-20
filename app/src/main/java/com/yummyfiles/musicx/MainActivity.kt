package com.yummyfiles.musicx

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yummyfiles.musicx.data.MusicRepository
import com.yummyfiles.musicx.data.SettingsRepository
import com.yummyfiles.musicx.playback.MusicController
import com.yummyfiles.musicx.ui.navigation.MusicXApp
import com.yummyfiles.musicx.ui.settings.SettingsViewModel
import com.yummyfiles.musicx.ui.songs.SongsViewModel
import com.yummyfiles.musicx.ui.theme.MusicXTheme

class MainActivity : ComponentActivity() {

    private lateinit var musicController: MusicController

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            songsViewModel.loadSongs()
        }
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(SettingsRepository(applicationContext)) as T
            }
        }
    }

    private val songsViewModel: SongsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SongsViewModel(MusicRepository(applicationContext)) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MusicX", "MainActivity onCreate")
        
        musicController = MusicController(this)
        
        enableEdgeToEdge()
        
        checkAndRequestPermissions()

        setContent {
            val themeState by settingsViewModel.themeState.collectAsState()

            MusicXTheme(themeState = themeState) {
                MusicXApp(songsViewModel, settingsViewModel, musicController)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            songsViewModel.loadSongs()
        }
    }

    override fun onDestroy() {
        Log.d("MusicX", "MainActivity onDestroy")
        if (::musicController.isInitialized) {
            musicController.release()
        }
        super.onDestroy()
    }
}
