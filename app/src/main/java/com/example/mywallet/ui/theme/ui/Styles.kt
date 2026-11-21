package com.example.mywallet.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.example.mywallet.R
import com.example.mywallet.data.AppState
import com.example.mywallet.data.Store
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * UI THEMES (the whole look: shapes, borders, shadows, font, background art).
 * Separate from Palettes: a palette only changes colours, a theme changes how everything is built.
 * mode: 0 normal rounded, 1 slanted cuts, 2 pixel steps, 3 flat and minimal
 */
class UiStyle(val id: String, val name: String, val blurb: String, val mode: Int, val fontScale: Float = 1f, val italic: Boolean = false)

val UI_STYLES: List<UiStyle> = listOf(
    UiStyle("classic", "Classic", "The original frosted glass look", 0),
    UiStyle("phantom", "Phantom", "Bold red and black, sharp slanted panels", 1, 1f, true),
    UiStyle("cat", "Cozy cat", "Warm, soft and round, with little paws", 0, 1f),
    UiStyle("pixel", "Retro pixel", "Chunky blocks, hard shadows, pixel icons", 2, 1f),
    UiStyle("minimal", "Minimal", "Flat, quiet and clean, no effects", 3),
    UiStyle("terminal", "Terminal", "Green on black, monospace, scanlines", 4),
    UiStyle("neon", "Neon night", "Glowing magenta and cyan outlines", 0),
)

private val PixelFont = FontFamily(Font(R.font.mw_pixel_regular, FontWeight.Normal), Font(R.font.mw_pixel_bold, FontWeight.Bold))
private val RoundFont = FontFamily(Font(R.font.mw_round_regular, FontWeight.Normal), Font(R.font.mw_round_semibold, FontWeight.SemiBold), Font(R.font.mw_round_bold, FontWeight.Bold))

object MwStyle {
    var cur by mutableStateOf(UI_STYLES[0])

    var coat by mutableStateOf("mixed")

    fun apply(id: String, coatId: String = "mixed") {
        val s = UI_STYLES.firstOrNull { it.id == id } ?: UI_STYLES[0]; if (s.id != cur.id) cur = s
        if (coatId != coat) coat = coatId
    }

    val isClassic: Boolean get() = cur.id == "classic"

    // colours used while the palette is Default (a chosen palette always wins)
    val bg: Color? get() = when (cur.id) { "phantom" -> Color(0xFF0B0B10); "cat" -> Color(0xFF171214); "pixel" -> Color(0xFF0F1424); "minimal" -> Color(0xFF0C0C0E); "terminal" -> Color(0xFF040804); "neon" -> Color(0xFF0A0614); else -> null }
    val accent: Color? get() = when (cur.id) { "phantom" -> Color(0xFFE8101F); "cat" -> Color(0xFFEE9444); "pixel" -> Color(0xFFFBBF3A); "minimal" -> Color(0xFFE9E9EC); "terminal" -> Color(0xFF39FF7A); "neon" -> Color(0xFFFF2BD6); else -> null }
    val mint: Color? get() = when (cur.id) { "phantom" -> Color(0xFF3FD8FF); "cat" -> Color(0xFF7ADBB5); "pixel" -> Color(0xFF45D777); "minimal" -> Color(0xFFB9BAC2); "terminal" -> Color(0xFF9CFFBC); "neon" -> Color(0xFF2BE4FF); else -> null }
    val tint: Color? get() = when (cur.id) { "phantom" -> Color(0xFF9E0B16); "cat" -> Color(0xFF5A3A2A); "pixel" -> Color(0xFF2E3B66); "minimal" -> Color(0xFF3A3A40); "terminal" -> Color(0xFF0E3A1C); "neon" -> Color(0xFF5A1050); else -> null }

    val font: FontFamily? get() = when (cur.id) { "pixel" -> PixelFont; "cat" -> RoundFont; "terminal" -> FontFamily.Monospace; else -> null }

