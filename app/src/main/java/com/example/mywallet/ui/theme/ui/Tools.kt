package com.example.mywallet.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.data.Account
import com.example.mywallet.data.AppState
import com.example.mywallet.data.Bill
import com.example.mywallet.data.Cat
import com.example.mywallet.data.Debt
import com.example.mywallet.data.Fav
import com.example.mywallet.data.Goal
import com.example.mywallet.data.Store
import com.example.mywallet.data.acc
import com.example.mywallet.data.paidSoFar
import com.example.mywallet.data.left
import com.example.mywallet.data.balBase
import com.example.mywallet.data.balanceNow
import com.example.mywallet.data.bankCalc
import com.example.mywallet.data.BillDue
import com.example.mywallet.data.billDue
import com.example.mywallet.data.cat
import com.example.mywallet.data.dn
import com.example.mywallet.data.dueText
import com.example.mywallet.data.fromDn
import com.example.mywallet.data.money
import com.example.mywallet.data.todayDn
import com.example.mywallet.data.todayStr
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private fun toast(ctx: android.content.Context, m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()
private fun ordinal(n: Int): String = n.toString() + (if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" })
private fun AppState.activeAccts() = accounts.filter { !it.ignored }
private fun AppState.goalSaved(g: Goal): Double {
    val a = g.acct?.takeIf { it.isNotEmpty() }?.let { acc(it) }
    return if (a != null) maxOf(0.0, balBase(a)) else g.contribs.sumOf { it.amount }
}
private fun num(t: String): Double = t.toDoubleOrNull() ?: 0.0

private fun fmtNum(v: Double): String = if (v == Math.floor(v)) v.toLong().toString() else String.format(java.util.Locale.US, "%.2f", v)
private fun fmt(d: Double): String = java.math.BigDecimal.valueOf(d).stripTrailingZeros().toPlainString()

/* ============================ shared look for bills and debts ============================ */

@Composable
private fun Pillow(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(99.dp)).background(color.copy(alpha = 0.18f)).padding(horizontal = 9.dp, vertical = 3.dp)) {
        Text(text, maxLines = 1, style = TextStyle(color = color, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold))
    }
}

@Composable
private fun SumTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.glass(RoundedCornerShape(22.dp), tint = color).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Lbl(label, color = color)
        Text(value, maxLines = 1, style = numStyle(22f))
    }
}

@Composable
private fun Avatar(text: String, color: Color, size: Dp = 44.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.22f)))), contentAlignment = Alignment.Center) {
        if (text == "\u2713") MwIcon("check", Color.White, size * 0.5f, weight = 2.4f)
        else if (text.length == 1 && text[0].isLetter()) Text(text, style = TextStyle(color = Color.White, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.ExtraBold))
        else GlyphIcon(text, Color.White, size * 0.5f)
    }
}

@Composable
private fun ProgressLine(f: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.12f))) {
        Box(Modifier.fillMaxWidth(f.coerceIn(0.02f, 1f)).height(6.dp).clip(RoundedCornerShape(6.dp)).background(color))
    }
}

@Composable
private fun SectionHead(text: String, count: Int? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Lbl(text, Modifier.weight(1f))
        if (count != null) Text("$count", style = TextStyle(color = MW.Faint, fontSize = 12.sp, fontWeight = FontWeight.Bold))
    }
}

/* ============================ BILLS ============================ */

@Composable
fun BillsScreen(s: AppState, store: Store, onClose: () -> Unit) {
    var form by remember { mutableStateOf<Bill?>(null) }
    var adding by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(false) }
    val t = todayDn()
    val ym = todayStr().take(7)
    val due = s.bills.filter { ym !in it.paid }.map { it to billDue(it, t) }.sortedBy { it.second.dn }
    val paid = s.bills.filter { ym in it.paid }
    val dueSum = due.sumOf { it.first.amount }
    val paidSum = paid.sumOf { it.amount }
    ToolShell("Bills", onClose, "New bill", { adding = true }) {
        if (s.bills.isEmpty()) EmptyCard("receipt", "No bills yet.\nAdd rent, load or subscriptions and they show up on your Plan page.")
        else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SumTile("Still to pay", s.money(dueSum), MW.Amber, Modifier.weight(1f))
                SumTile("Paid this month", s.money(paidSum), MW.Accent, Modifier.weight(1f))
            }
        }
        if (due.isNotEmpty()) SectionHead("To pay", due.size)
        due.forEach { (b, d) -> BillCard(s, store, b, d, t, false) { form = b } }
        if (paid.isNotEmpty()) SectionHead("Paid this month", paid.size)
        paid.forEach { b -> BillCard(s, store, b, billDue(b, t), t, true) { form = b } }
        val hist = s.bills.flatMap { b -> b.paid.map { Triple(it, b, b.amount) } }.filter { it.first != ym }.sortedByDescending { it.first }
        if (hist.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { history = !history }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Lbl("History", Modifier.weight(1f))
                Text(if (history) "Hide" else "Show " + hist.size, style = TextStyle(color = MW.Mint, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
            }
            if (history) Column(Modifier.fillMaxWidth().glassLite(RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 6.dp)) {
                hist.take(40).forEachIndexed { i, (m, b, amt) ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(b.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                            Text(monthLabel(m), style = TextStyle(color = MW.Dim, fontSize = 12.sp))
                        }
                        Text(s.money(amt), style = TextStyle(color = MW.Dim, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
                    }
                }
            }
        }
    }
    if (adding || form != null) BillForm(s, store, form) { adding = false; form = null }
}

