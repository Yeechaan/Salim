package com.chanbro.salim.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chanbro.salim.core.ui.theme.SalimTokens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ---------------------------------------------------------------------------
// 타이포그래피 (design.md: 헤드라인/금액 = 세리프(Gowun Batang 대체), 본문 = 산세리프(Gowun Dodum 대체))
// TODO: Gowun Batang / Gowun Dodum 다운로드블 폰트 연동 (지금은 fallback)
// ---------------------------------------------------------------------------

object SalimType {
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
// 공용 카드 (design.md: 흰 배경 + 라디우스 + 소프트 확산 그림자, 하드 보더 지양)
// ---------------------------------------------------------------------------

@Composable
fun SalimCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    contentPadding: Dp = 20.dp,
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
            .padding(contentPadding),
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// 상단 월 선택 (탭 시 월 피커 바텀시트) — 홈/가계부 공용
// ---------------------------------------------------------------------------

@Composable
fun MonthSelector(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = SalimType.display, color = SalimTokens.TextPrimary)
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = "월 선택",
            tint = SalimTokens.TextMuted,
            modifier = Modifier.size(28.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthPickerSheet(
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { pickerYear-- }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "이전 해", tint = SalimTokens.TextPrimary)
                }
                Text("${pickerYear}년", style = SalimType.titleLg, color = SalimTokens.TextPrimary)
                IconButton(onClick = { pickerYear++ }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "다음 해", tint = SalimTokens.TextPrimary)
                }
            }
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
            style = SalimType.bodyLg.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = if (selected) Color.White else SalimTokens.TextPrimary,
        )
    }
}

// ---------------------------------------------------------------------------
// 하단 탭 내비게이션 (전 화면 공통) — design.md 하단 탭바
// ---------------------------------------------------------------------------

enum class SalimTab(val label: String, val icon: ImageVector, val route: String) {
    Home("홈", Icons.Filled.Home, "home"),
    Expense("가계부", Icons.Filled.AccountBalanceWallet, "expense"),
    Schedule("일정", Icons.Filled.CalendarToday, "schedule"),
    DDay("디데이", Icons.Filled.CardGiftcard, "dday"),
    Settings("설정", Icons.Filled.Settings, "settings"),
}

@Composable
fun SalimBottomBar(selected: SalimTab, onSelect: (SalimTab) -> Unit) {
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
            SalimTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val color = if (isSelected) SalimTokens.Accent else SalimTokens.TextMuted
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(tab) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.size(24.dp))
                    Text(
                        tab.label,
                        style = SalimType.labelSm.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                        color = color,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 칩 (design.md: pill 형태. 선택 = Coral 배경+흰 텍스트, 미선택 = Primary Soft 배경+Coral 텍스트)
// 쓰임: 지출자/카테고리 선택, 일정 유형 필터 등
// ---------------------------------------------------------------------------

@Composable
fun SalimChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // 한 줄에 칩을 여러 개 균등 폭으로 깔 때(예산 빠른 금액 5개) 좁은 패딩/작은 글자로 줄여 쓸 수 있다.
    horizontalPadding: Dp = 18.dp,
    textStyle: TextStyle = SalimType.bodyMd,
    // 카테고리 칩처럼 모양으로 먼저 알아보게 할 때. 색은 글자와 같이 따라가서 칩 톤을 깨지 않는다.
    leadingIcon: ImageVector? = null,
) {
    val contentColor = if (selected) Color.White else SalimTokens.Accent
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) SalimTokens.Accent else SalimTokens.AccentSoft)
            .clickable(onClick = onClick)
            // 아이콘이 있으면 왼쪽 여백을 줄여 좌우가 시각적으로 같은 무게가 되게 한다.
            .padding(
                start = if (leadingIcon != null) horizontalPadding - 4.dp else horizontalPadding,
                end = horizontalPadding,
                top = 10.dp,
                bottom = 10.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        }
        Text(
            label,
            style = textStyle.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------
// D-day 배지 (design.md: D-day 배지 = Coral 배경 + 흰 굵은 텍스트)
// 쓰임: 홈 디데이 카드, 디데이 리스트
// ---------------------------------------------------------------------------

@Composable
fun DDayBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .widthIn(min = 56.dp)
            .clip(CircleShape)
            .background(SalimTokens.Accent)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = SalimType.labelMd.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}

// ---------------------------------------------------------------------------
// 입력 화면 공용 요소 (지출 입력 / 디데이 추가·수정 공유)
// ---------------------------------------------------------------------------

/** 카드 안 한 줄 입력 항목: 좌측 라벨 + 우측 현재 값. 탭하면 피커를 띄운다. */
@Composable
fun FieldRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = SalimType.bodyMd, color = SalimTokens.TextMuted)
        Text(value, style = SalimType.bodyLg, color = SalimTokens.TextPrimary)
    }
}

@Composable
fun FieldDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SalimTokens.Divider),
    )
}

/** 하단 고정 CTA 버튼 (design.md: Material3 Filled Button, 폭 100%) */
@Composable
fun SaveButton(enabled: Boolean, onClick: () -> Unit, label: String = "저장하기") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SalimTokens.Accent,
                contentColor = Color.White,
                disabledContainerColor = SalimTokens.ProgressTrack,
                disabledContentColor = SalimTokens.TextMuted,
            ),
        ) {
            Text(label, style = SalimType.bodyLg.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    initialMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let(onConfirm) ?: onDismiss() }) {
                Text("확인", color = SalimTokens.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = SalimTokens.TextMuted) }
        },
    ) {
        DatePicker(state = state)
    }
}

