package com.chanbro.salim.ui.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.domain.model.SpenderNames
import com.chanbro.salim.domain.model.SubtaskDraft
import com.chanbro.salim.domain.model.Todo
import com.chanbro.salim.domain.model.TodoAssignee
import com.chanbro.salim.domain.model.TodoItem
import com.chanbro.salim.domain.model.TodoSections
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimChip
import com.chanbro.salim.ui.common.SalimTab
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.common.SaveButton
import java.util.UUID

// ---------------------------------------------------------------------------
// 할 일 리스트 (todo.md 11-1) — 탭 랜딩 화면
// 하단 탭바/FAB는 상위 Scaffold가 제공, 여기서는 콘텐츠와 추가/수정 시트만
// ---------------------------------------------------------------------------

/**
 * @param addRequested 상위 Scaffold의 FAB가 눌렸는지. 시트를 열고 [onAddHandled]로 알린다.
 */
@Composable
fun TodoScreen(
    addRequested: Boolean,
    onAddHandled: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(addRequested) {
        if (addRequested) {
            editingId = null
            adding = true
            onAddHandled()
        }
    }

    TodoContent(
        state = state,
        modifier = modifier,
        onToggle = viewModel::setDone,
        onToggleSubtask = viewModel::setSubtaskDone,
        onItemClick = { editingId = it.todo.id },
    )

    val editing = editingId
    when {
        adding -> TodoEditSheet(
            baseline = null,
            connected = state.connected,
            names = state.names,
            onDismiss = { adding = false },
            onSave = { title, assignee, drafts ->
                viewModel.save(null, title, assignee, drafts)
                adding = false
            },
            onDelete = {},
        )
        editing != null -> {
            val target = state.sections.find(editing)
            if (target == null) {
                // 수정 중 상대방이 지웠다 — 시트를 닫는다. (todo.md 11-2 동작)
                if (!state.loading) LaunchedEffect(editing) { editingId = null }
            } else {
                // 시트는 연 순간의 항목을 기준으로 삼는다 — 열어 둔 사이 목록이 바뀌어도
                // 입력 중인 내용이 초기화되지 않고, 저장도 연 시점 기준으로 비교한다.
                val baseline = remember(editing) { target }
                TodoEditSheet(
                    baseline = baseline,
                    connected = state.connected,
                    names = state.names,
                    onDismiss = { editingId = null },
                    onSave = { title, assignee, drafts ->
                        viewModel.save(baseline, title, assignee, drafts)
                        editingId = null
                    },
                    onDelete = {
                        // 삭제는 지금 목록 기준 — 그사이 상대가 붙인 하위 항목도 함께 지운다.
                        viewModel.delete(target)
                        editingId = null
                    },
                )
            }
        }
    }
}

