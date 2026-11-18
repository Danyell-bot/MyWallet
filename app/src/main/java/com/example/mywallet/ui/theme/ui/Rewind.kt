package com.example.mywallet.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.example.mywallet.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.data.AppState
import com.example.mywallet.data.cat
import com.example.mywallet.data.dn
import com.example.mywallet.data.money
import com.example.mywallet.data.rateOf
import com.example.mywallet.data.todayDn
import com.example.mywallet.data.todayStr
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** the Rewind sounds: a start jingle, a soft tick between slides, and a finish fanfare */
private class Sfx(ctx: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(3)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val ids = intArrayOf(pool.load(ctx, R.raw.mw_rewind, 1), pool.load(ctx, R.raw.mw_tick, 1), pool.load(ctx, R.raw.mw_finish, 1))
    fun play(i: Int) { try { pool.play(ids[i], 1f, 1f, 1, 0, 1f) } catch (e: Exception) { } }
    fun release() { try { pool.release() } catch (e: Exception) { } }
}

private class Slide(val kind: String, val c1: Color, val c2: Color)

private const val SLIDE_MS = 5600
private val DAYS = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

@Composable
private fun countTo(target: Float, ms: Int = 1500, wait: Int = 350): Float {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(wait.toLong()); go = true }
    val v by animateFloatAsState(if (go) target else 0f, tween(ms, easing = FastOutSlowInEasing), label = "count")
    return v
}

@Composable
private fun Reveal(wait: Int, content: @Composable () -> Unit) {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(wait.toLong()); on = true }
    AnimatedVisibility(on, enter = fadeIn(tween(550)) + slideInVertically(tween(550)) { it / 4 }) { content() }
}

@Composable
private fun Kicker(t: String) = Text(t.uppercase(), style = TextStyle(color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp))

@Composable
private fun Sub(t: String) = Text(t, style = TextStyle(color = Color.White.copy(alpha = 0.92f), fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold))

