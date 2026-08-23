package com.yummyfiles.musicx.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.yummyfiles.musicx.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // splash screen vibes fr lol
    val image = ImageVector.vectorResource(id = R.drawable.musicx_logo)
    
    var fillAlpha by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(Unit) {
        // bruh let's wait a bit then fade in
        delay(500.milliseconds)
        
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3150)
        ) { value, _ ->
            fillAlpha = value
        }
        
        delay(1350.milliseconds)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = image,
            contentDescription = null,
            modifier = Modifier.size(240.dp),
            tint = Color.White.copy(alpha = fillAlpha)
        )
    }
}
