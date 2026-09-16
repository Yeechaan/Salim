package com.chanbro.salim.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.model.Schedule
import com.chanbro.salim.domain.model.ScheduleType
import com.chanbro.salim.domain.model.SpenderNames
import com.chanbro.salim.domain.usecase.ObserveConnectionUseCase
import com.chanbro.salim.domain.usecase.ObserveMonthSchedulesUseCase
import com.chanbro.salim.domain.usecase.ObserveSpenderNamesUseCase
import com.chanbro.salim.ui.common.currentYearMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

// UI 표시용 모델.
data class ScheduleRowUi(
    val id: String,
    val title: String,
    val meta: String,          // "오후 2:00 · 우리 일정" / "종일 · 개인(해리)"
    val type: ScheduleType,
)

data class DayCellUi(
    val cell: CalendarCell,
    val types: List<ScheduleType>,  // 그 날짜에 있는 일정 유형(점 표시용, 중복 제거)
)

data class ScheduleUiState(
    val year: Int = 0,
    val month: Int = 0,
    val selectedDateMillis: Long = 0L,
    val activeFilters: Set<ScheduleType> = ScheduleType.entries.toSet(),
    val weeks: List<List<DayCellUi>> = emptyList(),
    val selectedDayHeader: String = "",
    val selectedDayRows: List<ScheduleRowUi> = emptyList(),
    /** 미연결이면 유형 필터 칩과 메타의 유형을 숨긴다 — 모든 일정이 내 개인 일정이다. (schedule.md 5-1) */
    val connected: Boolean = false,
    /** 유형 칩·메타에 쓸 이름. 가계부 지출자와 같은 규칙 — 프로필을 비워 두면 "나"/"배우자". */
    val names: SpenderNames = SpenderNames(),
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    observeMonth: ObserveMonthSchedulesUseCase,
    observeConnection: ObserveConnectionUseCase,
    observeSpenderNames: ObserveSpenderNamesUseCase,
) : ViewModel() {

    private val yearMonth = MutableStateFlow(currentYearMonth())
    private val selectedDate = MutableStateFlow(todayUtc())
    private val filters = MutableStateFlow(ScheduleType.entries.toSet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ScheduleUiState> = combine(
        yearMonth.flatMapLatest { (year, month) ->
            observeMonth(year, month).map { year to month to it }
        },
        selectedDate,
        filters,
        observeConnection(),
        observeSpenderNames(),
    ) { (ym, schedules), selected, activeFilters, connection, names ->
        toUiState(ym.first, ym.second, schedules, selected, activeFilters, connection is Connection.Connected, names)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = currentYearMonth().let { (y, m) ->
            ScheduleUiState(year = y, month = m, selectedDateMillis = todayUtc())
        },
    )

    fun setMonth(year: Int, month: Int) {
        yearMonth.value = year to month
        // 월을 바꾸면 그 달 1일을 선택 상태로 (선택 날짜가 화면 밖에 남지 않도록).
        selectedDate.value = startOfMonth(year, month)
    }

    fun selectDate(dateMillis: Long) {
        selectedDate.value = dateMillis
    }

    fun toggleFilter(type: ScheduleType) {
        val current = filters.value
        // 전부 끄면 아무것도 안 보이므로 마지막 하나는 끌 수 없게 둔다.
        filters.value = when {
            type !in current -> current + type
            current.size > 1 -> current - type
            else -> current
        }
    }

    private fun toUiState(
        year: Int,
        month: Int,
        schedules: List<Schedule>,
        selectedDateMillis: Long,
        activeFilters: Set<ScheduleType>,
        connected: Boolean,
        names: SpenderNames,
    ): ScheduleUiState {
        // 미연결이면 칩을 숨기므로 필터도 걸지 않는다 — 연결돼 있을 때 꺼 둔 유형이 되돌릴 방법 없이 숨는 일이 없게.
        val visible = if (connected) schedules.filter { it.type in activeFilters } else schedules
        val byDate = visible.groupBy { it.dateMillis }

        val weeks = monthWeeks(year, month).map { week ->
            week.map { cell ->
                DayCellUi(
                    cell = cell,
                    types = cell.dateMillis
                        ?.let { byDate[it] }
                        ?.map { it.type }
                        ?.distinct()
                        ?.sortedBy { it.ordinal }
                        .orEmpty(),
                )
            }
        }

        val rows = byDate[selectedDateMillis]
            .orEmpty()
            // 종일을 먼저, 그다음 시각순
            .sortedWith(compareBy({ it.minuteOfDay ?: -1 }, { it.createdAtMillis }))
            .map { it.toRowUi(connected, names) }

        return ScheduleUiState(
            year = year,
            month = month,
            selectedDateMillis = selectedDateMillis,
            activeFilters = activeFilters,
            weeks = weeks,
            selectedDayHeader = formatDayHeader(selectedDateMillis),
            selectedDayRows = rows,
            connected = connected,
            names = names,
        )
    }

    private fun Schedule.toRowUi(connected: Boolean, names: SpenderNames): ScheduleRowUi {
        val time = minuteOfDay?.let(::formatMinuteOfDay) ?: "종일"
        return ScheduleRowUi(
            id = id,
            title = title,
            // 미연결이면 유형을 붙이지 않는다 — 개인 경로 일정은 유형을 고르지 않고 기본값(우리 일정)으로 저장된다. (schedule.md 5-2)
            meta = if (connected) "$time · ${scheduleTypeMeta(type, names)}" else time,
            type = type,
        )
    }
}

/**
 * 유형 칩(캘린더 필터 / 등록 화면)의 짧은 라벨. 개인 일정은 설정 > 프로필 이름으로 부른다 —
 * 가계부 지출자·할 일 담당자와 같은 규칙. 리스트 메타는 [scheduleTypeMeta]. (schedule.md 5-1, 5-2)
 */
fun scheduleTypeLabel(type: ScheduleType, names: SpenderNames): String = when (type) {
    ScheduleType.SHARED -> "우리"
    ScheduleType.MINE -> names.mine
    ScheduleType.PARTNER -> names.partner
}

/** 리스트 메타용 전체 표기: "우리 일정" / "개인(해리)". */
fun scheduleTypeMeta(type: ScheduleType, names: SpenderNames): String = when (type) {
    ScheduleType.SHARED -> "우리 일정"
    else -> "개인(${scheduleTypeLabel(type, names)})"
}

/** 0~1439 → "오후 2:00". */
fun formatMinuteOfDay(minuteOfDay: Int): String {
    val hour24 = minuteOfDay / 60
    val minute = minuteOfDay % 60
    val ampm = if (hour24 < 12) "오전" else "오후"
    val h12 = (hour24 % 12).let { if (it == 0) 12 else it }
    return "$ampm $h12:${minute.toString().padStart(2, '0')}"
}

/** 오늘 날짜의 UTC 자정 millis. */
fun todayUtc(): Long {
    val local = Calendar.getInstance()
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

fun formatDayHeader(dateMillis: Long): String =
    SimpleDateFormat("M월 d일 (E)", Locale.KOREAN).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(dateMillis))
