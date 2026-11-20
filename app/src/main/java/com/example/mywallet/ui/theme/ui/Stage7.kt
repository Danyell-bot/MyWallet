package com.example.mywallet.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.data.AppState
import com.example.mywallet.data.Store
import com.example.mywallet.data.acc
import com.example.mywallet.data.cat
import com.example.mywallet.data.dn
import com.example.mywallet.data.fromDn
import com.example.mywallet.data.money
import com.example.mywallet.data.moneyParts
import com.example.mywallet.data.todayDn
import com.example.mywallet.data.todayStr
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private fun toast7(ctx: android.content.Context, m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()
private fun num7(t: String): Double = t.toDoubleOrNull() ?: 0.0
private fun fmt7(d: Double): String = java.math.BigDecimal.valueOf(d).stripTrailingZeros().toPlainString()
private fun ord7(n: Int): String = n.toString() + (if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" })
private val WD = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

private fun JsonObject.str(k: String) = (this[k] as? JsonPrimitive)?.content ?: ""
private fun JsonObject.dbl(k: String) = str(k).toDoubleOrNull() ?: 0.0
private fun JsonObject.int(k: String, def: Int) = str(k).toDoubleOrNull()?.toInt() ?: def

/* ---------- small shared bits ---------- */

@Composable
fun ToggleRow(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).clickable { onChange(!on) }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold))
            if (sub.isNotEmpty()) Text(sub, style = TextStyle(color = MW.Dim, fontSize = 12.5.sp, lineHeight = 17.sp))
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.width(48.dp).height(28.dp).clip(CircleShape).background(if (on) MW.Accent else Color.White.copy(alpha = 0.18f)).padding(3.dp), contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(Color.White))
        }
    }
}

/* ============================ RECURRING ============================ */

private fun recurNext(r: JsonObject): Long? {
    val t = todayDn()
    val last = r.str("last")
    val from = if (last == todayStr()) t + 1 else t
    for (d in from until from + 400) if (recurDue(r, d)) return d
    return null
}

private fun recurDue(r: JsonObject, d: Long): Boolean {
    val (y, m, day) = com.example.mywallet.data.civilFromDays(d)
    val dim = com.example.mywallet.data.daysInMonth(y, m - 1)
    return when (r.str("freq")) {
        "weekly" -> Math.floorMod(d + 4, 7L) == r.int("wd", 1).toLong()
        "twice" -> day == minOf(r.int("d1", 1).toLong(), dim) || day == minOf(r.int("d2", 15).toLong(), dim)
        else -> day == minOf(r.int("d1", 1).toLong(), dim)
    }
}

private fun recurText(r: JsonObject): String = when (r.str("freq")) {
    "weekly" -> "Every " + WD[r.int("wd", 1).coerceIn(0, 6)]
    "twice" -> "On the " + ord7(r.int("d1", 1)) + " and " + ord7(r.int("d2", 15))
    else -> "On the " + ord7(r.int("d1", 1)) + " each month"
}

@Composable
fun RecurringScreen(s: AppState, store: Store, onClose: () -> Unit) {
    var form by remember { mutableStateOf<JsonObject?>(null) }
    var adding by remember { mutableStateOf(false) }
    ToolShell("Recurring", onClose, "New", { adding = true }) {
        if (s.recurring.isEmpty()) EmptyCard("repeat", "Nothing yet.\nAdd things that repeat, like an allowance, a subscription or weekly fare, and they are logged for you.")
        s.recurring.forEach { r ->
            val nx = recurNext(r)
            val inc = r.str("type") == "income"
            ItemRow(
                if (inc) "💵" else "🔁", r.str("name"),
                recurText(r) + (if (nx != null) " · next " + fmtDate(fromDn(nx)) else ""), MW.Dim, if (inc) MW.Mint else MW.Violet,
                trailing = { Text(s.money(r.dbl("amount")), style = TextStyle(color = if (inc) MW.Mint else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)) },
            ) { form = r }
        }
        if (s.recurring.isNotEmpty()) Text("When you open the app, records that came due since last time are added for you. Delete one from History if it did not happen.", style = TextStyle(color = MW.Faint, fontSize = 12.5.sp, lineHeight = 18.sp))
    }
    if (adding || form != null) RecurForm(s, store, form) { adding = false; form = null }
}

