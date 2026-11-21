package com.freshtrack.ocr.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "consumption_events")
data class ConsumptionEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val syncId: String = UUID.randomUUID().toString(),
    val productSyncId: String,
    val userId: String = "",
    val quantity: Double,
    val quantityUnit: String = "unit",
    val consumedAt: Long = System.currentTimeMillis()
)