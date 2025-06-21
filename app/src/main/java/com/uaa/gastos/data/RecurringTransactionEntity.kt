package com.uaa.gastos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// Define los tipos de recurrencia
enum class RecurrenceType {
    MONTHLY,
    // WEEKLY, // Podrías añadir más tipos luego
    // YEARLY
}

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL // Si se borra categoría, se pone a null
        )
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double, // Siempre positivo para gastos, negativo para ingresos recurrentes si los implementas
    val categoryId: Int?,
    val recurrenceType: RecurrenceType,
    val dayOfMonth: Int, // Para RecurrenceType.MONTHLY (1-31)
    // val dayOfWeek: Int, // Para RecurrenceType.WEEKLY (1-7)
    // val monthOfYear: Int, // Para RecurrenceType.YEARLY (1-12)
    val startDate: String, // "YYYY-MM-DD"
    val endDate: String?,  // "YYYY-MM-DD", opcional
    var nextDueDate: String, // "YYYY-MM-DD", se actualiza después de generar la transacción
    val isActive: Boolean = true // Para poder desactivar sin borrar
)