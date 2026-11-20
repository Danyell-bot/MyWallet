package com.example.mywallet.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.data.AppState
import com.example.mywallet.data.Rec
import com.example.mywallet.data.Store
import com.example.mywallet.data.earnedBadges
import kotlinx.serialization.json.JsonPrimitive
import com.example.mywallet.data.Account
import com.example.mywallet.data.acc
import com.example.mywallet.data.balBase
import com.example.mywallet.data.balanceNow
import com.example.mywallet.data.bankCalc
import com.example.mywallet.data.billDue
import com.example.mywallet.data.cat
import com.example.mywallet.data.dn
import com.example.mywallet.data.dueText
import com.example.mywallet.data.flameTier
import com.example.mywallet.data.homeOn
import com.example.mywallet.data.left
import com.example.mywallet.data.paidSoFar
import com.example.mywallet.data.loggedDays
import com.example.mywallet.data.money
import com.example.mywallet.data.moneyParts
import com.example.mywallet.data.rateOf
import com.example.mywallet.data.safeToday
import com.example.mywallet.data.spentIn
import com.example.mywallet.data.streak
import com.example.mywallet.data.streakWarn
import com.example.mywallet.data.allowanceInfo
import com.example.mywallet.data.todayDn
import com.example.mywallet.data.todayStr
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

private fun soon(ctx: android.content.Context, what: String) =
    Toast.makeText(ctx, "$what comes in the next stage", Toast.LENGTH_SHORT).show()

