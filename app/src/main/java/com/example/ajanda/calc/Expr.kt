package com.example.ajanda.calc

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Basit ifade çözücü. Desteklenenler: + − × ÷ ^ ( ) % ² √ π, sin cos tan ln log,
 * örtük çarpma (2π, 3(4)), ondalık için virgül.
 */
class Expr(input: String, private val degrees: Boolean) {
    private val s = input
        .replace('×', '*').replace('÷', '/').replace('−', '-')
        .replace(',', '.').replace(" ", "")
    private var i = 0

    fun eval(): Double {
        if (s.isEmpty()) return 0.0
        val v = expr()
        if (i < s.length) throw IllegalArgumentException("Beklenmeyen: ${s[i]}")
        return v
    }

    private fun peek(): Char? = if (i < s.length) s[i] else null

    private fun expr(): Double {
        var v = term()
        while (true) {
            when (peek()) {
                '+' -> { i++; v += term() }
                '-' -> { i++; v -= term() }
                else -> return v
            }
        }
    }

    private fun startsPrimary(c: Char?): Boolean =
        c != null && (c.isDigit() || c == '.' || c == '(' || c == 'π' || c == '√' || c.isLetter())

    private fun term(): Double {
        var v = factor()
        while (true) {
            val c = peek()
            when {
                c == '*' -> { i++; v *= factor() }
                c == '/' -> { i++; val r = factor(); if (r == 0.0) throw ArithmeticException("Sıfıra bölme"); v /= r }
                startsPrimary(c) -> v *= factor() // örtük çarpma
                else -> return v
            }
        }
    }

    private fun factor(): Double {
        val b = unary()
        if (peek() == '^') {
            i++
            return b.pow(factor())
        }
        return b
    }

    private fun unary(): Double = when (peek()) {
        '-' -> { i++; -unary() }
        '+' -> { i++; unary() }
        else -> postfix()
    }

    private fun postfix(): Double {
        var v = primary()
        while (true) {
            when (peek()) {
                '%' -> { i++; v /= 100.0 }
                '²' -> { i++; v *= v }
                else -> return v
            }
        }
    }

    private fun primary(): Double {
        val c = peek() ?: throw IllegalArgumentException("Eksik ifade")
        if (c == '(') {
            i++
            val v = expr()
            if (peek() == ')') i++ // kapanmayan parantezi tolere et
            return v
        }
        if (c == 'π') { i++; return PI }
        if (c == '√') { i++; return sqrt(unary()) }
        if (c.isDigit() || c == '.') {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            if (i < s.length && (s[i] == 'E' || s[i] == 'e') && i + 1 < s.length &&
                (s[i + 1].isDigit() || s[i + 1] == '-' || s[i + 1] == '+')
            ) {
                i += 2
                while (i < s.length && s[i].isDigit()) i++
            }
            return s.substring(start, i).toDouble()
        }
        if (c.isLetter()) {
            val start = i
            while (i < s.length && s[i].isLetter() && s[i] != 'π') i++
            val name = s.substring(start, i).lowercase(Locale.ROOT)
            val arg = primary()
            val rad = if (degrees) arg * PI / 180.0 else arg
            return when (name) {
                "sin" -> cleanTrig(sin(rad))
                "cos" -> cleanTrig(cos(rad))
                "tan" -> {
                    val cs = cos(rad)
                    if (abs(cs) < 1e-12) throw ArithmeticException("Tanımsız")
                    cleanTrig(tan(rad))
                }
                "ln" -> ln(arg)
                "log" -> log10(arg)
                else -> throw IllegalArgumentException("Bilinmeyen: $name")
            }
        }
        throw IllegalArgumentException("Beklenmeyen: $c")
    }

    private fun cleanTrig(v: Double) = if (abs(v) < 1e-12) 0.0 else v

    companion object {
        private val tr = DecimalFormatSymbols(Locale.forLanguageTag("tr-TR"))

        /** Ekranda gösterim: binlik nokta, ondalık virgül. */
        fun pretty(v: Double, decimals: Int = 10): String {
            if (v.isNaN() || v.isInfinite()) return "Hata"
            if (abs(v) >= 1e15 || (abs(v) < 1e-9 && v != 0.0)) {
                return DecimalFormat("0.######E0", tr).format(v)
            }
            val pattern = if (decimals <= 0) "#,##0" else "#,##0." + "#".repeat(decimals)
            return DecimalFormat(pattern, tr).format(v)
        }

        /** İfadeye geri yazılabilir biçim (binlik ayırıcı yok). */
        fun raw(v: Double): String {
            if (v.isNaN() || v.isInfinite()) return ""
            return DecimalFormat("0.##########", tr).format(v)
        }

        fun parseNumber(text: String): Double? {
            val t = text.trim()
            return if (t.contains(',')) t.replace(".", "").replace(',', '.').toDoubleOrNull()
            else t.toDoubleOrNull()
        }
    }
}
