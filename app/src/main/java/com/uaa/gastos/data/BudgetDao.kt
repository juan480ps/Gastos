package com.uaa.gastos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND monthYear = :monthYear LIMIT 1")
    fun getBudgetForCategoryAndMonth(categoryId: Int, monthYear: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear")
    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>>

    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteBudgetById(budgetId: Int)

     @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
     suspend fun deleteBudgetsForCategory(categoryId: Int)
}