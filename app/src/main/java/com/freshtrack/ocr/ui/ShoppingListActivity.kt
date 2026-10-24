
package com.freshtrack.ocr.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import com.freshtrack.ocr.R
import com.freshtrack.ocr.data.Product
import com.freshtrack.ocr.data.ProductDatabase
import com.freshtrack.ocr.data.ShoppingItem
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ShoppingListActivity : AppCompatActivity() {

    private lateinit var itemsContainer: LinearLayout
    private lateinit var emptyMessage: TextView
    private lateinit var filterContainer: LinearLayout

    private var allItems: List<ShoppingItem> = emptyList()
    private var selectedFilter = Filter.ALL

    private enum class Filter {
        ALL, TO_BUY, PURCHASED
    }

    private val database by lazy {
        ProductDatabase.getDatabase(applicationContext)
    }

    private val shoppingDao by lazy {
        database.shoppingItemDao()
    }

    private val productDao by lazy {
        database.productDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_list)

        itemsContainer = findViewById(R.id.layoutShoppingItems)
        emptyMessage = findViewById(R.id.tvShoppingEmpty)

        filterContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val addButton = findViewById<MaterialButton>(
            R.id.btnAddShoppingItem
        )

        val root =
            itemsContainer.parent as? android.widget.ScrollView
        val parent = root?.parent as? LinearLayout

        if (parent != null && root != null) {
            val scrollIndex = parent.indexOfChild(root)
            parent.addView(filterContainer, scrollIndex)
        }

        findViewById<TextView>(R.id.btnShoppingBack)
            .setOnClickListener {
                finish()
            }

        addButton.setOnClickListener {
            showAddItemDialog()
        }

        createFilterButtons()
        loadItems()
    }

    override fun onResume() {
        super.onResume()

        if (::itemsContainer.isInitialized) {
            loadItems()
        }
    }

    private fun createFilterButtons() {
        filterContainer.removeAllViews()

        listOf(
            Filter.ALL to "All",
            Filter.TO_BUY to "To buy",
            Filter.PURCHASED to "Purchased"
        ).forEach { (filter, label) ->

            val button = MaterialButton(this).apply {
                text = label
                isAllCaps = false
                textSize = 13f
                insetTop = 0
                insetBottom = 0

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    dp(42),
                    1f
                ).apply {
                    marginEnd = dp(4)
                    marginStart = dp(4)
                }

                setOnClickListener {
                    selectedFilter = filter
                    createFilterButtons()
                    displayItems(allItems)
                }
            }

            if (selectedFilter == filter) {
                button.backgroundTintList = ColorStateList.valueOf(
                    getColor(R.color.green_primary)
                )
                button.setTextColor(Color.WHITE)
            } else {
                button.backgroundTintList = ColorStateList.valueOf(
                    Color.WHITE
                )
                button.setTextColor(
                    getColor(R.color.green_primary)
                )
            }

            filterContainer.addView(button)
        }
    }

    private fun showAddItemDialog() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(8))
        }

        val nameLabel = TextView(this).apply {
            text = "Item name"
            textSize = 14f
            setTextColor(getColor(R.color.text_primary))
        }

        val nameInput = EditText(this).apply {
            hint = "e.g. Milk, Bread, Rice"
            isSingleLine = true
            textSize = 16f
            setTextColor(getColor(R.color.text_primary))
            setHintTextColor(getColor(R.color.text_secondary))
        }

        val quantityLabel = TextView(this).apply {
            text = "Quantity"
            textSize = 14f
            setTextColor(getColor(R.color.text_primary))
            setPadding(0, dp(12), 0, 0)
        }

        val quantityInput = EditText(this).apply {
            hint = "Enter quantity"
            setText("1")
            inputType = InputType.TYPE_CLASS_NUMBER
            isSingleLine = true
            textSize = 16f
            setTextColor(getColor(R.color.text_primary))
            setHintTextColor(getColor(R.color.text_secondary))
        }

        form.addView(nameLabel)
        form.addView(nameInput)
        form.addView(quantityLabel)
        form.addView(quantityInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add shopping item")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save item", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(
                android.R.color.white
            )

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(getColor(R.color.green_primary))

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(getColor(R.color.green_primary))

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val name = nameInput.text.toString().trim()
                    val quantity = quantityInput.text.toString()
                        .toIntOrNull()

                    if (name.isBlank()) {
                        nameInput.error = "Enter an item name"
                        return@setOnClickListener
                    }

                    if (quantity == null || quantity < 1) {
                        quantityInput.error =
                            "Enter a quantity of at least 1"
                        return@setOnClickListener
                    }

                    lifecycleScope.launch {
                        try {
                            shoppingDao.insertItem(
                                ShoppingItem(
                                    itemName = name,
                                    quantity = quantity
                                )
                            )

                            dialog.dismiss()
                            loadItems()
                        } catch (exception: Exception) {
                            showToast("Couldn't save item")
                        }
                    }
                }
        }

        dialog.show()
    }

    private fun loadItems() {
        lifecycleScope.launch {
            try {
                allItems = shoppingDao.getAllItems()
                displayItems(allItems)
            } catch (exception: Exception) {
                showToast("Couldn't load shopping items")
            }
        }
    }

    private fun displayItems(items: List<ShoppingItem>) {
        itemsContainer.removeAllViews()

        val filteredItems = when (selectedFilter) {
            Filter.ALL -> items
            Filter.TO_BUY -> items.filter { !it.isPurchased }
            Filter.PURCHASED -> items.filter { it.isPurchased }
        }

        if (filteredItems.isEmpty()) {
            emptyMessage.text = when (selectedFilter) {
                Filter.ALL -> "Your shopping list is empty."
                Filter.TO_BUY -> "You've bought everything on your list."
                Filter.PURCHASED -> "No purchased items yet."
            }

            emptyMessage.visibility = View.VISIBLE
            itemsContainer.addView(emptyMessage)
            return
        }

        emptyMessage.visibility = View.GONE

        filteredItems.forEach { item ->
            itemsContainer.addView(createItemCard(item))
        }
    }

    private fun createItemCard(
        item: ShoppingItem
    ): MaterialCardView {

        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }

            radius = dp(18).toFloat()
            cardElevation = dp(1).toFloat()

            setCardBackgroundColor(
                if (item.isPurchased) {
                    Color.rgb(235, 242, 235)
                } else {
                    getColor(R.color.card_dark)
                }
            )

            strokeWidth = dp(1)
            strokeColor = getColor(R.color.border_light)
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(12), dp(8))
        }

        val checkbox = CheckBox(this).apply {
            isChecked = item.isPurchased

            buttonTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(
                    getColor(R.color.green_primary),
                    Color.DKGRAY
                )
            )

            layoutParams = LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        }

        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(6), dp(4))
        }

        val name = TextView(this).apply {
            text = item.itemName
            textSize = 15f
            setTextColor(getColor(R.color.text_primary))

            if (item.isPurchased) {
                paint.isStrikeThruText = true
            }
        }

        val quantity = TextView(this).apply {
            text = "Quantity: ${item.quantity}"
            textSize = 12f
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, dp(3), 0, 0)
        }

        val status = TextView(this).apply {
            text = if (item.isPurchased) "Purchased" else ""
            textSize = 11f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.green_primary))
            visibility =
                if (item.isPurchased) View.VISIBLE else View.GONE
            setPadding(0, dp(3), 0, 0)
        }

        details.addView(name)
        details.addView(quantity)
        details.addView(status)

        val deleteButton = MaterialButton(this).apply {
            text = "Delete"
            isAllCaps = false

            setOnClickListener {
                AlertDialog.Builder(this@ShoppingListActivity)
                    .setTitle("Delete item?")
                    .setMessage(
                        "Remove ${item.itemName} from your shopping list?"
                    )
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            try {
                                shoppingDao.deleteItem(item)
                                loadItems()
                            } catch (exception: Exception) {
                                showToast("Couldn't delete item")
                            }
                        }
                    }
                    .show()
            }
        }

        checkbox.setOnCheckedChangeListener { _, checked ->
            if (checked == item.isPurchased) {
                return@setOnCheckedChangeListener
            }

            checkbox.isEnabled = false

            lifecycleScope.launch {
                try {
                    val latest = shoppingDao.getItemById(item.id)

                    if (latest == null) {
                        showToast("Shopping item no longer exists")
                        loadItems()
                        return@launch
                    }

                    if (!checked) {
                        shoppingDao.updateItem(
                            latest.copy(isPurchased = false)
                        )
                        loadItems()
                        return@launch
                    }

                    if (latest.inventoryTransferred) {
                        shoppingDao.updateItem(
                            latest.copy(isPurchased = true)
                        )
                        loadItems()
                        return@launch
                    }

                    showExpiryDatePicker(latest, checkbox)
                } catch (exception: Exception) {
                    showToast("Couldn't update item")
                    resetCheckbox(checkbox, item.isPurchased)
                    loadItems()
                } finally {
                    checkbox.isEnabled = true
                }
            }
        }

        row.addView(checkbox)

        row.addView(
            details,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        row.addView(deleteButton)
        card.addView(row)

        return card
    }

    private fun showExpiryDatePicker(
        item: ShoppingItem,
        checkbox: CheckBox
    ) {
        val today = Calendar.getInstance()

        val picker = DatePickerDialog(
            this,
            android.R.style.Theme_DeviceDefault_Light_Dialog_Alert,
            { _, year, month, dayOfMonth ->

                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }.time

                val formattedDate = SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
                ).format(selectedDate)

                transferToInventory(
                    item = item,
                    expiryDate = formattedDate,
                    checkbox = checkbox
                )
            },
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        )

        picker.setTitle("Expiry date for ${item.itemName}")

        picker.setOnCancelListener {
            resetCheckbox(checkbox, item.isPurchased)
        }

        picker.setOnDismissListener {
            checkbox.isEnabled = true
        }

        picker.show()

        // Set button colours after the dialog has been displayed.
        picker.getButton(DatePickerDialog.BUTTON_POSITIVE)
            .setTextColor(getColor(R.color.green_primary))

        picker.getButton(DatePickerDialog.BUTTON_NEGATIVE)
            .setTextColor(getColor(R.color.green_primary))
    }

    private fun transferToInventory(
        item: ShoppingItem,
        expiryDate: String,
        checkbox: CheckBox
    ) {
        lifecycleScope.launch {
            try {
                database.withTransaction {
                    val latest = shoppingDao.getItemById(item.id)
                        ?: throw IllegalStateException(
                            "Shopping item no longer exists"
                        )

                    if (!latest.inventoryTransferred) {
                        productDao.insertProduct(
                            Product(
                                productName = latest.itemName,
                                expiryDate = expiryDate,
                                rawOcrText = "",
                                category = "Other",
                                location = ""
                            )
                        )

                        shoppingDao.updateItem(
                            latest.copy(
                                isPurchased = true,
                                inventoryTransferred = true
                            )
                        )
                    } else {
                        shoppingDao.updateItem(
                            latest.copy(isPurchased = true)
                        )
                    }
                }

                showToast("${item.itemName} added to inventory")
                loadItems()
            } catch (exception: Exception) {
                resetCheckbox(checkbox, item.isPurchased)
                showToast(
                    "Couldn't transfer item. Please try again."
                )
                loadItems()
            } finally {
                checkbox.isEnabled = true
            }
        }
    }

    private fun resetCheckbox(
        checkbox: CheckBox,
        previousValue: Boolean
    ) {
        checkbox.setOnCheckedChangeListener(null)
        checkbox.isChecked = previousValue

        checkbox.setOnCheckedChangeListener { _, checked ->
            if (checked != previousValue) {
                loadItems()
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
