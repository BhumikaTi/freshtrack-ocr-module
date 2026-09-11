package com.freshtrack.ocr

import java.time.YearMonth

data class ParsedProduct(
    val productName: String?,
    val expiryDate: String?,
    val confidence: Confidence
)

enum class Confidence {
    HIGH,
    MEDIUM,
    LOW
}

object ExpiryParser {

    private val MONTH_NAMES = listOf(
        "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
        "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
    )

    fun parse(
        text: String
    ): ParsedProduct {

        val expiryDate =
            extractExpiryDate(text)

        val productName =
            extractProductName(text)

        val confidence =
            when {
                expiryDate == null ->
                    Confidence.LOW

                hasStrongExpiryLabel(text) ->
                    Confidence.HIGH

                else ->
                    Confidence.MEDIUM
            }

        return ParsedProduct(
            productName = productName,
            expiryDate = expiryDate,
            confidence = confidence
        )
    }

    fun extractExpiryDate(
        text: String
    ): String? {

        val lines =
            normalizeLines(text)

        if (lines.isEmpty()) {
            return null
        }

        val explicitExpiry =
            findExplicitExpiryDate(lines)

        if (explicitExpiry != null) {
            return explicitExpiry
        }

        val allDates: List<String> =
            collectDates(lines)

        if (allDates.isEmpty()) {
            return null
        }

        val shelfLifeMonths =
            findShelfLifeMonths(lines)

        if (shelfLifeMonths != null) {

            val manufacturingDate =
                findManufacturingDate(lines)

            if (manufacturingDate != null) {

                val calculated =
                    addMonths(
                        manufacturingDate,
                        shelfLifeMonths
                    )

                if (calculated != null) {
                    return calculated
                }
            }

            if (allDates.size == 1) {

                val calculated =
                    addMonths(
                        allDates.first(),
                        shelfLifeMonths
                    )

                if (calculated != null) {
                    return calculated
                }
            }

            if (allDates.size > 1) {

                var baseDate: String? = null
                var earliestValue: Int? = null

                for (date: String in allDates) {

                    val comparable =
                        parseComparableDate(date)

                    if (comparable == null) {
                        continue
                    }

                    val currentEarliest =
                        earliestValue

                    if (
                        currentEarliest == null ||
                        comparable < currentEarliest
                    ) {
                        earliestValue = comparable
                        baseDate = date
                    }
                }

                if (baseDate != null) {

                    val calculated =
                        addMonths(
                            baseDate,
                            shelfLifeMonths
                        )

                    if (calculated != null) {
                        return calculated
                    }
                }
            }
        }

        val anchorExpiry =
            findAnchorDate(lines)

        if (anchorExpiry != null) {
            return anchorExpiry
        }

        val labelledExpiry =
            findLabelledExpiryDate(lines)

        if (labelledExpiry != null) {
            return labelledExpiry
        }

        val sideBySideExpiry =
            findSideBySideExpiryDate(lines)

        if (sideBySideExpiry != null) {
            return sideBySideExpiry
        }

        if (allDates.size >= 2) {

            var latestDate: String? = null
            var latestValue: Int? = null

            for (date: String in allDates) {

                val comparable =
                    parseComparableDate(date)

                if (comparable == null) {
                    continue
                }

                val currentLatest =
                    latestValue

                if (
                    currentLatest == null ||
                    comparable > currentLatest
                ) {
                    latestValue = comparable
                    latestDate = date
                }
            }

            if (latestDate != null) {
                return latestDate
            }
        }

        return null
    }

