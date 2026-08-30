package com.chanbro.salim.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.UserProfile
import com.chanbro.salim.domain.usecase.ObserveProfileUseCase
import com.chanbro.salim.domain.usecase.SaveProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeProfile: ObserveProfileUseCase,
    private val saveProfile: SaveProfileUseCase,
) : ViewModel() {

    /** null이면 아직 로딩 중 — 빈 값이 잠깐 보이는 것을 막는다. */
    val profile: StateFlow<UserProfile?> = observeProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun save(
        displayName: String?,
        birthdayMillis: Long?,
        anniversaryMillis: Long?,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            saveProfile(UserProfile(displayName, birthdayMillis, anniversaryMillis))
            onDone()
        }
    }
}
