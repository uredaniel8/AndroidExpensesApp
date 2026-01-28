package com.expenses.app.data

import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)

    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    suspend fun categoryExists(name: String): Boolean = categoryDao.categoryExists(name) > 0

    suspend fun initializeDefaultCategories() {
        val defaultCategories = listOf(
            Category("Uncategorized", isDefault = true),
            Category("Fuel", isDefault = true),
            Category("Lunch", isDefault = true),
            Category("Dinner", isDefault = true),
            Category("Hotel", isDefault = true),
            Category("Transport", isDefault = true),
            Category("Office Supplies", isDefault = true),
            Category("Entertainment", isDefault = true)
        )
        
        defaultCategories.forEach { category ->
            if (!categoryExists(category.name)) {
                insertCategory(category)
            }
        }
    }
}
