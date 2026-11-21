package com.freshtrack.ocr.sync

import com.freshtrack.ocr.data.ConsumptionEvent
import com.freshtrack.ocr.data.Product
import com.freshtrack.ocr.data.ShoppingItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.OffsetDateTime

@Serializable
data class RemoteProduct(
    @SerialName("id") val syncId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("product_name") val productName: String,
    @SerialName("expiry_date") val expiryDate: String,
    @SerialName("raw_ocr_text") val rawOcrText: String,
    @SerialName("category") val category: String,
    @SerialName("location") val location: String,
    @SerialName("barcode") val barcode: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("quantity") val quantity: Double,
    @SerialName("quantity_unit") val quantityUnit: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class RemoteShoppingItem(
    @SerialName("id") val syncId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("item_name") val itemName: String,
    @SerialName("quantity") val quantity: Int,
    @SerialName("category") val category: String,
    @SerialName("is_purchased") val isPurchased: Boolean,
    @SerialName("inventory_transferred") val inventoryTransferred: Boolean,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class RemoteConsumptionEvent(
    @SerialName("id") val syncId: String,
    @SerialName("product_id") val productSyncId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("quantity") val quantity: Double,
    @SerialName("quantity_unit") val quantityUnit: String,
    @SerialName("consumed_at") val consumedAt: String
)

// --- Timestamp Helpers (Epoch Millis <-> Supabase timestamptz) ---

private fun Long.toIsoTimestamp(): String = Instant.ofEpochMilli(this).toString()

private fun String.toEpochMillis(): Long =
    runCatching { OffsetDateTime.parse(this).toInstant().toEpochMilli() }
        .getOrElse { Instant.parse(this).toEpochMilli() }

// --- Room -> Supabase Mappers ---

fun Product.toRemote(): RemoteProduct = RemoteProduct(
    syncId = syncId,
    userId = userId,
    productName = productName,
    expiryDate = expiryDate,
    rawOcrText = rawOcrText,
    category = category,
    location = location,
    barcode = barcode,
    imageUrl = imageUrl,
    quantity = quantity,
    quantityUnit = quantityUnit,
    updatedAt = updatedAt.toIsoTimestamp(),
    deletedAt = deletedAt?.toIsoTimestamp()
)

fun ShoppingItem.toRemote(): RemoteShoppingItem = RemoteShoppingItem(
    syncId = syncId,
    userId = userId,
    itemName = itemName,
    quantity = quantity,
    category = category,
    isPurchased = isPurchased,
    inventoryTransferred = inventoryTransferred,
    updatedAt = updatedAt.toIsoTimestamp(),
    deletedAt = deletedAt?.toIsoTimestamp()
)

fun ConsumptionEvent.toRemote(): RemoteConsumptionEvent = RemoteConsumptionEvent(
    syncId = syncId,
    productSyncId = productSyncId,
    userId = userId,
    quantity = quantity,
    quantityUnit = quantityUnit,
    consumedAt = consumedAt.toIsoTimestamp()
)

// --- Supabase -> Room Mappers ---

fun RemoteProduct.toLocal(existingId: Int = 0): Product = Product(
    id = existingId,
    productName = productName,
    expiryDate = expiryDate,
    rawOcrText = rawOcrText,
    category = category,
    location = location,
    barcode = barcode,
    imageUrl = imageUrl,
    userId = userId,
    syncId = syncId,
    quantity = quantity,
    quantityUnit = quantityUnit,
    updatedAt = updatedAt.toEpochMillis(),
    deletedAt = deletedAt?.toEpochMillis()
)

fun RemoteShoppingItem.toLocal(existingId: Int = 0): ShoppingItem = ShoppingItem(
    id = existingId,
    itemName = itemName,
    quantity = quantity,
    category = category,
    isPurchased = isPurchased,
    inventoryTransferred = inventoryTransferred,
    userId = userId,
    syncId = syncId,
    updatedAt = updatedAt.toEpochMillis(),
    deletedAt = deletedAt?.toEpochMillis()
)

fun RemoteConsumptionEvent.toLocal(existingId: Int = 0): ConsumptionEvent = ConsumptionEvent(
    id = existingId,
    syncId = syncId,
    productSyncId = productSyncId,
    userId = userId,
    quantity = quantity,
    quantityUnit = quantityUnit,
    consumedAt = consumedAt.toEpochMillis()
)