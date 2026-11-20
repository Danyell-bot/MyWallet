package com.example.mywallet.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.data.AppState
import com.example.mywallet.data.FLAME_DAYS
import com.example.mywallet.data.Store
import com.example.mywallet.data.bestStreak
import com.example.mywallet.data.cat
import com.example.mywallet.data.dn
import com.example.mywallet.data.flameTier
import com.example.mywallet.data.loggedDays
import com.example.mywallet.data.money
import com.example.mywallet.data.rateOf
import com.example.mywallet.data.streak
import com.example.mywallet.data.todayDn
import com.example.mywallet.data.todayStr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val PAL: List<Color> get() = listOf(MW.Accent, Color(0xFF7BB8FF), Color(0xFFFFCB7A), Color(0xFFB9A6FF), Color(0xFFFF9FB0), Color(0xFF8CFFC0), Color(0xFFFF8A5A), Color(0xFF6EE7F9))

/* ---------- month helpers ---------- */
fun ymAdd(ym: String, d: Int): String {
    val total = ym.take(4).toInt() * 12 + (ym.drop(5).toInt() - 1) + d
    val y = Math.floorDiv(total, 12); val m = Math.floorMod(total, 12) + 1
    return "%04d-%02d".format(y, m)
}
fun monthStart(ym: String) = dn("$ym-01")
fun monthEnd(ym: String) = dn(ymAdd(ym, 1) + "-01") - 1
fun monthLabel(ym: String): String {
    val f = SimpleDateFormat("MMMM yyyy", Locale.getDefault()); f.timeZone = TimeZone.getTimeZone("UTC")
    return f.format(Date(monthStart(ym) * 86400000L))
}
private fun shortMon(ym: String): String {
    val f = SimpleDateFormat("MMM", Locale.getDefault()); f.timeZone = TimeZone.getTimeZone("UTC")
    return f.format(Date(monthStart(ym) * 86400000L))
}
fun AppState.sumMonth(ym: String, type: String) = recs.filter { it.type == type && it.date.startsWith(ym) }.sumOf { it.amount * rateOf(it.acct) }
private fun AppState.spentIn(ym: String, catId: String) = recs.filter { it.type == "expense" && it.cat == catId && it.date.startsWith(ym) }.sumOf { it.amount * rateOf(it.acct) }
private fun toast(ctx: android.content.Context, m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()

@Composable
fun MonthNav(ym: String, onChange: (String) -> Unit, label: String = monthLabel(ym), step: Int = 1) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).glass(CircleShape).clickable { onChange(ymAdd(ym, -step)) }, contentAlignment = Alignment.Center) { MwIcon("back", Color.White, 20.dp, weight = 2f) }
        Text(label, Modifier.weight(1f), textAlign = TextAlign.Center, style = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold))
        Box(Modifier.size(42.dp).glass(CircleShape).clickable { onChange(ymAdd(ym, step)) }, contentAlignment = Alignment.Center) { MwIcon("back", Color.White, 20.dp, Modifier.rotate(180f), weight = 2f) }
    }
}

/* ============================ ANALYSIS ============================ */

