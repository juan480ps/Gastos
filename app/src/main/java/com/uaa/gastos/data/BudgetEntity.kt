package com.uaa.gastos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE // Si se borra una categoría, se borran sus presupuestos
        )
    ],
    // Asegura que solo haya un presupuesto por categoría para un mes/año específico
    indices = [Index(value = ["categoryId", "monthYear"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val monthYear: String, // Formato "YYYY-MM", ej: "2023-10"
    val amount: Double
)