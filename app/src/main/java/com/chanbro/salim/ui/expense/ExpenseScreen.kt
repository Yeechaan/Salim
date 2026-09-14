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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.domain.model.ExpenseFilter
import com.chanbro.salim.domain.model.Spender
import com.chanbro.salim.ui.common.CategoryChip
import com.chanbro.salim.ui.common.CategoryIconBadge
import com.chanbro.salim.ui.common.ChipFlowRow
import com.chanbro.salim.ui.common.DatePickerModal
import com.chanbro.salim.ui.common.FieldDivider
import com.chanbro.salim.ui.common.FieldRow
import com.chanbro.salim.ui.common.MonthPickerSheet
import com.chanbro.salim.ui.common.MonthSelector
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimChip
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.common.SaveButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ---------------------------------------------------------------------------
// 화면 (하단 탭바/FAB는 상위 Scaffold가 제공, 여기서는 콘텐츠만)
// ---------------------------------------------------------------------------

@Composable
fun ExpenseScreen(
    modifier: Modifier = Modifier,
    onItemClick: (ExpenseRowUi) -> Unit = {},
    viewModel: ExpenseListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }
    // 수정 화면에 다녀와도 검색어가 남아 있으면 입력창을 연 채로 둔다.
    var searchOpen by rememberSaveable { mutableStateOf(state.filter.query.isNotEmpty()) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        ExpenseTopBar(
            searchOpen = searchOpen,
            query = state.filter.query,
            filterActive = state.filter.hasConditions,
            onQueryChange = viewModel::setQuery,
            onSearchOpen = { searchOpen = true },
            onSearchClose = {
                viewModel.setQuery("")
                searchOpen = false
            },
            onFilterClick = { showFilterSheet = true },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            MonthSelector(
                label = stringResource(R.string.expense_month_label, state.year, state.month),
                onClick = { showMonthPicker = true },
            )
            if (state.filter.hasConditions) {
                ConditionChips(state, viewModel)
            }
            TotalCard(
                label = if (state.filter.isActive) {
                    stringResource(R.string.expense_filtered_total, state.resultCount)
                } else {
                    stringResource(R.string.expense_month_total)
                },
                total = state.monthTotal,
            )
            when {
                !state.monthHasExpenses -> EmptyState(stringResource(R.string.expense_empty_month))
                state.days.isEmpty() -> EmptyState(
                    text = stringResource(R.string.expense_empty_filtered),
                    actionLabel = stringResource(R.string.expense_filter_reset_all),
                    onAction = {
                        viewModel.clearAll()
                        searchOpen = false
                    },
                )
                else -> state.days.forEach { day ->
                    DayGroup(day, onItemClick)
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerSheet(
            year = state.year,
            month = state.month,
            onDismiss = { showMonthPicker = false },
            onSelect = { selectedYear, selectedMonth ->
                viewModel.setMonth(selectedYear, selectedMonth)
                showMonthPicker = false
            },
        )
    }

    if (showFilterSheet) {
        ExpenseFilterSheet(
            state = state,
            onDismiss = { showFilterSheet = false },
            onApply = {
                viewModel.applyConditions(it)
                showFilterSheet = false
            },
        )
    }
}

@Composable
private fun ExpenseTopBar(
    searchOpen: Boolean,
    query: String,
    filterActive: Boolean,
    onQueryChange: (String) -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onFilterClick: () -> Unit,
) {
    Surface(color = SalimTokens.Background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(start = 20.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (searchOpen) {
                SearchField(query, onQueryChange, Modifier.weight(1f))
                IconButton(onClick = onSearchClose) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.expense_search_close),
                        tint = SalimTokens.TextPrimary,
                    )
                }
            } else {
                Text(
                    stringResource(R.string.expense_title),
                    style = SalimType.headlineSm,
                    color = SalimTokens.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSearchOpen) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = stringResource(R.string.expense_search),
                        tint = SalimTokens.TextPrimary,
                    )
                }
            }
            IconButton(onClick = onFilterClick) {
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = stringResource(R.string.expense_filter),
                    // 조건이 걸려 있으면 Coral로 표시한다. (expense.md 4-1 필터)
                    tint = if (filterActive) SalimTokens.Accent else SalimTokens.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        textStyle = SalimType.bodyLg.copy(color = SalimTokens.TextPrimary),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        // 입력 즉시 걸러지므로 검색 키는 키보드만 내린다.
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        cursorBrush = SolidColor(SalimTokens.Accent),
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(SalimTokens.CardSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .focusRequester(focusRequester),
        decorationBox = { inner ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = SalimTokens.TextMuted)
                Box {
                    if (query.isEmpty()) {
                        Text(stringResource(R.string.expense_search_hint), style = SalimType.bodyLg, color = SalimTokens.TextMuted)
                    }
                    inner()
                }
            }
        },
    )
}

