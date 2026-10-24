package com.freshtrack.ocr.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Product::class, ShoppingItem::class],
    version = 6,
    exportSchema = false
)
abstract class ProductDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    abstract fun shoppingItemDao(): ShoppingItemDao

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

        // Version 3 -> 4: add barcode and image fields.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.addProductColumnIfMissing("barcode")
                database.addProductColumnIfMissing("imageUrl")
            }
        }

        // Version 4 -> 5: create the shopping-list table.
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

        // Version 5 -> 6: accommodate either branch's version-5 schema.
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
                        ADD COLUMN inventoryTransferred
                        INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )
                }
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
                        MIGRATION_5_6
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
