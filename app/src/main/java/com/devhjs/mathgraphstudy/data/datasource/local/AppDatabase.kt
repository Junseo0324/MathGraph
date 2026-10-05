package com.devhjs.mathgraphstudy.data.datasource.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GraphFunctionEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun graphFunctionDao(): GraphFunctionDao
}
