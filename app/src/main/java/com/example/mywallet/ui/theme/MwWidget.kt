package com.example.mywallet

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** set by the widget (and others) to ask the app to open the Add record sheet */
object LaunchBus {
    var add: String? by mutableStateOf(null)
    /** search text to open the Search screen with (for example a tapped tag) */
    var query: String? by mutableStateOf(null)
    /** developer mode: lock the app now */
    var lockNow: Boolean by mutableStateOf(false)
}

/** Home-screen widget for the native app: total balance, safe to spend, and a + button. */
class MwWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { ids.forEach { update(context, manager, it) } }

    companion object {
        private const val PREFS = "mw_widget"

        /** called by the app whenever data changes: remembers the two numbers and redraws the widget */
        fun push(c: Context, total: String, safe: String, hide: Boolean) {
            c.getSharedPreferences(PREFS, 0).edit().putString("total", total).putString("safe", safe).putBoolean("hide", hide).apply()
            refresh(c)
        }

        fun refresh(c: Context) {
            try {
                val m = AppWidgetManager.getInstance(c)
                m.getAppWidgetIds(ComponentName(c, MwWidget::class.java)).forEach { update(c, m, it) }
            } catch (e: Exception) { }
        }

        private fun open(c: Context, add: String?, code: Int): PendingIntent {
            val i = Intent(c, NativeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (add != null) i.putExtra("add", add)
            return PendingIntent.getActivity(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun update(c: Context, m: AppWidgetManager, id: Int) {
            val p = c.getSharedPreferences(PREFS, 0)
            val hide = p.getBoolean("hide", false)
            val total = p.getString("total", null)
            val v = RemoteViews(c.packageName, R.layout.widget_ipon)
            v.setTextViewText(R.id.w_total, if (total == null) "Open MyWallet" else if (hide) "Hidden" else total)
            v.setTextViewText(R.id.w_safe, if (total == null || hide) "" else "Safe today: " + (p.getString("safe", "") ?: ""))
            v.setOnClickPendingIntent(R.id.w_root, open(c, null, 10))
            v.setOnClickPendingIntent(R.id.w_add, open(c, "expense", 11))
            m.updateAppWidget(id, v)
        }
    }
}
