package com.example.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetSyncHelper {
    const val EXTRA_OPEN_TAB = "extra_open_tab"

    suspend fun refreshAll(context: Context) {
        runCatching {
            PriorityQuadrantWidget().updateAll(context.applicationContext)
        }
    }

    fun refreshAllAsync(context: Context, scope: CoroutineScope) {
        val appContext = context.applicationContext
        scope.launch(Dispatchers.IO) {
            refreshAll(appContext)
        }
    }
}