@Composable
fun HomeScreen(s: AppState, store: Store) {
    val ctx = LocalContext.current
    LaunchedEffect(s.settings.palette, s.settings.pal) { MwTheme.apply(s.settings.palette, s.settings.pal) }
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val page = pager.currentPage + pager.currentPageOffsetFraction
    var hide by remember { mutableStateOf(s.settings.hide) }
    val haze = rememberHazeState()
    var sheetOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Rec?>(null) }
    var nameDismissed by remember { mutableStateOf(false) }
    val stack = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    val screen = stack.lastOrNull()
    fun go(k: String) { if (stack.lastOrNull() != k) stack.add(k) }
    var debtFocus by remember { mutableStateOf<String?>(null) }
    fun openRoute(r: String) { if (r.startsWith("debts#")) { debtFocus = r.substringAfter('#'); go("debts") } else go(r) }
    fun back() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    var receiptsOnly by remember { mutableStateOf(false) }
    var menuKey by remember { mutableStateOf(0) }
    val menuScroll = remember(menuKey) { androidx.compose.foundation.ScrollState(0) }
    var acctEdit by remember { mutableStateOf<String?>(null) }
    var acctDel by remember { mutableStateOf<String?>(null) }
    var addAcct by remember { mutableStateOf(false) }
    var acctSel by remember { mutableStateOf<String?>(null) }
    var celebrate by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var snap by remember { mutableStateOf<Triple<Boolean, Int, Int>?>(null) }
    var warnPop by remember { mutableStateOf<com.example.mywallet.data.StreakWarn?>(null) }
    var endedPop by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    fun playPop(kind: String, n: Int) {
        when (kind) {
            "warnFree" -> warnPop = com.example.mywallet.data.StreakWarn(n, true, false)
            "warnNone" -> warnPop = com.example.mywallet.data.StreakWarn(n, false, false)
            "ended" -> endedPop = n to (0..3).random()
            "ended0" -> endedPop = n to 0
            "ended1" -> endedPop = n to 1
            "ended2" -> endedPop = n to 2
            "ended3" -> endedPop = n to 3
        }
    }
    // a streak that was alive the last time you opened the app and is now gone: the "streak ended" moment, once
    LaunchedEffect(Unit) {
        val p = ctx.getSharedPreferences("mw_prefs", 0)
        val seen = p.getInt("streakSeen", 0)
        if (s.streak() == 0 && seen >= 2) {
            p.edit().putInt("streakSeen", 0).apply()
            kotlinx.coroutines.delay(900)
            endedPop = seen to (0..3).random()
        }
    }
    LaunchedEffect(s) {
        val st = s.streak()
        val p = ctx.getSharedPreferences("mw_prefs", 0)
        if (st > 0 && p.getInt("streakSeen", 0) != st) p.edit().putInt("streakSeen", st).apply()
    }
    // once a day, when you open the app and a day was missed (the weekly free day was used): show the warning moment
    LaunchedEffect(Unit) {
        val w = s.streakWarn()
        val p = ctx.getSharedPreferences("mw_prefs", 0)
        val today = todayDn()
        if (w != null && p.getLong("warnDay", -1L) != today) {
            p.edit().putLong("warnDay", today).apply()
            kotlinx.coroutines.delay(900)
            warnPop = w
        }
    }
    // first record of a new day brings the streak back to life: show the "new day" moment
    LaunchedEffect(s) {
        val lit = s.loggedDays().contains(todayDn()); val st = s.streak(); val n = s.recs.size
        val p = snap
        if (p != null && !p.first && lit && st > 0 && n > p.third && n - p.third <= 3) {
            kotlinx.coroutines.delay(550)
            celebrate = maxOf(0, st - 1) to st
        }
        snap = Triple(lit, st, n)
    }
    // badges: unlock new ones with confetti (the first run only records what you already earned)
    LaunchedEffect(s) {
        val e = s.earnedBadges()
        if (!s.badgeInit) {
            store.setTops(mapOf("unlocked" to kotlinx.serialization.json.JsonArray(e.map { JsonPrimitive(it) }), "badgeInit" to JsonPrimitive(true)))
        } else {
            val fresh = e.filter { !s.unlocked.contains(it) }
            if (fresh.isNotEmpty()) {
                store.setTops(mapOf("unlocked" to kotlinx.serialization.json.JsonArray((s.unlocked + fresh).map { JsonPrimitive(it) })))
                val b = com.example.mywallet.data.BADGES.firstOrNull { it.id == fresh.first() }
                Fx.fire("Badge unlocked: " + (b?.name ?: ""))
            }
        }
    }
    var bankId by remember { mutableStateOf<String?>(null) }
    var preset by remember { mutableStateOf<Pair<String, String>?>(null) }
    LaunchedEffect(com.example.mywallet.LaunchBus.add) {
        if (com.example.mywallet.LaunchBus.add != null) { editing = null; preset = null; sheetOpen = true; com.example.mywallet.LaunchBus.add = null }
    }

    Box(Modifier.fillMaxSize().background(MW.Bg)) {
        CompositionLocalProvider(LocalScenePaused provides (screen != null)) { SceneBackground(page, Modifier.hazeSource(haze)) }
        CompositionLocalProvider(LocalHaze provides haze, LocalScenePaused provides (screen != null)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 22.dp)) { Header(s, { menuKey += 1; go("menu") }, { go("streak") }) }
            Spacer(Modifier.height(20.dp))
            Box(Modifier.padding(horizontal = 22.dp)) { TabPill(page) { i -> scope.launch { pager.animateScrollToPage(i) } } }
            Spacer(Modifier.height(6.dp))
            HorizontalPager(pager, Modifier.fillMaxSize(), pageSpacing = 0.dp) { p ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 150.dp)) {
                    when (p) {
                        0 -> TodayPage(s, hide, { hide = !hide }, { receiptsOnly = false; go("search") }, onBackup = { go("backup") }, onOpen = { openRoute(it) }, onRange = { rg -> com.example.mywallet.LaunchBus.range = rg; receiptsOnly = false; go("search") }, store = store) { r -> editing = r; sheetOpen = true }
                        1 -> MoneyPage(s, hide, onOpen = { id -> val a = s.acc(id); if (a?.kind == "bank") { bankId = id; go("bank") } }, onHold = { id -> acctSel = id },
                            onEdit = { id -> acctEdit = id }, onDelete = { id -> acctDel = id }, onAdd = { addAcct = true })
                        else -> PlanPage(s, hide) { openRoute(it) }
                    }
                }
            }
        }
        // fade under the dock
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(190.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, MW.Bg.copy(alpha = 0.55f), MW.Bg.copy(alpha = 0.9f))))
        )
        Dock(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp), ctx, { receiptsOnly = false; go("search") }) { editing = null; sheetOpen = true }
        }
        AnimatedContent(
            targetState = screen to stack.size, modifier = Modifier.fillMaxSize(), label = "screens",
            transitionSpec = {
                val push = targetState.second >= initialState.second
                val ct = if (push) (slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { it } togetherWith (slideOutHorizontally(tween(360, easing = FastOutSlowInEasing)) { -it / 4 } + fadeOut(tween(300))))
                else ((slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { -it / 4 } + fadeIn(tween(300))) togetherWith slideOutHorizontally(tween(360, easing = FastOutSlowInEasing)) { it }).apply { targetContentZIndex = -1f }
                ct.using(SizeTransform(clip = false) { _, _ -> snap() })
            },
        ) { st ->
            val sc = st.first
            if (sc == null) Spacer(Modifier.fillMaxSize())
            if (sc == "menu") MenuScreen(s, store, menuScroll, onOpen = { k -> if (k == "search-receipts") { receiptsOnly = true; go("search") } else go(k) }, onClose = { back() })
            if (sc == "search") SearchScreen(s, store, receiptsOnly, onEdit = { r -> editing = r; sheetOpen = true }, onClose = { back() })
            when (sc) {
                "bills" -> BillsScreen(s, store) { back() }
                "debts" -> DebtsScreen(s, store, debtFocus) { debtFocus = null; back() }
                "goals" -> GoalsScreen(s, store) { back() }
                "cats" -> CategoriesScreen(s, store) { back() }
                "quick" -> QuickAddScreen(s, store) { back() }
                "analysis" -> AnalysisScreen(s) { back() }
                "recap" -> RecapScreen(s) { back() }
                "budgets" -> BudgetsScreen(s, store) { back() }
                "receipts" -> ReceiptsScreen(s, store, onEdit = { r -> editing = r; sheetOpen = true }) { back() }
                "streak" -> StreakScreen(s) { back() }
                "recurring" -> RecurringScreen(s, store) { back() }
                "lock" -> AppLockScreen(store) { back() }
                "display" -> DisplayScreen(s, store) { back() }
                "help" -> HelpScreen { back() }
                "badges" -> BadgesScreen(s) { back() }
                "tags" -> TagsScreen(s, onSearch = { q -> com.example.mywallet.LaunchBus.query = q; receiptsOnly = false; go("search") }) { back() }
                "challenge" -> ChallengeScreen(s, store) { back() }
                "payday" -> PaydayScreen(s, store) { back() }
                "year" -> YearScreen(s) { back() }
                "backup" -> BackupScreen(s, store) { back() }
                "dev" -> DevScreen(s, store, { back() }, { k -> go(k) }, { a, b -> celebrate = a to b }, { k, n -> playPop(k, n) }) { ctx.getSharedPreferences("mw_prefs", 0).edit().putBoolean("dev", false).apply(); back() }
                "reminders" -> RemindersScreen(store) { back() }
                "bank" -> BankScreen(s, store, bankId, onRecord = { t, a -> editing = null; preset = t to a; sheetOpen = true }, onEdit = { r -> editing = r; sheetOpen = true }, onClose = { back() })
            }
        }
        if (addAcct) AccountForm(store, "cash") { addAcct = false }
        s.accounts.firstOrNull { it.id == acctEdit }?.let { a -> AccountForm(store, a.kind, a) { acctEdit = null } }
        s.accounts.firstOrNull { it.id == acctDel }?.let { a ->
            val n = s.recs.count { it.acct == a.id || it.to == a.id }
            ConfirmDialog("Delete " + a.name + "?", "This also deletes its $n record" + (if (n == 1) "" else "s") + ". Ignore it instead to keep your history.", "Delete", { store.deleteAccount(a.id); acctDel = null }, { acctDel = null })
        }
        s.accounts.firstOrNull { it.id == acctSel }?.let { a -> AccountActions(s, store, a, onBank = { bankId = a.id; go("bank") }, onClose = { acctSel = null }) }
        if (sheetOpen) CompositionLocalProvider(LocalHaze provides haze) { RecordSheet(s, store, editing, { sheetOpen = false; editing = null; preset = null }, preset?.first, preset?.second) }
        FxLayer()
        celebrate?.let { c -> StreakCelebration(c.first, c.second) { celebrate = null } }
        endedPop?.let { e -> StreakEnded(e.first, e.second, onLog = { endedPop = null; editing = null; sheetOpen = true }) { endedPop = null } }
        warnPop?.let { w -> StreakWarning(w.streak, w.missedYesterday, onLog = { warnPop = null; editing = null; sheetOpen = true }) { warnPop = null } }
        if (!nameDismissed && s.settings.name.isBlank() && !s.settings.nameAsked) {
            NamePrompt(onSave = { n -> store.updateSettings(mapOf("name" to JsonPrimitive(n), "nameAsked" to JsonPrimitive(true))); nameDismissed = true },
                onSkip = { store.updateSettings(mapOf("nameAsked" to JsonPrimitive(true))); nameDismissed = true })
        }
    }
}

