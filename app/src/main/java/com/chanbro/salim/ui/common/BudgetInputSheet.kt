package com.chanbro.salim.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chanbro.salim.core.ui.theme.SalimTokens

/** 빠른 금액 버튼 프리셋 (만원 단위). 탭할 때마다 현재 입력값에 누적된다. */
private val quickAmounts = listOf(
    "+1만" to 10_000L,
    "+5만" to 50_000L,
    "+10만" to 100_000L,
    "+50만" to 500_000L,
    "+100만" to 1_000_000L,
)

/** 직접 입력과 동일하게 10자리로 제한 (아래 take(10)과 맞춤) */
private const val MAX_AMOUNT = 9_999_999_999L

/**
 * 월 예산 설정 바텀시트 — 홈 예산 카드 / 설정 > 달별 예산 공용 (PRD 3. 홈, 7. 설정).
 * 키패드 직접 입력 + 만원 단위 빠른 금액 버튼(누적)을 함께 제공한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetInputSheet(
    year: Int,
    month: Int,
    initialAmount: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var digits by rememberSaveable {
        mutableStateOf(initialAmount?.takeIf { it > 0 }?.toString().orEmpty())
    }
    val sheetState = rememberModalBottomSheetState()
    val amountStyle = SalimType.display.copy(fontSize = 32.sp, lineHeight = 40.sp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SalimTokens.CardSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("${year}년 ${month}월 예산", style = SalimType.titleLg, color = SalimTokens.TextPrimary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = digits,
                    onValueChange = { digits = it.filter { c -> c.isDigit() }.take(10) },
                    modifier = Modifier.weight(1f),
                    textStyle = amountStyle.copy(color = SalimTokens.TextPrimary, textAlign = TextAlign.End),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(SalimTokens.Accent),
                    visualTransformation = ThousandsTransformation,
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (digits.isEmpty()) {
                                Text("0", style = amountStyle, color = SalimTokens.TextMuted)
                            }
                            inner()
                        }
                    },
                )
                Text("원", style = amountStyle, color = SalimTokens.TextPrimary)

                // 지우기 — 입력값이 있을 때만 노출. 자리는 항상 차지해 금액이 흔들리지 않게 한다.
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (digits.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SalimTokens.ProgressTrack)
                                .clickable { digits = "" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "금액 지우기",
                                tint = SalimTokens.TextMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            // 빠른 금액 칩 — 5개를 한 줄에 균등 폭으로 채운다.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                quickAmounts.forEach { (label, delta) ->
                    SalimChip(
                        label = label,
                        selected = false,
                        onClick = { digits = (parseAmount(digits) + delta).coerceAtMost(MAX_AMOUNT).toString() },
                        modifier = Modifier.weight(1f),
                        horizontalPadding = 4.dp,
                        textStyle = SalimType.labelMd,
                    )
                }
            }

            SaveButton(
                enabled = digits.isNotEmpty(),
                onClick = { onConfirm(parseAmount(digits)) },
                label = "저장하기",
            )
        }
    }
}
