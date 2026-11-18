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
import kotlin.math.PI
import kotlin.math.sin

private fun playOnce(ctx: Context, res: Int) {
    try {
        if (!ctx.getSharedPreferences("mw_prefs", 0).getBoolean("sfx", true)) return
        MediaPlayer.create(ctx, res)?.apply { setOnCompletionListener { it.release() }; start() }
    } catch (e: Exception) { }
}

@Composable
private fun RollDigit(old: Char, new: Char, p: Float) {
    val style = numStyle(64f, Color(0xFFFFE9B0))
    Box(Modifier.width(46.dp).height(76.dp).clip(RectangleShape), contentAlignment = Alignment.Center) {
        if (old == new || p >= 1f) {
            Text(new.toString(), style = style)
        } else {
            // the old digit rolls down and out, the new one drops in from above
            if (old != ' ') Text(old.toString(), Modifier.graphicsLayer { translationY = size.height * 1.1f * p; alpha = 1f - p }, style = style)
            Text(new.toString(), Modifier.graphicsLayer { translationY = -size.height * 1.1f * (1f - p); alpha = p }, style = style)
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
            .background(Color(0xFF03050A).copy(alpha = 0.9f * scrim.value))
            .clickable(remember { MutableInteractionSource() }, null) { closing = true },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "NEW DAY", Modifier.graphicsLayer { alpha = title.value; translationY = (1f - title.value) * 24f },
                style = TextStyle(color = Color(0xFFFFD27A), fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Your streak is back on", Modifier.graphicsLayer { alpha = title.value },
                style = TextStyle(color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
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
            Text("day streak", style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp))
        }
    }
}
