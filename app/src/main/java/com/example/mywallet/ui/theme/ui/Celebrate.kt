package com.example.mywallet.ui

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.R
import com.example.mywallet.data.flameTier
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.pow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.PI
import kotlin.math.sin

private fun playOnce(ctx: Context, res: Int) {
    try {
        if (!ctx.getSharedPreferences("mw_prefs", 0).getBoolean("sfx", true)) return
        MediaPlayer.create(ctx, res)?.apply { setOnCompletionListener { it.release() }; start() }
    } catch (e: Exception) { }
}

@Composable
private fun RollDigit(old: Char, new: Char, p: Float, tint: Color = Color(0xFFFFE9B0)) {
    val style = numStyle(64f, tint)
    // a blank slot has no width, so the digits that are left always sit centered
    val wOld = if (old == ' ') 0f else 1f
    val wNew = if (new == ' ') 0f else 1f
    val w = if (old == new || p >= 1f) wNew else wOld + (wNew - wOld) * p
    Box(Modifier.width((46f * w).dp).height(76.dp).clip(RectangleShape), contentAlignment = Alignment.Center) {
        if (old == new || p >= 1f) {
            if (new != ' ') Text(new.toString(), style = style, softWrap = false)
        } else {
            // the old digit rolls down and out, the new one drops in from above
            if (old != ' ') Text(old.toString(), Modifier.graphicsLayer { translationY = size.height * 1.1f * p; alpha = 1f - p }, style = style, softWrap = false)
            if (new != ' ') Text(new.toString(), Modifier.graphicsLayer { translationY = -size.height * 1.1f * (1f - p); alpha = p }, style = style, softWrap = false)
        }
    }
}

/**
 * The "new day" moment: everything darkens, the grey flame relights, then the streak number rolls up
 * one digit at a time (19 -> 20).
 */
@Composable
fun StreakCelebration(from: Int, to: Int, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val tier = flameTier(to)
    val len = maxOf(from.toString().length, to.toString().length)
    val a = from.toString().padStart(len, ' ')
    val b = to.toString().padStart(len, ' ')
    val scrim = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val lit = remember { Animatable(0f) }
    val out = remember { Animatable(1f) }
    val digits = remember { List(len) { Animatable(0f) } }
    var closing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { scrim.animateTo(1f, tween(450)) }
            launch { delay(250); title.animateTo(1f, tween(650)) }
            launch { delay(750); playOnce(ctx, R.raw.mw_finish); lit.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
        }
        delay(250)
        // the number ticks over, left digit first, then the next
        for (i in 0 until len) {
            if (a[i] != b[i]) {
                playOnce(ctx, R.raw.mw_tick)
                digits[i].animateTo(1f, tween(850, easing = FastOutSlowInEasing))
                delay(250)
            }
        }
        delay(1300)
        closing = true
    }
    LaunchedEffect(closing) {
        if (closing) { out.animateTo(0f, tween(350)); onDone() }
    }

    Box(
        Modifier.fillMaxSize().graphicsLayer { alpha = out.value }
            .background(MW.Bg.copy(alpha = 0.9f * scrim.value))
            .clickable(remember { MutableInteractionSource() }, null) { closing = true },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "NEW DAY", Modifier.graphicsLayer { alpha = title.value; translationY = (1f - title.value) * 24f },
                style = MwTs(color = Color(0xFFFFD27A), fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Your streak is back on", Modifier.graphicsLayer { alpha = title.value },
                style = MwTs(color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(18.dp))
            Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val l = lit.value
                    drawCircle(
                        Brush.radialGradient(listOf(Color(0xFFFFA63D).copy(alpha = 0.5f * l), Color.Transparent), radius = size.width * (0.25f + 0.25f * l)),
                        radius = size.width * (0.25f + 0.25f * l),
                    )
                }
                FlameIcon(tier, 170.dp, Modifier.graphicsLayer { alpha = 1f - lit.value }, locked = true)
                FlameIcon(tier, 170.dp, Modifier.graphicsLayer {
                    alpha = lit.value
                    val pop = 0.85f + 0.15f * lit.value + 0.12f * sin(PI.toFloat() * lit.value)
                    scaleX = pop; scaleY = pop
                })
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                for (i in 0 until len) RollDigit(a[i], b[i], digits[i].value)
            }
            Text("day streak", style = MwTs(color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp))
        }
    }
}

/**
 * The "you missed a day" moment, same style as the new-day one but dim: the flame stays grey and flickers,
 * and it says whether the weekly free day just saved the streak.
 */
