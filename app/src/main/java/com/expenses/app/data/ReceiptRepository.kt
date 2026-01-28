package com.expenses.app.data

import kotlinx.coroutines.flow.Flow

class ReceiptRepository(private val receiptDao: ReceiptDao) {

    fun getAllReceipts(): Flow<List<Receipt>> = receiptDao.getAllReceipts()

    suspend fun getReceiptById(id: String): Receipt? = receiptDao.getReceiptById(id)

    fun getReceiptsByDateRange(startDate: Long, endDate: Long): Flow<List<Receipt>> =
        receiptDao.getReceiptsByDateRange(startDate, endDate)

    suspend fun insertReceipt(receipt: Receipt) = receiptDao.insertReceipt(receipt)

    suspend fun updateReceipt(receipt: Receipt) = receiptDao.updateReceipt(receipt)

    suspend fun deleteReceipt(receipt: Receipt) = receiptDao.deleteReceipt(receipt)
}