@Composable
private fun TodoContent(
    state: TodoUiState,
    modifier: Modifier = Modifier,
    onToggle: (item: TodoItem, done: Boolean) -> Unit,
    onToggleSubtask: (id: String, done: Boolean) -> Unit,
    onItemClick: (TodoItem) -> Unit,
) {
    var doneExpanded by rememberSaveable { mutableStateOf(false) }
    // 하위 항목을 펼친 상위 항목 id. 항목마다 따로 기억한다.
    var expandedIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val open = state.sections.open
    val done = state.sections.done
    val rowActions = TodoRowActions(
        connected = state.connected,
        names = state.names,
        isExpanded = { it in expandedIds },
        onExpandToggle = { id -> expandedIds = if (id in expandedIds) expandedIds - id else expandedIds + id },
        onToggle = onToggle,
        onToggleSubtask = onToggleSubtask,
        onItemClick = onItemClick,
    )

    Column(modifier = modifier.fillMaxSize()) {
        TodoTopBar()
        when {
            state.loading -> Unit
            open.isEmpty() && done.isEmpty() -> EmptyState(stringResource(R.string.todo_empty))
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    // FAB에 마지막 줄이 가리지 않게 아래 여백을 넉넉히 둔다.
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SalimCard(cornerRadius = 24.dp, contentPadding = 8.dp) {
                    if (open.isEmpty()) {
                        Text(
                            stringResource(R.string.todo_all_done),
                            style = SalimType.bodyMd,
                            color = SalimTokens.TextMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                        )
                    } else {
                        TodoItems(open, rowActions)
                    }
                }
                // 완료 항목이 없으면 섹션 자체를 숨긴다.
                if (done.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DoneSectionHeader(
                            count = done.size,
                            expanded = doneExpanded,
                            onClick = { doneExpanded = !doneExpanded },
                        )
                        if (doneExpanded) {
                            SalimCard(cornerRadius = 24.dp, contentPadding = 8.dp) {
                                TodoItems(done, rowActions)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 목록 줄들이 공통으로 쓰는 상태와 동작. */
private class TodoRowActions(
    val connected: Boolean,
    val names: SpenderNames,
    val isExpanded: (String) -> Boolean,
    val onExpandToggle: (String) -> Unit,
    val onToggle: (TodoItem, Boolean) -> Unit,
    val onToggleSubtask: (String, Boolean) -> Unit,
    val onItemClick: (TodoItem) -> Unit,
)

@Composable
private fun TodoItems(items: List<TodoItem>, actions: TodoRowActions) {
    items.forEachIndexed { index, item ->
        val expanded = item.subtasks.isNotEmpty() && actions.isExpanded(item.todo.id)
        TodoRow(item = item, expanded = expanded, actions = actions)
        if (expanded) {
            item.subtasks.forEach { subtask ->
                SubtaskRow(
                    subtask = subtask,
                    // 미연결이면 모두 내 할 일이라 담당자를 보여주지 않는다.
                    assigneeLabel = if (actions.connected) assigneeLabel(subtask.assignee, actions.names) else null,
                    onToggle = { actions.onToggleSubtask(subtask.id, !subtask.done) },
                    onClick = { actions.onItemClick(item) },
                )
            }
        }
        if (index != items.lastIndex) {
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

@Composable
private fun TodoTopBar() {
    Surface(color = SalimTokens.Background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 하단 탭바와 같은 아이콘을 참조해 탭 ↔ 상단 바를 같은 기호로 묶는다
            Icon(
                SalimTab.Todo.icon,
                contentDescription = null,
                tint = SalimTokens.Accent,
                modifier = Modifier.size(20.dp),
            )
            Text(stringResource(R.string.todo_title), style = SalimType.headlineSm, color = SalimTokens.TextPrimary)
        }
    }
}

@Composable
private fun TodoRow(item: TodoItem, expanded: Boolean, actions: TodoRowActions) {
    val todo = item.todo
    // 메타: 담당자(연결 시) · 하위 항목 진행. 둘 다 없으면 줄을 숨긴다. (todo.md 11-1)
    val meta = listOfNotNull(
        if (actions.connected) assigneeLabel(todo.assignee, actions.names) else null,
        if (item.subtasks.isNotEmpty()) {
            stringResource(R.string.todo_subtask_progress, item.doneSubtaskCount, item.subtasks.size)
        } else {
            null
        },
    ).joinToString(" · ").takeIf { it.isNotEmpty() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { actions.onItemClick(item) }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 체크박스는 줄 탭(수정)과 따로 눌린다.
        CheckButton(checked = todo.done, size = 24.dp, onClick = { actions.onToggle(item, !todo.done) })
        TodoTexts(todo.title, todo.done, meta, Modifier.weight(1f), SalimType.bodyLg)
        if (item.subtasks.isNotEmpty()) {
            IconButton(onClick = { actions.onExpandToggle(todo.id) }) {
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(if (expanded) R.string.todo_collapse else R.string.todo_expand),
                    tint = SalimTokens.TextMuted,
                )
            }
        }
    }
}

/** 펼친 하위 항목 한 줄. 상위 항목 체크박스 열만큼 들여쓴다. */
@Composable
private fun SubtaskRow(subtask: Todo, assigneeLabel: String?, onToggle: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 36.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckButton(checked = subtask.done, size = 20.dp, onClick = onToggle)
        TodoTexts(subtask.title, subtask.done, assigneeLabel, Modifier.weight(1f), SalimType.bodyMd)
    }
}

@Composable
private fun TodoTexts(
    title: String,
    done: Boolean,
    meta: String?,
    modifier: Modifier,
    titleStyle: androidx.compose.ui.text.TextStyle,
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            title,
            style = titleStyle.copy(
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
            ),
            color = if (done) SalimTokens.TextMuted else SalimTokens.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (meta != null) {
            Text(meta, style = SalimType.bodySm, color = SalimTokens.TextMuted)
        }
    }
}

@Composable
private fun CheckButton(checked: Boolean, size: Dp, onClick: () -> Unit) {
    val label = stringResource(if (checked) R.string.todo_uncheck else R.string.todo_check)
    IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = label }) {
        TodoCheckbox(checked = checked, size = size)
    }
}

/** 원형 체크. 완료 시 Coral 채움 + 흰 체크. (design.md 리스트 아이템 - 할 일) */
@Composable
private fun TodoCheckbox(checked: Boolean, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (checked) {
                    Modifier.background(SalimTokens.Accent)
                } else {
                    Modifier.border(2.dp, SalimTokens.TextMuted.copy(alpha = 0.6f), CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.66f))
        }
    }
}

@Composable
private fun DoneSectionHeader(count: Int, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.todo_done_section, count), style = SalimType.labelMd, color = SalimTokens.TextMuted)
        Icon(
            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(if (expanded) R.string.todo_collapse else R.string.todo_expand),
            tint = SalimTokens.TextMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = SalimType.bodyLg, color = SalimTokens.TextMuted)
    }
}

