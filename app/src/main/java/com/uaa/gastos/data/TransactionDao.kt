package com.uaa.gastos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY id DESC")
    fun getAll(): Flow<List<TransactionEntity>>
}
