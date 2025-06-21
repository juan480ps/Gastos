package com.uaa.gastos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringTransaction: RecurringTransactionEntity): Long

    @Update
    suspend fun update(recurringTransaction: RecurringTransactionEntity)

    @Delete
    suspend fun delete(recurringTransaction: RecurringTransactionEntity)

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: Int): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions ORDER BY nextDueDate ASC")
    fun getAll(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextDueDate <= :currentDate")
    suspend fun getDueRecurringTransactions(currentDate: String): List<RecurringTransactionEntity>
}