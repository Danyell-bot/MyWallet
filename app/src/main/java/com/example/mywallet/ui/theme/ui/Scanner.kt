package com.example.mywallet.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** what the scanner could read from a receipt (hadText = it saw some words, even if no total) */
class ScanResult(val total: Double?, val merchant: String?, val hadText: Boolean = true)

// 1,250.00  or  150.50  or  150,00
private val DECIMAL = Regex("""(?<![\d.,])(\d{1,3}(?:,\d{3})+|\d+)\s?[.,]\s?(\d{2})(?!\d)""")
// P150  /  PHP 1,250  /  ₱150  (OCR often turns the peso sign into P, R or F)
private val CURRENCY = Regex("""(?<![A-Za-z])(?:₱|PHP|Php|php|P|R|F)\s?(\d{1,3}(?:,\d{3})+|\d+)(?:\.(\d{1,2}))?(?![\d])""")
private val PLAIN = Regex("""(?<![\d.,])(\d{1,3}(?:,\d{3})+|\d{1,6})(?![\d.,])""")

private fun decimalsIn(line: String): List<Double> =
    DECIMAL.findAll(line).mapNotNull { m -> (m.groupValues[1].replace(",", "") + "." + m.groupValues[2]).toDoubleOrNull() }.toList()

private fun currencyIn(line: String): List<Double> =
    CURRENCY.findAll(line).mapNotNull { m -> (m.groupValues[1].replace(",", "") + (if (m.groupValues[2].isNotEmpty()) "." + m.groupValues[2] else "")).toDoubleOrNull() }.toList()

private fun plainIn(line: String): List<Double> =
    PLAIN.findAll(line).mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }.filter { it in 1.0..999999.0 }.toList()

private val KEYS = listOf("grand total", "total due", "amount due", "balance due", "to pay", "total", "amount", "you paid", "paid", "payment", "sent", "payable", "charge")
private val SKIP = listOf("subtotal", "sub total", "vatable", "change", "vat ", "reference", "ref no", "ref.", "account", "mobile", "number")

/** pure text rules: prefer the number on a "total" or "amount" line, otherwise the biggest amount on the receipt */
fun parseReceipt(text: String): ScanResult {
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val cands = ArrayList<Double>()
    lines.forEachIndexed { i, l ->
        val lo = l.lowercase()
        if (KEYS.any { lo.contains(it) } && SKIP.none { lo.contains(it) }) {
            var here = decimalsIn(l) + currencyIn(l)
            if (here.isEmpty()) here = plainIn(l.replace(Regex("(?i)(grand total|total due|amount due|balance due|to pay|total|amount|you paid|paid|payment|sent|payable|charge)"), " "))
            if (here.isEmpty()) lines.getOrNull(i + 1)?.let { n -> here = decimalsIn(n) + currencyIn(n) + (if (n.length < 14) plainIn(n) else emptyList()) }
            cands.addAll(here)
        }
    }
    val decimals = lines.flatMap { decimalsIn(it) }
    val currency = lines.flatMap { currencyIn(it) }
    val total = when {
        cands.isNotEmpty() -> cands.maxOrNull()
        decimals.isNotEmpty() -> decimals.maxOrNull()
        currency.isNotEmpty() -> currency.maxOrNull()
        else -> null
    }
    val merchant = lines.firstOrNull { l -> val letters = l.count { it.isLetter() }; letters >= 3 && l.count { it.isDigit() } <= letters / 2 }?.take(28)
    return ScanResult(total, merchant, text.isNotBlank())
}

/** reads the receipt photo on the phone (no internet needed) and reports what it found */
fun scanReceipt(dataUrl: String, onDone: (ScanResult?) -> Unit) {
    try {
        val bytes = Base64.decode(dataUrl.substringAfter("base64,"), Base64.DEFAULT)
        var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return onDone(null)
        // small pictures (an old low-resolution photo) read much better when they are enlarged first
        val shortSide = minOf(bmp.width, bmp.height)
        if (shortSide < 900) {
            val k = 1000f / shortSide
            bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt(), (bmp.height * k).toInt(), true)
        }
        val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        client.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { t -> onDone(parseReceipt(t.text)) }
            .addOnFailureListener { onDone(null) }
    } catch (e: Throwable) { onDone(null) }
}
