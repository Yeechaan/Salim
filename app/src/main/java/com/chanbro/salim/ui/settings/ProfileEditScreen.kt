package com.chanbro.salim.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.domain.model.UserProfile
import com.chanbro.salim.ui.common.DatePickerModal
import com.chanbro.salim.ui.common.FieldDivider
import com.chanbro.salim.ui.common.FieldRow
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.common.SaveButton
import com.chanbro.salim.ui.common.formatDate
import com.chanbro.salim.ui.common.todayUtcMillis

// ---------------------------------------------------------------------------
// 프로필 수정 (settings.md 3 "프로필 수정")
// 여기 입력한 생일/기념일이 디데이 탭에 AUTO 항목으로 자동 반영된다 (PRD 6.).
// ---------------------------------------------------------------------------

@Composable
fun ProfileEditScreen(
    onClose: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    // 로딩 전에는 그리지 않는다 (빈 값이 잠깐 보이는 것 방지).
    val loaded = profile ?: return

    ProfileEditContent(
        initial = loaded,
        onClose = onClose,
        onSave = { birthday, anniversary -> viewModel.save(birthday, anniversary, onDone) },
        modifier = modifier,
    )
}

@Composable
private fun ProfileEditContent(
    initial: UserProfile,
    onClose: () -> Unit,
    onSave: (birthdayMillis: Long?, anniversaryMillis: Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var birthday by rememberSaveable { mutableStateOf(initial.birthdayMillis) }
    var anniversary by rememberSaveable { mutableStateOf(initial.anniversaryMillis) }
    var picking by rememberSaveable { mutableStateOf<PickTarget?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SalimTokens.Background),
    ) {
        ProfileTopBar(onClose)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SalimCard(cornerRadius = 20.dp, modifier = Modifier.padding(top = 16.dp)) {
                DateFieldRow(
                    label = "생일",
                    millis = birthday,
                    onClick = { picking = PickTarget.BIRTHDAY },
                    onClear = { birthday = null },
                )
                FieldDivider()
                DateFieldRow(
                    label = "기념일",
                    millis = anniversary,
                    onClick = { picking = PickTarget.ANNIVERSARY },
                    onClear = { anniversary = null },
                )
            }
            Text(
                "입력한 생일과 기념일은 디데이 탭에 자동으로 표시돼요.",
                style = SalimType.bodySm,
                color = SalimTokens.TextMuted,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        SaveButton(
            enabled = true,
            onClick = { onSave(birthday, anniversary) },
            label = "저장하기",
        )
    }

    val target = picking
    if (target != null) {
        val current = when (target) {
            PickTarget.BIRTHDAY -> birthday
            PickTarget.ANNIVERSARY -> anniversary
        }
        DatePickerModal(
            initialMillis = current ?: todayUtcMillis(),
            onConfirm = { picked ->
                when (target) {
                    PickTarget.BIRTHDAY -> birthday = picked
                    PickTarget.ANNIVERSARY -> anniversary = picked
                }
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

private enum class PickTarget { BIRTHDAY, ANNIVERSARY }

@Composable
private fun ProfileTopBar(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = SalimTokens.TextPrimary)
        }
        Text("프로필 수정", style = SalimType.titleLg, color = SalimTokens.TextPrimary)
    }
}

/** 미입력 상태를 허용해야 해서 값 우측에 지우기를 둔다. */
@Composable
private fun DateFieldRow(
    label: String,
    millis: Long?,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    Column {
        FieldRow(
            label = label,
            value = millis?.let(::formatDate) ?: "미입력",
            onClick = onClick,
        )
        if (millis != null) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onClear) {
                    Text("지우기", style = SalimType.labelSm, color = SalimTokens.TextMuted)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 프리뷰
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProfileEditScreenPreview() {
    SalimTheme {
        ProfileEditContent(
            initial = UserProfile(birthdayMillis = todayUtcMillis(), anniversaryMillis = null),
            onClose = {},
            onSave = { _, _ -> },
        )
    }
}