@Composable
private fun Header(s: AppState, onMenu: () -> Unit, onStreak: () -> Unit) {
    val ctx = LocalContext.current
    val cal = Calendar.getInstance()
    val hr = cal.get(Calendar.HOUR_OF_DAY)
    val greet = if (hr < 12) "Good morning" else if (hr < 18) "Good afternoon" else "Good evening"
    val name = s.settings.name.trim()
    val dateText = java.text.SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(cal.time)
    val st = s.streak()
    val lit = s.loggedDays().contains(todayDn())
    val tier = if (st > 0 && !lit) 0 else flameTier(st)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {
            Text(dateText, style = TextStyle(color = MW.Dim, fontSize = 13.sp, fontWeight = FontWeight.Medium))
            Text(
                if (name.isEmpty()) greet else "$greet, $name", maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
                style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
            )
        }
        Row(Modifier.height(40.dp).glass(CircleShape).clickable { onStreak() }.padding(start = 9.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            FlameIcon(tier, 32.dp)
            Spacer(Modifier.width(5.dp))
            Text("$st", style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold))
        }
        Box(Modifier.size(40.dp).glass(CircleShape).clickable { onMenu() }, contentAlignment = Alignment.Center) {
            Text((name.firstOrNull() ?: 'M').uppercase(), style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold))
        }
    }
}