@Composable
fun AnalysisScreen(s: AppState, onClose: () -> Unit) {
    var ym by remember { mutableStateOf(todayStr().take(7)) }
    var year by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf("expense") }
    var donut by remember { mutableStateOf(true) }
    val key = if (year) ym.take(4) else ym
    val by = s.recs.filter { it.type == type && it.date.startsWith(key) }.groupBy { it.cat }.mapValues { (_, l) -> l.sumOf { it.amount * s.rateOf(it.acct) } }
    val list = by.entries.sortedByDescending { it.value }
    val tot = list.sumOf { it.value }
    val tone = if (type == "expense") MW.Rose else MW.Mint
    var tab by remember { mutableStateOf("b") }
    ToolShell("Analysis", onClose) {
        FChips("", listOf(opt("b", "Breakdown"), opt("t", "Trends")), tab, { tab = it }, scroll = false)
        if (tab == "t") { TrendsBody(s); return@ToolShell }
        MonthNav(ym, { ym = it }, if (year) ym.take(4) else monthLabel(ym), if (year) 12 else 1)
        FChips("", listOf(opt("m", "Month"), opt("y", "Year")), if (year) "y" else "m", { year = it == "y" }, scroll = false)
        FChips("", listOf(opt("expense", "Expenses"), opt("income", "Income")), type, { type = it }, scroll = false)
        FChips("", listOf(opt("d", "Donut"), opt("b", "Bars")), if (donut) "d" else "b", { donut = it == "d" }, scroll = false)
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (donut && list.isNotEmpty()) {
                Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(210.dp)) {
                        val sw = 30.dp.toPx()
                        var start = -90f
                        list.forEachIndexed { i, e ->
                            val sweep = (e.value / tot * 360.0).toFloat()
                            drawArc(PAL[i % 8], start + 1.2f, (sweep - 2.4f).coerceAtLeast(0.5f), false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Butt))
                            start += sweep
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (type == "expense") "SPENT" else "EARNED", style = TextStyle(color = MW.Dim, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp))
                        Text(s.money(tot), style = numStyle(24f))
                    }
                }
            } else {
                Lbl(if (type == "expense") "Spent" else "Earned")
                Text(s.money(tot), style = numStyle(40f, tone))
            }
        }
        if (list.isEmpty()) EmptyCard("pie", "Nothing here yet.\nAdd some " + (if (type == "expense") "expenses" else "income") + " and they show up as a breakdown.")
        else Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            list.forEachIndexed { i, e ->
                val c = s.cat(e.key)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (donut) { Box(Modifier.size(10.dp).clip(CircleShape).background(PAL[i % 8])); Spacer(Modifier.width(8.dp)) }
                        GlyphIcon(c?.icon ?: "•", Color.White, 18.dp); Spacer(Modifier.width(8.dp))
                        Text(c?.name ?: "No category", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
                        Text("${Math.round(e.value / tot * 100)}%", style = TextStyle(color = MW.Dim, fontSize = 12.sp)); Spacer(Modifier.width(10.dp))
                        Text(s.money(e.value), style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"))
                    }
                    Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) {
                        Box(Modifier.fillMaxWidth((e.value / list[0].value).toFloat().coerceIn(0.02f, 1f)).height(7.dp).clip(CircleShape).background(PAL[i % 8]))
                    }
                }
            }
        }
    }
}

/* ============================ TRENDS ============================ */

