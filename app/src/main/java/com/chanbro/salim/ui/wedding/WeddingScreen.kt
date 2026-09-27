package com.chanbro.salim.ui.wedding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.domain.model.Spender
import com.chanbro.salim.domain.model.Todo
import com.chanbro.salim.domain.model.TodoAssignee
import com.chanbro.salim.domain.model.VendorStatus
import com.chanbro.salim.domain.model.WeddingExpense
import com.chanbro.salim.domain.model.WeddingItem
import com.chanbro.salim.domain.model.WeddingOverview
import com.chanbro.salim.domain.model.WeddingPeriod
import com.chanbro.salim.domain.model.WeddingSettings
import com.chanbro.salim.domain.model.WeddingTask
import com.chanbro.salim.domain.model.WeddingTaskGroup
import com.chanbro.salim.domain.model.WeddingVendor
import com.chanbro.salim.domain.model.currentWeddingPeriod
import com.chanbro.salim.ui.common.BudgetInputSheet
import com.chanbro.salim.ui.common.CategoryIconBadge
import com.chanbro.salim.ui.common.CheckCircleButton
import com.chanbro.salim.ui.common.ChipFlowRow
import com.chanbro.salim.ui.common.DDayBadge
import com.chanbro.salim.ui.common.DatePickerModal
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimChip
import com.chanbro.salim.ui.common.SalimProgressBar
import com.chanbro.salim.ui.common.SalimSegmentedControl
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.common.SaveButton
import com.chanbro.salim.ui.common.SubScreenTopBar
import com.chanbro.salim.ui.common.formatWon
import com.chanbro.salim.ui.common.todayUtcMillis
import com.chanbro.salim.ui.dday.dDayLabel
import com.chanbro.salim.ui.dday.daysUntil

// ---------------------------------------------------------------------------
// 결혼 준비 메인 (wedding.md 12-1) — 요약 카드 + 세그먼트(체크리스트 / 예산 / 업체)
// 하위 화면이라 하단 탭바가 없고, FAB는 이 화면이 직접 그린다 (디데이 관리와 같은 방식).
// ---------------------------------------------------------------------------

enum class WeddingSegment { CHECKLIST, BUDGET, VENDORS }