@Composable
private fun TabPill(page: Float, onPick: (Int) -> Unit) {
    val names = listOf("Today", "Money", "Plan")
    BoxWithConstraints(Modifier.fillMaxWidth().glassDark(RoundedCornerShape(18.dp)).padding(4.dp)) {
        val w = maxWidth / 3
        Box(
            Modifier.offset(x = w * page).width(w).height(36.dp).clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.3f))
        )
        Row(Modifier.fillMaxWidth()) {
            names.forEachIndexed { i, n ->
                val on = Math.abs(page - i) < 0.5f
                Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(14.dp)).clickable { onPick(i) }, contentAlignment = Alignment.Center) {
                    Text(n, style = TextStyle(color = Color.White.copy(alpha = if (on) 1f else 0.9f), fontSize = 13.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
private fun Dock(modifier: Modifier, ctx: android.content.Context, onSearch: () -> Unit, onAdd: () -> Unit) {
    Row(
        modifier.width(240.dp).height(68.dp).glassDark(RoundedCornerShape(34.dp)).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround,
    ) {
        Box(Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Home, "Home", tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Box(
            Modifier.size(54.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MW.Mint, MW.Accent))).clickable { onAdd() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, "Add a record", tint = MW.OnAccent, modifier = Modifier.size(28.dp)) }
        Box(Modifier.size(50.dp).clip(CircleShape).clickable { onSearch() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Search, "Search and jump", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
        }
    }
}

/* ============================ shared bits ============================ */

private fun AppState.hidden(parts: Pair<String, String>, hide: Boolean) =
    if (hide) (settings.cur + " •••••") to "" else parts

@Composable
private fun Row3(icon: String, tint: Color, title: String, sub: String, subColor: Color, amount: String, amountColor: Color = Color.White, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            GlyphIcon(icon, Color.White.copy(alpha = 0.92f), 19.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
            Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = subColor, fontSize = 12.sp))
        }
        Text(amount, style = TextStyle(color = amountColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
    }
}

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.09f)))

/* ============================ TODAY ============================ */

private class NeedItem(val icon: String, val tint: Color, val title: String, val sub: String, val subColor: Color, val amount: String, val amountColor: Color, val sortDn: Long, val route: String = "")