@Composable
private fun BillCard(s: AppState, store: Store, b: Bill, due: BillDue, t: Long, isPaid: Boolean, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    val k = due.dn - t
    val late = !isPaid && k < 0
    val soon = !isPaid && k in 0..3
    val tone = if (isPaid) MW.Mint else if (late) MW.Rose else if (soon) MW.Amber else MW.Dim
    Column(Modifier.fillMaxWidth().glassLite(RoundedCornerShape(22.dp)).clickable { onOpen() }.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(s.cat(b.cat)?.icon ?: "🧾", if (isPaid) MW.Accent else if (late) Color(0xFFFF6B81) else MW.Amber)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(b.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pillow(if (isPaid) "Paid" else dueText(due.dn), tone)
                    Text("every " + ordinal(b.day), style = TextStyle(color = MW.Faint, fontSize = 12.sp))
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.money(b.amount), style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"))
                if (!isPaid) Box(Modifier.clip(RoundedCornerShape(99.dp)).background(Brush.linearGradient(listOf(MW.Mint, MW.Accent))).clickable {
                    if (s.acc(b.acct) == null) toast(ctx, "Pick a pay-from account first")
                    else {
                        store.payBill(b.id, due.key, buildJsonObject {
                            put("id", store.newId()); put("ts", System.currentTimeMillis()); put("type", "expense"); put("acct", b.acct); put("to", "")
                            put("cat", b.cat); put("amount", b.amount); put("date", todayStr()); put("note", b.name)
                        })
                        toast(ctx, b.name + " paid")
                    }
                }.padding(horizontal = 16.dp, vertical = 6.dp)) { Text("Pay", style = TextStyle(color = MW.OnAccent, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)) }
            }
        }
    }
}

@Composable
private fun BillForm(s: AppState, store: Store, edit: Bill?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(edit?.name ?: "") }
    var amount by remember { mutableStateOf(edit?.let { fmt(it.amount) } ?: "") }
    var day by remember { mutableStateOf(edit?.day?.toString() ?: "1") }
    var acct by remember { mutableStateOf(edit?.acct?.ifEmpty { null } ?: s.activeAccts().firstOrNull()?.id ?: "") }
    var cat by remember { mutableStateOf(edit?.cat ?: s.cats.firstOrNull { it.id == "e_Bills" }?.id ?: "") }
    var del by remember { mutableStateOf(false) }
    FormPanel(if (edit == null) "New bill" else "Edit bill", onClose) {
        FText("Name", name, { name = it }, "e.g. Boarding house")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FText("Amount", amount, { amount = it }, "0", decimal = true, modifier = Modifier.weight(1f))
            FText("Due day (1-31)", day, { day = it.filter { c -> c.isDigit() }.take(2) }, "1", decimal = true, modifier = Modifier.weight(1f))
        }
        FChips("Pay from", s.activeAccts().map { opt(it.id, it.name, it.icon) }, acct, { acct = it })
        FChips("Category", s.cats.filter { it.type == "expense" && !it.ignored }.map { opt(it.id, it.name, it.icon) }, cat, { cat = it })
        BigButton("Save") {
            val a = num(amount); val d = day.toIntOrNull() ?: 0
            when {
                name.isBlank() -> toast(ctx, "Name the bill")
                a <= 0 -> toast(ctx, "Enter an amount")
                d !in 1..31 -> toast(ctx, "Pick a day from 1 to 31")
                else -> {
                    val o = buildJsonObject { put("name", name.trim()); put("amount", a); put("day", d); put("acct", acct); put("cat", cat) }
                    if (edit == null) store.addItem("bills", JsonObject(o + mapOf("id" to JsonPrimitive(store.newId()), "paid" to JsonArray(emptyList()))))
                    else store.updateItem("bills", edit.id, o)
                    toast(ctx, "Bill saved"); onClose()
                }
            }
        }
        if (edit != null) GhostButton("Delete", Modifier.fillMaxWidth(), Color(0xFFFF8FA0), "trash") { del = true }
    }
    if (del && edit != null) ConfirmDialog("Delete this bill?", edit.name, "Delete", { store.deleteItem("bills", edit.id); del = false; onClose() }, { del = false })
}

/* ============================ DEBTS ============================ */

@Composable
fun DebtsScreen(s: AppState, store: Store, focus: String? = null, onClose: () -> Unit) {
    var view by remember { mutableStateOf<String?>(focus) }
    var adding by remember { mutableStateOf(false) }
    var split by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(false) }
    val t = todayDn()
    val open = s.debts.filter { !it.paid }.sortedBy { if (it.due.isEmpty()) Long.MAX_VALUE else dn(it.due) }
    val theirs = open.filter { it.theyOweMe }
    val mine = open.filter { !it.theyOweMe }
    val done = s.debts.filter { it.paid }.reversed()
    ToolShell("Debts", onClose, "New debt", { adding = true }) {
        if (s.debts.isEmpty()) EmptyCard("swap", "No debts yet.\nTrack money you lent or borrowed. Your balance changes when someone pays.")
        else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SumTile("Owed to you", s.money(theirs.sumOf { it.left() }), MW.Accent, Modifier.weight(1f))
                SumTile("You owe", s.money(mine.sumOf { it.left() }), Color(0xFFFF6B81), Modifier.weight(1f))
            }
        }
        GhostButton("Split a bill with friends", Modifier.fillMaxWidth(), icon = "users") { split = true }
        if (theirs.isNotEmpty()) SectionHead("Owed to you", theirs.size)
        theirs.forEach { d -> DebtCard(s, d, t) { view = d.id } }
        if (mine.isNotEmpty()) SectionHead("You owe", mine.size)
        mine.forEach { d -> DebtCard(s, d, t) { view = d.id } }
        if (done.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { history = !history }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Lbl("History", Modifier.weight(1f))
                Text(if (history) "Hide" else "Show " + done.size, style = TextStyle(color = MW.Mint, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
            }
            if (history) done.forEach { d -> DebtCard(s, d, t) { view = d.id } }
        }
    }
    if (adding) DebtForm(store) { adding = false }
    if (split) SplitForm(s, store) { split = false }
    view?.let { id -> s.debts.firstOrNull { it.id == id }?.let { d -> DebtView(s, store, d) { view = null } } ?: run { view = null } }
}

