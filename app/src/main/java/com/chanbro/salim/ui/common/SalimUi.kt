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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chanbro.salim.core.ui.theme.SalimTokens

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

enum class SalimTab(val label: String, val icon: ImageVector) {
    Home("홈", Icons.Filled.Home),
    Expense("가계부", Icons.Filled.AccountBalanceWallet),
    Schedule("일정", Icons.Filled.CalendarToday),
    DDay("디데이", Icons.Filled.CardGiftcard),
    Settings("설정", Icons.Filled.Settings),
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
// 유틸
// ---------------------------------------------------------------------------

fun parseAmount(text: String): Long =
    text.filter { it.isDigit() }.toLongOrNull() ?: 0L