@Composable
private fun TodayPage(s: AppState, hide: Boolean, toggleHide: () -> Unit, onHistory: () -> Unit, onBackup: () -> Unit, onOpen: (String) -> Unit, onRange: (String) -> Unit, store: Store, onEdit: (Rec) -> Unit) {
    val nowMs = System.currentTimeMillis()
    val bkAge = if (s.lastBackup > 0) ((nowMs - s.lastBackup) / 86_400_000.0).toInt() else -1
    if (!s.sample && s.recs.size >= 5 && nowMs >= s.backupSnooze && (bkAge < 0 || bkAge >= 14)) {
        Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp), tint = MW.Amber).padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            MwIcon("cloud", MW.Amber, 22.dp)
            Spacer(Modifier.width(10.dp))
            Text(if (bkAge < 0) "Back up your data. You have not made one yet." else "Back up your data. The last one was $bkAge days ago.", Modifier.weight(1f), style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp))
            Box(Modifier.clip(RoundedCornerShape(99.dp)).background(MW.Accent).clickable { onBackup() }.padding(horizontal = 14.dp, vertical = 8.dp)) { Text("Back up", style = TextStyle(color = MW.OnAccent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)) }
            Box(Modifier.size(36.dp).clip(CircleShape).clickable { store.setTopLive("backupSnooze", JsonPrimitive(System.currentTimeMillis() + 7L * 86_400_000L)) }, contentAlignment = Alignment.Center) { MwIcon("close", MW.Dim, 16.dp, weight = 2f) }
        }
        Spacer(Modifier.height(12.dp))
    }
    val active = s.accounts.filter { !it.ignored }
    val total = active.sumOf { s.balBase(it) }
    val ym = todayStr().take(7)
    val monthRecs = s.recs.filter { it.date.startsWith(ym) }
    val inn = monthRecs.filter { it.type == "income" }.sumOf { it.amount * s.rateOf(it.acct) }
    val out = monthRecs.filter { it.type == "expense" }.sumOf { it.amount * s.rateOf(it.acct) }
    val safe = s.safeToday()
    val al = s.allowanceInfo()

    GlassCard(Modifier.fillMaxWidth(), radius = 28.dp, padding = 22.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Lbl("Total balance", Modifier.weight(1f))
            Box(Modifier.clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha = 0.14f)).clickable { toggleHide() }.padding(horizontal = 12.dp, vertical = 5.dp)) {
                Text(if (hide) "Show" else "Hide", style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
            }
        }
        Spacer(Modifier.height(10.dp))
        MoneyText(s.hidden(s.moneyParts(total), hide), 46f)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("↓ " + (if (hide) "•••" else s.money(inn)), MW.Mint, MW.Accent)
            Pill("↑ " + (if (hide) "•••" else s.money(out)), MW.Rose, Color(0xFFFF6B81))
            Text("this month", style = TextStyle(color = MW.Dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
        }
    }
    Spacer(Modifier.height(14.dp))
    if (s.homeOn("safe")) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard(Modifier.weight(1f), tint = MW.Accent) {
            Lbl("Safe to spend today", color = MW.Mint)
            Spacer(Modifier.height(8.dp))
            MoneyText(s.hidden(s.moneyParts(safe.amount), hide), 26f)
            Spacer(Modifier.height(10.dp))
            if (al != null) {
                val used = if (al.per > 0) (al.spent / al.per).toFloat().coerceIn(0f, 1f) else 0f
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.14f))) {
                    Box(Modifier.fillMaxWidth(1f - used).height(5.dp).clip(RoundedCornerShape(5.dp)).background(MW.Accent))
                }
                Spacer(Modifier.height(8.dp))
                Text("${al.days} days to payday", style = TextStyle(color = MW.Dim, fontSize = 12.sp))
            } else {
                Text("from " + safe.reason, style = TextStyle(color = MW.Dim, fontSize = 12.sp), maxLines = 2)
            }
        }
        val bank = active.firstOrNull { it.kind == "bank" && it.bank != null }
        val bi = bank?.let { s.bankCalc(it) }
        val bankOk = bi != null && (bi.verdict == "ok" || bi.verdict == "locked")
        GlassCard(Modifier.weight(1f), tint = if (bi == null) null else if (bankOk) MW.Accent else Color(0xFFFF6B81)) {
            if (bank != null && bi != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Lbl("Safe in " + bank.name, Modifier.weight(1f, fill = false), if (bankOk) MW.Mint else MW.Rose)
                }
                Spacer(Modifier.height(8.dp))
                MoneyText(s.hidden(s.moneyParts(kotlin.math.max(0.0, bi.safe)), hide), 26f)
                Spacer(Modifier.height(10.dp))
                val ok = bi.verdict == "ok" || bi.verdict == "locked"
                Text(
                    (if (ok) "✓ Fee-safe" else if (bi.verdict == "add") "Add " + (if (hide) "•••" else s.money(bi.need)) else "Fee risk") + " · ${bi.daysLeft} days left",
                    style = TextStyle(color = if (ok) MW.Mint else MW.Rose, fontSize = 12.sp, fontWeight = FontWeight.SemiBold), maxLines = 2,
                )
            } else {
                Lbl("Bank")
                Spacer(Modifier.height(8.dp))
                Text("No bank account yet", style = TextStyle(color = MW.Dim, fontSize = 13.sp))
            }
        }
    }

    // Coming up (bills and open debts that are close)
    val t = todayDn()
    val needs = ArrayList<NeedItem>()
    for (b in s.bills) {
        val d = billDue(b, t).dn
        if (d - t <= 7) needs.add(NeedItem("\uD83D\uDDD3", MW.Amber, b.name, dueText(d), if (d < t) MW.Rose else MW.Dim, s.money(b.amount), Color.White, d, "bills"))
    }
    for (d in s.debts.filter { !it.paid }) {
        val due = if (d.due.isNotEmpty()) dn(d.due) else Long.MAX_VALUE
        val late = d.due.isNotEmpty() && due < t
        val who = if (d.theyOweMe) "${d.who} owes you" else "You owe ${d.who}"
        val sub = if (d.due.isEmpty()) who else who + " · " + dueText(due)
        needs.add(NeedItem("\uD83D\uDD01", if (d.theyOweMe) MW.Accent else Color(0xFFFF6B81), d.who, sub, if (late) MW.Rose else MW.Dim, s.money(d.left()), if (d.theyOweMe) MW.Mint else Color.White, due, "debts#" + d.id))
    }
    needs.sortBy { it.sortDn }
    if (needs.isNotEmpty() && s.homeOn("needs")) {
        Spacer(Modifier.height(14.dp))
        GlassCard(Modifier.fillMaxWidth(), radius = 26.dp, padding = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Lbl("Coming up", Modifier.weight(1f))
                Box(Modifier.height(22.dp).clip(RoundedCornerShape(11.dp)).background(MW.Amber.copy(alpha = 0.22f)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                    Text("${needs.size}", style = TextStyle(color = MW.Amber, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold))
                }
            }
            needs.take(4).forEachIndexed { i, n ->
                if (i > 0) Divider()
                Row3(n.icon, n.tint, n.title, n.sub, n.subColor, if (hide) "••••" else n.amount, n.amountColor, onClick = { if (n.route.isNotEmpty()) onOpen(n.route) })
            }
        }
    }

    // Today
    val today = todayStr()
    val todays = s.recs.filter { it.date == today }.sortedByDescending { it.ts }
    if (s.homeOn("today")) {
    Spacer(Modifier.height(18.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Lbl("Today", Modifier.weight(1f))
        if (todays.isNotEmpty()) Text("See all", Modifier.clickable { onRange("today") }.padding(vertical = 4.dp), style = TextStyle(color = MW.Mint, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
    }
    if (todays.isEmpty()) {
        Text("Nothing logged yet today.", Modifier.padding(top = 12.dp), style = TextStyle(color = MW.Dim, fontSize = 13.sp))
    } else {
        todays.take(6).forEachIndexed { i, r ->
            if (i > 0) Divider()
            val c = s.cat(r.cat)
            val sign = if (r.type == "income") 1.0 else if (r.type == "expense") -1.0 else 0.0
            val title = r.note.ifBlank { c?.name ?: when (r.type) { "transfer" -> "Transfer"; "check" -> "Balance check"; else -> "Record" } }
            Row3(
                if (r.type == "transfer") "⇄" else if (r.type == "check") "\u2696" else c?.icon ?: if (r.type == "income") "+" else "−", Color.White,
                title, (c?.name ?: "") + (if (c != null) " · " else "") + (s.acc(r.acct)?.name ?: ""), MW.Dim,
                if (hide) "••••" else if (r.type == "check") "= " + s.money(r.amount) else s.money(sign * r.amount, signed = true).let { if (sign == 0.0) s.money(r.amount) else it },
                if (r.type == "income") MW.Mint else Color.White,
                onClick = { onEdit(r) },
            )
        }
    }
    }

    // Earlier days
    val earlier = s.recs.filter { it.date < today }.sortedWith(compareByDescending<Rec> { it.date }.thenByDescending { it.ts }).take(8)
    if (earlier.isNotEmpty() && s.homeOn("earlier")) {
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Lbl("Earlier", Modifier.weight(1f))
            Text("See all", Modifier.clickable { onRange("earlier") }.padding(vertical = 4.dp), style = TextStyle(color = MW.Mint, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
        }
        earlier.forEachIndexed { i, r ->
            if (i > 0) Divider()
            val c = s.cat(r.cat)
            val sign = if (r.type == "income") 1.0 else if (r.type == "expense") -1.0 else 0.0
            val title = r.note.ifBlank { c?.name ?: when (r.type) { "transfer" -> "Transfer"; "check" -> "Balance check"; else -> "Record" } }
            Row3(
                if (r.type == "transfer") "⇄" else if (r.type == "check") "\u2696" else c?.icon ?: if (r.type == "income") "+" else "−", Color.White,
                title, fmtDate(r.date) + " · " + (s.acc(r.acct)?.name ?: ""), MW.Dim,
                if (hide) "••••" else if (r.type == "check") "= " + s.money(r.amount) else s.money(sign * r.amount, signed = true).let { if (sign == 0.0) s.money(r.amount) else it },
                if (r.type == "income") MW.Mint else Color.White,
                onClick = { onEdit(r) },
            )
        }
        Spacer(Modifier.height(14.dp))
        GhostButton("See all history", Modifier.fillMaxWidth(), icon = "search", onClick = onHistory)
    }
}

@Composable
private fun Pill(text: String, fg: Color, bg: Color) {
    Box(Modifier.clip(RoundedCornerShape(99.dp)).background(bg.copy(alpha = 0.18f)).padding(horizontal = 11.dp, vertical = 6.dp)) {
        Text(text, style = TextStyle(color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold))
    }
}

/* ============================ MONEY ============================ */

private val BRAND_TINT = listOf(
    Color(0xFF1E78FF), Color(0xFF22A05A), Color(0xFFFF8A28), Color(0xFF8A6AF0),
    Color(0xFFE13C4B), Color(0xFF14C88C), Color(0xFFB07F12), Color(0xFF5E9A44),
)

@Composable
private fun MoneyPage(s: AppState, hide: Boolean, onOpen: (String) -> Unit, onHold: (String) -> Unit, onEdit: (String) -> Unit, onDelete: (String) -> Unit, onAdd: () -> Unit) {
    var editMode by remember { mutableStateOf(false) }
    val bals = s.accounts.map { s.balBase(it) }
    val assets = bals.filter { it > 0 }.sum()
    val liab = -bals.filter { it < 0 }.sum()
    val total = bals.sum()
    Lbl("Net balance")
    Spacer(Modifier.height(8.dp))
    MoneyText(s.hidden(s.moneyParts(total), hide), 38f)
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).glass(RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
            Lbl("Assets", color = MW.Mint)
            Spacer(Modifier.height(3.dp))
            Text(s.hidden(s.moneyParts(assets), hide).let { it.first + it.second }, style = numStyle(17f))
        }
        Column(Modifier.weight(1f).glass(RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
            Lbl("Liabilities", color = MW.Rose)
            Spacer(Modifier.height(3.dp))
            Text(s.hidden(s.moneyParts(liab), hide).let { it.first + it.second }, style = numStyle(17f))
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("${s.accounts.size} account${if (s.accounts.size == 1) "" else "s"}", Modifier.weight(1f), style = TextStyle(color = MW.Dim, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold))
        Row(
            Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (editMode) MW.Accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f))
                .clickable { editMode = !editMode }.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MwIcon(if (editMode) "check" else "gear", if (editMode) MW.Mint else Color.White, 18.dp, weight = 2f)
            if (editMode) { Spacer(Modifier.width(6.dp)); Text("Done", style = TextStyle(color = MW.Mint, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)) }
        }
    }
    Spacer(Modifier.height(12.dp))
    s.accounts.withIndex().chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { (idx, a) -> AccountCard(s, a, idx, hide, editMode, Modifier.weight(1f), { onOpen(a.id) }, { onHold(a.id) }, { onEdit(a.id) }, { onDelete(a.id) }) }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    GhostButton("Add account", Modifier.fillMaxWidth(), icon = "plus", onClick = onAdd)
    if (!editMode && s.accounts.isNotEmpty()) Text("Tap a bank account to open its tracker. Hold any account to edit it.", Modifier.fillMaxWidth().padding(top = 10.dp), textAlign = TextAlign.Center, style = TextStyle(color = MW.Faint, fontSize = 11.5.sp))
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AccountCard(s: AppState, a: Account, idx: Int, hide: Boolean, editMode: Boolean, modifier: Modifier, onClick: () -> Unit, onHold: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val bal = s.balanceNow(a)
    val tint = BRAND_TINT[((a.color ?: idx) % BRAND_TINT.size + BRAND_TINT.size) % BRAND_TINT.size]
    val kind = when (a.kind) { "bank" -> "Bank account"; "card" -> "Card"; "ewallet" -> "E-wallet"; else -> "Cash / wallet" }
    val bi = if (a.kind == "bank") s.bankCalc(a) else null
    Box(modifier.height(128.dp)) {
        Column(
            Modifier.fillMaxSize().glass(RoundedCornerShape(24.dp), tint).combinedClickable(onClick = { if (editMode) onEdit() else onClick() }, onLongClick = onHold).padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    GlyphIcon(a.icon.ifBlank { a.name.take(1) }, Color.White, 22.dp)
                }
                if (bi != null && !editMode) {
                    val ok = bi.verdict == "ok" || bi.verdict == "locked"
                    Box(Modifier.clip(RoundedCornerShape(99.dp)).background((if (ok) MW.Accent else Color(0xFFFF6B81)).copy(alpha = 0.22f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            when (bi.verdict) { "ok" -> "ON TRACK"; "locked" -> "LOCKED IN"; "add" -> "ADD MONEY"; else -> "FEE RISK" },
                            style = TextStyle(color = if (ok) MW.Mint else MW.Rose, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp),
                        )
                    }
                }
            }
            Column {
                Text(a.name + if (a.ignored) " (ignored)" else "", maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                Text(kind, style = TextStyle(color = MW.Dim, fontSize = 11.5.sp))
            }
            val p = if (hide) (s.settings.cur + " \u2022\u2022\u2022\u2022\u2022") to "" else s.moneyParts(bal)
            MoneyText(p, 20f, if (bal < 0) MW.Rose else Color.White)
        }
        androidx.compose.animation.AnimatedVisibility(
            editMode, Modifier.align(Alignment.TopEnd),
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.6f),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.6f),
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(MW.Accent).clickable { onEdit() }, contentAlignment = Alignment.Center) { MwIcon("edit", MW.OnAccent, 20.dp, weight = 2.2f) }
                Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFFFCB7A)).clickable { onDelete() }, contentAlignment = Alignment.Center) { MwIcon("trash", Color(0xFF2A1400), 20.dp, weight = 2.2f) }
            }
        }
    }
}

