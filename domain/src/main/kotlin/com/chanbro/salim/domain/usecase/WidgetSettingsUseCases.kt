package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.repository.WidgetSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 예산 위젯 금액 표시 여부를 관찰. (PRD 10-2) */
class ObserveWidgetShowAmountUseCase @Inject constructor(
    private val repository: WidgetSettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.showAmount
}

/** 설정 > "위젯에 금액 표시" 토글. (PRD 7) */
class SetWidgetShowAmountUseCase @Inject constructor(
    private val repository: WidgetSettingsRepository,
) {
    suspend operator fun invoke(show: Boolean) = repository.setShowAmount(show)
}