@Composable
private fun DebtCard(s: AppState, d: Debt, t: Long, onOpen: () -> Unit) {
    val tone = if (d.theyOweMe) MW.Accent else Color(0xFFFF6B81)
    val late = !d.paid && d.due.isNotEmpty() && dn(d.due) < t
    Column(Modifier.fillMaxWidth().glassLite(RoundedCornerShape(22.dp)).clickable { onOpen() }.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(if (d.paid) "\u2713" else d.who.trim().take(1).uppercase().ifEmpty { "?" }, tone)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(d.who, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (d.paid) Pillow(if (d.theyOweMe) "Paid you back" else "You paid back", MW.Mint)
                    else if (d.due.isNotEmpty()) Pillow(dueText(dn(d.due)), if (late) MW.Rose else MW.Amber)
                    else Pillow(if (d.theyOweMe) "Owes you" else "You owe", tone)
                    if (d.note.isNotBlank()) Text(d.note, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = MW.Faint, fontSize = 12.sp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(s.money(if (d.paid) d.amount else d.left()), style = TextStyle(color = if (d.paid) MW.Dim else if (d.theyOweMe) MW.Mint else Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"))
                if (!d.paid && d.pays.isNotEmpty()) Text("of " + s.money(d.amount), style = TextStyle(color = MW.Faint, fontSize = 11.5.sp))
            }
        }
        if (!d.paid && d.pays.isNotEmpty()) ProgressLine((d.paidSoFar() / d.amount).toFloat(), tone)
    }
}

@Composable
private fun DebtForm(store: Store, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var mine by remember { mutableStateOf("1") }
    var who by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    FormPanel("New debt", onClose) {
        FChips("", listOf(opt("1", "They owe me"), opt("0", "I owe them")), mine, { mine = it }, scroll = false)
        FText("Who", who, { who = it }, "Name")
        FText("Amount", amount, { amount = it }, "0", decimal = true)
        FDate("Due date (optional)", due, { due = it }, optional = true)
        FText("Note", note, { note = it }, "Optional")
        Text("This only tracks the debt. Your balances change when you mark it as paid.", style = TextStyle(color = MW.Dim, fontSize = 12.5.sp))
        BigButton("Save") {
            val a = num(amount)
            if (who.isBlank()) toast(ctx, "Who is it?") else if (a <= 0) toast(ctx, "Enter an amount")
            else {
                store.addItem("debts", buildJsonObject {
                    put("id", store.newId()); put("who", who.trim()); put("amount", a); put("theyOweMe", mine == "1"); put("date", todayStr()); put("due", due); put("note", note.trim()); put("paid", false)
                })
                toast(ctx, "Debt saved"); onClose()
            }
        }
    }
}

@Composable
private fun DebtView(s: AppState, store: Store, d: Debt, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var acct by remember { mutableStateOf(s.activeAccts().firstOrNull()?.id ?: "") }
    var del by remember { mutableStateOf(false) }
    val left = d.left()
    var pay by remember { mutableStateOf(if (left > 0) fmtNum(left) else "") }
    val tone = if (d.theyOweMe) MW.Accent else Color(0xFFFF6B81)
    FormPanel(d.who, onClose) {
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp), tint = tone).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Avatar(if (d.paid) "\u2713" else d.who.trim().take(1).uppercase().ifEmpty { "?" }, tone, 56.dp)
            Lbl(if (d.paid) "Settled" else if (d.theyOweMe) "Still owes you" else "You still owe", color = tone)
            Text(s.money(if (d.paid) d.amount else left), style = numStyle(38f, if (d.paid) MW.Dim else Color.White))
            if (d.pays.isNotEmpty() && !d.paid) {
                Text("of " + s.money(d.amount) + " · paid " + s.money(d.paidSoFar()), style = TextStyle(color = MW.Dim, fontSize = 13.sp))
                ProgressLine((d.paidSoFar() / d.amount).toFloat(), tone)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (d.due.isNotEmpty()) Pillow("Due " + fmtDate(d.due), if (!d.paid && dn(d.due) < todayDn()) MW.Rose else MW.Amber)
                if (d.date.isNotEmpty()) Pillow("Since " + fmtDate(d.date), MW.Dim)
            }
            if (d.note.isNotBlank()) Text(d.note, style = TextStyle(color = MW.Dim, fontSize = 13.sp))
        }
        if (d.pays.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().glassLite(RoundedCornerShape(22.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Lbl("Payments")
                d.pays.forEach { p ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(tone))
                        Spacer(Modifier.width(10.dp))
                        Text(fmtDate(p.date), Modifier.weight(1f), style = TextStyle(color = MW.Dim, fontSize = 13.sp))
                        Text(s.money(p.amount), style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
                    }
                }
            }
        }
        if (!d.paid) {
            FText(if (d.theyOweMe) "They paid you" else "You paid", pay, { pay = it }, "0", decimal = true)
            FChips(if (d.theyOweMe) "Money goes into" else "Money comes out of", s.activeAccts().map { opt(it.id, it.name, it.icon) }, acct, { acct = it })
            Text("Pay part of it, or all of it. What is still owed stays on the list.", style = TextStyle(color = MW.Dim, fontSize = 12.5.sp))
            BigButton("Record payment", icon = "check") {
                val a = num(pay)
                if (s.acc(acct) == null) toast(ctx, "Choose an account")
                else if (a <= 0) toast(ctx, "Enter how much was paid")
                else {
                    val amt = minOf(a, left)
                    store.payDebt(d.id, amt, todayStr(), buildJsonObject {
                        put("id", store.newId()); put("ts", System.currentTimeMillis()); put("type", if (d.theyOweMe) "income" else "expense"); put("acct", acct); put("to", "")
                        put("cat", if (d.theyOweMe) "i_Other" else "e_Other"); put("amount", amt); put("date", todayStr())
                        put("note", if (d.theyOweMe) d.who + " paid back" else "Paid back " + d.who)
                    })
                    toast(ctx, if (amt >= left - 0.005) "Debt settled" else "Payment saved"); onClose()
                }
            }
        }
        GhostButton("Delete", Modifier.fillMaxWidth(), Color(0xFFFF8FA0), "trash") { del = true }
    }
    if (del) ConfirmDialog("Delete this debt?", d.who + " · " + s.money(d.amount), "Delete", { store.deleteItem("debts", d.id); del = false; onClose() }, { del = false })
}

