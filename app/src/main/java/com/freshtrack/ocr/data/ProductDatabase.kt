package com.freshtrack.ocr.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(
    entities = [
        Product::class,
        ShoppingItem::class,
        ConsumptionEvent::class,
        SyncOutbox::class
    ],
    version = 8,
    exportSchema = false
)
abstract class ProductDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    abstract fun shoppingItemDao(): ShoppingItemDao

    abstract fun consumptionEventDao(): ConsumptionEventDao

    abstract fun syncOutboxDao(): SyncOutboxDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                if (!database.hasColumn("Product", "category")) {
                    database.execSQL(
                        "ALTER TABLE Product ADD COLUMN category TEXT NOT NULL DEFAULT 'Other'"
                    )
                }
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                if (!database.hasColumn("Product", "location")) {
                    database.execSQL(
                        "ALTER TABLE Product ADD COLUMN location TEXT NOT NULL DEFAULT ''"
                    )
                }
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.addProductColumnIfMissing("barcode")
                database.addProductColumnIfMissing("imageUrl")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shopping_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemName TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        isPurchased INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.addProductColumnIfMissing("barcode")
                database.addProductColumnIfMissing("imageUrl")

                if (!database.hasTable("shopping_items")) {
                    database.execSQL(
                        """
                        CREATE TABLE shopping_items (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            itemName TEXT NOT NULL,
                            quantity INTEGER NOT NULL,
                            category TEXT NOT NULL,
                            isPurchased INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }

                if (!database.hasColumn(
                        "shopping_items",
                        "inventoryTransferred"
                    )
                ) {
                    database.execSQL(
                        """
                        ALTER TABLE shopping_items
                        ADD COLUMN inventoryTransferred INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )
                }
            }
        }

        // Version 6 -> 7: add userId to both tables.
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {

                if (!database.hasColumn("Product", "userId")) {
                    database.execSQL(
                        "ALTER TABLE Product ADD COLUMN userId TEXT NOT NULL DEFAULT ''"
                    )
                }

                if (!database.hasColumn("shopping_items", "userId")) {
                    database.execSQL(
                        "ALTER TABLE shopping_items ADD COLUMN userId TEXT NOT NULL DEFAULT ''"
                    )
                }
            }
        }

        // Version 7 -> 8: add sync + quantity columns, consumption_events, and sync_outbox.
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Add new columns to Product
                if (!database.hasColumn("Product", "syncId")) {
                    database.execSQL(
                        "ALTER TABLE Product ADD COLUMN syncId TEXT NOT NULL DEFAULT ''"
                    )
                }
                if (!database.hasColumn("Product", "quantity")) {
                    database.execSQL(
                        "ALTER TABLE Product ADD COLUMN quantity REAL NOT NULL DEFAULT 1.0"
                    )
                }
                if (!database.hasColumn("Product", "quantityUnit")) {
                    database.execSQL(
                        "ALTER TABLE Product ADD COLUMN quantityUnit TEXT NOT NULL DEFAULT 'unit'"
                    )
                }
                if (!database.hasColumn("Product", "updatedAt")) {
                    database.execSQL(
                        "ALTER TABLE Product ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0"
                    )
                }
                if (!database.hasColumn("Product", "deletedAt")) {
                    database.execSQL(
                        "ALTER TABLE Product ADD COLUMN deletedAt INTEGER"
                    )
                }

                // 2. Add new columns to shopping_items
                if (!database.hasColumn("shopping_items", "syncId")) {
                    database.execSQL(
                        "ALTER TABLE shopping_items ADD COLUMN syncId TEXT NOT NULL DEFAULT ''"
                    )
                }
                if (!database.hasColumn("shopping_items", "updatedAt")) {
                    database.execSQL(
                        "ALTER TABLE shopping_items ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0"
                    )
                }
                if (!database.hasColumn("shopping_items", "deletedAt")) {
                    database.execSQL(
                        "ALTER TABLE shopping_items ADD COLUMN deletedAt INTEGER"
                    )
                }

                // 3. Give existing rows a unique UUID and current timestamp
                val now = System.currentTimeMillis()
                database.backfillSyncFields("Product", now)
                database.backfillSyncFields("shopping_items", now)

                // 4. Create consumption_events table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS consumption_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        syncId TEXT NOT NULL,
                        productSyncId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        quantityUnit TEXT NOT NULL,
                        consumedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // 5. Create sync_outbox table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_outbox (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entityType TEXT NOT NULL,
                        entitySyncId TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL,
                        lastError TEXT,
                        nextAttemptAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: ProductDatabase? = null

        fun getDatabase(context: Context): ProductDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ProductDatabase::class.java,
                    "freshtrack_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}

private fun SupportSQLiteDatabase.hasColumn(
    tableName: String,
    columnName: String
): Boolean {
    query("PRAGMA table_info(`$tableName`)").use { cursor ->
        val nameIndex = cursor.getColumnIndexOrThrow("name")

        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == columnName) {
                return true
            }
        }
    }
    return false
}

private fun SupportSQLiteDatabase.hasTable(
    tableName: String
): Boolean {
    query(
        "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
        arrayOf(tableName)
    ).use { cursor ->
        return cursor.moveToFirst()
    }
}

private fun SupportSQLiteDatabase.addProductColumnIfMissing(
    columnName: String
) {
    if (!hasColumn("Product", columnName)) {
        execSQL("ALTER TABLE Product ADD COLUMN `$columnName` TEXT")
    }
}

private fun SupportSQLiteDatabase.backfillSyncFields(
    tableName: String,
    now: Long
) {
    val ids = mutableListOf<Int>()
    query("SELECT id FROM `$tableName` WHERE syncId = ''").use { cursor ->
        while (cursor.moveToNext()) {
            ids.add(cursor.getInt(0))
        }
    }

    for (rowId in ids) {
        execSQL(
            "UPDATE `$tableName` SET syncId = ?, updatedAt = ? WHERE id = ?",
            arrayOf(UUID.randomUUID().toString(), now, rowId)
        )
    }
}