package com.freshtrack.ocr.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProductListActivity : AppCompatActivity() {

    private lateinit var productListContainer: LinearLayout
    private lateinit var productDao: ProductDao
    private lateinit var etSearchProducts: TextInputEditText
    private lateinit var actvFilterCategory: AutoCompleteTextView

    private var loadJob: Job? = null

    private var allProducts: List<Product> = emptyList()
    private var searchQuery = ""
    private var selectedCategory = "All"

    private val filterCategories = listOf(
        "All",
        "Fruits & Vegetables",
        "Dairy & Eggs",
        "Meat & Seafood",
        "Bakery",
        "Beverages",
        "Snacks",
        "Pantry & Groceries",
        "Personal Care",
        "Medicines & Health",
        "Cleaning Supplies",
        "Baby Care",
        "Pet Supplies",
        "Cosmetics",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_product_list)

        productListContainer =
            findViewById(R.id.productListContainer)

        etSearchProducts =
            findViewById(R.id.etSearchProducts)

        actvFilterCategory =
            findViewById(R.id.activeFilterCategory)

        val database =
            ProductDatabase.getDatabase(this)

        productDao = database.productDao()

        setupCategoryFilter()
        setupSearch()

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

    private fun setupCategoryFilter() {

        val adapter = ArrayAdapter(
            this,
            R.layout.item_category_dropdown,
            filterCategories
        )

        actvFilterCategory.setAdapter(adapter)
        actvFilterCategory.setText("All", false)

        actvFilterCategory.setOnItemClickListener {
                _, _, position, _ ->

            selectedCategory = filterCategories[position]

            applySearchAndFilter()
        }
    }

    private fun setupSearch() {

        etSearchProducts.addTextChangedListener(
            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    searchQuery = s.toString().trim()
                    applySearchAndFilter()
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {}
            }
        )
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

            allProducts = productDao.getAllProducts()

            applySearchAndFilter()
        }
    }

    private fun applySearchAndFilter() {

        productListContainer.removeAllViews()

        if (allProducts.isEmpty()) {
            showEmptyMessage()
            return
        }

        val filteredProducts = allProducts.filter { product ->

            val matchesSearch =
                product.productName.contains(
                    searchQuery,
                    ignoreCase = true
                )

            val matchesCategory =
                selectedCategory == "All" ||
                        product.category.equals(
                            selectedCategory,
                            ignoreCase = true
                        )

            matchesSearch && matchesCategory
        }

        if (filteredProducts.isEmpty()) {
            showNoResultsMessage()
            return
        }

        val sortedProducts = filteredProducts.sortedBy {
            getExpirySortKey(
                getExpiryStatus(it.expiryDate)
            )
        }

        for (product in sortedProducts) {
            createProductCard(product)
        }
    }

    private fun getExpirySortKey(status: String): Long {

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

        val emptyText = TextView(this)

        emptyText.text =
            getString(R.string.products_empty)

        emptyText.textSize = 14f
        emptyText.gravity = Gravity.CENTER

        emptyText.setTextColor(
            getColor(R.color.text_secondary)
        )

        emptyText.setPadding(
            0,
            dp(12),
            0,
            dp(12)
        )

        productListContainer.addView(emptyText)
    }

    private fun showNoResultsMessage() {

        val emptyText = TextView(this)

        emptyText.text =
            "No matching products found."

        emptyText.textSize = 14f
        emptyText.gravity = Gravity.CENTER

        emptyText.setTextColor(
            getColor(R.color.text_secondary)
        )

        emptyText.setPadding(
            0,
            dp(20),
            0,
            dp(20)
        )

        productListContainer.addView(emptyText)
    }

    private fun createProductCard(product: Product) {

        val card = MaterialCardView(this)

        card.setCardBackgroundColor(
            getColor(R.color.card_dark)
        )

        card.radius = dp(12).toFloat()
        card.strokeWidth = dp(1)

        card.strokeColor =
            getColor(R.color.border_light)

        val cardLayout = LinearLayout(this)

        cardLayout.orientation =
            LinearLayout.VERTICAL

        cardLayout.setPadding(
            dp(12),
            dp(9),
            dp(12),
            dp(9)
        )

        // Product name

        val nameText = TextView(this)

        nameText.text = product.productName
        nameText.textSize = 17f

        nameText.setTextColor(
            getColor(R.color.text_primary)
        )

        // Category

        val categoryText = TextView(this)

        categoryText.text = product.category
        categoryText.textSize = 12f

        categoryText.setTextColor(
            getColor(R.color.text_secondary)
        )

        val categoryParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        categoryParams.topMargin = dp(2)

        // Expiry date

        val expiryText = TextView(this)

        expiryText.text = getString(
            R.string.expires_label,
            product.expiryDate
        )

        expiryText.textSize = 13f

        expiryText.setTextColor(
            getColor(R.color.text_secondary)
        )

        val expiryParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        expiryParams.topMargin = dp(2)

        // Expiry status

        val status = getExpiryStatus(
            product.expiryDate
        )

        val statusText = TextView(this)

        statusText.text = status
        statusText.textSize = 12f

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

        val statusParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        statusParams.topMargin = dp(1)

        // Button layout

        val buttonLayout = LinearLayout(this)

        buttonLayout.orientation =
            LinearLayout.HORIZONTAL

        buttonLayout.gravity =
            Gravity.CENTER_VERTICAL

        // Edit button

        val editButton = MaterialButton(this)

        editButton.text =
            getString(R.string.button_edit)

        editButton.textSize = 11f
        editButton.gravity = Gravity.CENTER

        editButton.setTextColor(
            getColor(R.color.green_primary)
        )

        editButton.backgroundTintList =
            ColorStateList.valueOf(Color.WHITE)

        editButton.strokeColor =
            ColorStateList.valueOf(
                getColor(R.color.border_light)
            )

        editButton.minHeight = 0
        editButton.minWidth = 0
        editButton.insetTop = 0
        editButton.insetBottom = 0

        editButton.setPadding(0, 0, 0, 0)
        editButton.strokeWidth = dp(1)
        editButton.cornerRadius = dp(9)

        // Delete button

        val deleteButton = MaterialButton(this)

        deleteButton.text =
            getString(R.string.button_delete)

        deleteButton.textSize = 11f
        deleteButton.gravity = Gravity.CENTER

        deleteButton.setTextColor(
            getColor(R.color.green_primary)
        )

        deleteButton.backgroundTintList =
            ColorStateList.valueOf(Color.WHITE)

        deleteButton.strokeColor =
            ColorStateList.valueOf(
                getColor(R.color.border_light)
            )

        deleteButton.minHeight = 0
        deleteButton.minWidth = 0
        deleteButton.insetTop = 0
        deleteButton.insetBottom = 0

        deleteButton.setPadding(0, 0, 0, 0)
        deleteButton.strokeWidth = dp(1)
        deleteButton.cornerRadius = dp(9)

        // Edit click

        editButton.setOnClickListener {

            val intent = Intent(
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

            intent.putExtra(
                "category",
                product.category
            )

            startActivity(intent)
        }

        // Delete click

        deleteButton.setOnClickListener {

            lifecycleScope.launch {

                productDao.deleteProduct(product)

                loadProducts()
            }
        }

        // Button sizes

        val editParams = LinearLayout.LayoutParams(
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

        val deleteParams = LinearLayout.LayoutParams(
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

        // Add content to card

        cardLayout.addView(nameText)

        cardLayout.addView(
            categoryText,
            categoryParams
        )

        cardLayout.addView(
            expiryText,
            expiryParams
        )

        cardLayout.addView(
            statusText,
            statusParams
        )

        cardLayout.addView(buttonLayout)

        card.addView(cardLayout)

        // Card spacing

        val cardParams = LinearLayout.LayoutParams(
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

    // Expiry status

    private fun getExpiryStatus(
        expiryDate: String
    ): String {

        return try {

            val date = expiryDate.trim()

            val formats = listOf(
                "dd/MM/yyyy",
                "d/M/yyyy",
                "dd MMMM yyyy",
                "d MMMM yyyy",
                "dd MMM yyyy",
                "d MMM yyyy",
                "MM/yyyy",
                "M/yyyy",
                "MMM yyyy",
                "MMMM yyyy",
                "MMM/yyyy",
                "MMMM/yyyy",
                "MMM yy",
                "MMMM yy",
                "MM/yy",
                "M/yy"
            )

            var expiryCalendar: Calendar? = null

            for (format in formats) {

                val formatter =
                    SimpleDateFormat(
                        format,
                        Locale.ENGLISH
                    )

                formatter.isLenient = false

                val parsedDate: Date? =
                    try {
                        formatter.parse(date)
                    } catch (e: Exception) {
                        null
                    }

                if (parsedDate != null) {

                    val calendar =
                        Calendar.getInstance()

                    calendar.time = parsedDate

                    // Month-only formats use the last day
                    // of the month as the expiry date.

                    if (
                        format == "MM/yyyy" ||
                        format == "M/yyyy" ||
                        format == "MMM yyyy" ||
                        format == "MMMM yyyy" ||
                        format == "MMM/yyyy" ||
                        format == "MMMM/yyyy" ||
                        format == "MMM yy" ||
                        format == "MMMM yy" ||
                        format == "MM/yy" ||
                        format == "M/yy"
                    ) {

                        calendar.set(
                            Calendar.DAY_OF_MONTH,
                            calendar.getActualMaximum(
                                Calendar.DAY_OF_MONTH
                            )
                        )
                    }

                    expiryCalendar = calendar
                    break
                }
            }

            if (expiryCalendar == null) {
                return "Status unavailable"
            }

            val today = Calendar.getInstance()

            today.set(
                Calendar.HOUR_OF_DAY,
                0
            )

            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            expiryCalendar.set(
                Calendar.HOUR_OF_DAY,
                0
            )

            expiryCalendar.set(Calendar.MINUTE, 0)
            expiryCalendar.set(Calendar.SECOND, 0)
            expiryCalendar.set(Calendar.MILLISECOND, 0)

            val difference =
                expiryCalendar.timeInMillis -
                        today.timeInMillis

            val daysLeft =
                difference / (1000 * 60 * 60 * 24)

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

    // DP helper

    private fun dp(value: Int): Int {

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
