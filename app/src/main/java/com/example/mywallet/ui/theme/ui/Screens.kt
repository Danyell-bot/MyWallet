package com.example.mywallet.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.example.mywallet.data.AppState
import com.example.mywallet.data.Rec
import com.example.mywallet.data.Store
import com.example.mywallet.data.acc
import com.example.mywallet.data.cat
import com.example.mywallet.data.dn
import com.example.mywallet.data.levelInfo
import com.example.mywallet.data.flameTier
import com.example.mywallet.data.loggedDays
import com.example.mywallet.data.money
import com.example.mywallet.data.streak
import com.example.mywallet.data.todayDn
import com.example.mywallet.data.todayStr
import kotlinx.serialization.json.JsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun dateHeader(date: String): String {
    val d = dn(date) - todayDn()
    if (d == 0L) return "Today"
    if (d == -1L) return "Yesterday"
    val f = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    f.timeZone = TimeZone.getTimeZone("UTC")
    return f.format(Date(dn(date) * 86400000L))
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).glass(CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) {
            MwIcon("back", Color.White, 22.dp, weight = 2f)
        }
        Text(title, Modifier.weight(1f), textAlign = TextAlign.Center, style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold))
        if (trailing != null) trailing() else Spacer(Modifier.size(40.dp))
    }
}

@Composable
private fun Tile(modifier: Modifier, icon: String, title: String, sub: String, onClick: () -> Unit) {
    Column(modifier.height(112.dp).glass(RoundedCornerShape(24.dp)).clickable { onClick() }.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Text(icon, fontSize = 26.sp)
        Column {
            Text(title, maxLines = 1, style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold))
            Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = MW.Dim, fontSize = 12.sp))
        }
    }
}

/* ============================ MENU ============================ */

private fun MT(icon: String, label: String, go: () -> Unit) = Triple(icon, label, go)

@Composable
private fun MenuTile(modifier: Modifier, icon: String, label: String, tint: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.height(94.dp).glassLite(shape).clickable { onClick() }.padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(15.dp))
                .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.36f), tint.copy(alpha = 0.10f))))
                .border(1.dp, Brush.linearGradient(listOf(tint.copy(alpha = 0.75f), tint.copy(alpha = 0.12f))), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) { MwIcon(icon, lerpTint(tint), 22.dp) }
        Spacer(Modifier.height(8.dp))
        Text(label, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 13.sp))
    }
}

private fun lerpTint(c: Color): Color = androidx.compose.ui.graphics.lerp(c, Color.White, 0.55f)


