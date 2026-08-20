package com.chanbro.salim.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.DDayBadge
import com.chanbro.salim.ui.common.MonthPickerSheet
import com.chanbro.salim.ui.common.MonthSelector
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.common.categoryVisual
import com.chanbro.salim.ui.dday.DDayListViewModel

// ---------------------------------------------------------------------------
// 화면 (하단 탭바는 상위 Scaffold가 제공, 여기서는 콘텐츠만)
// ---------------------------------------------------------------------------

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }
    var showBudgetSheet by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        HomeTopBar()
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
            BudgetCard(state = state, onClick = { showBudgetSheet = true })
            CategoryCard(state.categories)
            UpcomingDDayCard()
            RecentTransactionsCard(state.recent)
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
    if (showBudgetSheet) {
        BudgetInputSheet(
            year = state.year,
            month = state.month,
            initialAmount = state.budgetAmount,
            onDismiss = { showBudgetSheet = false },
            onConfirm = { amount ->
                viewModel.setBudget(amount)
                showBudgetSheet = false
            },
        )
    }
}

@Composable
private fun HomeTopBar() {
    Surface(color = SalimTokens.Background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("같이살림", style = SalimType.headlineSm, color = SalimTokens.TextPrimary)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SalimTokens.AccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Person, contentDescription = "프로필", tint = SalimTokens.Accent, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun BudgetCard(state: HomeUiState, onClick: () -> Unit) {
    SalimCard(cornerRadius = 24.dp, modifier = Modifier.clickable(onClick = onClick)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("이번 달 예산", style = SalimType.labelMd, color = SalimTokens.TextMuted)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(state.spentText, style = SalimType.headlineMd, color = SalimTokens.TextPrimary)
                val budgetText = state.budgetText
                if (budgetText != null) {
                    Text(
                        "/ $budgetText",
                        style = SalimType.bodyMd,
                        color = SalimTokens.TextMuted,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
            }
            val ratio = state.usedRatio
            if (ratio == null) {
                // 예산 미설정 — 카드를 탭해 설정하도록 안내 (PRD 3.)
                Text("예산을 설정해보세요", style = SalimType.bodySm, color = SalimTokens.Accent)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(SalimTokens.ProgressTrack),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(SalimTokens.ProgressFillStart, SalimTokens.ProgressFillEnd),
                                    ),
                                ),
                        )
                    }
                    Text(
                        "${state.remainText} · ${state.usedPercent}% 사용",
                        style = SalimType.bodySm,
                        color = SalimTokens.TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(categories: List<CategorySpendUi>) {
    SalimCard(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("카테고리별 지출", style = SalimType.headlineSm, color = SalimTokens.TextPrimary)
            if (categories.isEmpty()) {
                Text("이번 달 지출이 없어요", style = SalimType.bodyMd, color = SalimTokens.TextMuted)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    DonutChart(categories)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        categories.forEach { cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(categoryVisual(cat.name).second))
                                    Text(cat.name, style = SalimType.bodyMd, color = SalimTokens.TextMuted)
                                }
                                Text(cat.amount, style = SalimType.bodyMd, color = SalimTokens.TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChart(categories: List<CategorySpendUi>) {
    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        val colors = categories.map { categoryVisual(it.name).second }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.22f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = SalimTokens.ProgressTrack,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            var startAngle = -90f
            categories.forEachIndexed { i, cat ->
                val sweep = cat.ratio * 360f
                drawArc(
                    color = colors[i],
                    startAngle = startAngle + 1.5f,
                    sweepAngle = (sweep - 3f).coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )
                startAngle += sweep
            }
        }
    }
}

@Composable
private fun UpcomingDDayCard(viewModel: DDayListViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 가장 가까운 1~2건만 노출. 이미 지난 항목은 제외 (wireframe/home.md 4.)
    val upcoming = state.rows.filterNot { it.dDayText.startsWith("D+") }.take(2)

    SalimCard(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("다가오는 디데이", style = SalimType.headlineSm, color = SalimTokens.TextPrimary)
            if (upcoming.isEmpty()) {
                Text("디데이를 추가해보세요", style = SalimType.bodyMd, color = SalimTokens.TextMuted)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    upcoming.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DDayBadge(row.dDayText)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(row.title, style = SalimType.bodyLg, color = SalimTokens.TextPrimary)
                                Text(row.dateText, style = SalimType.bodySm, color = SalimTokens.TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsCard(items: List<TransactionUi>) {
    SalimCard(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("최근 지출 내역", style = SalimType.headlineSm, color = SalimTokens.TextPrimary)
                Text("전체보기", style = SalimType.labelMd, color = SalimTokens.Accent)
            }
            if (items.isEmpty()) {
                Text("아직 등록된 지출이 없어요", style = SalimType.bodyMd, color = SalimTokens.TextMuted)
            } else Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items.forEachIndexed { index, txn ->
                    TransactionRow(txn)
                    if (index != items.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SalimTokens.Divider),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(txn: TransactionUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(txn.title, style = SalimType.bodyLg, color = SalimTokens.TextPrimary)
            Text(txn.meta, style = SalimType.bodySm, color = SalimTokens.TextMuted)
        }
        Text(txn.amount, style = SalimType.bodyLg.copy(fontWeight = FontWeight.Medium), color = SalimTokens.TextPrimary)
    }
}

// ---------------------------------------------------------------------------
// 프리뷰
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    SalimTheme {
        HomeScreen(modifier = Modifier.background(SalimTokens.Background))
    }
}
