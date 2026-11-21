package com.freshtrack.ocr.ui

import com.freshtrack.ocr.auth.CurrentUserProvider
import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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

class AddProductActivity : AppCompatActivity() {

    private lateinit var etProductName: TextInputEditText
    private lateinit var etExpiryDate: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var actvCategory: AutoCompleteTextView
    private lateinit var productDao: ProductDao

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private val categories = listOf(
        "Fruits & Vegetables",
        "Dairy",
        "Meat & Seafood",
        "Bakery",
        "Beverages",
        "Snacks",
        "Pantry",
        "Personal Care",
        "Cleaning Supplies",
        "Pet Supplies",
        "Cosmetics",
        "Medicines",
        "Other"
    )

    // Room product ID.
    // Null = adding a new product.
    // Non-null = editing an existing product.
    private var productId: Int? = null

    // marking to change in case of error(neko)
    private var barcode: String? = null
    private var imageUrl: String? = null

    // Reads the expiry date from a photo. The existing OCR screen sends the date
    // back here, and it fills the expiry field.
    private val scanDateLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val date = result.data
                    ?.getStringExtra(CropConfirmActivity.EXTRA_EXPIRY_DATE_RESULT)

                if (!date.isNullOrBlank()) {
                    etExpiryDate.setText(date)
                }
            }
        }

    private val pickDateImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                scanDateLauncher.launch(
                    Intent(this, CropConfirmActivity::class.java)
                        .putExtra(CropConfirmActivity.EXTRA_URI, uri.toString())
                        .putExtra(CropConfirmActivity.EXTRA_RETURN_DATE_ONLY, true)
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_product)

        etProductName = findViewById(R.id.etProductName)
        etExpiryDate = findViewById(R.id.etExpiryDate)
        etLocation = findViewById(R.id.etLocation)
        actvCategory = findViewById(R.id.actvCategory)

        // Expiry date is picked from the dialog, so don't open the keyboard here.
        etExpiryDate.showSoftInputOnFocus = false

        // Set up category dropdown
        val categoryAdapter = ArrayAdapter(
            this,
            R.layout.item_category_dropdown,
            categories
        )

        actvCategory.setAdapter(categoryAdapter)
        actvCategory.setText("Other", false)

        // Initialize Room database
        val database = ProductDatabase.getDatabase(this)
        productDao = database.productDao()

        // Marking in case error occur (neko/bhumika)
        productId = intent.getIntExtra("productId", -1).takeIf {
            it != -1
        }

        barcode = intent.getStringExtra("barcode")
        imageUrl = intent.getStringExtra("imageUrl")

        // The scan icons only make sense for products that came from a barcode scan.
        val fromBarcode = !barcode.isNullOrBlank()

        findViewById<LinearLayout>(R.id.expiryScanIcons).visibility =
            if (fromBarcode) View.VISIBLE else View.GONE

        if (fromBarcode) {
            // Keep typed dates from running underneath the two icons.
            etExpiryDate.setPaddingRelative(
                etExpiryDate.paddingStart,
                etExpiryDate.paddingTop,
                dp(104),
                etExpiryDate.paddingBottom
            )
        }

        // Get existing product information when editing
        val existingProductName = intent.getStringExtra("productName")
        val existingExpiryDate = intent.getStringExtra("expiryDate")
        val existingLocation = intent.getStringExtra("location")
        val existingCategory = intent.getStringExtra("category")

        if (!existingCategory.isNullOrBlank() &&
            categories.contains(existingCategory)
        ) {
            actvCategory.setText(existingCategory, false)
        }

        if (!existingProductName.isNullOrBlank()) {
            etProductName.setText(existingProductName)
        }

        if (!existingExpiryDate.isNullOrBlank()) {
            etExpiryDate.setText(existingExpiryDate)
        }

        if (!existingLocation.isNullOrBlank()) {
            etLocation.setText(existingLocation)
        }

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        etExpiryDate.setOnClickListener {
            showDatePicker()
        }

        findViewById<android.widget.ImageButton>(R.id.btnExpiryCamera).setOnClickListener {
            scanDateLauncher.launch(
                Intent(this, CameraScanActivity::class.java)
                    .putExtra(CropConfirmActivity.EXTRA_RETURN_DATE_ONLY, true)
            )
        }

        findViewById<android.widget.ImageButton>(R.id.btnExpiryGallery).setOnClickListener {
            pickDateImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        findViewById<MaterialButton>(R.id.btnSaveProduct).setOnClickListener {
            saveProduct()
        }
    }

    /**
     * Typing box is shown first. The calendar icon opens a calendar below it.
     * Picking a day fills the box, and OK saves it to the field.
     * Colors follow the app theme (card, green accent, light border).
     */
    private fun showDatePicker() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
            isLenient = false
        }
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val green = getColor(R.color.green_primary)

        // Returns a Calendar only if the text is a full, valid dd/MM/yyyy date.
        fun parseDate(text: String): Calendar? {
            if (text.length != 10) return null
            return runCatching {
                Calendar.getInstance().apply { time = dateFormat.parse(text)!! }
            }.getOrNull()
        }

        var displayed: Calendar =
            parseDate(etExpiryDate.text.toString().trim()) ?: Calendar.getInstance()

        val dialog = Dialog(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(12))
            background = GradientDrawable().apply {
                setColor(getColor(R.color.card_dark))
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), getColor(R.color.border_light))
            }
        }

        // -------------------------
        // Title
        // -------------------------

        val title = TextView(this).apply {
            text = "Enter date"
            textSize = 22f
            setTextColor(getColor(R.color.text_primary))
        }
        root.addView(title, LinearLayout.LayoutParams(MATCH, WRAP))

        val divider = View(this).apply {
            setBackgroundColor(getColor(R.color.border_light))
        }
        root.addView(
            divider,
            LinearLayout.LayoutParams(MATCH, dp(1)).apply { topMargin = dp(12) }
        )

        // -------------------------
        // Typing box + calendar icon
        // -------------------------

        val typeInput = EditText(this).apply {
            hint = "dd/mm/yyyy"
            textSize = 16f
            setTextColor(getColor(R.color.text_primary))
            setHintTextColor(getColor(R.color.text_secondary))
            // Underline in the app's green instead of the default theme color.
            backgroundTintList = ColorStateList.valueOf(green)
            // Normal keyboard, so "/" is available. The watcher adds "/" automatically.
            inputType = InputType.TYPE_CLASS_TEXT
            filters = arrayOf(InputFilter.LengthFilter(10))
            setSingleLine(true)
        }

        // Show the current date in the box if the field already has a valid one.
        val existingText = etExpiryDate.text.toString().trim()
        if (parseDate(existingText) != null) {
            typeInput.setText(existingText)
        }

        // Add the mask after setting the text, so the existing value is not changed.
        typeInput.addTextChangedListener(DateMaskWatcher())

        val calendarIcon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_my_calendar)
            setColorFilter(green)
            contentDescription = "Pick date from calendar"
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val fieldRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        fieldRow.addView(typeInput, LinearLayout.LayoutParams(0, WRAP, 1f))
        fieldRow.addView(calendarIcon, LinearLayout.LayoutParams(dp(44), dp(44)))

        root.addView(
            fieldRow,
            LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(16) }
        )

        // -------------------------
        // Calendar (hidden until the icon is tapped)
        // -------------------------

        val calendarSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(8), 0, 0)
        }

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
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_primary))
        }

        val nextMonth = TextView(this).apply {
            text = "›"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_primary))
        }

        monthRow.addView(previousMonth, LinearLayout.LayoutParams(dp(32), dp(34)))
        monthRow.addView(monthText, LinearLayout.LayoutParams(0, dp(34), 1f))
        monthRow.addView(nextMonth, LinearLayout.LayoutParams(dp(32), dp(34)))
        calendarSection.addView(monthRow, LinearLayout.LayoutParams(MATCH, dp(38)))

        val calendarGrid = GridLayout(this).apply {
            columnCount = 7
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false
        }
        calendarSection.addView(calendarGrid, LinearLayout.LayoutParams(MATCH, WRAP))

        root.addView(calendarSection, LinearLayout.LayoutParams(MATCH, WRAP))

        fun cellParams(heightDp: Int) = GridLayout.LayoutParams().apply {
            width = 0
            height = dp(heightDp)
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }

        fun updateCalendar() {
            calendarGrid.removeAllViews()

            // Highlight whatever is in the box right now, if it is a valid date.
            val sel = parseDate(typeInput.text.toString().trim())

            monthText.text = monthFormat.format(displayed.time)

            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                val tv = TextView(this).apply {
                    text = day
                    textSize = 9f
                    gravity = Gravity.CENTER
                    setTextColor(getColor(R.color.text_secondary))
                }
                calendarGrid.addView(tv, cellParams(24))
            }

            val firstDay = displayed.clone() as Calendar
            firstDay.set(Calendar.DAY_OF_MONTH, 1)
            val startDay = firstDay.get(Calendar.DAY_OF_WEEK) - 1
            val daysInMonth = displayed.getActualMaximum(Calendar.DAY_OF_MONTH)

            repeat(startDay) {
                calendarGrid.addView(TextView(this), cellParams(27))
            }

            for (day in 1..daysInMonth) {
                val dayView = TextView(this).apply {
                    text = day.toString()
                    textSize = 10f
                    gravity = Gravity.CENTER
                    setTextColor(getColor(R.color.text_primary))

                    val isSelected = sel != null &&
                            sel.get(Calendar.YEAR) == displayed.get(Calendar.YEAR) &&
                            sel.get(Calendar.MONTH) == displayed.get(Calendar.MONTH) &&
                            sel.get(Calendar.DAY_OF_MONTH) == day

                    if (isSelected) {
                        setTextColor(Color.WHITE)
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(green)
                        }
                    }

                    setOnClickListener {
                        val chosen = displayed.clone() as Calendar
                        chosen.set(Calendar.DAY_OF_MONTH, day)

                        typeInput.setText(dateFormat.format(chosen.time))
                        typeInput.setSelection(typeInput.text?.length ?: 0)
                        calendarSection.visibility = View.GONE
                        imm.hideSoftInputFromWindow(typeInput.windowToken, 0)
                    }
                }
                calendarGrid.addView(dayView, cellParams(27))
            }
        }

        // -------------------------
        // Buttons
        // -------------------------

        val cancelButton = TextView(this).apply {
            text = "Cancel"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(green)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        val okButton = TextView(this).apply {
            text = "OK"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(green)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(16), 0, 0)
        }
        buttonRow.addView(cancelButton)
        buttonRow.addView(okButton)
        root.addView(buttonRow, LinearLayout.LayoutParams(MATCH, WRAP))

        // -------------------------
        // Actions
        // -------------------------

        calendarIcon.setOnClickListener {
            if (calendarSection.visibility == View.VISIBLE) {
                calendarSection.visibility = View.GONE
            } else {
                // Open the calendar on the month of the typed date, if there is one.
                displayed = parseDate(typeInput.text.toString().trim()) ?: Calendar.getInstance()
                updateCalendar()
                calendarSection.visibility = View.VISIBLE
                imm.hideSoftInputFromWindow(typeInput.windowToken, 0)
            }
        }

        previousMonth.setOnClickListener {
            displayed.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        nextMonth.setOnClickListener {
            displayed.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        cancelButton.setOnClickListener {
            imm.hideSoftInputFromWindow(typeInput.windowToken, 0)
            dialog.dismiss()
        }

        okButton.setOnClickListener {
            val typed = typeInput.text.toString().trim()

            if (parseDate(typed) == null) {
                Toast.makeText(this, "Enter a valid date as dd/mm/yyyy", Toast.LENGTH_SHORT).show()
            } else {
                imm.hideSoftInputFromWindow(typeInput.windowToken, 0)
                etExpiryDate.setText(typed)
                dialog.dismiss()
            }
        }

        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        dialog.show()
        dialog.window?.setLayout(dp(300), WindowManager.LayoutParams.WRAP_CONTENT)

        // Open the keyboard straight away, since typing is the default.
        typeInput.post {
            typeInput.requestFocus()
            typeInput.setSelection(typeInput.text?.length ?: 0)
            imm.showSoftInput(typeInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun saveProduct() {
        val productName = etProductName.text.toString().trim()
        val expiryDate = etExpiryDate.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val category = actvCategory.text.toString().trim()

        if (category !in categories) {
            actvCategory.error = "Please select a valid category"
            return
        }

        if (productName.isEmpty()) {
            etProductName.error = "Enter product name"
            return
        }

        if (expiryDate.isEmpty()) {
            etExpiryDate.error = "Select expiry date"
            return
        }

        lifecycleScope.launch {
            // Both new and edited products must belong to the signed-in user.
            val userId = CurrentUserProvider.getUserId()
            if (userId.isNullOrBlank()) {
                Toast.makeText(
                    this@AddProductActivity,
                    "Please log in again to save products.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val existingId = productId

            if (existingId == null) {
                // Add a new product for the current user.
                val product = Product(
                    productName = productName,
                    expiryDate = expiryDate,
                    rawOcrText = "",
                    location = location,
                    category = category,
                    barcode = barcode,
                    imageUrl = imageUrl,
                    userId = userId
                )

                productDao.insertProduct(product)

                Toast.makeText(
                    this@AddProductActivity,
                    "Product saved successfully!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Only allow editing a product owned by the current user.
                val existing = productDao.getProductById(existingId, userId)

                if (existing == null) {
                    Toast.makeText(
                        this@AddProductActivity,
                        "Product not found in your account.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val updatedProduct = existing.copy(
                    productName = productName,
                    expiryDate = expiryDate,
                    location = location,
                    category = category
                )

                productDao.updateProduct(updatedProduct)

                Toast.makeText(
                    this@AddProductActivity,
                    "Product updated successfully!",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Return to Product List.
            val intent = Intent(
                this@AddProductActivity,
                ProductListActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
            setResult(RESULT_OK)
            finish()
        }
    }
}

/**
 * Adds "/" automatically as the user types, so typing 15082029 shows 15/08/2029.
 */
private class DateMaskWatcher : TextWatcher {
    private var editing = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (editing || s == null) return

        val digits = s.toString().filter { it.isDigit() }.take(8)
        val formatted = StringBuilder()
        digits.forEachIndexed { i, c ->
            if (i == 2 || i == 4) formatted.append('/')
            formatted.append(c)
        }

        if (formatted.toString() != s.toString()) {
            editing = true
            s.replace(0, s.length, formatted.toString())
            editing = false
        }
    }
}