@Composable
fun MenuScreen(s: AppState, store: Store, scroll: androidx.compose.foundation.ScrollState, onOpen: (String) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    BackHandler { onClose() }
    var rename by remember { mutableStateOf(false) }
    var taps by remember { mutableStateOf(0) }
    var devOn by remember { mutableStateOf(ctx.getSharedPreferences("mw_prefs", 0).getBoolean("dev", false)) }

    val name = s.settings.name.trim()
    val st = s.streak()
    val lit = s.loggedDays().contains(todayDn())
    val tier = if (st > 0 && !lit) 0 else flameTier(st)

    val haze = rememberHazeState()
    Box(Modifier.fillMaxSize().background(MW.Bg)) {
        SceneBackground(1f, Modifier.hazeSource(haze))
        CompositionLocalProvider(LocalHaze provides haze) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp).verticalScroll(scroll)) {
            TopBar("Menu", onClose, trailing = {
                Box(Modifier.size(40.dp).glass(CircleShape).clickable { onOpen("help") }, contentAlignment = Alignment.Center) { MwIcon("help", Color.White, 22.dp, weight = 2f) }
            })
            // profile card
            val li = s.levelInfo()
            val prog = li.progress
            Column(
                Modifier.fillMaxWidth().glass(RoundedCornerShape(30.dp), tint = MW.Tint).clickable { rename = true }.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FlameIcon(tier, 64.dp, Modifier.clickable(remember { MutableInteractionSource() }, null) { onOpen("streak") })
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name.ifEmpty { "Add your name" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold))
                        Text("Level ${li.level} \u00B7 ${li.title}", style = TextStyle(color = MW.Dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
                    }
                    Box(
                        Modifier.padding(horizontal = 8.dp).size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MW.Amber.copy(alpha = 0.35f), MW.Amber.copy(alpha = 0.12f))))
                            .border(1.dp, MW.Amber.copy(alpha = 0.6f), CircleShape).clickable { onOpen("badges") },
                        contentAlignment = Alignment.Center,
                    ) { MwIcon("award", MW.Amber, 22.dp, weight = 2f) }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$st", style = numStyle(34f, Color.White))
                        Text(if (st == 1) "day streak" else "day streak", style = TextStyle(color = MW.Dim, fontSize = 11.5.sp))
                    }
                }
                Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f))) {
                    Box(Modifier.fillMaxWidth(prog).height(7.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(MW.Accent, MW.Mint))))
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "${li.left} more record" + (if (li.left == 1) "" else "s") + " to level ${li.level + 1}",
                        Modifier.weight(1f), style = TextStyle(color = MW.Dim, fontSize = 12.sp),
                    )
                    Text("${FLAME_NAMES[tier]} \u00B7 one free freeze a week", style = TextStyle(color = MW.Dim, fontSize = 12.sp))
                }
            }
            @Composable fun Section(title: String, items: List<Triple<String, String, () -> Unit>>, tints: List<Color>) {
                Lbl(title, Modifier.padding(top = 22.dp, bottom = 10.dp))
                items.chunked(4).forEachIndexed { r, row ->
                    Row(Modifier.fillMaxWidth().padding(top = if (r > 0) 10.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEachIndexed { i, (icon, label, go) -> MenuTile(Modifier.weight(1f), icon, label, tints[(r * 4 + i) % tints.size], go) }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            val green = MW.Accent; val sky = Color(0xFF9CC4FF); val amber = Color(0xFFFFCB7A); val violet = Color(0xFFB9A6FF); val rose = Color(0xFFFF9FB0)
            Section("Plan", listOf(
                MT("receipt", "Bills") { onOpen("bills") }, MT("swap", "Debts") { onOpen("debts") },
                MT("target", "Goals") { onOpen("goals") }, MT("wallet", "Budgets") { onOpen("budgets") },
                MT("coin", "Challenge") { onOpen("challenge") },
            ), listOf(amber, rose, green, sky, amber))
            Section("Insights", listOf(
                MT("pie", "Analysis") { onOpen("analysis") }, MT("sparkles", "Rewind") { onOpen("recap") },
                MT("tag", "Tags") { onOpen("tags") }, MT("image", "Receipts") { onOpen("receipts") },
            ), listOf(green, amber, violet, sky))
            Section("Manage", listOf(
                MT("landmark", "Categories") { onOpen("cats") }, MT("zap", "Quick add") { onOpen("quick") },
                MT("repeat", "Recurring") { onOpen("recurring") }, MT("calendar", "Payday") { onOpen("payday") },
            ), listOf(violet, amber, green, sky))
            Section("Settings and data", buildList {
                add(MT("eye", "Display") { onOpen("display") }); add(MT("lock", "App lock") { onOpen("lock") })
                add(MT("bell", "Reminders") { onOpen("reminders") }); add(MT("cloud", "Backup") { onOpen("backup") })
                if (devOn) add(MT("code", "Developer") { onOpen("dev") })
            }, listOf(rose, sky, amber, violet))
            Spacer(Modifier.height(26.dp))
            Text(
                "MyWallet 1.3 \u00B7 native", Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, null) {
                    taps += 1
                    if (!devOn && taps >= 7) {
                        devOn = true; taps = 0
                        ctx.getSharedPreferences("mw_prefs", 0).edit().putBoolean("dev", true).apply()
                        Toast.makeText(ctx, "Developer mode is on. Find it under Settings and data.", Toast.LENGTH_LONG).show()
                    } else if (!devOn && taps >= 3) Toast.makeText(ctx, "${7 - taps} more taps for developer mode", Toast.LENGTH_SHORT).show()
                },
                textAlign = TextAlign.Center, style = TextStyle(color = MW.Faint, fontSize = 12.sp),
            )
            Spacer(Modifier.height(24.dp))
        }
        }
    }

    if (rename) {
        var text by remember { mutableStateOf(name) }
        androidx.compose.ui.window.Dialog(onDismissRequest = { rename = false }) {
            Column(Modifier.popIn().fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xFF0E1426)).border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(28.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Your name", style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)).padding(16.dp)) {
                    BasicTextField(text, { text = it.take(24) }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(color = Color.White, fontSize = 16.sp), cursorBrush = SolidColor(MW.Accent))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.1f)).clickable { rename = false }, contentAlignment = Alignment.Center) {
                        Text("Cancel", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold))
                    }
                    Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(MW.Accent).clickable {
                        store.updateSettings(mapOf("name" to JsonPrimitive(text.trim()), "nameAsked" to JsonPrimitive(true))); rename = false
                    }, contentAlignment = Alignment.Center) { Text("Save", style = TextStyle(color = MW.OnAccent, fontWeight = FontWeight.ExtraBold)) }
                }
            }
        }
    }
}

/* ============================ SEARCH / HISTORY ============================ */

