package com.example.mywallet.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mywallet.data.AppState
import com.example.mywallet.data.Rec
import com.example.mywallet.data.Store
import com.example.mywallet.data.acc
import com.example.mywallet.data.cat
import com.example.mywallet.data.money
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val thumbCache = android.util.LruCache<String, ImageBitmap>(80)

private fun decodePhoto(b: ByteArray?, sample: Int): ImageBitmap? = try {
    if (b == null) null else BitmapFactory.decodeByteArray(b, 0, b.size, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
} catch (e: Throwable) { null }

@Composable
private fun Thumb(s: AppState, store: Store, r: Rec, modifier: Modifier, onClick: () -> Unit) {
    val bmp by produceState<ImageBitmap?>(null, r.id) { value = thumbCache.get(r.id) ?: withContext(Dispatchers.Default) { decodePhoto(store.photoBytes(r.id), 4) }?.also { thumbCache.put(r.id, it) } }
    Box(modifier.aspectRatio(1f).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.08f)).clickable { onClick() }) {
        bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))).padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(s.money(r.amount), maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold))
        }
    }
}

@Composable
fun ReceiptsScreen(s: AppState, store: Store, onEdit: (Rec) -> Unit, onClose: () -> Unit) {
    BackHandler { onClose() }
    var view by remember { mutableStateOf<Rec?>(null) }
    val list = remember(s) { s.recs.filter { store.hasPhoto(it.id) }.sortedWith(compareByDescending<Rec> { it.date }.thenByDescending { it.ts }) }
    val haze = rememberHazeState()
    Box(Modifier.fillMaxSize().background(MW.Bg)) {
        SceneBackground(1f, Modifier.hazeSource(haze))
        CompositionLocalProvider(LocalHaze provides haze) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).glass(CircleShape).clickable { onClose() }, contentAlignment = Alignment.Center) { MwIcon("back", Color.White, 22.dp, weight = 2f) }
                    Text("Receipts", Modifier.weight(1f), textAlign = TextAlign.Center, style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold))
                    Spacer(Modifier.size(40.dp))
                }
                if (list.isEmpty()) {
                    EmptyCard("image", "No receipts yet.\nAdd a photo when you save a record and it shows up here.")
                } else {
                    Text("${list.size} receipt${if (list.size == 1) "" else "s"}", Modifier.padding(bottom = 10.dp), style = TextStyle(color = MW.Dim, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold))
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(list.chunked(3)) { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { r -> Thumb(s, store, r, Modifier.weight(1f)) { view = r } }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        item { Spacer(Modifier.height(30.dp)) }
                    }
                }
            }
        }
    }
    view?.let { r ->
        Dialog(onDismissRequest = { view = null }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            val full by produceState<ImageBitmap?>(null, r.id) { value = withContext(Dispatchers.Default) { decodePhoto(store.photoBytes(r.id), 1) } }
            Column(Modifier.fillMaxSize().background(Color(0xF2050810)).statusBarsPadding().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    full?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                }
                Column(Modifier.fillMaxWidth().glassDark(RoundedCornerShape(22.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(r.note.ifBlank { s.cat(r.cat)?.name ?: "Record" }, style = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold))
                    Text(s.money(r.amount) + " · " + fmtDate(r.date) + (s.acc(r.acct)?.let { " · " + it.name } ?: ""), style = TextStyle(color = MW.Dim, fontSize = 13.sp))
                }
                BigButton("Open record", icon = "edit") { val x = r; view = null; onEdit(x) }
                GhostButton("Close", Modifier.fillMaxWidth()) { view = null }
            }
        }
    }
}
