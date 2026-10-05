package com.devhjs.mathgraphstudy.data.datasource.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ParameterDao {
    @Query("SELECT * FROM parameters ORDER BY name ASC")
    fun observeAll(): Flow<List<ParameterEntity>>

    @Query("SELECT * FROM parameters")
    suspend fun getAll(): List<ParameterEntity>

    @Upsert
    suspend fun upsert(entity: ParameterEntity)
}
