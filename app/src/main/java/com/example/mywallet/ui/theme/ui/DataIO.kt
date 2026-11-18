package com.example.mywallet.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.data.AppState
import com.example.mywallet.data.BackupCrypto
import com.example.mywallet.data.Store
import com.example.mywallet.data.acc
import com.example.mywallet.data.cat
import com.example.mywallet.data.money
import com.example.mywallet.data.todayStr
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.abs

private fun tst(ctx: Context, m: String) = Toast.makeText(ctx, m, Toast.LENGTH_LONG).show()

/* ============================ CSV ============================ */

private fun csvCell(v: String): String = if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\"" else v

/** all records as CSV text (same columns as the web app, so it can be read back in) */
fun csvOf(s: AppState): String {
    val sb = StringBuilder("﻿")
    sb.append("Date,Type,Account,To account,Category,Amount,Currency,Note\r\n")
    s.recs.sortedWith(compareBy({ it.date }, { it.ts })).forEach { r ->
        val a = s.acc(r.acct)
        val cols = listOf(
            r.date, r.type, a?.name ?: "", s.acc(r.to)?.name ?: "", s.cat(r.cat)?.name ?: "",
            java.math.BigDecimal.valueOf(r.amount).stripTrailingZeros().toPlainString(),
            (a?.cur?.ifEmpty { null }) ?: s.settings.cur, r.note,
        )
        sb.append(cols.joinToString(",") { csvCell(it) }).append("\r\n")
    }
    return sb.toString()
}

fun parseCsv(text: String): List<List<String>> {
    val t = text.removePrefix("﻿")
    val first = t.lineSequence().firstOrNull() ?: ""
    val delim = listOf(',', ';', '\t').maxByOrNull { d -> first.count { it == d } } ?: ','
    val rows = ArrayList<List<String>>()
    var row = ArrayList<String>()
    val sb = StringBuilder()
    var q = false
    var i = 0
    while (i < t.length) {
        val c = t[i]
        if (q) {
            if (c == '"') { if (i + 1 < t.length && t[i + 1] == '"') { sb.append('"'); i++ } else q = false } else sb.append(c)
        } else if (c == '"') q = true
        else if (c == delim) { row.add(sb.toString()); sb.setLength(0) }
        else if (c == '\n') { row.add(sb.toString()); sb.setLength(0); rows.add(row); row = ArrayList() }
        else if (c != '\r') sb.append(c)
        i++
    }
    if (sb.isNotEmpty() || row.isNotEmpty()) { row.add(sb.toString()); rows.add(row) }
    return rows.filter { r -> r.any { it.isNotBlank() } }
}

private fun parseDate(raw: String, ord: String): String? {
    val t = raw.trim().substringBefore('T').substringBefore(' ')
    val p = t.split('-', '/', '.')
    if (p.size < 3) return null
    val a = p[0].toIntOrNull() ?: return null
    val b = p[1].toIntOrNull() ?: return null
    val c = p[2].toIntOrNull() ?: return null
    val y: Int; val m: Int; val d: Int
    if (p[0].length == 4) { y = a; m = b; d = c }
    else if (ord == "dmy") { d = a; m = b; y = if (c < 100) 2000 + c else c }
    else { m = a; d = b; y = if (c < 100) 2000 + c else c }
    if (m !in 1..12 || d !in 1..31 || y < 1970) return null
    return String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m, d)
}

private fun parseAmount(raw: String): Double? {
    val cleaned = raw.replace(Regex("[^0-9.\\-()]"), "")
    if (cleaned.isEmpty()) return null
    val neg = cleaned.contains("-") || cleaned.startsWith("(")
    val v = cleaned.replace("-", "").replace("(", "").replace(")", "").toDoubleOrNull() ?: return null
    return if (neg) -v else v
}

private class CsvPlan(val recs: List<JsonObject>, val newAcc: List<JsonObject>, val newCat: List<JsonObject>, val dupes: Int, val first: String, val last: String, val skipped: Int)

private class Raw(val date: String, val type: String, val amount: Double, val note: String, val cat: String, val acct: String, val to: String)

