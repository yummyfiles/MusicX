package com.example.musicx.ui.import

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicx.data.SettingsRepository
import com.example.musicx.ui.songs.SongsViewModel
import com.example.musicx.ui.theme.MusicXTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(viewModel: SongsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { SettingsRepository(context) }
    val savedApiUrl by settingsRepository.ytApiBaseUrl.collectAsState(initial = "http://localhost:5000")

    var ytUrl by remember { mutableStateOf("") }
    var apiUrl by remember(savedApiUrl) { mutableStateOf(savedApiUrl) }
    var showSuccess by remember { mutableStateOf(false) }

    val downloadState by viewModel.ytDownloadState.collectAsState()

    LaunchedEffect(downloadState) {
        when (downloadState) {
            is SongsViewModel.YtDownloadState.Success -> {
                showSuccess = true
                ytUrl = ""
                viewModel.resetYtDownloadState()
            }
            is SongsViewModel.YtDownloadState.Error -> {
                Toast.makeText(context, (downloadState as SongsViewModel.YtDownloadState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetYtDownloadState()
            }
            else -> {}
        }
    }

    fun persistReadAccess(uris: List<Uri>) {
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                Log.w("ImportScreen", "Could not persist access for $uri", e)
            }
        }
    }

    val localFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                persistReadAccess(uris)
                viewModel.importSongs(uris)
                Toast.makeText(context, "Imported ${uris.size} files", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Import",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MusicXTheme.colors.primaryText
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MusicXTheme.colors.topBar,
                    titleContentColor = MusicXTheme.colors.primaryText
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = MusicXTheme.colors.primaryBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ImportCard(
                title = "Local Files",
                description = "Choose audio files from your device storage.",
                icon = Icons.Rounded.FileUpload,
                actionLabel = "Select Files",
                onActionClick = { localFilePicker.launch(arrayOf("audio/*")) }
            )

            YtDownloadCard(
                youtubeUrl = ytUrl,
                onYoutubeUrlChange = { ytUrl = it },
                apiUrl = apiUrl,
                onApiUrlChange = {
                    apiUrl = it
                    scope.launch { settingsRepository.setYtApiBaseUrl(it) }
                },
                isDownloading = downloadState is SongsViewModel.YtDownloadState.RequestingToken
                        || downloadState is SongsViewModel.YtDownloadState.Downloading
                        || downloadState is SongsViewModel.YtDownloadState.Importing,
                downloadStatusText = when (downloadState) {
                    is SongsViewModel.YtDownloadState.RequestingToken -> "Requesting token..."
                    is SongsViewModel.YtDownloadState.Downloading -> "Downloading audio..."
                    is SongsViewModel.YtDownloadState.Importing -> "Importing to library..."
                    else -> null
                },
                showSuccess = showSuccess,
                onSuccessDismiss = { showSuccess = false },
                onDownloadClick = {
                    if (ytUrl.isNotBlank() && apiUrl.isNotBlank()) {
                        viewModel.importFromYoutube(ytUrl.trim(), apiUrl.trim(), context.cacheDir)
                    }
                }
            )
        }
    }
}

@Composable
fun ImportCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MusicXTheme.colors.cardBackground),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MusicXTheme.colors.outline.copy(alpha = 0.2f))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MusicXTheme.colors.iconPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MusicXTheme.colors.primaryText
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyLarge,
                color = MusicXTheme.colors.secondaryText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onActionClick,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, MusicXTheme.colors.buttonOutline),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MusicXTheme.colors.buttonBackground,
                    contentColor = MusicXTheme.colors.buttonText
                )
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun YtDownloadCard(
    youtubeUrl: String,
    onYoutubeUrlChange: (String) -> Unit,
    apiUrl: String,
    onApiUrlChange: (String) -> Unit,
    isDownloading: Boolean,
    downloadStatusText: String?,
    showSuccess: Boolean,
    onSuccessDismiss: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MusicXTheme.colors.cardBackground),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MusicXTheme.colors.outline.copy(alpha = 0.2f))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.Download,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MusicXTheme.colors.iconPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "YouTube Download",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MusicXTheme.colors.primaryText
            )
            Text(
                "Paste a YouTube URL to download and add it to your library.",
                style = MaterialTheme.typography.bodyLarge,
                color = MusicXTheme.colors.secondaryText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = youtubeUrl,
                onValueChange = onYoutubeUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("YouTube URL", color = MusicXTheme.colors.inputHint) },
                singleLine = true,
                enabled = !isDownloading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MusicXTheme.colors.primaryText,
                    unfocusedTextColor = MusicXTheme.colors.primaryText,
                    focusedBorderColor = MusicXTheme.colors.primaryAccent,
                    unfocusedBorderColor = MusicXTheme.colors.outlineVariant,
                    focusedContainerColor = MusicXTheme.colors.inputBackground,
                    unfocusedContainerColor = MusicXTheme.colors.inputBackground
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = apiUrl,
                onValueChange = onApiUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("API Server URL", color = MusicXTheme.colors.inputHint) },
                singleLine = true,
                enabled = !isDownloading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MusicXTheme.colors.primaryText,
                    unfocusedTextColor = MusicXTheme.colors.primaryText,
                    focusedBorderColor = MusicXTheme.colors.primaryAccent,
                    unfocusedBorderColor = MusicXTheme.colors.outlineVariant,
                    focusedContainerColor = MusicXTheme.colors.inputBackground,
                    unfocusedContainerColor = MusicXTheme.colors.inputBackground
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            downloadStatusText?.let {
                if (it.isNotBlank()) {
                    Text(
                        it,
                        color = MusicXTheme.colors.secondaryText,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (showSuccess) {
                Text(
                    "Added to library!",
                    color = MusicXTheme.colors.primaryAccent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = onDownloadClick,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = youtubeUrl.isNotBlank() && apiUrl.isNotBlank() && !isDownloading,
                border = androidx.compose.foundation.BorderStroke(2.dp, MusicXTheme.colors.buttonOutline),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MusicXTheme.colors.buttonBackground,
                    contentColor = MusicXTheme.colors.buttonText
                )
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MusicXTheme.colors.buttonText
                    )
                } else {
                    Text(if (showSuccess) "Done" else "Download & Import")
                }
            }
        }
    }
}
