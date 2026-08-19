package com.chanbro.salim.data.repository

import com.chanbro.salim.domain.model.Expense
import com.chanbro.salim.domain.model.Spender
import com.chanbro.salim.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인메모리 지출 저장소 (Firebase 연동 전 임시 SSOT).
 * 프로세스 생존 동안만 유지되며, 앱 재시작 시 시드 데이터로 초기화된다.
 */
@Singleton
class InMemoryExpenseRepository @Inject constructor() : ExpenseRepository {

    private val expenses = MutableStateFlow(seed())

    override fun observeMonth(year: Int, month: Int): Flow<List<Expense>> =
        expenses.map { list ->
            list.filter { inMonth(it.spentAtMillis, year, month) }
                .sortedByDescending { it.createdAtMillis }
        }

    override suspend fun add(expense: Expense) {
        expenses.update { it + expense }
    }

    private fun inMonth(utcMillis: Long, year: Int, month: Int): Boolean {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        return cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) + 1 == month
    }

    private companion object {
        /** 특정 날짜/시각(UTC 기준)의 millis. */
        fun utc(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(year, month - 1, day, hour, minute)
            }.timeInMillis

        fun seed(): List<Expense> {
            var createdAt = 1_000L
            fun next() = createdAt++
            return listOf(
                Expense("e1", 6_500, utc(2026, 8, 5, 9, 20), Spender.ME, "식비", "스타벅스", next()),
                Expense("e2", 84_000, utc(2026, 8, 5, 19, 10), Spender.PARTNER, "식비", "이마트", next()),
                Expense("e3", 3_000, utc(2026, 8, 4, 8, 5), Spender.PARTNER, "교통", "지하철", next()),
                Expense("e4", 28_000, utc(2026, 8, 4, 20, 30), Spender.ME, "문화/여가", "CGV", next()),
            )
        }
    }
}