@Composable
private fun RecurForm(s: AppState, store: Store, edit: JsonObject?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val accts = s.accounts.filter { !it.ignored }
    var type by remember { mutableStateOf(edit?.str("type")?.ifEmpty { null } ?: "expense") }
    var name by remember { mutableStateOf(edit?.str("name") ?: "") }
    var amount by remember { mutableStateOf(edit?.let { fmt7(it.dbl("amount")) } ?: "") }
    var acct by remember { mutableStateOf(edit?.str("acct")?.ifEmpty { null } ?: accts.firstOrNull()?.id ?: "") }
    var cat by remember { mutableStateOf(edit?.str("cat") ?: "") }
    var freq by remember { mutableStateOf(edit?.str("freq")?.ifEmpty { null } ?: "monthly") }
    var d1 by remember { mutableStateOf((edit?.int("d1", 1) ?: 1).toString()) }
    var d2 by remember { mutableStateOf((edit?.int("d2", 15) ?: 15).toString()) }
    var wd by remember { mutableStateOf((edit?.int("wd", 1) ?: 1).toString()) }
    var start by remember { mutableStateOf(if (edit != null) fromDn(dn(edit.str("last")) + 1) else todayStr()) }
    var del by remember { mutableStateOf(false) }
    FormPanel(if (edit == null) "New recurring" else "Edit recurring", onClose) {
        FChips("", listOf(opt("expense", "Expense"), opt("income", "Income")), type, { type = it; cat = "" }, scroll = false)
        FText("Name", name, { name = it }, "e.g. Allowance")
        FText("Amount", amount, { amount = it }, "0", decimal = true)
        FChips("Account", accts.map { opt(it.id, it.name, it.icon) }, acct, { acct = it })
        FChips("Category (optional)", listOf(opt("", "None")) + s.cats.filter { it.type == type && !it.ignored }.map { opt(it.id, it.name, it.icon) }, cat, { cat = it })
        FChips("Repeats", listOf(opt("monthly", "Monthly"), opt("twice", "Twice a month"), opt("weekly", "Weekly")), freq, { freq = it })
        if (freq == "weekly") FChips("Day of the week", WD.mapIndexed { i, n -> opt(i.toString(), n.take(3)) }, wd, { wd = it })
        else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FText(if (freq == "twice") "First day (1-31)" else "Day of month (1-31)", d1, { d1 = it.filter { c -> c.isDigit() }.take(2) }, "1", decimal = true, modifier = Modifier.weight(1f))
            if (freq == "twice") FText("Second day", d2, { d2 = it.filter { c -> c.isDigit() }.take(2) }, "15", decimal = true, modifier = Modifier.weight(1f))
        }
        FDate("Starts on", start, { start = it })
        BigButton("Save") {
            val a = num7(amount); val x1 = d1.toIntOrNull() ?: 0; val x2 = d2.toIntOrNull() ?: 0
            when {
                name.isBlank() -> toast7(ctx, "Give it a name")
                a <= 0 -> toast7(ctx, "Enter an amount")
                acct.isEmpty() -> toast7(ctx, "Add an account first")
                freq != "weekly" && x1 !in 1..31 -> toast7(ctx, "Pick a day from 1 to 31")
                freq == "twice" && x2 !in 1..31 -> toast7(ctx, "Pick a day from 1 to 31")
                else -> {
                    var last = fromDn(dn(start) - 1)
                    if (edit != null) {
                        val keep = dn(edit.str("last"))
                        last = if (dn(start) - 1 < keep && dn(start) - 1 >= todayDn() - 1) last else fromDn(maxOf(keep, dn(last)))
                    }
                    val o = buildJsonObject {
                        put("name", name.trim()); put("type", type); put("amount", a); put("acct", acct); put("cat", cat)
                        put("freq", freq); put("d1", x1.coerceIn(1, 31)); put("d2", x2.coerceIn(1, 31)); put("wd", wd.toIntOrNull() ?: 1); put("last", last)
                    }
                    if (edit == null) store.addItem("recurring", JsonObject(o + mapOf("id" to JsonPrimitive(store.newId()))))
                    else store.updateItem("recurring", edit.str("id"), o)
                    val n = store.runRecurring()
                    toast7(ctx, if (n > 0) "Saved · $n record${if (n == 1) "" else "s"} added" else "Saved"); onClose()
                }
            }
        }
        if (edit != null) GhostButton("Delete", Modifier.fillMaxWidth(), Color(0xFFFF8FA0), "trash") { del = true }
    }
    if (del && edit != null) ConfirmDialog("Delete this recurring record?", "Records already added stay.", "Delete", { store.deleteItem("recurring", edit.str("id")); del = false; onClose() }, { del = false })
}