/** 적용 중인 기간·카테고리·지출자 조건. 탭하면 그 조건만 풀린다. (design.md 칩 - 조건 칩) */
@Composable
private fun ConditionChips(state: ExpenseListUiState, viewModel: ExpenseListViewModel) {
    val filter = state.filter
    ChipFlowRow(spacing = 8.dp) {
        if (filter.hasPeriod) {
            SalimChip(
                label = periodLabel(filter.startDayUtc, filter.endDayUtc),
                selected = true,
                onClick = viewModel::clearPeriod,
                trailingIcon = Icons.Filled.Close,
            )
        }
        state.selectedCategories.forEach { category ->
            CategoryChip(
                iconKey = category.iconKey,
                colorKey = category.colorKey,
                label = category.name,
                selected = true,
                onClick = { viewModel.removeCategory(category.id) },
                trailingIcon = Icons.Filled.Close,
            )
        }
        Spender.entries.filter { it in filter.spenders }.forEach { spender ->
            SalimChip(
                label = state.names.labelOf(spender),
                selected = true,
                onClick = { viewModel.removeSpender(spender) },
                trailingIcon = Icons.Filled.Close,
            )
        }
    }
}

/** "8월 3일 ~ 8월 10일". 한쪽만 정했으면 반대쪽을 비워 둔다. */
private fun periodLabel(startDayUtc: Long?, endDayUtc: Long?): String {
    val format = SimpleDateFormat("M월 d일", Locale.KOREAN).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val start = startDayUtc?.let { format.format(Date(it)) }.orEmpty()
    val end = endDayUtc?.let { format.format(Date(it)) }.orEmpty()
    return "$start ~ $end".trim()
}

