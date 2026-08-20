package com.chanbro.salim.ui.schedule

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.domain.model.ScheduleType
import com.chanbro.salim.ui.common.DatePickerModal
import com.chanbro.salim.ui.common.FieldDivider
import com.chanbro.salim.ui.common.FieldRow
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimChip
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.common.SaveButton
import com.chanbro.salim.ui.common.TimePickerModal
import com.chanbro.salim.ui.common.formatDate

private const val DEFAULT_MINUTE_OF_DAY = 9 * 60 // 종일을 끌 때 기본 시각 오전 9:00

// ---------------------------------------------------------------------------
// 일정 등록 / 수정 (schedule.md 5-2, 5-3) — 전체 화면 목적지
// 두 화면은 레이아웃이 같아 scheduleId 유무로 모드를 가른다.
// ---------------------------------------------------------------------------

@Composable
fun ScheduleInputScreen(
    onClose: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    scheduleId: String? = null,
    defaultDateMillis: Long = todayUtc(),
    viewModel: ScheduleInputViewModel = hiltViewModel(),
) {
    LaunchedEffect(scheduleId) { viewModel.load(scheduleId) }
    val initial by viewModel.initial.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    // 프리필을 받기 전에는 그리지 않는다 (빈 입력값이 잠깐 보이는 것 방지).
    if (loading) return
    if (scheduleId != null && initial == null) return

    ScheduleInputContent(
        isEdit = scheduleId != null,
        initial = initial,
        defaultDateMillis = defaultDateMillis,
        onClose = onClose,
        onSave = { title, dateMillis, minuteOfDay, type ->
            viewModel.save(title, dateMillis, minuteOfDay, type, onDone)
        },
        onDelete = { viewModel.delete(onDone) },
        modifier = modifier,
    )
}

@Composable
private fun ScheduleInputContent(
    isEdit: Boolean,
    initial: ScheduleInitial?,
    defaultDateMillis: Long,
    onClose: () -> Unit,
    onSave: (title: String, dateMillis: Long, minuteOfDay: Int?, type: ScheduleType) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var dateMillis by rememberSaveable { mutableLongStateOf(initial?.dateMillis ?: defaultDateMillis) }
    var allDay by rememberSaveable { mutableStateOf(initial?.minuteOfDay == null && initial != null) }
    var minuteOfDay by rememberSaveable {
        mutableIntStateOf(initial?.minuteOfDay ?: DEFAULT_MINUTE_OF_DAY)
    }
    var type by rememberSaveable { mutableStateOf(initial?.type ?: ScheduleType.SHARED) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    val canSave = title.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SalimTokens.Background),
    ) {
        ScheduleInputTopBar(
            isEdit = isEdit,
            onClose = onClose,
            onDeleteClick = { showDeleteConfirm = true },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            SalimCard(cornerRadius = 20.dp, modifier = Modifier.padding(top = 16.dp)) {
                TitleField(title = title, onTitleChange = { title = it })
                FieldDivider()
                FieldRow(
                    label = "날짜",
                    value = formatDate(dateMillis),
                    onClick = { showDatePicker = true },
                )
                FieldDivider()
                AllDayToggleRow(checked = allDay, onCheckedChange = { allDay = it })
                // 종일이면 시간 입력을 숨긴다.
                if (!allDay) {
                    FieldDivider()
                    FieldRow(
                        label = "시간",
                        value = formatMinuteOfDay(minuteOfDay),
                        onClick = { showTimePicker = true },
                    )
                }
                FieldDivider()
                TypeField(selected = type, onSelect = { type = it })
            }
        }

        SaveButton(
            enabled = canSave,
            onClick = { onSave(title, dateMillis, if (allDay) null else minuteOfDay, type) },
            label = if (isEdit) "수정 완료" else "저장하기",
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
            initialHour = minuteOfDay / 60,
            initialMinute = minuteOfDay % 60,
            onConfirm = { h, m -> minuteOfDay = h * 60 + m; showTimePicker = false },
            onDismiss = { showTimePicker = false },
        )
    }
    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            onConfirm = { showDeleteConfirm = false; onDelete() },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun ScheduleInputTopBar(isEdit: Boolean, onClose: () -> Unit, onDeleteClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
            // 등록은 닫기(X), 수정은 뒤로가기 (schedule.md 5-2 / 5-3)
            if (isEdit) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = SalimTokens.TextPrimary)
            } else {
                Icon(Icons.Filled.Close, contentDescription = "닫기", tint = SalimTokens.TextPrimary)
            }
        }
        Text(
            if (isEdit) "일정 수정" else "일정 등록",
            style = SalimType.titleLg,
            color = SalimTokens.TextPrimary,
        )
        if (isEdit) {
            IconButton(onClick = onDeleteClick, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Outlined.Delete, contentDescription = "삭제", tint = SalimTokens.TextMuted)
            }
        }
    }
}

@Composable
private fun TitleField(title: String, onTitleChange: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("제목", style = SalimType.bodyMd, color = SalimTokens.TextMuted)
        BasicTextField(
            value = title,
            onValueChange = { onTitleChange(it.take(30)) },
            textStyle = SalimType.bodyLg.copy(color = SalimTokens.TextPrimary),
            singleLine = true,
            cursorBrush = SolidColor(SalimTokens.Accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (title.isEmpty()) {
                    Text("예) 저녁 약속", style = SalimType.bodyLg, color = SalimTokens.TextMuted)
                }
                inner()
            },
        )
    }
}

@Composable
private fun AllDayToggleRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("종일", style = SalimType.bodyMd, color = SalimTokens.TextMuted)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SalimTokens.Accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = SalimTokens.ProgressTrack,
                uncheckedBorderColor = SalimTokens.ProgressTrack,
            ),
        )
    }
}

@Composable
private fun TypeField(selected: ScheduleType, onSelect: (ScheduleType) -> Unit) {
    Column(
        modifier = Modifier.padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("유형", style = SalimType.bodyMd, color = SalimTokens.TextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ScheduleType.entries.forEach { option ->
                SalimChip(
                    label = when (option) {
                        ScheduleType.SHARED -> "우리"
                        ScheduleType.MINE -> "나"
                        ScheduleType.PARTNER -> "배우자"
                    },
                    selected = option == selected,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("일정을 삭제할까요?", style = SalimType.titleLg, color = SalimTokens.TextPrimary) },
        text = { Text("삭제하면 되돌릴 수 없어요.", style = SalimType.bodyMd, color = SalimTokens.TextMuted) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("삭제", color = SalimTokens.Accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = SalimTokens.TextMuted) }
        },
        containerColor = SalimTokens.CardSurface,
    )
}

// ---------------------------------------------------------------------------
// 프리뷰
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ScheduleInputAddPreview() {
    SalimTheme {
        ScheduleInputContent(
            isEdit = false,
            initial = null,
            defaultDateMillis = todayUtc(),
            onClose = {},
            onSave = { _, _, _, _ -> },
            onDelete = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ScheduleInputEditPreview() {
    SalimTheme {
        ScheduleInputContent(
            isEdit = true,
            initial = ScheduleInitial("저녁 약속", todayUtc(), 19 * 60, ScheduleType.SHARED),
            defaultDateMillis = todayUtc(),
            onClose = {},
            onSave = { _, _, _, _ -> },
            onDelete = {},
        )
    }
}
