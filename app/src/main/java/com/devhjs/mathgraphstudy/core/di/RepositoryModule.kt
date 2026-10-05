package com.devhjs.mathgraphstudy.core.di

import com.devhjs.mathgraphstudy.data.repository.GraphFunctionRepositoryImpl
import com.devhjs.mathgraphstudy.domain.repository.GraphFunctionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGraphFunctionRepository(
        impl: GraphFunctionRepositoryImpl
    ): GraphFunctionRepository
}
