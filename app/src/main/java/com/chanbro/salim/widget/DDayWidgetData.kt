package com.chanbro.salim.widget

import com.chanbro.salim.domain.usecase.ObserveDDaysUseCase
import com.chanbro.salim.domain.usecase.ObserveProfileUseCase
import com.chanbro.salim.ui.common.todayUtcMillis
import com.chanbro.salim.ui.dday.DDayRowUi
import com.chanbro.salim.ui.dday.toAutoDDays
import com.chanbro.salim.ui.dday.toRows
import com.chanbro.salim.ui.dday.upcoming
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 위젯에 띄울 가장 가까운 디데이 1건 (PRD 10-1). 없으면 null.
 * 디데이 관리와 같은 목록(직접 추가 + 프로필 생일/기념일)에서 홈 카드와 같은 기준으로 고른다.
 */
internal fun nearestDDay(
    observeDDays: ObserveDDaysUseCase,
    observeProfile: ObserveProfileUseCase,
): Flow<DDayRowUi?> = combine(observeDDays(), observeProfile()) { manual, profile ->
    (manual + profile.toAutoDDays()).toRows(todayUtcMillis()).upcoming().firstOrNull()
}

/** 당일(D-DAY)이면 위젯 전체를 강조한다 (widget.md 10-1 "당일"). */
internal val DDayRowUi.isToday: Boolean get() = dDayText == "D-DAY"
