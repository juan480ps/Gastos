// TransactionDao

package com.uaa.misgastosapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY id DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): TransactionEntity?

    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = :categoryId AND date LIKE :monthYearPattern AND amount < 0")
    fun getSpentAmountForCategoryInMonth(categoryId: Int, monthYearPattern: String): Double?
}
