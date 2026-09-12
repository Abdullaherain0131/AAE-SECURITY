package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.util.ProtectionPreferences
import com.example.data.AntivirusDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull

class SingularityWidgetProvider : AppWidgetProvider() {

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
            val views = RemoteViews(context.packageName, R.layout.widget_singularity_core)
            
            // PendingIntent to launch app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_progress, pendingIntent)
            
            // Read active threats from DB using a quick coroutine
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AntivirusDatabase.getDatabase(context)
                    val threatsFlow = db.antivirusDao().getActiveThreats()
                    val threats = threatsFlow.firstOrNull() ?: emptyList()
                    val threatCount = threats.size
                    
                    val prefs = ProtectionPreferences(context)
                    val isVpnActive = prefs.activeDnsProfileId.isNotEmpty()
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        views.setTextViewText(R.id.widget_text_3, "> İZOLE EDİLEN TEHDİT: $threatCount")
                        
                        if (isVpnActive) {
                            views.setTextViewText(R.id.widget_text_2, "> AĞ İZLEME: AKTİF [ŞİFRELİ]")
                        } else {
                            views.setTextViewText(R.id.widget_text_2, "> AĞ İZLEME: PASİF")
                        }
                        
                        if (threatCount > 0) {
                            // Turn red
                            views.setTextColor(R.id.widget_text_1, android.graphics.Color.RED)
                            views.setTextColor(R.id.widget_text_3, android.graphics.Color.RED)
                            // We would normally change the progress drawable color, but RemoteViews is limited.
                        } else {
                            // Safe Cyan
                            views.setTextColor(R.id.widget_text_1, android.graphics.Color.parseColor("#00E5FF"))
                            views.setTextColor(R.id.widget_text_3, android.graphics.Color.WHITE)
                        }
                        
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
