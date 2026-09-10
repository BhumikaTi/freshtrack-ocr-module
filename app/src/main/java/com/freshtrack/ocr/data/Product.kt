package com.freshtrack.ocr.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val productName: String,
    val expiryDate: String,
    val rawOcrText: String
)