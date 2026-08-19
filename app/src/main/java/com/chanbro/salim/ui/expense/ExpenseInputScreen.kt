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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimChip
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.common.formatThousands

private val spenders = listOf("나", "배우자")
private val quickCategories = listOf("식비", "카페", "교통", "문화/여가", "생활")

// ---------------------------------------------------------------------------
// 지출 입력 (expense.md 4-2) — 전체 화면 목적지
// ---------------------------------------------------------------------------

@Composable
fun ExpenseInputScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var amountDigits by rememberSaveable { mutableStateOf("") }
    var spender by rememberSaveable { mutableStateOf(spenders.first()) }
    var category by rememberSaveable { mutableStateOf(quickCategories.first()) }
    var memo by rememberSaveable { mutableStateOf("") }

    val canSave = amountDigits.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SalimTokens.Background),
    ) {
        InputTopBar(onClose)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AmountInput(
                digits = amountDigits,
                onDigitsChange = { amountDigits = it.filter { c -> c.isDigit() }.take(10) },
            )

            SalimCard(cornerRadius = 20.dp) {
                FieldRow(label = "날짜", value = "2026년 8월 19일", onClick = { /* TODO 날짜 선택 */ })
                FieldDivider()
                FieldRow(label = "시간", value = "오후 2:30", onClick = { /* TODO 시간 선택 */ })
                FieldDivider()
                ChipsField(
                    label = "지출자",
                    options = spenders,
                    selected = spender,
                    onSelect = { spender = it },
                )
                FieldDivider()
                CategoryField(
                    selected = category,
                    onSelect = { category = it },
                )
                FieldDivider()
                MemoField(memo = memo, onMemoChange = { memo = it })
            }
        }

        SaveButton(enabled = canSave, onClick = onSave)
    }
}

@Composable
private fun InputTopBar(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = SalimTokens.TextPrimary)
        }
        Text("지출 입력", style = SalimType.titleLg, color = SalimTokens.TextPrimary)
    }
}

@Composable
private fun AmountInput(digits: String, onDigitsChange: (String) -> Unit) {
    val amountStyle = SalimType.display.copy(fontSize = 40.sp, lineHeight = 48.sp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = digits,
            onValueChange = onDigitsChange,
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
}

@Composable
private fun FieldRow(label: String, value: String, onClick: () -> Unit) {
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
private fun ChipsField(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = SalimType.bodyMd, color = SalimTokens.TextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { opt ->
                SalimChip(label = opt, selected = opt == selected, onClick = { onSelect(opt) })
            }
        }
    }
}

@Composable
private fun CategoryField(selected: String, onSelect: (String) -> Unit) {
    val moreLabel = "+더보기"
    val chips = quickCategories + moreLabel
    Column(
        modifier = Modifier.padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("카테고리", style = SalimType.bodyMd, color = SalimTokens.TextMuted)
        // FlowRow는 compose-foundation 버전 스큐 이슈가 있어 수동 래핑(3개씩)으로 처리.
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            chips.chunked(3).forEach { rowChips ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowChips.forEach { cat ->
                        val isMore = cat == moreLabel
                        SalimChip(
                            label = cat,
                            selected = !isMore && cat == selected,
                            onClick = { if (!isMore) onSelect(cat) /* TODO 더보기: 전체 카테고리 */ },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoField(memo: String, onMemoChange: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("메모", style = SalimType.bodyMd, color = SalimTokens.TextMuted)
        BasicTextField(
            value = memo,
            onValueChange = onMemoChange,
            textStyle = SalimType.bodyLg.copy(color = SalimTokens.TextPrimary),
            singleLine = true,
            cursorBrush = SolidColor(SalimTokens.Accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (memo.isEmpty()) {
                    Text("메모 입력", style = SalimType.bodyLg, color = SalimTokens.TextMuted)
                }
                inner()
            },
        )
    }
}

@Composable
private fun FieldDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SalimTokens.Divider),
    )
}

@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
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
                contentColor = androidx.compose.ui.graphics.Color.White,
                disabledContainerColor = SalimTokens.ProgressTrack,
                disabledContentColor = SalimTokens.TextMuted,
            ),
        ) {
            Text("저장하기", style = SalimType.bodyLg.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
        }
    }
}

// ---------------------------------------------------------------------------
// 금액 입력 천단위 콤마 VisualTransformation
// ---------------------------------------------------------------------------

private val ThousandsTransformation = object : VisualTransformation {
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
// 프리뷰
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ExpenseInputScreenPreview() {
    SalimTheme {
        ExpenseInputScreen(onClose = {}, onSave = {})
    }
}