/* ============================ APP LOCK ============================ */

@Composable
private fun PinPad(buf: String, error: String, title: String, sub: String, onKey: (String) -> Unit, onDel: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold), textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(if (error.isNotEmpty()) error else sub, textAlign = TextAlign.Center, style = TextStyle(color = if (error.isNotEmpty()) MW.Rose else MW.Dim, fontSize = 13.5.sp))
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { i -> Box(Modifier.size(16.dp).clip(CircleShape).background(if (i < buf.length) MW.Mint else Color.White.copy(alpha = 0.2f))) }
        }
        Spacer(Modifier.height(26.dp))
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "<")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                row.forEach { k ->
                    if (k.isEmpty()) Spacer(Modifier.size(70.dp))
                    else Box(Modifier.size(70.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.09f)).clickable { if (k == "<") onDel() else onKey(k) }, contentAlignment = Alignment.Center) {
                        if (k == "<") MwIcon("back", Color.White, 24.dp, weight = 2f) else Text(k, style = TextStyle(color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Medium))
                    }
                }
            }
        }
    }
}

/** full-screen lock shown when the app opens */
@Composable
fun LockScreen(store: Store, onUnlock: () -> Unit) {
    var buf by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val bioOn = store.settingStr("bio") == "true" && canUseBiometric(ctx)
    fun askBio() { findActivity(ctx)?.let { showBiometric(it, "Unlock MyWallet") { onUnlock() } } }
    LaunchedEffect(Unit) { if (bioOn) { kotlinx.coroutines.delay(350); askBio() } }
    BackHandler { }
    Box(Modifier.fillMaxSize().background(MW.Bg)) {
        SceneBackground(1f)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { MwIcon("lock", MW.Mint, 26.dp) }
            Spacer(Modifier.height(18.dp))
            PinPad(buf, error, "MyWallet is locked", "Enter your passcode", onKey = { k ->
                if (buf.length < 4) {
                    buf += k; error = ""
                    if (buf.length == 4) {
                        if (store.lockWaitSec() > 0) { buf = ""; error = "Too many tries. Wait ${store.lockWaitSec()} seconds." }
                        else if (store.checkPin(buf)) onUnlock()
                        else { buf = ""; error = if (store.lockWaitSec() > 0) "Too many tries. Wait ${store.lockWaitSec()} seconds." else "Wrong passcode, try again" }
                    }
                }
            }, onDel = { buf = buf.dropLast(1) })
            if (bioOn) {
                Spacer(Modifier.height(6.dp))
                GhostButton("Use fingerprint", Modifier, Color.White, "lock") { askBio() }
            }
        }
    }
}

