package com.expenses.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY receiptDate DESC")
    fun getAllReceipts(): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getReceiptById(id: String): Receipt?

    @Query("SELECT * FROM receipts WHERE receiptDate BETWEEN :startDate AND :endDate ORDER BY receiptDate DESC")
    fun getReceiptsByDateRange(startDate: Long, endDate: Long): Flow<List<Receipt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: Receipt)

    @Update
    suspend fun updateReceipt(receipt: Receipt)

    @Delete
    suspend fun deleteReceipt(receipt: Receipt)

    @Query("DELETE FROM receipts")
    suspend fun deleteAllReceipts()
}
