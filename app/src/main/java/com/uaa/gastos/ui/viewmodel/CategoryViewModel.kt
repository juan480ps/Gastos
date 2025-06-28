// CategoryViewModel

package com.uaa.gastos.ui.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.util.Log
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
            try {
                entities.map { Category(id = it.id, name = it.name) }
            } catch (e: Exception) {
                Log.e("CategoryVM", "Error mapping categories: ${e.message}")
                emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun addCategory(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (name.isNotBlank()) {
                    val existingCategory = categories.value.firstOrNull {
                        it.name.equals(name, ignoreCase = true)
                    }
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
            } catch (e: SQLiteConstraintException) {
                Log.e("CategoryVM", "Constraint error: ${e.message}")
                onError("La categoría ya existe.")
            } catch (e: SQLiteException) {
                Log.e("CategoryVM", "Database error: ${e.message}")
                onError("Error en la base de datos.")
            } catch (e: Exception) {
                Log.e("CategoryVM", "Unexpected error: ${e.message}")
                onError("Error inesperado al guardar la categoría.")
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryDao.delete(CategoryEntity(id = category.id, name = category.name))
            } catch (e: SQLiteException) {
                Log.e("CategoryVM", "Database error deleting category: ${e.message}")
            } catch (e: Exception) {
                Log.e("CategoryVM", "Unexpected error deleting category: ${e.message}")
            }
        }
    }
}