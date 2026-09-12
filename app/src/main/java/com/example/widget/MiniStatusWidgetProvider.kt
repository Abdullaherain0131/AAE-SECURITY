package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AntivirusDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MiniStatusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_mini_status)
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_mini_root, pendingIntent)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AntivirusDatabase.getDatabase(context)
                    val threatsFlow = db.antivirusDao().getActiveThreats()
                    val threats = threatsFlow.firstOrNull() ?: emptyList()
                    val threatCount = threats.size
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        if (threatCount > 0) {
                            views.setTextViewText(R.id.widget_mini_status_text, "RİSKLİ")
                            views.setTextColor(R.id.widget_mini_status_text, android.graphics.Color.RED)
                            views.setTextViewText(R.id.widget_mini_threats, "$threatCount TEHDİT")
                            views.setTextColor(R.id.widget_mini_threats, android.graphics.Color.RED)
                        } else {
                            views.setTextViewText(R.id.widget_mini_status_text, "GÜVENDE")
                            views.setTextColor(R.id.widget_mini_status_text, android.graphics.Color.parseColor("#00E5FF"))
                            views.setTextViewText(R.id.widget_mini_threats, "TEMİZ")
                            views.setTextColor(R.id.widget_mini_threats, android.graphics.Color.WHITE)
                        }
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {}
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
