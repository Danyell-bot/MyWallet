package com.example.mywallet.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Single source of truth = the raw JSON from your backup (`root`). The screens read a parsed copy
 * (`state`). Every change edits `root` directly, so fields this app does not know about
 * (receipt photos, badges, dev settings, ...) are never lost and can be exported back to the web app.
 */
class Store(private val context: Context) {
    private val file = File(context.filesDir, "mywallet-v13.json")
    private val bak = File(context.filesDir, "mywallet-v13.json.bak")
    private val photoDir = File(context.filesDir, "photos")
    private var root: JsonObject? = null
    var state: AppState? by mutableStateOf(null)
        private set

    /* ---- saving: in the background, compressed and encrypted, always keeping the previous version ---- */
    private val io = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val pending = java.util.concurrent.atomic.AtomicReference<JsonObject?>(null)

    private fun saveLater(r: JsonObject) { pending.set(r); io.execute { pending.getAndSet(null)?.let { writeNow(it) } } }

    private fun writeNow(r: JsonObject) {
        try {
            val bytes = Vault.seal(r.toString().toByteArray(Charsets.UTF_8))
            val tmp = File(context.filesDir, "mywallet-v13.json.tmp")
            tmp.writeBytes(bytes)
            if (file.exists()) { bak.delete(); file.renameTo(bak) }
            if (!tmp.renameTo(file)) { file.writeBytes(bytes); tmp.delete() }
        } catch (e: Exception) { }
    }

    /** wait until everything is saved (call when the app goes to the background) */
    fun flush() { try { io.submit { }.get(4, java.util.concurrent.TimeUnit.SECONDS) } catch (e: Exception) { } }

    /* ---- receipt photos: one small encrypted file each, outside the data file ---- */
    private val photoIds = HashSet<String>()
    private fun safeId(id: String) = id.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
    private fun pf(id: String) = File(photoDir, safeId(id) + ".p")

    private fun scanPhotos() {
        photoIds.clear()
        photoDir.listFiles()?.forEach { if (it.name.endsWith(".p")) photoIds.add(it.name.removeSuffix(".p")) }
    }

    private fun savePhotoFromUrl(id: String, url: String) {
        try {
            val bytes = android.util.Base64.decode(url.substringAfter("base64,"), android.util.Base64.DEFAULT)
            photoDir.mkdirs()
            pf(id).writeBytes(Vault.seal(bytes, compress = false))
            photoIds.add(safeId(id))
        } catch (e: Exception) { }
    }

    fun hasPhoto(id: String): Boolean = photoIds.contains(safeId(id))

    fun photoBytes(id: String): ByteArray? = try { if (hasPhoto(id)) Vault.open(pf(id).readBytes()) else null } catch (e: Exception) { null }

