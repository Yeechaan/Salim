package com.chanbro.salim.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens

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
// 타이포그래피 (design.md: 헤드라인/금액 = 세리프(Gowun Batang 대체), 본문 = 산세리프(Gowun Dodum 대체))
// TODO: Gowun Batang / Gowun Dodum 다운로드블 폰트 연동 (지금은 fallback)
// ---------------------------------------------------------------------------

private object HomeType {
    val display = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp)
    val headlineMd = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp)
    val headlineSm = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp)
    val titleLg = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 28.sp)
    val bodyLg = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    val bodyMd = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    val bodySm = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    val labelMd = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.3.sp)
    val labelSm = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
}

// ---------------------------------------------------------------------------
// 화면
// ---------------------------------------------------------------------------

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeUiState = HomeUiState(),
) {
    var year by rememberSaveable { mutableIntStateOf(state.year) }
    var month by rememberSaveable { mutableIntStateOf(state.month) }
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SalimTokens.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { HomeTopBar() },
        bottomBar = { HomeBottomBar() },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
            Text("같이살림", style = HomeType.headlineSm, color = SalimTokens.TextPrimary)
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
private fun MonthSelector(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = HomeType.display, color = SalimTokens.TextPrimary)
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = "월 선택",
            tint = SalimTokens.TextMuted,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun BudgetCard(state: HomeUiState) {
    SalimCard(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("이번 달 예산", style = HomeType.labelMd, color = SalimTokens.TextMuted)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(state.budgetSpent, style = HomeType.headlineMd, color = SalimTokens.TextPrimary)
                Text(
                    "/ ${state.budgetTotal}",
                    style = HomeType.bodyMd,
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
                    style = HomeType.bodySm,
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
            Text("카테고리별 지출", style = HomeType.headlineSm, color = SalimTokens.TextPrimary)
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
                                Text(cat.name, style = HomeType.bodyMd, color = SalimTokens.TextMuted)
                            }
                            Text(cat.amount, style = HomeType.bodyMd, color = SalimTokens.TextPrimary)
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
            // 트랙(전체 원)
            drawArc(
                color = SalimTokens.ProgressTrack,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            // 세그먼트 (사이에 미세 간격)
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
            Text("다가오는 디데이", style = HomeType.headlineSm, color = SalimTokens.TextPrimary)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                dDays.take(2).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DDayBadge(item.dDay)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.title, style = HomeType.bodyLg, color = SalimTokens.TextPrimary)
                            Text(item.date, style = HomeType.bodySm, color = SalimTokens.TextMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DDayBadge(text: String) {
    Box(
        modifier = Modifier
            .widthIn(min = 56.dp)
            .clip(CircleShape)
            .background(SalimTokens.Accent)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = HomeType.labelMd.copy(fontWeight = FontWeight.Bold), color = Color.White)
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
                Text("최근 지출 내역", style = HomeType.headlineSm, color = SalimTokens.TextPrimary)
                Text("전체보기", style = HomeType.labelMd, color = SalimTokens.Accent)
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
            Text(txn.title, style = HomeType.bodyLg, color = SalimTokens.TextPrimary)
            Text(txn.subtitle, style = HomeType.bodySm, color = SalimTokens.TextMuted)
        }
        Text(txn.amount, style = HomeType.bodyLg.copy(fontWeight = FontWeight.Medium), color = SalimTokens.TextPrimary)
    }
}

@Composable
private fun HomeBottomBar() {
    val items = listOf(
        BottomItem("홈", Icons.Filled.Home, selected = true),
        BottomItem("가계부", Icons.Filled.AccountBalanceWallet, selected = false),
        BottomItem("일정", Icons.Filled.CalendarToday, selected = false),
        BottomItem("디데이", Icons.Filled.CardGiftcard, selected = false),
        BottomItem("설정", Icons.Filled.Settings, selected = false),
    )
    Surface(color = SalimTokens.CardSurface, shadowElevation = 12.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(66.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val color = if (item.selected) SalimTokens.Accent else SalimTokens.TextMuted
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = color, modifier = Modifier.size(24.dp))
                    Text(
                        item.label,
                        style = HomeType.labelSm.copy(fontWeight = if (item.selected) FontWeight.Bold else FontWeight.Medium),
                        color = color,
                    )
                }
            }
        }
    }
}

private data class BottomItem(val label: String, val icon: ImageVector, val selected: Boolean)

// ---------------------------------------------------------------------------
// 월 선택 바텀시트 (home.md 3-1: 상단 월 탭 시 다른 월로 이동)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthPickerSheet(
    year: Int,
    month: Int,
    onDismiss: () -> Unit,
    onSelect: (year: Int, month: Int) -> Unit,
) {
    var pickerYear by rememberSaveable { mutableIntStateOf(year) }
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SalimTokens.CardSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 연도 이동
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { pickerYear-- }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "이전 해", tint = SalimTokens.TextPrimary)
                }
                Text("${pickerYear}년", style = HomeType.titleLg, color = SalimTokens.TextPrimary)
                IconButton(onClick = { pickerYear++ }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "다음 해", tint = SalimTokens.TextPrimary)
                }
            }
            // 월 그리드 (3열 × 4행)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        for (col in 0 until 3) {
                            val m = row * 3 + col + 1
                            MonthCell(
                                label = "${m}월",
                                selected = pickerYear == year && m == month,
                                modifier = Modifier.weight(1f),
                                onClick = { onSelect(pickerYear, m) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCell(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) SalimTokens.Accent else SalimTokens.ProgressTrack)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = HomeType.bodyLg.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = if (selected) Color.White else SalimTokens.TextPrimary,
        )
    }
}

// ---------------------------------------------------------------------------
// 공용 카드 (design.md: 흰 배경 + 라디우스 + 소프트 확산 그림자, 하드 보더 지양)
// ---------------------------------------------------------------------------

@Composable
private fun SalimCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val shadowColor = Color(0xFF785A46) // 웜 브라운 계열 그림자
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = shadowColor.copy(alpha = 0.10f),
                spotColor = shadowColor.copy(alpha = 0.20f),
            )
            .clip(shape)
            .background(SalimTokens.CardSurface)
            .padding(20.dp),
    ) {
        content()
    }
}

private fun parseAmount(text: String): Long =
    text.filter { it.isDigit() }.toLongOrNull() ?: 0L

// ---------------------------------------------------------------------------
// 프리뷰
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    SalimTheme {
        HomeScreen()
    }
}
