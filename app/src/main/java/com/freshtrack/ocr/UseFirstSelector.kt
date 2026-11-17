package com.freshtrack.ocr

import com.freshtrack.ocr.data.Product
import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** How soon a "use first" item runs out. */
enum class Urgency {
    TODAY,          // expires today
    WITHIN_3_DAYS,  // 1 to 3 days left
    THIS_WEEK,      // 4 to 7 days left
    LATER           // more than 7 days left
}

/** A food item that is still good, with the days left. */
data class UseFirstItem(
    val product: Product,
    val expiry: LocalDate,
    val daysLeft: Int,
    val urgency: Urgency
)

/** An item whose date has passed. Shown separately and never as "use first". */
data class ExpiredItem(
    val product: Product,
    val expiry: LocalDate,
    val daysAgo: Int
)

data class UseFirstResult(
    /** Food that is still good, earliest expiry first. */
    val useFirst: List<UseFirstItem>,
    /** Anything past its date, most recently expired first. Check and discard. */
    val expired: List<ExpiredItem>,
    /** Food whose date could not be read. */
    val unknownDate: List<Product>
) {
    /** The first [count] items, for a small card on the home screen. */
    fun top(count: Int): List<UseFirstItem> = useFirst.take(count)
}

/**
 * Works out which products to use first.
 *
 * Rules:
 *  1. Each saved date is read into a real date. A month-and-year date such as 11/2026 counts
 *     as the last day of that month.
 *  2. A date before today means expired. Expired items are listed separately, for every
 *     category, and are never put in the "use first" list.
 *  3. Only food categories can be "use first". Medicines, cosmetics, personal care, cleaning
 *     and pet items are left out, because "use it up" does not apply to them. They still get
 *     expiry reminders and still show up as expired.
 *  4. Food that is still good is sorted by earliest expiry. On the same day, fresh food
 *     (fruit, dairy, meat, bakery) comes before drinks, snacks and pantry items, then by name.
 *  5. Food with a date that cannot be read goes in its own list. A date is never guessed.
 *
 * This file does not touch the database or any screen, so it can be tested on its own.
 */
object UseFirstSelector {

    /** Food categories (the same names as the category list in the app). */
    private val FOOD_ORDER = listOf(
        "fruits & vegetables",
        "dairy",
        "meat & seafood",
        "bakery",
        "beverages",
        "snacks",
        "pantry",
        "other"           // the default category: could be food, so it is included
    )

    private val NOT_FOOD = setOf(
        "personal care",
        "cleaning supplies",
        "pet supplies",
        "cosmetics",
        "medicines"
    )

    fun select(
        products: List<Product>,
        today: LocalDate = LocalDate.now()
    ): UseFirstResult {

        val useFirst = ArrayList<UseFirstItem>()
        val expired = ArrayList<ExpiredItem>()
        val unknown = ArrayList<Product>()

        for (product in products) {

            val expiry = parseExpiry(product.expiryDate)
            val food = isFood(product.category)

            if (expiry == null) {
                if (food) unknown += product
                continue
            }

            val daysLeft = ChronoUnit.DAYS.between(today, expiry).toInt()

            when {
                daysLeft < 0 ->
                    expired += ExpiredItem(product, expiry, -daysLeft)

                food ->
                    useFirst += UseFirstItem(product, expiry, daysLeft, urgencyOf(daysLeft))

                else -> Unit   // not food: no "use first" nudge
            }
        }

        return UseFirstResult(
            useFirst = useFirst.sortedWith(
                compareBy<UseFirstItem> { it.expiry }
                    .thenBy { freshnessRank(it.product.category) }
                    .thenBy { it.product.productName.lowercase() }
            ),
            expired = expired.sortedWith(
                compareByDescending<ExpiredItem> { it.expiry }
                    .thenBy { it.product.productName.lowercase() }
            ),
            unknownDate = unknown.sortedBy { it.productName.lowercase() }
        )
    }

    fun isFood(category: String): Boolean {
        val key = category.trim().lowercase()
        return key !in NOT_FOOD
    }

    fun urgencyOf(daysLeft: Int): Urgency = when {
        daysLeft <= 0 -> Urgency.TODAY
        daysLeft <= 3 -> Urgency.WITHIN_3_DAYS
        daysLeft <= 7 -> Urgency.THIS_WEEK
        else -> Urgency.LATER
    }

    /** Reads the saved date text. Returns null if it cannot be read. */
    fun parseExpiry(text: String): LocalDate? {
        val result = ExpiryDateParser.extract(text) ?: return null
        return try {
            val day = result.day
            if (day != null) {
                LocalDate.of(result.year, result.month, day)
            } else {
                YearMonth.of(result.year, result.month).atEndOfMonth()
            }
        } catch (e: DateTimeException) {
            null
        }
    }

    private fun freshnessRank(category: String): Int {
        val index = FOOD_ORDER.indexOf(category.trim().lowercase())
        return if (index >= 0) index else FOOD_ORDER.size
    }
}