@Composable
fun ColumnScope.TrendsBody(s: AppState) {
    val now = todayStr().take(7)
    val ms = (-5..0).map { ymAdd(now, it) }
    val ex = ms.map { s.sumMonth(it, "expense") }
    val inc = ms.map { s.sumMonth(it, "income") }
    var sel by remember { mutableStateOf(ms.lastIndex) }
    val mx = maxOf(1.0, (ex + inc).max()) * 1.12
    run {
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(CircleShape).background(MW.Rose)); Spacer(Modifier.width(6.dp)); Text("Expenses", style = TextStyle(color = MW.Dim, fontSize = 12.sp)) }
                Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(CircleShape).background(MW.Mint)); Spacer(Modifier.width(6.dp)); Text("Income", style = TextStyle(color = MW.Dim, fontSize = 12.sp)) }
            }
            Canvas(
                Modifier.fillMaxWidth().height(200.dp).pointerInput(Unit) {
                    detectTapGestures { o ->
                        val padL = 12.dp.toPx(); val w = size.width - 2 * padL
                        sel = Math.round(((o.x - padL) / w) * (ms.size - 1)).coerceIn(0, ms.lastIndex)
                    }
                },
            ) {
                val padL = 12.dp.toPx(); val padT = 14.dp.toPx(); val padB = 26.dp.toPx()
                val w = size.width - 2 * padL; val h = size.height - padT - padB
                fun x(i: Int) = padL + w * i / (ms.size - 1)
                fun y(v: Double) = padT + h * (1f - (v / mx).toFloat())
                listOf(0f, 0.5f, 1f).forEach { f -> drawLine(Color.White.copy(alpha = 0.1f), Offset(padL, padT + h * f), Offset(padL + w, padT + h * f), 1.dp.toPx()) }
                drawLine(Color.White.copy(alpha = 0.25f), Offset(x(sel), padT), Offset(x(sel), padT + h), 1.dp.toPx())
                fun line(vals: List<Double>, c: Color) {
                    val path = Path()
                    vals.forEachIndexed { i, v -> if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v)) }
                    drawPath(path, c, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    vals.forEachIndexed { i, v -> drawCircle(c, if (i == sel) 6.dp.toPx() else 4.dp.toPx(), Offset(x(i), y(v))); drawCircle(Color(0xFF060912), 2.dp.toPx(), Offset(x(i), y(v))) }
                }
                line(inc, MW.Mint); line(ex, MW.Rose)
                val paint = android.graphics.Paint().apply { color = android.graphics.Color.argb(170, 255, 255, 255); textSize = 11.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true }
                ms.forEachIndexed { i, m -> drawContext.canvas.nativeCanvas.drawText(shortMon(m), x(i), size.height - 6.dp.toPx(), paint) }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(monthLabel(ms[sel]), Modifier.weight(1f), style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                Text("In " + s.money(inc[sel]), style = TextStyle(color = MW.Mint, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)); Spacer(Modifier.width(12.dp))
                Text("Out " + s.money(ex[sel]), style = TextStyle(color = MW.Rose, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
            }
        }
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row { Text("Month", Modifier.weight(1f), style = TextStyle(color = MW.Dim, fontSize = 12.sp)); Text("Income", Modifier.width(100.dp), textAlign = TextAlign.End, style = TextStyle(color = MW.Dim, fontSize = 12.sp)); Text("Expense", Modifier.width(100.dp), textAlign = TextAlign.End, style = TextStyle(color = MW.Dim, fontSize = 12.sp)) }
            ms.forEachIndexed { i, m ->
                Row { Text(shortMon(m), Modifier.weight(1f), style = TextStyle(color = Color.White, fontSize = 14.sp)); Text(s.money(inc[i]), Modifier.width(100.dp), textAlign = TextAlign.End, style = TextStyle(color = MW.Mint, fontSize = 13.sp, fontFeatureSettings = "tnum")); Text(s.money(ex[i]), Modifier.width(100.dp), textAlign = TextAlign.End, style = TextStyle(color = Color.White, fontSize = 13.sp, fontFeatureSettings = "tnum")) }
            }
        }
        Lbl("Insights", Modifier.padding(top = 4.dp))
        val ins = insightsFor(s)
        if (ins.isEmpty()) Text("Add a few more expenses to see insights.", style = TextStyle(color = MW.Dim, fontSize = 13.sp))
        ins.forEach { t -> Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp), MW.Amber).padding(16.dp), verticalAlignment = Alignment.CenterVertically) { MwIcon("sparkles", MW.Amber, 20.dp); Spacer(Modifier.width(12.dp)); Text(highlightMoney(t, s.settings.cur, MW.Amber, Color.White), style = TextStyle(color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)) } }
    }
}

private fun insightsFor(s: AppState): List<String> {
    val out = ArrayList<String>()
    val now = todayStr().take(7); val prev = ymAdd(now, -1)
    val e = s.sumMonth(now, "expense"); val pe = s.sumMonth(prev, "expense")
    if (pe > 0 && e > 0) {
        val d = Math.round((e - pe) / pe * 100).toInt()
        out.add(if (d > 0) "You have spent $d% more than last month (${s.money(e)} so far, against ${s.money(pe)})." else if (d < 0) "You are spending ${-d}% less than last month (${s.money(e)} so far, against ${s.money(pe)})." else "Your spending matches last month so far.")
    }
    val by = s.recs.filter { it.type == "expense" && it.date.startsWith(now) }.groupBy { it.cat }.mapValues { (_, l) -> l.sumOf { it.amount * s.rateOf(it.acct) } }
    val top = by.entries.maxByOrNull { it.value }
    if (top != null && e > 0) out.add("${s.cat(top.key)?.name ?: "No category"} is your biggest category this month: ${s.money(top.value)} (${Math.round(top.value / e * 100)}% of spending).")
    val day = todayStr().drop(8).toInt()
    if (e > 0) out.add("You spend about ${s.money(e / day)} a day this month.")
    val big = s.recs.filter { it.type == "expense" && it.date.startsWith(now) }.maxByOrNull { it.amount * s.rateOf(it.acct) }
    if (big != null) out.add("Your biggest expense this month was ${s.money(big.amount * s.rateOf(big.acct))}${if (big.note.isNotBlank()) " (" + big.note + ")" else ""}.")
    return out
}

