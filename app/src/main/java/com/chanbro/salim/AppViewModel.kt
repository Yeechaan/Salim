package com.chanbro.salim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.usecase.CompleteOnboardingUseCase
import com.chanbro.salim.domain.usecase.ObserveAuthStateUseCase
import com.chanbro.salim.domain.usecase.ObserveOnboardingCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 앱 진입 시 어느 화면부터 보여줄지. (PRD 1. 온보딩 및 로그인)
 *
 * 로그인 여부가 확정되기 전에는 Loading이며, 이 동안에는 NavHost를 만들지 않아
 * 시작 목적지가 한 번만 정해지도록 한다(화면 깜빡임 방지).
 */
sealed interface AppUiState {
    data object Loading : AppUiState
    data object Onboarding : AppUiState
    data object Login : AppUiState
    data object Main : AppUiState
}

@HiltViewModel
class AppViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    observeOnboardingCompleted: ObserveOnboardingCompletedUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
) : ViewModel() {

    val uiState: StateFlow<AppUiState> =
        combine(observeAuthState(), observeOnboardingCompleted()) { user, onboarded ->
            when {
                user != null -> AppUiState.Main
                onboarded -> AppUiState.Login
                else -> AppUiState.Onboarding
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppUiState.Loading,
        )

    fun onOnboardingFinished() {
        viewModelScope.launch { completeOnboarding() }
    }
}
