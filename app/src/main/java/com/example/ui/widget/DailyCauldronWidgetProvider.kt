package com.example.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class DailyCauldronWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val remainingKcal = prefs.getInt(KEY_REMAINING_KCAL, 0)
        val missingProt = prefs.getInt(KEY_MISSING_PROT, 0)
        val statusText = prefs.getString(KEY_STATUS_TITLE, "Caldero Listo") ?: "Caldero Listo"
        val waterText = prefs.getString(KEY_WATER_TEXT, "0/8 vasos") ?: "0/8 vasos"

        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_daily_cauldron).apply {
                setTextViewText(R.id.widget_kcal_remaining, "Antes del techo: $remainingKcal kcal")
                setTextViewText(
                    R.id.widget_protein_remaining,
                    "Proteína faltante: ${missingProt}g · Agua: $waterText"
                )
                setTextViewText(R.id.widget_status_badge, statusText)
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    companion object {
        private const val PREFS_NAME = "estudihambre_widget_snapshot"
        private const val KEY_REMAINING_KCAL = "remaining_kcal"
        private const val KEY_MISSING_PROT = "missing_prot"
        private const val KEY_STATUS_TITLE = "status_title"
        private const val KEY_WATER_TEXT = "water_text"

        fun saveSnapshotAndRefresh(
            context: Context,
            remainingBeforeCeilingKcal: Int,
            missingProteinG: Int,
            statusTitle: String,
            waterGlasses: Int,
            waterTarget: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putInt(KEY_REMAINING_KCAL, remainingBeforeCeilingKcal.coerceAtLeast(0))
                .putInt(KEY_MISSING_PROT, missingProteinG.coerceAtLeast(0))
                .putString(KEY_STATUS_TITLE, statusTitle)
                .putString(KEY_WATER_TEXT, "$waterGlasses/$waterTarget vasos")
                .apply()

            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val component = ComponentName(appContext, DailyCauldronWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(appContext, DailyCauldronWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                appContext.sendBroadcast(intent)
            }
        }
    }
}