/* ============================ BUDGETS ============================ */

@Composable
fun BudgetsScreen(s: AppState, store: Store, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var ym by remember { mutableStateOf(todayStr().take(7)) }
    var editCat by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    val b = s.budgets[ym] ?: emptyMap()
    val ids = b.keys.filter { s.cat(it) != null }
    val totB = ids.sumOf { b[it] ?: 0.0 }
    val totS = ids.sumOf { s.spentIn(ym, it) }
    ToolShell("Budgets", onClose, "New", { adding = true }) {
        MonthNav(ym, { ym = it })
        val left = totB - totS
        val hot = if (left >= 0) MW.Mint else Color(0xFFFF8FA0)
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp), if (left >= 0) MW.Accent else Color(0xFFFF6B81)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Lbl("Spent this month")
            Text(s.money(totS), style = numStyle(40f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)).padding(12.dp)) { Text("Total budget", style = TextStyle(color = MW.Dim, fontSize = 11.sp)); Text(s.money(totB), style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum")) }
                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(hot.copy(alpha = 0.18f)).padding(12.dp)) { Text(if (left >= 0) "Left" else "Over by", style = TextStyle(color = hot, fontSize = 11.sp, fontWeight = FontWeight.Bold)); Text(s.money(Math.abs(left)), style = TextStyle(color = hot, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum")) }
            }
        }
        if (ids.isEmpty()) EmptyCard("wallet", "No budget in this month.\nSet a limit on a category, or copy last month's budgets.")
        ids.forEach { id ->
            val c = s.cat(id)!!; val lim = b[id] ?: 0.0; val sp = s.spentIn(ym, id); val pct = if (lim > 0) sp / lim else 0.0; val l = lim - sp
            val bc = if (pct > 1) Color(0xFFFF6B81) else if (pct >= 0.8) MW.Amber else MW.Accent
            Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).clickable { editCat = id }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { GlyphIcon(c.icon, Color.White, 20.dp) }
                    Spacer(Modifier.width(12.dp))
                    Text(c.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    Text(s.money(sp) + " / " + s.money(lim), style = TextStyle(color = MW.Dim, fontSize = 12.5.sp, fontFeatureSettings = "tnum"))
                }
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))) { Box(Modifier.fillMaxWidth(pct.toFloat().coerceIn(0f, 1f)).height(8.dp).clip(CircleShape).background(bc)) }
                Text(if (l >= 0) s.money(l) + " left" else "Over by " + s.money(-l), style = TextStyle(color = if (l >= 0) MW.Dim else Color(0xFFFF8FA0), fontSize = 12.5.sp, fontWeight = if (l < 0) FontWeight.ExtraBold else FontWeight.Normal))
            }
        }
        GhostButton("Copy budgets from previous month", Modifier.fillMaxWidth(), icon = "repeat") {
            if (store.copyBudgets(ymAdd(ym, -1), ym)) toast(ctx, "Copied") else toast(ctx, "Previous month has no budget")
        }
        Text("A budget is only a limit you watch. It never changes an account balance.", style = TextStyle(color = MW.Faint, fontSize = 12.sp))
    }
    if (adding) BudgetForm(s, store, ym, null) { adding = false }
    editCat?.let { BudgetForm(s, store, ym, it) { editCat = null } }
}

