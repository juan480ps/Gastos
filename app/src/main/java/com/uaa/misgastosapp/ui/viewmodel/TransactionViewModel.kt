// TransactionViewModel

package com.uaa.gastos.ui.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.gastos.data.AppDatabase
import com.uaa.gastos.data.TransactionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.uaa.gastos.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.uaa.gastos.utils.Result

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionDao = AppDatabase.getInstance(application).transactionDao()
    private val categoryDao = AppDatabase.getInstance(application).categoryDao()

    private val _operationStatus = MutableStateFlow<Result<String>?>(null)
    val operationStatus: StateFlow<Result<String>?> = _operationStatus.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = transactionDao.getAll()
        .map { entityList ->
            try {
                entityList.map { entity ->
                    val categoryName = entity.categoryId?.let { catId ->
                        try {
                            categoryDao.getCategoryNameById(catId)
                        } catch (e: Exception) {
                            Log.e("TransactionVM", "Error getting category name: ${e.message}")
                            null
                        }
                    } ?: "Sin Categoría"
                    Transaction(
                        id = entity.id,
                        title = entity.title,
                        amount = entity.amount,
                        date = entity.date,
                        categoryId = entity.categoryId,
                        categoryName = categoryName
                    )
                }
            } catch (e: Exception) {
                Log.e("TransactionVM", "Error mapping transactions: ${e.message}")
                emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun addTransaction(title: String, amount: Double, date: String, categoryId: Int?) {
        viewModelScope.launch {
            try {
                _operationStatus.value = Result.Loading

                val transaction = TransactionEntity(
                    title = title,
                    amount = amount,
                    date = date,
                    categoryId = categoryId
                )
                transactionDao.insert(transaction)

                _operationStatus.value = Result.Success("Transacción agregada exitosamente")
            } catch (e: SQLiteException) {
                Log.e("TransactionVM", "Database error: ${e.message}")
                _operationStatus.value = Result.Error("Error en la base de datos")
            } catch (e: Exception) {
                Log.e("TransactionVM", "Unexpected error: ${e.message}")
                _operationStatus.value = Result.Error("Error al agregar transacción")
            }
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            try {
                _operationStatus.value = Result.Loading

                val transactionToDelete = transactionDao.getById(id)
                if (transactionToDelete != null) {
                    transactionDao.delete(transactionToDelete)
                    _operationStatus.value = Result.Success("Transacción eliminada")
                } else {
                    _operationStatus.value = Result.Error("Transacción no encontrada")
                }
            } catch (e: SQLiteException) {
                Log.e("TransactionVM", "Database error: ${e.message}")
                _operationStatus.value = Result.Error("Error al eliminar transacción")
            } catch (e: Exception) {
                Log.e("TransactionVM", "Unexpected error: ${e.message}")
                _operationStatus.value = Result.Error("Error inesperado")
            }
        }
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }
}