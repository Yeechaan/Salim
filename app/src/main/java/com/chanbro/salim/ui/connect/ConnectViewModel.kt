package com.chanbro.salim.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.usecase.ObserveConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 연결 상태만 보는 ViewModel. 연결 관리(9-1)·완료(9-5) 화면과
 * 홈 배너 / 설정 카드가 함께 쓴다. (PRD 9)
 */
@HiltViewModel
class ConnectViewModel @Inject constructor(
    observeConnection: ObserveConnectionUseCase,
) : ViewModel() {

    val connection: StateFlow<Connection> = observeConnection().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Connection.Unknown,
    )
}
