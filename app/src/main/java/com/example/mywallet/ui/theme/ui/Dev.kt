package com.example.mywallet.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.composed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.GentleReminders
import com.example.mywallet.LaunchBus
import com.example.mywallet.data.AppState
import com.example.mywallet.data.BADGES
import com.example.mywallet.data.DAY_SHIFT
import com.example.mywallet.data.FLAME_DAYS
import com.example.mywallet.data.LEVEL_TITLES
import com.example.mywallet.data.Store
import com.example.mywallet.data.earnedBadges
import com.example.mywallet.data.bestStreak
import com.example.mywallet.data.fromDn
import com.example.mywallet.data.guessCat
import com.example.mywallet.data.loggedDays
import com.example.mywallet.data.streakWarn
import com.example.mywallet.data.levelInfoFor
import com.example.mywallet.data.streak
import com.example.mywallet.data.todayDn
import com.example.mywallet.data.todayStr
import com.example.mywallet.data.xp
import com.example.mywallet.data.xpForLevel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private fun dt(ctx: Context, m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()

@Composable
private fun DevCard(title: String, sub: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().glass(MwShape(24.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MwTs(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold))
        if (sub != null) Text(sub, style = MwTs(color = MW.Dim, fontSize = 12.5.sp, lineHeight = 18.sp))
        content()
    }
}

/** a row of small buttons that scrolls sideways */
@Composable
private fun Btns(vararg items: Pair<String, () -> Unit>) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (n, f) -> GhostButton(n, onClick = f) }
    }
}

private val SAMPLE_NOTES = listOf("Lunch", "Grab ride", "Groceries #food", "Coffee", "Load", "Netflix #subs", "Notebook #school", "Snacks", "Jeep fare", "Milk tea #treat")

