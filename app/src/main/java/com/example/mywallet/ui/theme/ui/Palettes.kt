package com.example.mywallet.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.data.AppState
import com.example.mywallet.data.Store
import kotlinx.serialization.json.JsonPrimitive

/**
 * A theme is three colours:
 *  c1 = main background glow, c2 = second glow (a light one is toned down), c3 = accent (buttons, highlights).
 * The default theme keeps the original green look.
 */
class Palette(val id: String, val name: String, val c1: Color, val c2: Color, val c3: Color)

private fun hx(v: Long) = Color(0xFF000000 or v)

val DEFAULT_PALETTE = Palette("default", "Default", hx(0x0FB98A), hx(0x1E6FD9), hx(0x2DD4A7))

val PALETTES: List<Palette> = listOf(
    DEFAULT_PALETTE,
    Palette("dusk", "Dusk gold", hx(0x6B5B7B), hx(0x2B2B2E), hx(0xF2C14E)),
    Palette("forest", "Forest coral", hx(0x2F4A3A), hx(0xF3EBDD), hx(0xFE886A)),
    Palette("navy", "Navy sunset", hx(0x1B2A4A), hx(0xE8D8B9), hx(0xFF9126)),
    Palette("ocean", "Ocean", hx(0x0E4F5C), hx(0xD9F2EF), hx(0xFFB454)),
    Palette("berry", "Berry", hx(0x6A2C70), hx(0x1F1426), hx(0xFF7EB6)),
    Palette("sunset", "Brick", hx(0x7B2D26), hx(0xF6E0C8), hx(0xFFC857)),
    Palette("slate", "Slate lime", hx(0x2B3A42), hx(0xCFD8DC), hx(0xB6F542)),
    Palette("coffee", "Coffee", hx(0x4B2E22), hx(0xEFE0CF), hx(0xE9A15D)),
    Palette("mono", "Mono blue", hx(0x3A3F4B), hx(0xE6E6E6), hx(0x9AD1FF)),
)

fun parseHex(t: String): Color? {
    val h = t.trim().removePrefix("#")
    if (h.length != 6) return null
    val v = h.toLongOrNull(16) ?: return null
    return hx(v)
}

object MwTheme {
    var palette by mutableStateOf(DEFAULT_PALETTE)

    val isDefault: Boolean get() = palette.id == "default"
    val bg: Color get() = if (isDefault) Color(0xFF060912) else lerp(vivid(palette.c1, 1.15f, 1.0f), Color.Black, 0.76f)
    /** colour for tinted cards (profile, level, badges): the old deep green on Default */
    val tint: Color get() = if (isDefault) Color(0xFF16A37F) else lerp(accent, Color.Black, 0.22f)
    val accent: Color get() = if (isDefault) Color(0xFF2DD4A7) else palette.c3
    val mint: Color get() = if (isDefault) Color(0xFF7DF0CF) else lerp(palette.c3, Color.White, 0.4f)