    val fill: Color get() = when (cur.id) { "phantom" -> Color(0xFF111118).copy(alpha = 0.96f); "cat" -> Color(0xFF241D21).copy(alpha = 0.94f); "pixel" -> Color(0xFF1C2340).copy(alpha = 0.97f); "terminal" -> Color(0xFF081008).copy(alpha = 0.96f); "neon" -> Color(0xFF150B24).copy(alpha = 0.92f); else -> Color.White.copy(alpha = 0.05f) }
    val fillDark: Color get() = when (cur.id) { "phantom" -> Color(0xFF08080C); "cat" -> Color(0xFF1A1417); "pixel" -> Color(0xFF141A30); "terminal" -> Color(0xFF050A05); "neon" -> Color(0xFF0E0719); else -> Color(0xFF111114) }
    val borderColor: Color get() = when (cur.id) { "phantom" -> Color.White.copy(alpha = 0.9f); "cat" -> MW.Accent.copy(alpha = 0.28f); "pixel" -> Color(0xFF4A5A94); "terminal" -> MW.Accent.copy(alpha = 0.6f); "neon" -> MW.Mint.copy(alpha = 0.7f); else -> Color.White.copy(alpha = 0.10f) }
    val borderWidth: Dp get() = when (cur.id) { "phantom" -> 2.dp; "pixel" -> 2.dp; "neon" -> 1.5.dp; else -> 1.dp }
    val shadowColor: Color? get() = when (cur.id) { "phantom" -> MW.Accent; "pixel" -> Color.Black.copy(alpha = 0.55f); "neon" -> MW.Accent.copy(alpha = 0.45f); else -> null }
    val shadowOffset: Dp get() = when (cur.id) { "phantom" -> 3.dp; "pixel" -> 4.dp; "neon" -> 2.dp; else -> 0.dp }
}

/* ---------------------------- shapes ---------------------------- */

private class StyledShape(val base: Shape, val mode: Int) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val o = base.createOutline(size, layoutDirection, density)
        val rr = (o as? Outline.Rounded)?.roundRect ?: return o
        val w = size.width; val h = size.height; val d = density.density
        val tl = rr.topLeftCornerRadius.x; val tr = rr.topRightCornerRadius.x; val br = rr.bottomRightCornerRadius.x; val bl = rr.bottomLeftCornerRadius.x
        val big = maxOf(maxOf(tl, tr), maxOf(br, bl))
        if (big < 1f) return o
        when (mode) {
            1 -> {
                val c = minOf(big, 14f * d, minOf(w, h) * 0.4f)
                val p = Path()
                p.moveTo(0f, 0f)
                if (tr >= 1f) { p.lineTo(w - c, 0f); p.lineTo(w, c) } else p.lineTo(w, 0f)
                p.lineTo(w, h)
                if (bl >= 1f) { p.lineTo(c, h); p.lineTo(0f, h - c) } else p.lineTo(0f, h)
                p.close()
                return Outline.Generic(p)
            }
            2 -> {
                val s = minOf(4f * d, h / 4f, w / 4f)
                val p = Path()
                if (tl >= 1f) { p.moveTo(0f, s); p.lineTo(s, s); p.lineTo(s, 0f) } else p.moveTo(0f, 0f)
                if (tr >= 1f) { p.lineTo(w - s, 0f); p.lineTo(w - s, s); p.lineTo(w, s) } else p.lineTo(w, 0f)
                if (br >= 1f) { p.lineTo(w, h - s); p.lineTo(w - s, h - s); p.lineTo(w - s, h) } else p.lineTo(w, h)
                if (bl >= 1f) { p.lineTo(s, h); p.lineTo(s, h - s); p.lineTo(0f, h - s) } else p.lineTo(0f, h)
                p.close()
                return Outline.Generic(p)
            }
            4 -> return Outline.Rounded(RoundRect(0f, 0f, w, h, CornerRadius(minOf(big, 3f * d))))
            3 -> {
                if (big >= minOf(w, h) / 2f - 1f) return o
                val r = minOf(big, 12f * d)
                return Outline.Rounded(RoundRect(0f, 0f, w, h, CornerRadius(minOf(tl, r)), CornerRadius(minOf(tr, r)), CornerRadius(minOf(br, r)), CornerRadius(minOf(bl, r))))
            }
        }
        return o
    }
    override fun equals(other: Any?) = other is StyledShape && other.base == base && other.mode == mode
    override fun hashCode() = base.hashCode() * 31 + mode
}

