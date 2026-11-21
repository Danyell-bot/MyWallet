package com.example.mywallet.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.data.AppState
import com.example.mywallet.data.BADGES
import com.example.mywallet.data.LEVEL_TITLES
import com.example.mywallet.data.Store
import com.example.mywallet.data.bestStreak
import com.example.mywallet.data.cat
import com.example.mywallet.data.dn
import com.example.mywallet.data.fromDn
import com.example.mywallet.data.levelInfo
import com.example.mywallet.data.money
import com.example.mywallet.data.payCfg
import com.example.mywallet.data.payDates
import com.example.mywallet.data.rateOf
import com.example.mywallet.data.tagsOf
import com.example.mywallet.data.todayDn
import com.example.mywallet.data.todayStr
import com.example.mywallet.data.xpForLevel
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/* ============================ FX: confetti + banner ============================ */

object Fx {
    var burst by mutableIntStateOf(0)
    var banner by mutableStateOf<String?>(null)
    /** confetti, and an optional message at the top */
    fun fire(msg: String?) { if (msg != null) banner = msg; burst += 1 }
    fun say(msg: String) { banner = msg }
}

@Composable
private fun Confetti() {
    val parts = remember {
        val r = kotlin.random.Random(System.nanoTime())
        List(110) { floatArrayOf(r.nextFloat(), r.nextFloat() * 0.25f, (r.nextFloat() - 0.5f) * 0.7f, 0.5f + r.nextFloat() * 0.9f, r.nextFloat() * 360f, r.nextInt(6).toFloat(), 6f + r.nextFloat() * 8f) }
    }
    val cols = listOf(MW.Accent, Color(0xFFFFCB7A), Color(0xFFFF9FB0), Color(0xFF9CC4FF), Color(0xFFB9A6FF), Color(0xFFFFFFFF))
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(2600, easing = LinearEasing)) }
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val p = t.value
        val fade = 1f - max(0f, (p - 0.7f) / 0.3f)
        parts.forEach { q ->
            val x = (q[0] + q[2] * p) * w
            val y = (-0.08f + q[1] * 0.3f + q[3] * p * 0.7f + 0.55f * p * p) * h
            rotate(q[4] + p * 540f * (if (q[5] > 2f) 1f else -1f), Offset(x, y)) {
                drawRect(cols[q[5].toInt()].copy(alpha = fade), Offset(x, y), Size(q[6], q[6] * 0.55f))
            }
        }
    }
}