    /** more saturated and a touch brighter, so glows look rich instead of muddy */
    fun vivid(c: Color, sat: Float, bright: Float): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(android.graphics.Color.rgb((c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt()), hsv)
        hsv[1] = (hsv[1] * sat).coerceIn(0f, 1f); hsv[2] = (hsv[2] * bright).coerceIn(0f, 1f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    val onAccent: Color get() = if (accent.luminance() > 0.35f) Color(0xFF04130F) else Color.White

    /** colours for the story slides (Rewind, Year): the original rainbow on Default, theme shades otherwise */
    fun slide(i: Int, a: Color, b: Color): Pair<Color, Color> {
        if (isDefault) return a to b
        val base = listOf(palette.c3, palette.c1, palette.c2, palette.c3, palette.c1, palette.c2)[i % 6]
        val top = if (base.luminance() > 0.5f) lerp(base, Color.Black, 0.55f) else lerp(base, Color.White, 0.08f)
        return top to lerp(top, Color.Black, 0.62f)
    }

    fun resolve(id: String, custom: String): Palette {
        if (id == "custom") {
            val p = custom.split(',').mapNotNull { parseHex(it) }
            if (p.size == 3) return Palette("custom", "Custom", p[0], p[1], p[2])
        }
        return PALETTES.firstOrNull { it.id == id } ?: DEFAULT_PALETTE
    }

    fun apply(id: String, custom: String) {
        val p = resolve(id, custom)
        if (p.id != palette.id || p.c1 != palette.c1 || p.c2 != palette.c2 || p.c3 != palette.c3) palette = p
    }
}

@Composable
fun ThemePicker(s: AppState, store: Store) {
    val sel = s.settings.palette
    var custom by remember { mutableStateOf(false) }
    Lbl("Theme", Modifier.padding(top = 4.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val list = PALETTES + listOfNotNull(MwTheme.resolve("custom", s.settings.pal).takeIf { it.id == "custom" })
        list.forEach { p ->
            val on = sel == p.id
            Column(
                Modifier.width(108.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.07f))
                    .border(if (on) BorderStroke(2.dp, MW.Accent) else BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(18.dp))
                    .clickable { store.updateSettings(mapOf("palette" to JsonPrimitive(p.id))) }.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(10.dp))) {
                    Box(Modifier.weight(1f).height(34.dp).background(if (p.id == "default") Color(0xFF060912) else lerp(p.c1, Color.Black, 0.3f)))
                    Box(Modifier.weight(1f).height(34.dp).background(p.c2))
                    Box(Modifier.weight(1f).height(34.dp).background(p.c3))
                }
                Text(p.name, maxLines = 1, style = TextStyle(color = Color.White, fontSize = 12.5.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold))
            }
        }
        Column(
            Modifier.width(108.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.07f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)), RoundedCornerShape(18.dp))
                .clickable { custom = true }.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                Text("+", style = TextStyle(color = MW.Mint, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold))
            }
            Text(if (sel == "custom") "Edit custom" else "Make your own", maxLines = 1, style = TextStyle(color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold))
        }
    }
    if (custom) CustomThemeForm(s, store) { custom = false }
}

@Composable
private fun CustomThemeForm(s: AppState, store: Store, onClose: () -> Unit) {
    val cur = s.settings.pal.split(',').let { if (it.size == 3) it else listOf("#6b5b7b", "#2b2b2e", "#f2c14e") }
    var a by remember { mutableStateOf(cur[0]) }
    var b by remember { mutableStateOf(cur[1]) }
    var c by remember { mutableStateOf(cur[2]) }
    val ca = parseHex(a); val cb = parseHex(b); val cc = parseHex(c)
    FormPanel("Your own theme", onClose) {
        Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))) {
            Box(Modifier.weight(1f).height(56.dp).background(ca ?: Color.DarkGray))
            Box(Modifier.weight(1f).height(56.dp).background(cb ?: Color.DarkGray))
            Box(Modifier.weight(1f).height(56.dp).background(cc ?: Color.DarkGray))
        }
        FText("Main color (background glow)", a, { a = it.take(7) }, "#6b5b7b")
        FText("Second color (soft glow)", b, { b = it.take(7) }, "#2b2b2e")
        FText("Accent color (buttons, highlights)", c, { c = it.take(7) }, "#f2c14e")
        Text("Type colors as 6 digits like #f2c14e. The accent should be a bright color so buttons stay readable.", style = TextStyle(color = MW.Dim, fontSize = 12.5.sp))
        BigButton("Use this theme") {
            if (ca != null && cb != null && cc != null) {
                fun h(x: String) = "#" + x.trim().removePrefix("#").lowercase()
                store.updateSettings(mapOf("pal" to JsonPrimitive(h(a) + "," + h(b) + "," + h(c)), "palette" to JsonPrimitive("custom")))
                onClose()
            }
        }
    }
}
