package com.example.mywallet

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Saves the reminder settings the web page sends, and sets the daily alarm. */
object Reminders {
    const val CHANNEL = "ipon_soft"

    fun prefs(c: Context): SharedPreferences = c.getSharedPreferences("ipon", Context.MODE_PRIVATE)

    fun apply(c: Context, enabled: Boolean, hour: Int, json: String) {
        prefs(c).edit().putBoolean("rem_on", enabled).putInt("rem_hour", hour).putString("rem_sched", json).apply()
        if (enabled) schedule(c) else cancel(c)
    }

    private fun pending(c: Context): PendingIntent =
        PendingIntent.getBroadcast(
            c, 1, Intent(c, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun schedule(c: Context) {
        val p = prefs(c)
        if (!p.getBoolean("rem_on", false)) return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, p.getInt("rem_hour", 20))
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending(c))
    }

    fun cancel(c: Context) {
        (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(c))
    }

    /** Picks today's message from the 14-day list the page prepared. */
    fun todayText(c: Context): String {
        val fallback = "Take a minute to log today's spending."
        val raw = prefs(c).getString("rem_sched", null) ?: return fallback
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return try {
            val a = JSONArray(raw)
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                if (o.optString("d") == today) return o.optString("t", fallback)
            }
            fallback
        } catch (e: Exception) { fallback }
    }

    /** Shows one notification right now. */
    fun show(ctx: Context, text: String) {
        val sound = Uri.parse("android.resource://" + ctx.packageName + "/" + R.raw.soft_chime)
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.deleteNotificationChannel("ipon_reminders") // old loud channel
            val ch = NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            ch.setSound(sound, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            ch.enableVibration(false)
            nm.createNotificationChannel(ch)
        }
        val open = PendingIntent.getActivity(
            ctx, 2, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_ipon)
            .setContentTitle("MyWallet")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(sound)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            if (NotificationManagerCompat.from(ctx).areNotificationsEnabled()) NotificationManagerCompat.from(ctx).notify(1, n)
        } catch (e: SecurityException) { }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Reminders.schedule(ctx); return
        }
        if (!Reminders.prefs(ctx).getBoolean("rem_on", false)) return
        Reminders.show(ctx, Reminders.todayText(ctx))
        Reminders.schedule(ctx) // set tomorrow's alarm
    }
}
