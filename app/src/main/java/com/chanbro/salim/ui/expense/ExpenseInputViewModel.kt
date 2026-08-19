package com.chanbro.salim.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Expense
import com.chanbro.salim.domain.model.Spender
import com.chanbro.salim.domain.usecase.AddExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ExpenseInputViewModel @Inject constructor(
    private val addExpense: AddExpenseUseCase,
) : ViewModel() {

    /**
     * 입력값으로 지출 1건을 저장.
     * @param dateUtcMillis DatePicker의 UTC-자정 millis
     * @param hour24 0~23, @param minute 0~59 (시간 부분을 UTC 오프셋으로 합산)
     * @param onDone 저장 완료 후 호출(예: 뒤로가기)
     */
    fun save(
        amount: Long,
        spenderLabel: String,
        categoryName: String,
        memo: String,
        dateUtcMillis: Long,
        hour24: Int,
        minute: Int,
        onDone: () -> Unit,
    ) {
        val spender = if (spenderLabel == Spender.PARTNER.label) Spender.PARTNER else Spender.ME
        val spentAt = dateUtcMillis + hour24 * 3_600_000L + minute * 60_000L
        val expense = Expense(
            id = UUID.randomUUID().toString(),
            amount = amount,
            spentAtMillis = spentAt,
            spender = spender,
            categoryName = categoryName,
            memo = memo.trim().ifBlank { null },
            createdAtMillis = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            addExpense(expense)
            onDone()
        }
    }
}