/* ============================ GOALS ============================ */

@Composable
private fun Ring(p: Float, size: Dp, color: Color, stroke: Float, content: @Composable () -> Unit) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = stroke.dp.toPx()
            drawCircle(Color.White.copy(alpha = 0.12f), radius = (this.size.width - sw) / 2f, style = Stroke(sw))
            drawArc(color, -90f, 360f * p.coerceIn(0f, 1f), false, Offset(sw / 2, sw / 2), Size(this.size.width - sw, this.size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun GoalsScreen(s: AppState, store: Store, onClose: () -> Unit) {
    var view by remember { mutableStateOf<Goal?>(null) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Goal?>(null) }
    ToolShell("Goals", onClose, "New goal", { adding = true }) {
        if (s.goals.isEmpty()) EmptyCard("target", "No goals yet.\nPick something to save for, like a laptop or a trip.")
        s.goals.forEach { g ->
            val saved = s.goalSaved(g); val p = if (g.target > 0) (saved / g.target).toFloat() else 0f
            val col = GOAL_COL[((g.color ?: 0) % 8 + 8) % 8]
            Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).clickable { view = g }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Ring(p, 66.dp, col, 7f) { Text("${(p * 100).toInt()}%", style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { GlyphIcon(g.icon.ifBlank { "🎯" }, col, 18.dp); Spacer(Modifier.width(8.dp)); Text(g.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)) }
                    Text(s.money(saved) + " of " + s.money(g.target), style = TextStyle(color = MW.Dim, fontSize = 12.5.sp))
                    if (p >= 1f) Text("Goal reached!", style = TextStyle(color = MW.Mint, fontSize = 12.5.sp, fontWeight = FontWeight.Bold))
                    else if (g.deadline.isNotEmpty()) {
                        val w = maxOf(1.0, Math.ceil((dn(g.deadline) - todayDn()) / 7.0)); Text("Save " + s.money((g.target - saved) / w) + " a week for " + fmtDate(g.deadline), style = TextStyle(color = MW.Dim, fontSize = 12.5.sp))
                    }
                }
            }
        }
        if (s.goals.isNotEmpty()) Text("Link a goal to an account and that account grows the goal on its own.", style = TextStyle(color = MW.Faint, fontSize = 12.sp))
    }
    if (adding) GoalForm(s, store, null) { adding = false }
    editing?.let { g -> GoalForm(s, store, g) { editing = null } }
    view?.let { g0 -> val g = s.goals.firstOrNull { it.id == g0.id }; if (g == null) view = null else GoalView(s, store, g, { view = null }, { editing = g; view = null }) }
}

