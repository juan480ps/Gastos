package com.uaa.gastos.ui.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.gastos.data.*
import com.uaa.gastos.model.RecurringTransaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@RequiresApi(Build.VERSION_CODES.O)
class RecurringTransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val recurringDao = AppDatabase.getInstance(application).recurringTransactionDao()
    private val transactionDao = AppDatabase.getInstance(application).transactionDao()
    private val categoryDao = AppDatabase.getInstance(application).categoryDao()

    val recurringTransactions: StateFlow<List<RecurringTransaction>> = recurringDao.getAll()
        .map { entities ->
            entities.map { entity ->
                val categoryName = entity.categoryId?.let { catId ->
                    categoryDao.getCategoryNameById(catId)
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
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun getRecurringTransactionById(id: Int, callback: (RecurringTransactionEntity?) -> Unit) {
        viewModelScope.launch {
            callback(recurringDao.getById(id))
        }
    }

    fun addOrUpdateRecurringTransaction(
        id: Int? = null, // null para añadir, valor para editar
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

            val formatter = DateTimeFormatter.ISO_LOCAL_DATE // YYYY-MM-DD

            // Calcular la primera 'nextDueDate'
            var calculatedNextDueDate = calculateNextDueDate(startDate, dayOfMonth, recurrenceType)
            val today = LocalDate.now()
            // Si la primera fecha de vencimiento calculada ya pasó Y es ANTES o IGUAL a la fecha de inicio,
            // la avanzamos al siguiente periodo válido que sea igual o posterior a hoy.
            while (calculatedNextDueDate.isBefore(startDate) || calculatedNextDueDate.isBefore(today)) {
                calculatedNextDueDate = when (recurrenceType) {
                    RecurrenceType.MONTHLY -> getNextMonthlyDueDate(calculatedNextDueDate, dayOfMonth)
                    // Implementar otros tipos si es necesario
                }
            }


            val entity = RecurringTransactionEntity(
                id = id ?: 0,
                title = title,
                amount = amount, // Guardar como positivo, se convertirá a negativo al crear la transacción
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
        }
    }

    fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction) {
        viewModelScope.launch {
            recurringDao.delete(
                RecurringTransactionEntity( // Reconstruir Entity para el DAO
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
        }
    }

    fun processDueRecurringTransactions() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            val dueItems = recurringDao.getDueRecurringTransactions(today.format(formatter))

            for (item in dueItems) {
                if (!item.isActive) continue

                val nextDueDate = LocalDate.parse(item.nextDueDate, formatter)
                val endDate = item.endDate?.let { LocalDate.parse(it, formatter) }

                // No generar si la fecha de vencimiento es posterior a la fecha de finalización
                if (endDate != null && nextDueDate.isAfter(endDate)) {
                    // Opcionalmente, desactivar la recurrencia aquí
                    // recurringDao.update(item.copy(isActive = false))
                    continue
                }

                // Generar la transacción
                transactionDao.insert(
                    TransactionEntity(
                        title = item.title,
                        amount = -item.amount, // Hacerlo negativo para que sea un gasto
                        date = item.nextDueDate,
                        categoryId = item.categoryId
                    )
                )

                // Actualizar la 'nextDueDate' para la siguiente ocurrencia
                val newNextDueDate = calculateNextDueDate(nextDueDate.plusDays(1), item.dayOfMonth, item.recurrenceType)

                // Si la nueva fecha de vencimiento es después de la fecha de finalización,
                // podríamos desactivar la recurrencia o simplemente dejar que la comprobación anterior lo maneje.
                if (endDate != null && newNextDueDate.isAfter(endDate)) {
                    recurringDao.update(item.copy(isActive = false, nextDueDate = newNextDueDate.format(formatter)))
                } else {
                    recurringDao.update(item.copy(nextDueDate = newNextDueDate.format(formatter)))
                }
            }
        }
    }

    private fun calculateNextDueDate(fromDate: LocalDate, dayOfMonth: Int, type: RecurrenceType): LocalDate {
        return when (type) {
            RecurrenceType.MONTHLY -> getNextMonthlyDueDate(fromDate, dayOfMonth)
            // Agrega otros tipos aquí
        }
    }

    private fun getNextMonthlyDueDate(currentDate: LocalDate, day: Int): LocalDate {
        var yearMonth = YearMonth.from(currentDate)
        var targetDay = day.coerceAtMost(yearMonth.lengthOfMonth()) // Ajustar si el día es > días en mes

        var nextDate = yearMonth.atDay(targetDay)

        // Si la fecha calculada para este mes ya pasó (o es hoy y 'currentDate' es de hoy para el siguiente), o es antes de 'currentDate'
        // avanzamos al siguiente mes.
        if (nextDate.isBefore(currentDate) || nextDate.isEqual(currentDate) ) {
            yearMonth = yearMonth.plusMonths(1)
            targetDay = day.coerceAtMost(yearMonth.lengthOfMonth())
            nextDate = yearMonth.atDay(targetDay)
        }
        return nextDate
    }
}