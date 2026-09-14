package com.chanbro.salim.di

import android.content.Context
import com.chanbro.salim.data.local.OnboardingPreferences
import com.chanbro.salim.data.repository.FirebaseAuthRepository
import com.chanbro.salim.data.repository.FirestoreConnectionRepository
import com.chanbro.salim.data.repository.FirestoreBudgetRepository
import com.chanbro.salim.data.repository.FirestoreCategoryRepository
import com.chanbro.salim.data.repository.FirestoreDDayRepository
import com.chanbro.salim.data.repository.FirestoreExpenseRepository
import com.chanbro.salim.data.repository.FirestoreProfileRepository
import com.chanbro.salim.data.repository.FirestoreScheduleRepository
import com.chanbro.salim.data.repository.FirestoreTodoRepository
import com.chanbro.salim.domain.repository.AuthRepository
import com.chanbro.salim.domain.repository.BudgetRepository
import com.chanbro.salim.domain.repository.CategoryRepository
import com.chanbro.salim.domain.repository.ConnectionRepository
import com.chanbro.salim.domain.repository.DDayRepository
import com.chanbro.salim.domain.repository.ExpenseRepository
import com.chanbro.salim.domain.repository.OnboardingRepository
import com.chanbro.salim.domain.repository.ProfileRepository
import com.chanbro.salim.domain.repository.ScheduleRepository
import com.chanbro.salim.domain.repository.TodoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: FirestoreScheduleRepository): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindTodoRepository(impl: FirestoreTodoRepository): TodoRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: FirestoreProfileRepository): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindConnectionRepository(impl: FirestoreConnectionRepository): ConnectionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: FirestoreCategoryRepository): CategoryRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

        // :data 모듈은 Hilt를 쓰지 않으므로 Context 주입이 필요한 구현체는 여기서 만든다.
        @Provides
        @Singleton
        fun provideOnboardingRepository(
            @ApplicationContext context: Context,
        ): OnboardingRepository = OnboardingPreferences(context)
    }
}
