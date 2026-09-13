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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.domain.model.Category
import com.chanbro.salim.domain.model.Spender
import com.chanbro.salim.domain.model.SpenderNames
import com.chanbro.salim.ui.common.ChipFlowRow
import com.chanbro.salim.ui.common.categoryVisual
import com.chanbro.salim.ui.common.DatePickerModal
import com.chanbro.salim.ui.common.FieldDivider
import com.chanbro.salim.ui.common.FieldRow
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.TimePickerModal
import com.chanbro.salim.ui.common.SalimChip
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.common.SaveButton
import com.chanbro.salim.ui.common.formatDate
import com.chanbro.salim.ui.common.ThousandsTransformation
import com.chanbro.salim.ui.common.todayUtcMillis
import java.util.Calendar

// ---------------------------------------------------------------------------
// 지출 입력 (expense.md 4-2) — 전체 화면 목적지
// ---------------------------------------------------------------------------

@Composable
fun ExpenseInputScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    onEditCategories: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseInputViewModel = hiltViewModel(),
) {
    // 미연결이면 모든 지출이 본인 것이라 지출자를 고를 이유가 없다. (expense.md 4-2)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var amountDigits by rememberSaveable { mutableStateOf("") }
    var spender by rememberSaveable { mutableStateOf(Spender.ME) }
    // 선택은 id로 들고 있는다 — 카테고리 수정에서 이름이나 고정/더보기 자리가 바뀌어도 선택이 유지된다.
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val category = state.categories.firstOrNull { it.id == categoryId }
        ?: state.fixedCategories.firstOrNull()
        ?: state.categories.first()
    var memo by rememberSaveable { mutableStateOf("") }
    var dateMillis by rememberSaveable { mutableLongStateOf(todayUtcMillis()) }
    var hour by rememberSaveable { mutableIntStateOf(nowHour()) }
    var minute by rememberSaveable { mutableIntStateOf(nowMinute()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showCategoryMore by rememberSaveable { mutableStateOf(false) }

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
                if (state.connected) {
                    SpenderField(
                        names = state.names,
                        selected = spender,
                        onSelect = { spender = it },
                    )
                    FieldDivider()
                }
                CategoryField(
                    fixedCategories = state.fixedCategories,
                    selected = category,
                    onSelect = { categoryId = it.id },
                    onMoreClick = { showCategoryMore = true },
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
                    // 칩을 숨긴 상태에서 이전 선택이 남아 있어도 본인으로 저장한다.
                    spender = if (state.connected) spender else Spender.ME,
                    category = category,
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
    if (showCategoryMore) {
        CategoryMoreSheet(
            moreCategories = state.moreCategories,
            selected = category,
            onDismiss = { showCategoryMore = false },
            onSelect = { categoryId = it.id; showCategoryMore = false },
            onEdit = { showCategoryMore = false; onEditCategories() },
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

/** 칩 라벨은 설정 > 프로필의 이름과 상대 이름을 쓴다. 이름이 없으면 "나"/"배우자". */
@Composable
private fun SpenderField(
    names: SpenderNames,
    selected: Spender,
    onSelect: (Spender) -> Unit,
) {
    Column(
        modifier = Modifier.padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("지출자", style = SalimType.bodyMd, color = SalimTokens.TextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spender.entries.forEach { option ->
                SalimChip(
                    label = names.labelOf(option),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

/**
 * 고정 칩 + "+더보기". 더보기에서 고른 항목은 "+더보기" 칩 자리에 "교통 ▾"처럼 선택 상태로 보여준다
 * — 칩 줄 수가 그대로고, 누르면 시트가 다시 열린다는 것도 ▾로 읽힌다. (expense.md 4-2)
 */
@Composable
private fun CategoryField(
    fixedCategories: List<Category>,
    selected: Category,
    onSelect: (Category) -> Unit,
    onMoreClick: () -> Unit,
) {
    val moreSelected = !selected.fixed
    Column(
        modifier = Modifier.padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.expense_category), style = SalimType.bodyMd, color = SalimTokens.TextMuted)
        // 이름 길이가 제각각이라 개수가 아니라 폭으로 줄을 바꾼다.
        ChipFlowRow {
            fixedCategories.forEach { cat ->
                SalimChip(
                    label = cat.name,
                    selected = cat.id == selected.id,
                    onClick = { onSelect(cat) },
                    leadingIcon = categoryVisual(cat.iconKey).first,
                )
            }
            SalimChip(
                // "+더보기"는 아이콘 없이, 더보기 항목이 골라져 있으면 그 항목의 아이콘을 붙인다.
                leadingIcon = if (moreSelected) categoryVisual(selected.iconKey).first else null,
                label = if (moreSelected) {
                    stringResource(R.string.expense_category_more_selected, selected.name)
                } else {
                    stringResource(R.string.expense_category_more)
                },
                selected = moreSelected,
                onClick = onMoreClick,
            )
        }
    }
}

/** 고정 칩에 없는 카테고리만 모아 보여준다. 고르면 바로 닫힌다. "편집"은 카테고리 수정 화면으로 간다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryMoreSheet(
    moreCategories: List<Category>,
    selected: Category,
    onDismiss: () -> Unit,
    onSelect: (Category) -> Unit,
    onEdit: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
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
                Text(
                    stringResource(R.string.expense_category_more_title),
                    style = SalimType.titleLg,
                    color = SalimTokens.TextPrimary,
                )
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.expense_category_edit), style = SalimType.bodyMd, color = SalimTokens.Accent)
                }
            }
            if (moreCategories.isEmpty()) {
                Text(
                    stringResource(R.string.expense_category_more_empty),
                    style = SalimType.bodyMd,
                    color = SalimTokens.TextMuted,
                )
            } else {
                ChipFlowRow {
                    moreCategories.forEach { cat ->
                        SalimChip(
                            label = cat.name,
                            selected = cat.id == selected.id,
                            onClick = { onSelect(cat) },
                            leadingIcon = categoryVisual(cat.iconKey).first,
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
        ExpenseInputScreen(onClose = {}, onSave = {}, onEditCategories = {})
    }
}
