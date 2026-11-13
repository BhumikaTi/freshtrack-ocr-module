package com.freshtrack.ocr

import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale


data class ExpiryDateResult(
    val year: Int,
    val month: Int,
    val day: Int?,
    val confidence: Confidence
) {
    fun format(): String =
        if (day != null) {
            String.format(Locale.ROOT, "%02d/%02d/%04d", day, month, year)
        } else {
            String.format(Locale.ROOT, "%02d/%04d", month, year)
        }
}


object ExpiryDateParser {

    private enum class Kind { NUM, MON, WORD }

    private enum class Label { EXPIRY, MANUFACTURE }

    private class Tok(
        val kind: Kind,
        val text: String,
        val sepBefore: String,
        val number: Int = 0,
        val month: Int = 0,
        val exactMonth: Boolean = true,
        val prefixMonth: Int = 0
    ) {
        val len: Int get() = text.length
    }

    private class Cand(
        val year: Int,
        val month: Int,
        val day: Int?,
        val start: Int,
        val end: Int,
        val needsLabel: Boolean,
        val fuzzy: Boolean,
        val tokenCount: Int
    ) {
        var label: Label? = null
        var line: Int = 0

        val key: Int get() = year * 10000 + month * 100 + (day ?: 1)
    }

    private class LineInfo(
        val tokens: List<Tok>,
        val labels: List<Label>,
        var candidates: List<Cand>
    )



    private val FULL_MONTHS = listOf(
        "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
        "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
    )

    private val SHORT_MONTHS = FULL_MONTHS.map { it.take(3) }


    private val TWO_LETTER_MONTHS = mapOf(
        "JA" to 1, "FE" to 2, "AP" to 4, "AU" to 8,
        "SE" to 9, "OC" to 10, "NO" to 11, "DE" to 12
    )

    private val DIGIT_TO_LETTERS = mapOf(
        '0' to "O", '1' to "ILT", '2' to "Z", '3' to "E",
        '4' to "A", '5' to "S", '6' to "G", '8' to "B"
    )

    private val LETTER_TO_DIGIT = mapOf(
        'O' to '0', 'I' to '1', 'L' to '1', 'S' to '5', 'B' to '8', 'Z' to '2'
    )

    private val EXPIRY_WORDS = setOf(
        "EXP", "EXPD", "EXPR", "EXR", "EXF", "EKP", "EXO",
        "EXPIRY", "EXPIRES", "EXPIRE", "EXPIRATION", "EXPIRING",
        "BB", "BBE", "BBD", "EXPDATE", "BESTBEFORE", "USEBY", "USEBEFORE"
    )

    private val MANUFACTURE_WORDS = setOf(
        "MFG", "MFD", "MFGD", "MF6", "MFO", "MPG", "MANUF",
        "MANUFACTURED", "MANUFACTURING", "MANUFACTURE",
        "PKD", "PACKED", "PACKING", "PACKAGED", "DOM", "MFGDATE"
    )

    private val LONG_EXPIRY = listOf("EXPIRY", "EXPIRES", "EXPIRATION", "EXPIRE")

    private val LONG_MANUFACTURE = listOf(
        "MANUFACTURED", "MANUFACTURING", "MANUFACTURE", "PACKED", "PACKING", "PACKAGED"
    )

    private val SMALL_CAPS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘʀꜱᴛᴜᴠᴡʏᴢ"
    private const val SMALL_CAPS_PLAIN = "ABCDEFGHIJKLMNOPRSTUVWYZ"

    private val RUN = Regex("[A-Z0-9]+")
    private val SPLIT_LETTERS_DIGITS = Regex("(?<=[A-Z]{3})(?=[0-9])|(?<=[0-9])(?=[A-Z]{3})")

    private val SHELF_LIFE = Regex(
        """(\d{1,3})\s*(M[O0]NTHS?|MTHS?|MNTHS?|MOS|YEARS?|YRS?|DAYS?|WEEKS?)"""
    )


