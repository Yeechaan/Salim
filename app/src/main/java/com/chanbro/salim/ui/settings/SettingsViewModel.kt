package com.chanbro.salim.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Budget
import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.usecase.ObserveBudgetUseCase
import com.chanbro.salim.domain.usecase.ObserveConnectionUseCase
import com.chanbro.salim.domain.usecase.ObserveProfileUseCase
import com.chanbro.salim.domain.usecase.SaveBudgetUseCase
import com.chanbro.salim.domain.usecase.ObserveWidgetShowAmountUseCase
import com.chanbro.salim.domain.usecase.ObserveWeddingSettingsUseCase
import com.chanbro.salim.domain.usecase.SetWeddingEnabledUseCase
import com.chanbro.salim.domain.usecase.SetWidgetShowAmountUseCase
import com.chanbro.salim.domain.usecase.SignOutUseCase
import com.chanbro.salim.ui.common.currentYearMonth
import com.chanbro.salim.ui.common.formatWon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val year: Int = 0,
    val month: Int = 0,
    val budgetAmount: Long? = null,
    val connection: Connection = Connection.Unknown,
    val profileName: String? = null,
    /** 예산 위젯 금액 표시 (PRD 7/10-2). 기본 켜짐. */
    val widgetShowAmount: Boolean = true,
    /** 결혼 준비 (PRD 7/12). 연결 상태에서는 두 사람에게 함께 적용된다. */
    val weddingEnabled: Boolean = false,
) {
    /** 목록 우측에 노출할 현재 예산값. 미설정이면 안내 문구. (wireframe/settings.md 3.) */
    val budgetText: String
        get() = budgetAmount?.let(::formatWon) ?: "미설정"

    /** 프로필 줄 우측 값. 이름을 정했으면 그 이름이 가장 알아보기 쉽다. */
    val profileText: String
        get() = profileName?.takeIf { it.isNotBlank() } ?: "이름 · 생일 · 기념일"
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeBudget: ObserveBudgetUseCase,
    observeConnection: ObserveConnectionUseCase,
    observeProfile: ObserveProfileUseCase,
    observeWidgetShowAmount: ObserveWidgetShowAmountUseCase,
    observeWeddingSettings: ObserveWeddingSettingsUseCase,
    private val saveBudget: SaveBudgetUseCase,
    private val signOut: SignOutUseCase,
    private val setWidgetShowAmount: SetWidgetShowAmountUseCase,
    private val setWeddingEnabled: SetWeddingEnabledUseCase,
) : ViewModel() {

    // 설정의 "달별 예산"은 이번 달 기준으로 보여준다.
    private val yearMonth = currentYearMonth()

    val uiState: StateFlow<SettingsUiState> =
        combine(
            observeBudget(yearMonth.first, yearMonth.second),
            observeConnection(),
            observeProfile(),
            observeWidgetShowAmount(),
            observeWeddingSettings(),
        ) { budget, connection, profile, widgetShowAmount, wedding ->
            SettingsUiState(
                year = yearMonth.first,
                month = yearMonth.second,
                budgetAmount = budget?.amount,
                connection = connection,
                profileName = profile.displayName,
                widgetShowAmount = widgetShowAmount,
                weddingEnabled = wedding.enabled,
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

    /** 예산 위젯은 이 값을 구독하고 있어 저장만 하면 다시 그려진다 (WidgetUpdater). */
    fun onWidgetShowAmountChange(show: Boolean) {
        viewModelScope.launch { setWidgetShowAmount(show) }
    }

    /** 켜면 홈에 결혼 준비 카드가 생긴다. 처음 켤 때 체크리스트 기본 항목은 저장소가 채운다. 꺼도 데이터는 남는다. */
    fun onWeddingEnabledChange(enabled: Boolean) {
        viewModelScope.launch { runCatching { setWeddingEnabled(enabled) } }
    }

    /** 로그아웃 후 화면 이동은 인증 게이트(AppViewModel)가 auth 상태를 보고 처리한다. */
    fun onSignOut() {
        viewModelScope.launch { signOut() }
    }
}
