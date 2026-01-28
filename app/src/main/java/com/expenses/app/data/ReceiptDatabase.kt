package com.expenses.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Receipt::class, Category::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ReceiptDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: ReceiptDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add description field to receipts table
                database.execSQL("ALTER TABLE receipts ADD COLUMN description TEXT DEFAULT NULL")
                
                // Create categories table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        name TEXT PRIMARY KEY NOT NULL,
                        isDefault INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
                
                // Insert default categories
                val defaultCategories = listOf(
                    "Uncategorized", "Fuel", "Lunch", "Dinner", 
                    "Hotel", "Transport", "Office Supplies", "Entertainment"
                )
                val currentTime = System.currentTimeMillis()
                defaultCategories.forEach { category ->
                    database.execSQL(
                        "INSERT OR IGNORE INTO categories (name, isDefault, createdAt) VALUES (?, 1, ?)",
                        arrayOf(category, currentTime)
                    )
                }
            }
        }

        fun getDatabase(context: Context): ReceiptDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReceiptDatabase::class.java,
                    "receipt_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
