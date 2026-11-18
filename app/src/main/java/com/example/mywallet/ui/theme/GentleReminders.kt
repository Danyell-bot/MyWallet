package com.example.mywallet

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.mywallet.data.AppState
import com.example.mywallet.data.Store
import com.example.mywallet.data.bankCalc
import com.example.mywallet.data.billDue
import com.example.mywallet.data.dn
import com.example.mywallet.data.loggedDays
import com.example.mywallet.data.streak
import com.example.mywallet.data.todayDn
import java.util.Calendar
import java.util.Random

/**
 * Gentle reminders: one on/off switch, no fixed time. A few times a day, at random moments
 * between 9 AM and 9 PM, MyWallet sends a short nudge (a bill coming up, a bank top-up, your
 * streak, or a friendly "anything to log?"). Notifications never show amounts.
 */
object GentleReminders {
    const val CHANNEL = "mywallet_gentle_v13b"
    private const val ACTION = "com.example.mywallet.REMIND"
    private const val FROM_H = 9
    private const val TO_H = 21

    fun enabled(ctx: Context): Boolean = Store(ctx).also { it.load() }.settingStr("remind") == "true"

    private fun pending(ctx: Context, create: Boolean): PendingIntent? {
        val i = Intent(ctx, GentleReceiver::class.java).setAction(ACTION)
        val flags = PendingIntent.FLAG_IMMUTABLE or (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE)
        return PendingIntent.getBroadcast(ctx, 4711, i, flags)
    }

    /** next random moment: 2 to 6 hours from now, always inside the 9 AM - 9 PM window */
    fun nextTime(now: Long = System.currentTimeMillis(), rnd: Random = Random()): Long {
        val c = Calendar.getInstance().apply { timeInMillis = now + (120 + rnd.nextInt(240)) * 60_000L }
        val h = c.get(Calendar.HOUR_OF_DAY)
        if (h < FROM_H || h >= TO_H) {
            if (h >= TO_H) c.add(Calendar.DAY_OF_YEAR, 1)
            c.set(Calendar.HOUR_OF_DAY, FROM_H); c.set(Calendar.MINUTE, rnd.nextInt(180)); c.set(Calendar.SECOND, 0)
        }
        return c.timeInMillis
    }

    /** make sure one alarm is waiting (does nothing if there already is one) */
    fun ensure(ctx: Context) {
        if (!enabled(ctx)) { cancel(ctx); return }
        if (pending(ctx, false) == null) schedule(ctx)
    }

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(ctx, true) ?: return
        // inexact on purpose: no special permission, and the phone can batch it to save battery
        am.set(AlarmManager.RTC, nextTime(), pi)
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        pending(ctx, false)?.let { am.cancel(it); it.cancel() }
    }

    fun canNotify(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return false
        return (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).areNotificationsEnabled()
    }

    private fun channel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val attrs = android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            val ch = NotificationChannel(CHANNEL, "Gentle reminders", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "A few friendly nudges a day"
            ch.setSound(android.net.Uri.parse("android.resource://" + ctx.packageName + "/" + R.raw.mw_chime), attrs)
            ch.enableVibration(true)
            nm.createNotificationChannel(ch)
        }
    }

    /** what to say right now. Never includes amounts. */
    fun message(s: AppState?, rnd: Random = Random()): Pair<String, String> {
        val generic = listOf(
            "Quick check" to "Spent anything today? Log it before you forget.",
            "Keep your flame lit" to "A tiny record a day keeps your streak going.",
            "Money moment" to "Take 10 seconds to log what you spent today.",
            "How is today going?" to "Add today's spending while it is fresh.",
        )
        if (s == null) return generic[rnd.nextInt(generic.size)]
        val picks = ArrayList<Pair<String, String>>()
        val t = todayDn()
        s.bills.map { it to billDue(it, t).dn }.filter { it.second - t <= 2 }.minByOrNull { it.second }?.let { (b, d) ->
            picks.add("Bill coming up" to (b.name + (if (d < t) " is overdue." else if (d == t) " is due today." else if (d == t + 1) " is due tomorrow." else " is due in 2 days.")))
        }
        s.debts.filter { !it.paid && it.due.isNotEmpty() && dn(it.due) - t <= 2 }.minByOrNull { dn(it.due) }?.let { d ->
            picks.add("Debt reminder" to (if (d.theyOweMe) d.who + " owes you and it is nearly due." else "You owe " + d.who + " and it is nearly due."))
        }
        s.accounts.firstOrNull { it.kind == "bank" && !it.ignored && it.bank != null }?.let { a ->
            val bi = s.bankCalc(a)
            if (bi != null && bi.verdict == "add") picks.add("Stay fee-safe" to (a.name + " needs a top-up to avoid a fee."))
        }
        val st = s.streak()
        if (st > 0 && !s.loggedDays().contains(t)) picks.add("Do not lose your streak" to ("You are on a $st-day streak. Log something today to keep it."))
        // real nudges first, otherwise something friendly
        return if (picks.isNotEmpty() && rnd.nextInt(4) != 0) picks[rnd.nextInt(picks.size)] else generic[rnd.nextInt(generic.size)]
    }

    fun show(ctx: Context, title: String, text: String) {
        if (!canNotify(ctx)) return
        channel(ctx)
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, NativeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val b = if (Build.VERSION.SDK_INT >= 26) android.app.Notification.Builder(ctx, CHANNEL) else @Suppress("DEPRECATION") android.app.Notification.Builder(ctx)
        if (Build.VERSION.SDK_INT < 26) b.setSound(android.net.Uri.parse("android.resource://" + ctx.packageName + "/" + R.raw.mw_chime)).setDefaults(android.app.Notification.DEFAULT_VIBRATE)
        val n = b.setSmallIcon(android.R.drawable.stat_notify_more).setContentTitle(title).setContentText(text).setContentIntent(open).setAutoCancel(true).build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(4711, n)
    }
}

class GentleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ctx = context.applicationContext
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) { GentleReminders.ensure(ctx); return }
        val store = Store(ctx).also { it.load() }
        if (store.settingStr("remind") != "true") return
        val (title, text) = GentleReminders.message(store.state)
        GentleReminders.show(ctx, title, text)
        GentleReminders.schedule(ctx)
    }
}
