package com.chanbro.salim.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 홈 화면 위젯 설정. (PRD 7 "위젯에 금액 표시", PRD 10-2)
 * 이 기기의 위젯에만 적용되는 값이라 상대와 공유하지 않고 기기 로컬에 둔다.
 */
interface WidgetSettingsRepository {
    /** 예산 위젯에 금액을 보여줄지. 기본 true. */
    val showAmount: Flow<Boolean>

    suspend fun setShowAmount(show: Boolean)
}