private fun styled(b: Shape): Shape { val m = MwStyle.cur.mode; return if (m == 0) b else StyledShape(b, m) }

/** drop-in for RoundedCornerShape: follows the chosen theme */
fun MwShape(size: Dp): Shape = styled(RoundedCornerShape(size))
fun MwShape(topStart: Dp = 0.dp, topEnd: Dp = 0.dp, bottomEnd: Dp = 0.dp, bottomStart: Dp = 0.dp): Shape = styled(RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart))
val MwCircle: Shape get() = styled(CircleShape)

fun Modifier.hardShadow(shape: Shape, color: Color, off: Dp): Modifier = this.drawBehind {
    val o = shape.createOutline(size, layoutDirection, this)
    translate(off.toPx(), off.toPx()) {
        when (o) {
            is Outline.Rectangle -> drawRect(color, o.rect.topLeft, o.rect.size)
            is Outline.Rounded -> drawPath(Path().apply { addRoundRect(o.roundRect) }, color)
            is Outline.Generic -> drawPath(o.path, color)
        }
    }
}

/* ---------------------------- text ---------------------------- */

/** drop-in for TextStyle(...): adds the theme's font */
fun MwTs(
    color: Color = Color.Unspecified, fontSize: TextUnit = TextUnit.Unspecified, fontWeight: FontWeight? = null, fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null, letterSpacing: TextUnit = TextUnit.Unspecified, textDecoration: TextDecoration? = null,
    textAlign: TextAlign = TextAlign.Unspecified, lineHeight: TextUnit = TextUnit.Unspecified, fontFeatureSettings: String? = null,
): TextStyle {
    val st = MwStyle.cur
    var fs = if (fontSize.isSpecified && st.fontScale != 1f) fontSize * st.fontScale else fontSize
    // the pixel font is hard to read at small sizes, and worse for numbers: numbers use a clean font and nothing is tiny
    val isNum = fontFeatureSettings?.contains("tnum") == true
    if (st.id == "pixel" && fs.isSpecified && fs.value < 12.5f) fs = 12.5f.sp
    val fam = fontFamily ?: if (st.id == "pixel" && isNum) FontFamily.SansSerif else MwStyle.font
    return TextStyle(
        color = color, fontSize = fs, fontWeight = fontWeight, fontStyle = fontStyle ?: if (st.italic) FontStyle.Italic else null,
        fontFamily = fam, letterSpacing = if (st.id == "pixel" && isNum) TextUnit.Unspecified else letterSpacing, textDecoration = textDecoration, textAlign = textAlign,
        lineHeight = lineHeight, fontFeatureSettings = fontFeatureSettings,
    )
}

/* ---------------------------- panels ---------------------------- */

/** the panel look for every theme except Classic */
fun Modifier.themedPanel(shape: Shape, tint: Color?, dark: Boolean): Modifier {
    val sh = MwStyle.shadowColor
    var m = this
    if (sh != null) m = m.hardShadow(shape, sh, MwStyle.shadowOffset)
    if (MwStyle.cur.id == "cat") m = m.catEars()
    m = m.clip(shape).background(if (dark) MwStyle.fillDark else MwStyle.fill)
    if (tint != null) m = m.background(tint.copy(alpha = 0.22f))
    return m.border(MwStyle.borderWidth, MwStyle.borderColor, shape)
}

/** a cat coat: ear colours and markings (the Cozy cat theme puts these ears on the panels) */
class Coat(
    val id: String, val name: String, val left: Color, val right: Color, val inner: Color, val edge: Color,
    val stripes: Boolean = false, val tip: Color? = null, val tall: Float = 1f, val fold: Boolean = false,
)

