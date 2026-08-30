package com.chanbro.salim.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.ConnectResult
import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.model.Invite
import com.chanbro.salim.domain.model.InviteCode
import com.chanbro.salim.domain.model.InviteLookup
import com.chanbro.salim.domain.usecase.ConnectWithInviteUseCase
import com.chanbro.salim.domain.usecase.LookupInviteUseCase
import com.chanbro.salim.domain.usecase.ObserveConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 실패 사유. 문구는 화면에서 strings.xml로 붙인다. (wireframe/connect.md 상태 분기 종합) */
enum class ConnectError {
    NOT_FOUND,
    EXPIRED,
    OWN_CODE,
    ALREADY_CONNECTED,
    PARTNER_CONNECTED,
    QR_INVALID,
    NETWORK,
}

data class CodeInputUiState(
    val code: String = "",
    val error: ConnectError? = null,
    val checking: Boolean = false,
    /** 값이 있으면 확인 시트(9-4)를 띄운다. */
    val pending: Invite? = null,
    val connecting: Boolean = false,
    val done: Boolean = false,
) {
    val canSubmit: Boolean get() = InviteCode.isComplete(code) && !checking && !connecting
}

/** 코드 입력 + 확인 시트. (wireframe/connect.md 9-3, 9-4) */
@HiltViewModel
class CodeInputViewModel @Inject constructor(
    private val lookupInvite: LookupInviteUseCase,
    private val connectWithInvite: ConnectWithInviteUseCase,
    observeConnection: ObserveConnectionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CodeInputUiState())
    val uiState: StateFlow<CodeInputUiState> = _uiState.asStateFlow()

    /**
     * Eagerly인 이유 — 화면이 구독하지 않고 submit()에서 `.value`만 읽는다.
     * WhileSubscribed면 상류가 아예 돌지 않아 값이 Unknown에 머문다.
     */
    private val connection: StateFlow<Connection> = observeConnection().stateIn(
        viewModelScope, SharingStarted.Eagerly, Connection.Unknown,
    )

    /** 딥링크로 들어온 코드를 1회 채운다. */
    fun prefill(rawCode: String?) {
        if (rawCode.isNullOrBlank() || _uiState.value.code.isNotEmpty()) return
        onCodeChange(rawCode)
    }

    fun onCodeChange(raw: String) {
        val normalized = InviteCode.normalize(raw).take(InviteCode.LENGTH)
        // 고치기 시작하면 이전 오류 문구는 치운다.
        _uiState.value = _uiState.value.copy(code = normalized, error = null)
    }

    /** QR 스캔 결과. 딥링크 형태와 코드 원문을 모두 받는다. */
    fun onScanned(raw: String?) {
        val code = parseInviteCode(raw)
        if (code == null) {
            _uiState.value = _uiState.value.copy(error = ConnectError.QR_INVALID)
            return
        }
        _uiState.value = _uiState.value.copy(code = code, error = null)
        submit()
    }

    fun onScanFailed() {
        _uiState.value = _uiState.value.copy(error = ConnectError.QR_INVALID)
    }

    fun submit() {
        val code = _uiState.value.code
        if (!InviteCode.isComplete(code)) return
        // 내가 이미 연결된 경우는 코드를 조회할 것도 없이 여기서 끊는다.
        if (connection.value is Connection.Connected) {
            _uiState.value = _uiState.value.copy(error = ConnectError.ALREADY_CONNECTED)
            return
        }
        _uiState.value = _uiState.value.copy(checking = true, error = null)
        viewModelScope.launch {
            val state = when (val result = lookupInvite(code)) {
                is InviteLookup.Found -> _uiState.value.copy(checking = false, pending = result.invite)
                InviteLookup.NotFound -> _uiState.value.copy(checking = false, error = ConnectError.NOT_FOUND)
                InviteLookup.Expired -> _uiState.value.copy(checking = false, error = ConnectError.EXPIRED)
                InviteLookup.OwnCode -> _uiState.value.copy(checking = false, error = ConnectError.OWN_CODE)
                InviteLookup.Failed -> _uiState.value.copy(checking = false, error = ConnectError.NETWORK)
            }
            _uiState.value = state
        }
    }

    fun dismissConfirm() {
        _uiState.value = _uiState.value.copy(pending = null)
    }

    fun confirm() {
        val invite = _uiState.value.pending ?: return
        _uiState.value = _uiState.value.copy(connecting = true)
        viewModelScope.launch {
            when (connectWithInvite(invite)) {
                ConnectResult.Success ->
                    _uiState.value = _uiState.value.copy(connecting = false, pending = null, done = true)
                ConnectResult.PartnerAlreadyConnected -> _uiState.value = _uiState.value.copy(
                    connecting = false,
                    pending = null,
                    code = "",
                    error = ConnectError.PARTNER_CONNECTED,
                )
                ConnectResult.Failed -> _uiState.value = _uiState.value.copy(
                    connecting = false,
                    pending = null,
                    error = ConnectError.NETWORK,
                )
            }
        }
    }
}

/** `salim://invite/CODE` 딥링크와 코드 원문을 모두 받아들인다. */
fun parseInviteCode(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val candidate = raw.substringAfterLast('/')
    return InviteCode.normalize(candidate).takeIf { InviteCode.isComplete(it) }
}