    fun extract(text: String): ExpiryDateResult? {
        val cleaned = clean(text)
        val rawLines = cleaned.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (rawLines.isEmpty()) return null

        val lines = ArrayList<LineInfo>()
        for ((lineIndex, line) in rawLines.withIndex()) {
            val tokens = tokenize(line)
            val effective = applyPrefixMonths(tokens)
            val labels = findLabels(effective)
            val candidates = findCandidates(effective)
            candidates.forEach { it.line = lineIndex }
            lines += LineInfo(effective, labels.map { it.second }, candidates)
            attachLabels(effective, candidates, labels, lines.getOrNull(lines.size - 2))
        }

        val all = lines
            .flatMap { it.candidates }
            .filter { c ->
                when {
                    c.needsLabel && c.label == null -> false
                    c.fuzzy && c.label == null && c.tokenCount < 3 -> false
                    else -> true
                }
            }

        val expiryDates = all.filter { it.label == Label.EXPIRY }
        val manufactureDates = all.filter { it.label == Label.MANUFACTURE }
        val unlabelled = all.filter { it.label == null }


        val everyDate = (expiryDates + manufactureDates + unlabelled).distinctBy { it.key }
        if (everyDate.size >= 2) {
            everyDate.maxByOrNull { it.key }?.let {
                val conf = if (it.label == Label.EXPIRY) Confidence.HIGH else Confidence.MEDIUM
                return ExpiryDateResult(it.year, it.month, it.day, conf)
            }
        }

        // 1. Only one date: use it if it is labelled as the expiry date.
        expiryDates.firstOrNull()?.let { exp ->
            return ExpiryDateResult(exp.year, exp.month, exp.day, Confidence.HIGH)
        }

        // 2. "12 months from manufacture", "shelf life 9 months", "use within 24 months".
        shelfLifeExpiry(cleaned, manufactureDates, unlabelled)?.let { return it }

        // 3. Dates without a label: the latest one is the expiry date.
        unlabelled.maxByOrNull { it.key }?.let {
            return ExpiryDateResult(it.year, it.month, it.day, Confidence.MEDIUM)
        }

        // Only one manufacture date, or nothing usable.
        return null
    }