/** m = columns: 0 date, 1 amount, 2 type, 3 note, 4 category, 5 account, 6 to-account (-1 = none) */
private fun buildPlan(s: AppState, store: Store, rows: List<List<String>>, m: IntArray, ord: String, posIs: String, defAcct: String): CsvPlan {
    fun cell(r: List<String>, i: Int) = if (i >= 0 && i < r.size) r[i].trim() else ""
    val raws = ArrayList<Raw>()
    var skipped = 0
    rows.drop(1).forEach { r ->
        val d = parseDate(cell(r, m[0]), ord)
        val a = parseAmount(cell(r, m[1]))
        if (d == null || a == null || a == 0.0) { skipped++; return@forEach }
        val rawType = cell(r, m[2]).lowercase()
        val explicit = when (rawType) {
            "expense", "debit", "out", "withdrawal", "spent" -> "expense"
            "income", "credit", "in", "deposit" -> "income"
            "transfer" -> "transfer"
            "check", "balance" -> "check"
            else -> null
        }
        val type = explicit ?: (if (a < 0) (if (posIs == "expense") "income" else "expense") else posIs)
        raws.add(Raw(d, type, abs(a), cell(r, m[3]), cell(r, m[4]), cell(r, m[5]), cell(r, m[6])))
    }
    val first = raws.minOfOrNull { it.date } ?: ""
    val last = raws.maxOfOrNull { it.date } ?: ""
    val accByName = HashMap<String, String>()
    s.accounts.forEach { accByName[it.name.trim().lowercase()] = it.id }
    val catByName = HashMap<String, String>()
    s.cats.forEach { catByName[it.type + "|" + it.name.trim().lowercase()] = it.id }
    val newAcc = ArrayList<JsonObject>()
    val newCat = ArrayList<JsonObject>()
    fun accId(name: String): String {
        val k = name.trim().lowercase()
        if (k.isEmpty()) return defAcct
        accByName[k]?.let { return it }
        val id = store.newId()
        accByName[k] = id
        newAcc.add(buildJsonObject { put("id", id); put("name", name.trim()); put("icon", "👛"); put("kind", "cash"); put("initial", 0.0); put("openDate", first); put("ignored", false) })
        return id
    }
    fun catId(type: String, name: String): String {
        val k = type + "|" + name.trim().lowercase()
        if (name.isBlank()) return s.cats.firstOrNull { it.type == type && it.name.equals("Other", true) }?.id ?: s.cats.firstOrNull { it.type == type }?.id ?: ""
        catByName[k]?.let { return it }
        val id = store.newId()
        catByName[k] = id
        newCat.add(buildJsonObject { put("id", id); put("name", name.trim()); put("icon", "🏷"); put("type", type); put("ignored", false) })
        return id
    }
    val have = HashSet<String>()
    s.recs.forEach { have.add(it.date + "|" + it.type + "|" + it.acct + "|" + Math.round(it.amount * 100) + "|" + it.note.trim()) }
    val recs = ArrayList<JsonObject>()
    var dupes = 0
    val base = System.currentTimeMillis()
    raws.forEach { x ->
        val ac = accId(x.acct)
        var to = ""
        if (x.type == "transfer") { to = accId(x.to); if (x.to.isBlank() || to == ac) { skipped++; return@forEach } }
        val key = x.date + "|" + x.type + "|" + ac + "|" + Math.round(x.amount * 100) + "|" + x.note.trim()
        if (have.contains(key)) { dupes++; return@forEach }
        val cid = if (x.type == "expense" || x.type == "income") catId(x.type, x.cat) else ""
        val id = store.newId() + recs.size
        recs.add(buildJsonObject {
            put("id", id); put("ts", base + recs.size); put("type", x.type); put("acct", ac); put("to", to); put("cat", cid)
            put("amount", x.amount); put("date", x.date); put("note", x.note)
        })
    }
    return CsvPlan(recs, newAcc, newCat, dupes, first, last, skipped)
}

private fun guess(h: List<String>, vararg keys: String, not: String? = null): Int =
    h.indexOfFirst { x -> val l = x.trim().lowercase(); keys.any { l.contains(it) } && (not == null || !l.contains(not)) }

