package com.example.musicx.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.currentState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.Color
import androidx.glance.unit.dp
import androidx.glance.unit.sp

class ShuffleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val hasSongs = prefs[HAS_SONGS] ?: false

            Column(
                modifier = GlanceModifier.fillMaxSize().background(Color.Black).padding(8.dp)
                    .actionRunCallback<WidgetActionCallback>(
                        actionParametersOf(WidgetActions.ACTION_KEY to WidgetActions.ACTION_SHUFFLE_ALL)
                    ),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = if (hasSongs) "\uD83D\uDD00" else "\u274C",
                    style = TextStyle(color = Color.White, fontSize = 20.sp),
                    modifier = GlanceModifier.padding(bottom = 4.dp)
                )
                Text(
                    text = if (hasSongs) "Shuffle All" else "No Music",
                    style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    companion object {
        val HAS_SONGS = booleanPreferencesKey("shuffle_has_songs")
    }
}

class ShuffleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ShuffleWidget()
}
