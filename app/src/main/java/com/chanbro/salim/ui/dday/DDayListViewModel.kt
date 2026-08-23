package com.chanbro.salim.ui.dday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.DDay
import com.chanbro.salim.domain.model.DDaySource
import com.chanbro.salim.domain.model.UserProfile
import com.chanbro.salim.domain.usecase.ObserveDDaysUseCase
import com.chanbro.salim.domain.usecase.ObserveProfileUseCase
import com.chanbro.salim.ui.common.todayUtcMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// UI 표시용 모델 (도메인 → 화면 매핑 결과).
data class DDayRowUi(
    val id: String,
    val title: String,
    val dateText: String,   // 실제 대상 날짜 (반복이면 다음 기념일)
    val dDayText: String,   // D-11 / D-DAY / D+3
    val isAuto: Boolean,
    val repeatYearly: Boolean,
)

data class DDayListUiState(
    val rows: List<DDayRowUi> = emptyList(),
)

@HiltViewModel
class DDayListViewModel @Inject constructor(
    observeDDays: ObserveDDaysUseCase,
    observeProfile: ObserveProfileUseCase,
) : ViewModel() {

    val uiState: StateFlow<DDayListUiState> = combine(
        observeDDays(),
        observeProfile(),
    ) { manual, profile ->
        DDayListUiState(rows = (manual + profile.toAutoDDays()).toRows(todayUtcMillis()))
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DDayListUiState(),
        )
}

/**
 * 설정 > 프로필의 생일/기념일을 AUTO 디데이로 파생한다 (PRD 6.).
 * ddays 컬렉션에 쓰지 않으므로 프로필을 고치면 즉시 반영되고, 출처가 하나로 유지된다.
 * id는 프로필에서 온 항목임을 드러내는 합성 값 — 이 항목은 수정 화면에 진입하지 않는다.
 */
internal fun UserProfile.toAutoDDays(): List<DDay> = listOfNotNull(
    birthdayMillis?.let { autoDDay(AUTO_ID_BIRTHDAY, "생일", it) },
    anniversaryMillis?.let { autoDDay(AUTO_ID_ANNIVERSARY, "기념일", it) },
)

internal const val AUTO_ID_BIRTHDAY = "auto:birthday"
internal const val AUTO_ID_ANNIVERSARY = "auto:anniversary"

private fun autoDDay(id: String, title: String, dateMillis: Long) = DDay(
    id = id,
    title = title,
    dateMillis = dateMillis,
    repeatYearly = true,
    source = DDaySource.AUTO,
    createdAtMillis = 0L,
)

/**
 * 가까운 순 정렬 (PRD 6.). 다가올 날짜를 남은 일수 오름차순으로 먼저 놓고,
 * 이미 지난 1회성 항목은 최근에 지난 것부터 뒤에 붙인다.
 */
internal fun List<DDay>.toRows(todayMillis: Long): List<DDayRowUi> =
    map { dDay ->
        val target = nextOccurrence(dDay.dateMillis, todayMillis, dDay.repeatYearly)
        dDay to target
    }
        .sortedWith(
            compareBy(
                { daysUntil(it.second, todayMillis) < 0 },
                { kotlin.math.abs(daysUntil(it.second, todayMillis)) },
            ),
        )
        .map { (dDay, target) ->
            DDayRowUi(
                id = dDay.id,
                title = dDay.title,
                dateText = formatDotDate(target),
                dDayText = dDayLabel(daysUntil(target, todayMillis)),
                isAuto = dDay.source == DDaySource.AUTO,
                repeatYearly = dDay.repeatYearly,
            )
        }
