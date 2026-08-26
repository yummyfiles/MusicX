package com.yummyfiles.musicx.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yummyfiles.musicx.ui.theme.MusicXTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    // This app isn't like the others.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", color = MusicXTheme.colors.primaryText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MusicXTheme.colors.iconPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MusicXTheme.colors.topBar,
                ),
            )
        },
        containerColor = MusicXTheme.colors.primaryBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Our cool logo.
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MusicXTheme.colors.cardBackground)
                    .border(2.dp, MusicXTheme.colors.primaryAccent, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = com.yummyfiles.musicx.R.drawable.musicx_logo),
                    contentDescription = "MusicX Logo",
                    modifier = Modifier.size(80.dp),
                    tint = MusicXTheme.colors.primaryAccent
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MusicX",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MusicXTheme.colors.primaryText,
            )

            Text(
                text = "Version 2.0.6",
                fontSize = 14.sp,
                color = MusicXTheme.colors.secondaryText
            )

            Spacer(modifier = Modifier.height(48.dp))

            val uriHandler = LocalUriHandler.current

            AboutSection(
                title = "Developer",
                content = "YUMMYFILES // DEV"
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AboutLink(
                    icon = ImageVector.vectorResource(id = com.yummyfiles.musicx.R.drawable.ic_github),
                    label = "Project",
                ) {
                    uriHandler.openUri("https://github.com/MusicX")
                }
                Spacer(modifier = Modifier.width(24.dp))
                AboutLink(
                    icon = Icons.Rounded.Person,
                    label = "Profile",
                    onClick = { uriHandler.openUri("https://github.com/yummyfiles") }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            AboutSection(
                title = "Inspiration",
                content = "MusicX is an offline music player app for android built because I was tired of music apps locking basic features behind paywalls or stripping away customization."
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Made With ♡ by me!",
                fontSize = 14.sp,
                color = MusicXTheme.colors.primaryAccent,
                modifier = Modifier.padding(vertical = 16.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AboutSection(title: String, content: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MusicXTheme.colors.primaryAccent
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            fontSize = 16.sp,
            color = MusicXTheme.colors.primaryText,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun AboutLink(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MusicXTheme.colors.cardBackground,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MusicXTheme.colors.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MusicXTheme.colors.primaryAccent
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MusicXTheme.colors.primaryAccent
            )
        }
    }
}
