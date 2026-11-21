package com.example.mywallet

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.data.Store
import com.example.mywallet.ui.GlassCard
import com.example.mywallet.ui.HomeScreen
import com.example.mywallet.ui.MW
import com.example.mywallet.ui.MwShape
import com.example.mywallet.ui.MwTs
import com.example.mywallet.ui.SceneBackground

/** The new native MyWallet. Stage 1 = read-only Home. Your old WebView app still works next to it. */
class NativeActivity : androidx.fragment.app.FragmentActivity() {
    private lateinit var store: Store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // the keyboard slides over the screen instead of resizing it, so nothing jumps
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        store = Store(applicationContext)
        LaunchBus.add = intent?.getStringExtra("add")
        store.load()
        if (store.state != null) store.runRecurring()
        locked = store.lockOn()
        if (store.settingStr("secure") == "true") window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        if (store.state != null) GentleReminders.ensure(applicationContext)
        store.state?.let { com.example.mywallet.ui.MwTheme.apply(it.settings.palette, it.settings.pal); com.example.mywallet.ui.MwStyle.apply(it.settings.uiTheme, it.settings.catCoat) }
        setContent {
            val s = store.state
            androidx.compose.runtime.LaunchedEffect(LaunchBus.lockNow) { if (LaunchBus.lockNow) { LaunchBus.lockNow = false; if (store.lockOn()) locked = true } }
            if (s == null) ImportScreen(store)
            else if (locked) com.example.mywallet.ui.LockScreen(store) { locked = false }
            else HomeScreen(s, store)
        }
    }

    // lock again when you come back after being away (a short camera or file-picker trip does not lock)
    private var locked by androidx.compose.runtime.mutableStateOf(false)
    private var leftAt = 0L

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        LaunchBus.add = intent.getStringExtra("add")
    }

    override fun onStop() { super.onStop(); leftAt = System.currentTimeMillis(); store.flush() }

    override fun onStart() {
        super.onStart()
        if (leftAt > 0 && store.state != null) {
            if (store.lockOn() && System.currentTimeMillis() - leftAt > 30_000) locked = true
            store.runRecurring()
            store.touch()    // a new day may have started
        }
    }
}

@Composable
private fun ImportScreen(store: Store) {
    val ctx = LocalContext.current
    var pw by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val bytes = try { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } } catch (e: Exception) { null }
            val err = if (bytes == null) "Could not open that file." else store.importBackupBytes(bytes, pw).let { e ->
                when (e) { "NEED_PASSWORD" -> "This backup has a password. Type it in the box and choose the file again."; "WRONG_PASSWORD" -> "That password is not right."; else -> e }
            }
            if (err != null) Toast.makeText(ctx, err, Toast.LENGTH_LONG).show()
        }
    }
    Box(Modifier.fillMaxSize().background(MW.Bg)) {
        SceneBackground(0f)
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
            GlassCard(Modifier.fillMaxWidth(), radius = 28.dp, padding = 24.dp) {
                Text("Welcome to the new MyWallet", style = MwTs(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold))
                Spacer(Modifier.height(10.dp))
                Text(
                    "Pick a MyWallet backup file and all your records come across. Or start with a clean, empty wallet.",
                    style = MwTs(color = MW.Dim, fontSize = 14.sp, lineHeight = 20.sp),
                )
                Spacer(Modifier.height(14.dp))
                com.example.mywallet.ui.FText("Backup password (only if you set one)", pw, { pw = it }, "Leave empty if none")
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier.fillMaxWidth().height(52.dp).clip(MwShape(26.dp))
                        .background(Brush.linearGradient(listOf(MW.Mint, MW.Accent)))
                        .clickable { pick.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    contentAlignment = Alignment.Center,
                ) { Text("Choose backup file", style = MwTs(color = MW.OnAccent, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)) }
                Spacer(Modifier.height(12.dp))
                com.example.mywallet.ui.GhostButton("Start fresh", Modifier.fillMaxWidth()) { store.startFresh() }
            }
        }
    }
}
