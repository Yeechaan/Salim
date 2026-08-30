package com.chanbro.salim.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Budget
import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.usecase.ObserveBudgetUseCase
import com.chanbro.salim.domain.usecase.ObserveConnectionUseCase
import com.chanbro.salim.domain.usecase.SaveBudgetUseCase
import com.chanbro.salim.domain.usecase.SignOutUseCase
import com.chanbro.salim.ui.common.formatThousands
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class SettingsUiState(
    val year: Int = 0,
    val month: Int = 0,
    val budgetAmount: Long? = null,
    val connection: Connection = Connection.Unknown,
) {
    /** 목록 우측에 노출할 현재 예산값. 미설정이면 안내 문구. (wireframe/settings.md 3.) */
    val budgetText: String
        get() = budgetAmount?.let { "${formatThousands(it.toString())}원" } ?: "미설정"
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeBudget: ObserveBudgetUseCase,
    observeConnection: ObserveConnectionUseCase,
    private val saveBudget: SaveBudgetUseCase,
    private val signOut: SignOutUseCase,
) : ViewModel() {

    // 설정의 "달별 예산"은 이번 달 기준으로 보여준다.
    private val yearMonth = currentYearMonth()

    val uiState: StateFlow<SettingsUiState> =
        combine(
            observeBudget(yearMonth.first, yearMonth.second),
            observeConnection(),
        ) { budget, connection ->
            SettingsUiState(
                year = yearMonth.first,
                month = yearMonth.second,
                budgetAmount = budget?.amount,
                connection = connection,
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

    /** 로그아웃 후 화면 이동은 인증 게이트(AppViewModel)가 auth 상태를 보고 처리한다. */
    fun onSignOut() {
        viewModelScope.launch { signOut() }
    }
}

private fun currentYearMonth(): Pair<Int, Int> = Calendar.getInstance().let {
    it.get(Calendar.YEAR) to it.get(Calendar.MONTH) + 1
}
