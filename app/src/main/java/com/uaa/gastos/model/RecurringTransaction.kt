package com.uaa.gastos.model

import com.uaa.gastos.data.RecurrenceType

data class RecurringTransaction(
    val id: Int,
    val title: String,
    val amount: Double,
    val categoryId: Int?,
    val categoryName: String?,
    val recurrenceType: RecurrenceType,
    val dayOfMonth: Int,
    val startDate: String,
    val endDate: String?,
    val nextDueDate: String,
    val isActive: Boolean
)