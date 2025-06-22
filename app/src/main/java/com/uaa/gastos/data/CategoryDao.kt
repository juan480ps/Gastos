package com.uaa.gastos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): CategoryEntity?

    @Query("SELECT name FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryNameById(id: Int): String?

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("UPDATE categories SET name = :newName WHERE id = :id")
    suspend fun update(id: Int, newName: String)
}