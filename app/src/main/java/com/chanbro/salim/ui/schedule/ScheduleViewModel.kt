package com.chanbro.salim.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Schedule
import com.chanbro.salim.domain.model.ScheduleType
import com.chanbro.salim.domain.usecase.ObserveMonthSchedulesUseCase
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
    val meta: String,          // "오후 2:00 · 우리 일정" / "종일 · 개인(나)"
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
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    observeMonth: ObserveMonthSchedulesUseCase,
) : ViewModel() {

    private val yearMonth = MutableStateFlow(currentYearMonth())
    private val selectedDate = MutableStateFlow(todayUtc())
    private val filters = MutableStateFlow(ScheduleType.entries.toSet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ScheduleUiState> = yearMonth
        .flatMapLatest { (year, month) ->
            observeMonth(year, month).map { year to month to it }
        }
        .combine(selectedDate) { (ym, schedules), selected -> Triple(ym, schedules, selected) }
        .combine(filters) { (ym, schedules, selected), activeFilters ->
            toUiState(ym.first, ym.second, schedules, selected, activeFilters)
        }
        .stateIn(
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
    ): ScheduleUiState {
        val visible = schedules.filter { it.type in activeFilters }
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
            .map { it.toRowUi() }

        return ScheduleUiState(
            year = year,
            month = month,
            selectedDateMillis = selectedDateMillis,
            activeFilters = activeFilters,
            weeks = weeks,
            selectedDayHeader = formatDayHeader(selectedDateMillis),
            selectedDayRows = rows,
        )
    }

    private fun Schedule.toRowUi() = ScheduleRowUi(
        id = id,
        title = title,
        meta = "${minuteOfDay?.let(::formatMinuteOfDay) ?: "종일"} · ${type.label}",
        type = type,
    )

    private fun currentYearMonth(): Pair<Int, Int> = Calendar.getInstance().let {
        it.get(Calendar.YEAR) to it.get(Calendar.MONTH) + 1
    }
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
