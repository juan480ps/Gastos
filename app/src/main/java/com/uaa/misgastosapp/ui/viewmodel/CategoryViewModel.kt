// CategoryViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.AppDatabase
import com.uaa.misgastosapp.data.repository.CategoryRepository
import com.uaa.misgastosapp.model.Category
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CategoryRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = CategoryRepository(db.categoryDao())
    }

    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun addCategory(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    throw IllegalArgumentException("El nombre de la categoría no puede estar vacío.")
                }
                repository.insertCategory(name)
                onSuccess()
            } catch (e: Exception) {
                Log.e("CategoryVM", "Error al agregar categoría", e)
                onError(e.message ?: "Error inesperado.")
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(category)
            } catch (e: Exception) {
                Log.e("CategoryVM", "Error al eliminar categoría", e)
                // Opcional: Notificar a la UI sobre el error
            }
        }
    }
}