@Composable
private fun GoalForm(s: AppState, store: Store, edit: Goal?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(edit?.name ?: "") }
    var icon by remember { mutableStateOf(edit?.icon?.ifBlank { null } ?: "💻") }
    var acct by remember { mutableStateOf(edit?.acct ?: "") }
    var target by remember { mutableStateOf(edit?.let { fmt(it.target) } ?: "") }
    var deadline by remember { mutableStateOf(edit?.deadline ?: "") }
    var color by remember { mutableStateOf(edit?.color ?: s.goals.size % 8) }
    FormPanel(if (edit == null) "New goal" else "Edit goal", onClose) {
        FText("What are you saving for?", name, { name = it }, "e.g. New laptop")
        FIcons("Icon", EMO_GOAL, icon, { icon = it })
        FChips("Track an account (optional)", listOf(opt("", "No, I add by hand")) + s.accounts.map { opt(it.id, it.name, it.icon) }, acct, { acct = it })
        FText("Target amount", target, { target = it }, "0", decimal = true)
        FDate("Deadline (optional)", deadline, { deadline = it }, optional = true)
        Column {
            Lbl("Colour", Modifier.padding(bottom = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GOAL_COL.forEachIndexed { i, c ->
                    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(12.dp)).background(c).border(if (color == i) 3.dp else 0.dp, Color.White, RoundedCornerShape(12.dp)).clickable { color = i })
                }
            }
        }
        BigButton("Save") {
            val t = num(target)
            if (name.isBlank()) toast(ctx, "Name your goal") else if (t <= 0) toast(ctx, "Enter a target amount")
            else {
                val o = buildJsonObject { put("name", name.trim()); put("icon", icon); put("target", t); put("deadline", deadline); put("color", color); put("acct", acct) }
                if (edit == null) store.addItem("goals", JsonObject(o + mapOf("id" to JsonPrimitive(store.newId()), "contribs" to JsonArray(emptyList()))))
                else store.updateItem("goals", edit.id, o)
                toast(ctx, "Goal saved"); onClose()
            }
        }
    }
}