@Composable
private fun assigneeLabel(assignee: TodoAssignee, names: SpenderNames): String = when (assignee) {
    TodoAssignee.TOGETHER -> stringResource(R.string.todo_assignee_together)
    TodoAssignee.ME -> names.mine
    TodoAssignee.PARTNER -> names.partner
}

// ---------------------------------------------------------------------------
// 할 일 추가 / 수정 시트 (todo.md 11-2)
// ---------------------------------------------------------------------------

/** 시트의 하위 항목 한 줄. [key]는 새 줄에도 붙는 화면용 식별자, [id]는 저장된 항목일 때만. */
private data class SubtaskLine(
    val key: String,
    val id: String?,
    val title: String,
    val assignee: TodoAssignee,
)

/** 회전해도 입력 중인 하위 항목이 사라지지 않게 문자열 목록으로 풀어 저장한다. */
private val SubtaskLinesSaver = listSaver<SnapshotStateList<SubtaskLine>, String>(
    save = { lines -> lines.flatMap { listOf(it.key, it.id.orEmpty(), it.title, it.assignee.name) } },
    restore = { flat ->
        flat.chunked(4).map { (key, id, title, assignee) ->
            SubtaskLine(key, id.ifEmpty { null }, title, TodoAssignee.valueOf(assignee))
        }.toMutableStateList()
    },
)

private fun List<SubtaskLine>.toMutableStateList(): SnapshotStateList<SubtaskLine> =
    mutableStateListOf<SubtaskLine>().also { it.addAll(this) }