@Composable
fun StreakWarning(streak: Int, missedYesterday: Boolean, onLog: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val tier = flameTier(streak)
    val scrim = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val flick = remember { Animatable(0f) }
    val out = remember { Animatable(1f) }
    var closing by remember { mutableStateOf(false) }
    val warm = Color(0xFFFFB45A)

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { scrim.animateTo(1f, tween(450)) }
            launch { delay(250); title.animateTo(1f, tween(650)) }
            launch {
                delay(700); playOnce(ctx, R.raw.mw_chime)
                // the grey flame tries to catch, then dies down again
                repeat(3) { flick.animateTo(0.45f, tween(260)); flick.animateTo(0.05f, tween(380)) }
            }
        }
    }
    LaunchedEffect(closing) { if (closing) { out.animateTo(0f, tween(300)); onDone() } }

    Box(
        Modifier.fillMaxSize().graphicsLayer { alpha = out.value }
            .background(MW.Bg.copy(alpha = 0.9f * scrim.value))
            .clickable(remember { MutableInteractionSource() }, null) { closing = true },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
            Text(
                if (missedYesterday) "MISSED A DAY" else "NO FREE DAY LEFT",
                Modifier.graphicsLayer { alpha = title.value; translationY = (1f - title.value) * 24f },
                style = MwTs(color = warm, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (missedYesterday) "Your free day for this week was used" else "This week's free day is already used",
                Modifier.graphicsLayer { alpha = title.value },
                style = MwTs(color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
            )
            Spacer(Modifier.height(18.dp))
            Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val l = flick.value
                    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFA63D).copy(alpha = 0.4f * l), Color.Transparent), radius = size.width * 0.4f), radius = size.width * 0.4f)
                }
                FlameIcon(tier, 170.dp, locked = true)
                FlameIcon(tier, 170.dp, Modifier.graphicsLayer { alpha = flick.value; val p = 0.95f + 0.1f * flick.value; scaleX = p; scaleY = p })
            }
            Spacer(Modifier.height(6.dp))
            Text("$streak", style = numStyle(64f, Color(0xFFFFE9B0)))
            Text("day streak, still alive", style = MwTs(color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp))
            Spacer(Modifier.height(16.dp))
            Text(
                if (missedYesterday) "Log something today. Missing another day this week will end your streak."
                else "Log something today to keep your streak going.",
                Modifier.graphicsLayer { alpha = title.value },
                style = MwTs(color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
            )
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier.clip(MwShape(99.dp)).background(Brush.linearGradient(listOf(MW.Mint, MW.Accent)))
                    .clickable(remember { MutableInteractionSource() }, null) { onLog() }.padding(horizontal = 28.dp, vertical = 13.dp),
            ) { Text("Log something now", style = MwTs(color = MW.OnAccent, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)) }
            Spacer(Modifier.height(10.dp))
            Text("Later", Modifier.clickable(remember { MutableInteractionSource() }, null) { closing = true }.padding(12.dp), style = MwTs(color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
        }
    }
}

/* ============================ STREAK ENDED (the snap) ============================ */

/* ============================ STREAK ENDED (the fire goes out) ============================ */

private fun rn(i: Int, k: Int): Float { val v = sin(i * 12.9898 + k * 78.233) * 43758.5453; return (v - floor(v)).toFloat() }
private fun sm(x: Float): Float { val c = x.coerceIn(0f, 1f); return c * c * (3f - 2f * c) }

/** grey wisps that rise from the dead flame; `lean` pushes them sideways (wind) */
private fun DrawScope.smoke(t: Float, from: Float, lean: Float, tone: Color) {
    val u = density; val fx = size.width / 2; val fy = size.height / 2
    for (i in 0 until 9) {
        val tau = ((t - from - i * 0.028f) / 0.38f)
        if (tau <= 0f || tau >= 1f) continue
        val x = fx + sin(tau * 5f + i * 1.7f) * 14f * u + lean * tau * 90f * u + (rn(i, 4) - 0.5f) * 26f * u
        val y = fy - 10f * u - tau * 125f * u
        drawCircle(tone.copy(alpha = 0.34f * (1f - tau) * sm(tau * 6f)), (10f + tau * 24f) * u, Offset(x, y))
    }
}

