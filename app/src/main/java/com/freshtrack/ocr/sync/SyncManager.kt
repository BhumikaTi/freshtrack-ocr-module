package com.freshtrack.ocr.sync

import android.content.Context
import android.util.Log
import com.freshtrack.ocr.auth.SupabaseClientProvider
import com.freshtrack.ocr.data.ProductDatabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SyncManager {

    private const val TAG = "FreshTrackSync"

    suspend fun syncAll(context: Context, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (userId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("User not logged in"))
            }

            try {
                val supabase = SupabaseClientProvider.client

                // Wait for Supabase Auth to finish loading the saved session from storage
                supabase.auth.awaitInitialization()
                val session = supabase.auth.currentSessionOrNull()
                if (session == null) {
                    Log.w(TAG, "Skipping sync: no active Supabase session yet")
                    return@withContext Result.failure(IllegalStateException("No active Supabase session"))
                }

                val db = ProductDatabase.getDatabase(context)
                val productDao = db.productDao()
                val shoppingDao = db.shoppingItemDao()
                val consumptionDao = db.consumptionEventDao()

                // 1. PULL products from Supabase -> Room
                val remoteProducts = supabase.from("products")
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<RemoteProduct>()

                val remoteProductMap = remoteProducts.associateBy { it.syncId }

                for (remote in remoteProducts) {
                    val local = productDao.getProductBySyncId(remote.syncId)
                    val mapped = remote.toLocal(existingId = local?.id ?: 0)
                    if (local == null) {
                        productDao.insertProduct(mapped)
                    } else if (mapped.updatedAt > local.updatedAt) {
                        productDao.updateProduct(mapped)
                    }
                }
                Log.d(TAG, "Pulled ${remoteProducts.size} products")

                // 2. PUSH only new or newer local products -> Supabase
                val localProducts = productDao.getAllProductsIncludingDeleted(userId)
                val productsToPush = localProducts.filter { local ->
                    val remote = remoteProductMap[local.syncId]
                    remote == null || local.updatedAt > remote.toLocal().updatedAt
                }
                if (productsToPush.isNotEmpty()) {
                    supabase.from("products").upsert(productsToPush.map { it.toRemote() }) {
                        onConflict = "id"
                    }
                    Log.d(TAG, "Pushed ${productsToPush.size} products")
                }

                // 3. PULL shopping items from Supabase -> Room
                val remoteShopping = supabase.from("shopping_items")
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<RemoteShoppingItem>()

                val remoteShoppingMap = remoteShopping.associateBy { it.syncId }

                for (remote in remoteShopping) {
                    val local = shoppingDao.getItemBySyncId(remote.syncId)
                    val mapped = remote.toLocal(existingId = local?.id ?: 0)
                    if (local == null) {
                        shoppingDao.insertItem(mapped)
                    } else if (mapped.updatedAt > local.updatedAt) {
                        shoppingDao.updateItem(mapped)
                    }
                }
                Log.d(TAG, "Pulled ${remoteShopping.size} shopping items")

                // 4. PUSH only new or newer local shopping items -> Supabase
                val localShopping = shoppingDao.getAllItemsIncludingDeleted(userId)
                val shoppingToPush = localShopping.filter { local ->
                    val remote = remoteShoppingMap[local.syncId]
                    remote == null || local.updatedAt > remote.toLocal().updatedAt
                }
                if (shoppingToPush.isNotEmpty()) {
                    supabase.from("shopping_items").upsert(shoppingToPush.map { it.toRemote() }) {
                        onConflict = "id"
                    }
                    Log.d(TAG, "Pushed ${shoppingToPush.size} shopping items")
                }

                // 5. PULL consumption events from Supabase -> Room
                val remoteEvents = supabase.from("consumption_events")
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<RemoteConsumptionEvent>()

                for (remote in remoteEvents) {
                    val local = consumptionDao.getEventBySyncId(remote.syncId, userId)
                    if (local == null) {
                        consumptionDao.insertEvent(remote.toLocal())
                    }
                }
                Log.d(TAG, "Pulled ${remoteEvents.size} consumption events")

                // 6. PUSH local consumption events -> Supabase
                val remoteEventIds = remoteEvents.map { it.syncId }.toSet()
                val localEvents = consumptionDao.getAllEvents(userId)
                val eventsToPush = localEvents.filter { it.syncId !in remoteEventIds }
                if (eventsToPush.isNotEmpty()) {
                    supabase.from("consumption_events").upsert(eventsToPush.map { it.toRemote() }) {
                        onConflict = "id"
                    }
                    Log.d(TAG, "Pushed ${eventsToPush.size} consumption events")
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed: ${e.message}", e)
                Result.failure(e)
            }
        }
}