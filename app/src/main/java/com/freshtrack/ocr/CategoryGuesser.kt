package com.freshtrack.ocr

/**
 * Guesses an Add Product category from a product name, offline and instantly.
 * Returns null when nothing matches, so the caller leaves the category alone.
 * Category names must match the list in AddProductActivity.
 *
 * Order matters: more specific groups (pets, medicines, cleaning) are checked first so that
 * "dog food" is not read as food and "hand wash" is not read as a drink.
 */
object CategoryGuesser {

    private val RULES: List<Pair<String, List<String>>> = listOf(
        "Pet Supplies" to listOf("dog", "cat food", "puppy", "kitten", "pet food", "pedigree", "whiskas", "cat litter", "pet "),
        "Medicines" to listOf(
            "tablet", "capsule", "syrup", "ointment", "paracetamol", "ibuprofen", "antacid",
            "cough", "vitamin", "supplement", "bandage", "antiseptic", "eye drop", "pain relief", "balm"
        ),
        "Cleaning Supplies" to listOf(
            "detergent", "dishwash", "dish wash", "floor cleaner", "toilet cleaner", "bleach",
            "disinfectant", "surf excel", "vim", "harpic", "lizol", "glass cleaner", "laundry", "fabric softener"
        ),
        "Cosmetics" to listOf(
            "moisturizer", "moisturiser", "moisturizing", "moisturising", "lipstick", "lip balm", "foundation", "serum", "sunscreen",
            "face cream", "night cream", "kajal", "mascara", "eyeliner", "compact", "nail polish",
            "toner", "face mask", "body lotion", "cold cream", "skin"
        ),
        "Personal Care" to listOf(
            "shampoo", "conditioner", "soap", "face wash", "facewash", "hand wash", "handwash", "body wash",
            "toothpaste", "toothbrush", "mouthwash", "deodorant", "deo ", "razor", "shaving", "sanitary",
            "sanitizer", "sanitiser", "hair oil", "hair gel", "talc", "powder", "wipes", "diaper"
        ),
        "Dairy" to listOf("milk", "curd", "yogurt", "yoghurt", "cheese", "butter", "paneer", "ghee", "cream", "dahi", "lassi"),
        "Meat & Seafood" to listOf("chicken", "mutton", "fish", "prawn", "shrimp", "meat", "sausage", "salami", "egg", "bacon"),
        "Bakery" to listOf("bread", "bun", "cake", "pastry", "croissant", "muffin", "cookie dough", "pav", "rusk", "bagel"),
        "Beverages" to listOf(
            "juice", "cola", "soda", "tea", "coffee", "energy drink", "mineral water", "drinking water",
            "squash", "smoothie", "sharbat", "lemonade", "beer", "wine"
        ),
        "Snacks" to listOf(
            "biscuit", "chips", "namkeen", "chocolate", "candy", "wafer", "cracker", "popcorn", "bhujia",
            "cookies", "snack", "bar", "chikki", "mixture", "kurkure"
        ),
        "Fruits & Vegetables" to listOf(
            "apple", "banana", "mango", "orange", "grapes", "tomato", "potato", "onion", "carrot", "spinach",
            "fruit", "vegetable", "lettuce", "cucumber", "capsicum"
        ),
        "Pantry" to listOf(
            "rice", "atta", "flour", "maida", "dal", "lentil", "oil", "salt", "sugar", "masala", "spice",
            "pasta", "noodles", "sauce", "ketchup", "jam", "honey", "oats", "cereal", "pickle", "vinegar",
            "besan", "poha", "soup", "peanut butter"
        )
    )

    fun guess(name: String?): String? {
        val text = " " + (name ?: "").lowercase().replace(Regex("[^a-z0-9& ]"), " ").replace(Regex("\\s+"), " ").trim() + " "
        if (text.isBlank()) return null
        for ((category, keywords) in RULES) {
            if (keywords.any { k -> matches(text, k) }) return category
        }
        return null
    }

    // Whole-word match, so "bar" does not match "barley" and "tea" does not match "steak".
    private fun matches(text: String, keyword: String): Boolean {
        val k = keyword.lowercase()
        return text.contains(" ${k.trim()} ") || text.contains(" ${k.trim()}s ")
    }
}