package com.expenses.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM categories WHERE isDefault = 0")
    suspend fun deleteAllNonDefaultCategories()

    @Query("SELECT COUNT(*) FROM categories WHERE name = :name")
    suspend fun categoryExists(name: String): Int
}