@Composable
fun AppLockScreen(store: Store, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var on by remember { mutableStateOf(store.lockOn()) }
    var mode by remember { mutableStateOf("") }          // new / confirm / verifyOff / verifyChange
    var first by remember { mutableStateOf("") }
    var buf by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf(store.settingStr("bio") == "true") }
    fun finish() { mode = ""; buf = ""; first = ""; error = "" }
    fun savePin(p: String) {
        store.updateSettings(store.pinSettings(p))
        on = true; finish(); toast7(ctx, "Passcode saved")
    }
    ToolShell("App lock", onClose) {
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { MwIcon("lock", if (on) MW.Mint else MW.Dim, 26.dp) }
            Text(if (on) "App lock is on" else "App lock is off", style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold))
            Text(
                if (on) "MyWallet asks for your 4-digit passcode when you open it, or come back after a while." else "Protect your money with a 4-digit passcode.",
                textAlign = TextAlign.Center, style = TextStyle(color = MW.Dim, fontSize = 13.5.sp, lineHeight = 19.sp),
            )
        }
        if (!on) BigButton("Set a passcode", icon = "lock") { mode = "new"; buf = ""; error = "" }
        else {
            BigButton("Change passcode") { mode = "verifyChange"; buf = ""; error = "" }
            if (canUseBiometric(ctx)) ToggleRow("Unlock with fingerprint", "Skip typing the passcode. The passcode always works too.", bio) { want ->
                if (!want) { store.updateSettings(mapOf("bio" to JsonPrimitive(false))); bio = false }
                else findActivity(ctx)?.let { act -> showBiometric(act, "Turn on fingerprint unlock") { store.updateSettings(mapOf("bio" to JsonPrimitive(true))); bio = true } }
            }
            var secure by remember { mutableStateOf(store.settingStr("secure") == "true") }
            ToggleRow("Hide app preview and block screenshots", "The app looks blank in the recent apps list, and screenshots are blocked.", secure) { want ->
                store.updateSettings(mapOf("secure" to JsonPrimitive(want))); secure = want
                findActivity(ctx)?.window?.let { w -> if (want) w.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) else w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
            }
            GhostButton("Turn off app lock", Modifier.fillMaxWidth(), Color(0xFFFF8FA0), "lock") { mode = "verifyOff"; buf = ""; error = "" }
        }
        Text("The passcode works like in your old MyWallet. If you forget it, restore a backup to get back in.", style = TextStyle(color = MW.Faint, fontSize = 12.5.sp, lineHeight = 18.sp))
    }
    if (mode.isNotEmpty()) {
        val (title, sub) = when (mode) {
            "new" -> "New passcode" to "Pick 4 digits"
            "confirm" -> "Confirm passcode" to "Enter the same 4 digits again"
            "verifyChange" -> "Current passcode" to "Enter it to continue"
            else -> "Current passcode" to "Enter it to turn the lock off"
        }
        FormPanel(if (mode == "verifyOff") "Turn off app lock" else "Passcode", { finish() }) {
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                PinPad(buf, error, title, sub, onKey = { k ->
                    if (buf.length < 4) {
                        buf += k; error = ""
                        if (buf.length == 4) {
                            val p = buf
                            when (mode) {
                                "new" -> { first = p; buf = ""; mode = "confirm" }
                                "confirm" -> if (p == first) savePin(p) else { buf = ""; first = ""; mode = "new"; error = "They did not match, start again" }
                                "verifyChange" -> if (store.checkPin(p)) { buf = ""; mode = "new" } else { buf = ""; error = "Wrong passcode" }
                                else -> if (store.checkPin(p)) {
                                    store.updateSettings(mapOf("lock" to JsonPrimitive(false), "bio" to JsonPrimitive(false), "passHash" to JsonPrimitive(""), "passSalt" to JsonPrimitive("")))
                                    on = false; finish(); toast7(ctx, "App lock is off")
                                } else { buf = ""; error = "Wrong passcode" }
                            }
                        }
                    }
                }, onDel = { buf = buf.dropLast(1) })
            }
        }
    }
}

/* ============================ DISPLAY ============================ */

@Composable
fun DisplayScreen(s: AppState, store: Store, onClose: () -> Unit) {
    var cur by remember { mutableStateOf(s.settings.cur) }
    ToolShell("Display", onClose) {
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Lbl("Preview")
            Spacer(Modifier.height(6.dp))
            MoneyText(s.moneyParts(1234567.891), 34f)
        }
        ThemePicker(s, store)
        FChips("Currency", listOf("₱", "$", "€", "£", "¥", "₹", "₩", "RM", "Rp").map { opt(it, it) }, s.settings.cur, { cur = it; store.updateSettings(mapOf("cur" to JsonPrimitive(it))) })
        FText("Or type your own symbol", cur, { cur = it.take(4); if (it.isNotBlank()) store.updateSettings(mapOf("cur" to JsonPrimitive(it.trim()))) }, "₱")
        FChips("Symbol position", listOf(opt("start", "Before amount"), opt("end", "After amount")), s.settings.pos, { store.updateSettings(mapOf("pos" to JsonPrimitive(it))) }, scroll = false)
        FChips("Decimals", listOf(opt("0", "None"), opt("1", "1"), opt("2", "2")), s.settings.dec.toString(), { store.updateSettings(mapOf("dec" to JsonPrimitive(it.toInt()))) }, scroll = false)
        ToggleRow("Hide amounts", "Show dots instead of money, handy in public.", s.settings.hide) { store.updateSettings(mapOf("hide" to JsonPrimitive(it))) }
        ToggleRow("Spending alerts", "A short note after you save an expense when a budget is nearly used, over, or the day is unusually high.", s.settings.alerts) { store.updateSettings(mapOf("alerts" to JsonPrimitive(it))) }
        Lbl("Home screen", Modifier.padding(top = 8.dp))
        listOf(
            Triple("safe", "Safe-to-spend cards", "The two cards under your balance"),
            Triple("needs", "Coming up", "Bills and debts that are close"),
            Triple("today", "Today's records", "What you logged today"),
            Triple("earlier", "Earlier records", "Your most recent past days"),
        ).forEach { (k, title, sub) ->
            val cur = (s.settings.home?.get(k) as? JsonPrimitive)?.content != "false"
            ToggleRow(title, sub, cur) { v -> store.updateSettings(mapOf("home" to JsonObject((s.settings.home ?: JsonObject(emptyMap())) + (k to JsonPrimitive(v))))) }
        }
    }
}

