package com.freshtrack.ocr.data

import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room

@Database(entities = [Product::class], version = 1, exportSchema = false)
abstract class ProductDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    companion object {
        fun getDatabase(context: Context): ProductDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ProductDatabase::class.java,
                "freshtrack_database"
            ).build()
        }
    }
}