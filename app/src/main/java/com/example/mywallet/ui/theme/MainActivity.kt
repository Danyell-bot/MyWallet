package com.example.mywallet

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.provider.MediaStore
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.webkit.WebViewAssetLoader
import android.widget.FrameLayout
import android.content.ContentValues
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Environment
import android.provider.Settings
import android.graphics.drawable.ColorDrawable

private var printWv: WebView? = null

class MainActivity : FragmentActivity() {

    private lateinit var web: WebView
    private lateinit var root: FrameLayout
    private var chooserCb: ValueCallback<Array<Uri>>? = null
    private var pendingSave: String? = null

    // Lets the web page open the phone's file picker (restore backup, receipt photos).
    private val chooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        chooserCb?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(r.resultCode, r.data))
        chooserCb = null
    }

    // Receipt photos: camera and gallery are handled natively, then the picture is sent to the page.
    private var cameraUri: Uri? = null
    private val takePic = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = cameraUri
        cameraUri = null
        if (ok && u != null) deliverPhoto(u) else js("window.__photo && window.__photo('')")
    }
    private val pickPic = registerForActivityResult(ActivityResultContracts.GetContent()) { u ->
        if (u != null) deliverPhoto(u) else js("window.__photo && window.__photo('')")
    }
    private val camPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) launchCamera() else photoFail("Camera permission is off. Open phone Settings > Apps > MyWallet > Permissions > Camera.")
    }
    private val startPerms = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    private fun photoFail(msg: String) {
        js("window.__photo && window.__photo('', " + org.json.JSONObject.quote(msg) + ")")
    }

    private fun granted(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun takePhoto() {
        if (granted(Manifest.permission.CAMERA)) launchCamera() else camPerm.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        try {
            val dir = java.io.File(cacheDir, "photos").apply { mkdirs() }
            val f = java.io.File(dir, "receipt_" + System.currentTimeMillis() + ".jpg")
            val uri = FileProvider.getUriForFile(this, "com.example.mywallet.files", f)
            cameraUri = uri
            takePic.launch(uri)
        } catch (e: Exception) {
            photoFail("Camera error: " + e.javaClass.simpleName + " " + (e.message ?: ""))
        }
    }

    // Shrinks the picture (max 800px, upright) and sends it to the page as a data URL.
    private fun deliverPhoto(uri: Uri) {
        Thread {
            var out = ""
            var err = ""
            try {
                val bytes = contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
                var k = 1
                while (maxOf(o.outWidth, o.outHeight) / k > 1600) k *= 2
                var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = k })!!
                val deg = when (ExifInterface(java.io.ByteArrayInputStream(bytes))
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                if (deg != 0f) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, true)
                val sc = minOf(1f, 800f / maxOf(bmp.width, bmp.height))
                if (sc < 1f) bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * sc).toInt(), (bmp.height * sc).toInt(), true)
                val bo = java.io.ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 70, bo)
                out = "data:image/jpeg;base64," + android.util.Base64.encodeToString(bo.toByteArray(), android.util.Base64.NO_WRAP)
            } catch (e: Throwable) { err = "Photo error: " + e.javaClass.simpleName + " " + (e.message ?: "") }
            if (out.isEmpty()) photoFail(err) else js("window.__photo && window.__photo('$out')")
        }.start()
    }

    // Asks for notifications and camera once, the first time the app opens.
    private fun askPermissionsOnce() {
        val p = Reminders.prefs(this)
        if (p.getBoolean("perm_asked", false)) return
        p.edit().putBoolean("perm_asked", true).apply()
        val need = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33 && !granted(Manifest.permission.POST_NOTIFICATIONS)) need += Manifest.permission.POST_NOTIFICATIONS
        if (!granted(Manifest.permission.CAMERA)) need += Manifest.permission.CAMERA
        if (need.isNotEmpty()) startPerms.launch(need.toTypedArray())
    }

    // Saves straight into the phone's Downloads folder (Android 10+). Returns false if it cannot.
    private fun saveToDownloads(name: String, mime: String, text: String): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        return try {
            val v = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, if (mime.isBlank()) "text/plain" else mime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val u = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: return false
            contentResolver.openOutputStream(u)?.use { it.write(text.toByteArray(Charsets.UTF_8)) } ?: return false
            true
        } catch (e: Exception) { false }
    }

    // Lets the web page save files (backup, CSV) wherever the user picks.
    private val saver = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val uri = r.data?.data
        val text = pendingSave
        pendingSave = null
        var ok = false
        if (r.resultCode == RESULT_OK && uri != null && text != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                ok = true
            } catch (e: Exception) { }
        }
        js("window.__saved && window.__saved($ok, 'picker')")
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MyWallet) // leave the splash theme
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        val night = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val first = Color.parseColor(if (night) "#0E1515" else "#ECF2F2")
        window.setBackgroundDrawable(ColorDrawable(first))
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }

        root = FrameLayout(this)
        root.setBackgroundColor(first)
        web = WebView(this)
        web.setBackgroundColor(first)
        root.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)

        // Keep the page out from under the status bar, nav bar and keyboard.
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val i = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(i.left, i.top, i.right, i.bottom)
            WindowInsetsCompat.CONSUMED
        }

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        web.overScrollMode = android.view.View.OVER_SCROLL_NEVER
        web.addJavascriptInterface(Bridge(), "Android")
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)

            // Only our own page may load inside the app. Anything else is ignored.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                request.url.host != "appassets.androidplatform.net"
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                w: WebView, cb: ValueCallback<Array<Uri>>, p: FileChooserParams
            ): Boolean {
                chooserCb?.onReceiveValue(null)
                chooserCb = cb
                return try {
                    chooser.launch(p.createIntent())
                    true
                } catch (e: Exception) {
                    chooserCb = null; cb.onReceiveValue(null); false
                }
            }
        }

        // Back button: let the page close a sheet or go up a screen first.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("(window.__back && window.__back())") { res ->
                    if (res != "true") finish()
                }
            }
        })

        val add = if (savedInstanceState == null) intent?.getStringExtra("add") else null
        val url = "https://appassets.androidplatform.net/assets/index.html" +
            (if (add != null) "?add=$add" else "")
        if (savedInstanceState != null) web.restoreState(savedInstanceState) else web.loadUrl(url)
        Reminders.schedule(this)
        if (savedInstanceState == null) askPermissionsOnce()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    // The app is already open and a shortcut or widget button was tapped.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("add")?.let { js("window.openAdd && window.openAdd('$it')") }
    }

    override fun onStop() {
        super.onStop()
        js("window.__hidAt = Date.now()")
    }

    override fun onStart() {
        super.onStart()
        js("window.__resumed && window.__resumed()")
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }

    private fun js(code: String) {
        if (::web.isInitialized) web.post { web.evaluateJavascript(code, null) }
    }

    /** Everything the web page can ask the phone to do. The page calls these as window.Android.xxx */
    inner class Bridge {

        @JavascriptInterface
        fun haptic(ms: Int) {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            val d = ms.toLong().coerceIn(5, 80)
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(d, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") v.vibrate(d)
        }

        @JavascriptInterface
        fun setBars(bg: String, dark: Boolean) {
            runOnUiThread {
                try {
                    val c = Color.parseColor(bg)
                    root.setBackgroundColor(c)
                    window.setBackgroundDrawable(ColorDrawable(c))
                    web.setBackgroundColor(c)
                    val ctl = WindowCompat.getInsetsController(window, window.decorView)
                    ctl.isAppearanceLightStatusBars = !dark
                    ctl.isAppearanceLightNavigationBars = !dark
                } catch (e: Exception) { }
            }
        }

        @JavascriptInterface
        fun saveFile(name: String, mime: String, text: String) {
            if (saveToDownloads(name, mime, text)) { js("window.__saved && window.__saved(true, 'downloads')"); return }
            runOnUiThread {
                pendingSave = text
                val i = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = if (mime.isBlank()) "*/*" else mime
                    putExtra(Intent.EXTRA_TITLE, name)
                }
                try { saver.launch(i) } catch (e: Exception) {
                    pendingSave = null
                    shareBytes(name, if (mime.isBlank()) "text/plain" else mime, text.toByteArray(Charsets.UTF_8))
                }
            }
        }

        @JavascriptInterface
        fun pickPhoto(mode: String) {
            runOnUiThread {
                try { if (mode == "cam") takePhoto() else pickPic.launch("image/*") }
                catch (e: Exception) { photoFail("Photo error: " + e.javaClass.simpleName + " " + (e.message ?: "")) }
            }
        }

        @JavascriptInterface
        fun notifStatus(): String =
            if (androidx.core.app.NotificationManagerCompat.from(this@MainActivity).areNotificationsEnabled()) "ok" else "blocked"

        @JavascriptInterface
        fun openNotifSettings() {
            runOnUiThread {
                try {
                    startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                } catch (e: Exception) { }
            }
        }

        @JavascriptInterface
        fun printHtml(html: String, title: String) {
            runOnUiThread {
                val wv = WebView(this@MainActivity)
                printWv = wv
                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        val pm = getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                        val name = if (title.isBlank()) "MyWallet" else title
                        pm.print(name, view.createPrintDocumentAdapter(name), android.print.PrintAttributes.Builder().build())
                    }
                }
                wv.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }

        @JavascriptInterface
        fun testNotification() {
            runOnUiThread {
                if (Build.VERSION.SDK_INT >= 33 && !granted(Manifest.permission.POST_NOTIFICATIONS)) {
                    startPerms.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    js("window.toast && window.toast('Allow notifications, then tap the test button again')")
                } else if (!androidx.core.app.NotificationManagerCompat.from(this@MainActivity).areNotificationsEnabled()) {
                    js("window.toast && window.toast('Notifications are off for MyWallet. Use Open notification settings.')")
                } else {
                    Reminders.show(this@MainActivity, "This is a test. Your reminders will look like this.")
                }
            }
        }

        @JavascriptInterface
        fun biometricAvailable(): Boolean =
            BiometricManager.from(this@MainActivity)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

        // Opens the Android share sheet so the user can pick Google Drive, Messenger, email, etc.
        private fun shareBytes(name: String, mime: String, bytes: ByteArray) {
            try {
                val dir = java.io.File(cacheDir, "photos").apply { mkdirs() }
                val f = java.io.File(dir, name.replace(Regex("[^A-Za-z0-9._-]"), "_"))
                f.writeBytes(bytes)
                val uri = FileProvider.getUriForFile(this@MainActivity, "com.example.mywallet.files", f)
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runOnUiThread { startActivity(Intent.createChooser(i, "Share")) }
            } catch (e: Exception) {
                js("window.toast && window.toast('Could not share the file')")
            }
        }

        @JavascriptInterface
        fun shareFile(name: String, mime: String, text: String) = shareBytes(name, mime, text.toByteArray(Charsets.UTF_8))

        @JavascriptInterface
        fun shareImage(base64: String, name: String) =
            shareBytes(name, "image/png", android.util.Base64.decode(base64, android.util.Base64.DEFAULT))

        // Reads the text on a receipt photo, on the phone itself (no internet, no account).
        @JavascriptInterface
        fun recognize(base64: String) {
            try {
                val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bmp, 0)
                val rec = com.google.mlkit.vision.text.TextRecognition.getClient(
                    com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
                rec.process(image)
                    .addOnSuccessListener { js("window.__ocr && window.__ocr(" + org.json.JSONObject.quote(it.text) + ")") }
                    .addOnFailureListener { js("window.__ocr && window.__ocr('')") }
            } catch (e: Exception) {
                js("window.__ocr && window.__ocr('')")
            }
        }

        @JavascriptInterface
        fun biometricStatus(): String =
            when (BiometricManager.from(this@MainActivity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                BiometricManager.BIOMETRIC_SUCCESS -> "ok"
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "none_enrolled"
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "no_hardware"
                else -> "unavailable:" + BiometricManager.from(this@MainActivity)
                    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            }

        // Hides the app preview in recent apps and blocks screenshots while the lock is on.
        @JavascriptInterface
        fun setSecure(on: Boolean) {
            runOnUiThread {
                if (on) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        // Proves it is the phone owner using the phone's own screen lock (PIN, pattern or password).
        @JavascriptInterface
        fun deviceAuth() {
            runOnUiThread {
                val can = BiometricManager.from(this@MainActivity)
                    .canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                if (can != BiometricManager.BIOMETRIC_SUCCESS) {
                    js("window.__devAuth && window.__devAuth(false, 'no_lock')")
                    return@runOnUiThread
                }
                val prompt = BiometricPrompt(
                    this@MainActivity, ContextCompat.getMainExecutor(this@MainActivity),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            js("window.__devAuth && window.__devAuth(true)")
                        }
                        override fun onAuthenticationError(code: Int, msg: CharSequence) {
                            js("window.__devAuth && window.__devAuth(false, 'error')")
                        }
                    })
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Confirm it's you")
                    .setSubtitle("Use your phone's screen lock to reset the passcode")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()
                prompt.authenticate(info)
            }
        }

        @JavascriptInterface
        fun biometric() {
            runOnUiThread {
                val prompt = BiometricPrompt(
                    this@MainActivity, ContextCompat.getMainExecutor(this@MainActivity),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            js("window.__bio && window.__bio(true)")
                        }
                        override fun onAuthenticationError(code: Int, msg: CharSequence) {
                            js("window.__bio && window.__bio(false, " + org.json.JSONObject.quote(msg.toString()) + ")")
                        }
                    })
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock MyWallet")
                    .setNegativeButtonText("Use passcode")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build()
                prompt.authenticate(info)
            }
        }

        @JavascriptInterface
        fun setWidget(json: String) {
            val o = org.json.JSONObject(json)
            Reminders.prefs(this@MainActivity).edit()
                .putString("w_total", o.optString("total"))
                .putString("w_safe", o.optString("safe"))
                .putBoolean("w_hide", o.optBoolean("hide"))
                .apply()
            IponWidget.refresh(this@MainActivity)
        }

        @JavascriptInterface
        fun setReminders(enabled: Boolean, hour: Int, scheduleJson: String) {
            Reminders.apply(this@MainActivity, enabled, hour, scheduleJson)
        }

        @JavascriptInterface
        fun requestNotif() {
            runOnUiThread {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
            }
        }
    }
}
