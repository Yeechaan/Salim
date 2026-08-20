package com.chanbro.salim.ui.dday

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
import androidx.compose.runtime.getValue
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
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.DatePickerModal
import com.chanbro.salim.ui.common.FieldDivider
import com.chanbro.salim.ui.common.FieldRow
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.common.SaveButton
import com.chanbro.salim.ui.common.formatDate
import com.chanbro.salim.ui.common.todayUtcMillis

// ---------------------------------------------------------------------------
// 디데이 추가 / 수정 (dday.md 6-2, 6-3) — 전체 화면 목적지
// 두 화면은 레이아웃이 같아 initial 유무로 모드를 가른다.
// 자동 반영 항목(생일/기념일)은 여기 진입하지 않는다 — 설정 > 프로필에서만 수정 (PRD 6.)
// ---------------------------------------------------------------------------

@Composable
fun DDayInputScreen(
    onClose: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    initial: DDayEntry? = null,
    onDelete: () -> Unit = {},
) {
    val isEdit = initial != null

    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var dateMillis by rememberSaveable { mutableLongStateOf(initial?.dateMillis ?: todayUtcMillis()) }
    var repeatYearly by rememberSaveable { mutableStateOf(initial?.repeatYearly ?: false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    val canSave = title.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SalimTokens.Background),
    ) {
        DDayInputTopBar(
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
                RepeatToggleRow(checked = repeatYearly, onCheckedChange = { repeatYearly = it })
            }
        }

        SaveButton(
            enabled = canSave,
            onClick = onSave,
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
    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            onConfirm = { showDeleteConfirm = false; onDelete() },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun DDayInputTopBar(isEdit: Boolean, onClose: () -> Unit, onDeleteClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
            // 추가는 닫기(X), 수정은 뒤로가기 (dday.md 6-2 / 6-3)
            if (isEdit) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = SalimTokens.TextPrimary)
            } else {
                Icon(Icons.Filled.Close, contentDescription = "닫기", tint = SalimTokens.TextPrimary)
            }
        }
        Text(
            if (isEdit) "디데이 수정" else "디데이 추가",
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
                    Text("예) 제주 여행", style = SalimType.bodyLg, color = SalimTokens.TextMuted)
                }
                inner()
            },
        )
    }
}

@Composable
private fun RepeatToggleRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("매년 반복", style = SalimType.bodyMd, color = SalimTokens.TextMuted)
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
private fun DeleteConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("디데이를 삭제할까요?", style = SalimType.titleLg, color = SalimTokens.TextPrimary) },
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
private fun DDayInputScreenAddPreview() {
    SalimTheme {
        DDayInputScreen(onClose = {}, onSave = {})
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DDayInputScreenEditPreview() {
    SalimTheme {
        DDayInputScreen(
            onClose = {},
            onSave = {},
            initial = DDayEntry("제주 여행", utcDate(2026, 9, 15)),
        )
    }
}
