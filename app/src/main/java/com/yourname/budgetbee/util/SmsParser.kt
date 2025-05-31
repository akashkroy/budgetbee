package com.yourname.budgetbee.util

import com.yourname.budgetbee.data.models.Transaction
import java.text.SimpleDateFormat
import java.util.*

object SmsParser {

    private val publicBanks = listOf("SBI", "PNB", "BOB", "Canara", "Union Bank", "Indian Bank", "IOB", "UCO", "BOM", "CBI", "PSB")
    private val privateBanks = listOf("HDFC", "ICICI", "AXIS", "Kotak", "IndusInd", "YES", "IDFC", "Federal", "SIB", "RBL", "DCB")
    private val wallets = listOf("Paytm", "PhonePe", "PPBL", "UPI Lite", "GPay", "Amazon Pay")
    private val allBanks = publicBanks + privateBanks

    fun parse(body: String, address: String, timestamp: Long): Transaction? {
        val cleanedBody = body.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
        val dateFromTimestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))

        // ⛔ Skip obvious promotional messages
        if (cleanedBody.contains("OTP", true) || cleanedBody.length < 30) return null

        // 💰 Extract amount
        val amountRegex = Regex("""(?:Rs|INR)[^\d]*([\d,.]+)""", RegexOption.IGNORE_CASE)
        val rawAmount = amountRegex.find(cleanedBody)?.groupValues?.get(1)
        val amount = rawAmount?.replace(",", "")?.toDoubleOrNull()

        // 📅 Extract date (if exists)
        var date = dateFromTimestamp
        val possibleFormats = listOf("dd-MM-yy", "dd-MM-yyyy", "ddMMMyy", "dd-MMM-yy", "dd-MMM-yyyy")
        val dateRegex = Regex("""\d{2}[-]?[A-Za-z]{3}[-]?\d{2,4}|\d{2}[-/]\d{2}[-/]\d{2,4}""")
        val dateMatch = dateRegex.find(cleanedBody)?.value

        dateMatch?.let {
            for (format in possibleFormats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.ENGLISH)
                    val parsed = sdf.parse(it.replace("/", "-").replace(" ", ""))
                    if (parsed != null && parsed.before(Date())) {
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parsed)
                        break
                    }
                } catch (_: Exception) {
                }
            }
        }

        // 🧠 Detect type
        val isCredit = Regex("""\b(credited|received|deposited)\b""", RegexOption.IGNORE_CASE).containsMatchIn(cleanedBody)
        val isDebit = Regex("""\b(debited|paid|spent|transferred|withdrawn)\b""", RegexOption.IGNORE_CASE).containsMatchIn(cleanedBody)

        val type = when {
            isDebit -> "debit"
            isCredit -> "credit"
            else -> null
        }

        // 🛍 Merchant extraction (stop at 'credited', 'UPI', 'Avl Lmt', URLs, etc.)
        var merchant = address
        if (cleanedBody.contains("Card no", ignoreCase = true) || cleanedBody.contains("Avl Lmt", ignoreCase = true)) {
            // Card transaction merchant extraction
            val merchantRegex = Regex("""(?:\d{2}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\s+(.*?)\s+Avl Lmt""", RegexOption.IGNORE_CASE)
            val match = merchantRegex.find(cleanedBody)
            merchant = match?.groupValues?.get(1)?.trim() ?: merchant
        } else {
            // Fallback merchant logic for bank/UPI transactions
            val merchantRegex = Regex("""(?:to|by|UPI:|InfoACH\*TP\s*ACH\s*IN\.|;)\s*([A-Za-z0-9\s.]+?)\s*(credited|debited|UPI|IMPS|Ref|Avl|\.|$)""", RegexOption.IGNORE_CASE)
            val match = merchantRegex.find(cleanedBody)
            merchant = match?.groupValues?.get(1)?.trim() ?: address
        }

        // 🏷️ Category logic
        val category = when {
            cleanedBody.contains("UPI Lite", true) -> "UPI Lite"
            wallets.any { cleanedBody.contains(it, true) } -> "Wallet"
            cleanedBody.contains("UPI", true) -> "UPI"
            cleanedBody.contains("NEFT", true) || cleanedBody.contains("IMPS", true) || cleanedBody.contains("Cheque", true) -> "Bank"
            cleanedBody.contains("Card no", true) || cleanedBody.contains("Avl Lmt", true) -> "Credit Card"
            allBanks.any { cleanedBody.contains(it, true) || address.contains(it, true) } -> "Bank"
            else -> "Other"
        }

        // ✅ Final sanity check
        return if (amount != null && type != null && date <= SimpleDateFormat("yyyy-MM-dd").format(Date())) {
            Transaction(
                amount = amount,
                type = type,
                category = category,
                merchant = merchant,
                date = date
            )
        } else null
    }
}
