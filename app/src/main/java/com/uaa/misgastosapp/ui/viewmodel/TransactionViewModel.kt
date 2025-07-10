// TransactionViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.AppDatabase
import com.uaa.misgastosapp.data.repository.TransactionRepository
import com.uaa.misgastosapp.model.Transaction
import com.uaa.misgastosapp.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = TransactionRepository(db.transactionDao(), db.categoryDao())
    }

    private val _operationStatus = MutableStateFlow<Result<String>?>(null)
    val operationStatus: StateFlow<Result<String>?> = _operationStatus.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun addTransaction(title: String, amount: Double, date: String, categoryId: Int?) {
        viewModelScope.launch {
            try {
                _operationStatus.value = Result.Loading
                repository.insertTransaction(title, amount, date, categoryId)
                _operationStatus.value = Result.Success("Transacción agregada exitosamente")
            } catch (e: Exception) {
                Log.e("TransactionVM", "Error al agregar transacción", e)
                _operationStatus.value = Result.Error("Error al agregar transacción: ${e.message}")
            }
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            try {
                _operationStatus.value = Result.Loading
                repository.deleteTransaction(id)
                _operationStatus.value = Result.Success("Transacción eliminada")
            } catch (e: Exception) {
                Log.e("TransactionVM", "Error al eliminar transacción", e)
                _operationStatus.value = Result.Error("Error al eliminar transacción: ${e.message}")
            }
        }
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }
}