val COATS: List<Coat> = listOf(
    Coat("tabby", "Orange tabby", Color(0xFFD98A3D), Color(0xFFD98A3D), Color(0xFFF2A9A0), Color(0xFF8A4F1A), stripes = true),
    Coat("tux", "Tuxedo", Color(0xFF16161C), Color(0xFF16161C), Color(0xFFF3C6CC), Color(0xFF5A5A66)),
    Coat("white", "White", Color(0xFFF3EFE9), Color(0xFFF3EFE9), Color(0xFFF4B6BE), Color(0xFFB8B2A8)),
    Coat("gray", "Gray tabby", Color(0xFF8E96A4), Color(0xFF8E96A4), Color(0xFFE7B5BB), Color(0xFF4F5663), stripes = true),
    Coat("calico", "Calico", Color(0xFFE08A2E), Color(0xFF1A1A1F), Color(0xFFF2B7B0), Color(0xFF6B4A2A)),
    Coat("siamese", "Siamese", Color(0xFFE8D8BF), Color(0xFFE8D8BF), Color(0xFFE9B5AE), Color(0xFF8A7458), tip = Color(0xFF5A3F2E), tall = 1.35f),
    Coat("black", "Black cat", Color(0xFF0E0E10), Color(0xFF0E0E10), Color(0xFFB07080), Color(0xFF6A6A74)),
    Coat("fold", "Scottish fold", Color(0xFF9AA8BA), Color(0xFF9AA8BA), Color(0xFFE2B8BE), Color(0xFF5C6B80), fold = true),
)

private fun DrawScope.ear(x0: Float, w: Float, h: Float, lean: Float, c: Coat, color: Color) {
    val d = density
    val apexX = x0 + w / 2f + lean
    val p = Path()
    if (c.fold) {
        p.moveTo(x0, 4f * d); p.quadraticTo(x0 + w * 0.05f, -h, x0 + w * 0.5f, -h); p.quadraticTo(x0 + w * 0.95f, -h, x0 + w, 4f * d)
    } else { p.moveTo(x0, 4f * d); p.lineTo(apexX, -h); p.lineTo(x0 + w, 4f * d) }
    p.close()
    drawPath(p, color)
    val q = Path().apply {
        moveTo(x0 + w * 0.22f, 3f * d)
        if (c.fold) { quadraticTo(x0 + w * 0.25f, -h * 0.7f, x0 + w * 0.5f, -h * 0.7f); quadraticTo(x0 + w * 0.75f, -h * 0.7f, x0 + w * 0.78f, 3f * d) }
        else { lineTo(apexX, -h + 5f * d); lineTo(x0 + w * 0.78f, 3f * d) }
        close()
    }
    drawPath(q, c.inner.copy(alpha = if (color.luminance() > 0.5f) 0.85f else 0.55f))
    c.tip?.let { tc ->
        val t = Path().apply { moveTo(apexX, -h); lineTo(apexX - w * 0.2f, -h * 0.45f); lineTo(apexX + w * 0.2f, -h * 0.45f); close() }
        drawPath(t, tc)
    }
    if (c.stripes) {
        drawLine(c.edge, Offset(apexX - 3.2f * d, -h * 0.4f), Offset(apexX + 3.2f * d, -h * 0.4f), 1.4f * d)
        drawLine(c.edge, Offset(apexX - 2f * d, -h * 0.7f), Offset(apexX + 2f * d, -h * 0.7f), 1.4f * d)
    }
    drawPath(p, c.edge.copy(alpha = 0.7f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f * d))
}

/** little cat ears sticking up from big panels (drawn behind the panel, so only the tips show) */
private fun Modifier.catEars(): Modifier = this.drawBehind {
    val d = density
    if (size.width < 150f * d || size.height < 58f * d) return@drawBehind
    // "mixed" gives every panel its own cat, picked from its size so it stays the same between redraws
    val c = COATS.firstOrNull { it.id == MwStyle.coat } ?: COATS[((size.width * 7f + size.height * 13f).toInt() / 3).mod(COATS.size)]
    val earW = (c.tall.coerceAtMost(1.1f) * 22f) * d; val earH = (if (c.fold) 9f else 13f) * c.tall * d
    for (side in 0..1) {
        val x0 = if (side == 0) 20f * d else size.width - 20f * d - earW
        ear(x0, earW, earH, if (side == 0) -3f * d else 3f * d, c, if (side == 0) c.left else c.right)
    }
}

