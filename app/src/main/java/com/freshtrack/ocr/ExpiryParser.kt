package com.freshtrack.ocr

import java.time.YearMonth

object ExpiryParser {

    fun extractExpiryDate(text: String): String? {

        val lines = text
            .uppercase()
            .replace("\r", "")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }


        // PRIORITY 1: @ means expiry date
        for (line in lines) {
            if (line.contains("@")) {

                val afterAt = line.substringAfter("@")
                val date = findDate(afterAt)

                if (date != null) {
                    return date
                }
            }
        }


        // PRIORITY 2:
        // Handles MFG + EXPIRY labels followed by dates
        for (i in lines.indices) {

            val isMfgLabel =
                lines[i].contains("MFG") ||
                        lines[i].contains("MFD") ||
                        lines[i].contains("MANUFACTUR")

            if (isMfgLabel && i + 1 < lines.size) {

                val nextLineIsExpiry =
                    lines[i + 1].contains("EXP") ||
                            lines[i + 1].contains("EXPIRY")

                if (nextLineIsExpiry) {

                    val datesAfterLabels = mutableListOf<String>()

                    for (j in i + 2 until lines.size) {

                        val date = findDate(lines[j])

                        if (date != null) {
                            datesAfterLabels.add(date)
                        }

                        if (datesAfterLabels.size == 2) {
                            break
                        }
                    }

                    if (datesAfterLabels.size >= 2) {
                        return datesAfterLabels[1]
                    }
                }
            }
        }


        // PRIORITY 3:
        // EXP, EXPIRY, USE BY, BEST BEFORE with date on same line
        for (line in lines) {

            if (
                line.contains("EXP") ||
                line.contains("EXPIRY") ||
                line.contains("USE BY") ||
                line.contains("BEST BEFORE")
            ) {

                val date = findDate(line)

                if (date != null) {
                    return date
                }
            }
        }


        // PRIORITY 4:
        // BEST BEFORE 12 MONTHS
        val bestBeforeMonths = findBestBeforeMonths(text)

        if (bestBeforeMonths != null) {

            val manufacturingDate = findManufacturingDate(lines)

            if (manufacturingDate != null) {

                val calculatedDate =
                    addMonths(manufacturingDate, bestBeforeMonths)

                if (calculatedDate != null) {
                    return calculatedDate
                }
            }
        }


        // PRIORITY 5:
        // Expiry label with date on following lines
        for (i in lines.indices) {

            val line = lines[i]

            if (
                line.contains("EXP") ||
                line.contains("EXPIRY") ||
                line.contains("USE BY") ||
                line.contains("BEST BEFORE")
            ) {
                val datesAfterLabel = mutableListOf<String>()

                // Look ahead up to 10 lines
                for (j in 1..10) {

                    if (i + j < lines.size) {

                        val date = findDate(lines[i + j])

                        if (date != null) {
                            datesAfterLabel.add(date)
                        }
                    }
                }
                if (datesAfterLabel.size >= 2) {
                    return datesAfterLabel.last()
                }

                if (datesAfterLabel.size == 1) {
                    return datesAfterLabel.first()
                }
            }
        }

        // PRIORITY 6:
        // No clear label -> find all dates and use the last one
        val foundDates = mutableListOf<String>()

        for (line in lines) {
            val isManufacturingLine =
                line.contains("MFG") ||
                        line.contains("MFD") ||
                        line.contains("MANUFACTUR")
            if (!isManufacturingLine) {
                val dates = findAllDates(line)
                foundDates.addAll(dates)
            }
        }

        if (foundDates.size >= 2) {
            return foundDates.last()
        }

        return foundDates.firstOrNull()
    }


    // Finds: BEST BEFORE 12 MONTHS
    private fun findBestBeforeMonths(text: String): Int? {

        val match = Regex(
            """BEST\s*BEFORE\s*(\d+)\s*MONTHS?"""
        ).find(text.uppercase())

        return match?.groupValues?.get(1)?.toIntOrNull()
    }


    // Finds manufacturing date
    private fun findManufacturingDate(
        lines: List<String>
    ): String? {

        for (line in lines) {

            if (
                line.contains("MFG") ||
                line.contains("MFD") ||
                line.contains("MANUFACTUR") ||
                line.contains("PACKED ON")
            ) {

                val date = findDate(line)

                if (date != null) {
                    return date
                }
            }
        }

        return null
    }


    // Adds months to MM/YY or MM/YYYY
    private fun addMonths(
        date: String,
        months: Int
    ): String? {

        return try {

            val parts = date.split("/")

            val month = parts[0].toInt()

            var year = parts[1].toInt()

            if (year < 100) {
                year += 2000
            }

            val result =
                YearMonth.of(year, month)
                    .plusMonths(months.toLong())

            "%02d/%02d".format(
                result.monthValue,
                result.year % 100
            )

        } catch (e: Exception) {
            null
        }
    }


    // Finds one date
    private fun findDate(text: String): String? {

        // YYYY MON
        // Example: 2025 JUL
        val yearMonthName =
            Regex(
                """\b(\d{4})\s+(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\b"""
            ).find(text)

        if (yearMonthName != null) {
            return yearMonthName.value.trim()
        }


        // YYYY/MM/DD
        // Example: 2027/05/11
        val yearFirstDate =
            Regex(
                """\b(\d{4})\s*[/\-.]\s*(0?[1-9]|1[0-2])\s*[/\-.]\s*(0?[1-9]|[12][0-9]|3[01])\b"""
            ).find(text)

        if (yearFirstDate != null) {
            return yearFirstDate.value
                .replace(" ", "")
                .replace("-", "/")
                .replace(".", "/")
        }


        // DD/MM/YYYY
        // Example: 11/05/2027
        val fullDate =
            Regex(
                """\b(0?[1-9]|[12][0-9]|3[01])\s*[/\-.]\s*(0?[1-9]|1[0-2])\s*[/\-.]\s*(\d{4})\b"""
            ).find(text)

        if (fullDate != null) {
            return fullDate.value
                .replace(" ", "")
                .replace("-", "/")
                .replace(".", "/")
        }


        // MM/YY or MM/YYYY
        // Example: 04/23
        val numericDate =
            Regex(
                """\b(0[1-9]|1[0-2])\s*[/\-.]\s*(\d{2,4})\b"""
            ).find(text)

        if (numericDate != null) {
            return numericDate.value
                .replace(" ", "")
                .replace(".", "/")
                .replace("-", "/")
        }


        // MON YYYY
        // Example: APR 2027
        val monthDate =
            Regex(
                """\b(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)[\s.\-]*(\d{2,4})\b"""
            ).find(text)

        if (monthDate != null) {
            return monthDate.value
                .replace(".", " ")
                .replace("-", " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
        }

        return null
    }


    // Finds ALL dates in one line
    private fun findAllDates(text: String): List<String> {

        val dates = mutableListOf<String>()

        // Numeric dates
        val numericPattern =
            Regex(
                """\b(0[1-9]|1[0-2])\s*[/\-.]\s*(\d{2,4})\b"""
            )

        numericPattern.findAll(text).forEach { match ->

            dates.add(
                match.value
                    .replace(" ", "")
                    .replace(".", "/")
                    .replace("-", "/")
            )
        }


        // Month-name dates
        val monthPattern =
            Regex(
                """\b(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)[\s.\-]+(\d{2,4})\b"""
            )

        monthPattern.findAll(text).forEach { match ->

            dates.add(
                match.value
                    .replace(".", " ")
                    .replace("-", " ")
                    .replace(Regex("""\s+"""), " ")
                    .trim()
            )
        }

        return dates
    }
}