    /** receipt photo as a data URL, or "" */
    fun photoOf(id: String): String =
        photoBytes(id)?.let { "data:image/jpeg;base64," + android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) } ?: ""

    /** set (or with "" remove) the receipt photo of a record */
    fun setPhoto(id: String, dataUrl: String) {
        if (dataUrl.isEmpty()) {
            if (!hasPhoto(id)) return
            pf(id).delete(); photoIds.remove(safeId(id))
        } else {
            // same picture as before? nothing to do
            savePhotoFromUrl(id, dataUrl)
        }
        touch()
    }

    /** remove photos whose record no longer exists. Returns how many were removed. */
    fun cleanPhotos(): Int {
        val ids = state?.recs?.map { safeId(it.id) }?.toSet() ?: return 0
        var n = 0
        photoDir.listFiles()?.forEach { f ->
            if (f.name.endsWith(".p") && !ids.contains(f.name.removeSuffix(".p"))) { if (f.delete()) n++ }
        }
        scanPhotos()
        return n
    }

    fun photoCount(): Int = photoIds.size
    fun photoBytesTotal(): Long = photoDir.listFiles()?.sumOf { it.length() } ?: 0L
    fun fileSize(): Long = if (file.exists()) file.length() else 0L

    private fun readFile(f: File): JsonObject? {
        return try {
            if (!f.exists()) return null
            val text = Vault.open(f.readBytes())?.toString(Charsets.UTF_8) ?: return null
            AppJson.parseToJsonElement(text) as? JsonObject
        } catch (e: Exception) { null }
    }

    fun load() {
        var plain = false
        var r = readFile(file)
        if (r != null) plain = try { file.inputStream().use { it.read() } == '{'.code } catch (e: Exception) { false }
        if (r == null) r = readFile(bak)
        if (r == null) return
        try {
            scanPhotos()
            // older versions kept receipt photos inside the data file: move them out
            val ph = r["photos"] as? JsonObject
            if (ph != null) {
                ph.forEach { (id, v) -> (v as? JsonPrimitive)?.takeIf { it.isString }?.let { savePhotoFromUrl(id, it.content) } }
                r = JsonObject(r - "photos")
                plain = true
            }
            root = r
            val st = AppJson.decodeFromJsonElement(AppState.serializer(), r)
            state = st
            if (plain) { writeNow(r); writeNow(r) }   // now encrypted, and the plain copy is rotated out
            pushWidget(st)
        } catch (e: Exception) { root = null; state = null }
    }

    /** Returns null on success, or a short message to show the user. */
    fun importBackup(text: String): String? = applyBackup(text.trim().removePrefix("\uFEFF"))

    /** reads backup files made by this app (compressed, optionally password protected) and plain .json backups */
    fun importBackupBytes(data: ByteArray, password: String?): String? {
        var b = data
        if (BackupCrypto.isEncrypted(b)) {
            if (password.isNullOrEmpty()) return "NEED_PASSWORD"
            b = BackupCrypto.open(b, password) ?: return "WRONG_PASSWORD"
        } else if (isGzip(b)) b = try { gunzipBytes(b) } catch (e: Exception) { return "That is not a valid backup file." }
        return applyBackup(b.toString(Charsets.UTF_8).trim().removePrefix("\uFEFF"))
    }

    private fun applyBackup(clean: String): String? {
        val r = try { AppJson.parseToJsonElement(clean) as? JsonObject } catch (e: Exception) { null }
            ?: return "That is not a valid backup file."
        if (r["accounts"] !is JsonArray || r["recs"] !is JsonArray || r["cats"] !is JsonArray)
            return "That is not a MyWallet backup."
        val nr = JsonObject(r - "photos")
        val parsed = try { AppJson.decodeFromJsonElement(AppState.serializer(), nr) } catch (e: Exception) {
            return "Could not read this backup: " + (e.message ?: "unknown error").take(120)
        }
        photoDir.listFiles()?.forEach { it.delete() }
        photoIds.clear()
        (r["photos"] as? JsonObject)?.forEach { (id, v) -> (v as? JsonPrimitive)?.takeIf { it.isString }?.let { savePhotoFromUrl(id, it.content) } }
        writeNow(nr)
        root = nr
        state = parsed
        pushWidget(parsed)
        return null
    }

    /** the data without receipt photos, as text (developer tools) */
    fun exportJson(): String? = root?.toString()

    /** a full backup file: data + receipt photos, compressed, and locked with the password if one is given */
    fun backupBytes(password: String?): ByteArray {
        val r = root ?: return ByteArray(0)
        val out = ByteArrayOutputStream()
        val head = r.toString()
        out.write(head.substring(0, head.length - 1).toByteArray(Charsets.UTF_8))
        if (r.isNotEmpty()) out.write(','.code)
        out.write("\"photos\":{".toByteArray(Charsets.UTF_8))
        var first = true
        photoIds.forEach { id ->
            val b = photoBytes(id) ?: return@forEach
            if (!first) out.write(','.code)
            first = false
            out.write(JsonPrimitive(id).toString().toByteArray(Charsets.UTF_8))
            out.write(":\"data:image/jpeg;base64,".toByteArray(Charsets.UTF_8))
            out.write(android.util.Base64.encodeToString(b, android.util.Base64.NO_WRAP).toByteArray(Charsets.US_ASCII))
            out.write('"'.code)
        }
        out.write("}}".toByteArray(Charsets.UTF_8))
        return BackupCrypto.seal(out.toByteArray(), password)
    }

    private fun commit(newRoot: JsonObject) {
        val parsed = AppJson.decodeFromJsonElement(AppState.serializer(), newRoot)
        root = newRoot
        state = parsed
        saveLater(newRoot)
        pushWidget(parsed)
    }

    /** a brand new, empty MyWallet (Cash + Savings accounts, standard categories, two quick-add favourites) */
    fun startFresh(cur: String = "\u20B1") {
        val t = todayStr()
        fun cat(id: String, n: String, i: String, type: String) = kotlinx.serialization.json.buildJsonObject {
            put("id", id); put("name", n); put("icon", i); put("type", type); put("ignored", false)
        }
        val ex = listOf("Food" to "\uD83C\uDF5C", "Transport" to "\uD83D\uDE8C", "Bills" to "\uD83E\uDDFE", "Shopping" to "\uD83D\uDECD\uFE0F", "School" to "\uD83D\uDCDA",
            "Health" to "\uD83D\uDC8A", "Phone & data" to "\uD83D\uDCF1", "Fun" to "\uD83C\uDFAE", "Bank charges" to "\uD83C\uDFE6", "Other" to "\uD83E\uDDE9")
        val inc = listOf("Allowance" to "\uD83D\uDCB5", "Salary" to "\uD83D\uDCBC", "Gift" to "\uD83C\uDF81", "Bank interest" to "\uD83E\uDE99", "Other" to "\uD83E\uDDE9")
        val r = kotlinx.serialization.json.buildJsonObject {
            put("v", 1); put("sample", false)
            put("settings", kotlinx.serialization.json.buildJsonObject { put("cur", cur); put("pos", "start"); put("dec", 2); put("theme", "auto"); put("carry", true) })
            put("accounts", JsonArray(listOf(
                kotlinx.serialization.json.buildJsonObject { put("id", "a1"); put("name", "Cash"); put("icon", "\uD83D\uDCB5"); put("kind", "cash"); put("initial", 0); put("openDate", t); put("ignored", false) },
                kotlinx.serialization.json.buildJsonObject {
                    put("id", "a2"); put("name", "Savings"); put("icon", "\uD83C\uDFE6"); put("kind", "bank"); put("initial", 0); put("openDate", t); put("ignored", false)
                    put("bank", kotlinx.serialization.json.buildJsonObject { put("bankName", ""); put("rule", "avg"); put("min", 500); put("fee", 200); put("period", "calendar"); put("startDay", 1); put("interest", 0); put("otherFees", JsonArray(emptyList())) })
                },
            )))
            put("cats", JsonArray(ex.map { cat("e_" + it.first, it.first, it.second, "expense") } + inc.map { cat("i_" + it.first, it.first, it.second, "income") }))
            put("recs", JsonArray(emptyList())); put("budgets", JsonObject(emptyMap())); put("profiles", JsonArray(emptyList()))
            put("favs", JsonArray(listOf(
                kotlinx.serialization.json.buildJsonObject { put("id", "f1"); put("name", "Lunch"); put("icon", "\uD83C\uDF5C"); put("type", "expense"); put("cat", "e_Food"); put("acct", "a1"); put("amount", 80) },
                kotlinx.serialization.json.buildJsonObject { put("id", "f2"); put("name", "Bus"); put("icon", "\uD83D\uDE8C"); put("type", "expense"); put("cat", "e_Transport"); put("acct", "a1"); put("amount", 13) },
            )))
        }
        photoDir.listFiles()?.forEach { it.delete() }; photoIds.clear()
        writeNow(r)
        root = r
        val st = AppJson.decodeFromJsonElement(AppState.serializer(), r)
        state = st
        pushWidget(st)
    }

    /** delete everything this app stores (data file, its backup copy, receipt photos) */
    fun resetAll() {
        io.submit { }.get(4, java.util.concurrent.TimeUnit.SECONDS)
        pending.set(null)
        file.delete(); bak.delete()
        photoDir.listFiles()?.forEach { it.delete() }; photoIds.clear()
        root = null
        state = null
    }

    /** redraw everything (time travel, a new day, a changed photo) */
    fun touch() { state = state?.let { it.copy(rev = it.rev + 1) } }

    private fun pushWidget(s: AppState) {
        try {
            val total = s.accounts.filter { !it.ignored }.sumOf { s.balBase(it) }
            com.example.mywallet.MwWidget.push(context, s.money(total), s.money(s.safeToday().amount), s.settings.hide)
        } catch (e: Exception) { }
    }

    private fun recsOf(r: JsonObject): List<JsonElement> = (r["recs"] as? JsonArray) ?: emptyList()

    fun newId(): String = "n" + System.currentTimeMillis().toString(36) + (100..999).random()

    fun addRec(rec: JsonObject) {
        val r = root ?: return
        commit(JsonObject(r + ("recs" to JsonArray(recsOf(r) + rec))))
    }

    /** merge `patch` into the record with this id, dropping keys in `remove` */
    fun updateRec(id: String, patch: JsonObject, remove: Set<String> = emptySet()) {
        val r = root ?: return
        val list = recsOf(r).map { e ->
            val o = e as? JsonObject
            if (o != null && (o["id"] as? JsonPrimitive)?.content == id) JsonObject((o - remove) + patch) else e
        }
        commit(JsonObject(r + ("recs" to JsonArray(list))))
    }

    fun deleteRec(id: String) {
        val r = root ?: return
        val list = recsOf(r).filter { e -> (e as? JsonObject)?.get("id")?.jsonPrimitive?.content != id }
        commit(JsonObject(r + ("recs" to JsonArray(list))))
        if (hasPhoto(id)) { pf(id).delete(); photoIds.remove(safeId(id)) }
    }

    fun updateSettings(patch: Map<String, JsonElement>) {
        val r = root ?: return
        val cur = (r["settings"] as? JsonObject) ?: JsonObject(emptyMap())
        commit(JsonObject(r + ("settings" to JsonObject(cur + patch))))
    }

    fun addFav(fav: JsonObject) {
        val r = root ?: return
        val cur = (r["favs"] as? JsonArray) ?: JsonArray(emptyList())
        commit(JsonObject(r + ("favs" to JsonArray(cur + fav))))
    }

    /** like setTop, but the screens update right away */
    fun setTopLive(key: String, value: JsonElement) { val r = root ?: return; commit(JsonObject(r + (key to value))) }

    fun setTops(map: Map<String, JsonElement>) { val r = root ?: return; commit(JsonObject(r + map)) }

    fun removeTop(key: String) { val r = root ?: return; commit(JsonObject(r - key)) }

    /** add several items to several lists in one save, for example {"recs": [...], "cats": [...]} */
    fun appendLists(map: Map<String, List<JsonObject>>) {
        val r = root ?: return
        var nr = r
        map.forEach { (k, v) -> if (v.isNotEmpty()) nr = JsonObject(nr + (k to JsonArray(listAt(nr, k) + v))) }
        commit(nr)
    }

    /** replace a whole list (for example empty "recs") */
    fun setList(key: String, items: List<JsonElement>) { val r = root ?: return; commit(JsonObject(r + (key to JsonArray(items)))) }

    fun clearPhotos() { photoDir.listFiles()?.forEach { it.delete() }; photoIds.clear(); touch() }

    /** set one top-level field of the backup (for example `lastBackup`) */
    fun setTop(key: String, value: JsonElement) {
        val r = root ?: return
        val nr = JsonObject(r + (key to value))
        saveLater(nr)
        root = nr
    }

    /* ---- generic lists with an `id` (bills, debts, goals, cats, favs, accounts) ---- */
    private fun listAt(r: JsonObject, key: String): List<JsonElement> = (r[key] as? JsonArray) ?: emptyList()

    fun addItem(key: String, item: JsonObject) {
        val r = root ?: return
        commit(JsonObject(r + (key to JsonArray(listAt(r, key) + item))))
    }

    fun updateItem(key: String, id: String, patch: JsonObject, remove: Set<String> = emptySet()) {
        val r = root ?: return
        val list = listAt(r, key).map { e ->
            val o = e as? JsonObject
            if (o != null && (o["id"] as? JsonPrimitive)?.content == id) JsonObject((o - remove) + patch) else e
        }
        commit(JsonObject(r + (key to JsonArray(list))))
    }

    fun deleteItem(key: String, id: String) {
        val r = root ?: return
        val list = listAt(r, key).filter { e -> (e as? JsonObject)?.get("id")?.jsonPrimitive?.content != id }
        commit(JsonObject(r + (key to JsonArray(list))))
    }

    /* ---- settings that live only in the raw JSON (lock, passcode, ...) ---- */
    fun settingStr(key: String): String = ((root?.get("settings") as? JsonObject)?.get(key) as? JsonPrimitive)?.content ?: ""
    fun lockOn(): Boolean = settingStr("lock") == "true" && settingStr("passHash").isNotEmpty()

    /** old passcode hash (web app): sha256("ipon:" + pin) in hex. Only used to read old passcodes. */
    fun hashPin(pin: String): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(("ipon:" + pin).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun slowHash(pin: String, salt: String): String {
        val f = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return f.generateSecret(javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt.toByteArray(Charsets.UTF_8), 120_000, 256)).encoded.joinToString("") { "%02x".format(it) }
    }

    /** the settings to save for a new passcode: a random salt and a slow hash */
    fun pinSettings(pin: String): Map<String, JsonElement> {
        val salt = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        return mapOf("passHash" to JsonPrimitive(slowHash(pin, salt)), "passSalt" to JsonPrimitive(salt), "lock" to JsonPrimitive(true))
    }

    private val prefs get() = context.getSharedPreferences("mw_prefs", 0)

    /** seconds left before another passcode try is allowed (after 5 wrong tries the wait grows) */
    fun lockWaitSec(): Long = maxOf(0L, (prefs.getLong("pin_until", 0L) - System.currentTimeMillis() + 999) / 1000)

    fun checkPin(pin: String): Boolean {
        if (lockWaitSec() > 0) return false
        val salt = settingStr("passSalt")
        val stored = settingStr("passHash")
        val ok = if (salt.isEmpty()) hashPin(pin) == stored else java.security.MessageDigest.isEqual(slowHash(pin, salt).toByteArray(), stored.toByteArray())
        if (ok) {
            prefs.edit().putInt("pin_fails", 0).putLong("pin_until", 0L).apply()
            if (salt.isEmpty()) updateSettings(pinSettings(pin))    // old passcode: upgrade it silently
        } else {
            val n = prefs.getInt("pin_fails", 0) + 1
            val wait = if (n >= 5) minOf(900_000L, 30_000L shl minOf(n - 5, 5)) else 0L
            prefs.edit().putInt("pin_fails", n).putLong("pin_until", if (wait > 0) System.currentTimeMillis() + wait else 0L).apply()
        }
        return ok
    }

    /* ---- recurring records ---- */
    private fun dueOn(r: JsonObject, d: Long): Boolean {
        val freq = (r["freq"] as? JsonPrimitive)?.content ?: "monthly"
        val (y, m, day) = civilFromDays(d)
        val dim = daysInMonth(y, m - 1)
        fun n(k: String, def: Long) = (r[k] as? JsonPrimitive)?.content?.toDoubleOrNull()?.toLong() ?: def
        return when (freq) {
            "weekly" -> Math.floorMod(d + 4, 7L) == n("wd", 1)
            "twice" -> day == minOf(n("d1", 1), dim) || day == minOf(n("d2", 15), dim)
            else -> day == minOf(n("d1", 1), dim)
        }
    }

    /** adds every recurring record that came due since last time. Returns how many were added. */
    fun runRecurring(): Int {
        val r = root ?: return 0
        val rec = listAt(r, "recurring")
        if (rec.isEmpty()) return 0
        val accIds = listAt(r, "accounts").mapNotNull { ((it as? JsonObject)?.get("id") as? JsonPrimitive)?.content }.toSet()
        val t = todayDn()
        val added = mutableListOf<JsonElement>()
        val now = System.currentTimeMillis()
        val updated = rec.map { e ->
            val o = e as? JsonObject ?: return@map e
            val acct = (o["acct"] as? JsonPrimitive)?.content ?: ""
            if (acct !in accIds) return@map e
            val last = (o["last"] as? JsonPrimitive)?.content ?: todayStr()
            for (d in maxOf(dn(last) + 1, t - 400)..t) {
                if (dueOn(o, d)) added.add(JsonObject(mapOf(
                    "id" to JsonPrimitive(newId()), "ts" to JsonPrimitive(now + added.size),
                    "type" to (o["type"] ?: JsonPrimitive("expense")), "acct" to JsonPrimitive(acct), "to" to JsonPrimitive(""),
                    "cat" to (o["cat"] ?: JsonPrimitive("")), "amount" to (o["amount"] ?: JsonPrimitive(0)),
                    "date" to JsonPrimitive(fromDn(d)), "note" to (o["name"] ?: JsonPrimitive("")), "rid" to (o["id"] ?: JsonPrimitive("")),
                )))
            }
            JsonObject(o + ("last" to JsonPrimitive(fromDn(t))))
        }
        commit(JsonObject(r + ("recurring" to JsonArray(updated)) + ("recs" to JsonArray(recsOf(r) + added))))
        return added.size
    }

    /** delete an account like the web app: its records go, transfers are turned into plain income/expense on the other side */
    fun deleteAccount(id: String) {
        val r = root ?: return
        fun JsonObject.s(k: String) = (this[k] as? JsonPrimitive)?.content ?: ""
        val recs = mutableListOf<JsonElement>()
        for (e in listAt(r, "recs")) {
            val o = e as? JsonObject ?: continue
            if (o.s("type") != "transfer") { if (o.s("acct") != id) recs.add(o); continue }
            val note = o.s("note")
            if (o.s("acct") == id) {
                if (o.s("to").isEmpty()) continue
                var n = JsonObject(o + mapOf("type" to JsonPrimitive("income"), "acct" to JsonPrimitive(o.s("to")), "to" to JsonPrimitive(""),
                    "note" to JsonPrimitive((if (note.isNotEmpty()) "$note \u00B7 " else "") + "From a deleted account")))
                o["toAmount"]?.let { ta -> n = JsonObject((n - "toAmount") + ("amount" to ta)) }
                recs.add(n)
            } else if (o.s("to") == id) {
                recs.add(JsonObject(o + mapOf("type" to JsonPrimitive("expense"), "to" to JsonPrimitive(""),
                    "note" to JsonPrimitive((if (note.isNotEmpty()) "$note \u00B7 " else "") + "Sent to a deleted account"))))
            } else recs.add(o)
        }
        val accts = listAt(r, "accounts").filter { (it as? JsonObject)?.get("id")?.jsonPrimitive?.content != id }
        commit(JsonObject(r + ("recs" to JsonArray(recs)) + ("accounts" to JsonArray(accts))))
        cleanPhotos()
    }

    /** raw object of one list item (so unknown fields are kept when editing) */
    fun rawItem(key: String, id: String): JsonObject? =
        root?.let { listAt(it, key) }?.firstOrNull { (it as? JsonObject)?.get("id")?.jsonPrimitive?.content == id } as? JsonObject

    /** delete a category together with its records and budgets, like the web app */
    fun deleteCategory(id: String) {
        val r = root ?: return
        val recs = listAt(r, "recs").filter { (it as? JsonObject)?.get("cat")?.jsonPrimitive?.content != id }
        val cats = listAt(r, "cats").filter { (it as? JsonObject)?.get("id")?.jsonPrimitive?.content != id }
        val bud = (r["budgets"] as? JsonObject)?.let { b -> JsonObject(b.mapValues { (_, v) -> (v as? JsonObject)?.let { o -> JsonObject(o - id) } ?: v }) }
        var nr = r + ("recs" to JsonArray(recs)) + ("cats" to JsonArray(cats))
        if (bud != null) nr = nr + ("budgets" to bud)
        commit(JsonObject(nr))
    }

    /** mark a debt paid and book the matching record in one go */
    fun settleDebt(debtId: String, rec: JsonObject) {
        val r = root ?: return
        val debts = listAt(r, "debts").map { e ->
            val o = e as? JsonObject
            if (o != null && (o["id"] as? JsonPrimitive)?.content == debtId) JsonObject(o + ("paid" to kotlinx.serialization.json.JsonPrimitive(true))) else e
        }
        commit(JsonObject(r + ("debts" to JsonArray(debts)) + ("recs" to JsonArray(listAt(r, "recs") + rec))))
    }

    /** a part payment (or the last one) on a debt: books the record and remembers how much was paid */
    fun payDebt(debtId: String, amount: Double, date: String, rec: JsonObject) {
        val r = root ?: return
        val debts = listAt(r, "debts").map { e ->
            val o = e as? JsonObject
            if (o != null && (o["id"] as? JsonPrimitive)?.content == debtId) {
                val total = (o["amount"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
                val old = (o["pays"] as? JsonArray) ?: JsonArray(emptyList())
                val sum = old.sumOf { ((it as? JsonObject)?.get("amount") as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0 } + amount
                val pay = kotlinx.serialization.json.buildJsonObject { put("date", date); put("amount", amount) }
                JsonObject(o + ("pays" to JsonArray(old + pay)) + ("paid" to kotlinx.serialization.json.JsonPrimitive(sum >= total - 0.005)))
            } else e
        }
        commit(JsonObject(r + ("debts" to JsonArray(debts)) + ("recs" to JsonArray(listAt(r, "recs") + rec))))
    }

    /** pay a bill: book an expense and mark this period paid */
    fun payBill(billId: String, key: String, rec: JsonObject) {
        val r = root ?: return
        val bills = listAt(r, "bills").map { e ->
            val o = e as? JsonObject
            if (o != null && (o["id"] as? JsonPrimitive)?.content == billId) {
                val paid = (o["paid"] as? JsonArray) ?: JsonArray(emptyList())
                JsonObject(o + ("paid" to JsonArray(paid + kotlinx.serialization.json.JsonPrimitive(key))))
            } else e
        }
        commit(JsonObject(r + ("bills" to JsonArray(bills)) + ("recs" to JsonArray(listAt(r, "recs") + rec))))
    }

    /** set (or with null remove) one category limit for a month */
    fun setBudget(month: String, catId: String, limit: Double?) {
        val r = root ?: return
        val all = (r["budgets"] as? JsonObject) ?: JsonObject(emptyMap())
        val m = (all[month] as? JsonObject) ?: JsonObject(emptyMap())
        val nm = if (limit == null) m - catId else m + (catId to JsonPrimitive(limit))
        commit(JsonObject(r + ("budgets" to JsonObject(all + (month to JsonObject(nm))))))
    }

    /** copy all limits of one month to another. false when the source month has none */
    fun copyBudgets(from: String, to: String): Boolean {
        val r = root ?: return false
        val all = (r["budgets"] as? JsonObject) ?: JsonObject(emptyMap())
        val src = (all[from] as? JsonObject)?.takeIf { it.isNotEmpty() } ?: return false
        commit(JsonObject(r + ("budgets" to JsonObject(all + (to to src)))))
        return true
    }
}