/* ============================ HELP ============================ */

@Composable
fun HelpScreen(onClose: () -> Unit) {
    val items = listOf(
        Triple("plus", "Add a record", "Tap the green + in the dock. Pick Expense, Income, Transfer or Balance, type the amount (you can use + - × ÷ right in the box), choose an account and category, and save. You can attach a receipt photo too."),
        Triple("landmark", "Bank minimum balance", "On the Money tab tap a bank account to set its rule: the minimum balance and how it is checked. The app then tells you how much is safe to spend and warns before a fee."),
        Triple("flame", "Streak", "Log at least one record a day to keep your flame lit. You get one free missed day each week. Tap the flame at the top to see all 16 flames."),
        Triple("repeat", "Recurring", "Allowance, subscriptions, weekly fare: set them once in Menu > Recurring and they are added for you on the day."),
        Triple("receipt", "Bills, debts and goals", "Menu > Plan. Bills remind you before they are due, debts track who owes who, and goals track what you are saving for."),
        Triple("cloud", "Backup", "Menu > Backup saves one file with everything. Keep a copy somewhere safe. Import restores it and replaces what is on the phone."),
        Triple("lock", "App lock", "Menu > App lock sets a 4-digit passcode. It is the same passcode as in your old MyWallet if you restore its backup."),
    )
    ToolShell("Help", onClose) {
        items.forEach { (ic, t, d) ->
            Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(16.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { MwIcon(ic, MW.Mint, 22.dp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(t, style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    Text(d, style = TextStyle(color = MW.Dim, fontSize = 13.sp, lineHeight = 19.sp))
                }
            }
        }
        Text("MyWallet 1.3 · native", Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center, style = TextStyle(color = MW.Faint, fontSize = 12.sp))
    }
}

/* ============================ REMINDERS ============================ */

@Composable
fun RemindersScreen(store: Store, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var on by remember { mutableStateOf(store.settingStr("remind") == "true") }
    var allowed by remember { mutableStateOf(com.example.mywallet.GentleReminders.canNotify(ctx)) }
    fun turnOn() {
        store.updateSettings(mapOf("remind" to JsonPrimitive(true)))
        on = true; com.example.mywallet.GentleReminders.schedule(ctx)
    }
    val ask = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok ->
        allowed = ok || com.example.mywallet.GentleReminders.canNotify(ctx)
        if (ok) turnOn() else toast7(ctx, "Notifications are blocked for MyWallet")
    }
    ToolShell("Reminders", onClose) {
        ToggleRow("Gentle reminders", "A few friendly nudges a day, at random times between 9 AM and 9 PM. No fixed schedule to manage.", on) { want ->
            if (!want) {
                store.updateSettings(mapOf("remind" to JsonPrimitive(false))); on = false; com.example.mywallet.GentleReminders.cancel(ctx)
            } else if (android.os.Build.VERSION.SDK_INT >= 33 && !com.example.mywallet.GentleReminders.canNotify(ctx)) ask.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            else turnOn()
        }
        Text(
            "They remind you about bills and debts that are close, a bank that needs a top-up, or your streak. When there is nothing urgent, you just get a light nudge to log your day. Notifications never show amounts.",
            style = TextStyle(color = MW.Dim, fontSize = 13.sp, lineHeight = 19.sp),
        )
        if (on && !allowed && android.os.Build.VERSION.SDK_INT >= 33) Text("Notifications are turned off for MyWallet in your phone settings, so nothing will show up.", style = TextStyle(color = MW.Rose, fontSize = 13.sp, lineHeight = 19.sp))
        GhostButton("Send a test notification", Modifier.fillMaxWidth(), Color.White, "bell") {
            if (!com.example.mywallet.GentleReminders.canNotify(ctx)) {
                if (android.os.Build.VERSION.SDK_INT >= 33) ask.launch(android.Manifest.permission.POST_NOTIFICATIONS) else toast7(ctx, "Notifications are turned off for MyWallet")
            } else {
                val (t, m) = com.example.mywallet.GentleReminders.message(null); com.example.mywallet.GentleReminders.show(ctx, t, m); toast7(ctx, "Sent")
            }
        }
        Text("If nothing arrives, open phone Settings > Apps > MyWallet > Battery and choose Unrestricted.", style = TextStyle(color = MW.Faint, fontSize = 12.5.sp, lineHeight = 18.sp))
    }
}
