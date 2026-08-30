package com.chanbro.salim.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.usecase.GetOrCreateInviteUseCase
import com.chanbro.salim.domain.usecase.ObserveConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil

data class InviteCodeUiState(
    val loading: Boolean = true,
    val code: String? = null,
    /** 남은 시간(분). 0이면 1분 미만. */
    val remainingMinutes: Int = 0,
    val expired: Boolean = false,
    val failed: Boolean = false,
)

/** 내 초대 코드 화면. (wireframe/connect.md 9-2) */
@HiltViewModel
class InviteCodeViewModel @Inject constructor(
    private val getOrCreateInvite: GetOrCreateInviteUseCase,
    observeConnection: ObserveConnectionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteCodeUiState())
    val uiState: StateFlow<InviteCodeUiState> = _uiState.asStateFlow()

    /**
     * 상대가 코드를 수락하는 순간을 이 화면이 직접 본다. 서버 푸시가 없어도
     * 앱을 켜 둔 발급자는 조작 없이 완료 화면으로 넘어간다. (PRD 9 "1차 구현 범위")
     */
    val connected: StateFlow<Boolean> = observeConnection()
        .map { it is Connection.Connected }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var expiresAtMillis: Long = 0L

    init {
        load(forceNew = false)
        startTicker()
    }

    fun regenerate() = load(forceNew = true)

    private fun load(forceNew: Boolean) {
        _uiState.value = InviteCodeUiState(loading = true)
        viewModelScope.launch {
            runCatching { getOrCreateInvite(forceNew) }
                .onSuccess { invite ->
                    expiresAtMillis = invite.expiresAtMillis
                    _uiState.value = InviteCodeUiState(
                        loading = false,
                        code = invite.code,
                        remainingMinutes = remainingMinutes(),
                        expired = false,
                    )
                }
                .onFailure {
                    _uiState.value = InviteCodeUiState(loading = false, failed = true)
                }
        }
    }

    /**
     * 1초마다 재보되, 표시값이 바뀔 때만 상태를 갱신한다 — 30분짜리를 초 단위로
     * 깜빡이게 하면 재촉하는 인상만 남는다. (wireframe/connect.md 9-2)
     */
    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                val state = _uiState.value
                if (state.code == null) continue
                val minutes = remainingMinutes()
                val expired = expiresAtMillis <= System.currentTimeMillis()
                if (minutes != state.remainingMinutes || expired != state.expired) {
                    _uiState.value = state.copy(remainingMinutes = minutes, expired = expired)
                }
            }
        }
    }

    /** 올림으로 센다 — "1분 남음"이 떴는데 이미 죽어 있는 것보다 낫다. */
    private fun remainingMinutes(): Int {
        val remain = expiresAtMillis - System.currentTimeMillis()
        return if (remain <= 0) 0 else ceil(remain / 60_000.0).toInt()
    }
}