    private fun extractProductName(
        text: String
    ): String? {

        val lines =
            normalizeLines(text)

        if (lines.isEmpty()) {
            return null
        }

        var bestCandidate: String? = null
        var bestScore = 0

        val strongProductWords = listOf(
            "FACE WASH",
            "FACIAL WASH",
            "BODY WASH",
            "CLEANSER",
            "CLEANSING",
            "SERUM",
            "MOISTURIZER",
            "MOISTURISER",
            "CREAM",
            "LOTION",
            "GEL",
            "SCRUB",
            "MASK",
            "SHAMPOO",
            "CONDITIONER",
            "TONER",
            "SUNSCREEN",
            "FACE",
            "SKIN",
            "HAIR",
            "SOAP"
        )

        val blockedWords = listOf(
            "BATCH",
            "BATCH NO",
            "B.NO",
            "LOT",
            "MRP",
            "PRICE",
            "QUANTITY",
            "NET VOL",
            "NET WT",
            "NET CONTENT",
            "CUSTOMER CARE",
            "CUSTOMERCARE",
            "TOLL FREE",
            "MARKETED BY",
            "MARKETED IN",
            "MANUFACTURED BY",
            "MANUFACTURED",
            "MANUFACTURER",
            "MANUFACTUR",
            "ADDRESS",
            "PLOT NO",
            "INDUSTRIAL AREA",
            "WARNING",
            "DIRECTIONS",
            "DIRECTIONS FOR USE",
            "STORAGE",
            "STORAGE CONDITION",
            "INGREDIENTS",
            "INGREDIENT",
            "CONTENTS",
            "FOR BEST RESULTS",
            "RINSE",
            "KEEP OUT",
            "MADE IN INDIA",
            "MADE IN",
            "LICENSE",
            "LIC NO",
            "LIC. NO",
            "REGN NO",
            "REGISTRATION",
            "WWW.",
            "HTTP",
            "@",
            "ONLY",
            "ETHYL ALCOHOL",
            "ALCOHOL",
            "Rs.",
            "RS.",
            "INCL. OF",
            "INCLUSIVE OF",
            "ALL TAXES",
            "PER ML"
        )

        for (
        line in lines.take(15)
        ) {

            if (line.length < 4) {
                continue
            }

            if (
                findDate(line) != null
            ) {
                continue
            }

            if (
                containsExpiryLabel(line)
            ) {
                continue
            }

            if (
                containsManufacturingLabel(line)
            ) {
                continue
            }

            if (
                blockedWords.any { blocked ->
                    line.contains(
                        blocked,
                        ignoreCase = true
                    )
                }
            ) {
                continue
            }

            val letters =
                line.count {
                    it.isLetter()
                }

            val digits =
                line.count {
                    it.isDigit()
                }

            if (
                letters < 4 ||
                digits > letters
            ) {
                continue
            }

            val words =
                line.split(
                    Regex("\\s+")
                ).filter {
                    it.isNotBlank()
                }

            if (words.isEmpty()) {
                continue
            }

            var score = 0

            for (
            productWord in strongProductWords
            ) {

                if (
                    line.contains(
                        productWord,
                        ignoreCase = true
                    )
                ) {
                    score += 30
                }
            }

            if (words.size <= 8) {
                score += 10
            }

            if (words.size <= 5) {
                score += 5
            }

            if (words.size > 10) {
                score -= 20
            }

            val punctuation =
                line.count {
                    !it.isLetterOrDigit() &&
                            !it.isWhitespace()
                }

            if (punctuation > 6) {
                score -= 10
            }

            if (letters >= 8) {
                score += 5
            }

            if (
                !strongProductWords.any { word ->
                    line.contains(
                        word,
                        ignoreCase = true
                    )
                }
            ) {
                score -= 15
            }

            if (
                score > bestScore
            ) {
                bestScore = score
                bestCandidate = line
            }
        }

        if (
            bestCandidate == null ||
            bestScore < 20
        ) {
            return null
        }

        var cleaned =
            bestCandidate
                .trim()

        cleaned =
            cleaned.replace(
                Regex(
                    """\s*\.?\s*MADE\s+IN\s+INDIA\s*$"""
                ),
                ""
            )
                .trim()

        cleaned =
            cleaned.replace(
                Regex("""\s+"""),
                " "
            )
                .trim()

        return cleaned.ifBlank {
            null
        }
    }

    private fun normalizeLines(
        text: String
    ): List<String> {

        return text
            .uppercase()
            .replace(
                "\r",
                ""
            )
            .lines()
            .map {
                normalizeOcrLine(it)
            }
            .map {
                it.trim()
            }
            .filter {
                it.isNotEmpty()
            }
    }

