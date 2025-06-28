// CategoryViewModel

package com.uaa.gastos.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.gastos.data.AppDatabase
import com.uaa.gastos.data.CategoryEntity
import com.uaa.gastos.model.Category
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val categoryDao = AppDatabase.getInstance(application).categoryDao()

    val categories: StateFlow<List<Category>> = categoryDao.getAll()
        .map { entities ->
            entities.map { Category(id = it.id, name = it.name) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun addCategory(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val existingCategory = categories.value.firstOrNull { it.name.equals(name, ignoreCase = true) }
                if (existingCategory != null) {
                    onError("La categoría '$name' ya existe.")
                } else {
                    val result = categoryDao.insert(CategoryEntity(name = name))
                    if (result != -1L) {
                        onSuccess()
                    } else {
                        onError("No se pudo guardar la categoría. Puede que ya exista.")
                    }
                }
            } else {
                onError("El nombre de la categoría no puede estar vacío.")
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.delete(CategoryEntity(id = category.id, name = category.name))
        }
    }
}