/** @param baseline 수정할 항목(시트를 연 시점). null이면 추가. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoEditSheet(
    baseline: TodoItem?,
    connected: Boolean,
    names: SpenderNames,
    onDismiss: () -> Unit,
    onSave: (title: String, assignee: TodoAssignee, drafts: List<SubtaskDraft>) -> Unit,
    onDelete: () -> Unit,
) {
    val isEdit = baseline != null
    var title by rememberSaveable { mutableStateOf(baseline?.todo?.title.orEmpty()) }
    var assignee by rememberSaveable { mutableStateOf(baseline?.todo?.assignee ?: TodoAssignee.TOGETHER) }
    val lines = rememberSaveable(saver = SubtaskLinesSaver) {
        baseline?.subtasks.orEmpty()
            .map { SubtaskLine(key = it.id, id = it.id, title = it.title, assignee = it.assignee) }
            .toMutableStateList()
    }
    // 방금 추가한 줄에 포커스를 준다.
    var focusKey by remember { mutableStateOf<String?>(null) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val canSave = title.isNotBlank()

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(if (isEdit) R.string.todo_edit else R.string.todo_add),
                        style = SalimType.titleLg,
                        color = SalimTokens.TextPrimary,
                    )
                    if (isEdit) {
                        IconButton(onClick = { confirmingDelete = true }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.todo_delete),
                                tint = SalimTokens.TextPrimary,
                            )
                        }
                    }
                }

                SheetTextField(
                    value = title,
                    onValueChange = { title = it },
                    hint = stringResource(R.string.todo_title_hint),
                    focusRequester = titleFocus,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                // 미연결이면 담당자 줄을 숨기고 본인으로 저장한다. (todo.md 11-2)
                if (connected) {
                    SheetSection(stringResource(R.string.todo_assignee)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TodoAssignee.entries.forEach { option ->
                                SalimChip(
                                    label = assigneeLabel(option, names),
                                    selected = assignee == option,
                                    onClick = { assignee = option },
                                )
                            }
                        }
                    }
                }

                SheetSection(stringResource(R.string.todo_subtasks)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        lines.forEachIndexed { index, line ->
                            SubtaskEditLine(
                                line = line,
                                connected = connected,
                                names = names,
                                requestFocus = focusKey == line.key,
                                onFocused = { focusKey = null },
                                onChange = { lines[index] = it },
                                onRemove = { lines.removeAt(index) },
                            )
                        }
                        // 다 차면 추가 버튼을 숨긴다.
                        if (lines.size < Todo.SUBTASK_MAX) {
                            Text(
                                stringResource(R.string.todo_subtask_add),
                                style = SalimType.bodyMd.copy(fontWeight = FontWeight.Medium),
                                color = SalimTokens.Accent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        // 담당자 기본값은 지금 고른 상위 항목 담당자.
                                        val line = SubtaskLine(UUID.randomUUID().toString(), null, "", assignee)
                                        lines.add(line)
                                        focusKey = line.key
                                    }
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }

            SaveButton(
                enabled = canSave,
                onClick = {
                    onSave(title, assignee, lines.map { SubtaskDraft(it.id, it.title, it.assignee) })
                },
                label = stringResource(if (isEdit) R.string.todo_edit_done else R.string.common_save),
            )
        }
    }

    // 추가할 때만 바로 입력하게 한다. 수정은 담당자·하위 항목만 바꾸려는 경우가 있어 키보드를 띄우지 않는다.
    if (!isEdit) {
        LaunchedEffect(Unit) { titleFocus.requestFocus() }
    }

    if (confirmingDelete) {
        val subtaskCount = baseline?.subtasks?.size ?: 0
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = {
                Text(stringResource(R.string.todo_delete_confirm), style = SalimType.titleLg, color = SalimTokens.TextPrimary)
            },
            text = {
                val body = stringResource(R.string.todo_delete_confirm_body)
                Text(
                    if (subtaskCount > 0) "$body ${stringResource(R.string.todo_delete_with_subtasks, subtaskCount)}" else body,
                    style = SalimType.bodyMd,
                    color = SalimTokens.TextMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.todo_delete), color = SalimTokens.Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text(stringResource(R.string.common_cancel), color = SalimTokens.TextMuted)
                }
            },
            containerColor = SalimTokens.CardSurface,
        )
    }
}

@Composable
private fun SheetSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = SalimType.labelMd, color = SalimTokens.TextMuted)
        content()
    }
}

@Composable
private fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    textStyle: androidx.compose.ui.text.TextStyle = SalimType.bodyLg,
    verticalPadding: Dp = 14.dp,
) {
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= Todo.TITLE_MAX_LENGTH) onValueChange(it) },
        textStyle = textStyle.copy(color = SalimTokens.TextPrimary),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        cursorBrush = SolidColor(SalimTokens.Accent),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SalimTokens.Background)
            .padding(horizontal = 16.dp, vertical = verticalPadding)
            .focusRequester(focusRequester),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(hint, style = textStyle, color = SalimTokens.TextMuted)
                }
                inner()
            }
        },
    )
}

/** 시트의 하위 항목 한 줄: 제목 입력 + 담당자 버튼(연결 시) + 지우기. 체크는 목록에서 한다. */
@Composable
private fun SubtaskEditLine(
    line: SubtaskLine,
    connected: Boolean,
    names: SpenderNames,
    requestFocus: Boolean,
    onFocused: () -> Unit,
    onChange: (SubtaskLine) -> Unit,
    onRemove: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    if (requestFocus) {
        LaunchedEffect(line.key) {
            focusRequester.requestFocus()
            onFocused()
        }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SheetTextField(
            value = line.title,
            onValueChange = { onChange(line.copy(title = it)) },
            hint = stringResource(R.string.todo_subtask_hint),
            focusRequester = focusRequester,
            textStyle = SalimType.bodyMd,
            verticalPadding = 12.dp,
            modifier = Modifier.weight(1f),
        )
        if (connected) {
            AssigneeMenuButton(
                selected = line.assignee,
                names = names,
                onSelect = { onChange(line.copy(assignee = it)) },
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.todo_subtask_remove),
                tint = SalimTokens.TextMuted,
            )
        }
    }
}

