package com.chanbro.salim.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Budget
import com.chanbro.salim.domain.usecase.ObserveBudgetUseCase
import com.chanbro.salim.domain.usecase.SaveBudgetUseCase
import com.chanbro.salim.ui.common.formatThousands
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class SettingsUiState(
    val year: Int = 0,
    val month: Int = 0,
    val budgetAmount: Long? = null,
) {
    /** 목록 우측에 노출할 현재 예산값. 미설정이면 안내 문구. (wireframe/settings.md 3.) */
    val budgetText: String
        get() = budgetAmount?.let { "${formatThousands(it.toString())}원" } ?: "미설정"
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeBudget: ObserveBudgetUseCase,
    private val saveBudget: SaveBudgetUseCase,
) : ViewModel() {

    // 설정의 "달별 예산"은 이번 달 기준으로 보여준다.
    private val yearMonth = currentYearMonth()

    val uiState: StateFlow<SettingsUiState> =
        observeBudget(yearMonth.first, yearMonth.second)
            .map {
                SettingsUiState(
                    year = yearMonth.first,
                    month = yearMonth.second,
                    budgetAmount = it?.amount,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SettingsUiState(yearMonth.first, yearMonth.second),
            )

    fun setBudget(amount: Long) {
        viewModelScope.launch {
            saveBudget(Budget(yearMonth.first, yearMonth.second, amount))
        }
    }
}

private fun currentYearMonth(): Pair<Int, Int> = Calendar.getInstance().let {
    it.get(Calendar.YEAR) to it.get(Calendar.MONTH) + 1
}
