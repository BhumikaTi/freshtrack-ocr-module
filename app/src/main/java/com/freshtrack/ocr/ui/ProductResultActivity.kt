package com.freshtrack.ocr.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.freshtrack.ocr.R
import com.freshtrack.ocr.data.ProductInfo
import com.freshtrack.ocr.data.ProductLookupResult
import com.freshtrack.ocr.data.ProductRepository
import kotlinx.coroutines.launch
import java.util.Locale

class ProductResultActivity : AppCompatActivity() {

    private val repository = ProductRepository()


    private val saveProductLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_result)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        val barcode = intent.getStringExtra(EXTRA_BARCODE)
        if (barcode.isNullOrBlank()) {
            Toast.makeText(this, "No barcode received", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.barcodeLabel).text = "Barcode: $barcode"
        lookupProduct(barcode)
    }

    private fun lookupProduct(barcode: String) {
        setLoading(true)
        lifecycleScope.launch {
            when (val result = repository.lookup(barcode)) {
                is ProductLookupResult.Found ->
                    showProduct(result.product, aiSuggested = false)

                is ProductLookupResult.AiSuggestion ->
                    showProduct(result.product, aiSuggested = true)

                is ProductLookupResult.NotFound ->
                    showMessage("Product not found", notFoundMessage(result.aiNote))

                is ProductLookupResult.Error ->
                    showMessage("Lookup failed", result.message)
            }
            setLoading(false)
        }
    }

    private fun notFoundMessage(aiNote: String?): String {
        val openFacts = "This barcode isn't in the Open Facts databases yet."
        val ai = aiNote ?: "An AI web search couldn't identify it either."
        return "$openFacts $ai Please check the packaging for the ingredients."
    }

    private fun showProduct(product: ProductInfo, aiSuggested: Boolean) {
        findViewById<TextView>(R.id.allergensWarning).visibility = View.GONE
        findViewById<TextView>(R.id.allergenUnknownNote).visibility = View.VISIBLE
        findViewById<View>(R.id.nutritionCard).visibility = View.GONE
        findViewById<TextView>(R.id.productName).text = product.name

        findViewById<TextView>(R.id.productBrand).apply {
            text = product.brand.orEmpty()
            visibility = if (product.brand.isNullOrBlank()) View.GONE else View.VISIBLE
        }

        showImage(product.imageUrl)
        showAllergens(product.allergens)

        findViewById<View>(R.id.ingredientsSection).visibility = View.VISIBLE
        findViewById<TextView>(R.id.ingredientsText).text =
            product.ingredientsText?.takeIf { it.isNotBlank() }
                ?: "Ingredients information not available."

        val dataNote = findViewById<TextView>(R.id.dataNote)

        if (aiSuggested) {
            // Clearly label anything that did not come from Open Food Facts.
            findViewById<TextView>(R.id.messageText).apply {
                text = "AI suggestion, not verified. This product wasn't found in Open Food Facts, " +
                        "so these details come from an AI web search and may be wrong. " +
                        "Allergen information isn't available. Please check the packaging."
                setTextColor(Color.parseColor("#B26A00"))
                visibility = View.VISIBLE
            }
            dataNote.text = "This information came from an AI web search, not from Open Food Facts. " +
                    "It can be wrong or incomplete, so please check the packaging."
        } else {
            // Skincare and other non-food products have no nutrition table, so hide that card.
            val hasNutritionTable =
                product.productType != "beauty" &&
                        product.productType != "product"

            if (hasNutritionTable) {
                val nutritionText = buildNutritionText(product)

                if (product.nutriments != null &&
                    nutritionText != "Nutrition information not available."
                ) {
                    findViewById<TextView>(R.id.nutritionFacts).text = nutritionText
                    findViewById<View>(R.id.nutritionCard).visibility = View.VISIBLE
                }
            }
        }

        dataNote.visibility = View.VISIBLE
        setupSaveCard(product)
    }

    private fun showImage(url: String?) {
        val imageView = findViewById<ImageView>(R.id.productImage)
        if (url.isNullOrBlank()) {
            imageView.visibility = View.GONE
            return
        }
        imageView.visibility = View.VISIBLE
        imageView.load(url) {
            crossfade(true)
            // A broken image link should not leave an empty box on screen.
            listener(onError = { _, _ -> imageView.visibility = View.GONE })
        }
    }

    private fun showAllergens(allergens: List<String>) {
        val warning = findViewById<TextView>(R.id.allergensWarning)
        val unknownNote = findViewById<TextView>(R.id.allergenUnknownNote)

        if (allergens.isEmpty()) {
            warning.visibility = View.GONE
            unknownNote.visibility = View.VISIBLE
            return
        }

        warning.text = "Declared allergens: " +
                allergens.joinToString(", ") { it.replace('-', ' ') }

        warning.visibility = View.VISIBLE
        unknownNote.visibility = View.GONE
    }

    private fun buildNutritionText(product: ProductInfo): String {
        val n = product.nutriments ?: return "Nutrition information not available."
        val lines = listOfNotNull(
            n.energyKcalPer100g?.let { "Energy: ${fmt(it)} kcal" },
            n.proteinsPer100g?.let { "Protein: ${fmt(it)} g" },
            n.carbohydratesPer100g?.let { "Carbs: ${fmt(it)} g" },
            n.sugarsPer100g?.let { "   of which sugars: ${fmt(it)} g" },
            n.fatPer100g?.let { "Fat: ${fmt(it)} g" },
            n.saturatedFatPer100g?.let { "   of which saturated: ${fmt(it)} g" },
            n.fiberPer100g?.let { "Fiber: ${fmt(it)} g" },
            n.saltPer100g?.let { "Salt: ${fmt(it)} g" }
        )
        if (lines.isEmpty()) return "Nutrition information not available."
        return "Per 100 g:\n" + lines.joinToString("\n")
    }

    private fun fmt(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString()
        else String.format(Locale.US, "%.1f", value)


    private fun setupSaveCard(product: ProductInfo) {
        val input = findViewById<EditText>(R.id.productNameInput)
        val button = findViewById<Button>(R.id.addButton)

        findViewById<View>(R.id.addCard).visibility = View.VISIBLE
        input.setText(product.name)

        button.setOnClickListener {
            // If the user clears the name box, fall back to the product's own name.
            val name = input.text.toString().trim().ifBlank { product.name }

            val intent = Intent(this, AddProductActivity::class.java)
                .putExtra("productName", name)
                .putExtra("barcode", product.barcode)
                .putExtra("imageUrl", product.imageUrl)

            // Skincare barcodes start in the Cosmetics category. The user can change it.
            if (product.productType == "beauty") {
                intent.putExtra("category", "Cosmetics")
            }

            saveProductLauncher.launch(intent)
        }
    }

    private fun showMessage(title: String, message: String) {
        findViewById<TextView>(R.id.productName).text = title
        findViewById<TextView>(R.id.messageText).apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    private fun setLoading(loading: Boolean) {
        findViewById<View>(R.id.progressBar).visibility = if (loading) View.VISIBLE else View.GONE
    }

    companion object {
        const val EXTRA_BARCODE = "extra_barcode"
    }
}