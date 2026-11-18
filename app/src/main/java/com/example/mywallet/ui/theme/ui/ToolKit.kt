package com.example.mywallet.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler
import com.example.mywallet.data.civilFromDays
import com.example.mywallet.data.daysFromCivil
import com.example.mywallet.data.dn
import com.example.mywallet.data.todayDn
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val EMO_CAT = listOf("🍜", "🍔", "🍕", "☕", "🧋", "🛒", "🚌", "🚗", "⛽", "✈️", "🧾", "🏠", "📱", "🛍️", "📚", "🎓", "💊", "🎮", "🎁", "🏦", "🪙", "💼", "💵", "🧩")
val EMO_GOAL = listOf("💻", "📱", "✈️", "🏠", "🚗", "🎓", "🎁", "🎮", "💼", "🪙", "💵", "🏦", "📚", "☕", "🛒", "🎯")
val EMO_ACCT = listOf("💵", "👛", "💳", "🏦", "🪙", "💰", "📱", "🏠", "🚗", "💼", "🎓", "🎯")
val GOAL_COL = listOf(Color(0xFF2DD4A7), Color(0xFF7BB8FF), Color(0xFFFFCB7A), Color(0xFFB9A6FF), Color(0xFFFF9FB0), Color(0xFF8CFFC0), Color(0xFFFF8A5A), Color(0xFF6EE7F9))

fun fmtDate(date: String): String {
    if (date.isEmpty()) return ""
    val f = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()); f.timeZone = TimeZone.getTimeZone("UTC")
    return f.format(Date(dn(date) * 86400000L))
}

/** full-screen page: aurora background, back button, title and an optional action pill */
@Composable
fun ToolShell(title: String, onClose: () -> Unit, actionLabel: String? = null, onAction: () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    BackHandler { onClose() }
    val haze = rememberHazeState()
    Box(Modifier.fillMaxSize().background(MW.Bg)) {
        SceneBackground(1f, Modifier.hazeSource(haze))
        CompositionLocalProvider(LocalHaze provides haze) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).glass(CircleShape).clickable { onClose() }, contentAlignment = Alignment.Center) { MwIcon("back", Color.White, 22.dp, weight = 2f) }
                    Text(title, Modifier.weight(1f), textAlign = TextAlign.Center, style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold))
                    if (actionLabel != null) {
                        Row(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(MW.Mint, MW.Accent))).clickable { onAction() }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            MwIcon("plus", Color(0xFF04130F), 16.dp, weight = 2.2f); Spacer(Modifier.width(4.dp))
                            Text(actionLabel, style = TextStyle(color = Color(0xFF04130F), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
                        }
                    } else Spacer(Modifier.size(40.dp))
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp), content = { content(); Spacer(Modifier.height(40.dp)) })
            }
        }
    }
}

/** scale + fade in, for centered pop-up cards */
@Composable
fun Modifier.popIn(): Modifier {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { on = true }
    val v by animateFloatAsState(if (on) 1f else 0f, tween(260, easing = FastOutSlowInEasing), label = "pop")
    return this.graphicsLayer { scaleX = 0.9f + 0.1f * v; scaleY = 0.9f + 0.1f * v; alpha = v }
}

/** bottom panel with a form inside; slides up, and slides away when closed */
@Composable
fun FormPanel(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    var shown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { shown = true }
    fun close() { shown = false; scope.launch { delay(230); onClose() } }
    Dialog(onDismissRequest = { close() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(shown, Modifier.fillMaxSize(), enter = fadeIn(tween(240)), exit = fadeOut(tween(200))) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(remember0, null) { close() })
            }
            AnimatedVisibility(
                shown,
                enter = slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(230)) { it } + fadeOut(tween(180)),
            ) {
                val top = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 760.dp).clip(top).background(Color(0xFF0B1020)).border(1.dp, Color.White.copy(alpha = 0.12f), top)
                        .clickable(remember0, null) { }.navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, Modifier.weight(1f), style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold))
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable { close() }, contentAlignment = Alignment.Center) { MwIcon("close", Color.White, 17.dp, weight = 2f) }
                    }
                    content()
                }
            }
        }
    }
}

private val remember0 = MutableInteractionSource()

@Composable
fun FText(label: String, value: String, onChange: (String) -> Unit, hint: String = "", decimal: Boolean = false, modifier: Modifier = Modifier) {
    Column(modifier) {
        Lbl(label, Modifier.padding(bottom = 6.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 16.dp, vertical = 14.dp)) {
            BasicTextField(
                value, { v -> onChange(if (decimal) v.filter { c -> c.isDigit() || c == '.' }.take(12) else v.take(40)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp), cursorBrush = SolidColor(MW.Accent),
                keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Text),
                decorationBox = { inner -> Box { if (value.isEmpty()) Text(hint, style = TextStyle(color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp)); inner() } },
            )
        }
    }
}

