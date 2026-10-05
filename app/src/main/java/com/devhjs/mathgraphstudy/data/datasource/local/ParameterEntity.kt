package com.devhjs.mathgraphstudy.data.datasource.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parameters")
data class ParameterEntity(
    @PrimaryKey val name: String,
    val value: Double,
    val min: Double,
    val max: Double
)