    private fun normalizeOcrLine(
        line: String
    ): String {

        return line
            .replace(
                "—",
                "-"
            )
            .replace(
                "–",
                "-"
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()
    }

    private fun findExplicitExpiryDate(lines: List<String>): String? {
        for (i in lines.indices) {
            val line = lines[i]
            if (!containsExpiryLabel(line)) continue

            val expiryDateAfterLabel =
                Regex("""(?:^|\s)(?:E|EXP|EXPIRY|EXPIRES)\s*(\d{1,2}[/\-.]\d{2,4})\b""")
                    .find(line)
                    ?.groupValues
                    ?.getOrNull(1)

            if (expiryDateAfterLabel != null) {
                return findDate(expiryDateAfterLabel)
            }

            val tokens = tokenize(line)
            val expiryIndex = tokens.indexOfFirst { token ->
                fuzzyMatches(token, "EXP", 1) ||
                        fuzzyMatches(token, "EXPIRY", 2) ||
                        fuzzyMatches(token, "EXPIRES", 2) ||
                        token == "E"
            }

            if (expiryIndex >= 0) {
                val afterExpiry = tokens.drop(expiryIndex + 1).joinToString(" ")
                val sameLineDate = findDate(afterExpiry)
                if (sameLineDate != null) return sameLineDate
            }

            val sameLineDate = findDate(line)
            if (sameLineDate != null) return sameLineDate

            val candidates = mutableListOf<Pair<Int, String>>()

            for (distance in 1..5) {
                val after = i + distance
                if (after >= lines.size) break

                val candidateLine = lines[after]
                if (containsExpiryLabel(candidateLine)) break

                val date = findDate(candidateLine)
                if (date != null) candidates.add(after to date)
            }

            if (candidates.isEmpty()) continue

            val manufacturingDates = findManufacturingDateCandidates(lines)
            val expiryCandidates = candidates.filter { candidate ->
                manufacturingDates.none {
                    it.second == candidate.second && it.first == candidate.first
                }
            }

            if (expiryCandidates.isNotEmpty()) {
                var latestDate: String? = null
                var latestValue: Int? = null

                for (candidate in expiryCandidates) {
                    val comparable = parseComparableDate(candidate.second) ?: continue
                    if (latestValue == null || comparable > latestValue!!) {
                        latestValue = comparable
                        latestDate = candidate.second
                    }
                }

                if (latestDate != null) return latestDate

                return expiryCandidates.minByOrNull {
                    kotlin.math.abs(it.first - i)
                }?.second
            }

            return candidates.minByOrNull {
                kotlin.math.abs(it.first - i)
            }?.second
        }

        return null
    }

    private fun findManufacturingDateCandidates(
        lines: List<String>
    ): List<Pair<Int, String>> {

        val result =
            mutableListOf<Pair<Int, String>>()

        for (i in lines.indices) {

            if (
                !containsManufacturingLabel(
                    lines[i]
                )
            ) {
                continue
            }

            val sameLine =
                findDate(
                    lines[i]
                )

            if (sameLine != null) {

                result.add(
                    i to sameLine
                )
            }

            for (distance in 1..3) {

                val index =
                    i + distance

                if (index >= lines.size) {
                    break
                }

                if (
                    containsExpiryLabel(
                        lines[index]
                    )
                ) {
                    break
                }

                val date =
                    findDate(
                        lines[index]
                    )

                if (date != null) {

                    result.add(
                        index to date
                    )

                    break
                }
            }
        }

        return result
    }

    private fun findAnchorDate(
        lines: List<String>
    ): String? {

        for (i in lines.indices) {

            val line =
                lines[i]

            val tokens =
                tokenize(line)

            if (
                containsPhrase(
                    tokens,
                    "USE",
                    "BEFORE"
                )
            ) {

                val useBeforeIndex =
                    findPhraseStart(
                        tokens,
                        "USE",
                        "BEFORE"
                    )

                if (useBeforeIndex >= 0) {

                    val afterLabel =
                        tokens
                            .drop(
                                useBeforeIndex + 2
                            )
                            .joinToString(" ")

                    val sameLineDate =
                        findDate(afterLabel)

                    if (sameLineDate != null) {
                        return sameLineDate
                    }
                }

                for (distance in 1..3) {

                    val nextIndex =
                        i + distance

                    if (
                        nextIndex >= lines.size
                    ) {
                        break
                    }

                    val candidateLine =
                        lines[nextIndex]

                    if (
                        containsExpiryLabel(
                            candidateLine
                        )
                    ) {
                        break
                    }

                    if (
                        containsManufacturingLabel(
                            candidateLine
                        )
                    ) {
                        continue
                    }

                    val nearbyDate =
                        findDate(
                            candidateLine
                        )

                    if (nearbyDate != null) {
                        return nearbyDate
                    }
                }
            }

            if (
                containsBeforeAnchor(line) ||
                containsByAnchor(line)
            ) {

                val beforeIndex =
                    tokens.indexOfFirst {
                        fuzzyMatches(
                            it,
                            "BEFORE",
                            2
                        )
                    }

                if (beforeIndex >= 0) {

                    val afterAnchor =
                        tokens
                            .drop(
                                beforeIndex + 1
                            )
                            .joinToString(" ")

                    val sameLineDate =
                        findDate(
                            afterAnchor
                        )

                    if (sameLineDate != null) {
                        return sameLineDate
                    }
                }

                for (distance in 1..3) {

                    val nextIndex =
                        i + distance

                    if (
                        nextIndex >= lines.size
                    ) {
                        break
                    }

                    val candidateLine =
                        lines[nextIndex]

                    if (
                        containsExpiryLabel(
                            candidateLine
                        )
                    ) {
                        break
                    }

                    if (
                        containsManufacturingLabel(
                            candidateLine
                        )
                    ) {
                        continue
                    }

                    val nearbyDate =
                        findDate(
                            candidateLine
                        )

                    if (nearbyDate != null) {
                        return nearbyDate
                    }
                }
            }
        }

        return null
    }

    private fun findPhraseStart(
        tokens: List<String>,
        first: String,
        second: String
    ): Int {

        if (tokens.size < 2) {
            return -1
        }

        for (
        i in 0 until tokens.size - 1
        ) {

            if (
                fuzzyMatches(
                    tokens[i],
                    first,
                    if (first.length <= 3) 1 else 2
                ) &&
                fuzzyMatches(
                    tokens[i + 1],
                    second,
                    if (second.length <= 3) 1 else 2
                )
            ) {
                return i
            }
        }

        return -1
    }

    private fun findShelfLifeMonths(
        lines: List<String>
    ): Int? {

        for (line in lines) {

            val tokens =
                tokenize(line)

            if (tokens.isEmpty()) {
                continue
            }

            for (i in tokens.indices) {

                val token =
                    tokens[i]

                if (
                    fuzzyMatches(
                        token,
                        "BEFORE",
                        2
                    )
                ) {

                    val months =
                        findMonthsAfter(
                            tokens,
                            i
                        )

                    if (months != null) {
                        return months
                    }
                }

                if (
                    fuzzyMatches(
                        token,
                        "WITHIN",
                        2
                    )
                ) {

                    val months =
                        findMonthsAfter(
                            tokens,
                            i
                        )

                    if (months != null) {
                        return months
                    }
                }

                if (
                    fuzzyMatches(
                        token,
                        "FOR",
                        1
                    )
                ) {

                    val months =
                        findMonthsAfter(
                            tokens,
                            i
                        )

                    if (months != null) {
                        return months
                    }
                }
            }
        }

        return null
    }

    private fun findMonthsAfter(
        tokens: List<String>,
        anchorIndex: Int
    ): Int? {

        for (offset in 1..5) {

            val numberIndex =
                anchorIndex + offset

            if (
                numberIndex >= tokens.size
            ) {
                break
            }

            val number =
                tokens[numberIndex]
                    .filter {
                        it.isDigit()
                    }
                    .toIntOrNull()
                    ?: continue

            val unitIndex =
                numberIndex + 1

            if (
                unitIndex >= tokens.size
            ) {
                continue
            }

            val unit =
                tokens[unitIndex]

            if (
                fuzzyMatches(
                    unit,
                    "MONTH",
                    2
                ) ||
                fuzzyMatches(
                    unit,
                    "MONTHS",
                    2
                )
            ) {

                if (
                    number in 1..120
                ) {
                    return number
                }
            }
        }

        return null
    }

    private fun findManufacturingDate(
        lines: List<String>
    ): String? {

        for (i in lines.indices) {

            if (
                !containsManufacturingLabel(
                    lines[i]
                )
            ) {
                continue
            }

            val sameLine =
                findDate(
                    lines[i]
                )

            if (sameLine != null) {
                return sameLine
            }

            for (distance in 1..3) {

                val index =
                    i + distance

                if (
                    index >= lines.size
                ) {
                    break
                }

                if (
                    containsExpiryLabel(
                        lines[index]
                    )
                ) {
                    break
                }

                val date =
                    findDate(
                        lines[index]
                    )

                if (date != null) {
                    return date
                }
            }

            for (distance in 1..3) {

                val index =
                    i - distance

                if (
                    index < 0
                ) {
                    break
                }

                if (
                    containsExpiryLabel(
                        lines[index]
                    )
                ) {
                    break
                }

                val date =
                    findDate(
                        lines[index]
                    )

                if (date != null) {
                    return date
                }
            }
        }

        return null
    }

    private fun findLabelledExpiryDate(
        lines: List<String>
    ): String? {

        for (i in lines.indices) {

            if (
                !containsManufacturingLabel(
                    lines[i]
                )
            ) {
                continue
            }

            for (distance in 1..5) {

                val after =
                    i + distance

                if (
                    after >= lines.size
                ) {
                    continue
                }

                if (
                    !containsExpiryLabel(
                        lines[after]
                    )
                ) {
                    continue
                }

                val sameLine =
                    findDate(
                        lines[after]
                    )

                if (sameLine != null) {
                    return sameLine
                }

                for (d in 1..2) {

                    val dateIndex =
                        after + d

                    if (
                        dateIndex >= lines.size
                    ) {
                        continue
                    }

                    val nearbyDate =
                        findDate(
                            lines[dateIndex]
                        )

                    if (nearbyDate != null) {
                        return nearbyDate
                    }
                }
            }
        }

        return null
    }

    private fun findSideBySideExpiryDate(
        lines: List<String>
    ): String? {

        for (line in lines) {

            val dates =
                findAllDates(line)

            if (
                dates.size != 2
            ) {
                continue
            }

            val first =
                parseComparableDate(
                    dates[0]
                )

            val second =
                parseComparableDate(
                    dates[1]
                )

            if (
                first == null ||
                second == null
            ) {
                continue
            }

            return if (
                second > first
            ) {
                dates[1]
            } else {
                dates[0]
            }
        }

        return null
    }

    private fun containsExpiryLabel(
        text: String
    ): Boolean {

        val tokens =
            tokenize(text)

        for (token in tokens) {

            if (
                fuzzyMatches(
                    token,
                    "EXP",
                    1
                ) ||
                fuzzyMatches(
                    token,
                    "EXPIRY",
                    2
                ) ||
                fuzzyMatches(
                    token,
                    "EXPIRES",
                    2
                )
            ) {
                return true
            }
        }

        if (
            containsPhrase(
                tokens,
                "USE",
                "BY"
            )
        ) {
            return true
        }

        if (
            containsPhrase(
                tokens,
                "USE",
                "BEFORE"
            )
        ) {
            return true
        }

        if (
            containsPhrase(
                tokens,
                "BEST",
                "BEFORE"
            )
        ) {
            return true
        }

        if (
            containsPhrase(
                tokens,
                "BEST",
                "BY"
            )
        ) {
            return true
        }

        if (
            containsPhrase(
                tokens,
                "SELL",
                "BY"
            )
        ) {
            return true
        }

        if (
            containsPhrase(
                tokens,
                "CONSUME",
                "BY"
            )
        ) {
            return true
        }

        if (
            containsPhrase(
                tokens,
                "VALID",
                "UNTIL"
            )
        ) {
            return true
        }

        if (
            containsPhrase(
                tokens,
                "DISPLAY",
                "UNTIL"
            )
        ) {
            return true
        }

        if (
            containsPhrase(
                tokens,
                "FREEZE",
                "BY"
            )
        ) {
            return true
        }

        if (
            tokens.any {
                fuzzyMatches(
                    it,
                    "IMP",
                    1
                )
            }
        ) {
            return true
        }

        if (
            tokens.any {
                fuzzyMatches(
                    it,
                    "USEBY",
                    2
                )
            }
        ) {
            return true
        }

        if (
            tokens.any { it == "E" } ||
            Regex("""(^|[^A-Z])E\s*(?=\d{1,2}[/\-.])""").containsMatchIn(text)
        ) {
            return true
        }

        return false
    }

    private fun hasStrongExpiryLabel(
        text: String
    ): Boolean {

        return containsExpiryLabel(
            text
        )
    }

    private fun containsBeforeAnchor(
        text: String
    ): Boolean {

        return tokenize(text)
            .any {
                fuzzyMatches(
                    it,
                    "BEFORE",
                    2
                )
            }
    }

    private fun containsByAnchor(
        text: String
    ): Boolean {

        val tokens =
            tokenize(text)

        return (
                containsPhrase(
                    tokens,
                    "USE",
                    "BY"
                ) ||
                        containsPhrase(
                            tokens,
                            "BEST",
                            "BY"
                        ) ||
                        containsPhrase(
                            tokens,
                            "SELL",
                            "BY"
                        ) ||
                        containsPhrase(
                            tokens,
                            "CONSUME",
                            "BY"
                        )
                )
    }

    private fun containsManufacturingLabel(
        text: String
    ): Boolean {

        val tokens =
            tokenize(text)

        for (token in tokens) {

            if (
                fuzzyMatches(
                    token,
                    "MFG",
                    1
                ) ||
                fuzzyMatches(
                    token,
                    "MFD",
                    1
                ) ||
                fuzzyMatches(
                    token,
                    "MANUFACTURING",
                    3
                ) ||
                fuzzyMatches(
                    token,
                    "MANUFACTURE",
                    3
                ) ||
                fuzzyMatches(
                    token,
                    "MANUFACTURED",
                    3
                ) ||
                fuzzyMatches(
                    token,
                    "PACKED",
                    2
                ) ||
                fuzzyMatches(
                    token,
                    "PKD",
                    1
                )
            ) {
                return true
            }
        }

        if (
            tokens.any { it == "E" } ||
            Regex("""(^|[^A-Z])E\s*(?=\d{1,2}[/\-.])""").containsMatchIn(text)
        ) {
            return true
        }

        return false
    }

    private fun tokenize(
        text: String
    ): List<String> {

        return text
            .uppercase()
            .replace(
                Regex("[^A-Z0-9/]"),
                " "
            )
            .split(
                Regex("\\s+")
            )
            .filter {
                it.isNotBlank()
            }
    }

    private fun containsPhrase(
        tokens: List<String>,
        first: String,
        second: String
    ): Boolean {

        if (
            tokens.size < 2
        ) {
            return false
        }

        for (
        i in 0 until tokens.size - 1
        ) {

            if (
                fuzzyMatches(
                    tokens[i],
                    first,
                    if (
                        first.length <= 3
                    ) {
                        1
                    } else {
                        2
                    }
                ) &&
                fuzzyMatches(
                    tokens[i + 1],
                    second,
                    if (
                        second.length <= 3
                    ) {
                        1
                    } else {
                        2
                    }
                )
            ) {
                return true
            }
        }

        return false
    }

    private fun fuzzyMatches(
        actual: String,
        expected: String,
        maxDistance: Int
    ): Boolean {

        if (
            actual == expected
        ) {
            return true
        }

        if (
            actual.isEmpty()
        ) {
            return false
        }

        if (
            kotlin.math.abs(
                actual.length -
                        expected.length
            ) > maxDistance
        ) {
            return false
        }

        return levenshteinDistance(
            actual,
            expected
        ) <= maxDistance
    }

    private fun levenshteinDistance(
        a: String,
        b: String
    ): Int {

        val dp =
            Array(
                a.length + 1
            ) {
                IntArray(
                    b.length + 1
                )
            }

        for (
        i in 0..a.length
        ) {
            dp[i][0] = i
        }

        for (
        j in 0..b.length
        ) {
            dp[0][j] = j
        }

        for (
        i in 1..a.length
        ) {

            for (
            j in 1..b.length
            ) {

                val cost =
                    if (
                        a[i - 1] ==
                        b[j - 1]
                    ) {
                        0
                    } else {
                        1
                    }

                dp[i][j] =
                    minOf(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + cost
                    )
            }
        }

        return dp[a.length][b.length]
    }

    private fun findDate(
        text: String
    ): String? {

        Regex(
            """\b(0?[1-9]|[12][0-9]|3[01])\s*[/\-.]\s*(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\s*[/\-.]\s*(\d{2,4})\b"""
        )
            .find(text)
            ?.let {

                return it.value
                    .replace(
                        Regex("""[\-.]"""),
                        "/"
                    )
                    .replace(
                        " ",
                        ""
                    )
            }

        Regex(
            """\b\d{4}\s*[/\-]\s*(0?[1-9]|1[0-2])\s*[/\-]\s*(0?[1-9]|[12][0-9]|3[01])\b"""
        )
            .find(text)
            ?.let {

                return it.value
                    .replace(
                        " ",
                        ""
                    )
                    .replace(
                        "-",
                        "/"
                    )
                    .replace(
                        ".",
                        "/"
                    )
            }

        Regex(
            """\b(0?[1-9]|[12][0-9]|3[01])\s*[/\-]\s*(0?[1-9]|1[0-2])\s*[/\-]\s*(\d{2,4})\b"""
        )
            .find(text)
            ?.let {

                return it.value
                    .replace(
                        " ",
                        ""
                    )
                    .replace(
                        "-",
                        "/"
                    )
                    .replace(
                        ".",
                        "/"
                    )
            }

        Regex(
            """\b(0?[1-9]|1[0-2])\s*[/\-.]\s*(\d{2,4})\b"""
        )
            .find(text)
            ?.let {

                return it.value
                    .replace(
                        " ",
                        ""
                    )
                    .replace(
                        ".",
                        "/"
                    )
                    .replace(
                        "-",
                        "/"
                    )
            }

        Regex(
            """\b\d{4}\s+(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\b"""
        )
            .find(text)
            ?.let {

                return it.value.trim()
            }

        Regex(
            """\b(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\s*(\d{2,4})\b"""
        )
            .find(text)
            ?.let {

                val month = it.groupValues[1]
                val year = it.groupValues[2]
                return "$month $year"
            }

        return null
    }

    private fun findAllDates(
        text: String
    ): List<String> {

        val dates =
            mutableListOf<String>()

        val patterns =
            listOf(

                Regex(
                    """\b\d{4}\s*[/\-.]\s*(0?[1-9]|1[0-2])\s*[/\-.]\s*(0?[1-9]|[12][0-9]|3[01])\b"""
                ),

                Regex(
                    """\b(0?[1-9]|[12][0-9]|3[01])\s*[/\-.]\s*(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\s*[/\-.]\s*\d{2,4}\b"""
                ),

                Regex(
                    """\b(0?[1-9]|[12][0-9]|3[01])\s*[/\-.]\s*(0?[1-9]|1[0-2])\s*[/\-.]\s*\d{2,4}\b"""
                ),

                Regex(
                    """\b(0?[1-9]|1[0-2])\s*[/\-.]\s*\d{2,4}\b"""
                ),

                Regex(
                    """\b\d{4}\s+(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\b"""
                ),

                Regex(
                    """\b(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)[\s.\-]+\d{2,4}\b"""
                )
            )

        for (
        pattern in patterns
        ) {

            pattern
                .findAll(text)
                .forEach { match ->

                    val normalized =
                        normalizeDateString(
                            match.value
                        )

                    if (
                        normalized != null &&
                        !dates.contains(
                            normalized
                        )
                    ) {
                        dates.add(
                            normalized
                        )
                    }
                }
        }

        return dates
    }

    private fun collectDates(
        lines: List<String>
    ): List<String> {

        val result =
            mutableListOf<String>()

        for (line: String in lines) {

            val dates: List<String> =
                findAllDates(line)

            for (date: String in dates) {

                var alreadyExists = false

                for (existing: String in result) {

                    val existingComparable =
                        parseComparableDate(existing)

                    val currentComparable =
                        parseComparableDate(date)

                    if (
                        existingComparable != null &&
                        currentComparable != null
                    ) {
                        if (
                            existingComparable ==
                            currentComparable
                        ) {
                            alreadyExists = true
                            break
                        }
                    } else if (
                        existing.equals(
                            date,
                            ignoreCase = true
                        )
                    ) {
                        alreadyExists = true
                        break
                    }
                }

                if (!alreadyExists) {
                    result.add(date)
                }
            }
        }

        return result
    }

    private fun normalizeDateString(
        value: String
    ): String? {

        return value
            .trim()
            .replace(
                "-",
                "/"
            )
            .replace(
                ".",
                "/"
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
    }

    private data class ParsedDate(
        val year: Int,
        val month: Int,
        val day: Int? = null
    )

    private fun parseComparableDate(
        date: String
    ): Int? {

        val parsed =
            parseDate(date)
                ?: return null

        return parsed.year * 10000 +
                parsed.month * 100 +
                (
                        parsed.day
                            ?: 1
                        )
    }

    private fun parseDate(
        date: String
    ): ParsedDate? {

        val value =
            date
                .uppercase()
                .trim()

        Regex(
            """^(\d{1,2})/([A-Z]{3})/(\d{2,4})$"""
        )
            .matchEntire(value)
            ?.let { match ->

                val day =
                    match.groupValues[1]
                        .toInt()

                val month =
                    MONTH_NAMES.indexOf(
                        match.groupValues[2]
                    ) + 1

                var year =
                    match.groupValues[3]
                        .toInt()

                if (
                    year < 100
                ) {
                    year += 2000
                }

                if (
                    day in 1..31 &&
                    month in 1..12
                ) {

                    return ParsedDate(
                        year = year,
                        month = month,
                        day = day
                    )
                }
            }

        Regex(
            """^(\d{4})/(\d{1,2})/(\d{1,2})$"""
        )
            .matchEntire(value)
            ?.let { match ->

                val year =
                    match.groupValues[1]
                        .toInt()

                val month =
                    match.groupValues[2]
                        .toInt()

                val day =
                    match.groupValues[3]
                        .toInt()

                if (
                    month in 1..12 &&
                    day in 1..31
                ) {

                    return ParsedDate(
                        year = year,
                        month = month,
                        day = day
                    )
                }
            }

        Regex(
            """^(\d{1,2})/(\d{1,2})/(\d{2,4})$"""
        )
            .matchEntire(value)
            ?.let { match ->

                val day =
                    match.groupValues[1]
                        .toInt()

                val month =
                    match.groupValues[2]
                        .toInt()

                var year =
                    match.groupValues[3]
                        .toInt()

                if (
                    year < 100
                ) {
                    year += 2000
                }

                if (
                    day in 1..31 &&
                    month in 1..12
                ) {

                    return ParsedDate(
                        year = year,
                        month = month,
                        day = day
                    )
                }
            }

        Regex(
            """^(\d{1,2})/(\d{2,4})$"""
        )
            .matchEntire(value)
            ?.let { match ->

                val month =
                    match.groupValues[1]
                        .toInt()

                var year =
                    match.groupValues[2]
                        .toInt()

                if (
                    year < 100
                ) {
                    year += 2000
                }

                if (
                    month in 1..12
                ) {

                    return ParsedDate(
                        year = year,
                        month = month
                    )
                }
            }

        Regex(
            """^(\d{4})\s+([A-Z]{3})$"""
        )
            .matchEntire(value)
            ?.let { match ->

                val year =
                    match.groupValues[1]
                        .toInt()

                val month =
                    MONTH_NAMES.indexOf(
                        match.groupValues[2]
                    ) + 1

                if (
                    month in 1..12
                ) {

                    return ParsedDate(
                        year = year,
                        month = month
                    )
                }
            }

        Regex(
            """^([A-Z]{3})\s+(\d{2,4})$"""
        )
            .matchEntire(value)
            ?.let { match ->

                val month =
                    MONTH_NAMES.indexOf(
                        match.groupValues[1]
                    ) + 1

                var year =
                    match.groupValues[2]
                        .toInt()

                if (
                    year < 100
                ) {
                    year += 2000
                }

                if (
                    month in 1..12
                ) {

                    return ParsedDate(
                        year = year,
                        month = month
                    )
                }
            }

        return null
    }

    private fun addMonths(
        date: String,
        months: Int
    ): String? {

        val parsed =
            parseDate(date)
                ?: return null

        return try {

            val result =
                YearMonth
                    .of(
                        parsed.year,
                        parsed.month
                    )
                    .plusMonths(
                        months.toLong()
                    )

            "%02d/%04d".format(
                result.monthValue,
                result.year
            )

        } catch (
            e: Exception
        ) {
            null
        }
    }
}
