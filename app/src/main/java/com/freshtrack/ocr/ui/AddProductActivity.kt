package com.freshtrack.ocr.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.freshtrack.ocr.R
import com.freshtrack.ocr.data.Product
import com.freshtrack.ocr.data.ProductDao
import com.freshtrack.ocr.data.ProductDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView

class AddProductActivity : AppCompatActivity() {

    private lateinit var etProductName: TextInputEditText
    private lateinit var etExpiryDate: TextInputEditText
    private lateinit var productDao: ProductDao

    // Room product ID.
    // Null = adding a new product.
    // Non-null = editing an existing product.
    private var productId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_product)

        etProductName = findViewById(R.id.etProductName)
        etExpiryDate = findViewById(R.id.etExpiryDate)

        // Initialize Room database
        val database = ProductDatabase.getDatabase(this)
        productDao = database.productDao()

        // Check whether this screen was opened for editing
        productId = intent.getIntExtra("productId", -1).takeIf {
            it != -1
        }

        // Get existing product information when editing
        val existingProductName =
            intent.getStringExtra("productName")

        val existingExpiryDate =
            intent.getStringExtra("expiryDate")

        if (!existingProductName.isNullOrBlank()) {
            etProductName.setText(existingProductName)
        }

        if (!existingExpiryDate.isNullOrBlank()) {
            etExpiryDate.setText(existingExpiryDate)
        }

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        etExpiryDate.setOnClickListener {
            showDatePicker()
        }

        findViewById<MaterialButton>(R.id.btnSaveProduct).setOnClickListener {
            saveProduct()
        }
    }

    private fun showDatePicker() {

        val calendar = Calendar.getInstance()

        val dialog = Dialog(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(4))
            setBackgroundColor(
                Color.argb(235, 255, 255, 255)
            )
        }

        // -------------------------
        // Header
        // -------------------------

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(12), dp(6))

            background = GradientDrawable().apply {
                setColor(getColor(R.color.green_primary))
                cornerRadius = dp(14).toFloat()
            }
        }

        val yearText = TextView(this).apply {
            textSize = 11f
            setTextColor(Color.WHITE)
        }

        val selectedDateText = TextView(this).apply {
            textSize = 19f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        header.addView(yearText)

        header.addView(selectedDateText)

        root.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        // -------------------------
        // Month navigation
        // -------------------------

        val monthRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val previousMonth = TextView(this).apply {
            text = "‹"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_primary))
        }

        val monthText = TextView(this).apply {
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_primary))
        }

        val nextMonth = TextView(this).apply {
            text = "›"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_primary))
        }

        monthRow.addView(
            previousMonth,
            LinearLayout.LayoutParams(
                dp(32),
                dp(34)
            )
        )

        monthRow.addView(
            monthText,
            LinearLayout.LayoutParams(
                0,
                dp(34),
                1f
            )
        )

        monthRow.addView(
            nextMonth,
            LinearLayout.LayoutParams(
                dp(32),
                dp(34)
            )
        )

        root.addView(
            monthRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(38)
            )
        )

        // -------------------------
        // Calendar grid
        // -------------------------

        val calendarGrid = GridLayout(this).apply {
            columnCount = 7
            rowCount = 7
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false
        }

        root.addView(
            calendarGrid,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(190)
            )
        )

        // -------------------------
        // Buttons
        // -------------------------

        dialog.setContentView(root)

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        // -------------------------
        // Calendar data
        // -------------------------

        var selectedCalendar =
            calendar.clone() as Calendar

        var displayedCalendar =
            calendar.clone() as Calendar

        fun updateCalendar() {

            calendarGrid.removeAllViews()

            monthText.text =
                SimpleDateFormat(
                    "MMMM yyyy",
                    Locale.getDefault()
                ).format(displayedCalendar.time)

            // Weekdays

            val weekdays = arrayOf(
                "S", "M", "T", "W", "T", "F", "S"
            )

            weekdays.forEach { day ->

                val textView = TextView(this).apply {
                    text = day
                    textSize = 9f
                    gravity = Gravity.CENTER
                    setTextColor(
                        getColor(R.color.text_secondary)
                    )
                }

                calendarGrid.addView(
                    textView,
                    GridLayout.LayoutParams().apply {
                        width = 0
                        height = dp(24)

                        columnSpec = GridLayout.spec(
                            GridLayout.UNDEFINED,
                            1f
                        )
                    }
                )
            }

            // First day of month

            val firstDay =
                displayedCalendar.clone() as Calendar

            firstDay.set(
                Calendar.DAY_OF_MONTH,
                1
            )

            val startDay =
                firstDay.get(Calendar.DAY_OF_WEEK) - 1

            val daysInMonth =
                displayedCalendar.getActualMaximum(
                    Calendar.DAY_OF_MONTH
                )

            // Empty spaces

            for (i in 0 until startDay) {

                val empty = TextView(this)

                calendarGrid.addView(
                    empty,
                    GridLayout.LayoutParams().apply {
                        width = 0
                        height = dp(27)

                        columnSpec = GridLayout.spec(
                            GridLayout.UNDEFINED,
                            1f
                        )
                    }
                )
            }

            // Dates

            for (day in 1..daysInMonth) {

                val dayView = TextView(this).apply {

                    text = day.toString()

                    textSize = 10f

                    gravity = Gravity.CENTER

                    setTextColor(
                        getColor(R.color.text_primary)
                    )

                    val isSelected =
                        selectedCalendar.get(Calendar.YEAR) ==
                                displayedCalendar.get(Calendar.YEAR) &&
                                selectedCalendar.get(Calendar.MONTH) ==
                                displayedCalendar.get(Calendar.MONTH) &&
                                selectedCalendar.get(Calendar.DAY_OF_MONTH) ==
                                day

                    if (isSelected) {

                        setTextColor(Color.WHITE)

                        background =
                            GradientDrawable().apply {
                                shape =
                                    GradientDrawable.OVAL

                                setColor(
                                    getColor(
                                        R.color.green_primary
                                    )
                                )
                            }
                    }

                    setOnClickListener {

                        selectedCalendar =
                            displayedCalendar.clone() as Calendar

                        selectedCalendar.set(
                            Calendar.DAY_OF_MONTH,
                            day
                        )

                        val dateFormat = SimpleDateFormat(
                            "dd MMM yyyy",
                            Locale.getDefault()
                        )

                        etExpiryDate.setText(
                            dateFormat.format(
                                selectedCalendar.time
                            )
                        )

                        dialog.dismiss()
                    }
                }

                calendarGrid.addView(
                    dayView,
                    GridLayout.LayoutParams().apply {
                        width = 0
                        height = dp(27)

                        columnSpec = GridLayout.spec(
                            GridLayout.UNDEFINED,
                            1f
                        )
                    }
                )
            }

            // Header values

            yearText.text =
                selectedCalendar.get(
                    Calendar.YEAR
                ).toString()

            selectedDateText.text =
                SimpleDateFormat(
                    "EEE, dd MMM",
                    Locale.getDefault()
                ).format(selectedCalendar.time)
        }

        // -------------------------
        // Month buttons
        // -------------------------

        previousMonth.setOnClickListener {

            displayedCalendar.add(
                Calendar.MONTH,
                -1
            )

            updateCalendar()
        }

        nextMonth.setOnClickListener {

            displayedCalendar.add(
                Calendar.MONTH,
                1
            )

            updateCalendar()
        }

        // -------------------------
        // Initial calendar
        // -------------------------

        updateCalendar()

        // Show compact dialog

        dialog.show()

        dialog.window?.setLayout(
            dp(250),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun dp(value: Int): Int {
        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }

    private fun saveProduct() {

        val productName =
            etProductName.text.toString().trim()

        val expiryDate =
            etExpiryDate.text.toString().trim()

        if (productName.isEmpty()) {
            etProductName.error = "Enter product name"
            return
        }

        if (expiryDate.isEmpty()) {
            etExpiryDate.error = "Select expiry date"
            return
        }

        lifecycleScope.launch {

            val existingId = productId

            if (existingId == null) {

                // -------------------------
                // ADD NEW PRODUCT
                // -------------------------

                val product = Product(
                    productName = productName,
                    expiryDate = expiryDate,
                    rawOcrText = ""
                )

                productDao.insertProduct(product)

                Toast.makeText(
                    this@AddProductActivity,
                    "Product saved successfully!",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                // -------------------------
                // UPDATE EXISTING PRODUCT
                // -------------------------

                val updatedProduct = Product(
                    id = existingId,
                    productName = productName,
                    expiryDate = expiryDate,
                    rawOcrText = ""
                )

                productDao.updateProduct(updatedProduct)

                Toast.makeText(
                    this@AddProductActivity,
                    "Product updated successfully!",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Return to Product List

            val intent = Intent(
                this@AddProductActivity,
                ProductListActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)

            finish()
        }
    }
}

