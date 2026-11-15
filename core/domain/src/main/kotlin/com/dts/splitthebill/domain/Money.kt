package com.dts.splitthebill.domain

import java.math.BigInteger

data class CurrencyUnit(
    val code: String,
    val exponent: Int,
) {
    init {
        if (!Regex("^[A-Z]{3}$").matches(code) || exponent !in 0..4) {
            CalculationChecks.fail(
                CalculationErrorCode.INVALID_CURRENCY,
                "Currency must have an uppercase ISO code and exponent from 0 to 4",
            )
        }
    }
}

data class Money(
    val amountMinor: Long,
    val currency: CurrencyUnit,
) {
    init {
        CalculationChecks.safeNonNegative(amountMinor, "amountMinor")
    }
}

object MoneyText {
    private val unsignedDecimal = Regex("^[0-9]+(?:\\.([0-9]+))?$")
    private val safeMaximum = BigInteger.valueOf(MAX_SAFE_INTEGER)

    /** Parses a UI decimal without Double/Float rounding or locale grouping. */
    fun parseUnsignedMinor(text: String, currency: CurrencyUnit): Long {
        if (text.length > 32) {
            CalculationChecks.fail(CalculationErrorCode.INVALID_AMOUNT, "Decimal amount is too long")
        }
        val match = unsignedDecimal.matchEntire(text)
            ?: CalculationChecks.fail(CalculationErrorCode.INVALID_AMOUNT, "Invalid decimal amount")
        val fraction = match.groupValues.getOrElse(1) { "" }
        if (fraction.length > currency.exponent) {
            CalculationChecks.fail(
                CalculationErrorCode.INVALID_AMOUNT,
                "Amount has more than ${currency.exponent} fractional digits",
            )
        }

        val whole = text.substringBefore('.')
        val scale = BigInteger.TEN.pow(currency.exponent)
        val fractionalMinor = fraction.padEnd(currency.exponent, '0').ifEmpty { "0" }
        val minor = whole.toBigInteger().multiply(scale).add(fractionalMinor.toBigInteger())
        if (minor > safeMaximum) {
            CalculationChecks.fail(CalculationErrorCode.OVERFLOW, "Amount exceeds the safe integer range")
        }
        return minor.longValueExact()
    }

    /** Formats a signed minor-unit value for display without adding a currency symbol. */
    fun formatMinor(amountMinor: Long, currency: CurrencyUnit): String {
        CalculationChecks.safeSigned(amountMinor, "amountMinor")
        val sign = if (amountMinor < 0) "-" else ""
        val digits = kotlin.math.abs(amountMinor).toString()
        if (currency.exponent == 0) return sign + digits
        val padded = digits.padStart(currency.exponent + 1, '0')
        val splitAt = padded.length - currency.exponent
        return sign + padded.substring(0, splitAt) + "." + padded.substring(splitAt)
    }
}