@Composable
private fun GoalView(s: AppState, store: Store, g: Goal, onClose: () -> Unit, onEdit: () -> Unit) {
    val ctx = LocalContext.current
    var amt by remember { mutableStateOf("") }
    var del by remember { mutableStateOf(false) }
    val saved = s.goalSaved(g); val p = if (g.target > 0) (saved / g.target).toFloat() else 0f
    val col = GOAL_COL[((g.color ?: 0) % 8 + 8) % 8]
    val tracked = g.acct?.isNotEmpty() == true && s.acc(g.acct) != null
    fun move(sign: Double) {
        val v = num(amt)
        if (v <= 0) return toast(ctx, "Enter an amount")
        val raw = store.rawItem("goals", g.id) ?: return
        val old = (raw["contribs"] as? JsonArray) ?: JsonArray(emptyList())
        val c = buildJsonObject { put("date", todayStr()); put("amount", sign * v) }
        store.updateItem("goals", g.id, buildJsonObject { put("contribs", JsonArray(old + c)) })
        amt = ""
        toast(ctx, if (sign > 0) "Added to your goal" else "Taken out of your goal")
    }
    FormPanel(g.name, onClose) {
        Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Ring(p, 96.dp, col, 10f) { Text("${(p * 100).toInt()}%", style = numStyle(22f)) }
            Spacer(Modifier.width(16.dp))
            Column {
                Lbl("Saved"); Text(s.money(saved), style = numStyle(28f))
                Text("of " + s.money(g.target), style = TextStyle(color = MW.Dim, fontSize = 13.sp))
            }
        }
        if (tracked) Text("Tracking " + (s.acc(g.acct!!)?.name ?: "") + ". The goal grows as that account grows.", style = TextStyle(color = MW.Dim, fontSize = 13.sp))
        else {
            FText("Amount", amt, { amt = it }, "0", decimal = true)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { BigButton("Add", icon = "plus") { move(1.0) } }
                GhostButton("Take out", Modifier.weight(1f).height(54.dp)) { move(-1.0) }
            }
        }
        if (g.contribs.isNotEmpty()) {
            Lbl("History")
            g.contribs.takeLast(6).reversed().forEach { c ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(fmtDate(c.date), Modifier.weight(1f), style = TextStyle(color = MW.Dim, fontSize = 13.sp))
                    Text(s.money(c.amount, signed = true), style = TextStyle(color = if (c.amount < 0) MW.Rose else MW.Mint, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton("Edit", Modifier.weight(1f), icon = "tag") { onEdit() }
            GhostButton("Delete", Modifier.weight(1f), Color(0xFFFF8FA0), "trash") { del = true }
        }
    }
    if (del) ConfirmDialog("Delete this goal?", "Its savings history is deleted too.", "Delete", { store.deleteItem("goals", g.id); del = false; onClose() }, { del = false })
}

/* ============================ CATEGORIES ============================ */

@Composable
fun CategoriesScreen(s: AppState, store: Store, onClose: () -> Unit) {
    var addType by remember { mutableStateOf<String?>(null) }
    var edit by remember { mutableStateOf<Cat?>(null) }
    ToolShell("Categories", onClose, "New", { addType = "expense" }) {
        listOf("expense" to "Expenses", "income" to "Income").forEach { (type, title) ->
            Lbl(title, Modifier.padding(top = 6.dp))
            s.cats.filter { it.type == type }.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { c ->
                        Column(
                            Modifier.weight(1f).height(86.dp).glass(RoundedCornerShape(22.dp)).clickable { edit = c }.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                        ) {
                            GlyphIcon(c.icon, Color.White.copy(alpha = if (c.ignored) 0.4f else 0.95f), 26.dp)
                            Spacer(Modifier.height(6.dp))
                            Text(c.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White.copy(alpha = if (c.ignored) 0.45f else 1f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Text("Tap a category to rename it, hide it or delete it.", style = TextStyle(color = MW.Faint, fontSize = 12.sp))
    }
    addType?.let { CatForm(s, store, null, it) { addType = null } }
    edit?.let { CatForm(s, store, it, it.type) { edit = null } }
}

@Composable
private fun CatForm(s: AppState, store: Store, edit: Cat?, type0: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var type by remember { mutableStateOf(type0) }
    var name by remember { mutableStateOf(edit?.name ?: "") }
    var icon by remember { mutableStateOf(edit?.icon?.ifBlank { null } ?: "🧩") }
    var del by remember { mutableStateOf(false) }
    FormPanel(if (edit == null) "New category" else "Edit category", onClose) {
        if (edit == null) FChips("", listOf(opt("expense", "Expense"), opt("income", "Income")), type, { type = it }, scroll = false)
        FText("Name", name, { name = it }, "e.g. Coffee")
        FIcons("Icon", EMO_CAT, icon, { icon = it })
        BigButton("Save") {
            if (name.isBlank()) toast(ctx, "Name it")
            else {
                if (edit == null) store.addItem("cats", buildJsonObject { put("id", store.newId()); put("name", name.trim()); put("icon", icon); put("type", type); put("ignored", false) })
                else store.updateItem("cats", edit.id, buildJsonObject { put("name", name.trim()); put("icon", icon) })
                toast(ctx, "Saved"); onClose()
            }
        }
        if (edit != null) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton(if (edit.ignored) "Show again" else "Hide", Modifier.weight(1f), icon = if (edit.ignored) "eye" else "eyeoff") {
                store.updateItem("cats", edit.id, buildJsonObject { put("ignored", !edit.ignored) }); onClose()
            }
            GhostButton("Delete", Modifier.weight(1f), Color(0xFFFF8FA0), "trash") { del = true }
        }
    }
    if (del && edit != null) ConfirmDialog("Delete this category?", "This also deletes all its records and budgets. Hide it instead to keep your history.", "Delete", { store.deleteCategory(edit.id); del = false; onClose() }, { del = false })
}

/* ============================ QUICK ADD ============================ */

@Composable
fun QuickAddScreen(s: AppState, store: Store, onClose: () -> Unit) {
    val ctx = LocalContext.current
    ToolShell("Quick add", onClose) {
        if (s.favs.isEmpty()) EmptyCard("zap", "No quick-adds yet.\nWhen you add a record, turn on \"Save as quick-add\" and it shows up here for one-tap logging.")
        s.favs.forEach { f ->
            val a = s.acc(f.acct)
            ItemRow(f.icon.ifBlank { "⚡" }, f.name, (a?.name ?: "Missing account") + " · tap to add today", if (a == null) MW.Rose else MW.Dim, if (f.type == "income") MW.Accent else MW.Amber,
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.money(f.amount), style = TextStyle(color = if (f.type == "income") MW.Mint else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable { store.deleteItem("favs", f.id) }, contentAlignment = Alignment.Center) { MwIcon("trash", MW.Dim, 16.dp) }
                    }
                },
            ) {
                if (a == null) toast(ctx, "That account no longer exists")
                else {
                    store.addRec(buildJsonObject {
                        put("id", store.newId()); put("ts", System.currentTimeMillis()); put("type", f.type); put("acct", f.acct); put("to", ""); put("cat", f.cat)
                        put("amount", f.amount); put("date", todayStr()); put("note", f.name)
                    })
                    toast(ctx, f.name + " added")
                }
            }
        }
    }
}

/* ============================ BANK TRACKER ============================ */

@Composable
fun BankScreen(s: AppState, store: Store, startId: String?, onRecord: (type: String, acct: String) -> Unit, onEdit: (com.example.mywallet.data.Rec) -> Unit = {}, onClose: () -> Unit) {
    val banks = s.accounts.filter { it.kind == "bank" && !it.ignored }
    var sel by remember { mutableStateOf(startId?.takeIf { id -> banks.any { it.id == id } } ?: banks.firstOrNull()?.id ?: "") }
    var settings by remember { mutableStateOf(false) }
    var view by remember { mutableStateOf("home") }
    var addBank by remember { mutableStateOf(false) }
    ToolShell("Bank tracker", onClose, if (banks.isEmpty()) null else "Rules", { settings = true }) {
        val a = banks.firstOrNull { it.id == sel } ?: banks.firstOrNull()
        if (a == null) {
            EmptyCard("landmark", "Add a bank account to track its minimum balance and see how much is safe to spend.")
            BigButton("Add bank account", icon = "plus") { addBank = true }
        } else {
            if (banks.size > 1) FChips("", banks.map { opt(it.id, it.name, it.icon) }, a.id, { sel = it })
            val c = s.bankCalc(a)
            val cfg = a.bank
            if (c != null && cfg != null) FChips("", listOf(opt("home", "Home"), opt("chart", "Chart"), opt("wi", "What if"), opt("ent", "Entries")), view, { view = it }, scroll = false)
            if (c != null && cfg != null && view == "chart") BankChart(s, a, c)
            if (c != null && cfg != null && view == "wi") BankWhatIf(s, a, c)
            if (c != null && cfg != null && view == "ent") BankEntries(s, a, onRecord, onEdit)
            if (c != null && cfg != null && view == "home") {
                val ok = c.verdict == "ok" || c.verdict == "locked"
                val tint = if (ok) MW.Accent else Color(0xFFFF6B81)
                val (vt, main, line) = when (c.verdict) {
                    "ok" -> Triple("On track", s.money(maxOf(0.0, c.safe)), "is safe to spend. Keep at least " + s.money(c.required) + when (cfg.rule) { "avg" -> " in the account from today."; "daily" -> " every day."; else -> " until the period ends." })
                    "locked" -> Triple("Locked in", s.money(maxOf(0.0, c.cur)), "is all yours. The average already reaches " + s.money(c.min) + " for this period.")
                    "add" -> Triple("Add money", s.money(c.need), "to add so you stay clear of the " + s.money(cfg.fee) + " charge.")
                    else -> Triple("Fee applies this period", s.money(cfg.fee), "charge likely. Your balance dipped under " + s.money(c.min) + " this period.")
                }
                val hot = if (ok) MW.Mint else Color(0xFFFF8FA0)
                val tag = when (c.verdict) { "ok" -> "SAFE TO SPEND"; "locked" -> "ALL YOURS"; "add" -> "ADD TO STAY SAFE"; else -> "FEE LIKELY" }
                Column(
                    Modifier.fillMaxWidth().glass(RoundedCornerShape(30.dp), tint)
                        .border(1.5.dp, Brush.linearGradient(listOf(hot.copy(alpha = 0.9f), hot.copy(alpha = 0.15f))), RoundedCornerShape(30.dp))
                        .background(Brush.verticalGradient(listOf(hot.copy(alpha = 0.22f), Color.Transparent))).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.clip(RoundedCornerShape(99.dp)).background(hot.copy(alpha = 0.28f)).border(1.dp, hot.copy(alpha = 0.7f), RoundedCornerShape(99.dp)).padding(horizontal = 12.dp, vertical = 5.dp)) {
                            Text(tag, style = TextStyle(color = hot, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp))
                        }
                        Spacer(Modifier.weight(1f))
                        Text(vt, style = TextStyle(color = MW.Dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
                    }
                    Text(main, style = numStyle(52f, hot))
                    Text(highlightMoney(line, s.settings.cur, hot), style = TextStyle(color = MW.Dim, fontSize = 14.5.sp, lineHeight = 21.sp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        "Balance now" to s.money(c.cur),
                        (if (cfg.rule == "avg") "Average so far" else "Minimum") to s.money(if (cfg.rule == "avg") c.avgNow else c.min),
                        "Lowest to keep" to s.money(c.required),
                    ).forEach { (l, v) ->
                        val key = l == "Lowest to keep"
                        Column(Modifier.weight(1f).glass(RoundedCornerShape(20.dp), if (key) MW.Amber else null).then(if (key) Modifier.border(1.dp, MW.Amber.copy(alpha = 0.6f), RoundedCornerShape(20.dp)) else Modifier).padding(12.dp)) {
                            Text(l, maxLines = 2, style = TextStyle(color = if (key) MW.Amber else MW.Dim, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = if (key) FontWeight.Bold else FontWeight.Normal))
                            Spacer(Modifier.height(4.dp)); Text(v, maxLines = 1, style = TextStyle(color = if (key) MW.Amber else Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"))
                        }
                    }
                }
                val n = (c.period.end - c.period.start + 1).toInt()
                val elapsed = (todayDn() - c.period.start + 1).toInt().coerceIn(1, n)
                Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(fmtDate(fromDn(c.period.start)) + " to " + fmtDate(fromDn(c.period.end)), style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))) {
                        Box(Modifier.fillMaxWidth(elapsed.toFloat() / n).height(8.dp).clip(CircleShape).background(if (ok) MW.Accent else Color(0xFFFF6B81)))
                    }
                    Text("Day $elapsed of $n · ${c.daysLeft - 1} days left · charge if below: " + s.money(cfg.fee), style = TextStyle(color = MW.Dim, fontSize = 12.5.sp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton("Money in", Modifier.weight(1f), MW.Mint, "plus") { onRecord("income", a.id) }
                    GhostButton("Money out", Modifier.weight(1f), MW.Rose, "minus") { onRecord("expense", a.id) }
                    GhostButton("Check", Modifier.weight(1f), MW.Amber, "check") { onRecord("check", a.id) }
                }
                if (cfg.otherFees.isNotEmpty()) Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Lbl("Other fees (reference)")
                    cfg.otherFees.forEach { f -> Row { Text(f.n, Modifier.weight(1f), style = TextStyle(color = Color.White, fontSize = 14.sp)); Text(s.money(f.a), style = TextStyle(color = MW.Dim, fontSize = 14.sp, fontFeatureSettings = "tnum")) } }
                }
                Text("Estimate only. Banks rarely publish their exact method, so confirm the rule type and period with your bank. Before your first entry the balance counts as zero, and a negative balance counts as zero.", style = TextStyle(color = MW.Faint, fontSize = 12.sp, lineHeight = 17.sp))
            }
            GhostButton("Add another bank account", Modifier.fillMaxWidth(), icon = "plus") { addBank = true }
        }
    }
    if (settings) banks.firstOrNull { it.id == sel }?.let { BankRules(store, it) { settings = false } }
    if (addBank) AccountForm(store, "bank") { addBank = false }
}

@Composable
private fun BankRules(store: Store, a: Account, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val cfg = a.bank ?: return
    var name by remember { mutableStateOf(cfg.bankName) }
    var rule by remember { mutableStateOf(cfg.rule) }
    var min by remember { mutableStateOf(fmt(cfg.min)) }
    var fee by remember { mutableStateOf(fmt(cfg.fee)) }
    var period by remember { mutableStateOf(cfg.period) }
    var startDay by remember { mutableStateOf(cfg.startDay.toString()) }
    var interest by remember { mutableStateOf(fmt(cfg.interest)) }
    FormPanel("Bank rules", onClose) {
        FText("Bank name", name, { name = it }, "e.g. BDO")
        FChips("How the minimum is checked", listOf(opt("avg", "Average daily"), opt("daily", "Daily minimum"), opt("end", "Month-end")), rule, { rule = it })
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FText("Minimum balance", min, { min = it }, "0", decimal = true, modifier = Modifier.weight(1f))
            FText("Charge if below", fee, { fee = it }, "0", decimal = true, modifier = Modifier.weight(1f))
        }
        FChips("Period", listOf(opt("calendar", "Calendar month"), opt("custom", "Starts on a day")), period, { period = it })
        if (period == "custom") FText("Period starts on day (1-28)", startDay, { startDay = it.filter { c -> c.isDigit() }.take(2) }, "1", decimal = true)
        FText("Balance that earns interest (0 = none)", interest, { interest = it }, "0", decimal = true)
        BigButton("Save") {
            val raw = store.rawItem("accounts", a.id)
            val old = (raw?.get("bank") as? JsonObject) ?: JsonObject(emptyMap())
            val nb = JsonObject(old + mapOf(
                "bankName" to JsonPrimitive(name.trim()), "rule" to JsonPrimitive(rule), "min" to JsonPrimitive(num(min)), "fee" to JsonPrimitive(num(fee)),
                "period" to JsonPrimitive(period), "startDay" to JsonPrimitive((startDay.toIntOrNull() ?: 1).coerceIn(1, 28)), "interest" to JsonPrimitive(num(interest)),
            ))
            store.updateItem("accounts", a.id, buildJsonObject { put("bank", nb) })
            toast(ctx, "Saved"); onClose()
        }
    }
}

@Composable
fun AccountForm(store: Store, kind0: String, edit: Account? = null, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var kind by remember { mutableStateOf(edit?.kind ?: kind0) }
    var name by remember { mutableStateOf(edit?.name ?: "") }
    var icon by remember { mutableStateOf(edit?.icon?.ifBlank { null } ?: if (kind0 == "bank") "🏦" else "💵") }
    var start by remember { mutableStateOf(edit?.let { if (it.initial == 0.0) "" else fmt(it.initial) } ?: "") }
    val defaultBank = buildJsonObject { put("bankName", ""); put("rule", "avg"); put("min", 500); put("fee", 200); put("period", "calendar"); put("startDay", 1); put("interest", 0); put("otherFees", JsonArray(emptyList())) }
    FormPanel(if (edit == null) "New account" else "Edit account", onClose) {
        FChips("Type", listOf(opt("cash", "Cash / wallet"), opt("bank", "Bank account"), opt("card", "Card"), opt("ewallet", "E-wallet")), kind, { kind = it })
        FText("Name", name, { name = it }, "e.g. BDO savings")
        FIcons("Icon", EMO_ACCT, icon, { icon = it })
        FText("Starting balance", start, { start = it.filter { c -> c.isDigit() || c == '.' || c == '-' } }, "0", decimal = true)
        BigButton(if (edit == null) "Add account" else "Save") {
            if (name.isBlank()) toast(ctx, "Name the account")
            else if (edit != null) {
                val patch = buildJsonObject { put("name", name.trim()); put("icon", icon); put("kind", kind); put("initial", num(start)) }
                store.updateItem("accounts", edit.id, if (kind == "bank" && edit.bank == null) JsonObject(patch + ("bank" to defaultBank)) else patch)
                toast(ctx, "Account saved"); onClose()
            } else {
                val o = buildJsonObject {
                    put("id", store.newId()); put("name", name.trim()); put("icon", icon); put("kind", kind); put("initial", num(start)); put("openDate", todayStr()); put("ignored", false)
                }
                store.addItem("accounts", if (kind == "bank") JsonObject(o + ("bank" to defaultBank)) else o)
                toast(ctx, "Account added"); onClose()
            }
        }
    }
}

/** what you can do with an account: open the bank tracker, edit, ignore or delete */
@Composable
fun AccountActions(s: AppState, store: Store, a: Account, onBank: () -> Unit, onClose: () -> Unit) {
    var edit by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf(false) }
    val recCount = s.recs.count { it.acct == a.id || it.to == a.id }
    if (edit) { AccountForm(store, a.kind, a) { edit = false; onClose() }; return }
    FormPanel(a.name, onClose) {
        Text(s.money(s.balanceNow(a)), style = numStyle(30f))
        if (a.kind == "bank") BigButton("Open bank tracker", icon = "landmark") { onClose(); onBank() }
        GhostButton("Edit account", Modifier.fillMaxWidth(), Color.White, "edit") { edit = true }
        GhostButton(if (a.ignored) "Include in totals again" else "Ignore (leave out of totals)", Modifier.fillMaxWidth(), Color.White, "eyeoff") {
            store.updateItem("accounts", a.id, buildJsonObject { put("ignored", !a.ignored) }); onClose()
        }
        GhostButton("Delete account", Modifier.fillMaxWidth(), Color(0xFFFF8FA0), "trash") { del = true }
    }
    if (del) ConfirmDialog("Delete " + a.name + "?", "This also deletes its $recCount record${if (recCount == 1) "" else "s"}. Ignore it instead to keep your history.", "Delete", { store.deleteAccount(a.id); del = false; onClose() }, { del = false })
}