@Composable
fun WeddingScreen(
    onBack: () -> Unit,
    onAddExpense: () -> Unit,
    onExpenseClick: (String) -> Unit,
    onAddVendor: () -> Unit,
    onVendorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeddingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 마지막으로 본 세그먼트. 하위 화면에 다녀와도 유지된다.
    var segment by rememberSaveable { mutableStateOf(WeddingSegment.CHECKLIST) }
    // 체크리스트 시트: 열림 여부 + 수정할 항목 id(추가면 null)
    var taskSheetOpen by rememberSaveable { mutableStateOf(false) }
    var taskSheetId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showBudgetSheet by rememberSaveable { mutableStateOf(false) }

    // 설정에서 끄면(상대방이 끈 경우 포함) 홈으로 돌아간다. 켜진 걸 본 뒤에 꺼졌을 때만 —
    // 첫 스냅샷이 캐시의 빈 문서여도 들어오자마자 튕기지 않게.
    var seenEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(state.loading, state.enabled) {
        if (state.loading) return@LaunchedEffect
        if (state.enabled) seenEnabled = true else if (seenEnabled) onBack()
    }

    WeddingContent(
        state = state,
        segment = segment,
        modifier = modifier,
        onBack = onBack,
        onSegmentChange = { segment = it },
        onDateClick = { showDatePicker = true },
        onBudgetClick = { showBudgetSheet = true },
        onToggleTask = viewModel::setTaskDone,
        onTaskClick = {
            taskSheetId = it.id
            taskSheetOpen = true
        },
        onExpenseClick = onExpenseClick,
        onVendorClick = onVendorClick,
        onFabClick = {
            when (segment) {
                WeddingSegment.CHECKLIST -> {
                    taskSheetId = null
                    taskSheetOpen = true
                }
                WeddingSegment.BUDGET -> onAddExpense()
                WeddingSegment.VENDORS -> onAddVendor()
            }
        },
    )

    val settings = state.overview.settings
    if (taskSheetOpen) {
        val targetId = taskSheetId
        val editing = targetId?.let { id -> state.overview.tasks.firstOrNull { it.id == id } }
        if (targetId != null && editing == null) {
            // 수정 중 상대방이 지웠다 — 시트를 닫는다.
            if (!state.loading) LaunchedEffect(targetId) { taskSheetOpen = false }
        } else {
            // 시트는 연 순간의 항목을 기준으로 삼는다 — 열어 둔 사이 목록이 바뀌어도 입력이 초기화되지 않게.
            val baseline = remember(targetId) { editing }
            TaskEditSheet(
                baseline = baseline,
                defaultPeriod = settings.weddingDateMillis?.let { currentWeddingPeriod(it, state.todayMillis) }
                    ?: WeddingPeriod.M12_6,
                connected = state.connected,
                names = state.names,
                onDismiss = { taskSheetOpen = false },
                onSave = { title, period, assignee ->
                    viewModel.saveTask(baseline, title, period, assignee)
                    taskSheetOpen = false
                },
                onDelete = {
                    baseline?.let(viewModel::deleteTask)
                    taskSheetOpen = false
                },
            )
        }
    }

    if (showDatePicker) {
        DatePickerModal(
            initialMillis = settings.weddingDateMillis ?: todayUtcMillis(),
            onConfirm = {
                viewModel.setDate(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            // 예식일이 있을 때만 "미정으로" (wedding.md 12-1)
            clearLabel = if (settings.weddingDateMillis != null) stringResource(R.string.wedding_date_clear) else null,
            onClear = {
                viewModel.setDate(null)
                showDatePicker = false
            },
        )
    }

    if (showBudgetSheet) {
        BudgetInputSheet(
            title = stringResource(R.string.wedding_budget_label),
            initialAmount = settings.totalBudget,
            onDismiss = { showBudgetSheet = false },
            onConfirm = {
                viewModel.setBudget(it)
                showBudgetSheet = false
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeddingContent(
    state: WeddingUiState,
    segment: WeddingSegment,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSegmentChange: (WeddingSegment) -> Unit,
    onDateClick: () -> Unit,
    onBudgetClick: () -> Unit,
    onToggleTask: (WeddingTask, Boolean) -> Unit,
    onTaskClick: (WeddingTask) -> Unit,
    onExpenseClick: (String) -> Unit,
    onVendorClick: (String) -> Unit,
    onFabClick: () -> Unit,
) {
    val overview = state.overview
    Box(modifier = modifier.fillMaxSize().background(SalimTokens.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(title = stringResource(R.string.wedding_title), onBack = onBack)
            if (!state.loading) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // FAB에 마지막 줄이 가리지 않게 아래 여백을 넉넉히 둔다.
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "summary") {
                        SummaryCard(
                            overview = overview,
                            todayMillis = state.todayMillis,
                            onDateClick = onDateClick,
                            onChecklistClick = { onSegmentChange(WeddingSegment.CHECKLIST) },
                            onBudgetClick = { onSegmentChange(WeddingSegment.BUDGET) },
                        )
                    }
                    // 스크롤하다 상단에 닿으면 고정된다. 뒤에 배경 띠를 깔아 아래 내용이 비치지 않게.
                    stickyHeader(key = "segment") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SalimTokens.Background)
                                .padding(vertical = 4.dp),
                        ) {
                            SalimSegmentedControl(
                                labels = listOf(
                                    stringResource(R.string.wedding_checklist),
                                    stringResource(R.string.wedding_budget),
                                    stringResource(R.string.wedding_vendors),
                                ),
                                selectedIndex = segment.ordinal,
                                onSelect = { onSegmentChange(WeddingSegment.entries[it]) },
                            )
                        }
                    }
                    when (segment) {
                        WeddingSegment.CHECKLIST -> checklistItems(state, onToggleTask, onTaskClick)
                        WeddingSegment.BUDGET -> budgetItems(state, onBudgetClick, onExpenseClick)
                        WeddingSegment.VENDORS -> vendorItems(overview, onVendorClick)
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onFabClick,
            containerColor = SalimTokens.Accent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            val description = when (segment) {
                WeddingSegment.CHECKLIST -> R.string.wedding_task_add
                WeddingSegment.BUDGET -> R.string.wedding_expense_add
                WeddingSegment.VENDORS -> R.string.wedding_vendor_add
            }
            Icon(Icons.Filled.Add, contentDescription = stringResource(description))
        }
    }
}

// --- 요약 카드 (12-1) ---

@Composable
private fun SummaryCard(
    overview: WeddingOverview,
    todayMillis: Long,
    onDateClick: () -> Unit,
    onChecklistClick: () -> Unit,
    onBudgetClick: () -> Unit,
) {
    val date = overview.settings.weddingDateMillis
    SalimCard(cornerRadius = 24.dp, contentPadding = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDateClick)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (date == null) {
                Text(
                    stringResource(R.string.wedding_date_empty),
                    style = SalimType.bodyLg,
                    color = SalimTokens.Accent,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SalimTokens.TextMuted)
            } else {
                Text(
                    formatFullDate(date),
                    style = SalimType.titleLg,
                    color = SalimTokens.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                DDayBadge(dDayLabel(daysUntil(date, todayMillis)))
            }
        }
        ThinDivider()
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryCell(
                label = stringResource(R.string.wedding_checklist),
                value = stringResource(R.string.wedding_checklist_progress, overview.doneTaskCount, overview.taskCount),
                ratio = if (overview.taskCount > 0) overview.doneTaskCount.toFloat() / overview.taskCount else 0f,
                modifier = Modifier.weight(1f),
                onClick = onChecklistClick,
            )
            val budget = overview.settings.totalBudget?.takeIf { it > 0 }
            SummaryCell(
                label = stringResource(R.string.wedding_budget_label),
                value = if (budget == null) {
                    stringResource(R.string.wedding_budget_unset)
                } else {
                    stringResource(R.string.wedding_budget_used, (overview.spentTotal * 100 / budget).toInt())
                },
                ratio = budget?.let { overview.spentTotal.toFloat() / it },
                over = budget != null && overview.spentTotal > budget,
                modifier = Modifier.weight(1f),
                onClick = onBudgetClick,
            )
        }
    }
}

/** 요약 카드·홈 카드의 한 칸: 라벨 + 값 + 얇은 진행률 바. [ratio]가 null이면 바를 숨긴다. */
@Composable
fun SummaryCell(
    label: String,
    value: String,
    ratio: Float?,
    modifier: Modifier = Modifier,
    over: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = SalimType.labelMd, color = SalimTokens.TextMuted)
        Text(value, style = SalimType.bodyMd.copy(fontWeight = FontWeight.Bold), color = SalimTokens.TextPrimary)
        if (ratio != null) {
            SalimProgressBar(ratio = ratio, over = over, height = 4.dp)
        } else {
            Box(Modifier.height(4.dp))
        }
    }
}

