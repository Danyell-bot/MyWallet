package com.example.mywallet.ui

import android.app.DatePickerDialog
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.mywallet.data.civilFromDays
import com.example.mywallet.data.daysFromCivil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.data.AppState
import com.example.mywallet.data.guessCat
import com.example.mywallet.data.Rec
import com.example.mywallet.data.Store
import com.example.mywallet.data.alertsFor
import com.example.mywallet.data.balanceNow
import com.example.mywallet.data.dn
import com.example.mywallet.data.fromDn
import com.example.mywallet.data.todayStr
import com.example.mywallet.data.money
import com.example.mywallet.data.rateOf
import com.example.mywallet.data.todayDn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun dayLabel(d: Long): String {
    val diff = d - todayDn()
    if (diff == 0L) return "Today"
    if (diff == -1L) return "Yesterday"
    if (diff == 1L) return "Tomorrow"
    val f = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    f.timeZone = TimeZone.getTimeZone("UTC")
    return f.format(Date(d * 86400000L))
}

/** tiny calculator: + - * / x × ÷ ( ) and decimals. Returns null when the text is not a valid sum. */
private fun evalMath(src: String): Double? {
    val t = src.replace("×", "*").replace("÷", "/").replace("x", "*").replace("X", "*").replace(",", "").replace(" ", "")
    if (t.isEmpty()) return null
    var i = 0
    fun peek(): Char? = if (i < t.length) t[i] else null
    fun expr(): Double {
        fun factor(): Double {
            val c = peek() ?: throw IllegalArgumentException()
            if (c == '-') { i++; return -factor() }
            if (c == '+') { i++; return factor() }
            if (c == '(') { i++; val v = expr(); if (peek() != ')') throw IllegalArgumentException(); i++; return v }
            val st = i
            while (i < t.length && (t[i].isDigit() || t[i] == '.')) i++
            if (st == i) throw IllegalArgumentException()
            return t.substring(st, i).toDouble()
        }
        fun term(): Double {
            var v = factor()
            while (true) {
                when (peek()) {
                    '*' -> { i++; v *= factor() }
                    '/' -> { i++; val d = factor(); if (d == 0.0) throw IllegalArgumentException(); v /= d }
                    '(' -> v *= factor()
                    else -> return v
                }
            }
        }
        var v = term()
        while (true) {
            when (peek()) {
                '+' -> { i++; v += term() }
                '-' -> { i++; v -= term() }
                else -> return v
            }
        }
    }
    return try {
        val v = expr()
        if (i != t.length || v.isNaN() || v.isInfinite()) null else Math.round(v * 100) / 100.0
    } catch (e: Exception) { null }
}

private fun trimNum(d: Double): String = java.math.BigDecimal.valueOf(d).stripTrailingZeros().toPlainString()

private fun typeColor(t: String): Color = when (t) { "income" -> MW.Mint; "transfer" -> MW.Sky; "check" -> MW.Amber; else -> MW.Rose }

/** read a photo, fix its rotation, shrink it and return it as a data URL (same format the web app stores) */
private suspend fun photoToDataUrl(ctx: android.content.Context, uri: Uri): String? = withContext(Dispatchers.Default) {
    try {
        val cr = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 2200) sample *= 2
        var bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return@withContext null
        val rot = try {
            cr.openInputStream(uri)?.use {
                when (android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
                    6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) { 0f }
        // keep enough detail for the text to stay readable (a tall phone screenshot is shrunk much less than a camera photo)
        val k = 2200f / maxOf(bmp.width, bmp.height)
        val m = Matrix()
        if (k < 1f) m.postScale(k, k)
        if (rot != 0f) m.postRotate(rot)
        if (!m.isIdentity) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
        "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (e: Throwable) { null }
}

private fun dataUrlToBitmap(url: String): Bitmap? = try {
    val b = Base64.decode(url.substringAfter("base64,"), Base64.DEFAULT)
    BitmapFactory.decodeByteArray(b, 0, b.size)
} catch (e: Exception) { null }

@Composable
private fun MiniAccount(modifier: Modifier, name: String, icon: String, sub: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.clip(shape)
            .background(if (selected) color.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f))
            .border(1.dp, if (selected) color else Color.White.copy(alpha = 0.12f), shape)
            .clickable { onClick() }.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        GlyphIcon(icon.ifBlank { name.take(1) }, if (selected) androidx.compose.ui.graphics.lerp(color, Color.White, 0.5f) else Color.White.copy(alpha = 0.85f), 24.dp)
        Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
        Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = MW.Dim, fontSize = 11.5.sp))
    }
}

