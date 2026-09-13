package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.model.Spender
import com.chanbro.salim.domain.model.SpenderNames
import com.chanbro.salim.domain.repository.ConnectionRepository
import com.chanbro.salim.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * 지출자 표시 이름을 관찰. 프로필(내 이름)과 연결 상태(상대 이름)를 합친다.
 *
 * 지출자 칩·리스트 메타가 모두 이 값을 쓴다 — 한쪽만 이름이고 다른 쪽이 "배우자"면
 * 같은 항목이 화면마다 다르게 불린다.
 */
class ObserveSpenderNamesUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val connectionRepository: ConnectionRepository,
) {
    operator fun invoke(): Flow<SpenderNames> = combine(
        profileRepository.observe(),
        connectionRepository.observeConnection(),
    ) { profile, connection ->
        SpenderNames(
            mine = profile.nameOrDefault,
            partner = (connection as? Connection.Connected)?.partner?.displayName
                ?.takeIf { it.isNotBlank() }
                ?: Spender.PARTNER.label,
        )
    }
        // 이름은 곁다리 정보다. 프로필/커플 조회가 실패했다고 이 흐름을 쓰는 홈·가계부 목록이
        // 통째로 멈추면 손해가 더 크므로, 기본 표기로 떨어뜨리고 화면은 계속 그린다.
        .catch { emit(SpenderNames()) }
}