/* ---------------------------- background art ---------------------------- */

private fun pr(i: Int, k: Int): Float { val v = sin(i * 12.9898 + k * 78.233) * 43758.5453; return (v - floor(v)).toFloat() }

@Composable
fun StyleScene(page: Float, modifier: Modifier = Modifier) {
    val id = MwStyle.cur.id
    val bg = MW.Bg; val ac = MW.Accent
    Canvas(modifier.fillMaxSize()) {
        drawRect(bg, topLeft = Offset(-100f, -100f), size = Size(size.width + 200f, size.height + 200f))
        val w = size.width; val h = size.height; val d = density
        val sx = -page * 40f * d
        when (id) {
            "phantom" -> {
                val p = Path().apply { moveTo(w * 0.28f + sx, 0f); lineTo(w + 200f, 0f); lineTo(w + 200f, h * 0.2f); lineTo(w * 0.5f + sx, h * 0.075f); close() }
                drawPath(p, ac.copy(alpha = 0.92f))
                val p2 = Path().apply { moveTo(w * 0.06f + sx, 0f); lineTo(w * 0.34f + sx, 0f); lineTo(w * 0.16f + sx, h * 0.05f); close() }
                drawPath(p2, Color.White.copy(alpha = 0.08f))
                val p3 = Path().apply { moveTo(-200f, h * 0.9f); lineTo(w * 0.62f + sx, h + 10f); lineTo(-200f, h + 10f); close() }
                drawPath(p3, ac.copy(alpha = 0.55f))
                val p4 = Path().apply { moveTo(w * 0.7f + sx, h + 10f); lineTo(w + 200f, h * 0.86f); lineTo(w + 200f, h + 10f); close() }
                drawPath(p4, Color.White.copy(alpha = 0.06f))
            }
            "cat" -> {
                drawCircle(Brush.radialGradient(listOf(ac.copy(alpha = 0.16f), Color.Transparent), Offset(w * 0.2f + sx, h * 0.12f), w * 0.8f), w * 0.8f, Offset(w * 0.2f + sx, h * 0.12f))
                drawCircle(Brush.radialGradient(listOf(MW.Mint.copy(alpha = 0.08f), Color.Transparent), Offset(w * 0.9f + sx, h * 0.8f), w * 0.7f), w * 0.7f, Offset(w * 0.9f + sx, h * 0.8f))
                for (i in 0 until 9) paw(Offset((pr(i, 1) * 1.2f - 0.1f) * w + sx * 0.6f, pr(i, 2) * h), (16f + 14f * pr(i, 3)) * d, (pr(i, 4) - 0.5f) * 50f, Color.White.copy(alpha = 0.045f))
            }
            "pixel" -> {
                var x = 0f
                while (x < w) { drawRect(Color.White.copy(alpha = 0.012f), Offset(x, 0f), Size(2f * d, h)); x += 28f * d }
                for (i in 0 until 10) {
                    val s = (3f + 3f * floor(pr(i, 3) * 2f)) * d
                    drawRect((if (i % 3 == 0) ac else Color.White).copy(alpha = 0.08f + 0.06f * pr(i, 4)), Offset(floor(pr(i, 1) * w / (4f * d)) * 4f * d + sx * 0.3f, floor(pr(i, 2) * h / (4f * d)) * 4f * d), Size(s, s))
                }
            }
            "terminal" -> {
                drawCircle(Brush.radialGradient(listOf(ac.copy(alpha = 0.10f), Color.Transparent), Offset(w * 0.5f, h * 0.2f), w), w, Offset(w * 0.5f, h * 0.2f))
                var y = 0f
                while (y < h) { drawRect(Color.Black.copy(alpha = 0.22f), Offset(0f, y), Size(w, 1.5f * d)); y += 4f * d }
            }
            "neon" -> {
                drawCircle(Brush.radialGradient(listOf(ac.copy(alpha = 0.28f), Color.Transparent), Offset(w * 0.1f + sx, h * 0.05f), w * 0.9f), w * 0.9f, Offset(w * 0.1f + sx, h * 0.05f))
                drawCircle(Brush.radialGradient(listOf(MW.Mint.copy(alpha = 0.20f), Color.Transparent), Offset(w * 0.95f + sx, h * 0.85f), w * 0.9f), w * 0.9f, Offset(w * 0.95f + sx, h * 0.85f))
                val gy = h * 0.72f
                var k = 0
                while (k <= 8) { val y = gy + (h - gy) * (k * k / 64f); drawRect(MW.Mint.copy(alpha = 0.07f), Offset(0f, y), Size(w, 1.2f * d)); k++ }
                for (i in -6..6) drawLine(MW.Mint.copy(alpha = 0.06f), Offset(w / 2f + i * 14f * d, gy), Offset(w / 2f + i * 70f * d, h), 1.2f * d)
            }
            else -> drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.035f), Color.Transparent), 0f, h * 0.4f))
        }
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, bg.copy(alpha = 0.5f)), startY = h * 0.6f, endY = h))
    }
}

