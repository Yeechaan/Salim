package com.chanbro.salim.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.MonthPickerSheet
import com.chanbro.salim.ui.common.MonthSelector
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimType

// ---------------------------------------------------------------------------
// 데이터 모델 (지금은 화면 상수. 이후 domain/data 모듈의 실제 데이터로 교체)
// ---------------------------------------------------------------------------

data class ExpenseItem(
    val icon: ImageVector,
    val color: Color,
    val title: String,
    val meta: String, // 카테고리 · 지출자
    val amount: String,
)

data class ExpenseDayGroup(
    val dateHeader: String, // 예: "8월 5일 (화)"
    val items: List<ExpenseItem>,
)

data class ExpenseUiState(
    val year: Int = 2026,
    val month: Int = 8,
    val monthTotal: String = "1,124,000원",
    val groups: List<ExpenseDayGroup> = sampleGroups,
)

private val sampleGroups = listOf(
    ExpenseDayGroup(
        "8월 5일 (화)",
        listOf(
            ExpenseItem(Icons.Filled.Restaurant, SalimTokens.CatFood, "스타벅스", "식비 · 나", "-6,500원"),
            ExpenseItem(Icons.Filled.Restaurant, SalimTokens.CatFood, "이마트", "식비 · 배우자", "-84,000원"),
        ),
    ),
    ExpenseDayGroup(
        "8월 4일 (월)",
        listOf(
            ExpenseItem(Icons.Filled.DirectionsBus, SalimTokens.CatTransport, "지하철", "교통 · 배우자", "-3,000원"),
            ExpenseItem(Icons.Filled.Movie, SalimTokens.CatCulture, "CGV", "문화/여가 · 나", "-28,000원"),
        ),
    ),
)

// ---------------------------------------------------------------------------
// 화면 (하단 탭바/FAB는 상위 Scaffold가 제공, 여기서는 콘텐츠만)
// ---------------------------------------------------------------------------

@Composable
fun ExpenseScreen(
    modifier: Modifier = Modifier,
    state: ExpenseUiState = ExpenseUiState(),
    onItemClick: (ExpenseItem) -> Unit = {},
) {
    var year by rememberSaveable { mutableIntStateOf(state.year) }
    var month by rememberSaveable { mutableIntStateOf(state.month) }
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        ExpenseTopBar(
            monthLabel = "${year}년 ${month}월",
            onMonthClick = { showMonthPicker = true },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            MonthTotalCard(state.monthTotal)
            if (state.groups.isEmpty()) {
                EmptyState()
            } else {
                state.groups.forEach { group ->
                    DayGroup(group, onItemClick)
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerSheet(
            year = year,
            month = month,
            onDismiss = { showMonthPicker = false },
            onSelect = { selectedYear, selectedMonth ->
                year = selectedYear
                month = selectedMonth
                showMonthPicker = false
            },
        )
    }
}

@Composable
private fun ExpenseTopBar(monthLabel: String, onMonthClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 12.dp, top = 4.dp)
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonthSelector(label = monthLabel, onClick = onMonthClick)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { /* TODO: 검색 패널 */ }) {
                Icon(Icons.Filled.Search, contentDescription = "검색", tint = SalimTokens.TextPrimary)
            }
            IconButton(onClick = { /* TODO: 필터 패널 */ }) {
                Icon(Icons.Filled.Tune, contentDescription = "필터", tint = SalimTokens.TextPrimary)
            }
        }
    }
}

@Composable
private fun MonthTotalCard(total: String) {
    SalimCard(cornerRadius = 18.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("이번 달 총 지출", style = SalimType.labelMd, color = SalimTokens.TextMuted)
            Text(total, style = SalimType.headlineMd, color = SalimTokens.TextPrimary)
        }
    }
}

@Composable
private fun DayGroup(group: ExpenseDayGroup, onItemClick: (ExpenseItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            group.dateHeader,
            style = SalimType.labelMd,
            color = SalimTokens.TextMuted,
            modifier = Modifier.padding(start = 4.dp),
        )
        SalimCard(cornerRadius = 20.dp, contentPadding = 8.dp) {
            group.items.forEachIndexed { index, item ->
                ExpenseRow(item, onClick = { onItemClick(item) })
                if (index != group.items.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .height(1.dp)
                            .background(SalimTokens.Divider),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(item: ExpenseItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(item.color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(22.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(item.title, style = SalimType.bodyLg, color = SalimTokens.TextPrimary)
            Text(item.meta, style = SalimType.bodySm, color = SalimTokens.TextMuted)
        }
        Text(
            item.amount,
            style = SalimType.bodyLg.copy(fontWeight = FontWeight.Medium),
            color = SalimTokens.TextPrimary,
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "이번 달 지출이 없어요",
            style = SalimType.bodyMd,
            color = SalimTokens.TextMuted,
            textAlign = TextAlign.Center,
        )
    }
}

// ---------------------------------------------------------------------------
// 프리뷰
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ExpenseScreenPreview() {
    SalimTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(SalimTokens.Background)
                .padding(PaddingValues(bottom = 66.dp)),
        ) {
            ExpenseScreen()
        }
    }
}
