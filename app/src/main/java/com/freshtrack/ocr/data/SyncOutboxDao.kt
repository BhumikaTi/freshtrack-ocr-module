package com.freshtrack.ocr.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncOutboxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboxItem(item: SyncOutbox)

    @Query("""
        SELECT * FROM sync_outbox
        WHERE nextAttemptAt <= :now
        ORDER BY createdAt ASC
    """)
    suspend fun getPendingItems(now: Long): List<SyncOutbox>

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("""
        UPDATE sync_outbox
        SET retryCount = retryCount + 1,
            lastError = :error,
            nextAttemptAt = :nextAttemptAt
        WHERE id = :id
    """)
    suspend fun markFailed(
        id: Int,
        error: String,
        nextAttemptAt: Long
    )
}