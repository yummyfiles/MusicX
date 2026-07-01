package com.example.musicx.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionCallback
import androidx.glance.action.ActionParameters

class WidgetActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, actionParameters: ActionParameters) {
        WidgetActions.handleAction(context, actionParameters)
    }
}
