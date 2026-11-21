package com.example.mywallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object MW {
    val Bg: Color get() = MwTheme.bg
    val Accent: Color get() = MwTheme.accent
    val Mint: Color get() = MwTheme.mint
    /** text and icons that sit on an accent-coloured button */
    val OnAccent: Color get() = MwTheme.onAccent
    val Tint: Color get() = MwTheme.tint
    val Rose = Color(0xFFFF9FB0)
    val Amber = Color(0xFFFFCB7A)
    val Sky = Color(0xFF9CC4FF)
    val Violet = Color(0xFFB9A6FF)
    val Text = Color.White
    val Dim = Color.White.copy(alpha = 0.78f)
    val Faint = Color.White.copy(alpha = 0.6f)
    val Font: FontFamily get() = MwStyle.font ?: FontFamily.Default
}

/** The shared blur source: the background scene registers it, every glass panel reads it. */
val LocalHaze = compositionLocalOf<HazeState?> { null }

@Composable
private fun Modifier.frost(): Modifier {
    val h = LocalHaze.current ?: return this
    return this.hazeEffect(h) {
        blurRadius = 24.dp
        noiseFactor = 0f
    }
}

/** Frosted-glass look: soft white gradient, thin bright border. */
@Composable
fun Modifier.glass(shape: Shape = MwShape(24.dp), tint: Color? = null): Modifier {
    if (!MwStyle.isClassic) return themedPanel(shape, tint, false)
    val top = tint?.copy(alpha = 0.42f) ?: Color.White.copy(alpha = 0.14f)
    return this
        .clip(shape)
        .frost()
        .background(MW.Bg.copy(alpha = 0.22f))
        .background(Brush.linearGradient(listOf(top, Color.White.copy(alpha = 0.05f))))
        .border(1.dp, Color.White.copy(alpha = 0.17f), shape)
}

/** Same look without the blur: much cheaper, used for long lists and many small tiles. */
fun Modifier.glassLite(shape: Shape = MwShape(24.dp), tint: Color? = null): Modifier {
    if (!MwStyle.isClassic) return themedPanel(shape, tint, false)
    val top = tint?.copy(alpha = 0.42f) ?: Color.White.copy(alpha = 0.14f)
    return this
        .clip(shape)
        .background(Color(0xFF0A0F1E).copy(alpha = 0.55f))
        .background(Brush.linearGradient(listOf(top, Color.White.copy(alpha = 0.05f))))
        .border(1.dp, Color.White.copy(alpha = 0.17f), shape)
}

/** Darker glass for the tab bar and the dock so they stay readable over the background. */
@Composable
fun Modifier.glassDark(shape: Shape): Modifier = if (!MwStyle.isClassic) themedPanel(shape, null, true) else this
    .clip(shape)
    .frost()
    .background(Brush.linearGradient(listOf(Color(0xFF080C18).copy(alpha = 0.66f), Color(0xFF080C18).copy(alpha = 0.44f))))
    .border(1.dp, Color.White.copy(alpha = 0.2f), shape)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    tint: Color? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.glass(MwShape(radius), tint).padding(padding), content = content)
}

@Composable
fun Lbl(text: String, modifier: Modifier = Modifier, color: Color = MW.Dim) {
    Text(
        (if (MwStyle.cur.id == "cat") "\uD83D\uDC3E " else "") + text.uppercase(), modifier,
        style = MwTs(color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.13.em),
    )
}

fun numStyle(size: Float, color: Color = Color.White) = MwTs(
    color = color, fontSize = size.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.035).em,
    fontFeatureSettings = "tnum",
)

@Composable
fun MoneyText(parts: Pair<String, String>, size: Float, color: Color = Color.White, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            append(parts.first)
            withStyle(SpanStyle(fontSize = (size * 0.56f).sp, color = color.copy(alpha = 0.55f))) { append(parts.second) }
        },
        modifier, style = numStyle(size, color),
    )
}
