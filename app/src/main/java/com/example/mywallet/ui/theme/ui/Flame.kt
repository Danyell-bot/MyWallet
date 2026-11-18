package com.example.mywallet.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import kotlin.random.Random
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val FLAME_NAMES = listOf("Cold", "Spark", "Flame", "Blaze", "Blue flame", "Violet flame", "Rose flame", "Legend", "Solar flame", "Emerald flame", "Sapphire flame", "Crimson crown", "Aurora flame", "Void flame", "Gilded flame", "Halo flame", "Eternal flame")

private const val BODY = "M12.4 1.6c.6 3.4-1.1 4.9-2.7 6.9C8 10.6 6.2 12.8 6.2 15.7a5.9 5.9 0 0 0 5.8 5.9 5.9 5.9 0 0 0 5.8-6.1c0-2.5-1.2-4.4-2.3-5.7-.3 1.4-1 2.2-1.9 2.6.8-3.4-.1-7.2-1.2-10.8z"
private const val CORE = "M12 21.6a3.3 3.3 0 0 1-3.3-3.4c0-1.6.9-2.5 1.7-3.5.4-.6.8-1.2.9-2.1 1.5 1.4 4 3.1 4 5.6a3.3 3.3 0 0 1-3.3 3.4z"

/**
 * Every tier is a bigger show than the one before it:
 * 1 spark -> 2 layered flame + embers -> 3 triple blaze -> 4 blue storm (lightning) -> 5 arcane atom (comets + rune ring)
 * -> 6 rose bloom (spinning petals) -> 7 legend (rainbow halo + comets + starbursts) -> 8 solar (twin ray crowns + shock flares)
 * -> 9 emerald vault (orbiting gems + ticked rings) -> 10 sapphire ice storm (crystal rays + comets + lightning)
 * -> 11 crimson crown (double wings + fire halo) -> 12 eternal (everything, prism colours).
 */
private class Spec(
    val top: Long, val bottom: Long, val core: Long, val glow: Long,
    val layers: Int = 1, val glowAmp: Float = 0.4f, val flick: Float = 0.05f, val shimmer: Float = 0f,
    val embers: Int = 0, val comets: Int = 0, val atom: Boolean = false, val stars: Int = 0, val gems: Int = 0,
    val rings: Int = 0, val ticks: Boolean = false, val ripples: Int = 0, val rays: Int = 0, val rays2: Int = 0,
    val ice: Boolean = false, val petals: Int = 0, val lightning: Int = 0, val sweep: Int = 0,
    val tongues: Boolean = false, val wings: Int = 0, val crown: Boolean = false, val laurel: Boolean = false,
    val rainbow: Boolean = false, val prism: Boolean = false, val aurora: Boolean = false, val halo: Boolean = false, val scale: Float = 1f,
)