    private fun clean(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            val idx = SMALL_CAPS.indexOf(ch)
            when {
                idx >= 0 -> sb.append(SMALL_CAPS_PLAIN[idx])
                ch == '​' || ch == '﻿' -> Unit
                ch == '–' || ch == '—' || ch == '−' -> sb.append('-')
                ch == '‘' || ch == '’' || ch == '`' || ch == '´' -> sb.append('\'')
                ch == '\r' -> sb.append('\n')
                else -> sb.append(ch)
            }
        }
        return repairDropped(sb.toString().uppercase(Locale.ROOT))
    }


    private val MISSING_SECOND_SEP = Regex("(?<![0-9])(\\d{2})(\\d{2})([/.\\-])(\\d{2}|\\d{4})(?![0-9])")
    private val MISSING_FIRST_SEP = Regex("(?<![0-9])(\\d{2})([/.\\-])(\\d{2})(\\d{2})(?![0-9])")

    private fun repairDropped(text: String): String {
        var out = MISSING_SECOND_SEP.replace(text) { m ->
            val day = m.groupValues[1].toInt()
            val month = m.groupValues[2].toInt()
            if (day in 1..31 && month in 1..12) {
                "${m.groupValues[1]}/${m.groupValues[2]}/${m.groupValues[4]}"
            } else m.value
        }
        out = MISSING_FIRST_SEP.replace(out) { m ->
            val month = m.groupValues[3].toInt()
            val day = m.groupValues[1].toInt()
            val fourDigits = (m.groupValues[3] + m.groupValues[4]).toInt()
            if (day in 1..31 && month in 1..12 && fourDigits !in 2015..2060) {
                "${m.groupValues[1]}/${m.groupValues[3]}/${m.groupValues[4]}"
            } else m.value
        }
        return out
    }

    private fun tokenize(line: String): List<Tok> {
        val out = ArrayList<Tok>()
        var previousEnd = -1
        for (match in RUN.findAll(line)) {
            val sep = if (previousEnd < 0) "" else line.substring(previousEnd, match.range.first)
            previousEnd = match.range.last + 1
            val parts = match.value.split(SPLIT_LETTERS_DIGITS).filter { it.isNotEmpty() }
            parts.forEachIndexed { index, part ->
                out += classify(part, if (index == 0) sep else "")
            }
        }
        return out
    }

    private fun classify(run: String, sep: String): Tok {
        numberValue(run)?.let {
            return Tok(Kind.NUM, run, sep, number = it)
        }
        monthOf(run)?.let { (month, exact) ->
            return Tok(Kind.MON, run, sep, month = month, exactMonth = exact)
        }
        return Tok(Kind.WORD, run, sep, prefixMonth = TWO_LETTER_MONTHS[run] ?: 0)
    }


    private fun numberValue(run: String): Int? {
        if (run.length > 8) return null
        var real = 0
        var fixed = 0
        val sb = StringBuilder()
        for (ch in run) {
            if (ch in '0'..'9') {
                real++
                sb.append(ch)
            } else {
                val d = LETTER_TO_DIGIT[ch] ?: return null
                fixed++
                sb.append(d)
            }
        }
        if (real == 0 || fixed > real) return null
        return sb.toString().toIntOrNull()
    }


    private fun monthOf(run: String): Pair<Int, Boolean>? {
        if (run.length < 3 || run.length > 10) return null
        val variants = letterVariants(run)
        if (variants.isEmpty()) return null

        for (v in variants) {
            exactMonth(v)?.let { return Pair(it, true) }
        }
        for (v in variants) {
            fuzzyMonth(v)?.let { return Pair(it, false) }
        }
        return null
    }


    private fun letterVariants(run: String): List<String> {
        if (run.count { it.isDigit() } > 2) return emptyList()
        var results = listOf("")
        for (ch in run) {
            val options = when {
                ch in 'A'..'Z' -> ch.toString()
                else -> DIGIT_TO_LETTERS[ch] ?: return emptyList()
            }
            results = results.flatMap { prefix -> options.map { prefix + it } }
            if (results.size > 12) return emptyList()
        }
        return results
    }

    private fun exactMonth(s: String): Int? {
        val full = FULL_MONTHS.indexOf(s)
        if (full >= 0) return full + 1
        val short = SHORT_MONTHS.indexOf(s)
        if (short >= 0) return short + 1
        if (s == "SEPT") return 9
        return null
    }

    private fun fuzzyMonth(s: String): Int? {
        // Cut-off words: NOVE, NOVEM, DECE, SEPTEM ...
        if (s.length >= 4) {
            val starts = FULL_MONTHS.indices.filter { FULL_MONTHS[it].startsWith(s) }
            if (starts.size == 1) return starts[0] + 1
        }
        val found = HashSet<Int>()
        if (s.length == 3) {
            for (i in 0 until 12) {
                if (distance(s, SHORT_MONTHS[i]) <= 1) found += i + 1
            }
        }
        if (s.length >= 4) {
            val allowed = if (s.length >= 8) 2 else 1
            for (i in 0 until 12) {
                if (distance(s, FULL_MONTHS[i]) <= allowed) found += i + 1
            }
        }
        return if (found.size == 1) found.first() else null
    }

    private fun distance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[a.length][b.length]
    }



    private fun labelAt(tokens: List<Tok>, i: Int): Label? {
        val t = tokens[i]
        if (t.kind != Kind.WORD) return null
        val w = t.text
        if (w in EXPIRY_WORDS) return Label.EXPIRY
        if (w in MANUFACTURE_WORDS) return Label.MANUFACTURE

        if (i > 0 && tokens[i - 1].kind == Kind.WORD) {
            val p = tokens[i - 1].text
            if ((w == "BEFORE" || w == "BY") && p in setOf("BEST", "USE", "CONSUME", "CONSUMED")) {
                return Label.EXPIRY
            }
            if ((w == "UNTIL" || w == "TILL" || w == "UPTO") && p == "VALID") {
                return Label.EXPIRY
            }
        }

        if (w.length >= 6) {
            if (LONG_EXPIRY.any { distance(w, it) <= 1 }) return Label.EXPIRY
            if (LONG_MANUFACTURE.any { distance(w, it) <= 1 }) return Label.MANUFACTURE
        }
        return null
    }

    private fun findLabels(tokens: List<Tok>): List<Pair<Int, Label>> =
        tokens.indices.mapNotNull { i -> labelAt(tokens, i)?.let { Pair(i, it) } }


    private fun applyPrefixMonths(tokens: List<Tok>): List<Tok> =
        tokens.mapIndexed { i, t ->
            if (t.kind == Kind.WORD && t.prefixMonth > 0 && labelJustBefore(tokens, i)) {
                Tok(Kind.MON, t.text, t.sepBefore, month = t.prefixMonth, exactMonth = false)
            } else {
                t
            }
        }

    private fun labelJustBefore(tokens: List<Tok>, i: Int): Boolean {
        var j = i - 1
        var steps = 0
        while (j >= 0 && steps < 3) {
            val t = tokens[j]
            if (t.kind != Kind.WORD) return false
            if (labelAt(tokens, j) != null) return true
            j--
            steps++
        }
        return false
    }

    private fun attachLabels(
        tokens: List<Tok>,
        candidates: List<Cand>,
        labels: List<Pair<Int, Label>>,
        previous: LineInfo?
    ) {
        var previousEnd = 0
        for (c in candidates) {
            // Nearest label before the date (up to 3 plain words in between).
            var found: Label? = null
            var j = c.start - 1
            while (j >= previousEnd && j >= c.start - 4) {
                if (tokens[j].kind != Kind.WORD) break
                val l = labels.firstOrNull { it.first == j }?.second
                if (l != null) {
                    found = l
                    break
                }
                j--
            }
            c.label = found
            previousEnd = c.end
        }

        // A single date followed by a label: "12/2026 EXP".
        if (candidates.size == 1 && candidates[0].label == null) {
            val c = candidates[0]
            val after = labels.firstOrNull { it.first >= c.end && it.first <= c.end + 2 }
            if (after != null) c.label = after.second
        }

        // Label on the line above: "EXP" / "12/2026", or "MFG EXP" / "10/2025 10/2027".
        val unlabelled = candidates.filter { it.label == null }
        if (previous != null && previous.candidates.isEmpty() && previous.labels.isNotEmpty() && unlabelled.isNotEmpty()) {
            unlabelled.forEachIndexed { index, c ->
                c.label = previous.labels.getOrNull(index)
            }
        }
    }

    // ------------------------------------------------------------------
    // Date shapes
    // ------------------------------------------------------------------

    private fun findCandidates(tokens: List<Tok>): List<Cand> {
        val out = ArrayList<Cand>()
        var i = 0
        while (i < tokens.size) {
            val c = if (tokens[i].kind != Kind.WORD) matchAt(tokens, i) else null
            if (c != null) {
                out += c
                i = c.end
            } else {
                i++
            }
        }
        return out
    }

    private fun sepOk(t: Tok): Boolean {
        val s = t.sepBefore.trim()
        return s.isEmpty() || (s.length == 1 && s[0] in "/.-,'")
    }

    private fun sepKind(t: Tok): String {
        val s = t.sepBefore.trim()
        return when {
            s.isNotEmpty() -> s
            t.sepBefore.isNotEmpty() -> " "
            else -> ""
        }
    }

    private fun validYear(y: Int) = y in 2015..2060

    private fun fullYear(t: Tok): Int? = when (t.len) {
        4 -> t.number
        2 -> 2000 + t.number
        else -> null
    }

    private fun validDay(y: Int, m: Int, d: Int): Boolean =
        m in 1..12 && d >= 1 && d <= YearMonth.of(y, m).lengthOfMonth()

    private fun matchAt(t: List<Tok>, i: Int): Cand? {
        val a = t[i]
        val b = t.getOrNull(i + 1)?.takeIf { it.kind != Kind.WORD && sepOk(it) }
        val c = if (b != null) t.getOrNull(i + 2)?.takeIf { it.kind != Kind.WORD && sepOk(it) } else null

        // ---- three tokens ----
        if (b != null && c != null) {
            // 2026-05-12  or  12/05/2026  or  14.07.27
            if (a.kind == Kind.NUM && b.kind == Kind.NUM && c.kind == Kind.NUM && sepKind(b) == sepKind(c)) {
                val space = sepKind(b) == " "
                if (a.len == 4 && b.len <= 2 && c.len <= 2) {
                    val y = a.number
                    if (validYear(y) && validDay(y, b.number, c.number)) {
                        return Cand(y, b.number, c.number, i, i + 3, space, false, 3)
                    }
                } else if (a.len <= 2 && b.len <= 2 && (c.len == 4 || c.len == 2)) {
                    val y = fullYear(c)!!
                    var d = a.number
                    var m = b.number
                    if (m > 12 && d <= 12) {
                        val tmp = d; d = m; m = tmp
                    }
                    if (validYear(y) && validDay(y, m, d)) {
                        return Cand(y, m, d, i, i + 3, space, false, 3)
                    }
                }
            }
            // 12 NOV 2026, 25/NOV/27
            if (a.kind == Kind.NUM && b.kind == Kind.MON && c.kind == Kind.NUM && a.len <= 2 && (c.len == 4 || c.len == 2)) {
                val y = fullYear(c)!!
                if (validYear(y) && validDay(y, b.month, a.number)) {
                    return Cand(y, b.month, a.number, i, i + 3, false, !b.exactMonth, 3)
                }
            }
            // NOV 8 2026, NOV 8, 2026
            if (a.kind == Kind.MON && b.kind == Kind.NUM && c.kind == Kind.NUM && b.len <= 2 && (c.len == 4 || c.len == 2)) {
                val y = fullYear(c)!!
                if (validYear(y) && validDay(y, a.month, b.number)) {
                    return Cand(y, a.month, b.number, i, i + 3, false, !a.exactMonth, 3)
                }
            }
        }

        // ---- two tokens ----
        if (b != null) {
            if (a.kind == Kind.NUM && b.kind == Kind.NUM) {
                val kind = sepKind(b)
                val space = kind == " "
                val dot = kind == "."
                // 2027-03
                if (a.len == 4 && b.len <= 2 && b.number in 1..12 && validYear(a.number)) {
                    return Cand(a.number, b.number, null, i, i + 2, space, false, 2)
                }
                // 11/2026, 12.2026, 10 2027
                if (a.len <= 2 && b.len == 4 && a.number in 1..12 && validYear(b.number)) {
                    return Cand(b.number, a.number, null, i, i + 2, false, false, 2)
                }
                // 05/27, 06/28
                if (a.len <= 2 && b.len == 2 && a.number in 1..12 && validYear(2000 + b.number)) {
                    return Cand(2000 + b.number, a.number, null, i, i + 2, space || dot, false, 2)
                }
            }
            // NOV 2026, SEP 26
            if (a.kind == Kind.MON && b.kind == Kind.NUM) {
                if (b.len == 4 && validYear(b.number)) {
                    return Cand(b.number, a.month, null, i, i + 2, false, !a.exactMonth, 2)
                }
                if (b.len == 2 && validYear(2000 + b.number)) {
                    return Cand(2000 + b.number, a.month, null, i, i + 2, false, !a.exactMonth, 2)
                }
            }
        }
        return null
    }



    private fun shelfLifeExpiry(
        cleaned: String,
        manufactureDates: List<Cand>,
        unlabelled: List<Cand>
    ): ExpiryDateResult? {
        val flat = cleaned.replace('\n', ' ')
        for (m in SHELF_LIFE.findAll(flat)) {
            val from = maxOf(0, m.range.first - 45)
            val to = minOf(flat.length, m.range.last + 46)
            val window = flat.substring(from, to)
            val hasHint = listOf("SHELF", "WITHIN", "FROM", "AFTER", "BEFORE", "EXPIR", "BEST", "USE")
                .any { window.contains(it) }
            if (!hasHint) continue

            val amount = m.groupValues[1].toIntOrNull() ?: continue
            val unit = m.groupValues[2]

            val base = manufactureDates.firstOrNull()
                ?: unlabelled.singleOrNull()?.takeIf {
                    window.contains("MANUF") || window.contains("MFG") || window.contains("MFD") ||
                            window.contains("PACK") || window.contains("PKD")
                }
                ?: continue

            return addShelfLife(base, amount, unit)
        }
        return null
    }

    private fun addShelfLife(base: Cand, amount: Int, unit: String): ExpiryDateResult? {
        val months = when {
            unit.startsWith("Y") -> amount * 12
            unit.startsWith("M") -> amount
            else -> 0
        }
        return if (months > 0) {
            if (base.day != null) {
                val d = LocalDate.of(base.year, base.month, base.day).plusMonths(months.toLong())
                ExpiryDateResult(d.year, d.monthValue, d.dayOfMonth, Confidence.HIGH)
            } else {
                val ym = YearMonth.of(base.year, base.month).plusMonths(months.toLong())
                ExpiryDateResult(ym.year, ym.monthValue, null, Confidence.HIGH)
            }
        } else {
            val days = if (unit.startsWith("W")) amount * 7L else amount.toLong()
            val d = LocalDate.of(base.year, base.month, base.day ?: 1).plusDays(days)
            ExpiryDateResult(d.year, d.monthValue, d.dayOfMonth, Confidence.HIGH)
        }
    }
}