package com.example.musicx

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.musicx.data.MusicRepository
import com.example.musicx.data.SettingsRepository
import com.example.musicx.playback.MusicController
import com.example.musicx.ui.navigation.MusicXApp
import com.example.musicx.ui.settings.SettingsViewModel
import com.example.musicx.ui.songs.SongsViewModel
import com.example.musicx.ui.theme.MusicXTheme
import com.example.musicx.ui.splash.SplashScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {

    private lateinit var musicController: MusicController

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

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Correctly handle the system splash screen
        installSplashScreen()

        super.onCreate(savedInstanceState)
        Log.d("MusicX", "MainActivity onCreate")
        
        try {
            musicController = MusicController(this)
        } catch (e: Exception) {
            Log.e("MusicX", "Failed to init MusicController", e)
        }
        
        enableEdgeToEdge()
        
        setContent {
            val themeState by settingsViewModel.themeState.collectAsState()
            var showSplash by rememberSaveable { mutableStateOf(true) }

            MusicXTheme(themeState = themeState) {
                if (showSplash) {
                    SplashScreen(onSplashFinished = { showSplash = false })
                } else {
                    val audioPermissions = remember {
                        val list = mutableListOf<String>()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            list.add(Manifest.permission.READ_MEDIA_AUDIO)
                        } else {
                            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        list
                    }

                    val audioPermissionState = rememberMultiplePermissionsState(audioPermissions)

                    val notificationPermissions = remember {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            listOf(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            emptyList()
                        }
                    }

                    val notificationPermissionState = if (notificationPermissions.isNotEmpty()) {
                        rememberMultiplePermissionsState(notificationPermissions)
                    } else {
                        null
                    }

                    LaunchedEffect(Unit) {
                        if (!audioPermissionState.allPermissionsGranted) {
                            audioPermissionState.launchMultiplePermissionRequest()
                        }
                    }

                    LaunchedEffect(audioPermissionState.allPermissionsGranted) {
                        if (audioPermissionState.allPermissionsGranted && notificationPermissionState?.allPermissionsGranted == false) {
                            notificationPermissionState.launchMultiplePermissionRequest()
                        }
                    }

                    if (audioPermissionState.allPermissionsGranted) {
                        LaunchedEffect(Unit) {
                            songsViewModel.loadSongs()
                        }

                        if (::musicController.isInitialized && musicController.mediaController.value != null) {
                            MusicXApp(songsViewModel, settingsViewModel, musicController)
                        } else {
                            LoadingScreen("Connecting to Audio Engine...")
                        }
                    } else {
                        LoadingScreen("Waiting for permissions...")
                    }
                }
            }
        }
    }

    @Composable
    private fun LoadingScreen(message: String) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MusicXTheme.colors.primaryBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MusicXTheme.colors.primaryAccent)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = message, color = MusicXTheme.colors.primaryText)
            }
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