/**
 * 필터 바텀시트. 시트 안에서 고친 조건은 "적용하기"를 눌러야 반영되고, 그냥 닫으면 버린다.
 * (expense.md 4-1 필터)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseFilterSheet(
    state: ExpenseListUiState,
    onDismiss: () -> Unit,
    onApply: (ExpenseFilter) -> Unit,
) {
    var draft by remember { mutableStateOf(state.filter) }
    // 어느 날짜 줄의 팝업을 띄웠는지. null이면 닫힘.
    var pickingStart by remember { mutableStateOf<Boolean?>(null) }
    val allLabel = stringResource(R.string.expense_filter_all)
    val dayFormat = remember {
        SimpleDateFormat("M월 d일 (E)", Locale.KOREAN).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SalimTokens.CardSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.expense_filter), style = SalimType.titleLg, color = SalimTokens.TextPrimary)
                TextButton(onClick = { draft = draft.clearConditions() }) {
                    Text(stringResource(R.string.expense_filter_reset), style = SalimType.bodyMd, color = SalimTokens.Accent)
                }
            }

            FilterSection(stringResource(R.string.expense_filter_period)) {
                Column {
                    FieldRow(
                        label = stringResource(R.string.expense_filter_start),
                        value = draft.startDayUtc?.let { dayFormat.format(Date(it)) } ?: allLabel,
                        onClick = { pickingStart = true },
                    )
                    FieldDivider()
                    FieldRow(
                        label = stringResource(R.string.expense_filter_end),
                        value = draft.endDayUtc?.let { dayFormat.format(Date(it)) } ?: allLabel,
                        onClick = { pickingStart = false },
                    )
                }
            }

            FilterSection(stringResource(R.string.expense_filter_category)) {
                ChipFlowRow {
                    state.categories.forEach { category ->
                        val selected = category.id in draft.categoryIds
                        CategoryChip(
                            iconKey = category.iconKey,
                            colorKey = category.colorKey,
                            label = category.name,
                            selected = selected,
                            onClick = {
                                draft = draft.copy(
                                    categoryIds = if (selected) draft.categoryIds - category.id else draft.categoryIds + category.id,
                                )
                            },
                        )
                    }
                }
            }

            // 미연결이면 모든 지출이 본인 것이라 지출자 구역을 숨긴다.
            if (state.connected) {
                FilterSection(stringResource(R.string.expense_filter_spender)) {
                    ChipFlowRow {
                        Spender.entries.forEach { spender ->
                            val selected = spender in draft.spenders
                            SalimChip(
                                label = state.names.labelOf(spender),
                                selected = selected,
                                onClick = {
                                    draft = draft.copy(
                                        spenders = if (selected) draft.spenders - spender else draft.spenders + spender,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            SaveButton(
                enabled = true,
                onClick = { onApply(draft) },
                label = stringResource(R.string.expense_filter_apply),
            )
        }
    }

    pickingStart?.let { isStart ->
        val current = if (isStart) draft.startDayUtc else draft.endDayUtc
        DatePickerModal(
            initialMillis = current ?: if (isStart) state.monthStartUtc else state.monthEndUtc,
            minDateUtc = state.monthStartUtc,
            maxDateUtc = state.monthEndUtc,
            onConfirm = { picked ->
                draft = if (isStart) {
                    draft.withPeriod(picked, draft.endDayUtc)
                } else {
                    draft.withPeriod(draft.startDayUtc, picked)
                }
                pickingStart = null
            },
            onDismiss = { pickingStart = null },
        )
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = SalimType.labelMd, color = SalimTokens.TextMuted)
        content()
    }
}

@Composable
private fun TotalCard(label: String, total: String) {
    SalimCard(cornerRadius = 18.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = SalimType.labelMd, color = SalimTokens.TextMuted)
            Text(total, style = SalimType.headlineMd, color = SalimTokens.TextPrimary)
        }
    }
}

@Composable
private fun DayGroup(day: ExpenseDayUi, onItemClick: (ExpenseRowUi) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            day.dateHeader,
            style = SalimType.labelMd,
            color = SalimTokens.TextMuted,
            modifier = Modifier.padding(start = 4.dp),
        )
        SalimCard(cornerRadius = 20.dp, contentPadding = 8.dp) {
            day.rows.forEachIndexed { index, row ->
                ExpenseRow(row, onClick = { onItemClick(row) })
                if (index != day.rows.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .height(1.dp)
                            .background(SalimTokens.Divider),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(row: ExpenseRowUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIconBadge(row.iconKey, row.colorKey)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(row.title, style = SalimType.bodyLg, color = SalimTokens.TextPrimary)
            Text(row.meta, style = SalimType.bodySm, color = SalimTokens.TextMuted)
        }
        Text(
            row.amount,
            style = SalimType.bodyLg.copy(fontWeight = FontWeight.Medium),
            color = SalimTokens.TextPrimary,
        )
    }
}

@Composable
private fun EmptyState(text: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text,
            style = SalimType.bodyMd,
            color = SalimTokens.TextMuted,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null) {
            SalimChip(label = actionLabel, selected = false, onClick = onAction)
        }
    }
}