private val SPECS = listOf(
    Spec(0xFFB9C2C3, 0xFF8B9899, 0xFFDDE3E4, 0xFF9AA6A7, glowAmp = 0f, flick = 0f),
    Spec(0xFFFFAA3D, 0xFFFF6A1F, 0xFFFFE08A, 0xFFFF7A2A, glowAmp = 0.35f, flick = 0.055f, embers = 3),
    Spec(0xFFFF9A2A, 0xFFE8190F, 0xFFFFE08A, 0xFFFF4A1F, layers = 2, glowAmp = 0.55f, flick = 0.08f, embers = 5),
    Spec(0xFFFFE04A, 0xFFFF6A00, 0xFFFFF8C8, 0xFFFFA000, layers = 3, glowAmp = 0.75f, flick = 0.085f, embers = 6, tongues = true),
    Spec(0xFF8FE4FF, 0xFF1458FF, 0xFFFFFFFF, 0xFF2F7BFF, layers = 3, glowAmp = 0.95f, flick = 0.06f, shimmer = 3.5f, lightning = 3, ripples = 1),
    Spec(0xFFD9A8FF, 0xFF5A1FD0, 0xFFFFFFFF, 0xFF8A4DFF, layers = 3, glowAmp = 0.85f, flick = 0.055f, comets = 2, atom = true),
    Spec(0xFFFFA8E6, 0xFFB5127F, 0xFFFFF0FA, 0xFFFF3DB0, layers = 3, glowAmp = 0.85f, flick = 0.06f, petals = 8, embers = 5),
    Spec(0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, layers = 3, glowAmp = 0.95f, flick = 0.065f, sweep = 1, rainbow = true),
    Spec(0xFFFFF6B0, 0xFFFF9A00, 0xFFFFFFFF, 0xFFFFC21F, layers = 3, glowAmp = 1f, flick = 0.06f, rays = 12, embers = 4),
    Spec(0xFF9CFFD0, 0xFF008F55, 0xFFF0FFF6, 0xFF18E08F, layers = 3, glowAmp = 0.9f, flick = 0.06f, gems = 4),
    Spec(0xFFA8C8FF, 0xFF1B38D6, 0xFFFFFFFF, 0xFF4F8CFF, layers = 3, glowAmp = 0.95f, flick = 0.055f, ice = true, stars = 2),
    Spec(0xFFFF7A5A, 0xFF7A0620, 0xFFFFE4D0, 0xFFFF2A3F, layers = 3, glowAmp = 0.95f, flick = 0.07f, wings = 1, crown = true, embers = 5),
    Spec(0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF4FD0C0, layers = 3, glowAmp = 0.95f, flick = 0.06f, sweep = 1, aurora = true),
    Spec(0xFF9A7BFF, 0xFF12002E, 0xFFF3EEFF, 0xFF7A3DFF, layers = 3, glowAmp = 0.9f, flick = 0.055f, stars = 3, ripples = 2),
    Spec(0xFFFFF0B0, 0xFFD98A00, 0xFFFFFFFF, 0xFFFFC21F, layers = 3, glowAmp = 0.95f, flick = 0.06f, laurel = true, stars = 2),
    Spec(0xFFFFFFFF, 0xFFBFD8FF, 0xFFFFFFFF, 0xFFFFF2C0, layers = 3, glowAmp = 0.95f, flick = 0.055f, halo = true, embers = 3),
    Spec(0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFB8A6FF, layers = 3, glowAmp = 1f, flick = 0.06f, wings = 2, crown = true, sweep = 1, prism = true),
)


private fun frac(x: Float) = x - kotlin.math.floor(x)
private fun wave(ph: Float, cycles: Int, off: Float = 0f) = sin(((ph * cycles) + off) * 2f * PI.toFloat())

private fun starPath(r: Float): Path = Path().apply {
    moveTo(0f, -r); lineTo(r * .2f, -r * .2f); lineTo(r, 0f); lineTo(r * .2f, r * .2f)
    lineTo(0f, r); lineTo(-r * .2f, r * .2f); lineTo(-r, 0f); lineTo(-r * .2f, -r * .2f); close()
}

private fun diamondPath(w: Float, h: Float): Path = Path().apply {
    moveTo(0f, -h); lineTo(w, 0f); lineTo(0f, h); lineTo(-w, 0f); close()
}

private fun wingPath(): Path = Path().apply {
    moveTo(20.5f, 23f)
    cubicTo(25f, 24f, 31f, 22f, 34.5f, 15f)
    cubicTo(32f, 16.5f, 30f, 16.5f, 28f, 16f)
    cubicTo(31f, 14.5f, 32.5f, 12.5f, 33f, 10f)
    cubicTo(30f, 12f, 27.5f, 12.5f, 25.5f, 12f)
    cubicTo(27f, 10f, 27.5f, 8.5f, 27f, 6.5f)
    cubicTo(24.5f, 9f, 22.5f, 12f, 21f, 17f)
    close()
}

private fun crownPath(): Path = Path().apply {
    moveTo(14.4f, 6.4f); lineTo(13.8f, 2.4f); lineTo(16.2f, 4.5f); lineTo(18f, 1.6f)
    lineTo(19.8f, 4.5f); lineTo(22.2f, 2.4f); lineTo(21.6f, 6.4f); close()
}

