package com.uaa.gastos.ui.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.gastos.data.AppDatabase
import com.uaa.gastos.data.BudgetEntity
import com.uaa.gastos.model.Budget
import com.uaa.gastos.model.Category
import com.uaa.gastos.model.Transaction
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
        _currentMonthYear.value = yearMonth
    }

    fun getBudgetForCategory(categoryId: Int, monthYear: String): Flow<BudgetEntity?> {
        return budgetDao.getBudgetForCategoryAndMonth(categoryId, monthYear)
    }

    fun setBudget(categoryId: Int, amount: Double, monthYear: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
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
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetsWithSpendingForCurrentMonth: Flow<List<Budget>> = currentMonthYearString.flatMapLatest { monthStr ->
        combine(
            categoryDao.getAll(), // Todas las categorías
            budgetDao.getBudgetsForMonth(monthStr), // Presupuestos para el mes actual
            getApplication<Application>().let { app -> // Necesitamos acceso a TransactionDao
                AppDatabase.getInstance(app).transactionDao().getAll() // Todas las transacciones
            }
        ) { categoriesEntities, budgetEntities, transactionEntities ->
            val transactionsForMonth = transactionEntities.filter {
                it.date.startsWith(monthStr) && it.amount < 0 // Solo gastos y del mes actual
            }

            categoriesEntities.map { categoryEntity ->
                val budgetEntity = budgetEntities.find { it.categoryId == categoryEntity.id }
                val spentAmount = transactionsForMonth
                    .filter { it.categoryId == categoryEntity.id }
                    .sumOf { it.amount * -1 } // Sumar como positivo

                val budgetAmount = budgetEntity?.amount ?: 0.0

                Budget(
                    id = budgetEntity?.id ?: 0, // Podría no tener presupuesto aún
                    categoryId = categoryEntity.id,
                    categoryName = categoryEntity.name,
                    monthYear = monthStr,
                    amount = budgetAmount,
                    spentAmount = spentAmount
                    // remainingAmount y progress se calculan en el data class
                )
            }.sortedBy { it.categoryName } // Ordenar alfabéticamente por nombre de categoría
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}