/** one-of-many pills; each option is (key, label, optional icon) */
@Composable
fun FChips(label: String, options: List<Triple<String, String, String?>>, selected: String, onSelect: (String) -> Unit, scroll: Boolean = true) {
    Column {
        if (label.isNotEmpty()) Lbl(label, Modifier.padding(bottom = 8.dp))
        Row(if (scroll) Modifier.horizontalScroll(rememberScrollState()) else Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (k, n, ic) ->
                val on = k == selected
                Row(
                    Modifier.then(if (scroll) Modifier else Modifier.weight(1f)).height(40.dp).clip(RoundedCornerShape(20.dp))
                        .background(if (on) MW.Accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                        .border(1.dp, if (on) MW.Accent else Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .clickable { onSelect(k) }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    if (ic != null) { GlyphIcon(ic, if (on) MW.Mint else Color.White, 18.dp); Spacer(Modifier.width(7.dp)) }
                    Text(n, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = if (on) MW.Mint else Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

fun opt(k: String, n: String, ic: String? = null) = Triple(k, n, ic)

@Composable
fun FIcons(label: String, icons: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column {
        Lbl(label, Modifier.padding(bottom = 8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            icons.chunked(6).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { e ->
                        val on = e == selected
                        Box(
                            Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(15.dp))
                                .background(if (on) MW.Accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.07f))
                                .border(1.dp, if (on) MW.Accent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(15.dp)).clickable { onSelect(e) },
                            contentAlignment = Alignment.Center,
                        ) { GlyphIcon(e, if (on) MW.Mint else Color.White.copy(alpha = 0.9f), 23.dp) }
                    }
                    repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun FDate(label: String, value: String, onChange: (String) -> Unit, optional: Boolean = false) {
    val ctx = LocalContext.current
    Column {
        Lbl(label, Modifier.padding(bottom = 6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)).clickable {
                    val (y, m, d) = civilFromDays(if (value.isEmpty()) todayDn() else dn(value))
                    DatePickerDialog(ctx, { _, yy, mm, dd -> onChange(com.example.mywallet.data.fromDn(daysFromCivil(yy.toLong(), (mm + 1).toLong(), dd.toLong()))) }, y.toInt(), (m - 1).toInt(), d.toInt()).show()
                }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                MwIcon("calendar", MW.Dim, 18.dp); Spacer(Modifier.width(10.dp))
                Text(if (value.isEmpty()) "No date" else fmtDate(value), style = TextStyle(color = if (value.isEmpty()) Color.White.copy(alpha = 0.4f) else Color.White, fontSize = 16.sp))
            }
            if (optional && value.isNotEmpty()) Box(Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)).clickable { onChange("") }, contentAlignment = Alignment.Center) { MwIcon("close", MW.Dim, 18.dp) }
        }
    }
}

@Composable
fun BigButton(text: String, color: Color = MW.Accent, textColor: Color = Color(0xFF04130F), icon: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.72f)))).clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { MwIcon(icon, textColor, 19.dp); Spacer(Modifier.width(8.dp)) }
        Text(text, style = TextStyle(color = textColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold))
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, color: Color = Color.White, icon: String? = null, onClick: () -> Unit) {
    Row(
        modifier.height(46.dp).clip(RoundedCornerShape(23.dp)).background(color.copy(alpha = 0.12f)).border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(23.dp)).clickable { onClick() }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { MwIcon(icon, color, 18.dp); Spacer(Modifier.width(7.dp)) }
        Text(text, maxLines = 1, style = TextStyle(color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.popIn().fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xFF0E1426)).border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(28.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(Color(0xFFFF6B81).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { MwIcon("trash", Color(0xFFFF8FA0), 26.dp) }
            Text(title, textAlign = TextAlign.Center, style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold))
            Text(text, textAlign = TextAlign.Center, style = TextStyle(color = MW.Dim, fontSize = 14.sp))
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Color.White.copy(alpha = 0.1f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) { Text("Keep it", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold)) }
                Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Color(0xFFFF6B81)).clickable { onConfirm() }, contentAlignment = Alignment.Center) { Text(confirm, style = TextStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) }
            }
        }
    }
}

@Composable
fun EmptyCard(icon: String, text: String) {
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { MwIcon(icon, MW.Mint, 26.dp) }
        Text(text, textAlign = TextAlign.Center, style = TextStyle(color = MW.Dim, fontSize = 14.sp, lineHeight = 20.sp))
    }
}

/** one list row inside a glass card */
@Composable
fun ItemRow(icon: String, title: String, sub: String, subColor: Color = MW.Dim, tint: Color = Color.White, trailing: @Composable (() -> Unit)? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().glassLite(RoundedCornerShape(22.dp)).clickable { onClick() }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { GlyphIcon(icon, Color.White.copy(alpha = 0.95f), 22.dp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold))
            Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = subColor, fontSize = 12.5.sp))
        }
        if (trailing != null) trailing()
    }
}

/** Makes every money amount in a sentence bold and coloured, so the numbers that matter are not missed. */
fun highlightMoney(text: String, cur: String, color: Color, base: Color = MW.Dim): androidx.compose.ui.text.AnnotatedString {
    val rx = Regex(Regex.escape(cur) + "\\s?-?[\\d,]+(\\.\\d+)?")
    return androidx.compose.ui.text.buildAnnotatedString {
        var last = 0
        rx.findAll(text).forEach { m ->
            append(text.substring(last, m.range.first))
            pushStyle(androidx.compose.ui.text.SpanStyle(color = color, fontWeight = FontWeight.ExtraBold))
            append(m.value); pop()
            last = m.range.last + 1
        }
        append(text.substring(last))
    }
}
