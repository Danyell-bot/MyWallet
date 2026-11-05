package com.example.mywallet

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** The home-screen widget: total balance, safe-to-spend, and a + button. */
class IponWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    companion object {
        fun refresh(c: Context) {
            val m = AppWidgetManager.getInstance(c)
            m.getAppWidgetIds(ComponentName(c, IponWidget::class.java)).forEach { update(c, m, it) }
        }

        private fun open(c: Context, add: String?, code: Int): PendingIntent {
            val i = Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (add != null) i.putExtra("add", add)
            return PendingIntent.getActivity(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun update(c: Context, m: AppWidgetManager, id: Int) {
            val p = Reminders.prefs(c)
            val hide = p.getBoolean("w_hide", false)
            val total = p.getString("w_total", null)
            val v = RemoteViews(c.packageName, R.layout.widget_ipon)
            v.setTextViewText(R.id.w_total, if (total == null) "Open MyWallet" else if (hide) "Hidden" else total)
            v.setTextViewText(R.id.w_safe, if (total == null || hide) "" else "Safe today: " + p.getString("w_safe", ""))
            v.setOnClickPendingIntent(R.id.w_root, open(c, null, 10))
            v.setOnClickPendingIntent(R.id.w_add, open(c, "expense", 11))
            m.updateAppWidget(id, v)
        }
    }
}