@Composable
fun RecapScreen(s: AppState, onClose: () -> Unit) {
    BackHandler { onClose() }
    val nowYm = todayStr().take(7)
    var ym by remember { mutableStateOf(nowYm) }

    val exp = s.recs.filter { it.type == "expense" && it.date.startsWith(ym) }
    val ex = exp.sumOf { it.amount * s.rateOf(it.acct) }
    val inc = s.sumMonth(ym, "income")
    val pex = s.sumMonth(ymAdd(ym, -1), "expense")
    val by = exp.groupBy { it.cat }.mapValues { (_, l) -> l.sumOf { it.amount * s.rateOf(it.acct) } }
    val ranked = by.entries.sortedByDescending { it.value }
    val top = ranked.firstOrNull()
    val big = exp.maxByOrNull { it.amount * s.rateOf(it.acct) }
    val last = minOf(monthEnd(ym), todayDn()); val days = maxOf(0L, last - monthStart(ym) + 1)
    val spentDays = exp.map { it.date }.toSet().size
    val quiet = maxOf(0L, days - spentDays).toInt()
    val diff = if (pex > 0) Math.round((ex - pex) / pex * 100).toInt() else null
    val byDow = DoubleArray(7)
    exp.forEach { byDow[Math.floorMod(dn(it.date) + 4, 7L).toInt()] += it.amount * s.rateOf(it.acct) }
    val busiest = if (ex > 0) byDow.indices.maxByOrNull { byDow[it] } else null

    val slides = remember(ym, exp.isEmpty()) {
        if (exp.isEmpty()) listOf(
            Slide("intro", Color(0xFF0FB98A), Color(0xFF0B3A7A)), Slide("empty", Color(0xFF3B2A8C), Color(0xFF0B1020)),
        ) else listOf(
            Slide("intro", Color(0xFF0FB98A), Color(0xFF0B3A7A)), Slide("spent", Color(0xFF7C3AED), Color(0xFF1B1464)),
            Slide("cats", Color(0xFF0E7490), Color(0xFF0B1B4D)), Slide("top", Color(0xFFFF8A5A), Color(0xFF8E1257)),
            Slide("big", Color(0xFFE8A93A), Color(0xFF7A0C2B)), Slide("days", Color(0xFF16C784), Color(0xFF1B38D6)),
            Slide("flow", Color(0xFF2F7BFF), Color(0xFF4B1FA8)), Slide("final", Color(0xFFB8A6FF), Color(0xFF1B2766)),
        )
    }
    var idx by remember(ym) { mutableStateOf(0) }
    val ctx = LocalContext.current
    var sound by remember { mutableStateOf(ctx.getSharedPreferences("mw_prefs", 0).getBoolean("sfx", true)) }
    val sfx = remember { Sfx(ctx) }
    DisposableEffect(Unit) { onDispose { sfx.release() } }
    LaunchedEffect(idx, slides.size) {
        if (sound) {
            if (idx == 0) { delay(650); sfx.play(0) } else if (idx == slides.lastIndex) sfx.play(2) else sfx.play(1)
        }
    }
    var paused by remember { mutableStateOf(false) }
    val prog = remember(idx, ym) { Animatable(0f) }
    LaunchedEffect(idx, paused, ym) {
        if (!paused && idx < slides.lastIndex) {
            prog.animateTo(1f, tween(((1f - prog.value) * SLIDE_MS).toInt().coerceAtLeast(1), easing = LinearEasing))
            idx += 1
        }
    }
    val sl = slides[idx.coerceIn(0, slides.lastIndex)]
    val c1 by animateColorAsState(sl.c1, tween(700), label = "c1")
    val c2 by animateColorAsState(sl.c2, tween(700), label = "c2")
    val stars = remember { val rnd = kotlin.random.Random(11); List(70) { floatArrayOf(rnd.nextFloat(), rnd.nextFloat(), 1f + rnd.nextFloat() * 2.2f, (1 + rnd.nextInt(2)).toFloat(), rnd.nextFloat()) } }
    val bubbles = remember { val rnd = kotlin.random.Random(5); List(16) { floatArrayOf(rnd.nextFloat(), rnd.nextFloat(), 30f + rnd.nextFloat() * 70f, (1 + rnd.nextInt(2)).toFloat(), rnd.nextFloat()) } }
    val inf = rememberInfiniteTransition(label = "bg")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart), label = "t")

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c1, c2)))
            .pointerInput(idx, slides.size) {
                detectTapGestures(
                    onPress = { paused = true; tryAwaitRelease(); paused = false },
                    onTap = { o -> if (o.x < size.width * 0.3f) { if (idx > 0) idx -= 1 } else if (idx < slides.lastIndex) idx += 1 },
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val a = (t * 2 * PI).toFloat()
            // soft glows
            val r = w * 0.75f
            val c = Offset(w * (0.2f + 0.12f * sin(a)), h * (0.18f + 0.06f * cos(a)))
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), center = c, radius = r), r, c)
            val d = Offset(w * (0.85f - 0.12f * cos(a)), h * (0.82f - 0.06f * sin(a)))
            drawCircle(Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent), center = d, radius = r), r, d)

            // rewind dial: rings and a hand turning backwards, like time running in reverse
            val mid = Offset(w * 0.5f, h * 0.44f)
            for (i in 0 until 5) {
                val rr = w * (0.26f + 0.17f * i)
                val dash = floatArrayOf(6f + 10f * i, 14f + 6f * i)
                rotate(-360f * t * (i + 1), mid) {
                    drawCircle(Color.White.copy(alpha = 0.10f + 0.03f * (4 - i)), rr, mid, style = Stroke(1.6f + 0.5f * i, pathEffect = PathEffect.dashPathEffect(dash)))
                }
            }
            rotate(-360f * t * 6, mid) {
                drawLine(Color.White.copy(alpha = 0.35f), mid, Offset(mid.x, mid.y - w * 0.56f), strokeWidth = 2.5f)
            }
            rotate(-360f * t, mid) {
                drawLine(Color.White.copy(alpha = 0.22f), mid, Offset(mid.x, mid.y - w * 0.34f), strokeWidth = 5f)
            }
            drawCircle(Color.White.copy(alpha = 0.5f), 6f, mid)

            // twinkling stars drifting down slowly
            for (p in stars) {
                val y = ((p[1] + t * p[3]) % 1f) * h
                val x = p[0] * w + sin(a * p[3] + p[4] * 6f) * 10f
                val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin(a * (p[3] * 2f) + p[4] * 9f))
                drawCircle(Color.White.copy(alpha = 0.55f * tw), p[2], Offset(x, y))
            }
            // memories floating up: soft glowing bubbles
            for (p in bubbles) {
                val y = h * (1f - ((p[1] + t * p[3]) % 1f))
                val x = p[0] * w + sin(a * p[3] + p[4] * 6f) * 18f
                val rad = p[2]
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.34f), Color.Transparent), center = Offset(x, y), radius = rad), rad, Offset(x, y))
            }
        }

        AnimatedContent(
            targetState = idx.coerceIn(0, slides.lastIndex), modifier = Modifier.fillMaxSize(), label = "slide",
            transitionSpec = { (fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 14 }) togetherWith fadeOut(tween(250)) },
        ) { i ->
            val k = slides[i].kind
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 30.dp).padding(top = 56.dp, bottom = 24.dp), verticalArrangement = Arrangement.Center) {
                when (k) {
                    "intro" -> {
                        Kicker("Your")
                        Spacer(Modifier.height(8.dp))
                        Text(monthLabel(ym).substringBefore(" "), style = TextStyle(color = Color.White, fontSize = 60.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp))
                        Text("REWIND", style = TextStyle(color = Color.White.copy(alpha = 0.9f), fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp))
                        Spacer(Modifier.height(22.dp))
                        Reveal(500) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)).clickable { ym = ymAdd(ym, -1) }, contentAlignment = Alignment.Center) { MwIcon("back", Color.White, 20.dp, weight = 2f) }
                                Text(monthLabel(ym), Modifier.padding(horizontal = 16.dp), style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                                if (ym < nowYm) Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)).clickable { ym = ymAdd(ym, 1) }, contentAlignment = Alignment.Center) { MwIcon("back", Color.White, 20.dp, Modifier.rotate(180f), weight = 2f) }
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Reveal(900) { Sub("Tap to start. Hold to pause.") }
                    }
                    "empty" -> {
                        Kicker("Quiet month")
                        Spacer(Modifier.height(10.dp))
                        Text("Nothing spent", style = TextStyle(color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Black))
                        Spacer(Modifier.height(10.dp))
                        Reveal(400) { Sub("There are no expenses in " + monthLabel(ym) + ". Pick another month from the first slide.") }
                        Spacer(Modifier.height(24.dp))
                        Reveal(700) { GhostButton("Close", Modifier.fillMaxWidth()) { onClose() } }
                    }
                    "spent" -> {
                        Kicker("You spent")
                        val v = countTo(ex.toFloat())
                        Spacer(Modifier.height(8.dp))
                        val txt = s.money(v.toDouble())
                        Text(txt, maxLines = 1, style = numStyle(if (txt.length > 10) 44f else 62f))
                        Spacer(Modifier.height(14.dp))
                        Reveal(1200) {
                            Sub(
                                if (diff == null) "across $days day${if (days == 1L) "" else "s"} in ${monthLabel(ym)}."
                                else if (diff > 0) "That is $diff% more than last month." else if (diff < 0) "That is ${-diff}% less than last month. Nice." else "Exactly the same as last month.",
                            )
                        }
                    }
                    "cats" -> {
                        Kicker("Where it went")
                        Spacer(Modifier.height(18.dp))
                        ranked.take(4).forEachIndexed { n, e ->
                            val share = if (ex > 0) (e.value / ex).toFloat() else 0f
                            var go by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { delay(300L + n * 220L); go = true }
                            val f by animateFloatAsState(if (go) share else 0f, tween(900, easing = FastOutSlowInEasing), label = "bar")
                            Column(Modifier.padding(bottom = 18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    GlyphIcon(s.cat(e.key)?.icon ?: "🏷", Color.White, 22.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(s.cat(e.key)?.name ?: "No category", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold))
                                    Text("${Math.round(share * 100)}%", style = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold))
                                }
                                Spacer(Modifier.height(8.dp))
                                Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))) {
                                    Box(Modifier.fillMaxWidth(f.coerceAtLeast(0.001f)).height(10.dp).clip(CircleShape).background(Color.White))
                                }
                            }
                        }
                    }
                    "top" -> {
                        Kicker("Your top category")
                        Spacer(Modifier.height(20.dp))
                        val pop by animateFloatAsState(1f, tween(700, easing = FastOutSlowInEasing), label = "pop")
                        Box(Modifier.size(110.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            GlyphIcon(s.cat(top?.key ?: "")?.icon ?: "🏷", Color.White, (56 * pop).dp)
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(s.cat(top?.key ?: "")?.name ?: "No category", maxLines = 2, style = TextStyle(color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black, lineHeight = 48.sp))
                        Spacer(Modifier.height(10.dp))
                        Reveal(700) { Sub(s.money(top?.value ?: 0.0) + " · " + (if (ex > 0 && top != null) Math.round(top.value / ex * 100) else 0) + "% of everything you spent.") }
                    }
                    "big" -> {
                        Kicker("Biggest expense")
                        val v = countTo(((big?.amount ?: 0.0) * (if (big != null) s.rateOf(big.acct) else 1.0)).toFloat())
                        Spacer(Modifier.height(8.dp))
                        val txt = s.money(v.toDouble())
                        Text(txt, maxLines = 1, style = numStyle(if (txt.length > 10) 44f else 62f))
                        Spacer(Modifier.height(14.dp))
                        Reveal(1200) { Sub((big?.note?.ifBlank { null } ?: s.cat(big?.cat ?: "")?.name ?: "Expense") + " on " + (big?.date?.let { fmtDate(it) } ?: "")) }
                    }
                    "days" -> {
                        Kicker("No-spend days")
                        val v = countTo(quiet.toFloat(), 1100)
                        Text(Math.round(v).toString(), style = numStyle(110f))
                        Reveal(900) {
                            Column {
                                Sub("out of $days day${if (days == 1L) "" else "s"} you spent nothing.")
                                if (busiest != null) { Spacer(Modifier.height(10.dp)); Sub("You spent the most on ${DAYS[busiest]}s.") }
                            }
                        }
                    }
                    "flow" -> {
                        Kicker("Money in and out")
                        Spacer(Modifier.height(22.dp))
                        val mx = maxOf(inc, ex, 1.0)
                        listOf(Triple("In", inc, Color(0xFFB8FFE6)), Triple("Out", ex, Color(0xFFFFC2CD))).forEachIndexed { n, (label, v, col) ->
                            var go by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { delay(300L + n * 350L); go = true }
                            val f by animateFloatAsState(if (go) (v / mx).toFloat() else 0f, tween(1000, easing = FastOutSlowInEasing), label = "flow")
                            Column(Modifier.padding(bottom = 18.dp)) {
                                Row { Text(label, Modifier.weight(1f), style = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)); Text(s.money(v), style = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)) }
                                Spacer(Modifier.height(8.dp))
                                Box(Modifier.fillMaxWidth().height(18.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))) { Box(Modifier.fillMaxWidth(f.coerceAtLeast(0.001f)).height(18.dp).clip(CircleShape).background(col)) }
                            }
                        }
                        Reveal(1300) { Sub(if (inc >= ex) "You kept ${s.money(inc - ex)}. Nice!" else "You spent ${s.money(ex - inc)} more than came in.") }
                    }
                    else -> {
                        Kicker("That is your " + monthLabel(ym).substringBefore(" "))
                        Spacer(Modifier.height(16.dp))
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = 0.16f)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            @Composable fun line(a: String, b: String) = Row { Text(a, Modifier.weight(1f), style = TextStyle(color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp)); Text(b, maxLines = 1, style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)) }
                            line("Spent", s.money(ex)); line("Money in", s.money(inc))
                            line("Top category", s.cat(top?.key ?: "")?.name ?: "None"); line("No-spend days", quiet.toString())
                            if (diff != null) line("vs last month", (if (diff > 0) "+" else "") + "$diff%")
                        }
                        Spacer(Modifier.height(20.dp))
                        BigButton("Done", Color.White, Color(0xFF14183A)) { onClose() }
                        Spacer(Modifier.height(10.dp))
                        GhostButton("Watch again", Modifier.fillMaxWidth()) { idx = 0 }
                    }
                }
            }
        }

        // progress bars + close
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                slides.forEachIndexed { i, _ ->
                    val f = if (i < idx) 1f else if (i == idx) (if (i == slides.lastIndex) 1f else prog.value) else 0f
                    Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))) {
                        Box(Modifier.fillMaxWidth(f.coerceIn(0.001f, 1f)).height(3.dp).background(Color.White))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Rewind · " + monthLabel(ym), Modifier.weight(1f), style = TextStyle(color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                Box(Modifier.size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.25f)).clickable {
                    sound = !sound; ctx.getSharedPreferences("mw_prefs", 0).edit().putBoolean("sfx", sound).apply()
                }, contentAlignment = Alignment.Center) { MwIcon(if (sound) "volume" else "volumeoff", Color.White, 16.dp, weight = 2f) }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.25f)).clickable { onClose() }, contentAlignment = Alignment.Center) { MwIcon("close", Color.White, 16.dp, weight = 2f) }
            }
        }
    }
}