@Composable
fun SearchScreen(s: AppState, store: Store, receiptsOnly: Boolean, onEdit: (Rec) -> Unit, onClose: () -> Unit) {
    BackHandler { onClose() }
    var q by remember { mutableStateOf(com.example.mywallet.LaunchBus.query ?: "") }
    androidx.compose.runtime.LaunchedEffect(Unit) { com.example.mywallet.LaunchBus.query = null }
    var range by remember { mutableStateOf(com.example.mywallet.LaunchBus.range ?: "all") }
    androidx.compose.runtime.LaunchedEffect(Unit) { com.example.mywallet.LaunchBus.range = null }
    var filter by remember { mutableStateOf(if (receiptsOnly) "receipt" else "all") }
    val query = q.trim().lowercase()

    val results = remember(s, query, filter, range) {
        val today = com.example.mywallet.data.todayStr()
        s.recs.filter { r ->
            (range == "all" || (range == "today" && r.date == today) || (range == "earlier" && r.date < today)) &&
            (filter == "all" || filter == r.type || (filter == "receipt" && store.hasPhoto(r.id))) &&
                (query.isEmpty() || r.note.lowercase().contains(query) || (s.cat(r.cat)?.name ?: "").lowercase().contains(query) ||
                    (s.acc(r.acct)?.name ?: "").lowercase().contains(query) || r.amount.toString().contains(query) || r.date.contains(query))
        }.sortedWith(compareByDescending<Rec> { it.date }.thenByDescending { it.ts })
    }

    val haze = rememberHazeState()
    Box(Modifier.fillMaxSize().background(MW.Bg)) {
        SceneBackground(1f, Modifier.hazeSource(haze))
        CompositionLocalProvider(LocalHaze provides haze) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
            TopBar("Search", onClose)
            Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                MwIcon("search", MW.Dim, 20.dp)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    BasicTextField(q, { q = it.take(40) }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(color = Color.White, fontSize = 16.sp), cursorBrush = SolidColor(MW.Accent))
                    if (q.isEmpty()) Text("Note, category, account, amount, date", style = TextStyle(color = Color.White.copy(alpha = 0.4f), fontSize = 15.sp))
                }
                if (q.isNotEmpty()) Box(Modifier.padding(start = 8.dp).clickable { q = "" }) { MwIcon("close", MW.Dim, 18.dp) }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "Any date", "today" to "Today", "earlier" to "Earlier").forEach { (k, n) ->
                    val on = range == k
                    Box(
                        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp))
                            .background(if (on) MW.Accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                            .border(1.dp, if (on) MW.Accent else Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                            .clickable { range = k }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(n, style = TextStyle(color = if (on) MW.Mint else Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "All", "expense" to "Expenses", "income" to "Income", "transfer" to "Transfers", "check" to "Balance", "receipt" to "Receipts").forEach { (k, n) ->
                    val on = filter == k
                    Box(
                        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp))
                            .background(if (on) MW.Accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                            .border(1.dp, if (on) MW.Accent else Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                            .clickable { filter = k }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(n, style = TextStyle(color = if (on) MW.Mint else Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("${results.size} record" + if (results.size == 1) "" else "s", style = TextStyle(color = MW.Dim, fontSize = 12.sp))
            if (results.isEmpty()) {
                Text("Nothing found.", Modifier.padding(top = 20.dp).fillMaxWidth(), textAlign = TextAlign.Center, style = TextStyle(color = MW.Dim, fontSize = 14.sp))
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 6.dp, bottom = 30.dp)) {
                var last = ""
                results.forEach { r ->
                    if (r.date != last) {
                        last = r.date
                        val h = dateHeader(r.date)
                        item(key = "h" + r.date) { Lbl(h, Modifier.padding(top = 14.dp, bottom = 4.dp)) }
                    }
                    item(key = r.id) { RecordRow(s, store, r, onClick = { onEdit(r) }) }
                }
            }
        }
        }
    }
}

@Composable
private fun RecordRow(s: AppState, store: Store, r: Rec, onClick: () -> Unit) {
    val c = s.cat(r.cat)
    val sign = if (r.type == "income") 1.0 else if (r.type == "expense") -1.0 else 0.0
    val icon = when (r.type) { "transfer" -> "⇄"; "check" -> "⚖️"; else -> c?.icon ?: if (r.type == "income") "+" else "−" }
    val title = r.note.ifBlank { c?.name ?: when (r.type) { "transfer" -> "Transfer"; "check" -> "Balance check"; else -> "Record" } }
    val sub = (c?.name ?: "") + (if (c != null) " · " else "") + (s.acc(r.acct)?.name ?: "")
    val amount = if (sign == 0.0) s.money(r.amount) else s.money(sign * r.amount, signed = true)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onClick() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { GlyphIcon(icon, Color.White.copy(alpha = 0.92f), 19.dp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                if (store.hasPhoto(r.id)) { Spacer(Modifier.width(6.dp)); MwIcon("paperclip", MW.Dim, 14.dp) }
            }
            Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = MW.Dim, fontSize = 12.sp))
        }
        Text(amount, style = TextStyle(color = if (r.type == "income") MW.Mint else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
    }
}