@Composable
private fun CsvImportForm(s: AppState, store: Store, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var rows by remember { mutableStateOf<List<List<String>>?>(null) }
    var m by remember { mutableStateOf(intArrayOf(-1, -1, -1, -1, -1, -1, -1)) }
    var ord by remember { mutableStateOf("mdy") }
    var posIs by remember { mutableStateOf("expense") }
    var defAcct by remember { mutableStateOf(s.accounts.firstOrNull { !it.ignored }?.id ?: "") }

    fun load(text: String) {
        val r = parseCsv(text)
        if (r.size < 2) { tst(ctx, "That does not look like a CSV with a header row."); return }
        val h = r[0]
        m = intArrayOf(guess(h, "date"), guess(h, "amount", "amt", "value"), guess(h, "type"), guess(h, "note", "description", "memo", "detail", "payee"), guess(h, "categor"), guess(h, "account", not = "to"), guess(h, "to account"))
        // a date written like 2025-03-04 is read the same way in any order
        rows = r
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val t = ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (t != null) load(t)
            } catch (e: Exception) { tst(ctx, "Could not open that file.") }
        }
    }

    FormPanel("Import from CSV", onClose) {
        val r = rows
        if (r == null) {
            Text("Choose a CSV file from your bank, a spreadsheet or another money app. Files made by MyWallet work too.", style = TextStyle(color = MW.Dim, fontSize = 13.sp, lineHeight = 19.sp))
            BigButton("Choose a file", icon = "file") { pick.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel", "*/*")) }
            GhostButton("Paste from clipboard", Modifier.fillMaxWidth(), icon = "copy") {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val t = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString() ?: ""
                if (t.isBlank()) tst(ctx, "The clipboard is empty.") else load(t)
            }
        } else {
            val head = r[0]
            fun cols(none: Boolean) = (if (none) listOf(opt("-1", "(none)")) else emptyList()) + head.mapIndexed { i, h -> opt(i.toString(), h.trim().ifEmpty { "Column ${i + 1}" }) }
            FChips("Date column", cols(false), m[0].toString(), { v -> m = m.copyOf().also { it[0] = v.toInt() } })
            FChips("Amount column", cols(false), m[1].toString(), { v -> m = m.copyOf().also { it[1] = v.toInt() } })
            FChips("Type column", cols(true), m[2].toString(), { v -> m = m.copyOf().also { it[2] = v.toInt() } })
            FChips("Note column", cols(true), m[3].toString(), { v -> m = m.copyOf().also { it[3] = v.toInt() } })
            FChips("Category column", cols(true), m[4].toString(), { v -> m = m.copyOf().also { it[4] = v.toInt() } })
            FChips("Account column", cols(true), m[5].toString(), { v -> m = m.copyOf().also { it[5] = v.toInt() } })
            FChips("Dates look like", listOf(opt("mdy", "Month/Day/Year"), opt("dmy", "Day/Month/Year")), ord, { ord = it }, scroll = false)
            FChips("No minus sign means", listOf(opt("expense", "Money out"), opt("income", "Money in")), posIs, { posIs = it }, scroll = false)
            if (m[5] < 0) FChips("Put them in account", s.accounts.filter { !it.ignored }.map { opt(it.id, it.name, it.icon) }, defAcct, { defAcct = it })
            val ok = m[0] >= 0 && m[1] >= 0
            val plan = remember(r, m.toList(), ord, posIs, defAcct) { if (ok) buildPlan(s, store, r, m, ord, posIs, defAcct) else null }
            Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (plan == null) Text("Pick the date and amount columns.", style = TextStyle(color = MW.Dim, fontSize = 13.sp))
                else {
                    Text("${plan.recs.size} record" + (if (plan.recs.size == 1) "" else "s") + " ready", style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold))
                    val extra = ArrayList<String>()
                    if (plan.recs.isNotEmpty()) extra.add(fmtDate(plan.first) + " to " + fmtDate(plan.last))
                    if (plan.newAcc.isNotEmpty()) extra.add("${plan.newAcc.size} new account" + (if (plan.newAcc.size == 1) "" else "s"))
                    if (plan.newCat.isNotEmpty()) extra.add("${plan.newCat.size} new categor" + (if (plan.newCat.size == 1) "y" else "ies"))
                    if (plan.dupes > 0) extra.add("${plan.dupes} already here, skipped")
                    if (plan.skipped > 0) extra.add("${plan.skipped} rows could not be read")
                    if (extra.isNotEmpty()) Text(extra.joinToString(" · "), style = TextStyle(color = MW.Dim, fontSize = 12.5.sp, lineHeight = 18.sp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Choose another", Modifier.weight(1f)) { rows = null }
                Column(Modifier.weight(1f)) {
                    BigButton("Import" + (if (plan != null && plan.recs.isNotEmpty()) " " + plan.recs.size else "")) {
                        if (plan == null || plan.recs.isEmpty()) tst(ctx, "Nothing new to add.")
                        else {
                            store.appendLists(mapOf("accounts" to plan.newAcc, "cats" to plan.newCat, "recs" to plan.recs))
                            tst(ctx, "Imported ${plan.recs.size} record" + (if (plan.recs.size == 1) "" else "s"))
                            onClose()
                        }
                    }
                }
            }
        }
    }
}

/* ============================ BACKUP + EXPORT SCREEN ============================ */

@Composable
fun BackupScreen(s: AppState, store: Store, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var restoreBytes by remember { mutableStateOf<ByteArray?>(null) }
    var restorePw by remember { mutableStateOf("") }
    var csvImport by remember { mutableStateOf(false) }
    var askPw by remember { mutableStateOf(false) }
    var wipe by remember { mutableStateOf(false) }
    var savePw by remember { mutableStateOf("") }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            try {
                val bytes = store.backupBytes(savePw)
                ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                store.setTopLive("lastBackup", JsonPrimitive(System.currentTimeMillis()))
                tst(ctx, "Backup saved (${bytes.size / 1024} KB)")
            } catch (e: Exception) { tst(ctx, "Could not save the backup") }
            savePw = ""
        }
    }
    val pickJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val t = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (t != null) { restorePw = ""; restoreBytes = t }
            } catch (e: Exception) { tst(ctx, "Could not open that file") }
        }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(csvOf(s).toByteArray(Charsets.UTF_8)) }
                tst(ctx, "CSV saved")
            } catch (e: Exception) { tst(ctx, "Could not save the file") }
        }
    }
    val age = if (s.lastBackup > 0) ((System.currentTimeMillis() - s.lastBackup) / 86_400_000.0).toInt() else -1

    ToolShell("Backup and export", onClose) {
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Back up", style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold))
            Text(
                (if (age < 0) "You have not made a backup yet." else if (age == 0) "Last backup: today." else "Last backup: $age day" + (if (age == 1) "" else "s") + " ago.") +
                    " Your data lives on this phone only. Backups are small (compressed) and include receipt photos.",
                style = TextStyle(color = MW.Dim, fontSize = 13.sp, lineHeight = 19.sp),
            )
            BigButton("Save backup file", icon = "cloud") { savePw = ""; askPw = true }
            GhostButton("Restore from a backup", Modifier.fillMaxWidth(), icon = "download") { pickJson.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")) }
        }
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Spreadsheet (CSV)", style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold))
            Text("Export every record to open in Excel or Google Sheets, or bring in records from your bank or another app.", style = TextStyle(color = MW.Dim, fontSize = 13.sp, lineHeight = 19.sp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Save CSV", Modifier.weight(1f), icon = "file") {
                    if (s.recs.isEmpty()) tst(ctx, "Nothing to export") else exportCsv.launch("mywallet-records-" + todayStr() + ".csv")
                }
                GhostButton("Share CSV", Modifier.weight(1f), icon = "share") {
                    if (s.recs.isEmpty()) tst(ctx, "Nothing to export")
                    else try { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, csvOf(s)).putExtra(Intent.EXTRA_SUBJECT, "MyWallet records"), "Share CSV")) } catch (e: Exception) { }
                }
            }
            GhostButton("Import from CSV", Modifier.fillMaxWidth(), icon = "upload") { csvImport = true }
        }
        var stm by remember { mutableStateOf(todayStr().take(7)) }
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Monthly statement (PDF)", style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold))
            MonthNav(stm, { stm = it })
            GhostButton("Make PDF", Modifier.fillMaxWidth(), icon = "file") { printStatement(ctx, s, stm) }
        }
        var tidy by remember { mutableStateOf(0) }
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Storage", style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold))
            Text(
                "Data: ${store.fileSize() / 1024} KB \u00B7 ${store.photoCount()} receipt photo" + (if (store.photoCount() == 1) "" else "s") + " (${store.photoBytesTotal() / 1024} KB)" +
                    "\nEverything is stored compressed and encrypted on this phone.",
                style = TextStyle(color = MW.Dim, fontSize = 13.sp, lineHeight = 19.sp),
            )
            GhostButton("Delete all my data", Modifier.fillMaxWidth(), Color(0xFFFF8FA0), "trash") { wipe = true }
            GhostButton("Clean up unused photos" + (if (tidy > 0) " ($tidy removed)" else ""), Modifier.fillMaxWidth(), icon = "trash") { tidy += store.cleanPhotos(); tst(ctx, "Done") }
        }
    }

    if (csvImport) CsvImportForm(s, store) { csvImport = false }
    if (wipe) ConfirmDialog("Delete all your data?", "Every record, account, receipt photo and setting is erased from this phone. Save a backup first if you may want it back.", "Delete everything", { wipe = false; store.resetAll() }, { wipe = false })
    if (askPw) FormPanel("Save a backup", { askPw = false }) {
        Text("Add a password to lock the backup file, or leave it empty. If you forget the password the backup cannot be opened, not even by me.", style = TextStyle(color = MW.Dim, fontSize = 13.sp, lineHeight = 19.sp))
        FText("Password (optional)", savePw, { savePw = it }, "Leave empty for no password")
        BigButton("Choose where to save", icon = "cloud") { askPw = false; exportJson.launch("mywallet-backup-" + todayStr() + ".mwbackup") }
    }
    restoreBytes?.let { bytes ->
        val enc = BackupCrypto.isEncrypted(bytes)
        FormPanel("Replace your data?", { restoreBytes = null }) {
            Text("Everything in this app is replaced by the backup. Back up first if you want to keep what you have now.", style = TextStyle(color = MW.Dim, fontSize = 13.5.sp, lineHeight = 19.sp))
            if (enc) FText("Backup password", restorePw, { restorePw = it }, "Password")
            BigButton("Replace my data", color = Color(0xFFFF6B81), textColor = Color.White) {
                val err = store.importBackupBytes(bytes, restorePw)
                when (err) {
                    null -> { restoreBytes = null; tst(ctx, "Backup restored") }
                    "NEED_PASSWORD" -> tst(ctx, "Type the backup password")
                    "WRONG_PASSWORD" -> tst(ctx, "That password is not right")
                    else -> { restoreBytes = null; tst(ctx, err) }
                }
            }
        }
    }
}

