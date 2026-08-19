package com.chanbro.salim.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.MonthPickerSheet
import com.chanbro.salim.ui.common.MonthSelector
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimType

// ---------------------------------------------------------------------------
// 카테고리 → 아이콘/색 (표시 전용 매핑)
// ---------------------------------------------------------------------------

private fun categoryVisual(name: String): Pair<ImageVector, Color> = when (name) {
    "식비" -> Icons.Filled.Restaurant to SalimTokens.CatFood
    "카페" -> Icons.Filled.LocalCafe to SalimTokens.Sage
    "교통" -> Icons.Filled.DirectionsBus to SalimTokens.CatTransport
    "문화/여가" -> Icons.Filled.Movie to SalimTokens.CatCulture
    "생활" -> Icons.Filled.ShoppingBag to SalimTokens.Accent
    else -> Icons.Filled.Receipt to SalimTokens.Lavender
}

// ---------------------------------------------------------------------------
// 화면 (하단 탭바/FAB는 상위 Scaffold가 제공, 여기서는 콘텐츠만)
// ---------------------------------------------------------------------------

@Composable
fun ExpenseScreen(
    modifier: Modifier = Modifier,
    onItemClick: (ExpenseRowUi) -> Unit = {},
    viewModel: ExpenseListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        ExpenseTopBar()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            MonthSelector(
                label = "${state.year}년 ${state.month}월",
                onClick = { showMonthPicker = true },
            )
            MonthTotalCard(state.monthTotal)
            if (state.days.isEmpty()) {
                EmptyState()
            } else {
                state.days.forEach { day ->
                    DayGroup(day, onItemClick)
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerSheet(
            year = state.year,
            month = state.month,
            onDismiss = { showMonthPicker = false },
            onSelect = { selectedYear, selectedMonth ->
                viewModel.setMonth(selectedYear, selectedMonth)
                showMonthPicker = false
            },
        )
    }
}

@Composable
private fun ExpenseTopBar() {
    Surface(color = SalimTokens.Background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(start = 20.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("가계부", style = SalimType.headlineSm, color = SalimTokens.TextPrimary)
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
private fun DayGroup(day: ExpenseDayUi, onItemClick: (ExpenseRowUi) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            day.dateHeader,
            style = SalimType.labelMd,
            color = SalimTokens.TextMuted,
            modifier = Modifier.padding(start = 4.dp),
        )
        SalimCard(cornerRadius = 20.dp, contentPadding = 8.dp) {
            day.rows.forEachIndexed { index, row ->
                ExpenseRow(row, onClick = { onItemClick(row) })
                if (index != day.rows.lastIndex) {
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
private fun ExpenseRow(row: ExpenseRowUi, onClick: () -> Unit) {
    val (icon, color) = categoryVisual(row.categoryName)
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
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(row.title, style = SalimType.bodyLg, color = SalimTokens.TextPrimary)
            Text(row.meta, style = SalimType.bodySm, color = SalimTokens.TextMuted)
        }
        Text(
            row.amount,
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
