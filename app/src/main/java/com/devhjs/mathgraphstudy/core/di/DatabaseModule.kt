package com.devhjs.mathgraphstudy.core.di

import android.content.Context
import androidx.room.Room
import com.devhjs.mathgraphstudy.data.datasource.local.AppDatabase
import com.devhjs.mathgraphstudy.data.datasource.local.GraphFunctionDao
import com.devhjs.mathgraphstudy.data.datasource.local.ParameterDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "math_graph.db")
            .build()

    @Provides
    fun provideParameterDao(database: AppDatabase): ParameterDao =
        database.parameterDao()

    @Provides
    fun provideGraphFunctionDao(database: AppDatabase): GraphFunctionDao =
        database.graphFunctionDao()
}