/* ============================ SPLIT A BILL ============================ */

@Composable
fun SplitForm(s: AppState, store: Store, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var total by remember { mutableStateOf("") }
    var people by remember { mutableStateOf("2") }
    var names by remember { mutableStateOf("") }
    var acct by remember { mutableStateOf(s.accounts.firstOrNull { !it.ignored }?.id ?: "") }
    var paidBy by remember { mutableStateOf("me") }
    FormPanel("Split a bill", onClose) {
        Text("Enter the total and how many people share it. Your share becomes an expense, and the rest becomes debts you can track.", style = TextStyle(color = MW.Dim, fontSize = 12.5.sp, lineHeight = 18.sp))
        FText("Total bill", total, { total = it }, "0", decimal = true)
        FText("How many people (including you)", people, { people = it.filter { c -> c.isDigit() }.take(2) }, "2")
        FText("Names, separated by commas (optional)", names, { names = it }, "Mika, Ben")
        FChips("Who paid?", listOf(opt("me", "I paid"), opt("friend", "A friend paid")), paidBy, { paidBy = it }, scroll = false)
        FChips("Account", s.accounts.filter { !it.ignored }.map { opt(it.id, it.name, it.icon) }, acct, { acct = it })
        BigButton("Split it") {
            val tot = total.toDoubleOrNull() ?: 0.0
            val n = maxOf(2, people.toIntOrNull() ?: 2)
            if (tot <= 0) { tst(ctx, "Enter the total bill"); return@BigButton }
            if (acct.isEmpty()) { tst(ctx, "Pick an account"); return@BigButton }
            val share = Math.round(tot / n * 100) / 100.0
            val who = names.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val food = s.cats.firstOrNull { it.type == "expense" && it.name.equals("Food", true) }?.id
                ?: s.cats.firstOrNull { it.type == "expense" && it.name.equals("Other", true) }?.id ?: s.cats.firstOrNull { it.type == "expense" }?.id ?: ""
            val now = System.currentTimeMillis()
            fun rec(a: Double) = buildJsonObject {
                put("id", store.newId()); put("ts", now); put("type", "expense"); put("acct", acct); put("to", ""); put("cat", food)
                put("amount", a); put("date", todayStr()); put("note", "Split bill")
            }
            fun debt(name: String, a: Double, theyOweMe: Boolean) = buildJsonObject {
                put("id", store.newId() + name.length + (0..99).random()); put("who", name); put("amount", a); put("theyOweMe", theyOweMe)
                put("date", todayStr()); put("due", ""); put("note", "Split bill"); put("paid", false)
            }
            if (paidBy == "me") {
                store.appendLists(mapOf("recs" to listOf(rec(tot)), "debts" to (0 until n - 1).map { debt(who.getOrNull(it) ?: "Friend ${it + 1}", share, true) }))
            } else {
                store.appendLists(mapOf("recs" to listOf(rec(share)), "debts" to listOf(debt(who.firstOrNull() ?: "Friend", share, false))))
            }
            tst(ctx, "Bill split. Each share is " + s.money(share))
            onClose()
        }
    }
}

