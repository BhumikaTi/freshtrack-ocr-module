package com.freshtrack.ocr.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.freshtrack.ocr.ExpiryNotificationWorker
import com.freshtrack.ocr.ExpiredItem
import com.freshtrack.ocr.R
import com.freshtrack.ocr.Urgency
import com.freshtrack.ocr.UseFirstItem
import com.freshtrack.ocr.UseFirstSelector
import com.freshtrack.ocr.auth.CurrentUserProvider
import com.freshtrack.ocr.auth.SupabaseAuthRepository
import com.freshtrack.ocr.data.Product
import com.freshtrack.ocr.data.ProductDatabase
import com.freshtrack.ocr.data.ShoppingItem
import com.freshtrack.ocr.sync.SyncManager
import com.freshtrack.ocr.sync.SyncWorker
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val EXPIRY_WORK_NAME = "expiry_notification_work"

        /** How many items the Use First card shows on the home screen. */
        private const val USE_FIRST_SHOWN = 5
        private const val EXPIRED_SHOWN = 3

        private val DATE_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    }

    private lateinit var recentProductsContainer: LinearLayout
    private lateinit var recentProductsEmpty: TextView
    private lateinit var shoppingPreviewContainer: LinearLayout
    private lateinit var shoppingEmpty: TextView
    private lateinit var useFirstContainer: LinearLayout
    private lateinit var useFirstEmpty: TextView
    private lateinit var expiredTitle: TextView
    private lateinit var expiredContainer: LinearLayout

    private val database by lazy {
        ProductDatabase.getDatabase(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags("en")
        )

        setContentView(R.layout.activity_main)

        recentProductsContainer =
            findViewById(R.id.layoutRecentProducts)

        recentProductsEmpty =
            findViewById(R.id.tvRecentProductsEmpty)

        shoppingPreviewContainer =
            findViewById(R.id.layoutShoppingPreview)

        shoppingEmpty =
            findViewById(R.id.tvShoppingListEmpty)

        useFirstContainer =
            findViewById(R.id.layoutUseFirst)

        useFirstEmpty =
            findViewById(R.id.tvUseFirstEmpty)

        expiredTitle =
            findViewById(R.id.tvExpiredTitle)

        expiredContainer =
            findViewById(R.id.layoutExpired)

        requestNotificationPermission()
        scheduleExpiryNotifications()
        SyncWorker.schedulePeriodicSync(this)

        findViewById<MaterialButton>(R.id.btnScanCamera)
            .setOnClickListener {
                startActivity(
                    Intent(this, AddOptionsActivity::class.java)
                )
            }

        findViewById<MaterialButton>(R.id.btnMyProducts)
            .setOnClickListener {
                startActivity(
                    Intent(this, ProductListActivity::class.java)
                )
            }

        findViewById<MaterialButton>(R.id.btnViewShoppingList)
            .setOnClickListener {
                openShoppingList()
            }

        findViewById<MaterialButton>(R.id.btnAskAi)
            .setOnClickListener {
                startActivity(
                    Intent(this, ChatActivity::class.java)
                )
            }

        findViewById<MaterialButton>(R.id.btnLogout)
            .setOnClickListener {
                logoutUser()
            }

        findViewById<MaterialButton>(R.id.btnAddShoppingItem)
            .setOnClickListener {
                showAddShoppingItemDialog()
            }
    }

    override fun onResume() {
        super.onResume()

        if (::recentProductsContainer.isInitialized) {
            loadRecentProducts()
            loadUseFirst()
            loadShoppingPreview()
            syncWithCloud()
        }
    }

    // ---------- CLOUD SYNC ----------

    private fun syncWithCloud() {
        lifecycleScope.launch {
            val userId = CurrentUserProvider.getUserId()
            if (userId.isNullOrBlank()) return@launch

            val result = SyncManager.syncAll(applicationContext, userId)
            if (result.isSuccess) {
                loadRecentProducts()
                loadUseFirst()
                loadShoppingPreview()
            } else {
                // Retry automatically in the background as soon as internet is back
                SyncWorker.enqueueOneTimeSync(applicationContext)
            }
        }
    }

    // ---------- LOGOUT ----------

    private fun logoutUser() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    SupabaseAuthRepository().logout()
                }

                val intent = Intent(
                    this@MainActivity,
                    LoginActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                startActivity(intent)
                finish()

            } catch (exception: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Couldn't log out. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun openShoppingList() {
        startActivity(
            Intent(this, ShoppingListActivity::class.java)
        )
    }

    // ---------- PRODUCT PREVIEW ----------

    private fun loadRecentProducts() {
        lifecycleScope.launch {
            try {
                val userId = CurrentUserProvider.getUserId()

                if (userId.isNullOrBlank()) {
                    showProductMessage(
                        "Please log in to view your products."
                    )
                    return@launch
                }

                val products =
                    database.productDao().getRecentProducts(userId)

                displayRecentProducts(products)

            } catch (exception: Exception) {
                showProductMessage("Couldn't load products.")
            }
        }
    }

    private fun displayRecentProducts(products: List<Product>) {
        recentProductsContainer.removeAllViews()

        if (products.isEmpty()) {
            showProductMessage(
                "No products yet. Scan a product to get started."
            )
            return
        }

        recentProductsEmpty.visibility = View.GONE

        products.forEach { product ->
            recentProductsContainer.addView(
                createProductCard(product)
            )
        }
    }

    private fun showProductMessage(message: String) {
        recentProductsContainer.removeAllViews()
        recentProductsEmpty.text = message
        recentProductsEmpty.visibility = View.VISIBLE
        recentProductsContainer.addView(recentProductsEmpty)
    }

    private fun createProductCard(
        product: Product
    ): MaterialCardView {

        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(86)
            ).apply {
                bottomMargin = dp(10)
            }

            radius = dp(18).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(getColor(R.color.card_dark))
            strokeWidth = dp(1)
            strokeColor = getColor(R.color.border_light)
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }

        val name = TextView(this).apply {
            text = product.productName
            textSize = 16f
            setTextColor(getColor(R.color.text_primary))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val expiry = TextView(this).apply {
            text = "Expires: ${product.expiryDate}"
            textSize = 13f
            setPadding(0, dp(4), 0, 0)
            setTextColor(getColor(R.color.text_secondary))
        }

        row.addView(name)
        row.addView(expiry)
        card.addView(row)

        return card
    }

    // ---------- USE FIRST ----------

    private fun loadUseFirst() {
        lifecycleScope.launch {
            try {
                val userId = CurrentUserProvider.getUserId()

                if (userId.isNullOrBlank()) {
                    showUseFirstMessage("Please log in to see what to use first.")
                    showExpired(emptyList())
                    return@launch
                }

                val products = database.productDao().getAllProducts(userId)
                val result = UseFirstSelector.select(products)

                displayUseFirst(result.top(USE_FIRST_SHOWN), result.useFirst.size)
                showExpired(result.expired)

            } catch (exception: Exception) {
                showUseFirstMessage("Couldn't load this list.")
                showExpired(emptyList())
            }
        }
    }

    private fun displayUseFirst(items: List<UseFirstItem>, total: Int) {
        useFirstContainer.removeAllViews()

        if (items.isEmpty()) {
            showUseFirstMessage("Nothing to use up. Scan some food to see it here.")
            return
        }

        items.forEach { item ->
            useFirstContainer.addView(createUseFirstCard(item))
        }

        if (total > items.size) {
            useFirstContainer.addView(
                TextView(this).apply {
                    text = "+ ${total - items.size} more in Your Products"
                    textSize = 13f
                    setTextColor(getColor(R.color.text_secondary))
                    setPadding(dp(4), 0, 0, dp(4))
                }
            )
        }
    }

    private fun showUseFirstMessage(message: String) {
        useFirstContainer.removeAllViews()
        useFirstEmpty.text = message
        useFirstContainer.addView(useFirstEmpty)
    }

    private fun showExpired(items: List<ExpiredItem>) {
        expiredContainer.removeAllViews()

        if (items.isEmpty()) {
            expiredTitle.visibility = View.GONE
            expiredContainer.visibility = View.GONE
            return
        }

        expiredTitle.visibility = View.VISIBLE
        expiredContainer.visibility = View.VISIBLE

        items.take(EXPIRED_SHOWN).forEach { item ->
            expiredContainer.addView(createExpiredCard(item))
        }

        if (items.size > EXPIRED_SHOWN) {
            expiredContainer.addView(
                TextView(this).apply {
                    text = "+ ${items.size - EXPIRED_SHOWN} more in Your Products"
                    textSize = 13f
                    setTextColor(getColor(R.color.text_secondary))
                    setPadding(dp(4), 0, 0, dp(4))
                }
            )
        }
    }

    private fun createUseFirstCard(item: UseFirstItem): MaterialCardView {

        val (label, colorRes) = when (item.urgency) {
            Urgency.TODAY -> "Expires today" to R.color.error_bright
            Urgency.WITHIN_3_DAYS -> daysText(item.daysLeft) to R.color.error_bright
            Urgency.THIS_WEEK -> daysText(item.daysLeft) to R.color.warning_amber
            Urgency.LATER -> daysText(item.daysLeft) to R.color.text_secondary
        }

        val date = item.expiry.format(DATE_FORMAT)

        return createItemCard(
            title = item.product.productName,
            subtitle = "$label  ·  $date",
            subtitleColor = getColor(colorRes),
            buttonText = "Used",
            onButton = {
                removeProduct(item.product, "\"${item.product.productName}\" marked as used")
            }
        )
    }

    private fun createExpiredCard(item: ExpiredItem): MaterialCardView {

        val label =
            if (item.daysAgo == 1) "Expired yesterday"
            else "Expired ${item.daysAgo} days ago"

        return createItemCard(
            title = item.product.productName,
            subtitle = "$label  ·  ${item.expiry.format(DATE_FORMAT)}",
            subtitleColor = getColor(R.color.error_bright),
            buttonText = "Remove",
            onButton = {
                removeProduct(item.product, "\"${item.product.productName}\" removed")
            }
        )
    }

    private fun createItemCard(
        title: String,
        subtitle: String,
        subtitleColor: Int,
        buttonText: String,
        onButton: () -> Unit
    ): MaterialCardView {

        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }

            radius = dp(18).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(getColor(R.color.card_dark))
            strokeWidth = dp(1)
            strokeColor = getColor(R.color.border_light)
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(10), dp(10))
        }

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        texts.addView(
            TextView(this).apply {
                text = title
                textSize = 16f
                setTextColor(getColor(R.color.text_primary))
                setTypeface(null, android.graphics.Typeface.BOLD)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
        )

        texts.addView(
            TextView(this).apply {
                text = subtitle
                textSize = 13f
                setPadding(0, dp(4), 0, 0)
                setTextColor(subtitleColor)
            }
        )

        val button = MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = buttonText
            setAllCaps(false)
            textSize = 13f
            minWidth = 0
            minimumWidth = 0
            setTextColor(getColor(R.color.green_primary))
            setOnClickListener { onButton() }
        }

        row.addView(texts)
        row.addView(button)
        card.addView(row)

        return card
    }

    private fun daysText(daysLeft: Int): String =
        when {
            daysLeft <= 0 -> "Expires today"
            daysLeft == 1 -> "Expires tomorrow"
            daysLeft > 60 -> "Expires later"
            else -> "Expires in $daysLeft days"
        }

    /** Soft-deletes the product so deletion syncs to Supabase, with an Undo option. */
    private fun removeProduct(product: Product, message: String) {
        lifecycleScope.launch {
            try {
                val userId = CurrentUserProvider.getUserId()

                if (userId.isNullOrBlank()) return@launch

                database.productDao().softDeleteProductById(
                    productId = product.id,
                    userId = userId,
                    timestamp = System.currentTimeMillis()
                )

                loadRecentProducts()
                loadUseFirst()
                syncWithCloud()

                Snackbar.make(
                    findViewById(android.R.id.content),
                    message,
                    Snackbar.LENGTH_LONG
                ).setAction("UNDO") {
                    lifecycleScope.launch {
                        database.productDao().updateProduct(
                            product.copy(
                                deletedAt = null,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        loadRecentProducts()
                        loadUseFirst()
                        syncWithCloud()
                    }
                }.show()

            } catch (exception: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Couldn't update the product.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ---------- SHOPPING LIST PREVIEW ----------

    private fun loadShoppingPreview() {
        lifecycleScope.launch {
            try {
                val userId = CurrentUserProvider.getUserId()

                if (userId.isNullOrBlank()) {
                    showShoppingMessage(
                        "Please log in to view your shopping list."
                    )
                    return@launch
                }

                val items =
                    database.shoppingItemDao().getRecentItems(userId)

                displayShoppingPreview(items)

            } catch (exception: Exception) {
                showShoppingMessage(
                    "Couldn't load shopping items."
                )
            }
        }
    }

    private fun displayShoppingPreview(
        items: List<ShoppingItem>
    ) {
        shoppingPreviewContainer.removeAllViews()

        if (items.isEmpty()) {
            showShoppingMessage("Your shopping list is empty.")
            return
        }

        shoppingEmpty.visibility = View.GONE

        items.forEach { item ->
            shoppingPreviewContainer.addView(
                createShoppingPreviewCard(item)
            )
        }
    }

    private fun showShoppingMessage(message: String) {
        shoppingPreviewContainer.removeAllViews()
        shoppingEmpty.text = message
        shoppingEmpty.visibility = View.VISIBLE
        shoppingPreviewContainer.addView(shoppingEmpty)
    }

    private fun createShoppingPreviewCard(
        item: ShoppingItem
    ): MaterialCardView {

        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }

            radius = dp(16).toFloat()
            cardElevation = 0f

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
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
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
            setPadding(0, dp(3), 0, 0)
            setTextColor(getColor(R.color.text_secondary))
        }

        details.addView(name)
        details.addView(quantity)

        row.addView(
            details,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        if (item.isPurchased) {
            val status = TextView(this).apply {
                text = "Purchased"
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(getColor(R.color.green_primary))
            }

            row.addView(status)
        }

        card.addView(row)

        return card
    }

    // ---------- ADD SHOPPING ITEM ----------

    private fun showAddShoppingItemDialog() {

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

                    val quantity =
                        quantityInput.text.toString().toIntOrNull()

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
                            val userId =
                                CurrentUserProvider.getUserId()

                            if (userId.isNullOrBlank()) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Please log in again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }

                            database.shoppingItemDao().insertItem(
                                ShoppingItem(
                                    itemName = name,
                                    quantity = quantity,
                                    userId = userId
                                )
                            )

                            dialog.dismiss()
                            loadShoppingPreview()
                            syncWithCloud()

                            Toast.makeText(
                                this@MainActivity,
                                "Shopping item added",
                                Toast.LENGTH_SHORT
                            ).show()

                        } catch (exception: Exception) {
                            Toast.makeText(
                                this@MainActivity,
                                "Couldn't save item",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
        }

        dialog.show()
    }

    // ---------- NOTIFICATIONS ----------

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun scheduleExpiryNotifications() {
        val workRequest =
            PeriodicWorkRequestBuilder<ExpiryNotificationWorker>(
                1,
                TimeUnit.DAYS
            ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            EXPIRY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}