// --- 체크리스트 (12-2) ---

private fun androidx.compose.foundation.lazy.LazyListScope.checklistItems(
    state: WeddingUiState,
    onToggle: (WeddingTask, Boolean) -> Unit,
    onTaskClick: (WeddingTask) -> Unit,
) {
    val groups = state.overview.taskGroups
    if (groups.isEmpty()) {
        item(key = "checklist-empty") { CenteredEmpty(stringResource(R.string.wedding_checklist_empty)) }
        return
    }
    val weddingDate = state.overview.settings.weddingDateMillis
    val current = weddingDate?.let { currentWeddingPeriod(it, state.todayMillis) }
    groups.forEach { group ->
        item(key = "period-${group.period}") {
            // 예식일이 있으면 지금 시기만, 미정이면 전부 펼친다. 펼침 상태는 화면에 있는 동안만. (12-2 펼침 규칙)
            var expanded by remember(current, group.period) { mutableStateOf(current == null || current == group.period) }
            TaskGroupCard(
                group = group,
                isCurrent = group.period == current,
                // 지난 시기에 못 한 항목이 남아 있으면 진행 표시를 Coral로.
                missed = current != null && group.period < current && group.openCount > 0,
                expanded = expanded,
                connected = state.connected,
                names = state.names,
                onHeaderClick = { expanded = !expanded },
                onToggle = onToggle,
                onTaskClick = onTaskClick,
            )
        }
    }
}

