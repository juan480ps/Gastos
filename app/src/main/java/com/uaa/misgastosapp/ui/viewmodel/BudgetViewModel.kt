// BudgetViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.AppDatabase
import com.uaa.misgastosapp.data.BudgetEntity
import com.uaa.misgastosapp.data.repository.BudgetRepository
import com.uaa.misgastosapp.model.Budget
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BudgetRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = BudgetRepository(db.budgetDao(), db.categoryDao(), db.transactionDao())
    }

    private val _currentMonthYear = MutableStateFlow(YearMonth.now())
    val currentMonthYear: StateFlow<YearMonth> = _currentMonthYear.asStateFlow()

    private val currentMonthYearString: Flow<String> = _currentMonthYear.map {
        it.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    val budgetsWithSpendingForCurrentMonth: StateFlow<List<Budget>> =
        repository.getBudgetsWithSpendingForMonth(currentMonthYearString)
            .catch { e ->
                Log.e("BudgetVM", "Error en el flujo de presupuestos", e)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCurrentMonthYear(yearMonth: YearMonth) {
        _currentMonthYear.value = yearMonth
    }

    fun getBudgetForCategory(categoryId: Int, monthYear: String): Flow<BudgetEntity?> {
        return repository.getBudgetForCategoryAndMonth(categoryId, monthYear)
            .catch { e ->
                Log.e("BudgetVM", "Error al obtener presupuesto para categoría", e)
                emit(null)
            }
    }

    fun setBudget(categoryId: Int, amount: Double, monthYear: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.setBudget(categoryId, amount, monthYear)
                onSuccess()
            } catch (e: Exception) {
                Log.e("BudgetVM", "Error al establecer presupuesto", e)
                onError(e.message ?: "Error inesperado.")
            }
        }
    }
}