package com.chanbro.salim.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.DDayBadge
import com.chanbro.salim.ui.common.MonthPickerSheet
import com.chanbro.salim.ui.common.MonthSelector
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.common.parseAmount

// ---------------------------------------------------------------------------
// 데이터 모델 (지금은 화면 상수. 이후 domain/data 모듈의 실제 데이터로 교체)
// ---------------------------------------------------------------------------

data class CategorySpend(val name: String, val amount: String, val color: Color)

data class DDayItem(val title: String, val date: String, val dDay: String)

data class Transaction(val title: String, val subtitle: String, val amount: String)

data class HomeUiState(
    val year: Int = 2026,
    val month: Int = 8,
    val budgetSpent: String = "842,000원",
    val budgetTotal: String = "1,200,000원",
    val budgetRemainText: String = "남은 예산 358,000원",
    val budgetUsedRatio: Float = 0.70f,
    val categories: List<CategorySpend> = listOf(
        CategorySpend("식비", "312,000원", SalimTokens.CatFood),
        CategorySpend("문화/여가", "350,000원", SalimTokens.CatCulture),
        CategorySpend("교통", "180,000원", SalimTokens.CatTransport),
    ),
    val dDays: List<DDayItem> = listOf(
        DDayItem("결혼기념일", "2026.08.31", "D-12"),
        DDayItem("제주 여행", "2026.09.15", "D-27"),
    ),
    val recent: List<Transaction> = listOf(
        Transaction("스타벅스", "식비 · 나 · 오늘", "-6,500원"),
        Transaction("지하철", "교통 · 배우자 · 어제", "-3,000원"),
        Transaction("영화관", "문화/여가 · 나 · 8/3", "-28,000원"),
    ),
)

// ---------------------------------------------------------------------------
// 화면 (하단 탭바는 상위 Scaffold가 제공, 여기서는 콘텐츠만)
// ---------------------------------------------------------------------------

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeUiState = HomeUiState(),
) {
    var year by rememberSaveable { mutableIntStateOf(state.year) }
    var month by rememberSaveable { mutableIntStateOf(state.month) }
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }

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
                label = "${year}년 ${month}월",
                onClick = { showMonthPicker = true },
            )
            BudgetCard(state)
            CategoryCard(state.categories)
            UpcomingDDayCard(state.dDays)
            RecentTransactionsCard(state.recent)
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
private fun BudgetCard(state: HomeUiState) {
    SalimCard(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("이번 달 예산", style = SalimType.labelMd, color = SalimTokens.TextMuted)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(state.budgetSpent, style = SalimType.headlineMd, color = SalimTokens.TextPrimary)
                Text(
                    "/ ${state.budgetTotal}",
                    style = SalimType.bodyMd,
                    color = SalimTokens.TextMuted,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
            }
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
                            .fillMaxWidth(state.budgetUsedRatio)
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
                    "${state.budgetRemainText} · ${(state.budgetUsedRatio * 100).toInt()}% 사용",
                    style = SalimType.bodySm,
                    color = SalimTokens.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(categories: List<CategorySpend>) {
    SalimCard(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("카테고리별 지출", style = SalimType.headlineSm, color = SalimTokens.TextPrimary)
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
                                Box(Modifier.size(10.dp).clip(CircleShape).background(cat.color))
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

@Composable
private fun DonutChart(categories: List<CategorySpend>) {
    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        val values = categories.map { parseAmount(it.amount) }
        val total = values.sum().coerceAtLeast(1L).toFloat()
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
                val sweep = values[i] / total * 360f
                drawArc(
                    color = cat.color,
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
private fun UpcomingDDayCard(dDays: List<DDayItem>) {
    SalimCard(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("다가오는 디데이", style = SalimType.headlineSm, color = SalimTokens.TextPrimary)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                dDays.take(2).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DDayBadge(item.dDay)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.title, style = SalimType.bodyLg, color = SalimTokens.TextPrimary)
                            Text(item.date, style = SalimType.bodySm, color = SalimTokens.TextMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsCard(items: List<Transaction>) {
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
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
private fun TransactionRow(txn: Transaction) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(txn.title, style = SalimType.bodyLg, color = SalimTokens.TextPrimary)
            Text(txn.subtitle, style = SalimType.bodySm, color = SalimTokens.TextMuted)
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
