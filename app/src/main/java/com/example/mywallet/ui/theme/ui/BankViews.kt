package com.example.mywallet.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.data.Account
import com.example.mywallet.data.AppState
import com.example.mywallet.data.BankInfo
import com.example.mywallet.data.Rec
import com.example.mywallet.data.cat
import com.example.mywallet.data.civilFromDays
import com.example.mywallet.data.dn
import com.example.mywallet.data.fromDn
import com.example.mywallet.data.money
import com.example.mywallet.data.series
import com.example.mywallet.data.todayDn
import com.example.mywallet.data.todayStr
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

private val MON = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private fun shortDate(d: Long): String { val (_, m, day) = civilFromDays(d); return MON[(m - 1).toInt()] + " " + day }

private fun niceMax(v: Double): Double {
    if (v <= 0) return 100.0
    val p = 10.0.pow(floor(log10(v)))
    val f = v / p
    return (if (f <= 1) 1.0 else if (f <= 2) 2.0 else if (f <= 2.5) 2.5 else if (f <= 5) 5.0 else 10.0) * p
}

private fun compact(v: Double): String =
    if (v >= 1e6) (if (v % 1e6 == 0.0) (v / 1e6).toInt().toString() else "%.1f".format(v / 1e6)) + "M"
    else if (v >= 1e3) (if (v % 1e3 == 0.0) (v / 1e3).toInt().toString() else "%.1f".format(v / 1e3)) + "k"
    else Math.round(v).toString()

/* ============================ CHART ============================ */

