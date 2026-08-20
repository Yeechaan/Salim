package com.chanbro.salim.ui.expense

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.DatePickerModal
import com.chanbro.salim.ui.common.FieldDivider
import com.chanbro.salim.ui.common.FieldRow
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimChip
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.common.SaveButton
import com.chanbro.salim.ui.common.formatDate
import com.chanbro.salim.ui.common.ThousandsTransformation
import com.chanbro.salim.ui.common.todayUtcMillis
import java.util.Calendar

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
    viewModel: ExpenseInputViewModel = hiltViewModel(),
) {
    var amountDigits by rememberSaveable { mutableStateOf("") }
    var spender by rememberSaveable { mutableStateOf(spenders.first()) }
    var category by rememberSaveable { mutableStateOf(quickCategories.first()) }
    var memo by rememberSaveable { mutableStateOf("") }
    var dateMillis by rememberSaveable { mutableLongStateOf(todayUtcMillis()) }
    var hour by rememberSaveable { mutableIntStateOf(nowHour()) }
    var minute by rememberSaveable { mutableIntStateOf(nowMinute()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

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
                FieldRow(label = "날짜", value = formatDate(dateMillis), onClick = { showDatePicker = true })
                FieldDivider()
                FieldRow(label = "시간", value = formatTime(hour, minute), onClick = { showTimePicker = true })
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

        SaveButton(
            enabled = canSave,
            onClick = {
                viewModel.save(
                    amount = amountDigits.toLongOrNull() ?: 0L,
                    spenderLabel = spender,
                    categoryName = category,
                    memo = memo,
                    dateUtcMillis = dateMillis,
                    hour24 = hour,
                    minute = minute,
                    onDone = onSave,
                )
            },
        )
    }

    if (showDatePicker) {
        DatePickerModal(
            initialMillis = dateMillis,
            onConfirm = { dateMillis = it; showDatePicker = false },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showTimePicker) {
        TimePickerModal(
            initialHour = hour,
            initialMinute = minute,
            onConfirm = { h, m -> hour = h; minute = m; showTimePicker = false },
            onDismiss = { showTimePicker = false },
        )
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

// ---------------------------------------------------------------------------
// 날짜 / 시간 피커 (Material3)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerModal(
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

private fun nowHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
private fun nowMinute(): Int = Calendar.getInstance().get(Calendar.MINUTE)

private fun formatTime(hour24: Int, minute: Int): String {
    val ampm = if (hour24 < 12) "오전" else "오후"
    val h12 = (hour24 % 12).let { if (it == 0) 12 else it }
    return "$ampm $h12:${minute.toString().padStart(2, '0')}"
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
