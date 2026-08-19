package com.chanbro.salim.di

import com.chanbro.salim.data.repository.InMemoryExpenseRepository
import com.chanbro.salim.domain.repository.ExpenseRepository
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
    abstract fun bindExpenseRepository(impl: InMemoryExpenseRepository): ExpenseRepository
}