private fun DrawScope.waterFx(t: Float) {
    val u = density; val fx = size.width / 2; val fy = size.height / 2
    val hit = Offset(fx, fy - 20f * u)
    val water = Color(0xFF6CC4FF); val deep = Color(0xFF2F8FE0)
    // a big blob of water falls from above and lands on the flame
    val fall = ((t - 0.06f) / 0.28f)
    if (fall in 0f..1f) {
        val q = fall * fall
        val y = -40f * u + (hit.y + 40f * u) * q
        val sw = (54f - 8f * fall) * u; val sh = (74f + 26f * fall) * u
        drawOval(Brush.radialGradient(listOf(water.copy(alpha = 0.95f), deep.copy(alpha = 0.9f)), Offset(fx, y), sh * 0.7f), Offset(fx - sw / 2, y - sh / 2), Size(sw, sh))
        drawOval(Color.White.copy(alpha = 0.55f), Offset(fx - sw * 0.28f, y - sh * 0.3f), Size(sw * 0.2f, sh * 0.26f))
        for (k in 0 until 5) {
            val yy = y - sh * 0.5f - (k + 1) * 14f * u
            if (yy > -10f * u) drawCircle(water.copy(alpha = 0.6f - k * 0.1f), (4f - k * 0.5f) * u, Offset(fx + (rn(k, 1) - 0.5f) * 18f * u, yy))
        }
    }
    val hitT = (t - 0.34f) / 0.5f
    if (hitT > 0f && hitT < 1f) {
        // splash crown: droplets thrown out in arcs
        for (i in 0 until 34) {
            val ang = PI.toFloat() * (1.05f + 0.9f * rn(i, 2))
            val sp = (70f + 90f * rn(i, 3)) * u
            val tau = (hitT * 1.4f).coerceAtMost(1f)
            val x = hit.x + cos(ang) * sp * tau
            val y = hit.y + sin(ang) * sp * tau + 120f * u * tau * tau
            drawCircle(water.copy(alpha = (1f - hitT) * 0.95f), (2.6f + 3f * rn(i, 5)) * u * (1f - 0.5f * tau), Offset(x, y))
        }
        // flat ripples spreading out on the ground
        for (r in 0 until 3) {
            val rt = (hitT * 1.3f - r * 0.18f).coerceIn(0f, 1f)
            if (rt > 0f) drawOval(water.copy(alpha = 0.5f * (1f - rt)), Offset(fx - (30f + 90f * rt) * u, fy + 70f * u - (7f + 11f * rt) * u), Size((60f + 180f * rt) * u, (14f + 22f * rt) * u), style = Stroke(2.2f * u))
        }
        drawOval(deep.copy(alpha = 0.35f * (1f - hitT)), Offset(fx - 70f * u * (0.4f + hitT), fy + 62f * u), Size(140f * u * (0.4f + hitT), 18f * u))
    }
    // steam rolls up while it hisses
    for (i in 0 until 9) {
        val tau = ((t - 0.4f - i * 0.045f) / 0.45f)
        if (tau <= 0f || tau >= 1f) continue
        drawCircle(Color.White.copy(alpha = 0.3f * (1f - tau) * sm(tau * 5f)), (14f + tau * 30f) * u, Offset(fx + sin(tau * 4f + i * 1.3f) * 22f * u, fy - 20f * u - tau * 110f * u))
    }
    smoke(t, 0.62f, 0f, Color(0xFFB5BCC8))
}