/** 지금 담당자 이름을 보여주고, 탭하면 우리 / 나 / 상대 메뉴를 연다. */
@Composable
private fun AssigneeMenuButton(selected: TodoAssignee, names: SpenderNames, onSelect: (TodoAssignee) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val changeLabel = stringResource(R.string.todo_assignee_change)
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(SalimTokens.AccentSoft)
                .clickable { open = true }
                .semantics { contentDescription = changeLabel }
                .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                assigneeLabel(selected, names),
                style = SalimType.bodySm.copy(fontWeight = FontWeight.Medium),
                color = SalimTokens.Accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 2.dp),
            )
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = SalimTokens.Accent, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = SalimTokens.CardSurface,
        ) {
            TodoAssignee.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            assigneeLabel(option, names),
                            style = SalimType.bodyMd.copy(
                                fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (option == selected) SalimTokens.Accent else SalimTokens.TextPrimary,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        open = false
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 프리뷰 (ViewModel 없이 TodoContent만)
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TodoScreenPreview() {
    SalimTheme {
        TodoContent(
            state = TodoUiState(
                loading = false,
                connected = true,
                sections = TodoSections(
                    open = listOf(
                        TodoItem(Todo("1", "우유 사기", TodoAssignee.TOGETHER, false, null, 1)),
                        TodoItem(
                            Todo("2", "여행 준비", TodoAssignee.TOGETHER, false, null, 2),
                            listOf(
                                Todo("2a", "여권 챙기기", TodoAssignee.ME, true, 5, 3, parentId = "2"),
                                Todo("2b", "숙소 예약", TodoAssignee.PARTNER, false, null, 4, parentId = "2"),
                            ),
                        ),
                    ),
                    done = listOf(TodoItem(Todo("4", "관리비 내기", TodoAssignee.ME, true, 10, 0))),
                ),
            ),
            modifier = Modifier.background(SalimTokens.Background),
            onToggle = { _, _ -> },
            onToggleSubtask = { _, _ -> },
            onItemClick = {},
        )
    }
}