@Composable
fun FlameIcon(tier: Int, size: Dp, modifier: Modifier = Modifier, locked: Boolean = false) {
    val tr = tier.coerceIn(0, SPECS.lastIndex)
    val sp = SPECS[tr]
    val animated = !locked && tr > 0
    val body = remember { PathParser().parsePathString(BODY).toPath() }
    val core = remember { PathParser().parsePathString(CORE).toPath() }
    val wing = remember { wingPath() }
    val crown = remember { crownPath() }
    val ph by (if (animated) rememberInfiniteTransition(label = "flame").animateFloat(0f, 1f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart), label = "ph")
    else remember { androidx.compose.runtime.mutableStateOf(0f) })

    val dark = if (locked) Modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
        drawContent()
        drawRect(Color(0xFF12171D).copy(alpha = 0.9f), blendMode = BlendMode.SrcAtop)
    } else Modifier
    Canvas(modifier.size(size).then(dark)) {
        val u = this.size.width / 36f
        scale(u, u, pivot = Offset.Zero) { drawFlame(sp, ph, animated, body, core, wing, crown) }
    }
}

private fun DrawScope.comet(cx: Float, cy: Float, rx: Float, ry: Float, tilt: Float, a0: Float, c: Color) {
    val ct = cos(tilt); val st = sin(tilt)
    for (j in 11 downTo 0) {
        val a = a0 - j * 0.13f
        val x = rx * cos(a); val y = ry * sin(a)
        val px = cx + x * ct - y * st; val py = cy + x * st + y * ct
        val f = 1f - j / 12f
        drawCircle(c.copy(alpha = f * 0.85f), radius = 0.25f + 1.15f * f, center = Offset(px, py))
        if (j == 0) drawCircle(Color.White, radius = 0.7f, center = Offset(px, py))
    }
}

