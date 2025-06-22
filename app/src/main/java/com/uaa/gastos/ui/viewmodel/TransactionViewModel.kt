package com.uaa.gastos.ui.viewmodel

import android.app.Application
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

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionDao = AppDatabase.getInstance(application).transactionDao()
    private val categoryDao = AppDatabase.getInstance(application).categoryDao()

    val transactions: StateFlow<List<Transaction>> = transactionDao.getAll()
        .map { entityList ->
            entityList.map { entity ->
                val categoryName = entity.categoryId?.let { catId ->
                    categoryDao.getCategoryNameById(catId)
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
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun addTransaction(title: String, amount: Double, date: String, categoryId: Int?) {
        viewModelScope.launch {
            val transaction = TransactionEntity(
                title = title,
                amount = amount,
                date = date,
                categoryId = categoryId
            )
            transactionDao.insert(transaction)
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            val transactionToDelete = transactionDao.getById(id)
            transactionToDelete?.let {
                transactionDao.delete(it)
            }
        }
    }
}