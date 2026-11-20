package com.example.mywallet.data

import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/* ---------- dates (no java.time, so it works on every Android version) ---------- */
fun daysFromCivil(y0: Long, m: Long, d: Long): Long {
    val y = if (m <= 2) y0 - 1 else y0
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val mp = (m + 9) % 12
    val doy = (153 * mp + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

fun civilFromDays(z0: Long): Triple<Long, Long, Long> {
    val z = z0 + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    return Triple(if (m <= 2) y + 1 else y, m, d)
}

/** day number from a (year, 0-based month, day), months may overflow like JavaScript's Date.UTC */
fun dnOf(y: Long, m0: Long, d: Long): Long =
    daysFromCivil(y + Math.floorDiv(m0, 12L), Math.floorMod(m0, 12L) + 1, d)

fun daysInMonth(y: Long, m0: Long): Long = dnOf(y, m0 + 1, 1) - dnOf(y, m0, 1)

fun dn(s: String): Long {
    val p = s.take(10).split("-")
    if (p.size < 3) return todayDn()
    val y = p[0].toLongOrNull() ?: return todayDn()
    val m = p[1].toLongOrNull() ?: return todayDn()
    val d = p[2].toLongOrNull() ?: return todayDn()
    return daysFromCivil(y, m, d)
}

fun fromDn(n: Long): String {
    val (y, m, d) = civilFromDays(n)
    return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
}

/** developer mode only: pretend today is this many days away (not saved, resets when the app restarts) */
var DAY_SHIFT = 0

fun todayStr(): String {
    val c = Calendar.getInstance()
    if (DAY_SHIFT != 0) c.add(Calendar.DAY_OF_MONTH, DAY_SHIFT)
    return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

fun todayDn(): Long = dn(todayStr())

/* ---------- money text ---------- */
fun AppState.moneyParts(n: Double, signed: Boolean = false): Pair<String, String> {
    val dec = settings.dec.coerceIn(0, 4)
    val scaled = Math.round(abs(n) * Math.pow(10.0, dec.toDouble()))
    val whole = scaled / Math.pow(10.0, dec.toDouble()).toLong()
    val frac = scaled - whole * Math.pow(10.0, dec.toDouble()).toLong()
    val w = String.format(Locale.US, "%,d", whole)
    val neg = n < 0 && scaled > 0
    val sign = if (neg) "−" else if (signed && n > 0) "+" else ""
    val head = if (settings.pos == "end") "$sign$w" else "$sign${settings.cur}$w"
    var tail = if (dec > 0) "." + frac.toString().padStart(dec, '0') else ""
    if (settings.pos == "end") tail += " ${settings.cur}"
    return head to tail
}

fun AppState.money(n: Double, signed: Boolean = false): String = moneyParts(n, signed).let { it.first + it.second }

/** which Today-page sections are shown (all on unless switched off in Display) */
fun AppState.homeOn(k: String): Boolean = (settings.home?.get(k) as? kotlinx.serialization.json.JsonPrimitive)?.content != "false"

/* ---------- balances ---------- */
fun AppState.acc(id: String) = accounts.firstOrNull { it.id == id }
fun AppState.cat(id: String) = cats.firstOrNull { it.id == id }
fun AppState.rateOf(id: String): Double = acc(id)?.rate?.takeIf { it > 0 } ?: 1.0

class Series(val start: Long, val vals: DoubleArray, val initial: Double) {
    private val last: Double get() = if (vals.isEmpty()) initial else vals.last()
    fun at(d: Long): Double = if (d < start) 0.0 else if (d - start >= vals.size) last else vals[(d - start).toInt()]
}

fun AppState.series(a: Account, endDn: Long): Series {
    var start = if (a.openDate.isNotEmpty()) dn(a.openDate) else todayDn()
    class E(val d: Long, val delta: Double, val check: Boolean, val ts: Double)
    val es = ArrayList<E>()
    for (r in recs) {
        if (r.type == "transfer") {
            if (r.acct == a.id) es.add(E(dn(r.date), -r.amount, false, r.ts))
            if (r.to == a.id) es.add(E(dn(r.date), r.toAmount ?: r.amount, false, r.ts))
        } else if (r.acct == a.id) {
            if (r.type == "check") es.add(E(dn(r.date), r.amount, true, r.ts))
            else es.add(E(dn(r.date), if (r.type == "income") r.amount else -r.amount, false, r.ts))
        }
    }
    for (e in es) if (e.d < start) start = e.d
    val by = HashMap<Long, MutableList<E>>()
    for (e in es) by.getOrPut(e.d) { ArrayList() }.add(e)
    var bal = a.initial
    val out = ArrayList<Double>()
    var d = start
    while (d <= endDn) {
        var chk: Double? = null
        by[d]?.sortedBy { it.ts }?.forEach { e -> if (e.check) chk = e.delta else bal += e.delta }
        if (chk != null) bal = chk!!
        out.add(bal)
        d++
    }
    return Series(start, out.toDoubleArray(), a.initial)
}

fun AppState.balanceNow(a: Account): Double = memo().bal.getOrPut(a.id) { series(a, todayDn()).at(todayDn()) }
fun AppState.balBase(a: Account): Double = balanceNow(a) * (if (a.rate > 0) a.rate else 1.0)

/* ---------- bank minimum-balance rules ---------- */
class Period(val start: Long, val end: Long)

fun bankPeriod(cfg: BankCfg, t: Long): Period {
    val (y, m, d) = civilFromDays(t)
    val m0 = m - 1
    if (cfg.period == "cycle") {
        val s = cfg.startDay.coerceIn(1, 28).toLong()
        var sy = y; var sm = m0
        if (d < s) { sm--; if (sm < 0) { sm = 11; sy-- } }
        return Period(dnOf(sy, sm, s), dnOf(sy, sm + 1, s) - 1)
    }
    return Period(dnOf(y, m0, 1), dnOf(y, m0 + 1, 1) - 1)
}

class BankInfo(
    val cur: Double, val min: Double, val need: Double, val safe: Double, val locked: Boolean,
    val verdict: String, val daysLeft: Long, val period: Period, val avgNow: Double, val required: Double,
)

fun AppState.bankCalc(a: Account, tOv: Long? = null): BankInfo? =
    if (tOv != null) bankCalcRaw(a, tOv) else memo().bank.getOrPut(a.id) { bankCalcRaw(a, null) }

private fun AppState.bankCalcRaw(a: Account, tOv: Long?): BankInfo? {
    val cfg = a.bank ?: return null
    val t = tOv ?: todayDn()
    val p = bankPeriod(cfg, t)
    val n = p.end - p.start + 1
    val ser = series(a, t)
    val cur = ser.at(t)
    val mn = cfg.min
    var pts = 0.0
    var breach: Long? = null
    var d = p.start
    while (d < t) {
        val b = ser.at(d)
        pts += max(0.0, b)
        if (breach == null && d >= ser.start && b < mn) breach = d
        d++
    }
    val r = p.end - t + 1
    val elapsed = t - p.start + 1
    val avgNow = (pts + max(0.0, cur)) / elapsed
    var l = mn
    var locked = false
    if (cfg.rule == "avg") {
        l = max(0.0, ceil((mn * n - pts) / r - 1e-9))
        locked = pts >= mn * n
    }
    val safe = cur - l
    val verdict = when (cfg.rule) {
        "avg" -> if (locked) "locked" else if (safe >= 0) "ok" else "add"
        "daily" -> if (breach != null) "missed" else if (safe >= 0) "ok" else "add"
        else -> if (safe >= 0) "ok" else "add"
    }
    return BankInfo(cur, mn, max(0.0, -safe), safe, locked, verdict, r, p, avgNow, l)
}

/* ---------- payday, allowance, safe today ---------- */
class PayDates(val prev: Long, val next: Long)

fun AppState.payCfg(): PayCfg {
    val p = settings.pay
    if (p != null && p.mode.isNotEmpty()) return p
    val d = settings.payday.toInt()
    return if (d > 0) PayCfg("m1", d, 20, 5) else PayCfg("off", 1, 15, 5)
}

private fun paydayOf(y: Long, m0: Long, day: Int): Long = dnOf(y, m0, min(day.toLong(), daysInMonth(y, m0)))

fun AppState.payDates(t: Long): PayDates? {
    val p = payCfg()
    if (p.mode == "off") return null
    if (p.mode == "w") {
        val dow = Math.floorMod(t + 4, 7L)
        val prev = t - Math.floorMod(dow - p.wd + 7, 7L)
        return PayDates(prev, prev + 7)
    }
    val days = if (p.mode == "m2") listOf(p.d1, p.d2) else listOf(p.d1)
    val (y, m, _) = civilFromDays(t)
    val c = ArrayList<Long>()
    for (k in -1..1) for (d in days) c.add(paydayOf(y, m - 1 + k, d))
    val prevs = c.filter { it <= t }
    val nexts = c.filter { it > t }
    if (prevs.isEmpty() || nexts.isEmpty()) return null
    return PayDates(prevs.max(), nexts.min())
}

fun AppState.spendAccts() = accounts.filter { a -> a.kind != "bank" && !a.ignored && goals.none { it.acct == a.id } }

class Allowance(val days: Long, val per: Double, val spent: Double, val left: Double)

fun AppState.allowanceInfo(): Allowance? {
    val t = todayDn()
    val pd = payDates(t) ?: return null
    val days = max(1L, pd.next - t)
    val ids = spendAccts().map { it.id }
    val pool = max(0.0, spendAccts().sumOf { balBase(it) }) * (1 - min(90.0, settings.holdPct) / 100)
    val today = todayStr()
    val spent = recs.filter { it.type == "expense" && it.date == today && it.acct in ids }.sumOf { it.amount * rateOf(it.acct) }
    val per = (pool + spent) / days
    return Allowance(days, per, spent, per - spent)
}

fun AppState.spentIn(ym: String, catId: String): Double =
    recs.filter { it.type == "expense" && it.cat == catId && it.date.startsWith(ym) }.sumOf { it.amount * rateOf(it.acct) }

class Safe(val amount: Double, val reason: String)

fun AppState.safeToday(): Safe {
    val c = ArrayList<Pair<Double, String>>()
    allowanceInfo()?.let { c.add(max(0.0, it.left) to "your daily allowance") }
    val ym = todayStr().take(7)
    val b = budgets[ym] ?: emptyMap()
    val ids = b.keys.filter { cat(it) != null }
    if (ids.isNotEmpty()) {
        val left = ids.sumOf { b[it] ?: 0.0 } - ids.sumOf { spentIn(ym, it) }
        val (y, m, d) = civilFromDays(todayDn())
        val dim = daysInMonth(y, m - 1)
        c.add(max(0.0, left / (dim - d + 1)) to "your budgets")
    }
    c.add(max(0.0, spendAccts().sumOf { balBase(it) }) to "cash on hand")
    return c.minByOrNull { it.first }!!.let { Safe(it.first, it.second) }
}

/* ---------- bills, debts ---------- */
/** how much of a debt has been paid back so far (a debt marked fully paid counts as all of it) */
fun Debt.paidSoFar(): Double = if (paid && pays.isEmpty()) amount else pays.sumOf { it.amount }.coerceAtMost(amount)

/** how much is still owed */
fun Debt.left(): Double = if (paid) 0.0 else (amount - paidSoFar()).coerceAtLeast(0.0)

class BillDue(val dn: Long, val key: String)

fun billDue(b: Bill, t: Long): BillDue {
    val (y0, m, _) = civilFromDays(t)
    var y = y0; var m0 = m - 1
    for (i in 0 until 24) {
        val key = String.format(Locale.US, "%04d-%02d", y, m0 + 1)
        if (key !in b.paid) return BillDue(paydayOf(y, m0, b.day), key)
        m0++; if (m0 > 11) { m0 = 0; y++ }
    }
    return BillDue(t, "")
}

fun dueText(d: Long): String {
    val k = d - todayDn()
    return when {
        k < 0 -> "${-k} day${if (k == -1L) "" else "s"} overdue"
        k == 0L -> "Due today"
        k == 1L -> "Due tomorrow"
        else -> "In $k days"
    }
}

/* ---------- streak and level ---------- */
fun AppState.loggedDays(): Set<Long> = memo().logged

private fun AppState.loggedDaysRaw(): Set<Long> {
    val t = todayDn()
    return recs.filter { it.type != "check" }.map { dn(it.date) }.filter { it <= t }.toSet()
}

/** one free "freeze" day per week, same rule as the web app */
fun AppState.streak(): Int = memo().streak

private fun AppState.streakRaw(): Int {
    val ds = loggedDays()
    val t = todayDn()
    var d = if (ds.contains(t)) t else t - 1
    var c = 0
    var guard = 0
    val used = HashSet<Long>()
    fun wk(x: Long) = floor((x + 3) / 7.0).toLong()
    while (guard++ < 4000) {
        if (ds.contains(d)) { c++; d--; continue }
        if (ds.contains(d - 1) && !used.contains(wk(d))) { used.add(wk(d)); d--; continue }
        break
    }
    return c
}

/** what to warn about on the Today page (null = nothing) */
class StreakWarn(val streak: Int, val missedYesterday: Boolean, val freeLeft: Boolean)

fun AppState.streakWarn(): StreakWarn? {
    val ds = loggedDays()
    val t = todayDn()
    if (ds.contains(t)) return null
    val st = streak()
    if (st <= 0) return null
    fun wk(x: Long) = floor((x + 3) / 7.0).toLong()
    var d = t - 1
    var guard = 0
    val used = HashSet<Long>()
    while (guard++ < 4000) {
        if (ds.contains(d)) { d--; continue }
        if (ds.contains(d - 1) && !used.contains(wk(d))) { used.add(wk(d)); d--; continue }
        break
    }
    val missedY = !ds.contains(t - 1)
    val freeLeft = !used.contains(wk(t))
    if (!missedY && freeLeft) return null
    return StreakWarn(st, missedY, freeLeft)
}

/** longest streak ever, with the same one-free-day-a-week rule */
fun AppState.bestStreak(): Int = memo().best

private fun AppState.bestStreakRaw(): Int {
    val ds = loggedDays()
    if (ds.isEmpty()) return 0
    val t = todayDn()
    var best = 0
    var run = 0
    var prevLogged = false
    val used = HashSet<Long>()
    fun wk(x: Long) = floor((x + 3) / 7.0).toLong()
    var d = ds.min()
    while (d <= t) {
        if (ds.contains(d)) { run++; prevLogged = true }
        else if (prevLogged && !used.contains(wk(d)) && ds.contains(d + 1)) { used.add(wk(d)); prevLogged = false }
        else { run = 0; prevLogged = false }
        if (run > best) best = run
        d++
    }
    return best
}

val FLAME_DAYS = intArrayOf(0, 1, 3, 7, 14, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330, 365)
fun flameTier(n: Int): Int { var t = 0; FLAME_DAYS.forEachIndexed { i, s -> if (i > 0 && n >= s) t = i }; return t }

/* ---------- levels: endless, with 10 titles ----------
   Each level needs 2 more records than the one before (8, 10, 12, ...), so the gaps stay small and there is
   always a next level. Titles only change at the levels below. */
val LEVEL_TITLES = listOf(
    1 to "Beginner", 3 to "Tracker", 6 to "Saver", 10 to "Budgeter", 15 to "Planner",
    22 to "Strategist", 30 to "Wealth Builder", 40 to "Money Master", 55 to "Tycoon", 75 to "Legend",
)

/** records needed to reach level l (level 1 = 0 records) */
fun xpForLevel(l: Int): Int = (l - 1) * (l + 6)

fun levelFor(xp: Int): Int {
    var l = ((-5.0 + Math.sqrt(25.0 + 4.0 * (xp + 6))) / 2.0).toInt().coerceAtLeast(1)
    while (xpForLevel(l + 1) <= xp) l++
    while (l > 1 && xpForLevel(l) > xp) l--
    return l
}

fun titleFor(level: Int): String = LEVEL_TITLES.last { it.first <= level }.second

class LevelInfo(val level: Int, val title: String, val xp: Int, val from: Int, val to: Int, val nextTitle: String?, val nextTitleLevel: Int?) {
    val progress: Float get() = ((xp - from).toFloat() / (to - from)).coerceIn(0f, 1f)
    val left: Int get() = to - xp
}

fun levelInfoFor(xp: Int): LevelInfo {
    val l = levelFor(xp)
    val nt = LEVEL_TITLES.firstOrNull { it.first > l }
    return LevelInfo(l, titleFor(l), xp, xpForLevel(l), xpForLevel(l + 1), nt?.second, nt?.first)
}

fun AppState.xp(): Int = recs.count { it.type != "check" }
fun AppState.levelInfo(): LevelInfo = levelInfoFor(xp())

/* ---------- badges ---------- */
class Badge(val id: String, val name: String, val desc: String, val icon: String)

val BADGES = listOf(
    Badge("first", "First step", "Log your first record", "sparkles"),
    Badge("rec25", "Getting going", "Log 25 records", "edit"),
    Badge("rec100", "Record keeper", "Log 100 records", "book"),
    Badge("streak3", "On a roll", "Log 3 days in a row", "flame"),
    Badge("streak7", "Week warrior", "Log 7 days in a row", "zap"),
    Badge("streak30", "Unstoppable", "Log 30 days in a row", "heart"),
    Badge("streak100", "Century", "Log 100 days in a row", "gift"),
    Badge("streak365", "Full year", "Log 365 days in a row", "trophy"),
    Badge("save1k", "First 1,000", "Hold 1,000 or more in total", "coin"),
    Badge("goal", "Goal getter", "Reach a savings goal", "target"),
    Badge("budget", "Under budget", "End a month with every budget in the green", "lock"),
    Badge("debt", "Square up", "Settle a debt", "swap"),
    Badge("planner", "Planner", "Set your payday", "calendar"),
    Badge("bank", "Bank safe", "Keep a bank account on track", "landmark"),
)

fun AppState.goalSavedAmt(g: Goal): Double {
    val a = g.acct?.takeIf { it.isNotEmpty() }?.let { acc(it) }
    return if (a != null) max(0.0, balBase(a)) else g.contribs.sumOf { it.amount }
}

fun AppState.earnedBadges(): Set<String> {
    val e = HashSet<String>()
    val n = xp(); val ls = bestStreak()
    if (n >= 1) e.add("first"); if (n >= 25) e.add("rec25"); if (n >= 100) e.add("rec100")
    if (ls >= 3) e.add("streak3"); if (ls >= 7) e.add("streak7"); if (ls >= 30) e.add("streak30")
    if (ls >= 100) e.add("streak100"); if (ls >= 365) e.add("streak365")
    if (n >= 1 && accounts.sumOf { balBase(it) } >= 1000) e.add("save1k")
    if (goals.any { it.target > 0 && goalSavedAmt(it) >= it.target }) e.add("goal")
    val (y, m, _) = civilFromDays(todayDn())
    val (py, pm, _) = civilFromDays(dnOf(y, m - 2, 1))
    val key = String.format(Locale.US, "%04d-%02d", py, pm)
    val pb = budgets[key] ?: emptyMap()
    val ids = pb.keys.filter { cat(it) != null }
    if (ids.isNotEmpty() && ids.all { spentIn(key, it) <= (pb[it] ?: 0.0) }) e.add("budget")
    if (debts.any { it.paid }) e.add("debt")
    if (payCfg().mode != "off") e.add("planner")
    if (accounts.any { a -> a.kind == "bank" && !a.ignored && a.bank != null && (bankCalc(a)?.verdict == "ok" || bankCalc(a)?.verdict == "locked") }) e.add("bank")
    return e
}

/* ---------- smart category guess (note or #tag -> category) ---------- */
private val KEYWORDS = mapOf(
    "transport" to listOf("grab", "jeep", "tricycle", "trike", "bus", "mrt", "lrt", "fare", "angkas", "taxi", "gas", "fuel", "joyride", "van"),
    "food" to listOf("jollibee", "mcdo", "mcdonald", "kfc", "lunch", "dinner", "breakfast", "snack", "milk tea", "milktea", "coffee", "food", "rice", "ulam", "canteen", "chowking", "mang inasal", "starbucks", "pizza", "burger", "bread"),
    "phone & data" to listOf("load", "data", "promo", "wifi", "phone", "sim", "gomo", "giga"),
    "load & data" to listOf("load", "data", "promo", "wifi"),
    "bills" to listOf("boarding", "rent", "water bill", "electric", "kuryente", "internet", "bill"),
    "school" to listOf("notebook", "pen", "book", "print", "xerox", "school", "project", "uniform", "tuition", "exam", "ballpen"),
    "shopping" to listOf("shopee", "lazada", "clothes", "shirt", "shoes", "mall", "bag"),
    "health" to listOf("medicine", "clinic", "vitamins", "doctor", "pharmacy", "mercury"),
    "fun" to listOf("netflix", "spotify", "game", "movie", "cinema", "robux", "valorant", "mlbb", "diamonds", "concert"),
)
private val IN_KEYWORDS = mapOf(
    "allowance" to listOf("allowance", "baon", "pocket money", "deposit"),
    "salary" to listOf("salary", "sweldo", "wage", "part time", "freelance"),
    "gift" to listOf("gift", "regalo", "aguinaldo", "lola", "lolo", "birthday"),
    "bank interest" to listOf("interest"),
)

/** picks a category id for a note: a #tag named like a category, a note you used before, words you used before, then common words */
fun AppState.guessCat(note: String, type: String): String? {
    val n = note.trim().lowercase()
    if (n.length < 2) return null
    val ok = cats.filter { it.type == type && !it.ignored }
    val okIds = ok.map { it.id }.toSet()
    // 1. a #tag that matches a category name, like #food or #phone
    tagsOf(n).forEach { tg ->
        val w = tg.removePrefix("#").replace('_', ' ').replace('-', ' ')
        ok.firstOrNull { it.name.lowercase() == w }?.let { return it.id }
        ok.firstOrNull { it.name.lowercase().startsWith(w) && w.length >= 3 }?.let { return it.id }
    }
    // 2. the same note used before
    val same = HashMap<String, Int>()
    recs.forEach { if (it.note.trim().lowercase() == n && it.cat in okIds) same[it.cat] = (same[it.cat] ?: 0) + 1 }
    same.maxByOrNull { it.value }?.let { return it.key }
    // 3. words and tags used before in the same category
    val words = n.split(Regex("[^\\p{L}\\p{N}#]+")).filter { it.length >= 3 }
    if (words.isNotEmpty()) {
        val tally = HashMap<String, HashMap<String, Int>>()
        recs.takeLast(800).forEach { r ->
            if (r.type != type || r.cat !in okIds) return@forEach
            val rw = r.note.lowercase().split(Regex("[^\\p{L}\\p{N}#]+"))
            words.forEach { w -> if (w in rw) { val m = tally.getOrPut(w) { HashMap() }; m[r.cat] = (m[r.cat] ?: 0) + 1 } }
        }
        var best: String? = null; var bs = 0
        tally.values.forEach { m ->
            val tot = m.values.sum(); val top = m.maxByOrNull { it.value }!!
            if (tot >= 2 && top.value.toDouble() / tot >= 0.7 && top.value > bs) { best = top.key; bs = top.value }
        }
        best?.let { return it }
    }
    // 4. common words
    val table = if (type == "expense") KEYWORDS else IN_KEYWORDS
    for ((name, ws) in table) if (ws.any { n.contains(it) }) ok.firstOrNull { it.name.lowercase() == name }?.let { return it.id }
    return null
}

/* ---------- tags (#word in a note) ---------- */
private val TAG_RX = Regex("#[\\p{L}\\p{N}_-]+")
fun tagsOf(note: String): List<String> = TAG_RX.findAll(note).map { it.value.lowercase() }.toList()

/* ---------- spending alerts, shown right after you save an expense ---------- */
fun AppState.alertsFor(r: Rec): List<String> {
    val out = ArrayList<String>()
    if (!settings.alerts || r.type != "expense") return out
    val mon = r.date.take(7)
    val rt = r.amount * rateOf(r.acct)
    if (r.cat.isNotEmpty()) {
        val lim = budgets[mon]?.get(r.cat) ?: 0.0
        if (lim > 0) {
            val sp = spentIn(mon, r.cat); val before = sp - rt; val nm = cat(r.cat)?.name ?: "this"
            if (sp > lim && before <= lim) out.add("Over your $nm budget.")
            else if (sp >= lim * 0.8 && before < lim * 0.8) out.add("80% of your $nm budget used.")
        }
    }
    if (r.date == todayStr() && recs.count { it.type == "expense" } >= 10) {
        val t = todayDn()
        val day = recs.filter { it.type == "expense" && it.date == r.date }.sumOf { it.amount * rateOf(it.acct) }
        val past = recs.filter { it.type == "expense" && dn(it.date) >= t - 30 && dn(it.date) < t }.sumOf { it.amount * rateOf(it.acct) } / 30.0
        if (past > 0 && day >= 3 * past && day - rt < 3 * past) out.add("Much more than your usual day.")
    }
    return out
}

/* ---------- remembered results, so the same maths is not repeated on every redraw ---------- */
class Memo(val s: AppState) {
    val logged: Set<Long> by lazy { s.loggedDaysRaw() }
    val streak: Int by lazy { s.streakRaw() }
    val best: Int by lazy { s.bestStreakRaw() }
    val bal = HashMap<String, Double>()
    val bank = HashMap<String, BankInfo?>()
}

@Volatile private var memoCache: Pair<AppState, Memo>? = null

/** one remembered set per version of the data (a new version is made whenever anything changes) */
fun AppState.memo(): Memo {
    val c = memoCache
    if (c != null && c.first === this) return c.second
    val m = Memo(this)
    memoCache = this to m
    return m
}