@Composable
fun DevScreen(s: AppState, store: Store, onClose: () -> Unit, onOpen: (String) -> Unit, onCelebrate: (Int, Int) -> Unit, onPlay: (String, Int) -> Unit, onTurnOff: () -> Unit) {
    val ctx = LocalContext.current
    var shift by remember { mutableStateOf(DAY_SHIFT) }
    var from by remember { mutableStateOf("19") }
    var to by remember { mutableStateOf("20") }
    var lvXp by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var demoForm by remember { mutableStateOf(false) }
    var demoConfirm by remember { mutableStateOf(false) }
    var demoOn by remember { mutableStateOf(true) }
    var chip by remember { mutableStateOf("a") }
    var showIcons by remember { mutableStateOf(false) }
    var popN by remember { mutableStateOf("12") }
    var guessNote by remember { mutableStateOf("") }
    var guessType by remember { mutableStateOf("expense") }
    var dbg by remember { mutableStateOf(0) }
    val prefs = remember { ctx.getSharedPreferences("mw_prefs", 0) }

    /** adds one small record on each of the given days (skips days that already have one) */
    fun logDays(days: List<Long>, label: String) {
        val acct = s.accounts.firstOrNull { !it.ignored }?.id ?: ""
        val cat = s.cats.firstOrNull { it.type == "expense" && !it.ignored }?.id ?: ""
        if (acct.isEmpty() || cat.isEmpty()) { dt(ctx, "Needs an account and an expense category"); return }
        val have = s.loggedDays()
        val base = System.nanoTime().toString(36)
        val list = days.filter { it !in have }.mapIndexed { i, d ->
            buildJsonObject {
                put("id", "ds$base$i"); put("ts", System.currentTimeMillis() - i); put("type", "expense"); put("acct", acct); put("to", "")
                put("cat", cat); put("amount", 25.0); put("date", fromDn(d)); put("note", "Dev streak")
            }
        }
        if (list.isNotEmpty()) store.appendLists(mapOf("recs" to list))
        dt(ctx, label + " (" + list.size + " records added)")
    }

    fun setShift(v: Int) {
        DAY_SHIFT = v; shift = v
        store.runRecurring(); store.touch()
    }

    fun addSamples(n: Int) {
        val accts = s.accounts.filter { !it.ignored }
        val ex = s.cats.filter { it.type == "expense" && !it.ignored }
        val inc = s.cats.filter { it.type == "income" && !it.ignored }
        if (accts.isEmpty() || ex.isEmpty()) { dt(ctx, "Needs an account and an expense category"); return }
        val base = System.nanoTime().toString(36)
        val now = System.currentTimeMillis()
        val list = (0 until n).map { i ->
            val isInc = inc.isNotEmpty() && (0..9).random() == 0
            val c = if (isInc) inc.random() else ex.random()
            buildJsonObject {
                put("id", "dv$base$i"); put("ts", now - i); put("type", if (isInc) "income" else "expense"); put("acct", accts.random().id)
                put("to", ""); put("cat", c.id); put("amount", if (isInc) (500..4000).random().toDouble() else (20..900).random().toDouble())
                put("date", fromDn(todayDn() - (0..120).random())); put("note", if (isInc) "Allowance" else SAMPLE_NOTES.random())
            }
        }
        store.appendLists(mapOf("recs" to list))
        dt(ctx, "Added $n records")
    }

    ToolShell("Developer tools", onClose) {
        DevCard("Time travel", "App date: " + todayStr() + (if (shift != 0) "  (${if (shift > 0) "+" else ""}$shift days from real)" else "  (real date)") + ". Bills, streaks, paydays and budgets follow it. It resets when you close the app.") {
            Btns("-30d" to { setShift(shift - 30) }, "-7d" to { setShift(shift - 7) }, "-1d" to { setShift(shift - 1) }, "+1d" to { setShift(shift + 1) }, "+7d" to { setShift(shift + 7) }, "+30d" to { setShift(shift + 30) }, "Reset" to { setShift(0) })
        }
        DevCard("Streak and flames", "Your real streak is ${s.streak()} days. Tap a flame to play its New Day moment.") {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FLAME_DAYS.forEachIndexed { i, d ->
                    Column(Modifier.clip(MwShape(16.dp)).background(Color.White.copy(alpha = 0.07f)).clickableNoRipple { onCelebrate(maxOf(0, d - 1), maxOf(1, d)) }.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        FlameIcon(i, 52.dp)
                        Text(if (d == 0) "Cold" else d.toString() + "d", style = MwTs(color = MW.Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                FText("From", from, { from = it.filter { c -> c.isDigit() }.take(4) }, "19", modifier = Modifier.weight(1f))
                FText("To", to, { to = it.filter { c -> c.isDigit() }.take(4) }, "20", modifier = Modifier.weight(1f))
                GhostButton("Play") { onCelebrate(from.toIntOrNull() ?: 0, to.toIntOrNull() ?: 1) }
            }
        }
        DevCard("Pop-ups", "Play any of the full-screen moments right now. The once-a-day rule does not apply here.") {
            FText("Streak number", popN, { popN = it.filter { c -> c.isDigit() }.take(4) }, "12")
            val n = popN.toIntOrNull()?.coerceAtLeast(1) ?: 12
            Btns(
                "New day" to { onCelebrate(maxOf(0, n - 1), n) },
                "Missed a day" to { onPlay("warnFree", n) },
                "No free day left" to { onPlay("warnNone", n) },
                "Ended: random" to { onPlay("ended", maxOf(2, n)) },
                "Ended: water" to { onPlay("ended0", maxOf(2, n)) },
                "Ended: sand" to { onPlay("ended1", maxOf(2, n)) },
                "Ended: wind" to { onPlay("ended2", maxOf(2, n)) },
                "Ended: rain" to { onPlay("ended3", maxOf(2, n)) },
            )
            Btns(
                "Reset once-a-day flags" to { prefs.edit().putLong("warnDay", -1L).putInt("streakSeen", s.streak()).apply(); dbg++; dt(ctx, "Pop-ups can show again next time the app opens") },
                "Pretend streak was $n" to { prefs.edit().putInt("streakSeen", maxOf(2, n)).apply(); dbg++; dt(ctx, "If your streak is 0 now, reopening the app shows Streak ended") },
            )
        }
        DevCard("Streak scenarios", "Builds real records so you can walk through the real rules. Tip: make a streak up to yesterday, then use Time travel +1d (missed a day pop-up) and +2d (streak ended).") {
            Btns(
                "10-day streak to today" to { val t = todayDn(); logDays((0L..9L).map { t - it }, "Streak through today"); prefs.edit().putInt("streakSeen", 10).apply() },
                "10-day streak to yesterday" to { val t = todayDn(); logDays((1L..10L).map { t - it }, "Streak through yesterday"); prefs.edit().putInt("streakSeen", 10).apply() },
                "Log today only" to { logDays(listOf(todayDn()), "Logged today") },
            )
            Btns(
                "Delete last 2 days" to { confirm = "Delete every record from yesterday and today?" to { store.deleteRecsSince(fromDn(todayDn() - 1)) } },
            )
        }
        DevCard("Palettes", "Tap to switch. Everything should change color. Current: " + MwTheme.palette.name + " / theme: " + MwStyle.cur.name) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PALETTES.forEach { p ->
                    Column(Modifier.clip(MwShape(14.dp)).background(Color.White.copy(alpha = 0.07f)).clickableNoRipple { store.updateSettings(mapOf("palette" to JsonPrimitive(p.id))) }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(Modifier.width(70.dp).clip(MwShape(8.dp))) {
                            listOf(p.c1, p.c2, p.c3).forEach { c -> Box(Modifier.weight(1f).size(10.dp, 22.dp).background(c)) }
                        }
                        Text(p.name, maxLines = 1, style = MwTs(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
                    }
                }
            }
            Btns(*UI_STYLES.map { st -> st.name to { store.updateSettings(mapOf("uiTheme" to JsonPrimitive(st.id))) } }.toTypedArray())
            Btns("Open display settings" to { onOpen("display") })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Bg" to MW.Bg, "Accent" to MW.Accent, "Mint" to MW.Mint, "Tint" to MW.Tint, "On accent" to MW.OnAccent).forEach { (n, c) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(40.dp, 28.dp).clip(MwShape(8.dp)).background(c))
                        Text(n, style = MwTs(color = MW.Faint, fontSize = 10.sp))
                    }
                }
            }
        }
        DevCard("Debts and bills", "Part payments, overdue and paid bills, to check the Bills and Debts screens.") {
            Btns(
                "+ part-paid debt (they owe)" to {
                    val t = todayStr()
                    store.appendLists(mapOf("debts" to listOf(buildJsonObject {
                        put("id", store.newId() + "p1"); put("who", "Ana"); put("amount", 300.0); put("theyOweMe", true); put("date", t); put("due", fromDn(todayDn() + 5)); put("note", "Concert ticket"); put("paid", false)
                        put("pays", JsonArray(listOf(buildJsonObject { put("date", t); put("amount", 250.0) })))
                    })))
                    dt(ctx, "Ana owes you 50 of 300")
                },
                "+ part-paid debt (you owe)" to {
                    val t = todayStr()
                    store.appendLists(mapOf("debts" to listOf(buildJsonObject {
                        put("id", store.newId() + "p2"); put("who", "Ben"); put("amount", 1000.0); put("theyOweMe", false); put("date", t); put("due", fromDn(todayDn() - 1)); put("note", "Borrowed"); put("paid", false)
                        put("pays", JsonArray(listOf(buildJsonObject { put("date", t); put("amount", 400.0) }, buildJsonObject { put("date", t); put("amount", 100.0) })))
                    })))
                    dt(ctx, "You owe Ben 500 of 1000 (overdue)")
                },
                "+ settled debt" to {
                    store.appendLists(mapOf("debts" to listOf(buildJsonObject {
                        put("id", store.newId() + "p3"); put("who", "Cara"); put("amount", 150.0); put("theyOweMe", true); put("date", todayStr()); put("due", ""); put("note", "Snacks"); put("paid", true)
                        put("pays", JsonArray(listOf(buildJsonObject { put("date", todayStr()); put("amount", 150.0) })))
                    })))
                },
            )
            val acct = s.accounts.firstOrNull { !it.ignored }?.id ?: ""
            val cat = s.cats.firstOrNull { it.type == "expense" && !it.ignored }?.id ?: ""
            fun bill(name: String, amt: Double, day: Int, paid: List<String>) = buildJsonObject {
                put("id", store.newId() + name.length); put("name", name); put("amount", amt); put("day", day); put("cat", cat); put("acct", acct); put("paid", JsonArray(paid.map { JsonPrimitive(it) }))
            }
            Btns(
                "+ bill due in 2 days" to { store.appendLists(mapOf("bills" to listOf(bill("Internet", 999.0, com.example.mywallet.data.civilFromDays(todayDn() + 2).third.toInt(), emptyList())))) },
                "+ overdue bill" to { store.appendLists(mapOf("bills" to listOf(bill("Water", 350.0, com.example.mywallet.data.civilFromDays(todayDn() - 3).third.toInt(), emptyList())))) },
                "+ paid this month" to { store.appendLists(mapOf("bills" to listOf(bill("Gym", 500.0, 1, listOf(todayStr().take(7)))))) },
                "Clear bills" to { confirm = "Delete all bills?" to { store.setList("bills", emptyList()) } },
            )
        }
        DevCard("Category guess", "Type a note (or a #tag) to see which category the app would pick.") {
            FText("Note", guessNote, { guessNote = it.take(120) }, "e.g. lunch, #food, grab")
            FChips("", listOf(opt("expense", "Expense"), opt("income", "Income")), guessType, { guessType = it }, scroll = false)
            val g = if (guessNote.isBlank()) null else s.guessCat(guessNote, guessType)
            val c = g?.let { id -> s.cats.firstOrNull { it.id == id } }
            Text(if (guessNote.isBlank()) "Waiting for a note" else if (c == null) "No guess (category stays empty)" else "Picks: " + c.icon + " " + c.name, style = MwTs(color = MW.Mint, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold))
        }
        DevCard("State") {
            val w = remember(s.rev, s.recs.size, shift, dbg) { s.streakWarn() }
            Text(
                "Today: " + todayStr() + "\nStreak: " + s.streak() + " (best " + s.bestStreak() + ")\n" +
                    "Missed-day warning: " + (if (w == null) "none" else if (w.missedYesterday) "missed yesterday, free day used" else "free day already used") + "\n" +
                    "Saved streak for 'ended': " + prefs.getInt("streakSeen", 0) + "\nLast warning day: " + prefs.getLong("warnDay", -1L) + "\nTheme: " + s.settings.palette + (if (s.settings.palette == "custom") " " + s.settings.pal else ""),
                style = MwTs(color = MW.Dim, fontSize = 13.sp, lineHeight = 19.sp),
            )
        }
        DevCard("Level preview", "You have ${s.xp()} records. Type a number of records to see its level.") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                FText("Records", lvXp, { lvXp = it.filter { c -> c.isDigit() }.take(6) }, "250", modifier = Modifier.weight(1f))
                val li = levelInfoFor(lvXp.toIntOrNull() ?: s.xp())
                Text("Level ${li.level}\n${li.title}", style = MwTs(color = MW.Mint, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold), modifier = Modifier.padding(bottom = 12.dp))
            }
            Btns(*listOf(0, 8, 50, 150, 500, 1000, 3000, 6000, 20000).map { v -> v.toString() to { lvXp = v.toString() } }.toTypedArray())
            Text(LEVEL_TITLES.joinToString("\n") { (l, n) -> "Level $l  $n  (${xpForLevel(l)} records)" }, style = MwTs(color = MW.Faint, fontSize = 12.sp, lineHeight = 17.sp))
        }
        DevCard("Sample data") {
            Btns("+30 records" to { addSamples(30) }, "+200 records" to { addSamples(200) }, "+3 debts" to {
                val t = todayStr()
                fun d(who: String, a: Double, they: Boolean, due: String, note: String) = buildJsonObject {
                    put("id", store.newId() + who.length); put("who", who); put("amount", a); put("theyOweMe", they); put("date", t); put("due", due); put("note", note); put("paid", false)
                }
                store.appendLists(mapOf("debts" to listOf(d("Mika", 250.0, true, fromDn(todayDn() + 3), "Lunch"), d("Kuya Ben", 1200.0, false, fromDn(todayDn() - 2), "Boarding"), d("Jo", 80.0, true, "", "Load"))))
                dt(ctx, "Added 3 debts")
            })
            Btns(
                "Clear debts" to { confirm = "Delete all debts?" to { store.setList("debts", emptyList()) } },
                "Delete all records" to { confirm = "Delete ALL records? Accounts and categories stay." to { store.setList("recs", emptyList()) } },
                "Clear receipt photos" to { confirm = "Delete all receipt photos?" to { store.clearPhotos(); store.touch() } },
            )
        }
        DevCard("Badges, backup and challenge") {
            Btns(
                "Unlock all badges" to { store.setTops(mapOf("unlocked" to JsonArray(BADGES.map { JsonPrimitive(it.id) }), "badgeInit" to JsonPrimitive(true))); Fx.fire("All badges unlocked") },
                "Back to real badges" to { store.setTops(mapOf("unlocked" to JsonArray(s.earnedBadges().map { JsonPrimitive(it) }), "badgeInit" to JsonPrimitive(true))); dt(ctx, "Badges reset to real") },
            )
            Btns(
                "Fake 40-day-old backup" to { store.setTops(mapOf("lastBackup" to JsonPrimitive(System.currentTimeMillis() - 40L * 86_400_000L), "backupSnooze" to JsonPrimitive(0))); dt(ctx, "Check the banner on Home") },
                "Remove backup date" to { store.setTops(mapOf("lastBackup" to JsonPrimitive(0), "backupSnooze" to JsonPrimitive(0))) },
            )
        }
        DevCard("Actions") {
            Btns(
                "Lock now" to { if (store.lockOn()) LaunchBus.lockNow = true else dt(ctx, "Set a passcode in App lock first") },
                "Run recurring now" to { dt(ctx, "Added ${store.runRecurring()} recurring records") },
                "Ask my name again" to { store.updateSettings(mapOf("name" to JsonPrimitive(""), "nameAsked" to JsonPrimitive(false))); dt(ctx, "It asks again next time the app opens") },
            )
            Btns(
                "Confetti" to { Fx.fire(null) },
                "Banner" to { Fx.say("This is a test message") },
                "Test notification" to {
                    val m = GentleReminders.message(s)
                    GentleReminders.show(ctx, m.first, m.second)
                },
            )
            Btns(
                "Open Bills" to { onOpen("bills") }, "Open Debts" to { onOpen("debts") }, "Open Display" to { onOpen("display") }, "Open Rewind" to { onOpen("recap") }, "Open Year in review" to { onOpen("year") }, "Open Badges" to { onOpen("badges") }, "Open Streak" to { onOpen("streak") },
            )
            val rm = remember(s.rev) { GentleReminders.message(s) }
            Text("Reminder preview: " + rm.first + " — " + rm.second, style = MwTs(color = MW.Dim, fontSize = 12.5.sp, lineHeight = 18.sp))
        }
        DevCard("UI elements", "Everything the app is built from, in one place.") {
            BigButton("Big button", icon = "plus") { dt(ctx, "Big button") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Ghost", icon = "copy") { dt(ctx, "Ghost") }
                GhostButton("Danger", color = MW.Rose, icon = "trash") { dt(ctx, "Danger") }
            }
            FChips("Chips", listOf(opt("a", "First"), opt("b", "Second"), opt("c", "Third")), chip, { chip = it })
            ToggleRow("Toggle row", "A switch with a short description.", demoOn) { demoOn = it }
            ItemRow("🍜", "Item row", "With a subtitle and a trailing amount", trailing = { Text("₱120.00", style = MwTs(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)) }) { dt(ctx, "Row tapped") }
            EmptyCard("sparkles", "An empty card looks like this.")
            Btns("Open bottom panel" to { demoForm = true }, "Open confirm dialog" to { demoConfirm = true }, if (showIcons) ("Hide icons" to { showIcons = false }) else ("Show all icons" to { showIcons = true }))
            if (showIcons) {
                com.example.mywallet.ui.allIconNames().chunked(5).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { n ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                MwIcon(n, Color.White, 26.dp)
                                Text(n, maxLines = 1, style = MwTs(color = MW.Faint, fontSize = 9.sp))
                            }
                        }
                        repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        DevCard("Storage") {
            Text(
                "${s.recs.size} records · ${s.accounts.size} accounts · ${s.cats.size} categories · ${s.debts.size} debts · ${s.bills.size} bills · ${s.goals.size} goals · ${store.photoCount()} photos\nData file: ${store.fileSize() / 1024} KB",
                style = MwTs(color = MW.Dim, fontSize = 13.sp, lineHeight = 19.sp),
            )
            Btns("Copy data as JSON" to {
                val t = store.exportJson() ?: ""
                (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("MyWallet data", t))
                dt(ctx, "Copied (${t.length / 1024} KB)")
            })
        }
        GhostButton("Turn off developer mode", Modifier.fillMaxWidth(), MW.Rose) { setShift(0); onTurnOff() }
    }

    confirm?.let { c -> ConfirmDialog("Are you sure?", c.first, "Do it", { c.second(); confirm = null }, { confirm = null }) }
    if (demoConfirm) ConfirmDialog("Confirm dialog", "This is how a warning looks.", "OK", { demoConfirm = false }, { demoConfirm = false })
    if (demoForm) FormPanel("Bottom panel", { demoForm = false }) {
        Text("Forms slide up like this.", style = MwTs(color = MW.Dim, fontSize = 14.sp))
        FText("A text box", "", { }, "Type here")
        BigButton("Close") { demoForm = false }
    }
}

/** clickable without a ripple, used for small tiles */
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    this.clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null) { onClick() }
}