/** 기기 시간대 기준 이번 달 (연, 월 1~12). 월 단위 화면(홈·가계부·일정·설정 예산)의 시작 달. */
fun currentYearMonth(): Pair<Int, Int> = Calendar.getInstance().let {
    it.get(Calendar.YEAR) to it.get(Calendar.MONTH) + 1
}

/** UTC 자정 기준(Material3 DatePicker 규약)으로 오늘 날짜의 millis. */
fun todayUtcMillis(): Long {
    val local = Calendar.getInstance()
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

fun formatDate(utcMillis: Long): String =
    SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(utcMillis))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerModal(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("확인", color = SalimTokens.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = SalimTokens.TextMuted) }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        containerColor = SalimTokens.CardSurface,
    )
}

// ---------------------------------------------------------------------------
// 카테고리 아이콘 키 → 아이콘/색 (표시 전용 매핑) — 가계부 리스트, 홈 카테고리 차트, 카테고리 수정 공용
// 키는 categories 문서의 icon 필드(domain DefaultCategories). 이름을 바꿔도 모양은 유지된다.
// ---------------------------------------------------------------------------

fun categoryVisual(iconKey: String): Pair<ImageVector, Color> = when (iconKey) {
    "food" -> Icons.Filled.Restaurant to SalimTokens.CatFood
    "cafe" -> Icons.Filled.LocalCafe to SalimTokens.Sage
    "shopping" -> Icons.Filled.ShoppingBag to SalimTokens.Accent
    "culture" -> Icons.Filled.Movie to SalimTokens.CatCulture
    "travel" -> Icons.Filled.Flight to SalimTokens.Mint
    "transport" -> Icons.Filled.DirectionsBus to SalimTokens.CatTransport
    "living" -> Icons.Filled.CleaningServices to SalimTokens.Accent
    "health" -> Icons.Filled.LocalHospital to SalimTokens.Peach
    "housing" -> Icons.Filled.Home to SalimTokens.Sage
    "gift" -> Icons.Filled.CardGiftcard to SalimTokens.Lavender
    // "etc"와 사용자가 추가한 항목("custom")
    else -> Icons.Filled.Receipt to SalimTokens.Lavender
}

/** 카테고리 아이콘을 옅은 같은 색 배경 위에 올린 배지. 가계부 리스트와 카테고리 수정 목록에서 쓴다. */
@Composable
fun CategoryIconBadge(iconKey: String, modifier: Modifier = Modifier, size: Dp = 42.dp) {
    val (icon, color) = categoryVisual(iconKey)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.31f))
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.52f))
    }
}

// ---------------------------------------------------------------------------
// 칩 줄바꿈 배치 — 카테고리 이름 길이가 제각각이라 개수 기준(3개씩)으로 자르면 넘친다.
// (foundation FlowRow는 compose-foundation 버전 스큐 이슈가 있어 직접 배치한다)
// ---------------------------------------------------------------------------

@Composable
fun ChipFlowRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(childConstraints) }

        val rows = mutableListOf<MutableList<Placeable>>()
        var rowWidth = 0
        placeables.forEach { p ->
            val needed = if (rows.isEmpty() || rows.last().isEmpty()) p.width else rowWidth + gap + p.width
            if (rows.isEmpty() || needed > constraints.maxWidth) {
                rows += mutableListOf(p)
                rowWidth = p.width
            } else {
                rows.last() += p
                rowWidth = needed
            }
        }

        val height = rows.sumOf { row -> row.maxOf { it.height } } + gap * (rows.size - 1).coerceAtLeast(0)
        layout(constraints.maxWidth, height.coerceAtLeast(constraints.minHeight)) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                row.forEach { p ->
                    p.placeRelative(x, y)
                    x += p.width + gap
                }
                y += row.maxOf { it.height } + gap
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 금액 입력 천단위 콤마 VisualTransformation — 지출 입력 / 예산 설정 공용
// ---------------------------------------------------------------------------

val ThousandsTransformation = object : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = formatThousands(digits)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                var seen = 0
                for (i in formatted.indices) {
                    if (formatted[i] != ',') {
                        seen++
                        if (seen == offset) return i + 1
                    }
                }
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                var seen = 0
                var i = 0
                while (i < offset && i < formatted.length) {
                    if (formatted[i] != ',') seen++
                    i++
                }
                return seen
            }
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

// ---------------------------------------------------------------------------
// 유틸
// ---------------------------------------------------------------------------

fun parseAmount(text: String): Long =
    text.filter { it.isDigit() }.toLongOrNull() ?: 0L

/** 숫자 문자열(예: "12000")을 천단위 콤마 포맷("12,000")으로. 빈 값이면 "". */
fun formatThousands(digits: String): String {
    val n = digits.filter { it.isDigit() }.trimStart('0')
    if (n.isEmpty()) return ""
    return n.reversed().chunked(3).joinToString(",").reversed()
}

/**
 * 표시용 금액 "12,000원". 0이면 "0원".
 * [formatThousands]는 입력칸 placeholder 때문에 0을 ""로 돌려주므로, 금액 표시는 이 함수를 쓴다.
 */
fun formatWon(amount: Long): String = "${formatThousands(amount.toString()).ifEmpty { "0" }}원"