@Composable
private fun TaskGroupCard(
    group: WeddingTaskGroup,
    isCurrent: Boolean,
    missed: Boolean,
    expanded: Boolean,
    connected: Boolean,
    names: com.chanbro.salim.domain.model.SpenderNames,
    onHeaderClick: () -> Unit,
    onToggle: (WeddingTask, Boolean) -> Unit,
    onTaskClick: (WeddingTask) -> Unit,
) {
    SalimCard(cornerRadius = 24.dp, contentPadding = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onHeaderClick)
                .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(group.period.labelRes()),
                style = SalimType.bodyLg.copy(fontWeight = FontWeight.Bold),
                color = SalimTokens.TextPrimary,
            )
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(SalimTokens.AccentSoft)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(stringResource(R.string.wedding_period_now), style = SalimType.labelSm, color = SalimTokens.Accent)
                }
            }
            Box(Modifier.weight(1f))
            Text(
                stringResource(R.string.wedding_checklist_count, group.doneCount, group.tasks.size),
                style = SalimType.labelMd,
                color = if (missed) SalimTokens.Accent else SalimTokens.TextMuted,
            )
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(if (expanded) R.string.todo_collapse else R.string.todo_expand),
                tint = SalimTokens.TextMuted,
                modifier = Modifier.padding(8.dp).size(20.dp),
            )
        }
        if (expanded) {
            group.tasks.forEach { task ->
                ThinDivider(Modifier.padding(horizontal = 12.dp))
                TaskRow(
                    task = task,
                    meta = if (connected) assigneeLabel(task.assignee, names) else null,
                    onToggle = { onToggle(task, !task.done) },
                    onClick = { onTaskClick(task) },
                )
            }
        }
    }
}

@Composable
private fun TaskRow(task: WeddingTask, meta: String?, onToggle: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircleButton(checked = task.done, size = 24.dp, onClick = onToggle)
        Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                task.title,
                style = SalimType.bodyLg.copy(textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None),
                color = if (task.done) SalimTokens.TextMuted else SalimTokens.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta != null) Text(meta, style = SalimType.bodySm, color = SalimTokens.TextMuted)
        }
    }
}