/* ============================ PLAN ============================ */

@Composable
private fun PlanPage(s: AppState, hide: Boolean, onOpen: (String) -> Unit) {
    val ym = todayStr().take(7)
    val b = s.budgets[ym] ?: emptyMap()
    val ids = b.keys.filter { s.cat(it) != null }
    if (ids.isNotEmpty()) {
        val bud = ids.sumOf { b[it] ?: 0.0 }
        val spent = ids.sumOf { s.spentIn(ym, it) }
        GlassCard(Modifier.fillMaxWidth(), radius = 26.dp, padding = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Lbl("Budget", Modifier.weight(1f))
                Text((if (hide) "•••" else s.money(bud - spent)) + " left", style = TextStyle(color = MW.Dim, fontSize = 12.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(if (hide) s.settings.cur + " •••••" else s.money(spent), style = numStyle(30f))
                Text("  of " + (if (hide) "•••" else s.money(bud)), style = TextStyle(color = MW.Dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(10.dp))
            Bar(if (bud > 0) (spent / bud).toFloat() else 0f, MW.Accent)
            Spacer(Modifier.height(14.dp))
            ids.take(4).forEach { id ->
                val lim = b[id] ?: 0.0
                val sp = s.spentIn(ym, id)
                val f = if (lim > 0) (sp / lim).toFloat() else 0f
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.cat(id)?.name ?: "", Modifier.width(78.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold))
                    Box(Modifier.weight(1f)) { Bar(f, if (f >= 0.85f) MW.Amber else MW.Accent) }
                    Text("${(f * 100).toInt()}%", Modifier.width(44.dp), style = TextStyle(color = if (f >= 0.85f) MW.Amber else Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    if (s.goals.isNotEmpty()) {
        s.goals.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { g ->
                    val saved = g.contribs.sumOf { it.amount }
                    val f = if (g.target > 0) (saved / g.target).toFloat().coerceIn(0f, 1f) else 0f
                    GlassCard(Modifier.weight(1f).clickable { onOpen("goals") }, radius = 24.dp, padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) { GlyphIcon(g.icon.ifBlank { "🎯" }, MW.Mint, 18.dp); Spacer(Modifier.width(8.dp)); Text(g.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)) }
                        Spacer(Modifier.height(6.dp))
                        Text("${(f * 100).toInt()}%", style = numStyle(24f))
                        Spacer(Modifier.height(6.dp))
                        Bar(f, MW.Accent)
                        Spacer(Modifier.height(6.dp))
                        Text(if (hide) "••••" else (s.money(saved) + " of " + s.money(g.target)), style = TextStyle(color = MW.Dim, fontSize = 11.5.sp), maxLines = 2)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }

    if (s.bills.isNotEmpty()) {
        val t = todayDn()
        GlassCard(Modifier.fillMaxWidth(), radius = 26.dp, padding = 18.dp) {
            Lbl("Bills")
            s.bills.sortedBy { billDue(it, t).dn }.take(4).forEachIndexed { i, b ->
                if (i > 0) Divider()
                val d = billDue(b, t).dn
                Row3("🧾", if (d <= t) MW.Amber else Color.White, b.name, dueText(d), if (d < t) MW.Rose else MW.Dim, if (hide) "••••" else s.money(b.amount), onClick = { onOpen("bills") })
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    val open = s.debts.filter { !it.paid }.sortedBy { if (it.due.isEmpty()) Long.MAX_VALUE else dn(it.due) }
    val owed = open.filter { it.theyOweMe }.sumOf { it.left() }
    val iowe = open.filter { !it.theyOweMe }.sumOf { it.left() }
    GlassCard(Modifier.fillMaxWidth(), radius = 26.dp, padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Lbl("Debts", Modifier.weight(1f))
            Text("Open", Modifier.clickable { onOpen("debts") }.padding(vertical = 4.dp), style = TextStyle(color = MW.Mint, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
        }
        if (open.isEmpty()) {
            Text("No open debts", Modifier.padding(top = 8.dp), style = TextStyle(color = MW.Dim, fontSize = 13.sp))
        } else {
            Text(if (hide) "Owed to you ••• · You owe •••" else "Owed to you ${s.money(owed)} · You owe ${s.money(iowe)}", Modifier.padding(top = 6.dp), style = TextStyle(color = MW.Dim, fontSize = 12.sp))
            val t = todayDn()
            open.take(4).forEach { d ->
                Divider()
                val late = d.due.isNotEmpty() && dn(d.due) < t
                val who = if (d.theyOweMe) "Owes you" else "You owe"
                val sub = who + (if (d.pays.isNotEmpty()) " · paid " + s.money(d.paidSoFar()) + " of " + s.money(d.amount) else "") + (if (d.due.isNotEmpty()) " · " + dueText(dn(d.due)) else "")
                Row3("\uD83D\uDD01", if (d.theyOweMe) MW.Accent else Color(0xFFFF6B81), d.who, sub, if (late) MW.Rose else MW.Dim, if (hide) "••••" else s.money(d.left()), if (d.theyOweMe) MW.Mint else Color.White, onClick = { onOpen("debts") })
            }
        }
    }
    if (ids.isEmpty() && s.goals.isEmpty() && s.bills.isEmpty()) {
        Spacer(Modifier.height(14.dp))
        Text("Budgets, goals and bills you set up will show here.", style = TextStyle(color = MW.Dim, fontSize = 13.sp))
    }
}

@Composable
private fun Bar(f: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.14f))) {
        Box(Modifier.fillMaxWidth(f.coerceIn(0.001f, 1f)).height(6.dp).clip(RoundedCornerShape(6.dp)).background(color))
    }
}


/** Asks once for a name so the greeting can say it. */
@Composable
private fun NamePrompt(onSave: (String) -> Unit, onSkip: () -> Unit) {
    var name by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onSkip) {
        Column(
            Modifier.popIn().fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xFF0E1426))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(28.dp)).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("What should I call you?", style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold))
            Text("Just for the greeting on Home. It stays on this phone.", style = TextStyle(color = MW.Dim, fontSize = 13.sp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 16.dp, vertical = 14.dp)) {
                androidx.compose.foundation.text.BasicTextField(
                    name, { name = it.take(24) }, singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MW.Accent),
                    decorationBox = { inner -> Box { if (name.isEmpty()) Text("Your name", style = TextStyle(color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp)); inner() } },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.1f)).clickable { onSkip() }, contentAlignment = Alignment.Center) {
                    Text("Skip", style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                }
                Box(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(MW.Mint, MW.Accent)))
                        .clickable { if (name.isNotBlank()) onSave(name.trim()) else onSkip() },
                    contentAlignment = Alignment.Center,
                ) { Text("Save", style = TextStyle(color = MW.OnAccent, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)) }
            }
        }
    }
}
