package com.example.mywallet.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private class Blob(val x: Float, val y: Float, val r: Float, val c: Color, val a: Float, val dx: Float, val dy: Float, val ph: Float)

// positions are in a wide 780 x 844 space; the three pages look at different slices of it
private val DefaultBlobs = listOf(
    Blob(90f, 130f, 330f, Color(0xFF0FB98A), 0.50f, 46f, 34f, 0.0f),
    Blob(330f, 420f, 300f, Color(0xFF1E6FD9), 0.34f, 40f, 52f, 1.3f),
    Blob(560f, 150f, 330f, Color(0xFFE8B04A), 0.30f, 50f, 38f, 2.4f),
    Blob(700f, 560f, 320f, Color(0xFF0FB98A), 0.40f, 36f, 46f, 3.6f),
    Blob(180f, 700f, 330f, Color(0xFF5B3FD0), 0.30f, 44f, 36f, 4.7f),
    Blob(470f, 800f, 280f, Color(0xFFE8B04A), 0.20f, 34f, 30f, 5.5f),
)

private fun blobsFor(p: Palette): List<Blob> {
    if (p.id == "default") return DefaultBlobs
    // richer glows: more saturated colours, a little stronger, with the accent peeking through in two places
    val g1 = MwTheme.vivid(p.c1, 1.35f, 1.25f)
    val g3 = MwTheme.vivid(p.c3, 1.1f, 1.0f)
    val dark2 = p.c2.luminance() < 0.1f
    val g2 = if (dark2) MwTheme.vivid(androidx.compose.ui.graphics.lerp(p.c2, p.c1, 0.45f), 1.3f, 1.4f) else MwTheme.vivid(p.c2, 1.2f, 1.0f)
    val soft2 = if (p.c2.luminance() > 0.55f) 0.5f else 1.0f
    return listOf(
        Blob(90f, 130f, 340f, g1, 0.78f, 46f, 34f, 0.0f),
        Blob(330f, 420f, 300f, g2, 0.50f * soft2, 40f, 52f, 1.3f),
        Blob(560f, 150f, 330f, g3, 0.42f, 50f, 38f, 2.4f),
        Blob(700f, 560f, 330f, g1, 0.62f, 36f, 46f, 3.6f),
        Blob(180f, 700f, 340f, g2, 0.42f * soft2, 44f, 36f, 4.7f),
        Blob(470f, 800f, 290f, g3, 0.32f, 34f, 30f, 5.5f),
    )
}

/**
 * Soft aurora: big slow colour glows on a near-black base. `page` is the pager position (0..2);
 * the scene moves half as far as the pages, so swiping drifts through the same space.
 */
/** true while another screen covers this one: the background stops moving (saves battery) */
val LocalScenePaused = androidx.compose.runtime.compositionLocalOf { false }

@Composable
fun SceneBackground(page: Float, modifier: Modifier = Modifier) {
    if (!MwStyle.isClassic) { StyleScene(page, modifier); return }
    val paused = LocalScenePaused.current
    val t by rememberInfiniteTransition(label = "aurora").animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(36000, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    Canvas(modifier.fillMaxSize()) {
        val t = if (paused) 1.2f else t
        drawRect(MW.Bg, topLeft = Offset(-100f, -100f), size = Size(size.width + 200f, size.height + 200f))
        val sc = size.width / 390f
        val h = size.height / sc
        val k = maxOf(1f, h / 844f)
        val shift = page * 195f
        blobsFor(MwTheme.palette).forEach { b ->
            val cx = (b.x + sin(t + b.ph) * b.dx - shift) * sc
            val cy = (b.y * k + cos(t * 1.0f + b.ph * 1.3f) * b.dy) * sc
            glow(cx, cy, b.r * sc * k.coerceAtMost(1.3f), b.c, b.a)
        }
        // a gentle darkening at the bottom keeps the dock and text readable
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, MW.Bg.copy(alpha = 0.55f)), startY = size.height * 0.55f, endY = size.height))
    }
}

private fun DrawScope.glow(cx: Float, cy: Float, r: Float, c: Color, a: Float) {
    drawCircle(
        Brush.radialGradient(
            0f to c.copy(alpha = a), 0.45f to c.copy(alpha = a * 0.55f), 1f to Color.Transparent,
            center = Offset(cx, cy), radius = r,
        ),
        radius = r, center = Offset(cx, cy),
    )
}
