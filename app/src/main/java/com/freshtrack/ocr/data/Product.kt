package com.freshtrack.ocr.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productName: String,
    val expiryDate: String,
    val rawOcrText: String,
    val category: String = "Other",
    val location: String = "",
    val barcode: String? = null,
    val imageUrl: String? = null,
    val userId: String = "",
    val syncId: String = UUID.randomUUID().toString(),
    val quantity: Double = 1.0,
    val quantityUnit: String = "unit",
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)