private fun DrawScope.sandFx(t: Float) {
    val u = density; val fx = size.width / 2; val fy = size.height / 2
    val baseY = fy + 74f * u
    val hMax = 118f * u
    val rise = sm(t / 0.78f)
    val fade = 1f - sm((t - 0.9f) / 0.1f)
    fun surface(x: Float): Float { val d = (x - fx) / (66f * u); return baseY - hMax * rise * (1f - d * d).coerceAtLeast(0f) }
    // a tapered stream pouring from the top
    run {
        val sOn = sm(t / 0.08f) * (1f - sm((t - 0.74f) / 0.1f))
        if (sOn > 0.01f) {
            val top = -8f * u; val bot = surface(fx)
            val p = Path(); p.moveTo(fx - 3f * u, top); p.lineTo(fx + 3f * u, top); p.lineTo(fx + 12f * u, bot); p.lineTo(fx - 12f * u, bot); p.close()
            drawPath(p, Brush.verticalGradient(listOf(Color(0xFFE8CB82).copy(alpha = 0.15f * sOn * fade), Color(0xFFD7B66A).copy(alpha = 0.6f * sOn * fade)), top, bot))
        }
    }
    // dust puffs where it lands
    for (i in 0 until 8) {
        val tau = ((t - 0.12f - i * 0.07f) / 0.35f)
        if (tau <= 0f || tau >= 1f || t > 0.8f) continue
        val side = if (i % 2 == 0) -1f else 1f
        drawCircle(Color(0xFFD9C48F).copy(alpha = 0.28f * (1f - tau) * fade), (8f + 18f * tau) * u, Offset(fx + side * (20f + 55f * tau) * u, baseY - 8f * u - tau * 14f * u))
    }
    // single grains
    for (i in 0 until 170) {
        val st = 0.04f + 0.7f * rn(i, 1)
        val tau = (t - st) / 0.2f
        if (tau <= 0f || tau >= 1f) continue
        val x = fx + (rn(i, 2) - 0.5f) * (22f + 56f * rise) * u
        val y0 = -6f * u
        val y1 = surface(x)
        drawCircle(lerp(Color(0xFFE8CB82), Color(0xFFB89048), rn(i, 3)).copy(alpha = fade), (1.6f + 1.8f * rn(i, 4)) * u, Offset(x, y0 + (y1 - y0) * tau * tau))
    }
    if (rise > 0.02f) {
        val p = Path()
        p.moveTo(fx - 70f * u, baseY)
        val n = 24
        for (k in 0..n) { val x = fx - 70f * u + 140f * u * k / n; p.lineTo(x, surface(x)) }
        p.lineTo(fx + 70f * u, baseY); p.close()
        drawPath(p, Brush.verticalGradient(listOf(Color(0xFFEBD28E).copy(alpha = fade), Color(0xFFB08A45).copy(alpha = fade)), baseY - hMax * rise, baseY))
        for (i in 0 until 40) {
            val x = fx + (rn(i, 7) - 0.5f) * 110f * u
            val y = surface(x) + (baseY - surface(x)) * rn(i, 8) * 0.8f
            if (y < baseY) drawCircle(Color(0xFF9A7634).copy(alpha = 0.45f * fade), 1.4f * u, Offset(x, y))
        }
    }
    smoke(t, 0.68f, 0f, Color(0xFFBDB7A8))
}

private fun DrawScope.gust(x0: Float, y: Float, len: Float, amp: Float, thick: Float, alpha: Float, curl: Float) {
    val n = 18
    var px = x0; var py = y
    for (k in 1..n) {
        val s = k.toFloat() / n
        val x = x0 + len * s
        val yy = y + sin(s * PI.toFloat() * 1.2f) * amp * -1f + curl * s * s * amp * 1.4f
        val w = sin(s * PI.toFloat()).coerceAtLeast(0.05f) * thick
        drawLine(Color.White.copy(alpha = alpha * sin(s * PI.toFloat())), Offset(px, py), Offset(x, yy), strokeWidth = w, cap = StrokeCap.Round)
        px = x; py = yy
    }
}

private fun DrawScope.windFx(t: Float) {
    val u = density; val fx = size.width / 2; val fy = size.height / 2
    val on = sm(t / 0.12f) * (1f - sm((t - 0.8f) / 0.16f))
    // long soft gusts blowing across the flame from the left
    for (i in 0 until 9) {
        val ph = ((t * 1.35f + rn(i, 1)) % 1f)
        val y = fy - 70f * u + (i / 8f) * 130f * u + (rn(i, 2) - 0.5f) * 14f * u
        val len = (110f + 80f * rn(i, 3)) * u
        val x0 = -len + ph * (size.width + 2f * len)
        gust(x0, y, len, (8f + 12f * rn(i, 4)) * u, (2.2f + 2.4f * rn(i, 5)) * u, 0.55f * on, if (i % 3 == 0) 1f else 0f)
    }
    // embers and sparks torn off the flame, with little trails
    if (t in 0.22f..0.82f) for (i in 0 until 14) {
        val tau = (((t - 0.22f) * 1.8f + rn(i, 6)) % 1f)
        val x = fx + 6f * u + tau * 170f * u
        val y = fy - 30f * u - rn(i, 7) * 60f * u + sin(tau * 7f + i) * 8f * u - tau * 16f * u
        val a = (1f - tau) * 0.95f
        drawLine(Color(0xFFFF8A2B).copy(alpha = a * 0.5f), Offset(x - 14f * u, y + 1.5f * u), Offset(x, y), strokeWidth = 1.4f * u, cap = StrokeCap.Round)
        drawCircle(Color(0xFFFFC463).copy(alpha = a), (1.6f + 1.6f * rn(i, 8)) * u, Offset(x, y))
    }
    smoke(t, 0.6f, 1.2f, Color(0xFFB5BCC8))
}

