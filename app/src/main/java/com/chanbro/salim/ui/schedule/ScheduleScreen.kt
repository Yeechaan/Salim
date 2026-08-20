package com.chanbro.salim.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.domain.model.ScheduleType
import com.chanbro.salim.ui.common.MonthPickerSheet
import com.chanbro.salim.ui.common.MonthSelector
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimChip
import com.chanbro.salim.ui.common.SalimType

// ---------------------------------------------------------------------------
// 일정 유형 → 색 (캘린더 점, 리스트 좌측 색상 바)
// ---------------------------------------------------------------------------

fun scheduleColor(type: ScheduleType): Color = when (type) {
    ScheduleType.SHARED -> SalimTokens.Accent
    ScheduleType.MINE -> SalimTokens.Sage
    ScheduleType.PARTNER -> SalimTokens.Lavender
}

/** 필터 칩용 짧은 라벨. 리스트 메타에는 type.label(전체 표기)을 쓴다. */
private fun shortLabel(type: ScheduleType): String = when (type) {
    ScheduleType.SHARED -> "우리"
    ScheduleType.MINE -> "나"
    ScheduleType.PARTNER -> "배우자"
}

private val WEEKDAYS = listOf("일", "월", "화", "수", "목", "금", "토")

// ---------------------------------------------------------------------------
// 일정 캘린더 (schedule.md 5-1) — 탭 랜딩 화면
// 하단 탭바/FAB는 상위 Scaffold가 제공, 여기서는 콘텐츠만
// ---------------------------------------------------------------------------

@Composable
fun ScheduleScreen(
    modifier: Modifier = Modifier,
    onItemClick: (ScheduleRowUi) -> Unit = {},
    onSelectedDateChange: (Long) -> Unit = {},
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // FAB(상위 Scaffold 소유)가 등록 화면에 넘길 기본 날짜를 알 수 있도록 올려보낸다.
    LaunchedEffect(state.selectedDateMillis) { onSelectedDateChange(state.selectedDateMillis) }
    ScheduleContent(
        state = state,
        modifier = modifier,
        onItemClick = onItemClick,
        onSelectMonth = viewModel::setMonth,
        onSelectDate = viewModel::selectDate,
        onToggleFilter = viewModel::toggleFilter,
    )
}

@Composable
private fun ScheduleContent(
    state: ScheduleUiState,
    modifier: Modifier = Modifier,
    onItemClick: (ScheduleRowUi) -> Unit,
    onSelectMonth: (Int, Int) -> Unit,
    onSelectDate: (Long) -> Unit,
    onToggleFilter: (ScheduleType) -> Unit,
) {
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        ScheduleTopBar()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MonthSelector(
                label = "${state.year}년 ${state.month}월",
                onClick = { showMonthPicker = true },
            )
            FilterChips(active = state.activeFilters, onToggle = onToggleFilter)
            CalendarCard(
                weeks = state.weeks,
                selectedDateMillis = state.selectedDateMillis,
                onSelectDate = onSelectDate,
            )
            DayScheduleCard(
                header = state.selectedDayHeader,
                rows = state.selectedDayRows,
                onItemClick = onItemClick,
            )
        }
    }

    if (showMonthPicker) {
        MonthPickerSheet(
            year = state.year,
            month = state.month,
            onDismiss = { showMonthPicker = false },
            onSelect = { year, month ->
                onSelectMonth(year, month)
                showMonthPicker = false
            },
        )
    }
}

@Composable
private fun ScheduleTopBar() {
    Surface(color = SalimTokens.Background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("일정", style = SalimType.headlineSm, color = SalimTokens.TextPrimary)
        }
    }
}

/** 유형 필터 (복수 선택). 전부 끄면 빈 화면이라 마지막 하나는 꺼지지 않는다. */
@Composable
private fun FilterChips(active: Set<ScheduleType>, onToggle: (ScheduleType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ScheduleType.entries.forEach { type ->
            SalimChip(
                label = shortLabel(type),
                selected = type in active,
                onClick = { onToggle(type) },
            )
        }
    }
}

@Composable
private fun CalendarCard(
    weeks: List<List<DayCellUi>>,
    selectedDateMillis: Long,
    onSelectDate: (Long) -> Unit,
) {
    SalimCard(cornerRadius = 24.dp, contentPadding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                WEEKDAYS.forEach { label ->
                    Text(
                        label,
                        style = SalimType.labelSm,
                        color = SalimTokens.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        DayCell(
                            day = day,
                            selected = day.cell.dateMillis == selectedDateMillis,
                            onClick = { day.cell.dateMillis?.let(onSelectDate) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: DayCellUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayOfMonth = day.cell.dayOfMonth
    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .then(if (dayOfMonth != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (dayOfMonth == null) return@Box  // 격자 채움용 빈 칸

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (selected) SalimTokens.Accent else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    dayOfMonth.toString(),
                    style = SalimType.bodyMd.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = if (selected) Color.White else SalimTokens.TextPrimary,
                )
            }
            // 일정 유형별 점 (최대 3개)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                day.types.take(3).forEach { type ->
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(scheduleColor(type)),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayScheduleCard(
    header: String,
    rows: List<ScheduleRowUi>,
    onItemClick: (ScheduleRowUi) -> Unit,
) {
    SalimCard(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(header, style = SalimType.headlineSm, color = SalimTokens.TextPrimary)
            if (rows.isEmpty()) {
                Text("등록된 일정이 없어요", style = SalimType.bodyMd, color = SalimTokens.TextMuted)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    rows.forEach { row ->
                        ScheduleRow(row = row, onClick = { onItemClick(row) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(row: ScheduleRowUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // design.md 리스트 아이템: 좌측 색상 바 있는 버전(일정)
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(scheduleColor(row.type)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.title, style = SalimType.bodyLg, color = SalimTokens.TextPrimary)
            Text(row.meta, style = SalimType.bodySm, color = SalimTokens.TextMuted)
        }
    }
}

// ---------------------------------------------------------------------------
// 프리뷰 (ViewModel 없이 Content만)
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ScheduleScreenPreview() {
    val year = 2026
    val month = 8
    val selected = startOfMonth(year, month) + 4 * DAY_MILLIS
    SalimTheme {
        ScheduleContent(
            state = ScheduleUiState(
                year = year,
                month = month,
                selectedDateMillis = selected,
                weeks = monthWeeks(year, month).map { week ->
                    week.map { cell ->
                        DayCellUi(
                            cell = cell,
                            types = when (cell.dayOfMonth) {
                                5 -> listOf(ScheduleType.SHARED, ScheduleType.MINE)
                                12 -> listOf(ScheduleType.PARTNER)
                                else -> emptyList()
                            },
                        )
                    }
                },
                selectedDayHeader = "8월 5일 (수)",
                selectedDayRows = listOf(
                    ScheduleRowUi("1", "저녁 약속", "오후 7:00 · 우리 일정", ScheduleType.SHARED),
                    ScheduleRowUi("2", "치과", "종일 · 개인(나)", ScheduleType.MINE),
                ),
            ),
            onItemClick = {},
            onSelectMonth = { _, _ -> },
            onSelectDate = {},
            onToggleFilter = {},
        )
    }
}