// --- 체크리스트 항목 추가/수정 시트 (12-3) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditSheet(
    baseline: WeddingTask?,
    defaultPeriod: WeddingPeriod,
    connected: Boolean,
    names: com.chanbro.salim.domain.model.SpenderNames,
    onDismiss: () -> Unit,
    onSave: (String, WeddingPeriod, TodoAssignee) -> Unit,
    onDelete: () -> Unit,
) {
    val isEdit = baseline != null
    var title by rememberSaveable { mutableStateOf(baseline?.title.orEmpty()) }
    var period by rememberSaveable { mutableStateOf(baseline?.period ?: defaultPeriod) }
    var assignee by rememberSaveable { mutableStateOf(baseline?.assignee ?: TodoAssignee.TOGETHER) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SalimTokens.CardSurface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().imePadding()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(if (isEdit) R.string.wedding_task_edit else R.string.wedding_task_add),
                        style = SalimType.titleLg,
                        color = SalimTokens.TextPrimary,
                    )
                    if (isEdit) {
                        IconButton(onClick = { confirmingDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.common_delete), tint = SalimTokens.TextPrimary)
                        }
                    }
                }
                SheetTextField(
                    value = title,
                    onValueChange = { title = it },
                    hint = stringResource(R.string.todo_title_hint),
                    maxLength = Todo.TITLE_MAX_LENGTH,
                    focusRequester = titleFocus,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                SheetSection(stringResource(R.string.wedding_task_period)) {
                    ChipFlowRow {
                        WeddingPeriod.entries.forEach { option ->
                            SalimChip(
                                label = stringResource(option.labelRes()),
                                selected = option == period,
                                onClick = { period = option },
                            )
                        }
                    }
                }
                // 미연결이면 담당자 줄을 숨기고 본인으로 저장한다.
                if (connected) {
                    SheetSection(stringResource(R.string.todo_assignee)) {
                        ChipFlowRow {
                            TodoAssignee.entries.forEach { option ->
                                SalimChip(
                                    label = assigneeLabel(option, names),
                                    selected = option == assignee,
                                    onClick = { assignee = option },
                                )
                            }
                        }
                    }
                }
            }
            SaveButton(
                enabled = title.isNotBlank(),
                onClick = { onSave(title, period, assignee) },
                label = stringResource(if (isEdit) R.string.common_edit_done else R.string.common_save),
            )
        }
    }
    if (!isEdit) LaunchedEffect(Unit) { titleFocus.requestFocus() }
    if (confirmingDelete) {
        DeleteConfirmDialog(
            onConfirm = {
                confirmingDelete = false
                onDelete()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}

// --- 예산 (12-4) ---

private fun androidx.compose.foundation.lazy.LazyListScope.budgetItems(
    state: WeddingUiState,
    onBudgetClick: () -> Unit,
    onExpenseClick: (String) -> Unit,
) {
    val overview = state.overview
    item(key = "budget-card") { WeddingBudgetCard(overview, onBudgetClick) }
    item(key = "item-totals") { ItemTotalsCard(overview) }
    item(key = "history-header") {
        Row(
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(stringResource(R.string.wedding_expense_history), style = SalimType.headlineSm, color = SalimTokens.TextPrimary)
            Text(
                stringResource(R.string.wedding_expense_count, overview.expenses.size),
                style = SalimType.bodyMd,
                color = SalimTokens.TextMuted,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
    val expenses = overview.sortedExpenses
    if (expenses.isEmpty()) {
        item(key = "history-empty") {
            Text(
                stringResource(R.string.wedding_expense_empty),
                style = SalimType.bodyMd,
                color = SalimTokens.TextMuted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
    } else {
        expenses.groupBy { it.dateMillis }.forEach { (date, list) ->
            item(key = "day-$date") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        formatDayHeader(date),
                        style = SalimType.labelMd,
                        color = SalimTokens.TextMuted,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                    SalimCard(cornerRadius = 20.dp, contentPadding = 8.dp) {
                        list.forEachIndexed { index, expense ->
                            if (index > 0) ThinDivider(Modifier.padding(horizontal = 12.dp))
                            WeddingExpenseRow(
                                expense = expense,
                                overview = overview,
                                spenderLabel = if (state.connected) state.names.labelOf(expense.spender) else null,
                                onClick = { onExpenseClick(expense.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeddingBudgetCard(overview: WeddingOverview, onClick: () -> Unit) {
    val budget = overview.settings.totalBudget?.takeIf { it > 0 }
    SalimCard(cornerRadius = 24.dp, modifier = Modifier.clickable(onClick = onClick)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.wedding_budget_label), style = SalimType.labelMd, color = SalimTokens.TextMuted)
                // 카드를 탭하면 예산을 고칠 수 있다는 표시 (홈 예산 카드와 같은 형태)
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(SalimTokens.AccentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.wedding_budget_edit),
                        tint = SalimTokens.Accent,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            if (budget == null) {
                Text(stringResource(R.string.wedding_budget_cta), style = SalimType.bodyLg, color = SalimTokens.Accent)
            } else {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(formatWon(overview.spentTotal), style = SalimType.headlineMd, color = SalimTokens.TextPrimary)
                    Text(
                        "/ ${formatWon(budget)}",
                        style = SalimType.bodyMd,
                        color = SalimTokens.TextMuted,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
                SalimProgressBar(
                    ratio = overview.spentTotal.toFloat() / budget,
                    plannedRatio = overview.plannedTotal.toFloat() / budget,
                    over = overview.spentTotal > budget,
                )
            }
            Column {
                LabelValueRow(stringResource(R.string.wedding_spent), formatWon(overview.spentTotal))
                LabelValueRow(stringResource(R.string.wedding_planned), formatWon(overview.plannedTotal))
                val remaining = overview.remainingBudget
                if (budget != null && remaining != null) {
                    if (remaining >= 0) {
                        LabelValueRow(stringResource(R.string.wedding_remaining), formatWon(remaining))
                    } else {
                        LabelValueRow(
                            stringResource(R.string.wedding_over_expected),
                            formatWon(-remaining),
                            valueColor = SalimTokens.Warning,
                            labelColor = SalimTokens.Warning,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemTotalsCard(overview: WeddingOverview) {
    SalimCard(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.wedding_item_totals), style = SalimType.headlineSm, color = SalimTokens.TextPrimary)
            WeddingItem.entries.forEach { item ->
                val planned = overview.plannedBy(item)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryIconBadge(icon = item.icon(), colorKey = item.colorKey(), size = 36.dp)
                    Text(
                        stringResource(item.labelRes()),
                        style = SalimType.bodyLg,
                        color = SalimTokens.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatWon(overview.spentBy(item)), style = SalimType.bodyLg, color = SalimTokens.TextPrimary)
                        if (planned > 0) {
                            Text(
                                stringResource(R.string.wedding_planned_caption, formatWon(planned)),
                                style = SalimType.bodySm,
                                color = SalimTokens.TextMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeddingExpenseRow(
    expense: WeddingExpense,
    overview: WeddingOverview,
    spenderLabel: String?,
    onClick: () -> Unit,
    showItemIcon: Boolean = true,
) {
    val itemLabel = stringResource(expense.item.labelRes())
    val vendor = overview.vendorOf(expense)
    // 제목: 메모 → 업체명 → 항목명 순으로 있는 것 (12-4). 제목으로 쓴 값은 메타에 되풀이하지 않는다.
    val memo = expense.memo?.takeIf { it.isNotBlank() }
    val title = memo ?: vendor?.name ?: itemLabel
    val meta = listOfNotNull(
        itemLabel.takeIf { it != title },
        vendor?.name?.takeIf { memo != null },
        spenderLabel,
    ).joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showItemIcon) CategoryIconBadge(icon = expense.item.icon(), colorKey = expense.item.colorKey(), size = 38.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = SalimType.bodyLg, color = SalimTokens.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, style = SalimType.bodySm, color = SalimTokens.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatWon(expense.amount), style = SalimType.bodyLg.copy(fontWeight = FontWeight.Medium), color = SalimTokens.TextPrimary)
    }
}

// --- 업체 (12-6) ---

private fun androidx.compose.foundation.lazy.LazyListScope.vendorItems(
    overview: WeddingOverview,
    onVendorClick: (String) -> Unit,
) {
    val groups = overview.vendorGroups
    if (groups.isEmpty()) {
        item(key = "vendors-empty") { CenteredEmpty(stringResource(R.string.wedding_vendor_empty)) }
        return
    }
    groups.forEach { (item, vendors) ->
        item(key = "vendors-$item") {
            SalimCard(cornerRadius = 24.dp, contentPadding = 8.dp) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryIconBadge(icon = item.icon(), colorKey = item.colorKey(), size = 32.dp)
                    Text(
                        stringResource(item.labelRes()),
                        style = SalimType.bodyLg.copy(fontWeight = FontWeight.Bold),
                        color = SalimTokens.TextPrimary,
                    )
                }
                vendors.forEach { vendor ->
                    ThinDivider(Modifier.padding(horizontal = 12.dp))
                    VendorRow(vendor, overview, onClick = { onVendorClick(vendor.id) })
                }
            }
        }
    }
}

@Composable
private fun VendorRow(vendor: WeddingVendor, overview: WeddingOverview, onClick: () -> Unit) {
    val meta = if (vendor.status == VendorStatus.CONTRACTED) {
        listOfNotNull(
            vendor.contractAmount?.let { stringResource(R.string.wedding_vendor_meta_contract, formatWon(it)) },
            overview.remainingFor(vendor)?.let {
                if (it >= 0) {
                    stringResource(R.string.wedding_vendor_meta_remaining, formatWon(it))
                } else {
                    stringResource(R.string.wedding_vendor_over, formatWon(-it))
                }
            },
            vendor.balanceDueMillis?.let { stringResource(R.string.wedding_vendor_meta_balance_due, formatShortDate(it)) },
        )
    } else {
        listOfNotNull(vendor.contractAmount?.let(::formatWon))
    }.joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    vendor.name,
                    style = SalimType.bodyLg,
                    color = SalimTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                VendorStatusTag(vendor.status)
            }
            if (meta.isNotEmpty()) Text(meta, style = SalimType.bodySm, color = SalimTokens.TextMuted)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SalimTokens.TextMuted, modifier = Modifier.size(20.dp))
    }
}

// ---------------------------------------------------------------------------
// 프리뷰
// ---------------------------------------------------------------------------

private val previewOverview = WeddingOverview(
    settings = WeddingSettings(enabled = true, weddingDateMillis = todayUtcMillis() + 120L * 86_400_000, totalBudget = 30_000_000),
    tasks = listOf(
        WeddingTask("1", "상견례", WeddingPeriod.M12_6, TodoAssignee.TOGETHER, true, 5, 1),
        WeddingTask("2", "예식장 계약", WeddingPeriod.M12_6, TodoAssignee.TOGETHER, false, null, 2),
        WeddingTask("3", "신혼여행 예약", WeddingPeriod.M6_3, TodoAssignee.ME, false, null, 3),
    ),
    vendors = listOf(
        WeddingVendor("v", "더채플 청담", WeddingItem.VENUE, VendorStatus.CONTRACTED, "0212345678", 12_000_000, null, null, 1),
    ),
    expenses = listOf(WeddingExpense("e", 2_000_000, todayUtcMillis(), WeddingItem.VENUE, "v", Spender.SHARED, "계약금", 1)),
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WeddingChecklistPreview() {
    SalimTheme {
        WeddingContent(
            state = WeddingUiState(loading = false, overview = previewOverview, connected = true),
            segment = WeddingSegment.CHECKLIST,
            onBack = {}, onSegmentChange = {}, onDateClick = {}, onBudgetClick = {},
            onToggleTask = { _, _ -> }, onTaskClick = {}, onExpenseClick = {}, onVendorClick = {}, onFabClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WeddingBudgetPreview() {
    SalimTheme {
        WeddingContent(
            state = WeddingUiState(loading = false, overview = previewOverview, connected = true),
            segment = WeddingSegment.BUDGET,
            onBack = {}, onSegmentChange = {}, onDateClick = {}, onBudgetClick = {},
            onToggleTask = { _, _ -> }, onTaskClick = {}, onExpenseClick = {}, onVendorClick = {}, onFabClick = {},
        )
    }
}