private fun DrawScope.rainFx(t: Float) {
    val u = density; val fx = size.width / 2; val fy = size.height / 2
    val enter = sm(t / 0.2f); val leave = sm((t - 0.88f) / 0.12f)
    val cy = fy - 120f * u - (1f - enter) * 90f * u - leave * 60f * u
    val ca = 1f - leave
    val cloud = Color(0xFF6B7482).copy(alpha = ca); val cloudDark = Color(0xFF4A5260).copy(alpha = ca)
    val sway = sin(t * 9f) * 4f * u
    drawCircle(cloudDark, 30f * u, Offset(fx - 36f * u + sway, cy + 8f * u))
    drawCircle(cloudDark, 34f * u, Offset(fx + 36f * u + sway, cy + 8f * u))
    drawCircle(cloud, 38f * u, Offset(fx + sway, cy - 4f * u))
    drawCircle(cloud, 28f * u, Offset(fx - 24f * u + sway, cy - 2f * u))
    drawCircle(cloud, 28f * u, Offset(fx + 26f * u + sway, cy))
    if (t in 0.18f..0.88f) {
        val yEnd = fy + 62f * u
        for (i in 0 until 64) {
            val ph = ((t * 3.4f + rn(i, 1)) % 1f)
            val x = fx + (rn(i, 2) - 0.5f) * 150f * u
            val y0 = cy + 26f * u
            val y = y0 + ph * (yEnd - y0)
            drawLine(Color(0xFF8FD0FF).copy(alpha = 0.8f), Offset(x, y), Offset(x - 3f * u, y + 13f * u), strokeWidth = 2f * u, cap = StrokeCap.Round)
            if (ph > 0.92f && abs(x - fx) < 50f * u) drawCircle(Color(0xFFBDE6FF).copy(alpha = 0.6f), 4f * u, Offset(x, yEnd), style = Stroke(1.4f * u))
        }
    }
    if (t in 0.2f..0.9f) for (i in 0 until 14) {
        val ph = ((t * 2.6f + rn(i, 11)) % 1f)
        val x = fx + (rn(i, 12) - 0.5f) * 120f * u
        drawOval(Color(0xFFBDE6FF).copy(alpha = 0.55f * (1f - ph)), Offset(x - (3f + 9f * ph) * u, fy + 62f * u - 2f * u), Size((6f + 18f * ph) * u, (2f + 5f * ph) * u), style = Stroke(1.3f * u))
    }
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.18f * ca), Color.Transparent), Offset(fx + sway, cy - 12f * u), 44f * u), 44f * u, Offset(fx + sway, cy - 12f * u))
    for (i in 0 until 6) {
        val tau = ((t - 0.45f - i * 0.05f) / 0.4f)
        if (tau <= 0f || tau >= 1f) continue
        drawCircle(Color.White.copy(alpha = 0.26f * (1f - tau)), (12f + tau * 24f) * u, Offset(fx + sin(tau * 5f + i) * 16f * u, fy - 30f * u - tau * 85f * u))
    }
    smoke(t, 0.72f, 0f, Color(0xFFAEB6C3))
}