private fun DrawScope.paw(c: Offset, r: Float, deg: Float, col: Color) {
    val a = Math.toRadians(deg.toDouble())
    fun rot(x: Float, y: Float) = Offset(c.x + (x * cos(a) - y * sin(a)).toFloat() * r, c.y + (x * sin(a) + y * cos(a)).toFloat() * r)
    drawOval(col, rot(-0.55f, 0f).let { Offset(c.x - 0.55f * r, c.y - 0.1f * r) }, Size(1.1f * r, 0.9f * r))
    listOf(-0.62f to -0.55f, -0.22f to -0.9f, 0.22f to -0.9f, 0.62f to -0.55f).forEach { (x, y) -> drawCircle(col, 0.2f * r, rot(x, y)) }
}

/* ---------------------------- picker (Settings > Display) ---------------------------- */

@Composable
fun UiThemePicker(s: AppState, store: Store) {
    val sel = s.settings.uiTheme
    Lbl("Theme", Modifier.padding(top = 4.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        UI_STYLES.forEach { st ->
            val on = sel == st.id
            Column(
                Modifier.width(150.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.07f))
                    .border(if (on) BorderStroke(2.dp, MW.Accent) else BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(18.dp))
                    .clickable { store.updateSettings(mapOf("uiTheme" to JsonPrimitive(st.id))) }.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StylePreview(st)
                Text(st.name, maxLines = 1, style = MwTs(color = Color.White, fontSize = 13.5.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Bold))
                Text(st.blurb, maxLines = 2, style = MwTs(color = Color.White.copy(alpha = 0.7f), fontSize = 11.5.sp, lineHeight = 14.sp))
            }
        }
    }
    if (sel == "cat") {
        Lbl("Your cat", Modifier.padding(top = 10.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf<Coat?>(null) + COATS).forEach { c ->
                val on = (c?.id ?: "mixed") == s.settings.catCoat
                Column(
                    Modifier.width(92.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.07f))
                        .border(if (on) BorderStroke(2.dp, MW.Accent) else BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(16.dp))
                        .clickable { store.updateSettings(mapOf("catCoat" to JsonPrimitive(c?.id ?: "mixed"))) }.padding(8.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Canvas(Modifier.fillMaxWidth().height(34.dp)) {
                        val d = density
                        val list = if (c == null) listOf(COATS[0], COATS[1], COATS[2]) else listOf(c, c)
                        val ew = 20f * d; val gap = size.width / list.size
                        list.forEachIndexed { k, cc ->
                            val x0 = gap * k + (gap - ew) / 2f
                            translate(0f, size.height - 4f * d) { ear(x0, ew, 14f * d * cc.tall, 0f, cc, if (k == 1 && c?.id == "calico") cc.right else cc.left) }
                        }
                        drawRect(Color(0xFF241D21), Offset(0f, size.height - 4f * d), Size(size.width, 4f * d))
                    }
                    Text(c?.name ?: "Mixed", maxLines = 1, style = MwTs(color = Color.White, fontSize = 11.5.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold))
                }
            }
        }
    }
    Text("A theme changes the whole look. Palettes below only change the colors.", Modifier.padding(top = 6.dp), style = MwTs(color = MW.Dim, fontSize = 12.sp))
}

