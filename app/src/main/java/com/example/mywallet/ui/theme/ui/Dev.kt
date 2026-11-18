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
import com.example.mywallet.data.fromDn
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
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold))
        if (sub != null) Text(sub, style = TextStyle(color = MW.Dim, fontSize = 12.5.sp, lineHeight = 18.sp))
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
fun DevScreen(s: AppState, store: Store, onClose: () -> Unit, onOpen: (String) -> Unit, onCelebrate: (Int, Int) -> Unit, onTurnOff: () -> Unit) {
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
                    Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.07f)).clickableNoRipple { onCelebrate(maxOf(0, d - 1), maxOf(1, d)) }.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        FlameIcon(i, 52.dp)
                        Text(if (d == 0) "Cold" else d.toString() + "d", style = TextStyle(color = MW.Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                FText("From", from, { from = it.filter { c -> c.isDigit() }.take(4) }, "19", modifier = Modifier.weight(1f))
                FText("To", to, { to = it.filter { c -> c.isDigit() }.take(4) }, "20", modifier = Modifier.weight(1f))
                GhostButton("Play") { onCelebrate(from.toIntOrNull() ?: 0, to.toIntOrNull() ?: 1) }
            }
        }
        DevCard("Level preview", "You have ${s.xp()} records. Type a number of records to see its level.") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                FText("Records", lvXp, { lvXp = it.filter { c -> c.isDigit() }.take(6) }, "250", modifier = Modifier.weight(1f))
                val li = levelInfoFor(lvXp.toIntOrNull() ?: s.xp())
                Text("Level ${li.level}\n${li.title}", style = TextStyle(color = MW.Mint, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold), modifier = Modifier.padding(bottom = 12.dp))
            }
            Btns(*listOf(0, 8, 50, 150, 500, 1000, 3000, 6000, 20000).map { v -> v.toString() to { lvXp = v.toString() } }.toTypedArray())
            Text(LEVEL_TITLES.joinToString("\n") { (l, n) -> "Level $l  $n  (${xpForLevel(l)} records)" }, style = TextStyle(color = MW.Faint, fontSize = 12.sp, lineHeight = 17.sp))
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
                "Open Rewind" to { onOpen("recap") }, "Open Year in review" to { onOpen("year") }, "Open Badges" to { onOpen("badges") }, "Open Streak" to { onOpen("streak") },
            )
            val rm = remember(s.rev) { GentleReminders.message(s) }
            Text("Reminder preview: " + rm.first + " — " + rm.second, style = TextStyle(color = MW.Dim, fontSize = 12.5.sp, lineHeight = 18.sp))
        }
        DevCard("UI elements", "Everything the app is built from, in one place.") {
            BigButton("Big button", icon = "plus") { dt(ctx, "Big button") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Ghost", icon = "copy") { dt(ctx, "Ghost") }
                GhostButton("Danger", color = MW.Rose, icon = "trash") { dt(ctx, "Danger") }
            }
            FChips("Chips", listOf(opt("a", "First"), opt("b", "Second"), opt("c", "Third")), chip, { chip = it })
            ToggleRow("Toggle row", "A switch with a short description.", demoOn) { demoOn = it }
            ItemRow("🍜", "Item row", "With a subtitle and a trailing amount", trailing = { Text("₱120.00", style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)) }) { dt(ctx, "Row tapped") }
            EmptyCard("sparkles", "An empty card looks like this.")
            Btns("Open bottom panel" to { demoForm = true }, "Open confirm dialog" to { demoConfirm = true }, if (showIcons) ("Hide icons" to { showIcons = false }) else ("Show all icons" to { showIcons = true }))
            if (showIcons) {
                com.example.mywallet.ui.allIconNames().chunked(5).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { n ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                MwIcon(n, Color.White, 26.dp)
                                Text(n, maxLines = 1, style = TextStyle(color = MW.Faint, fontSize = 9.sp))
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
                style = TextStyle(color = MW.Dim, fontSize = 13.sp, lineHeight = 19.sp),
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
        Text("Forms slide up like this.", style = TextStyle(color = MW.Dim, fontSize = 14.sp))
        FText("A text box", "", { }, "Type here")
        BigButton("Close") { demoForm = false }
    }
}

/** clickable without a ripple, used for small tiles */
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    this.clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null) { onClick() }
}
