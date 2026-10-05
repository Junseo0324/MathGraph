package com.devhjs.mathgraphstudy.data.datasource.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GraphFunctionDao {
    @Query("SELECT * FROM graph_functions ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<GraphFunctionEntity>>

    @Upsert
    suspend fun upsert(entity: GraphFunctionEntity)

    @Query("DELETE FROM graph_functions WHERE id = :id")
    suspend fun deleteById(id: String)
}
