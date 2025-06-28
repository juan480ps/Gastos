// Category

package com.uaa.gastos.model

data class Category(
    val id: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }
}