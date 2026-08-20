package com.chanbro.salim.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.common.SaveButton
import com.chanbro.salim.ui.common.ThousandsTransformation
import com.chanbro.salim.ui.common.parseAmount

/**
 * 이번 달 예산 설정 (PRD 3. 홈 "이번 달 예산").
 * 예산 카드를 탭하면 열린다.
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
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = digits,
                    onValueChange = { digits = it.filter { c -> c.isDigit() }.take(10) },
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
            }

            SaveButton(
                enabled = digits.isNotEmpty(),
                onClick = { onConfirm(parseAmount(digits)) },
                label = "저장하기",
            )
        }
    }
}