@Composable
private fun CatTile(modifier: Modifier, name: String, icon: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.height(78.dp).clip(shape)
            .background(if (selected) color.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f))
            .border(1.dp, if (selected) color else Color.White.copy(alpha = 0.12f), shape)
            .clickable { onClick() }.padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        GlyphIcon(icon.ifBlank { "•" }, if (selected) androidx.compose.ui.graphics.lerp(color, Color.White, 0.5f) else Color.White.copy(alpha = 0.85f), 26.dp)
        Spacer(Modifier.height(4.dp))
        Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun SmallPill(text: String, modifier: Modifier = Modifier, icon: String? = null, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha = 0.1f)).border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(99.dp)).clickable { onClick() }.padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { MwIcon(icon, Color.White.copy(alpha = 0.9f), 18.dp); Spacer(Modifier.width(8.dp)) }
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
    }
}

/** equal-width grid: every row has `cols` slots so nothing is lopsided */
@Composable
private fun <T> EqualGrid(items: List<T>, cols: Int, gap: Int = 10, cell: @Composable (Modifier, T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(gap.dp)) {
        items.chunked(cols).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap.dp)) {
                row.forEach { cell(Modifier.weight(1f), it) }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Add a record, or edit/delete one when `edit` is not null. Slides up as a frosted panel with its own keypad. */
@Composable
fun BoxScope.RecordSheet(s: AppState, store: Store, edit: Rec?, onClose: () -> Unit, presetType: String? = null, presetAcct: String? = null) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val accts = s.accounts.filter { !it.ignored }
    var shown by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(edit?.type ?: presetType ?: "expense") }
    var amountText by remember { mutableStateOf(edit?.let { trimNum(it.amount) } ?: "") }
    var acct by remember {
        mutableStateOf(edit?.acct ?: presetAcct?.takeIf { id -> accts.any { it.id == id } } ?: (s.recs.maxByOrNull { it.ts }?.acct?.takeIf { id -> accts.any { it.id == id } } ?: accts.firstOrNull()?.id ?: ""))
    }
    var to by remember { mutableStateOf(edit?.to?.takeIf { it.isNotEmpty() } ?: accts.firstOrNull { it.id != acct }?.id ?: "") }
    var cat by remember { mutableStateOf(edit?.cat ?: "") }
    var note by remember { mutableStateOf(edit?.note ?: "") }
    var autoCat by remember { mutableStateOf(false) }
    var day by remember { mutableStateOf(edit?.let { dn(it.date) } ?: todayDn()) }
    var askDelete by remember { mutableStateOf(false) }
    fun applyNote(v: String) {
        note = v.take(120)
        if (type == "expense" || type == "income") {
            if (cat.isEmpty() || autoCat) {
                val g = s.guessCat(note, type)
                if (g != null) { cat = g; autoCat = true } else if (autoCat) { cat = ""; autoCat = false }
            }
        }
    }
    var toAmtText by remember { mutableStateOf(edit?.toAmount?.let { trimNum(it) } ?: "") }
    var photo by remember { mutableStateOf(edit?.let { store.photoOf(it.id) } ?: "") }
    val photo0 = remember { photo }
    var photoView by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var fav by remember { mutableStateOf(false) }
    val preview = remember(photo) { if (photo.isEmpty()) null else dataUrlToBitmap(photo)?.asImageBitmap() }
    var camUri by remember { mutableStateOf<Uri?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch { photo = photoToDataUrl(ctx, uri) ?: run { Toast.makeText(ctx, "Could not read that photo", Toast.LENGTH_SHORT).show(); photo } }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = camUri
        if (ok && u != null) scope.launch {
            photo = photoToDataUrl(ctx, u) ?: run { Toast.makeText(ctx, "Could not read that photo", Toast.LENGTH_SHORT).show(); photo }
            try { ctx.contentResolver.delete(u, null, null) } catch (e: Exception) { }
        } else if (u != null) { try { ctx.contentResolver.delete(u, null, null) } catch (e: Exception) { } }
        camUri = null
    }
    fun launchCamera() {
        try {
            val v = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "mywallet-receipt-" + System.currentTimeMillis() + ".jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            val u = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)
            if (u == null) { Toast.makeText(ctx, "Camera is not available", Toast.LENGTH_SHORT).show(); return }
            camUri = u
            camera.launch(u)
        } catch (e: Exception) { Toast.makeText(ctx, "Camera is not available", Toast.LENGTH_SHORT).show() }
    }
    val camPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else Toast.makeText(ctx, "Camera permission is needed to take a photo", Toast.LENGTH_SHORT).show()
    }
    fun takePhoto() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) launchCamera()
        else camPerm.launch(android.Manifest.permission.CAMERA)
    }
    fun pickDate() {
        val (y, m, d) = civilFromDays(day)
        DatePickerDialog(ctx, { _, yy, mm, dd -> day = daysFromCivil(yy.toLong(), (mm + 1).toLong(), dd.toLong()) }, y.toInt(), (m - 1).toInt(), d.toInt()).show()
    }
    val amtFocus = remember { FocusRequester() }
    val focusMgr = androidx.compose.ui.platform.LocalFocusManager.current
    val col = typeColor(type)
    val cats = s.cats.filter { !it.ignored && it.type == type }

    LaunchedEffect(Unit) { shown = true }
    LaunchedEffect(Unit) { if (edit == null) { delay(380); try { amtFocus.requestFocus() } catch (e: Exception) { } } }

    fun close() { shown = false; scope.launch { delay(240); onClose() } }
    fun toast(m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()
    BackHandler { close() }

    fun save() {
        val amt = evalMath(amountText) ?: return toast(if (amountText.isBlank()) "Enter an amount" else "Check the calculation")
        if (amt <= 0 && type != "check") return toast("Amount cannot be zero")
        if (acct.isEmpty()) return toast("Pick an account")
        if (type == "transfer" && (to.isEmpty() || to == acct)) return toast("Pick two different accounts")
        val obj = buildJsonObject {
            put("id", edit?.id ?: store.newId())
            put("ts", edit?.ts?.toLong() ?: System.currentTimeMillis())
            put("type", type)
            put("acct", acct)
            put("cat", if (type == "expense" || type == "income") cat else "")
            put("amount", amt)
            put("date", fromDn(day))
            put("note", note.trim())
            if (type == "transfer") {
                put("to", to)
                val ra = s.rateOf(acct); val rb = s.rateOf(to)
                if (kotlin.math.abs(ra - rb) > 1e-9) {
                    val manual = toAmtText.toDoubleOrNull() ?: 0.0
                    put("toAmount", if (manual > 0) manual else Math.round(amt * ra / rb * 100) / 100.0)
                }
            }
        }
        val id = obj["id"]!!.toString().trim('"')
        if (edit == null) store.addRec(obj)
        else store.updateRec(edit.id, obj, if (type == "transfer") (if (obj.containsKey("toAmount")) emptySet() else setOf("toAmount")) else setOf("to", "toAmount"))
        if (photo != photo0) store.setPhoto(id, photo)
        if (fav && edit == null && (type == "expense" || type == "income")) {
            val c = s.cats.firstOrNull { it.id == cat }
            store.addFav(buildJsonObject {
                put("id", store.newId())
                put("name", note.trim().ifEmpty { c?.name ?: "Quick add" })
                put("icon", c?.icon ?: "\u26A1")
                put("type", type); put("cat", cat); put("acct", acct); put("amount", amt)
            })
        }
        var msg = if (edit == null) "Saved" else "Updated"
        if (edit == null && type == "expense") {
            try {
                val rec = com.example.mywallet.data.AppJson.decodeFromJsonElement(com.example.mywallet.data.Rec.serializer(), obj)
                store.state?.alertsFor(rec)?.firstOrNull()?.let { msg += ". $it" }
            } catch (e: Exception) { }
        }
        toast(msg)
        close()
    }

    /** copy this record as a new one dated today (receipt photo is not copied) */
    fun duplicate() {
        val amt = evalMath(amountText) ?: return toast("Check the amount first")
        if (amt <= 0 && type != "check") return toast("Amount cannot be zero")
        if (acct.isEmpty()) return toast("Pick an account")
        val obj = buildJsonObject {
            put("id", store.newId()); put("ts", System.currentTimeMillis()); put("type", type); put("acct", acct)
            put("cat", if (type == "expense" || type == "income") cat else ""); put("amount", amt)
            put("date", todayStr()); put("note", note.trim())
            if (type == "transfer") {
                put("to", to)
                val ra = s.rateOf(acct); val rb = s.rateOf(to)
                if (kotlin.math.abs(ra - rb) > 1e-9) {
                    val manual = toAmtText.toDoubleOrNull() ?: 0.0
                    put("toAmount", if (manual > 0) manual else Math.round(amt * ra / rb * 100) / 100.0)
                }
            }
        }
        store.addRec(obj)
        toast("Copied to today")
        close()
    }

    // dim the screen behind
    AnimatedVisibility(shown, Modifier.fillMaxSize(), enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(remember { MutableInteractionSource() }, null) { close() })
    }

    AnimatedVisibility(
        shown, Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(tween(280)) { it } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(240)) { it } + fadeOut(tween(200)),
    ) {
        val top = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.95f).glassDark(top)
                .background(Color(0xFF070B16).copy(alpha = 0.82f))
                .verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (edit == null) "New record" else "Edit record", Modifier.weight(1f), style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold))
                Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable { close() }, contentAlignment = Alignment.Center) {
                    MwIcon("close", Color.White, 18.dp, weight = 2f)
                }
            }

            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.07f)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("expense" to "Expense", "income" to "Income", "transfer" to "Transfer", "check" to "Balance").forEach { (k, n) ->
                    val on = type == k
                    val c = typeColor(k)
                    Box(
                        Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (on) c.copy(alpha = 0.26f) else Color.Transparent)
                            .border(1.dp, if (on) c.copy(alpha = 0.7f) else Color.Transparent, RoundedCornerShape(14.dp))
                            .clickable { type = k; if (s.cats.none { it.id == cat && it.type == k }) { cat = ""; autoCat = false }; if (cat.isEmpty() && note.isNotBlank() && (k == "expense" || k == "income")) s.guessCat(note, k)?.let { cat = it; autoCat = true } },
                        contentAlignment = Alignment.Center,
                    ) { Text(n, style = TextStyle(color = if (on) c else Color.White.copy(alpha = 0.85f), fontSize = 14.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold)) }
                }
            }

            // the amount (one centred line)
            if (type == "check") Text("Balance your bank shows right now", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = TextStyle(color = MW.Dim, fontSize = 12.5.sp))
            Text("Amount (" + s.settings.cur + ")", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = TextStyle(color = MW.Dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
            val heroSize by animateFloatAsState(if (amountText.length > 9) 38f else 58f, tween(180), label = "heroSize")
            Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                BasicTextField(
                    amountText, { v -> amountText = v.filter { c -> c.isDigit() || c in ".+-*/x\u00D7\u00F7()," }.take(40) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(amtFocus),
                    textStyle = numStyle(heroSize, col).copy(textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(col),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Done, autoCorrectEnabled = false),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusMgr.clearFocus() }),
                    decorationBox = { inner -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { if (amountText.isEmpty()) Text("0", style = numStyle(58f, Color.White.copy(alpha = 0.3f))); inner() } },
                )
            }
            val hasOp = amountText.drop(1).any { it in "+-*/x\u00D7\u00F7()" } || amountText.startsWith("(")
            val shown = evalMath(amountText)
            // one fixed-height line, so the layout never shifts while you type
            Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (hasOp) (if (shown != null) "= " + s.money(shown) else "Keep typing\u2026")
                    else if (amountText.isEmpty()) "You can type sums like 120+45\u00D72" else "",
                    textAlign = TextAlign.Center, maxLines = 1,
                    style = TextStyle(
                        color = if (hasOp && shown != null) col else MW.Faint,
                        fontSize = if (hasOp) 15.sp else 12.sp, fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
            // date: ‹ Today ›  centred
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable { day -= 1 }, contentAlignment = Alignment.Center) { MwIcon("back", Color.White, 20.dp, weight = 2f) }
                Box(Modifier.width(150.dp)) { SmallPill(dayLabel(day), Modifier.fillMaxWidth()) { pickDate() } }
                Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable { day += 1 }, contentAlignment = Alignment.Center) { MwIcon("back", Color.White, 20.dp, Modifier.rotate(180f), weight = 2f) }
            }
            // note: always visible, full width
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 16.dp, vertical = 14.dp)) {
                BasicTextField(
                    note, { applyNote(it) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp), cursorBrush = SolidColor(col),
                    decorationBox = { inner -> Box { if (note.isEmpty()) Text("Add a note or #tag", style = TextStyle(color = Color.White.copy(alpha = 0.4f), fontSize = 15.sp)); inner() } },
                )
            }

            Lbl(if (type == "transfer") "From" else "Account")
            if (accts.size <= 4) EqualGrid(accts, 2) { m, a ->
                MiniAccount(m, a.name, a.icon, s.money(s.balanceNow(a)), acct == a.id, col) { acct = a.id; if (to == a.id) to = accts.firstOrNull { it.id != a.id }?.id ?: "" }
            } else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                accts.forEach { a -> MiniAccount(Modifier.width(150.dp), a.name, a.icon, s.money(s.balanceNow(a)), acct == a.id, col) { acct = a.id; if (to == a.id) to = accts.firstOrNull { it.id != a.id }?.id ?: "" } }
            }

            if (type == "transfer") {
                Lbl("To")
                val others = accts.filter { it.id != acct }
                if (others.size <= 4) EqualGrid(others, 2) { m, a -> MiniAccount(m, a.name, a.icon, s.money(s.balanceNow(a)), to == a.id, col) { to = a.id } }
                else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    others.forEach { a -> MiniAccount(Modifier.width(150.dp), a.name, a.icon, s.money(s.balanceNow(a)), to == a.id, col) { to = a.id } }
                }
                if (to.isNotEmpty() && kotlin.math.abs(s.rateOf(acct) - s.rateOf(to)) > 1e-9) {
                    val guess = Math.round((evalMath(amountText) ?: 0.0) * s.rateOf(acct) / s.rateOf(to) * 100) / 100.0
                    Lbl("Amount received")
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 16.dp, vertical = 14.dp)) {
                        BasicTextField(
                            toAmtText, { toAmtText = it.filter { c -> c.isDigit() || c == '.' }.take(12) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp), cursorBrush = SolidColor(col),
                            decorationBox = { inner -> Box { if (toAmtText.isEmpty()) Text(if (guess > 0) "About " + trimNum(guess) else "Leave empty to use today's rate", style = TextStyle(color = Color.White.copy(alpha = 0.4f), fontSize = 15.sp)); inner() } },
                        )
                    }
                }
            } else if (type == "expense" || type == "income") {
                Lbl(if (autoCat && cat.isNotEmpty()) "Category  \u2728 picked for you" else "Category")
                val tiles = listOf<com.example.mywallet.data.Cat?>(null) + cats
                EqualGrid(tiles, 4) { m, c ->
                    if (c == null) CatTile(m, "None", "-", cat.isEmpty(), col) { cat = ""; autoCat = false }
                    else CatTile(m, c.name, c.icon, cat == c.id, col) { cat = c.id; autoCat = false }
                }
            }

            // receipt photo + quick-add
            Lbl("Receipt")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallPill("Take photo", Modifier.weight(1f), "camera") { takePhoto() }
                SmallPill(if (photo.isEmpty()) "Choose photo" else "Change photo", Modifier.weight(1f), "image") { gallery.launch("image/*") }
            }
            if (preview != null) {
                Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.35f)).clickable { photoView = true }) {
                    Image(preview, "Receipt photo", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)).clickable { photo = "" }, contentAlignment = Alignment.Center) {
                        MwIcon("close", Color.White, 16.dp, weight = 2f)
                    }
                }
            }
            if (preview != null) SmallPill(if (scanning) "Reading the receipt..." else "Scan receipt for the total", Modifier.fillMaxWidth(), "sparkles") {
                if (!scanning) {
                    scanning = true
                    scanReceipt(photo) { r ->
                        scanning = false
                        if (r?.total != null) {
                            amountText = trimNum(r.total)
                            if (note.isBlank() && r.merchant != null) applyNote(r.merchant)
                            toast("Found " + trimNum(r.total) + ". Please check it.")
                        } else if (r == null || r.error != null) toast("Scan failed: " + (r?.error ?: "unknown") )
                        else if (!r.hadText) toast("No text found. Try a sharper photo.")
                        else toast("No total found. Type it in.")
                    }
                }
            }
            if (edit == null && (type == "expense" || type == "income")) {
                Box(
                    Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp))
                        .background(if (fav) col.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f))
                        .border(1.dp, if (fav) col else Color.White.copy(alpha = 0.12f), RoundedCornerShape(23.dp))
                        .clickable { fav = !fav },
                    contentAlignment = Alignment.Center,
                ) { Row(verticalAlignment = Alignment.CenterVertically) { MwIcon("zap", if (fav) col else Color.White, 18.dp); Spacer(Modifier.width(8.dp)); Text(if (fav) "Saved as quick-add" else "Save as quick-add", style = TextStyle(color = if (fav) col else Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)) } }
            }

            Box(
                Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(col.copy(alpha = 0.95f), col.copy(alpha = 0.7f)))).clickable { save() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (edit != null) "Save changes" else when (type) { "income" -> "Add income"; "transfer" -> "Move money"; "check" -> "Save balance"; else -> "Add expense" },
                    style = TextStyle(color = MW.OnAccent, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
                )
            }
            if (edit != null) {
                Row(Modifier.fillMaxWidth().height(44.dp).clickable { duplicate() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    MwIcon("copy", MW.Mint, 18.dp, weight = 2f); Spacer(Modifier.width(8.dp))
                    Text("Copy to today", style = TextStyle(color = MW.Mint, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                }
                Box(Modifier.fillMaxWidth().height(44.dp).clickable { askDelete = true }, contentAlignment = Alignment.Center) {
                    Text("Delete this record", style = TextStyle(color = MW.Rose, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime))
        }
    }

    if (photoView && preview != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { photoView = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.94f)).clickable { photoView = false }, contentAlignment = Alignment.Center) {
                Image(preview, "Receipt photo", Modifier.fillMaxSize().padding(12.dp), contentScale = ContentScale.Fit)
            }
        }
    }

    if (askDelete && edit != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { askDelete = false }) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xFF0E1426))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(28.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFFF6B81).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    MwIcon("trash", Color(0xFFFF8FA0), 28.dp)
                }
                Text("Delete this record?", style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold), textAlign = TextAlign.Center)
                Text(
                    s.money(edit.amount) + if (edit.note.isNotBlank()) " · " + edit.note else "",
                    style = TextStyle(color = MW.Dim, fontSize = 14.sp), textAlign = TextAlign.Center, maxLines = 2,
                )
                Text("This can't be undone.", style = TextStyle(color = MW.Faint, fontSize = 12.sp))
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Color.White.copy(alpha = 0.1f)).clickable { askDelete = false }, contentAlignment = Alignment.Center) {
                        Text("Keep it", style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    }
                    Box(
                        Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Color(0xFFFF6B81))
                            .clickable { askDelete = false; store.deleteRec(edit.id); toast("Deleted"); close() },
                        contentAlignment = Alignment.Center,
                    ) { Text("Delete", style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)) }
                }
            }
        }
    }
}
