// CategoryRepository.kt

package com.uaa.misgastosapp.data.repository

import com.uaa.misgastosapp.data.CategoryDao
import com.uaa.misgastosapp.data.CategoryEntity
import com.uaa.misgastosapp.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class CategoryRepository(private val categoryDao: CategoryDao) {

    val allCategories: Flow<List<Category>> = categoryDao.getAll()
        .map { entities ->
            entities.map { Category(id = it.id, name = it.name) }
        }

    suspend fun insertCategory(name: String): Long {
        val existingCategories = allCategories.first()
        if (existingCategories.any { it.name.equals(name, ignoreCase = true) }) {
            throw IllegalStateException("La categoría '$name' ya existe.")
        }
        return categoryDao.insert(CategoryEntity(name = name))
    }

    suspend fun deleteCategory(category: Category) {
        val entity = CategoryEntity(id = category.id, name = category.name)
        categoryDao.delete(entity)
    }
}