@Composable
private fun BudgetForm(s: AppState, store: Store, ym: String, edit: String?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val used = (s.budgets[ym] ?: emptyMap()).keys
    val avail = s.cats.filter { it.type == "expense" && !it.ignored && (it.id !in used || it.id == edit) }
    var cat by remember { mutableStateOf(edit ?: avail.firstOrNull()?.id ?: "") }
    var limit by remember { mutableStateOf(edit?.let { (s.budgets[ym]?.get(it) ?: 0.0).let { v -> java.math.BigDecimal.valueOf(v).stripTrailingZeros().toPlainString() } } ?: "") }
    FormPanel(if (edit == null) "New budget" else "Edit budget", onClose) {
        if (avail.isEmpty()) Text("Every category already has a budget this month.", style = TextStyle(color = MW.Dim, fontSize = 14.sp))
        else {
            if (edit == null) FChips("Category", avail.map { opt(it.id, it.name, it.icon) }, cat, { cat = it })
            FText("Monthly limit", limit, { limit = it }, "0", decimal = true)
            BigButton("Save") {
                val v = limit.toDoubleOrNull() ?: 0.0
                if (cat.isEmpty()) toast(ctx, "Choose a category") else if (v <= 0) toast(ctx, "Limit cannot be zero")
                else { store.setBudget(ym, cat, v); onClose() }
            }
        }
        if (edit != null) GhostButton("Remove this budget", Modifier.fillMaxWidth(), Color(0xFFFF8FA0), "trash") { store.setBudget(ym, edit, null); onClose() }
    }
}

/* ============================ STREAK ============================ */

@Composable
fun StreakScreen(s: AppState, onClose: () -> Unit) {
    val st = s.streak(); val best = s.bestStreak()
    val lit = s.loggedDays().contains(todayDn())
    val real = flameTier(st)
    val shown = if (st > 0 && !lit) 0 else real
    val next = FLAME_DAYS.getOrNull(real + 1)
    val prev = FLAME_DAYS[real]
    ToolShell("Streak", onClose) {
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(32.dp), MW.Amber).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlameIcon(shown, 150.dp)
            Text("$st day" + if (st == 1) "" else "s", style = numStyle(40f))
            Text(FLAME_NAMES[real] + " · Best: $best", style = TextStyle(color = MW.Dim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
            if (st > 0 && !lit) Text("Log something today to keep your flame lit.", Modifier.padding(top = 6.dp), textAlign = TextAlign.Center, style = TextStyle(color = MW.Amber, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold))
            if (next != null) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))) {
                    Box(Modifier.fillMaxWidth(((st - prev).toFloat() / (next - prev)).coerceIn(0f, 1f)).height(8.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(MW.Amber, MW.Rose))))
                }
                Text("${next - st} more day" + (if (next - st == 1) "" else "s") + " to ${FLAME_NAMES[real + 1]}", style = TextStyle(color = MW.Amber, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
            } else Text("Top flame reached!", style = TextStyle(color = MW.Mint, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold))
        }
        Text("Log at least one record a day to grow your streak. You get one free missed day each week.", style = TextStyle(color = MW.Dim, fontSize = 13.sp, lineHeight = 19.sp))
        Lbl("Flame ladder", Modifier.padding(top = 4.dp))
        (1..16).toList().chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { t ->
                    val on = real >= t
                    Column(Modifier.weight(1f).glass(RoundedCornerShape(20.dp), if (on) MW.Amber else null).padding(vertical = 12.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            FlameIcon(t, 64.dp, locked = !on)
                            if (!on) MwIcon("lock", Color.White.copy(alpha = 0.55f), 24.dp, weight = 2f)
                        }
                        Text(FLAME_NAMES[t].removeSuffix(" flame"), maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = if (on) Color.White else MW.Faint, fontSize = 11.5.sp, fontWeight = FontWeight.Bold))
                        Text(FLAME_DAYS[t].toString() + "d", style = TextStyle(color = if (on) MW.Mint else MW.Dim, fontSize = 11.sp))
                    }
                }
            }
        }
    }
}
