// RecurringTransactionEntity

package com.uaa.gastos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class RecurrenceType {
    MONTHLY
}

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val categoryId: Int?,
    val recurrenceType: RecurrenceType,
    val dayOfMonth: Int,
    val startDate: String,
    val endDate: String?,
    var nextDueDate: String,
    val isActive: Boolean = true
)