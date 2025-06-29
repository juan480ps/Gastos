// RecurringTransactionViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.*
import com.uaa.misgastosapp.model.RecurringTransaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@RequiresApi(Build.VERSION_CODES.O)
class RecurringTransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val recurringDao = AppDatabase.getInstance(application).recurringTransactionDao()
    private val transactionDao = AppDatabase.getInstance(application).transactionDao()
    private val categoryDao = AppDatabase.getInstance(application).categoryDao()

    val recurringTransactions: StateFlow<List<RecurringTransaction>> = recurringDao.getAll()
        .map { entities ->
            try {
                entities.map { entity ->
                    val categoryName = entity.categoryId?.let { catId ->
                        try {
                            categoryDao.getCategoryNameById(catId)
                        } catch (e: Exception) {
                            Log.e("RecurringVM", "Error getting category name: ${e.message}")
                            null
                        }
                    }
                    RecurringTransaction(
                        id = entity.id,
                        title = entity.title,
                        amount = entity.amount,
                        categoryId = entity.categoryId,
                        categoryName = categoryName ?: "Sin Categoría",
                        recurrenceType = entity.recurrenceType,
                        dayOfMonth = entity.dayOfMonth,
                        startDate = entity.startDate,
                        endDate = entity.endDate,
                        nextDueDate = entity.nextDueDate,
                        isActive = entity.isActive
                    )
                }
            } catch (e: Exception) {
                Log.e("RecurringVM", "Error mapping recurring transactions: ${e.message}")
                emptyList()
            }
        }
        .catch { e ->
            Log.e("RecurringVM", "Flow error: ${e.message}")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun getRecurringTransactionById(id: Int, callback: (RecurringTransactionEntity?) -> Unit) {
        viewModelScope.launch {
            try {
                val entity = recurringDao.getById(id)
                callback(entity)
            } catch (e: Exception) {
                Log.e("RecurringVM", "Error getting recurring transaction by id: ${e.message}")
                callback(null)
            }
        }
    }

    fun addOrUpdateRecurringTransaction(
        id: Int? = null,
        title: String,
        amount: Double,
        categoryId: Int?,
        recurrenceType: RecurrenceType,
        dayOfMonth: Int,
        startDate: LocalDate,
        endDate: LocalDate?,
        isActive: Boolean = true,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (title.isBlank()) {
                    onError("El título no puede estar vacío.")
                    return@launch
                }
                if (amount <= 0) {
                    onError("El monto debe ser mayor a cero.")
                    return@launch
                }
                if (recurrenceType == RecurrenceType.MONTHLY && (dayOfMonth < 1 || dayOfMonth > 31)) {
                    onError("Día del mes inválido.")
                    return@launch
                }

                val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                var calculatedNextDueDate = calculateNextDueDate(startDate, dayOfMonth, recurrenceType)
                val today = LocalDate.now()

                while (calculatedNextDueDate.isBefore(startDate) || calculatedNextDueDate.isBefore(today)) {
                    calculatedNextDueDate = when (recurrenceType) {
                        RecurrenceType.MONTHLY -> getNextMonthlyDueDate(calculatedNextDueDate, dayOfMonth)
                    }
                }

                val entity = RecurringTransactionEntity(
                    id = id ?: 0,
                    title = title,
                    amount = amount,
                    categoryId = categoryId,
                    recurrenceType = recurrenceType,
                    dayOfMonth = dayOfMonth,
                    startDate = startDate.format(formatter),
                    endDate = endDate?.format(formatter),
                    nextDueDate = calculatedNextDueDate.format(formatter),
                    isActive = isActive
                )

                if (id == null) {
                    recurringDao.insert(entity)
                } else {
                    recurringDao.update(entity)
                }
                onSuccess()
            } catch (e: DateTimeParseException) {
                Log.e("RecurringVM", "Date parsing error: ${e.message}")
                onError("Error con el formato de fecha.")
            } catch (e: SQLiteException) {
                Log.e("RecurringVM", "Database error: ${e.message}")
                onError("Error en la base de datos.")
            } catch (e: Exception) {
                Log.e("RecurringVM", "Unexpected error: ${e.message}")
                onError("Error inesperado al guardar.")
            }
        }
    }

    fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction) {
        viewModelScope.launch {
            try {
                recurringDao.delete(
                    RecurringTransactionEntity(
                        id = recurringTransaction.id,
                        title = recurringTransaction.title,
                        amount = recurringTransaction.amount,
                        categoryId = recurringTransaction.categoryId,
                        recurrenceType = recurringTransaction.recurrenceType,
                        dayOfMonth = recurringTransaction.dayOfMonth,
                        startDate = recurringTransaction.startDate,
                        endDate = recurringTransaction.endDate,
                        nextDueDate = recurringTransaction.nextDueDate,
                        isActive = recurringTransaction.isActive
                    )
                )
            } catch (e: SQLiteException) {
                Log.e("RecurringVM", "Database error deleting: ${e.message}")
            } catch (e: Exception) {
                Log.e("RecurringVM", "Unexpected error deleting: ${e.message}")
            }
        }
    }

    fun processDueRecurringTransactions() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                val dueItems = recurringDao.getDueRecurringTransactions(today.format(formatter))

                for (item in dueItems) {
                    try {
                        if (!item.isActive) continue

                        val nextDueDate = LocalDate.parse(item.nextDueDate, formatter)
                        val endDate = item.endDate?.let { LocalDate.parse(it, formatter) }

                        if (endDate != null && nextDueDate.isAfter(endDate)) {
                            continue
                        }

                        transactionDao.insert(
                            TransactionEntity(
                                title = item.title,
                                amount = -item.amount,
                                date = item.nextDueDate,
                                categoryId = item.categoryId
                            )
                        )

                        val newNextDueDate = calculateNextDueDate(
                            nextDueDate.plusDays(1),
                            item.dayOfMonth,
                            item.recurrenceType
                        )

                        if (endDate != null && newNextDueDate.isAfter(endDate)) {
                            recurringDao.update(
                                item.copy(isActive = false, nextDueDate = newNextDueDate.format(formatter))
                            )
                        } else {
                            recurringDao.update(item.copy(nextDueDate = newNextDueDate.format(formatter)))
                        }
                    } catch (e: Exception) {
                        Log.e("RecurringVM", "Error processing item ${item.id}: ${e.message}")
                    }
                }
            } catch (e: DateTimeParseException) {
                Log.e("RecurringVM", "Date parsing error in processing: ${e.message}")
            } catch (e: SQLiteException) {
                Log.e("RecurringVM", "Database error in processing: ${e.message}")
            } catch (e: Exception) {
                Log.e("RecurringVM", "Unexpected error in processing: ${e.message}")
            }
        }
    }

    private fun calculateNextDueDate(fromDate: LocalDate, dayOfMonth: Int, type: RecurrenceType): LocalDate {
        return try {
            when (type) {
                RecurrenceType.MONTHLY -> getNextMonthlyDueDate(fromDate, dayOfMonth)
            }
        } catch (e: Exception) {
            Log.e("RecurringVM", "Error calculating next due date: ${e.message}")
            fromDate.plusMonths(1)
        }
    }

    private fun getNextMonthlyDueDate(currentDate: LocalDate, day: Int): LocalDate {
        return try {
            var yearMonth = YearMonth.from(currentDate)
            var targetDay = day.coerceAtMost(yearMonth.lengthOfMonth())
            var nextDate = yearMonth.atDay(targetDay)

            if (nextDate.isBefore(currentDate) || nextDate.isEqual(currentDate)) {
                yearMonth = yearMonth.plusMonths(1)
                targetDay = day.coerceAtMost(yearMonth.lengthOfMonth())
                nextDate = yearMonth.atDay(targetDay)
            }
            nextDate
        } catch (e: Exception) {
            Log.e("RecurringVM", "Error in getNextMonthlyDueDate: ${e.message}")
            currentDate.plusMonths(1)
        }
    }
}