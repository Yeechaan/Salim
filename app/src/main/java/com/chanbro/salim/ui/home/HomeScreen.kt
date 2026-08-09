package com.chanbro.salim.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

data class CategorySpend(val name: String, val amount: String, val chartColor: Color, val dotColor: Color)

data class Transaction(val title: String, val subtitle: String, val amount: String)

data class HomeUiState(
    val month: String = "2026년 8월",
    val budgetSpent: String = "842,000원",
    val budgetTotal: String = "1,200,000원",
    val budgetRemainText: String = "남은 예산 358,000원",
    val budgetUsedRatio: Float = 0.70f,
    val mySpending: String = "412,000원",
    val partnerSpending: String = "430,000원",
    val categories: List<CategorySpend> = listOf(
        CategorySpend("식비", "312,000원", SalimTokens.CatFood, SalimTokens.CatFood),
        CategorySpend("문화/여가", "350,000원", SalimTokens.CatCulture, Color(0xFF8A5A44).copy(alpha = 0.4f)),
        CategorySpend("교통", "180,000원", SalimTokens.CatTransport, SalimTokens.CatTransport),
    ),
    val recent: List<Transaction> = listOf(
        Transaction("스타벅스", "식비 · 나 · 오늘", "-6,500원"),
        Transaction("지하철", "교통 · 배우자 · 어제", "-3,000원"),
        Transaction("영화관", "문화/여가 · 나 · 8/3", "-28,000원"),
    ),
)

// ---------------------------------------------------------------------------
// 타이포그래피 (Stitch 시안: 헤드라인 = 세리프(Literata 대체), 본문 = 산세리프)
// ---------------------------------------------------------------------------

private object HomeType {
    val headlineSm = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp)
    val headlineMd = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp)
    val titleLg = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp)
    val bodyLg = TextStyle(fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp)
    val bodyMd = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    val bodySm = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val labelMd = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.7.sp)
}

private val TextMuted = SalimTokens.TextPrimary.copy(alpha = 0.7f)

// ---------------------------------------------------------------------------
// 화면
// ---------------------------------------------------------------------------

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeUiState = HomeUiState(),
) {
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            MonthSelector(state.month)
            BudgetCard(state)
            SpendingSummaryRow(state)
            CategoryCard(state.categories)
            RecentTransactionsCard(state.recent)
        }
    }
}

@Composable
private fun HomeTopBar() {
    Surface(color = SalimTokens.Background, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("같이살림", style = HomeType.headlineSm, color = SalimTokens.TextPrimary)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SalimTokens.TextPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Person, contentDescription = "프로필", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun MonthSelector(month: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.ChevronLeft, contentDescription = "이전 달", tint = SalimTokens.TextPrimary)
        Text(month, style = HomeType.headlineMd, color = SalimTokens.TextPrimary)
        Icon(Icons.Filled.ChevronRight, contentDescription = "다음 달", tint = SalimTokens.TextPrimary)
    }
}

@Composable
private fun BudgetCard(state: HomeUiState) {
    SalimCard(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("이번 달 예산", style = HomeType.labelMd, color = TextMuted)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.budgetSpent, style = HomeType.headlineMd, color = SalimTokens.TextPrimary)
                Text("/ ${state.budgetTotal}", style = HomeType.bodyMd, color = TextMuted, modifier = Modifier.padding(bottom = 4.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(SalimTokens.ProgressTrack),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(state.budgetUsedRatio)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(SalimTokens.ProgressFill),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(state.budgetRemainText, style = HomeType.bodySm, color = TextMuted)
                    Text(
                        "${(state.budgetUsedRatio * 100).toInt()}% 사용",
                        style = HomeType.bodySm.copy(fontWeight = FontWeight.Medium),
                        color = SalimTokens.TextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpendingSummaryRow(state: HomeUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SpendingCard(
            modifier = Modifier.weight(1f),
            label = "나의 지출",
            amount = state.mySpending,
            dotColor = SalimTokens.TextPrimary.copy(alpha = 0.4f),
        )
        SpendingCard(
            modifier = Modifier.weight(1f),
            label = "배우자 지출",
            amount = state.partnerSpending,
            dotColor = Color(0xFF8A5A44).copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun SpendingCard(modifier: Modifier, label: String, amount: String, dotColor: Color) {
    SalimCard(modifier = modifier, cornerRadius = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                Text(label, style = HomeType.labelMd, color = TextMuted)
            }
            Text(amount, style = HomeType.titleLg, color = SalimTokens.TextPrimary)
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(cat.dotColor))
                                Text(cat.name, style = HomeType.bodyMd, color = TextMuted)
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
            val strokeWidth = size.minDimension * 0.20f
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
            // 세그먼트
            var startAngle = -90f
            categories.forEachIndexed { i, cat ->
                val sweep = values[i] / total * 360f
                drawArc(
                    color = cat.chartColor,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )
                startAngle += sweep
            }
        }
        // 가운데 흰 원(디테일)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SalimTokens.Background),
        )
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
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items.forEachIndexed { index, txn ->
                    TransactionRow(txn)
                    if (index != items.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SalimTokens.Divider.copy(alpha = 0.5f)),
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
            Text(txn.subtitle, style = HomeType.bodySm, color = TextMuted)
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
        BottomItem("설정", Icons.Filled.Settings, selected = false),
    )
    Surface(color = SalimTokens.Background, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val color = if (item.selected) SalimTokens.TextPrimary else SalimTokens.TextPrimary.copy(alpha = 0.5f)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = color, modifier = Modifier.size(24.dp))
                    Text(
                        item.label,
                        style = HomeType.labelMd.copy(fontWeight = if (item.selected) FontWeight.Bold else FontWeight.SemiBold),
                        color = color,
                    )
                }
            }
        }
    }
}

private data class BottomItem(val label: String, val icon: ImageVector, val selected: Boolean)

// ---------------------------------------------------------------------------
// 공용 카드
// ---------------------------------------------------------------------------

@Composable
private fun SalimCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = shape, clip = false)
            .clip(shape)
            .background(SalimTokens.CardSurface)
            .border(width = 1.dp, color = SalimTokens.CardBorder, shape = shape)
            .padding(16.dp),
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