private fun DrawScope.drawFlame(sp: Spec, ph: Float, animated: Boolean, body: Path, core: Path, wing: Path, crown: Path) {
    val cx = 18f
    val cy = 20f
    val baseY = 27.6f
    val hue = when {
        sp.rainbow -> (ph * 360f) % 360f
        sp.prism -> 245f + 60f * wave(ph, 2)
        sp.aurora -> 230f + 70f * wave(ph, 2)
        else -> 0f
    }
    val cyc = sp.prism || sp.aurora
    val top = if (sp.rainbow) Color.hsv(hue, 0.5f, 1f) else if (cyc) Color.hsv(hue, 0.3f, 1f) else Color(sp.top)
    val bot = if (sp.rainbow) Color.hsv((hue + 45f) % 360f, 0.95f, 0.98f) else if (cyc) Color.hsv((hue + 55f) % 360f, 0.75f, 1f) else Color(sp.bottom)
    val glow = if (sp.rainbow) Color.hsv(hue, 0.85f, 1f) else if (cyc) Color.hsv(hue, 0.6f, 1f) else Color(sp.glow)
    val coreC = Color(sp.core)
    val alt = if (sp.rainbow) Color.hsv((hue + 120f) % 360f, 0.8f, 1f) else if (cyc) Color.hsv((hue + 90f) % 360f, 0.6f, 1f) else lerp(Color(sp.top), Color.White, 0.4f)
    val w = { c: Int, o: Float -> if (animated) wave(ph, c, o) else 0f }

    // big bloom behind everything (adds light)
    if (sp.glowAmp > 0f) {
        val a = sp.glowAmp * (0.72f + 0.28f * w(4, 0f))
        val r = 17.5f * sp.scale
        drawCircle(Brush.radialGradient(0f to glow.copy(alpha = (a * 0.8f).coerceAtMost(0.95f)), 0.45f to glow.copy(alpha = a * 0.32f), 1f to Color.Transparent, center = Offset(cx, cy), radius = r), radius = r, center = Offset(cx, cy), blendMode = BlendMode.Plus)
    }
    // rotating halo ring(s) of colour
    for (k in 0 until sp.sweep) {
        val dir = if (k == 0) 1f else -1f
        rotate(dir * ph * 360f * 2f, pivot = Offset(cx, cy)) {
            val c2 = if (k == 0) glow else alt
            drawCircle(
                Brush.sweepGradient(0f to c2, 0.35f to c2.copy(alpha = 0.05f), 0.6f to c2.copy(alpha = 0.0f), 1f to c2, center = Offset(cx, cy)),
                radius = 15.6f - k * 2.4f, center = Offset(cx, cy), style = Stroke(1.5f, cap = StrokeCap.Round),
            )
        }
    }
    // spinning rose petals
    if (sp.petals > 0) {
        rotate(ph * 360f, pivot = Offset(cx, cy)) {
            for (i in 0 until sp.petals) {
                rotate(i * 360f / sp.petals, pivot = Offset(cx, cy)) {
                    val pulse = 1f + 0.12f * w(4, i * 0.125f)
                    scale(1f, pulse, pivot = Offset(cx, cy)) {
                        drawOval(Brush.verticalGradient(listOf(glow.copy(alpha = 0.7f), glow.copy(alpha = 0.05f)), startY = cy - 14f, endY = cy - 4f), Offset(cx - 2.8f, cy - 14f), Size(5.6f, 10f))
                    }
                }
            }
        }
    }
    // ray crowns
    if (sp.rays > 0) {
        rotate(ph * 360f * 2f, pivot = Offset(cx, cy)) {
            for (i in 0 until sp.rays) rotate(i * 360f / sp.rays, pivot = Offset(cx, cy)) {
                val len = if (i % 2 == 0) 17.5f else 14f
                val p = Path().apply { moveTo(cx - 0.7f, cy - 7f); lineTo(cx, cy - len); lineTo(cx + 0.7f, cy - 7f); close() }
                drawPath(p, glow.copy(alpha = if (i % 2 == 0) 0.85f else 0.5f), blendMode = BlendMode.Plus)
            }
        }
    }
    if (sp.rays2 > 0) {
        rotate(-ph * 360f * 3f, pivot = Offset(cx, cy)) {
            for (i in 0 until sp.rays2) rotate(i * 360f / sp.rays2 + 9f, pivot = Offset(cx, cy)) {
                val p = Path().apply { moveTo(cx - 0.45f, cy - 6f); lineTo(cx, cy - 12.5f); lineTo(cx + 0.45f, cy - 6f); close() }
                drawPath(p, alt.copy(alpha = 0.7f), blendMode = BlendMode.Plus)
            }
        }
    }
    // ice crystals
    if (sp.ice) {
        rotate(ph * 360f, pivot = Offset(cx, cy)) {
            for (i in 0 until 6) rotate(i * 60f, pivot = Offset(cx, cy)) {
                translate(cx, cy - 11.5f) { drawPath(diamondPath(1.15f, 5.6f), Color(0xFFD6E6FF).copy(alpha = 0.85f), blendMode = BlendMode.Plus) }
                rotate(30f, pivot = Offset(cx, cy)) { translate(cx, cy - 9.5f) { drawPath(diamondPath(0.7f, 3.4f), glow.copy(alpha = 0.8f), blendMode = BlendMode.Plus) } }
            }
        }
    }
    // shock ripples
    for (k in 0 until sp.ripples) {
        val p = frac(ph * 3f + k / sp.ripples.toFloat())
        drawCircle(glow.copy(alpha = (1f - p) * 0.7f), radius = 5f + p * 13f, center = Offset(cx, cy), style = Stroke(1.1f * (1f - p) + 0.3f))
    }
    // dashed / ticked rings
    for (k in 0 until sp.rings) {
        val dir = if (k % 2 == 0) 1f else -1f
        val rad = 15.2f - k * 2.8f
        rotate(dir * ph * 360f * (3 + k), pivot = Offset(cx, cy)) {
            if (sp.ticks) {
                val n = 28 - k * 6
                for (i in 0 until n) rotate(i * 360f / n, pivot = Offset(cx, cy)) {
                    val big = i % 4 == 0
                    drawLine(glow.copy(alpha = if (big) 0.95f else 0.5f), Offset(cx, cy - rad), Offset(cx, cy - rad + if (big) 1.8f else 1f), strokeWidth = if (big) 0.55f else 0.4f, cap = StrokeCap.Round)
                }
            }
            drawCircle(glow.copy(alpha = 0.8f), radius = rad - 1.2f, center = Offset(cx, cy), style = Stroke(0.65f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.6f - k * 0.5f, 2.2f + k * 1.3f))))
        }
    }
    // wings (back pair bigger and slower)
    if (sp.wings > 0) {
        val flap = 1f + 0.09f * w(4, 0f)
        val rot = 5f * w(4, 0f)
        for (layer in 0 until sp.wings) {
            val back = layer == 0 && sp.wings > 1
            val k = if (back) 1.14f else 1f
            val r0 = if (back) rot * 0.6f - 8f else rot
            for (side in 0..1) {
                scale(if (side == 0) 1f else -1f, 1f, pivot = Offset(cx, 0f)) {
                    rotate(-r0, pivot = Offset(20.5f, 23f)) {
                        scale(flap * k, flap * k, pivot = Offset(20.5f, 23f)) {
                            val c1 = if (sp.prism) Color.hsv((hue + 20f * layer) % 360f, 0.45f, 1f) else Color(0xFFFFD9A0)
                            val c2 = if (back) alt.copy(alpha = 0.55f) else glow.copy(alpha = 0.9f)
                            drawPath(wing, Brush.linearGradient(listOf(c1, c2), start = Offset(21f, 22f), end = Offset(34f, 6f)), blendMode = BlendMode.Plus)
                            drawPath(wing, Color.White.copy(alpha = 0.4f), style = Stroke(0.35f))
                        }
                    }
                }
            }
        }
    }
    if (sp.laurel) {
        val sway = 1f + 0.04f * w(2, 0f)
        for (side in 0..1) scale(if (side == 0) 1f else -1f, 1f, pivot = Offset(cx, 0f)) {
            for (i in 0..6) {
                val a = (100f + i * 11f) * PI.toFloat() / 180f
                val x = cx + 12.4f * cos(a) * sway
                val y = 21f + 12.4f * sin(a)
                rotate(i * 11f + 190f, pivot = Offset(x, y)) { drawOval(Color(0xFFF2CE62), Offset(x - 0.95f, y - 2f), Size(1.9f, 4f)) }
            }
        }
    }
    // side flames
    if (sp.tongues) {
        for (side in 0..1) {
            val off = if (side == 0) -7f else 7f
            val ww = w(8, 0.35f * (side + 1))
            translate(6f + off, 6f) {
                scale(0.46f, 0.46f * (1f + 0.14f * ww), pivot = Offset(12f, 21.6f)) {
                    drawPath(body, Brush.verticalGradient(listOf(top, bot), startY = 1.6f, endY = 21.6f))
                }
            }
        }
    }
    // the main flame, 1-3 stacked tongues (outer -> hot core)
    fun layer(k: Float, t: Color, b: Color, off: Float) {
        val sy = 1f + sp.flick * (if (animated) wave(ph, 8, off) else 0f)
        val sx = 1f - sp.flick * 0.45f * w(8, off + 0.25f)
        val skew = sp.shimmer * w(6, off + 0.1f) + 1.5f * w(8, off + 0.5f)
        rotate(skew, pivot = Offset(cx, baseY)) {
            scale(k * sx * sp.scale, k * sy * sp.scale, pivot = Offset(cx, baseY)) {
                translate(6f, 6f) { drawPath(body, Brush.verticalGradient(listOf(t, b), startY = 1.6f, endY = 21.6f)) }
            }
        }
    }
    layer(1f, top, bot, 0f)
    if (sp.layers >= 2) layer(0.74f, lerp(top, Color.White, 0.35f), lerp(bot, top, 0.6f), 0.3f)
    if (sp.layers >= 3) layer(0.48f, Color.White.copy(alpha = 0.95f), lerp(coreC, top, 0.3f), 0.6f)
    if (sp.layers == 1) {
        rotate(1.5f * w(8, 0.5f), pivot = Offset(cx, baseY)) {
            scale(1f, 1f + sp.flick * (if (animated) wave(ph, 8) else 0f), pivot = Offset(cx, baseY)) {
                translate(6f, 6f) { drawPath(core, coreC.copy(alpha = 0.92f)) }
            }
        }
    }
    // floating halo
    if (sp.halo) {
        val bob = 0.4f * w(4, 0f)
        translate(0f, bob) {
            drawOval(Color(0xFFFFE9A0).copy(alpha = 0.35f), Offset(cx - 5.4f, 1.2f), Size(10.8f, 4.2f), blendMode = BlendMode.Plus)
            drawOval(Color(0xFFFFF4C8), Offset(cx - 5f, 1.8f), Size(10f, 3f), style = Stroke(1.1f))
        }
    }
    // crown
    if (sp.crown) {
        translate(0f, 0.4f * w(4, 0f)) {
            drawCircle(Color(0xFFFFD35A).copy(alpha = 0.5f), radius = 4.5f, center = Offset(cx, 4.2f), blendMode = BlendMode.Plus)
            drawPath(crown, Brush.verticalGradient(listOf(Color(0xFFFFF4C0), Color(0xFFE0A526)), startY = 1.6f, endY = 6.4f))
            drawCircle(Color.White.copy(alpha = 0.75f + 0.25f * w(6, 0f)), radius = 0.8f, center = Offset(cx, 4.6f))
        }
    }
    // lightning
    if (sp.lightning > 0 && animated) {
        val rnd = Random((ph * 90f).toInt())
        for (b in 0 until sp.lightning) {
            if (rnd.nextFloat() < 0.35f) continue
            val ang = (rnd.nextFloat() * 360f) * PI.toFloat() / 180f
            val pth = Path()
            var px = cx + cos(ang) * 8f; var py = cy + sin(ang) * 8f
            pth.moveTo(px, py)
            for (sg in 1..5) {
                val r = 8f + sg * 1.9f
                val jit = (rnd.nextFloat() - 0.5f) * 3.2f
                px = cx + cos(ang) * r - sin(ang) * jit; py = cy + sin(ang) * r + cos(ang) * jit
                pth.lineTo(px, py)
            }
            drawPath(pth, glow.copy(alpha = 0.5f), style = Stroke(1.6f, cap = StrokeCap.Round), blendMode = BlendMode.Plus)
            drawPath(pth, Color.White.copy(alpha = 0.95f), style = Stroke(0.5f, cap = StrokeCap.Round))
        }
    }
    // orbiting gems
    for (i in 0 until sp.gems) {
        val a = (ph * 3f + i / sp.gems.toFloat()) * 2f * PI.toFloat()
        val x = cx + 14f * cos(a); val y = cy + 11.5f * sin(a)
        translate(x, y) {
            rotate(ph * 720f + i * 40f, pivot = Offset.Zero) {
                drawPath(diamondPath(1.2f, 1.9f), (if (sp.prism) alt else top).copy(alpha = 0.95f))
                drawPath(diamondPath(0.5f, 0.9f), Color.White.copy(alpha = 0.85f))
            }
        }
    }
    // comets
    for (i in 0 until sp.comets) {
        val a0 = (ph * 3f * (if (sp.atom) 1f else 1.6f) + i / sp.comets.toFloat()) * 2f * PI.toFloat()
        val c = if (i % 2 == 0) glow else alt
        if (sp.atom) comet(cx, cy, 14f, 5.2f, (i * 60f - 30f) * PI.toFloat() / 180f, a0, c)
        else comet(cx, cy, 14.2f, 11.5f, (i * 40f) * PI.toFloat() / 180f, a0, c)
    }
    // embers
    for (i in 0 until sp.embers) {
        val p = frac(ph * 4f + i / sp.embers.toFloat())
        val x = cx + sin(i * 2.4f + p * 3f) * (3.2f + p * 2f)
        val y = 26f - p * 20f
        drawCircle(coreC.copy(alpha = (1f - p) * 0.95f), radius = 0.5f + 0.5f * (1f - p), center = Offset(x, y), blendMode = BlendMode.Plus)
    }
    // starbursts
    val spots = listOf(Offset(5.5f, 8f), Offset(30.5f, 9.5f), Offset(6.5f, 30f), Offset(30f, 29f))
    for (i in 0 until sp.stars) {
        val tw = (0.5f + 0.5f * w(5, i * 0.23f)).coerceIn(0f, 1f)
        translate(spots[i % 4].x, spots[i % 4].y) {
            rotate(ph * 360f * 2f, pivot = Offset.Zero) { drawPath(starPath(2.6f * tw + 0.4f), Color.White.copy(alpha = 0.35f + 0.6f * tw), blendMode = BlendMode.Plus) }
            drawCircle(glow.copy(alpha = 0.35f * tw), radius = 2.6f, blendMode = BlendMode.Plus)
        }
    }
}
