package com.devhjs.mathgraphstudy.data.datasource.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GraphFunctionEntity::class, ParameterEntity::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2) // parameters 테이블 추가
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun graphFunctionDao(): GraphFunctionDao
    abstract fun parameterDao(): ParameterDao
}
