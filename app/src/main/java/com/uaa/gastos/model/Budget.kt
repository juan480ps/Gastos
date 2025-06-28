// Budget

package com.uaa.gastos.model

data class Budget(
    val id: Int,
    val categoryId: Int,
    val categoryName: String,
    val monthYear: String,
    val amount: Double,
    val spentAmount: Double = 0.0,
    val remainingAmount: Double = amount - spentAmount,
    val progress: Float = if (amount > 0) (spentAmount / amount).toFloat() else 0f
)