/* ============================ PDF STATEMENT ============================ */

private var printView: android.webkit.WebView? = null

private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

private fun statementHtml(s: AppState, ym: String): String {
    val rs = s.recs.filter { it.type != "check" && it.date.startsWith(ym) }.sortedWith(compareBy({ it.date }, { it.ts }))
    val ex = s.sumMonth(ym, "expense"); val inc = s.sumMonth(ym, "income")
    val rows = rs.joinToString("") { r ->
        val a = s.acc(r.acct)
        val col = if (r.type == "income") "#12805C" else if (r.type == "expense") "#D1453B" else "#444"
        val sg = if (r.type == "income") "+" else if (r.type == "expense") "-" else ""
        val title = r.note.ifBlank { s.cat(r.cat)?.name ?: r.type }
        "<tr><td>${esc(fmtDate(r.date))}</td><td>${esc(title)}<br><small>${esc(s.cat(r.cat)?.name ?: "")}</small></td><td>${esc(a?.name ?: "")}</td><td style=\"text-align:right;color:$col;white-space:nowrap\">$sg${esc(s.money(r.amount))}</td></tr>"
    }.ifEmpty { "<tr><td colspan=\"4\">No records this month.</td></tr>" }
    return "<!doctype html><html><head><meta charset=\"utf-8\"><style>body{font:13px sans-serif;color:#111;margin:24px}h1{font-size:22px;margin:0}p{color:#555;margin:2px 0 14px}" +
        ".s{display:flex;gap:24px;margin:10px 0 18px}.s div{flex:1;border:1px solid #ddd;border-radius:10px;padding:10px}.s b{display:block;font-size:17px}" +
        "table{width:100%;border-collapse:collapse}th{text-align:left;font-size:11px;color:#777;border-bottom:2px solid #ccc;padding:6px 4px}td{border-bottom:1px solid #eee;padding:7px 4px;vertical-align:top}small{color:#777}</style></head><body>" +
        "<h1>MyWallet statement</h1><p>${esc(monthLabel(ym))}</p><div class=\"s\"><div>Money in<b style=\"color:#12805C\">${esc(s.money(inc))}</b></div><div>Spent<b style=\"color:#D1453B\">${esc(s.money(ex))}</b></div><div>Net<b>${esc(s.money(inc - ex, signed = true))}</b></div></div>" +
        "<table><thead><tr><th>Date</th><th>Details</th><th>Account</th><th style=\"text-align:right\">Amount</th></tr></thead><tbody>$rows</tbody></table></body></html>"
}

/** opens Android's print screen for the month, where you choose "Save as PDF" */
fun printStatement(ctx: Context, s: AppState, ym: String) {
    try {
        val host: Context = findActivity(ctx) ?: ctx
        val wv = android.webkit.WebView(host)
        printView = wv
        wv.settings.javaScriptEnabled = false
        wv.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(v: android.webkit.WebView, url: String?) {
                val pm = host.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                pm.print("MyWallet-$ym", v.createPrintDocumentAdapter("MyWallet-$ym"), android.print.PrintAttributes.Builder().build())
            }
        }
        wv.loadDataWithBaseURL(null, statementHtml(s, ym), "text/html", "utf-8", null)
    } catch (e: Exception) { tst(ctx, "Printing is not available here") }
}
