package com.freshtrack.ocr.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.freshtrack.ocr.R
import com.freshtrack.ocr.data.Product
import com.freshtrack.ocr.data.ProductDao
import com.freshtrack.ocr.data.ProductDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class ProductListActivity : AppCompatActivity() {

    private lateinit var productListContainer: LinearLayout
    private lateinit var productDao: ProductDao
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_product_list)

        productListContainer =
            findViewById(R.id.productListContainer)

        val database = ProductDatabase.getDatabase(this)
        productDao = database.productDao()

        findViewById<MaterialButton>(
            R.id.btnBack
        ).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(
            R.id.btnAddProduct
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    AddProductActivity::class.java
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()

        if (::productListContainer.isInitialized) {
            loadProducts()
        }
    }
    private fun loadProducts() {

        loadJob?.cancel()

        loadJob = lifecycleScope.launch {

            productListContainer.removeAllViews()

            val products =
                productDao.getAllProducts()

            if (products.isEmpty()) {
                showEmptyMessage()
                return@launch
            }

            val sortedProducts = products.sortedBy {
                getExpirySortKey(
                    getExpiryStatus(it.expiryDate)
                )
            }

            for (product in sortedProducts) {
                createProductCard(
                    product = product
                )
            }
        }
    }

    private fun getExpirySortKey(
        status: String
    ): Long {

        return when {

            status == "Expired" -> {
                Long.MIN_VALUE
            }

            status == "Expires today" -> {
                0L
            }

            status.startsWith("Expires soon") ||
                    status.startsWith("Fresh") -> {

                Regex("\\d+")
                    .find(status)
                    ?.value
                    ?.toLong()
                    ?: Long.MAX_VALUE
            }

            else -> {
                Long.MAX_VALUE
            }
        }
    }

    private fun showEmptyMessage() {

        val emptyText =
            TextView(this)

        emptyText.text =
            "No products added yet."

        emptyText.textSize =
            14f

        emptyText.gravity =
            Gravity.CENTER

        emptyText.setTextColor(
            getColor(R.color.text_secondary)
        )

        emptyText.setPadding(
            0,
            dp(12),
            0,
            dp(12)
        )

        productListContainer.addView(
            emptyText
        )
    }

    private fun createProductCard(
        product: Product
    ) {

        // -------------------------
        // CARD
        // -------------------------

        val card =
            MaterialCardView(this)

        card.setCardBackgroundColor(
            getColor(R.color.card_dark)
        )

        card.radius =
            dp(12).toFloat()

        card.strokeWidth =
            dp(1)

        card.strokeColor =
            getColor(R.color.border_light)

        // -------------------------
        // CARD CONTENT
        // -------------------------

        val cardLayout =
            LinearLayout(this)

        cardLayout.orientation =
            LinearLayout.VERTICAL

        cardLayout.setPadding(
            dp(12),
            dp(9),
            dp(12),
            dp(9)
        )

        // -------------------------
        // PRODUCT NAME
        // -------------------------

        val nameText =
            TextView(this)

        nameText.text =
            product.productName

        nameText.textSize =
            17f

        nameText.setTextColor(
            getColor(R.color.text_primary)
        )

        // -------------------------
        // EXPIRY DATE
        // -------------------------

        val expiryText =
            TextView(this)

        expiryText.text =
            "Expires: ${product.expiryDate}"

        expiryText.textSize =
            13f

        expiryText.setTextColor(
            getColor(R.color.text_secondary)
        )

        val expiryParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        expiryParams.topMargin =
            dp(2)

        // -------------------------
        // STATUS
        // -------------------------


        val status =
            getExpiryStatus(product.expiryDate)

        val statusText =
            TextView(this)

        statusText.text =
            status

        statusText.textSize =
            12f

        when {

            status.startsWith("Expired") ||
                    status.startsWith("Expires today") -> {

                statusText.setTextColor(
                    getColor(R.color.error_bright)
                )
            }

            status.startsWith("Expires soon") -> {

                statusText.setTextColor(
                    getColor(R.color.warning_amber)
                )
            }

            status.startsWith("Fresh") -> {

                statusText.setTextColor(
                    getColor(R.color.green_primary)
                )
            }

            else -> {

                statusText.setTextColor(
                    getColor(R.color.text_secondary)
                )
            }
        }

        val statusParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        statusParams.topMargin =
            dp(1)

        // -------------------------
        // BUTTON LAYOUT
        // -------------------------

        val buttonLayout =
            LinearLayout(this)

        buttonLayout.orientation =
            LinearLayout.HORIZONTAL

        buttonLayout.gravity =
            Gravity.CENTER_VERTICAL

        // -------------------------
        // EDIT BUTTON
        // -------------------------

        val editButton =
            MaterialButton(this)

        editButton.text =
            "Edit"

        editButton.textSize =
            11f

        editButton.gravity =
            Gravity.CENTER

        editButton.setTextColor(
            getColor(R.color.green_primary)
        )

        // White background
        editButton.backgroundTintList =
            ColorStateList.valueOf(
                Color.WHITE
            )

        // Light outline
        editButton.strokeColor =
            ColorStateList.valueOf(
                getColor(R.color.border_light)
            )

        editButton.minHeight =
            0

        editButton.minWidth =
            0

        editButton.insetTop =
            0

        editButton.insetBottom =
            0

        editButton.setPadding(
            0,
            0,
            0,
            0
        )

        editButton.strokeWidth =
            dp(1)

        editButton.cornerRadius =
            dp(9)

        // -------------------------
        // DELETE BUTTON
        // -------------------------

        val deleteButton =
            MaterialButton(this)

        deleteButton.text =
            "Delete"

        deleteButton.textSize =
            11f

        deleteButton.gravity =
            Gravity.CENTER

        deleteButton.setTextColor(
            getColor(R.color.green_primary)
        )

        // White background
        deleteButton.backgroundTintList =
            ColorStateList.valueOf(
                Color.WHITE
            )

        // Light outline
        deleteButton.strokeColor =
            ColorStateList.valueOf(
                getColor(R.color.border_light)
            )

        deleteButton.minHeight =
            0

        deleteButton.minWidth =
            0

        deleteButton.insetTop =
            0

        deleteButton.insetBottom =
            0

        deleteButton.setPadding(
            0,
            0,
            0,
            0
        )

        deleteButton.strokeWidth =
            dp(1)

        deleteButton.cornerRadius =
            dp(9)

        // -------------------------
        // EDIT CLICK
        // -------------------------

        editButton.setOnClickListener {

            val intent =
                Intent(
                    this,
                    AddProductActivity::class.java
                )

            intent.putExtra(
                "productId",
                product.id
            )

            intent.putExtra(
                "productName",
                product.productName
            )

            intent.putExtra(
                "expiryDate",
                product.expiryDate
            )

            startActivity(intent)
        }

        // -------------------------
        // DELETE CLICK
        // -------------------------

        deleteButton.setOnClickListener {

            lifecycleScope.launch {

                productDao.deleteProduct(product)

                loadProducts()
            }
        }

        // -------------------------
        // BUTTON SIZES
        // -------------------------

        val editParams =
            LinearLayout.LayoutParams(
                0,
                dp(34),
                1f
            )

        editParams.setMargins(
            0,
            dp(5),
            dp(4),
            0
        )

        val deleteParams =
            LinearLayout.LayoutParams(
                0,
                dp(34),
                1f
            )

        deleteParams.setMargins(
            dp(4),
            dp(5),
            0,
            0
        )

        buttonLayout.addView(
            editButton,
            editParams
        )

        buttonLayout.addView(
            deleteButton,
            deleteParams
        )

        // -------------------------
        // ADD CONTENT TO CARD
        // -------------------------

        cardLayout.addView(
            nameText
        )

        cardLayout.addView(
            expiryText,
            expiryParams
        )

        cardLayout.addView(
            statusText,
            statusParams
        )

        cardLayout.addView(
            buttonLayout
        )

        card.addView(
            cardLayout
        )

        // -------------------------
        // CARD SPACING
        // -------------------------

        val cardParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        cardParams.setMargins(
            0,
            0,
            0,
            dp(7)
        )

        productListContainer.addView(
            card,
            cardParams
        )
    }

    // -------------------------
    // EXPIRY STATUS
    // -------------------------

    private fun getExpiryStatus(
        expiryDate: String
    ): String {

        return try {

            val date =
                expiryDate.trim()

            val expiryCalendar =
                Calendar.getInstance()

            var parsed = false

            // --------------------------------
            // FORMAT 1: dd/MM/yyyy
            // Example: 16/05/2026
            // --------------------------------

            if (
                date.matches(
                    Regex(
                        "^\\d{1,2}/\\d{1,2}/\\d{4}$"
                    )
                )
            ) {

                val parts =
                    date.split("/")

                val day =
                    parts[0].toInt()

                val month =
                    parts[1].toInt()

                val year =
                    parts[2].toInt()

                if (
                    month in 1..12 &&
                    day in 1..31
                ) {

                    expiryCalendar.clear()

                    expiryCalendar.set(
                        year,
                        month - 1,
                        day
                    )

                    parsed = true
                }
            }

            // --------------------------------
            // FORMAT 2: dd Month yyyy
            // Example: 25 September 2026
            // --------------------------------

            if (!parsed) {

                val match =
                    Regex(
                        "(?i)^(\\d{1,2})\\s+" +
                                "(January|February|March|April|May|June|July|August|September|October|November|December)" +
                                "\\s+(\\d{4})$"
                    ).find(date)

                if (match != null) {

                    val day =
                        match.groupValues[1].toInt()

                    val monthName =
                        match.groupValues[2]
                            .lowercase(Locale.ENGLISH)

                    val year =
                        match.groupValues[3].toInt()

                    val month =
                        when (monthName) {

                            "january" ->
                                Calendar.JANUARY

                            "february" ->
                                Calendar.FEBRUARY

                            "march" ->
                                Calendar.MARCH

                            "april" ->
                                Calendar.APRIL

                            "may" ->
                                Calendar.MAY

                            "june" ->
                                Calendar.JUNE

                            "july" ->
                                Calendar.JULY

                            "august" ->
                                Calendar.AUGUST

                            "september" ->
                                Calendar.SEPTEMBER

                            "october" ->
                                Calendar.OCTOBER

                            "november" ->
                                Calendar.NOVEMBER

                            "december" ->
                                Calendar.DECEMBER

                            else -> -1
                        }

                    if (month >= 0) {

                        expiryCalendar.clear()

                        expiryCalendar.set(
                            year,
                            month,
                            day
                        )

                        parsed = true
                    }
                }
            }

            // --------------------------------
            // FORMAT 3: dd MMM yyyy
            // Examples:
            // 16 Oct 2026
            // 25 Sep 2026
            // 25 Sept 2026
            // --------------------------------

            if (!parsed) {

                val match =
                    Regex(
                        "(?i)^(\\d{1,2})\\s+" +
                                "(Jan|January|Feb|February|Mar|March|Apr|April|May|Jun|June|Jul|July|Aug|August|Sep|Sept|September|Oct|October|Nov|November|Dec|December)" +
                                "\\s+(\\d{4})$"
                    ).find(date)

                if (match != null) {

                    val day =
                        match.groupValues[1].toInt()

                    val monthName =
                        match.groupValues[2]
                            .lowercase(Locale.ENGLISH)

                    val year =
                        match.groupValues[3].toInt()

                    val month =
                        when (monthName) {

                            "jan",
                            "january" ->
                                Calendar.JANUARY

                            "feb",
                            "february" ->
                                Calendar.FEBRUARY

                            "mar",
                            "march" ->
                                Calendar.MARCH

                            "apr",
                            "april" ->
                                Calendar.APRIL

                            "may" ->
                                Calendar.MAY

                            "jun",
                            "june" ->
                                Calendar.JUNE

                            "jul",
                            "july" ->
                                Calendar.JULY

                            "aug",
                            "august" ->
                                Calendar.AUGUST

                            "sep",
                            "sept",
                            "september" ->
                                Calendar.SEPTEMBER

                            "oct",
                            "october" ->
                                Calendar.OCTOBER

                            "nov",
                            "november" ->
                                Calendar.NOVEMBER

                            "dec",
                            "december" ->
                                Calendar.DECEMBER

                            else -> -1
                        }

                    if (month >= 0) {

                        expiryCalendar.clear()

                        expiryCalendar.set(
                            year,
                            month,
                            day
                        )

                        parsed = true
                    }
                }
            }

            // --------------------------------
            // FORMAT 4: MM/yyyy
            // Example: 12/2026
            //
            // Uses LAST DAY of month
            // --------------------------------

            if (!parsed) {

                if (
                    date.matches(
                        Regex(
                            "^\\d{1,2}/\\d{4}$"
                        )
                    )
                ) {

                    val parts =
                        date.split("/")

                    val month =
                        parts[0].toInt()

                    val year =
                        parts[1].toInt()

                    if (month in 1..12) {

                        expiryCalendar.clear()

                        expiryCalendar.set(
                            year,
                            month - 1,
                            1
                        )

                        expiryCalendar.set(
                            Calendar.DAY_OF_MONTH,
                            expiryCalendar.getActualMaximum(
                                Calendar.DAY_OF_MONTH
                            )
                        )

                        parsed = true
                    }
                }
            }

            // --------------------------------
            // FORMAT 5: MMM yyyy
            // Example: APR 2026
            //
            // Uses LAST DAY of month
            // --------------------------------

            if (!parsed) {

                val match =
                    Regex(
                        "(?i)^(Jan|January|Feb|February|Mar|March|Apr|April|May|Jun|June|Jul|July|Aug|August|Sep|Sept|September|Oct|October|Nov|November|Dec|December)" +
                                "\\s+(\\d{4})$"
                    ).find(date)

                if (match != null) {

                    val monthName =
                        match.groupValues[1]
                            .lowercase(Locale.ENGLISH)

                    val year =
                        match.groupValues[2].toInt()

                    val month =
                        when (monthName) {

                            "jan",
                            "january" ->
                                Calendar.JANUARY

                            "feb",
                            "february" ->
                                Calendar.FEBRUARY

                            "mar",
                            "march" ->
                                Calendar.MARCH

                            "apr",
                            "april" ->
                                Calendar.APRIL

                            "may" ->
                                Calendar.MAY

                            "jun",
                            "june" ->
                                Calendar.JUNE

                            "jul",
                            "july" ->
                                Calendar.JULY

                            "aug",
                            "august" ->
                                Calendar.AUGUST

                            "sep",
                            "sept",
                            "september" ->
                                Calendar.SEPTEMBER

                            "oct",
                            "october" ->
                                Calendar.OCTOBER

                            "nov",
                            "november" ->
                                Calendar.NOVEMBER

                            "dec",
                            "december" ->
                                Calendar.DECEMBER

                            else -> -1
                        }

                    if (month >= 0) {

                        expiryCalendar.clear()

                        expiryCalendar.set(
                            year,
                            month,
                            1
                        )

                        expiryCalendar.set(
                            Calendar.DAY_OF_MONTH,
                            expiryCalendar.getActualMaximum(
                                Calendar.DAY_OF_MONTH
                            )
                        )

                        parsed = true
                    }
                }
            }

            // --------------------------------
            // FORMAT 6: MMM/yyyy
            // Example: SEP/2026
            //
            // Uses LAST DAY of month
            // --------------------------------

            if (!parsed) {

                val match =
                    Regex(
                        "(?i)^(Jan|January|Feb|February|Mar|March|Apr|April|May|Jun|June|Jul|July|Aug|August|Sep|Sept|September|Oct|October|Nov|November|Dec|December)" +
                                "/(\\d{4})$"
                    ).find(date)

                if (match != null) {

                    val monthName =
                        match.groupValues[1]
                            .lowercase(Locale.ENGLISH)

                    val year =
                        match.groupValues[2].toInt()

                    val month =
                        when (monthName) {

                            "jan",
                            "january" ->
                                Calendar.JANUARY

                            "feb",
                            "february" ->
                                Calendar.FEBRUARY

                            "mar",
                            "march" ->
                                Calendar.MARCH

                            "apr",
                            "april" ->
                                Calendar.APRIL

                            "may" ->
                                Calendar.MAY

                            "jun",
                            "june" ->
                                Calendar.JUNE

                            "jul",
                            "july" ->
                                Calendar.JULY

                            "aug",
                            "august" ->
                                Calendar.AUGUST

                            "sep",
                            "sept",
                            "september" ->
                                Calendar.SEPTEMBER

                            "oct",
                            "october" ->
                                Calendar.OCTOBER

                            "nov",
                            "november" ->
                                Calendar.NOVEMBER

                            "dec",
                            "december" ->
                                Calendar.DECEMBER

                            else -> -1
                        }

                    if (month >= 0) {

                        expiryCalendar.clear()

                        expiryCalendar.set(
                            year,
                            month,
                            1
                        )

                        expiryCalendar.set(
                            Calendar.DAY_OF_MONTH,
                            expiryCalendar.getActualMaximum(
                                Calendar.DAY_OF_MONTH
                            )
                        )

                        parsed = true
                    }
                }
            }

            // --------------------------------
            // FORMAT 7: MMM yy
            // Example: SEP 26
            //
            // Uses LAST DAY of month
            // --------------------------------

            if (!parsed) {

                val match =
                    Regex(
                        "(?i)^(Jan|January|Feb|February|Mar|March|Apr|April|May|Jun|June|Jul|July|Aug|August|Sep|Sept|September|Oct|October|Nov|November|Dec|December)" +
                                "\\s+(\\d{2})$"
                    ).find(date)

                if (match != null) {

                    val monthName =
                        match.groupValues[1]
                            .lowercase(Locale.ENGLISH)

                    val shortYear =
                        match.groupValues[2].toInt()

                    val year =
                        2000 + shortYear

                    val month =
                        when (monthName) {

                            "jan",
                            "january" ->
                                Calendar.JANUARY

                            "feb",
                            "february" ->
                                Calendar.FEBRUARY

                            "mar",
                            "march" ->
                                Calendar.MARCH

                            "apr",
                            "april" ->
                                Calendar.APRIL

                            "may" ->
                                Calendar.MAY

                            "jun",
                            "june" ->
                                Calendar.JUNE

                            "jul",
                            "july" ->
                                Calendar.JULY

                            "aug",
                            "august" ->
                                Calendar.AUGUST

                            "sep",
                            "sept",
                            "september" ->
                                Calendar.SEPTEMBER

                            "oct",
                            "october" ->
                                Calendar.OCTOBER

                            "nov",
                            "november" ->
                                Calendar.NOVEMBER

                            "dec",
                            "december" ->
                                Calendar.DECEMBER

                            else -> -1
                        }

                    if (month >= 0) {

                        expiryCalendar.clear()

                        expiryCalendar.set(
                            year,
                            month,
                            1
                        )

                        expiryCalendar.set(
                            Calendar.DAY_OF_MONTH,
                            expiryCalendar.getActualMaximum(
                                Calendar.DAY_OF_MONTH
                            )
                        )

                        parsed = true
                    }
                }
            }

            // --------------------------------
            // FORMAT 8: MM/yy
            // Example: 04/27
            //
            // Uses LAST DAY of month
            // --------------------------------

            if (!parsed) {

                if (
                    date.matches(
                        Regex(
                            "^\\d{1,2}/\\d{2}$"
                        )
                    )
                ) {

                    val parts =
                        date.split("/")

                    val month =
                        parts[0].toInt()

                    val shortYear =
                        parts[1].toInt()

                    val year =
                        2000 + shortYear

                    if (month in 1..12) {

                        expiryCalendar.clear()

                        expiryCalendar.set(
                            year,
                            month - 1,
                            1
                        )

                        expiryCalendar.set(
                            Calendar.DAY_OF_MONTH,
                            expiryCalendar.getActualMaximum(
                                Calendar.DAY_OF_MONTH
                            )
                        )

                        parsed = true
                    }
                }
            }

            // --------------------------------
            // IF DATE COULD NOT BE PARSED
            // --------------------------------

            if (!parsed) {
                return "Status unavailable"
            }

            // --------------------------------
            // TODAY
            // --------------------------------

            val today =
                Calendar.getInstance()

            today.set(
                Calendar.HOUR_OF_DAY,
                0
            )

            today.set(
                Calendar.MINUTE,
                0
            )

            today.set(
                Calendar.SECOND,
                0
            )

            today.set(
                Calendar.MILLISECOND,
                0
            )

            // --------------------------------
            // EXPIRY DATE
            // --------------------------------

            expiryCalendar.set(
                Calendar.HOUR_OF_DAY,
                0
            )

            expiryCalendar.set(
                Calendar.MINUTE,
                0
            )

            expiryCalendar.set(
                Calendar.SECOND,
                0
            )

            expiryCalendar.set(
                Calendar.MILLISECOND,
                0
            )

            // --------------------------------
            // CALCULATE DAYS LEFT
            // --------------------------------

            val difference =
                expiryCalendar.timeInMillis -
                        today.timeInMillis

            val daysLeft =
                difference /
                        (1000 * 60 * 60 * 24)

            // --------------------------------
            // STATUS
            // --------------------------------

            when {

                daysLeft < 0L -> {
                    "Expired"
                }

                daysLeft == 0L -> {
                    "Expires today"
                }

                daysLeft <= 7L -> {
                    "Expires soon — $daysLeft days left"
                }

                else -> {
                    "Fresh — $daysLeft days left"
                }
            }

        } catch (e: Exception) {

            "Status unavailable"
        }
    }

    // -------------------------
    // DP HELPER
    // -------------------------

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }

    override fun onDestroy() {

        loadJob?.cancel()

        super.onDestroy()
    }
}