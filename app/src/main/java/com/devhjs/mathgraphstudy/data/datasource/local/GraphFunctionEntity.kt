package com.devhjs.mathgraphstudy.data.datasource.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "graph_functions")
data class GraphFunctionEntity(
    @PrimaryKey val id: String,
    val nodeJson: String, // MathNodeDto 를 JSON 으로 직렬화한 수식 트리
    val color: Long,
    val isVisible: Boolean,
    val createdAt: Long
)