@Composable
fun BankChart(s: AppState, a: Account, c: BankInfo) {
    val t = todayDn()
    val ser = remember(s, a.id) { s.series(a, t) }
    val p = c.period
    val n = (p.end - p.start + 1).toInt()
    val vals = DoubleArray(n) { i -> val d = p.start + i; if (d <= t) ser.at(d) else Double.NaN }
    val avgs = DoubleArray(n)
    var run = 0.0
    for (i in 0 until n) { if (!vals[i].isNaN()) { run += max(0.0, vals[i]); avgs[i] = run / (i + 1) } else avgs[i] = Double.NaN }
    val isAvg = a.bank?.rule == "avg"
    val vmax = niceMax(max(c.min, max(1.0, vals.filter { !it.isNaN() }.maxOrNull() ?: 1.0)) * 1.08)
    var sel by remember(a.id) { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxWidth().glass(MwShape(26.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Lbl("Daily balance · " + shortDate(p.start) + " to " + shortDate(p.end))
        val i = sel
        Text(
            if (i != null && !vals[i].isNaN()) shortDate(p.start + i) + ": " + s.money(vals[i]) + (if (isAvg) " · avg " + s.money(avgs[i]) else "") else "Tap a bar to see that day",
            style = MwTs(color = if (i != null) Color.White else MW.Faint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
        )
        Canvas(
            Modifier.fillMaxWidth().height(230.dp).pointerInput(n) {
                detectTapGestures { o ->
                    val ml = 40.dp.toPx(); val pw = size.width - ml - 8.dp.toPx()
                    val idx = ((o.x - ml) / (pw / n)).toInt()
                    sel = if (idx in 0 until n) idx else null
                }
            },
        ) {
            val ml = 40.dp.toPx(); val mr = 8.dp.toPx(); val mt = 10.dp.toPx(); val mb = 24.dp.toPx()
            val pw = size.width - ml - mr; val ph = size.height - mt - mb
            val bw = pw / n
            fun y(v: Double) = (mt + ph - (max(0.0, v) / vmax) * ph).toFloat()
            fun cx(i: Int) = ml + bw * i + bw / 2
            val paint = Paint().apply { color = android.graphics.Color.argb(150, 255, 255, 255); textSize = 10.sp.toPx(); isAntiAlias = true }
            for (k in 0..4) {
                val tv = vmax * k / 4
                drawLine(Color.White.copy(alpha = 0.12f), Offset(ml, y(tv)), Offset(size.width - mr, y(tv)), 1f)
                paint.textAlign = Paint.Align.RIGHT
                drawContext.canvas.nativeCanvas.drawText(compact(tv), ml - 6.dp.toPx(), y(tv) + 4.dp.toPx(), paint)
            }
            val barW = max(2f, bw * 0.64f)
            for (k in 0 until n) {
                val v = vals[k]
                if (v.isNaN() || v <= 0) continue
                val top = y(v)
                val below = v < c.min
                val col = if (sel == k) Color.White else if (below) Color(0xFFFF8FA0) else MW.Accent
                drawRoundRect(col.copy(alpha = 0.9f), Offset(cx(k) - barW / 2, top), Size(barW, mt + ph - top), CornerRadius(minOf(3f, barW / 2)))
            }
            drawLine(Color.White.copy(alpha = 0.7f), Offset(ml, y(c.min)), Offset(size.width - mr, y(c.min)), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
            if (isAvg) {
                val path = Path(); var started = false; var lastK = -1
                for (k in 0 until n) if (!avgs[k].isNaN()) { val px = cx(k); val py = y(avgs[k]); if (!started) { path.moveTo(px, py); started = true } else path.lineTo(px, py); lastK = k }
                if (started) {
                    drawPath(path, Color(0xFFFFE08A), style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawCircle(Color(0xFFFFE08A), 6f, Offset(cx(lastK), y(avgs[lastK])))
                }
            }
            paint.textAlign = Paint.Align.LEFT
            drawContext.canvas.nativeCanvas.drawText(shortDate(p.start), ml, size.height - 6.dp.toPx(), paint)
            paint.textAlign = Paint.Align.CENTER
            drawContext.canvas.nativeCanvas.drawText(shortDate(p.start + n / 2), ml + pw / 2, size.height - 6.dp.toPx(), paint)
            paint.textAlign = Paint.Align.RIGHT
            drawContext.canvas.nativeCanvas.drawText(shortDate(p.end), size.width - mr, size.height - 6.dp.toPx(), paint)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            @Composable fun key(col: Color, label: String) { Box(Modifier.size(9.dp).clip(MwCircle).background(col)); Spacer(Modifier.size(5.dp)); Text(label, style = MwTs(color = MW.Dim, fontSize = 11.5.sp)) }
            key(MW.Accent, "Balance"); key(Color(0xFFFF8FA0), "Below min")
            if (isAvg) key(Color(0xFFFFE08A), "Average")
        }
        Text("Minimum " + s.money(c.min) + " (dashed line)", style = MwTs(color = MW.Faint, fontSize = 12.sp))
    }
}

/* ============================ WHAT IF ============================ */

@Composable
fun BankWhatIf(s: AppState, a: Account, c: BankInfo) {
    val cfg = a.bank ?: return
    var dir by remember { mutableStateOf("out") }
    var amt by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(todayStr()) }
    Column(Modifier.fillMaxWidth().glass(MwShape(26.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("What if", style = MwTs(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold))
        Text("Type an amount and see where this period would end up if nothing else changes.", style = MwTs(color = MW.Dim, fontSize = 13.sp, lineHeight = 18.sp))
        FChips("", listOf(opt("out", "Spend"), opt("in", "Deposit")), dir, { dir = it }, scroll = false)
        FText("Amount", amt, { amt = it }, "0", decimal = true)
        FDate("On date", date, { date = it })
    }
    val v = amt.toDoubleOrNull() ?: 0.0
    if (v <= 0) { EmptyCard("target", "Enter an amount to see the result."); return }
    val t = todayDn(); val p = c.period; val nDays = p.end - p.start + 1
    val dd = dn(date).coerceIn(t, p.end)
    val signed = if (dir == "out") -v else v
    val ser = s.series(a, t)
    var pts = 0.0; var breach = false
    for (d in p.start until t) { val b = ser.at(d); pts += max(0.0, b); if (d >= ser.start && b < c.min) breach = true }
    val bals = (t..p.end).map { d -> c.cur + (if (d >= dd) signed else 0.0) }
    val (measure, label, ok) = when (cfg.rule) {
        "avg" -> { val m = (pts + bals.sumOf { max(0.0, it) }) / nDays; Triple(m, "Average at period end", m >= c.min) }
        "daily" -> { val m = bals.minOrNull() ?: c.cur; Triple(m, "Lowest day from now", !breach && m >= c.min) }
        else -> { val m = bals.lastOrNull() ?: c.cur; Triple(m, "Balance on last day", m >= c.min) }
    }
    val gap = measure - c.min
    val hot = if (ok) MW.Mint else Color(0xFFFF8FA0)
    Column(Modifier.fillMaxWidth().glass(MwShape(26.dp), if (ok) MW.Accent else Color(0xFFFF6B81)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Lbl(label, Modifier.weight(1f))
            Box(Modifier.clip(MwShape(99.dp)).background(hot.copy(alpha = 0.25f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(if (ok) "Stays above minimum" else "Falls below minimum", style = MwTs(color = hot, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold))
            }
        }
        Text(s.money(measure), style = numStyle(40f, hot))
        Text(
            if (ok) s.money(kotlin.math.abs(gap)) + " above the " + s.money(c.min) + " minimum." else s.money(kotlin.math.abs(gap)) + " under the " + s.money(c.min) + " minimum. A " + s.money(cfg.fee) + " charge may apply.",
            style = MwTs(color = MW.Dim, fontSize = 13.5.sp, lineHeight = 19.sp),
        )
        Row { Text("Balance on " + shortDate(dd), Modifier.weight(1f), style = MwTs(color = Color.White, fontSize = 14.sp)); Text(s.money(c.cur + signed), style = MwTs(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)) }
        if (!ok && cfg.rule != "daily") {
            val fix = if (cfg.rule == "avg") ceil(kotlin.math.abs(gap) * nDays / (p.end - dd + 1)) else kotlin.math.abs(gap)
            Row { Text("Deposit to fix it", Modifier.weight(1f), style = MwTs(color = MW.Amber, fontSize = 14.sp, fontWeight = FontWeight.Bold)); Text(s.money(fix), style = MwTs(color = MW.Amber, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)) }
        }
    }
}

/* ============================ ENTRIES ============================ */

@Composable
fun BankEntries(s: AppState, a: Account, onRecord: (String, String) -> Unit, onEdit: (Rec) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GhostButton("Money in", Modifier.weight(1f), MW.Mint, "plus") { onRecord("income", a.id) }
        GhostButton("Money out", Modifier.weight(1f), MW.Rose, "minus") { onRecord("expense", a.id) }
        GhostButton("Check", Modifier.weight(1f), MW.Amber, "check") { onRecord("check", a.id) }
    }
    val es = s.recs.filter { it.acct == a.id || it.to == a.id }.sortedWith(compareByDescending<Rec> { it.date }.thenByDescending { it.ts })
    if (es.isEmpty()) { EmptyCard("receipt", "No entries yet.\nAdd the money you put in, the money you take out, and the balance your bank shows."); return }
    es.take(80).forEach { r ->
        val incoming = r.type == "income" || (r.type == "transfer" && r.to == a.id)
        val v = if (r.type == "check") r.amount else if (incoming) (if (r.type == "transfer" && r.toAmount != null) r.toAmount else r.amount) else -r.amount
        val tag = if (r.type == "check") "Check" else if (incoming) "In" else "Out"
        val icon = if (r.type == "transfer") "⇄" else if (r.type == "check") "⚖" else s.cat(r.cat)?.icon ?: if (incoming) "+" else "−"
        ItemRow(
            icon, fmtDate(r.date), tag + (if (r.note.isNotBlank()) " · " + r.note else ""), MW.Dim, if (r.type == "check") MW.Amber else if (incoming) MW.Accent else Color.White,
            trailing = { Text((if (r.type == "check") "= " else "") + s.money(v), style = MwTs(color = if (r.type == "check") MW.Amber else if (incoming) MW.Mint else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)) },
        ) { onEdit(r) }
    }
    if (es.size > 80) Text("Showing the latest 80 of ${es.size}. Older ones are in Search.", style = MwTs(color = MW.Faint, fontSize = 12.sp))
}
