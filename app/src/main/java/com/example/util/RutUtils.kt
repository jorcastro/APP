package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

sealed class RutValidationResult {
    object Empty : RutValidationResult()
    object Incomplete : RutValidationResult()
    data class Valid(val formattedRut: String) : RutValidationResult()
    data class Invalid(val reason: String) : RutValidationResult()
}

object RutUtils {

    /**
     * Cleans the RUT, keeping only digits and the letter K (in uppercase).
     */
    fun cleanRut(rawInput: String): String {
        return rawInput
            .uppercase(Locale.ROOT)
            .replace(Regex("[^0-9K]"), "")
    }

    /**
     * Formats a raw or partially typed RUT into standard Chilean notation (e.g. 12.345.678-K).
     */
    fun formatRut(rawInput: String): String {
        val clean = cleanRut(rawInput)
        if (clean.isEmpty()) return ""

        // If length is 1, just show the single character
        if (clean.length == 1) return clean

        // The last character is the DV
        val dv = clean.last()
        val body = clean.substring(0, clean.length - 1)

        val formattedBody = formatBodyWithDots(body)
        return "$formattedBody-$dv"
    }

    /**
     * Formats the numeric body with dots (e.g. 12345678 -> 12.345.678).
     */
    private fun formatBodyWithDots(body: String): String {
        if (body.isEmpty()) return ""
        val parsed = body.toLongOrNull() ?: return body
        val symbols = DecimalFormatSymbols(Locale("es", "CL")).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,###", symbols)
        return formatter.format(parsed)
    }

    /**
     * Calculates the expected check digit (DV) using the Chilean Modulo 11 algorithm.
     */
    fun calculateDv(body: String): Char? {
        val cleanBody = body.filter { it.isDigit() }
        if (cleanBody.isEmpty()) return null

        var sum = 0
        var multiplier = 2

        for (i in cleanBody.length - 1 downTo 0) {
            val digit = cleanBody[i].digitToInt()
            sum += digit * multiplier
            multiplier = if (multiplier == 7) 2 else multiplier + 1
        }

        val remainder = sum % 11
        val expectedDvInt = 11 - remainder

        return when (expectedDvInt) {
            11 -> '0'
            10 -> 'K'
            else -> expectedDvInt.digitToChar()
        }
    }

    /**
     * Validates whether a RUT is mathematically valid according to Modulo 11.
     */
    fun isValid(rut: String): Boolean {
        val clean = cleanRut(rut)
        if (clean.length < 8 || clean.length > 9) return false

        val body = clean.substring(0, clean.length - 1)
        val dv = clean.last()

        if (!body.all { it.isDigit() }) return false

        val calculatedDv = calculateDv(body) ?: return false
        return calculatedDv == dv
    }

    /**
     * Checks the validation state of the RUT string.
     */
    fun validate(rut: String): RutValidationResult {
        val clean = cleanRut(rut)
        if (clean.isEmpty()) return RutValidationResult.Empty
        if (clean.length < 8) return RutValidationResult.Incomplete

        return if (isValid(clean)) {
            RutValidationResult.Valid(formatRut(clean))
        } else {
            RutValidationResult.Invalid("Dígito verificador incorrecto")
        }
    }
}