/** put once on top of the Home screen: shows confetti and a short message pill when Fx.fire is called */
@Composable
fun FxLayer() {
    val b = Fx.burst
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(b) { if (b > 0) { show = true; delay(2700); show = false } }
    if (show) Confetti()
    val msg = Fx.banner
    var last by remember { mutableStateOf("") }
    if (msg != null) last = msg
    LaunchedEffect(msg) { if (msg != null) { delay(3400); if (Fx.banner == msg) Fx.banner = null } }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            msg != null, Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(tween(380, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(300)) { -it } + fadeOut(tween(250)),
        ) {
            Row(
                Modifier.statusBarsPadding().padding(top = 10.dp, start = 18.dp, end = 18.dp).clip(MwShape(26.dp))
                    .background(Color(0xFF0E1426)).border(1.dp, MW.Accent.copy(alpha = 0.6f), MwShape(26.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MwIcon("award", MW.Amber, 22.dp, weight = 2f)
                Spacer(Modifier.width(10.dp))
                Text(last, style = MwTs(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}

/* ============================ BADGES + LEVEL ============================ */

@Composable
fun BadgesScreen(s: AppState, onClose: () -> Unit) {
    val li = s.levelInfo()
    val have = s.unlocked.toSet()
    ToolShell("Badges and level", onClose) {
        Column(Modifier.fillMaxWidth().glass(MwShape(28.dp), tint = MW.Tint).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(MwCircle).background(Brush.linearGradient(listOf(MW.Mint, MW.Accent))), contentAlignment = Alignment.Center) {
                    Text(li.level.toString(), style = MwTs(color = MW.OnAccent, fontSize = if (li.level > 99) 22.sp else 28.sp, fontWeight = FontWeight.Black))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Lbl("Level " + li.level)
                    Text(li.title, style = MwTs(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold))
                }
            }
            var go by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { go = true }
            val p by animateFloatAsState(if (go) li.progress else 0f, tween(900, easing = FastOutSlowInEasing), label = "lv")
            Box(Modifier.fillMaxWidth().height(8.dp).clip(MwCircle).background(Color.White.copy(alpha = 0.16f))) {
                Box(Modifier.fillMaxWidth(p.coerceAtLeast(0.01f)).height(8.dp).clip(MwCircle).background(Brush.horizontalGradient(listOf(MW.Accent, MW.Mint))))
            }
            Text(
                "${li.left} more record" + (if (li.left == 1) "" else "s") + " to level ${li.level + 1}." +
                    (if (li.nextTitle != null) " Next title: ${li.nextTitle} at level ${li.nextTitleLevel}." else " You hold the top title."),
                style = MwTs(color = MW.Dim, fontSize = 13.sp, lineHeight = 18.sp),
            )
            Text("Every record you log earns a point. Levels never end, so you can keep climbing.", style = MwTs(color = MW.Faint, fontSize = 12.sp, lineHeight = 17.sp))
        }
        Lbl("Badges  " + have.size + " of " + BADGES.size, Modifier.padding(top = 10.dp))
        BADGES.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { b ->
                    val on = have.contains(b.id)
                    Column(
                        Modifier.weight(1f).height(142.dp).glassLite(MwShape(24.dp), tint = if (on) MW.Tint else null).padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Box(
                            Modifier.size(44.dp).clip(MwShape(15.dp)).background(if (on) Brush.linearGradient(listOf(MW.Amber, Color(0xFFFF8A5A))) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.05f)))),
                            contentAlignment = Alignment.Center,
                        ) { MwIcon(if (on) b.icon else "lock", if (on) Color(0xFF2A1400) else Color.White.copy(alpha = 0.45f), 24.dp, weight = 2f) }
                        Column {
                            Text(b.name, maxLines = 1, style = MwTs(color = if (on) Color.White else Color.White.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold))
                            Text(b.desc, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MwTs(color = if (on) MW.Dim else MW.Faint, fontSize = 11.5.sp, lineHeight = 15.sp))
                        }
                    }
                }
                if (row.size < 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/* ============================ TAGS ============================ */

@Composable
fun TagsScreen(s: AppState, onSearch: (String) -> Unit, onClose: () -> Unit) {
    var all by remember { mutableStateOf(false) }
    var ym by remember { mutableStateOf(todayStr().take(7)) }
    val rs = s.recs.filter { (it.type == "expense" || it.type == "income") && (all || it.date.startsWith(ym)) }
    val by = LinkedHashMap<String, DoubleArray>()   // count, out, in
    rs.forEach { r ->
        tagsOf(r.note).distinct().forEach { t ->
            val o = by.getOrPut(t) { DoubleArray(3) }
            o[0] += 1.0
            if (r.type == "expense") o[1] += r.amount * s.rateOf(r.acct) else o[2] += r.amount * s.rateOf(r.acct)
        }
    }
    val list = by.entries.sortedWith(compareByDescending<Map.Entry<String, DoubleArray>> { it.value[1] }.thenByDescending { it.value[0] })
    ToolShell("Tags", onClose) {
        FChips("", listOf(opt("m", "By month"), opt("a", "All time")), if (all) "a" else "m", { all = it == "a" }, scroll = false)
        if (!all) MonthNav(ym, { ym = it })
        if (list.isEmpty()) EmptyCard("tag", "No tags yet.\nAdd a tag to a note, like \"Notebook #school\", and see what each tag costs you.")
        list.forEach { (t, o) ->
            ItemRow("#", t.removePrefix("#"), o[0].toInt().toString() + " record" + (if (o[0].toInt() == 1) "" else "s"), MW.Dim, MW.Violet,
                trailing = { Text(s.money(o[1]), style = MwTs(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")) }) { onSearch(t) }
        }
        if (list.isNotEmpty()) Text("Tap a tag to see its records.", style = MwTs(color = MW.Faint, fontSize = 12.sp), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

/* ============================ SAVINGS CHALLENGE ============================ */

@Composable
fun ChallengeScreen(s: AppState, store: Store, onClose: () -> Unit) {
    val c = s.challenge
    var base by remember { mutableStateOf("10") }
    var weeks by remember { mutableStateOf("52") }
    var stop by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    ToolShell("Savings challenge", onClose) {
        if (c == null) {
            Column(Modifier.fillMaxWidth().glass(MwShape(26.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(50.dp).clip(MwShape(17.dp)).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { MwIcon("coin", MW.Amber, 26.dp) }
                Text("Save a little more each week", style = MwTs(color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold))
                Text("Week 1 you save a small amount. Each week you save a bit more. Tick each week off as you go. It is a fun way to build a habit.", style = MwTs(color = MW.Dim, fontSize = 13.sp, lineHeight = 19.sp))
            }
            FText("First week amount", base, { base = it }, "10", decimal = true)
            FChips("Length", listOf(opt("52", "52 weeks"), opt("26", "26 weeks")), weeks, { weeks = it }, scroll = false)
            BigButton("Start challenge") {
                val b = base.toDoubleOrNull() ?: 0.0
                if (b <= 0) Toast.makeText(ctx, "Enter the first week amount", Toast.LENGTH_SHORT).show()
                else store.setTopLive("challenge", buildJsonObject {
                    put("name", "Challenge"); put("base", b); put("weeks", weeks.toInt()); put("start", todayStr()); put("done", JsonArray(emptyList()))
                })
            }
        } else {
            val cb = (c["base"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
            val wk = ((c["weeks"] as? JsonPrimitive)?.content?.toDoubleOrNull()?.toInt() ?: 52).coerceIn(1, 104)
            val start = (c["start"] as? JsonPrimitive)?.content ?: todayStr()
            val done = (c["done"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content?.toDoubleOrNull()?.toInt() }?.toSet() ?: emptySet()
            val total = cb * wk * (wk + 1) / 2.0
            val saved = done.sumOf { cb * it }
            val cur = min(wk.toLong(), max(1L, floor((todayDn() - dn(start)) / 7.0).toLong() + 1)).toInt()
            val frac = if (total > 0) (saved / total).toFloat().coerceIn(0f, 1f) else 0f
            val hide = s.settings.hide
            fun amt(v: Double) = if (hide) "•••" else s.money(v)
            val ap by animateFloatAsState(frac, tween(700, easing = FastOutSlowInEasing), label = "ring")
            Row(Modifier.fillMaxWidth().glass(MwShape(26.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(92.dp)) {
                        val sw = 11.dp.toPx()
                        drawArc(Color.White.copy(alpha = 0.14f), 0f, 360f, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(sw))
                        drawArc(MW.Accent, -90f, 360f * ap, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
                    }
                    Text("${Math.round(frac * 100)}%", style = numStyle(20f))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Lbl("Saved so far")
                    Text(amt(saved), style = numStyle(26f))
                    Text("of ${amt(total)} · Week $cur of $wk", style = MwTs(color = MW.Dim, fontSize = 12.5.sp))
                }
            }
            Row(Modifier.fillMaxWidth().glass(MwShape(20.dp)).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("This week: save " + amt(cb * cur) + (if (done.contains(cur)) "  ✓ done" else ""), style = MwTs(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
            }
            (1..wk).chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { w ->
                        val on = done.contains(w)
                        val bg by animateColorAsState(if (on) MW.Accent.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.07f), tween(250), label = "tile")
                        Column(
                            Modifier.weight(1f).height(62.dp).clip(MwShape(16.dp)).background(bg)
                                .border(if (w == cur) 2.dp else 1.dp, if (w == cur) MW.Amber else if (on) MW.Accent else Color.White.copy(alpha = 0.12f), MwShape(16.dp))
                                .clickable {
                                    val nd = if (on) done - w else done + w
                                    store.setTopLive("challenge", JsonObject(c + ("done" to JsonArray(nd.sorted().map { JsonPrimitive(it) }))))
                                    if (!on && nd.size == wk) Fx.fire("Challenge complete!")
                                },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                        ) {
                            Text("Wk $w", style = MwTs(color = if (on) MW.Mint else MW.Faint, fontSize = 10.5.sp, fontWeight = FontWeight.Bold))
                            Text(if (hide) "••" else s.money(cb * w).let { t -> if (t.length > 8) t.substringBefore('.') else t }, maxLines = 1, style = MwTs(color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"))
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            GhostButton("Stop challenge", Modifier.fillMaxWidth(), MW.Rose) { stop = true }
        }
    }
    if (stop) ConfirmDialog("Stop the challenge?", "Your ticks will be cleared.", "Stop", { store.removeTop("challenge"); stop = false }, { stop = false })
}

/* ============================ PAYDAY ============================ */

@Composable
fun PaydayScreen(s: AppState, store: Store, onClose: () -> Unit) {
    val p = s.payCfg()
    var mode by remember { mutableStateOf(p.mode) }
    var d1 by remember { mutableStateOf(p.d1.toString()) }
    var d2 by remember { mutableStateOf(p.d2.toString()) }
    var wd by remember { mutableStateOf(p.wd.toString()) }
    fun apply(m: String, a: String, b: String, w: String) {
        val x = a.toIntOrNull()?.coerceIn(1, 31) ?: 1
        val y = b.toIntOrNull()?.coerceIn(1, 31) ?: 15
        store.updateSettings(mapOf("pay" to buildJsonObject { put("mode", m); put("d1", x); put("d2", y); put("wd", w.toIntOrNull() ?: 5) }))
    }
    ToolShell("Payday", onClose) {
        Column(Modifier.fillMaxWidth().glass(MwShape(24.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("When do you get paid?", style = MwTs(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold))
            Text("MyWallet uses this to show how much you can spend each day until the next payday.", style = MwTs(color = MW.Dim, fontSize = 13.sp, lineHeight = 19.sp))
        }
        FChips("", listOf(opt("off", "Off"), opt("m1", "Monthly"), opt("m2", "Twice a month"), opt("w", "Weekly")), mode, { mode = it; apply(it, d1, d2, wd) })
        if (mode == "m1" || mode == "m2") FText(if (mode == "m2") "First payday (day of month)" else "Payday (day of month)", d1, { d1 = it.filter { c -> c.isDigit() }.take(2); apply(mode, d1, d2, wd) }, "1")
        if (mode == "m2") FText("Second payday (day of month)", d2, { d2 = it.filter { c -> c.isDigit() }.take(2); apply(mode, d1, d2, wd) }, "15")
        if (mode == "w") FChips("Day of the week", listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").mapIndexed { i, n -> opt(i.toString(), n) }, wd, { wd = it; apply(mode, d1, d2, it) })
        val pd = s.payDates(todayDn())
        if (mode != "off" && pd != null) {
            Row(Modifier.fillMaxWidth().glass(MwShape(20.dp), tint = MW.Accent).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                MwIcon("calendar", MW.Mint, 22.dp)
                Spacer(Modifier.width(12.dp))
                val dl = pd.next - todayDn()
                Text("Next payday: " + fmtDate(fromDn(pd.next)) + " · in $dl day" + (if (dl == 1L) "" else "s"), style = MwTs(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
            }
        }
        Text("Days like the 31st use the last day of shorter months.", style = MwTs(color = MW.Faint, fontSize = 12.sp))
    }
}

/* ============================ YEAR IN REVIEW ============================ */

@Composable
fun YearScreen(s: AppState, onClose: () -> Unit) {
    BackHandler { onClose() }
    val ctx = LocalContext.current
    val nowY = todayStr().take(4).toInt()
    var year by remember { mutableStateOf(nowY) }
    val y = year.toString()
    val ex = s.recs.filter { it.type == "expense" && it.date.startsWith(y) }
    val spent = ex.sumOf { it.amount * s.rateOf(it.acct) }
    val inc = s.recs.filter { it.type == "income" && it.date.startsWith(y) }.sumOf { it.amount * s.rateOf(it.acct) }
    val topCat = ex.groupBy { it.cat }.mapValues { (_, l) -> l.sumOf { it.amount * s.rateOf(it.acct) } }.entries.maxByOrNull { it.value }
    val big = ex.maxByOrNull { it.amount * s.rateOf(it.acct) }
    val bus = ex.groupBy { it.date.take(7) }.mapValues { (_, l) -> l.sumOf { it.amount * s.rateOf(it.acct) } }.entries.maxByOrNull { it.value }
    val n = s.recs.count { it.type != "check" && it.date.startsWith(y) }
    val best = s.bestStreak()
    val pager = rememberPagerState(pageCount = { 6 })
    val pairs = listOf(
        Color(0xFF0FB98A) to Color(0xFF0B3A7A), Color(0xFF7C3AED) to Color(0xFF1B1464), Color(0xFFFF8A5A) to Color(0xFF8E1257),
        Color(0xFFE8A93A) to Color(0xFF7A0C2B), Color(0xFF2F7BFF) to Color(0xFF4B1FA8), Color(0xFF16C784) to Color(0xFF1B38D6),
    )
    val tp = MwTheme.slide(pager.currentPage, pairs[pager.currentPage].first, pairs[pager.currentPage].second)
    val c1 by animateColorAsState(tp.first, tween(600), label = "c1")
    val c2 by animateColorAsState(tp.second, tween(600), label = "c2")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c1, c2)))) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
            val vis = pager.currentPage == i
            val k by animateFloatAsState(if (vis) 1f else 0f, tween(1300, easing = FastOutSlowInEasing), label = "k")
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 30.dp), verticalArrangement = Arrangement.Center) {
                when (i) {
                    0 -> {
                        YKick("Your $y"); Spacer(Modifier.height(8.dp))
                        val t = s.money(spent * k)
                        Text(t, maxLines = 1, style = numStyle(if (t.length > 10) 44f else 60f)); YSub("spent in total.")
                    }
                    1 -> {
                        YKick("Money in"); Spacer(Modifier.height(8.dp))
                        val t = s.money(inc * k)
                        Text(t, maxLines = 1, style = numStyle(if (t.length > 10) 44f else 60f))
                        YSub(if (inc >= spent) "You kept ${s.money(inc - spent)} of it." else "${s.money(spent - inc)} more went out than came in.")
                    }
                    2 -> {
                        YKick("Top category"); Spacer(Modifier.height(18.dp))
                        Box(Modifier.size(110.dp).clip(MwCircle).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            GlyphIcon(s.cat(topCat?.key ?: "")?.icon ?: "🏷", Color.White, (56 * (0.5f + 0.5f * k)).dp)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(if (topCat == null) "No expenses yet" else (s.cat(topCat.key)?.name ?: "No category"), maxLines = 2, style = MwTs(color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black, lineHeight = 44.sp))
                        if (topCat != null && spent > 0) YSub(s.money(topCat.value) + " (" + Math.round(topCat.value / spent * 100) + "%)")
                    }
                    3 -> {
                        YKick("Biggest expense"); Spacer(Modifier.height(8.dp))
                        val v = (big?.let { it.amount * s.rateOf(it.acct) } ?: 0.0) * k
                        val t = s.money(v)
                        Text(t, maxLines = 1, style = numStyle(if (t.length > 10) 44f else 60f))
                        YSub(if (big == null) "Nothing yet." else (big.note.ifBlank { s.cat(big.cat)?.name ?: "Expense" }) + " on " + fmtDate(big.date))
                    }
                    4 -> {
                        YKick("Busiest month"); Spacer(Modifier.height(8.dp))
                        Text(if (bus == null) "—" else com.example.mywallet.ui.monthLabel(bus.key).substringBefore(" "), style = MwTs(color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Black))
                        if (bus != null) YSub(s.money(bus.value) + " spent.")
                    }
                    else -> {
                        YKick("Records logged"); Spacer(Modifier.height(8.dp))
                        Text(Math.round(n * k).toString(), style = numStyle(100f))
                        YSub("Best streak: $best day" + (if (best == 1) "" else "s"))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(MwCircle).background(Color.White.copy(alpha = 0.2f)).clickable { onClose() }, contentAlignment = Alignment.Center) { MwIcon("back", Color.White, 22.dp, weight = 2f) }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(42.dp).clip(MwCircle).background(Color.White.copy(alpha = 0.2f)).clickable { year -= 1 }, contentAlignment = Alignment.Center) { MwIcon("back", Color.White, 20.dp, weight = 2f) }
            Text(y, Modifier.padding(horizontal = 14.dp), style = MwTs(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold))
            Box(Modifier.size(42.dp).clip(MwCircle).background(Color.White.copy(alpha = if (year < nowY) 0.2f else 0.06f)).clickable { if (year < nowY) year += 1 }, contentAlignment = Alignment.Center) { MwIcon("back", Color.White, 20.dp, Modifier.rotate(180f), weight = 2f) }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(42.dp).clip(MwCircle).background(Color.White.copy(alpha = 0.2f)).clickable {
                val text = "My $y in money: spent ${s.money(spent)}, money in ${s.money(inc)}" +
                    (if (topCat != null) ", top category ${s.cat(topCat.key)?.name ?: ""}" else "") + ", $n records logged, best streak $best days."
                try { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share")) } catch (e: Exception) { }
            }, contentAlignment = Alignment.Center) { MwIcon("share", Color.White, 20.dp, weight = 2f) }
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(6) { i -> Box(Modifier.size(if (i == pager.currentPage) 22.dp else 7.dp, 7.dp).clip(MwCircle).background(Color.White.copy(alpha = if (i == pager.currentPage) 0.95f else 0.4f))) }
        }
    }
}

@Composable
private fun YKick(t: String) = Text(t.uppercase(), style = MwTs(color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp))

@Composable
private fun YSub(t: String) = Text(t, Modifier.padding(top = 12.dp), style = MwTs(color = Color.White.copy(alpha = 0.92f), fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold))
