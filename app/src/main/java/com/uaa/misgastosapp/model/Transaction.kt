// Transaction

package com.uaa.misgastosapp.model

data class Transaction(
    val id: Int,
    val title: String,
    val amount: Double,
    val date: String,
    val categoryId: Int?,
    val categoryName: String?
)