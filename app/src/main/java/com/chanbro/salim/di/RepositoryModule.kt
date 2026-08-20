package com.chanbro.salim.di

import com.chanbro.salim.data.repository.FirestoreBudgetRepository
import com.chanbro.salim.data.repository.FirestoreDDayRepository
import com.chanbro.salim.data.repository.FirestoreExpenseRepository
import com.chanbro.salim.domain.repository.BudgetRepository
import com.chanbro.salim.domain.repository.DDayRepository
import com.chanbro.salim.domain.repository.ExpenseRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: FirestoreExpenseRepository): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindDDayRepository(impl: FirestoreDDayRepository): DDayRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: FirestoreBudgetRepository): BudgetRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    }
}