@Composable
private fun StylePreview(st: UiStyle) {
    Canvas(Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(12.dp))) {
        val w = size.width; val h = size.height; val d = density
        val (bg, card, line, ac) = when (st.id) {
            "phantom" -> listOf(Color(0xFF0B0B10), Color(0xFF111118), Color.White, Color(0xFFE8101F))
            "cat" -> listOf(Color(0xFF171214), Color(0xFF241D21), Color(0xFF5A3F2E), Color(0xFFEE9444))
            "pixel" -> listOf(Color(0xFF0F1424), Color(0xFF1C2340), Color(0xFF05070F), Color(0xFFFBBF3A))
            "minimal" -> listOf(Color(0xFF0C0C0E), Color(0xFF17171A), Color(0xFF2A2A2E), Color(0xFFE9E9EC))
            "terminal" -> listOf(Color(0xFF040804), Color(0xFF081008), Color(0xFF2E9B52), Color(0xFF39FF7A))
            "neon" -> listOf(Color(0xFF0A0614), Color(0xFF150B24), Color(0xFF2BE4FF), Color(0xFFFF2BD6))
            else -> listOf(Color(0xFF060912), Color(0xFF16302E), Color(0xFF2A5A52), Color(0xFF2DD4A7))
        }
        drawRect(bg)
        if (st.id == "classic") { drawCircle(Brush.radialGradient(listOf(Color(0x660FB98A), Color.Transparent), Offset(w * 0.2f, 0f), w * 0.7f), w * 0.7f, Offset(w * 0.2f, 0f)) }
        if (st.id == "phantom") { val p = Path().apply { moveTo(w * 0.5f, 0f); lineTo(w, 0f); lineTo(w, h * 0.3f); close() }; drawPath(p, ac) }
        val x0 = 10f * d; val y0 = 18f * d; val cw = w - 20f * d; val ch = h - 28f * d
        val c = 7f * d
        if (st.id == "phantom") drawPath(Path().apply { moveTo(x0 + 3f * d, y0 + 3f * d); lineTo(x0 + cw - c + 3f * d, y0 + 3f * d); lineTo(x0 + cw + 3f * d, y0 + c + 3f * d); lineTo(x0 + cw + 3f * d, y0 + ch + 3f * d); lineTo(x0 + c + 3f * d, y0 + ch + 3f * d); lineTo(x0 + 3f * d, y0 + ch - c + 3f * d); close() }, ac)
        val shape = Path().apply {
            when (st.id) {
                "phantom" -> { moveTo(x0, y0); lineTo(x0 + cw - c, y0); lineTo(x0 + cw, y0 + c); lineTo(x0 + cw, y0 + ch); lineTo(x0 + c, y0 + ch); lineTo(x0, y0 + ch - c); close() }
                "pixel" -> { val s = 3f * d; moveTo(x0, y0 + s); lineTo(x0 + s, y0 + s); lineTo(x0 + s, y0); lineTo(x0 + cw - s, y0); lineTo(x0 + cw - s, y0 + s); lineTo(x0 + cw, y0 + s); lineTo(x0 + cw, y0 + ch - s); lineTo(x0 + cw - s, y0 + ch - s); lineTo(x0 + cw - s, y0 + ch); lineTo(x0 + s, y0 + ch); lineTo(x0 + s, y0 + ch - s); lineTo(x0, y0 + ch - s); close() }
                else -> addRoundRect(RoundRect(x0, y0, x0 + cw, y0 + ch, CornerRadius(if (st.id == "minimal") 5f * d else if (st.id == "terminal") 2f * d else 12f * d)))
            }
        }
        drawPath(shape, card)
        drawPath(shape, line, style = androidx.compose.ui.graphics.drawscope.Stroke((if (st.id == "phantom" || st.id == "pixel" || st.id == "neon") 2f else 1f) * d))
        drawRect(ac, Offset(x0 + 10f * d, y0 + 10f * d), Size(cw * 0.4f, 5f * d))
        drawRect(Color.White.copy(alpha = 0.5f), Offset(x0 + 10f * d, y0 + 22f * d), Size(cw * 0.65f, 4f * d))
    }
}