/** kind: 0 water, 1 sand, 2 wind, 3 rain */
@Composable
fun StreakEnded(streak: Int, kind: Int, onLog: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val tier = flameTier(streak)
    val scrim = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val ext = remember { Animatable(0f) }
    val grey = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    val out = remember { Animatable(1f) }
    val len = streak.toString().length
    val a = streak.toString().padStart(len, ' ')
    val b = "0".padStart(len, ' ')
    val digits = remember { List(len) { Animatable(0f) } }
    var closing by remember { mutableStateOf(false) }
    val red = Color(0xFFFF7A7A)

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { scrim.animateTo(1f, tween(450)) }
            launch { delay(250); title.animateTo(1f, tween(650)) }
        }
        delay(1700)
        playOnce(ctx, when (kind) { 0 -> R.raw.mw_water; 1 -> R.raw.mw_sand; 2 -> R.raw.mw_wind; else -> R.raw.mw_rain })
        ext.animateTo(1f, tween(3000, easing = androidx.compose.animation.core.LinearEasing))
        delay(150)
        // the number counts down to zero, left digit first
        launch { grey.animateTo(1f, tween(900)) }
        for (i in 0 until len) {
            if (a[i] != b[i]) {
                playOnce(ctx, R.raw.mw_tick)
                digits[i].animateTo(1f, tween(800, easing = FastOutSlowInEasing))
                delay(200)
            }
        }
        delay(250)
        playOnce(ctx, R.raw.mw_pop)
        pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.42f, stiffness = 380f))
    }
    LaunchedEffect(closing) { if (closing) { out.animateTo(0f, tween(300)); onDone() } }

    Box(
        Modifier.fillMaxSize().graphicsLayer { alpha = out.value }
            .background(MW.Bg.copy(alpha = 0.93f * scrim.value))
            .clickable(remember { MutableInteractionSource() }, null) { if (pop.value > 0.6f) closing = true },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
            Text(
                "STREAK ENDED", Modifier.graphicsLayer { alpha = title.value; translationY = (1f - title.value) * 24f },
                style = MwTs(color = red, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "A day was missed and there was no free day left", Modifier.graphicsLayer { alpha = title.value },
                style = MwTs(color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
            )
            Spacer(Modifier.height(10.dp))
            Box(Modifier.size(280.dp, 270.dp), contentAlignment = Alignment.Center) {
                // the warm glow, then the flame itself
                Canvas(Modifier.fillMaxSize()) {
                    val lit = 1f - litOut(kind, ext.value)
                    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFA63D).copy(alpha = 0.42f * lit), Color.Transparent), radius = size.width * 0.38f), radius = size.width * 0.38f)
                }
                FlameIcon(0, 170.dp, Modifier.graphicsLayer { alpha = sm((ext.value - 0.72f) / 0.28f) * 0.9f }, locked = true)
                FlameIcon(tier, 170.dp, Modifier.graphicsLayer {
                    val t = ext.value
                    val lit = 1f - litOut(kind, t)
                    val flick = if (t > 0.18f) 1f - 0.2f * abs(sin(t * 55f)) else 1f
                    alpha = lit * flick
                    transformOrigin = TransformOrigin(0.5f, 0.9f)
                    if (kind == 2) {
                        val gust = sm(t / 0.5f)
                        rotationZ = 13f * gust + sin(t * 46f) * 5f * gust
                        scaleX = 1f - 0.16f * gust
                        scaleY = 1f - 0.22f * sm((t - 0.2f) / 0.4f) + 0.05f * sin(t * 33f) * gust
                    } else if (kind == 0) {
                        scaleY = 1f - 0.5f * sm((t - 0.33f) / 0.2f); scaleX = 1f + 0.18f * sm((t - 0.33f) / 0.2f)
                    } else scaleY = 1f - 0.4f * sm((t - 0.3f) / 0.5f)
                })
                Canvas(Modifier.fillMaxSize()) {
                    val t = ext.value
                    if (t > 0f && t < 1f) when (kind) { 0 -> waterFx(t); 1 -> sandFx(t); 2 -> windFx(t); else -> rainFx(t) }
                }
            }
            Row(horizontalArrangement = Arrangement.Center) {
                val tint = lerp(Color(0xFFFFE9B0), Color.White.copy(alpha = 0.6f), grey.value)
                for (i in 0 until len) RollDigit(a[i], b[i], digits[i].value, tint)
            }
            Text("day streak", style = MwTs(color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp))
            Spacer(Modifier.height(22.dp))
            Column(
                Modifier.graphicsLayer { val s = pop.value; scaleX = s; scaleY = s; alpha = s.coerceIn(0f, 1f) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.clip(MwShape(99.dp)).background(Brush.linearGradient(listOf(MW.Mint, MW.Accent)))
                        .clickable(remember { MutableInteractionSource() }, null) { if (pop.value > 0.6f) onLog() }.padding(horizontal = 32.dp, vertical = 15.dp),
                ) { Text("Start a new streak", style = MwTs(color = MW.OnAccent, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)) }
                Spacer(Modifier.height(6.dp))
                Text("Later", Modifier.clickable(remember { MutableInteractionSource() }, null) { if (pop.value > 0.6f) closing = true }.padding(12.dp), style = MwTs(color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

/** how far the fire is out (0 burning .. 1 out) for each way of putting it out */
private fun litOut(kind: Int, t: Float): Float = when (kind) {
    0 -> sm((t - 0.36f) / 0.2f)
    1 -> sm((t - 0.5f) / 0.25f)
    2 -> sm((t - 0.52f) / 0.15f)
    else -> sm((t - 0.32f) / 0.45f)
}
