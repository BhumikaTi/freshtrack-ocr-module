package com.freshtrack.ocr.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConsumptionEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ConsumptionEvent)

    @Query("""
        SELECT * FROM consumption_events
        WHERE productSyncId = :productSyncId AND userId = :userId
        ORDER BY consumedAt DESC
    """)
    suspend fun getEventsForProduct(
        productSyncId: String,
        userId: String
    ): List<ConsumptionEvent>

    @Query("""
        SELECT SUM(quantity) FROM consumption_events
        WHERE productSyncId = :productSyncId AND userId = :userId
    """)
    suspend fun getTotalConsumedForProduct(
        productSyncId: String,
        userId: String
    ): Double?

    @Query("""
        SELECT * FROM consumption_events
        WHERE syncId = :syncId AND userId = :userId
        LIMIT 1
    """)
    suspend fun getEventBySyncId(
        syncId: String,
        userId: String
    ): ConsumptionEvent?

    @Query("""
        SELECT * FROM consumption_events
        WHERE userId = :userId
    """)
    suspend fun getAllEvents(
        userId: String
    ): List<ConsumptionEvent>
}