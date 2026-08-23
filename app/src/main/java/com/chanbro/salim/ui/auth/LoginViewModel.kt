package com.chanbro.salim.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.R
import com.chanbro.salim.domain.usecase.SignInWithGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val loading: Boolean = false,
    /** 노출할 에러 문구의 리소스 id. null이면 에러 없음. */
    val errorRes: Int? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInWithGoogle: SignInWithGoogleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** 시트를 띄우기 직전에 호출. 이전 에러를 지우고 로딩으로 전환한다. */
    fun onSignInStarted() {
        _uiState.value = LoginUiState(loading = true)
    }

    fun onSignInResult(result: GoogleSignInResult) {
        when (result) {
            is GoogleSignInResult.Success -> signIn(result.idToken)
            // 사용자가 스스로 닫은 경우라 안내 없이 원래 화면으로 되돌린다.
            GoogleSignInResult.Cancelled -> _uiState.value = LoginUiState()
            GoogleSignInResult.NoAccount ->
                _uiState.value = LoginUiState(errorRes = R.string.login_error_no_account)
            is GoogleSignInResult.Failure ->
                _uiState.value = LoginUiState(errorRes = R.string.login_error_generic)
        }
    }

    /**
     * 로그인 성공 시 별도 콜백을 쏘지 않는다 — 인증 게이트(AppViewModel)가
     * Firebase auth 상태를 관찰하고 있어 홈 이동은 거기서 일어난다.
     */
    private fun signIn(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, errorRes = null) }
            runCatching { signInWithGoogle(idToken) }
                .onFailure {
                    _uiState.value = LoginUiState(errorRes = R.string.login_error_generic)
                }
        }
    }
}
