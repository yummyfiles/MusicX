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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.Color
import androidx.glance.unit.dp
import androidx.glance.unit.sp

class QuickActionsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val hasSongs = prefs[HAS_SONGS] ?: false

            Row(
                modifier = GlanceModifier.fillMaxSize().background(Color.Black).padding(8.dp),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                if (hasSongs) {
                    Column(
                        modifier = GlanceModifier.defaultWeight().actionRunCallback<WidgetActionCallback>(
                            actionParametersOf(WidgetActions.ACTION_KEY to WidgetActions.ACTION_RESUME)
                        ),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                    ) {
                        Text(text = "\u25B6", style = TextStyle(color = Color.White, fontSize = 20.sp))
                        Text(text = "Resume", style = TextStyle(color = Color.White, fontSize = 10.sp))
                    }
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                Column(
                    modifier = GlanceModifier.defaultWeight().actionRunCallback<WidgetActionCallback>(
                        actionParametersOf(WidgetActions.ACTION_KEY to WidgetActions.ACTION_OPEN_SEARCH)
                    ),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text(text = "\uD83D\uDD0D", style = TextStyle(color = Color.White, fontSize = 20.sp))
                    Text(text = "Search", style = TextStyle(color = Color.White, fontSize = 10.sp))
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                Column(
                    modifier = GlanceModifier.defaultWeight().actionRunCallback<WidgetActionCallback>(
                        actionParametersOf(WidgetActions.ACTION_KEY to WidgetActions.ACTION_OPEN_IMPORT)
                    ),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text(text = "\u2B07", style = TextStyle(color = Color.White, fontSize = 20.sp))
                    Text(text = "Import", style = TextStyle(color = Color.White, fontSize = 10.sp))
                }
            }
        }
    }

    companion object {
        val HAS_SONGS = booleanPreferencesKey("qa_has_songs")
    }
}

class QuickActionsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QuickActionsWidget()
}
