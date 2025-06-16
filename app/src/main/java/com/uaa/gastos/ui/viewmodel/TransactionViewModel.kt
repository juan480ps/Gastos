package com.uaa.gastos.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.gastos.data.AppDatabase
import com.uaa.gastos.data.TransactionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.uaa.gastos.model.Transaction

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).transactionDao()

    val transactions = dao.getAll()
//        .map { it }
        .map { list -> list.map { Transaction(it.id, it.title, it.amount, it.date) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTransaction(title: String, amount: Double, date: String) {
        viewModelScope.launch {
            dao.insert(TransactionEntity(title = title, amount = amount, date = date))
        }
    }

//    fun deleteTransaction(transaction: TransactionEntity) {
//        viewModelScope.launch {
//            dao.delete(transaction)
//        }
//    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            val transactionToDelete = dao.getById(id)
            transactionToDelete?.let {
                dao.delete(it)
            }
        }
    }

}
