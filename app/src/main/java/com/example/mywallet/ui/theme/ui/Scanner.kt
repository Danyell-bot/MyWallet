package com.example.mywallet.ui

import android.graphics.BitmapFactory
import android.util.Base64
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** what the scanner could read from a receipt */
class ScanResult(val total: Double?, val merchant: String?)

private val AMOUNT = Regex("""(?<![\d.,])(\d{1,3}(?:,\d{3})+|\d+)\.(\d{2})(?!\d)""")

private fun amountsIn(line: String): List<Double> =
    AMOUNT.findAll(line).mapNotNull { m -> (m.groupValues[1].replace(",", "") + "." + m.groupValues[2]).toDoubleOrNull() }.toList()

/** pure text rules: prefer the number on a "total" line, otherwise the biggest amount on the receipt */
fun parseReceipt(text: String): ScanResult {
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val keys = listOf("grand total", "total due", "amount due", "balance due", "to pay", "total", "amount")
    val cands = ArrayList<Double>()
    lines.forEachIndexed { i, l ->
        val lo = l.lowercase()
        if (keys.any { lo.contains(it) } && !lo.contains("subtotal") && !lo.contains("sub total") && !lo.contains("vatable") && !lo.contains("change")) {
            val here = amountsIn(l)
            if (here.isNotEmpty()) cands.addAll(here) else lines.getOrNull(i + 1)?.let { cands.addAll(amountsIn(it)) }
        }
    }
    val all = lines.flatMap { amountsIn(it) }
    val total = (if (cands.isNotEmpty()) cands.maxOrNull() else all.maxOrNull())
    val merchant = lines.firstOrNull { l -> val letters = l.count { it.isLetter() }; letters >= 3 && l.count { it.isDigit() } <= letters / 2 }?.take(28)
    return ScanResult(total, merchant)
}

/** reads the receipt photo on the phone (no internet needed) and reports what it found */
fun scanReceipt(dataUrl: String, onDone: (ScanResult?) -> Unit) {
    try {
        val bytes = Base64.decode(dataUrl.substringAfter("base64,"), Base64.DEFAULT)
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return onDone(null)
        val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        client.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { t -> onDone(parseReceipt(t.text)) }
            .addOnFailureListener { onDone(null) }
    } catch (e: Throwable) { onDone(null) }
}
