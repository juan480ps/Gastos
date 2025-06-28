// BadgetViewModel

package com.uaa.gastos.ui.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.gastos.data.AppDatabase
import com.uaa.gastos.data.BudgetEntity
import com.uaa.gastos.model.Budget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val budgetDao = AppDatabase.getInstance(application).budgetDao()
    private val categoryDao = AppDatabase.getInstance(application).categoryDao()
    private val _currentMonthYear = MutableStateFlow(YearMonth.now())
    val currentMonthYear: StateFlow<YearMonth> = _currentMonthYear.asStateFlow()

    private val currentMonthYearString: Flow<String> = _currentMonthYear.map {
        it.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    fun setCurrentMonthYear(yearMonth: YearMonth) {
        try {
            _currentMonthYear.value = yearMonth
        } catch (e: Exception) {
            Log.e("BudgetVM", "Error setting month year: ${e.message}")
        }
    }

    fun getBudgetForCategory(categoryId: Int, monthYear: String): Flow<BudgetEntity?> {
        return budgetDao.getBudgetForCategoryAndMonth(categoryId, monthYear)
            .catch { e ->
                Log.e("BudgetVM", "Error getting budget: ${e.message}")
                emit(null)
            }
    }

    fun setBudget(categoryId: Int, amount: Double, monthYear: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (amount < 0) {
                    onError("El presupuesto no puede ser negativo.")
                    return@launch
                }

                budgetDao.insertOrUpdate(
                    BudgetEntity(
                        categoryId = categoryId,
                        monthYear = monthYear,
                        amount = amount
                    )
                )
                onSuccess()
            } catch (e: SQLiteException) {
                Log.e("BudgetVM", "Database error: ${e.message}")
                onError("Error al guardar el presupuesto en la base de datos.")
            } catch (e: Exception) {
                Log.e("BudgetVM", "Unexpected error: ${e.message}")
                onError("Error inesperado al guardar el presupuesto.")
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetsWithSpendingForCurrentMonth: Flow<List<Budget>> = currentMonthYearString.flatMapLatest { monthStr ->
        combine(
            categoryDao.getAll(),
            budgetDao.getBudgetsForMonth(monthStr),
            getApplication<Application>().let { app ->
                AppDatabase.getInstance(app).transactionDao().getAll()
            }
        ) { categoriesEntities, budgetEntities, transactionEntities ->
            try {
                val transactionsForMonth = transactionEntities.filter {
                    it.date.startsWith(monthStr) && it.amount < 0
                }

                categoriesEntities.map { categoryEntity ->
                    val budgetEntity = budgetEntities.find { it.categoryId == categoryEntity.id }
                    val spentAmount = transactionsForMonth
                        .filter { it.categoryId == categoryEntity.id }
                        .sumOf { it.amount * -1 }
                    val budgetAmount = budgetEntity?.amount ?: 0.0
                    Budget(
                        id = budgetEntity?.id ?: 0,
                        categoryId = categoryEntity.id,
                        categoryName = categoryEntity.name,
                        monthYear = monthStr,
                        amount = budgetAmount,
                        spentAmount = spentAmount
                    )
                }.sortedBy { it.categoryName }
            } catch (e: Exception) {
                Log.e("BudgetVM", "Error calculating budgets: ${e.message}")
                emptyList()
            }
        }.catch { e ->
            Log.e("BudgetVM", "Flow error: ${e.message}")
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}