package com.chanbro.salim.ui.settings

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.R
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
// 이름은 가계부 지출자 표시에(PRD 4.), 생일/기념일은 디데이(홈 카드 / 디데이 관리)에 AUTO 항목으로 반영된다(PRD 6.).
// ---------------------------------------------------------------------------

/** 지출자 칩 한 줄에 두 이름이 들어가야 해서 길이를 제한한다. */
private const val NAME_MAX_LENGTH = 12

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
        onSave = { name, birthday, anniversary -> viewModel.save(name, birthday, anniversary, onDone) },
        modifier = modifier,
    )
}

@Composable
private fun ProfileEditContent(
    initial: UserProfile,
    onClose: () -> Unit,
    onSave: (displayName: String?, birthdayMillis: Long?, anniversaryMillis: Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf(initial.displayName.orEmpty()) }
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
                NameFieldRow(name = name, onNameChange = { name = it })
                FieldDivider()
                DateFieldRow(
                    label = stringResource(R.string.profile_birthday),
                    millis = birthday,
                    onClick = { picking = PickTarget.BIRTHDAY },
                    onClear = { birthday = null },
                )
                FieldDivider()
                DateFieldRow(
                    label = stringResource(R.string.profile_anniversary),
                    millis = anniversary,
                    onClick = { picking = PickTarget.ANNIVERSARY },
                    onClear = { anniversary = null },
                )
            }
            Text(
                stringResource(R.string.profile_guide),
                style = SalimType.bodySm,
                color = SalimTokens.TextMuted,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        SaveButton(
            enabled = true,
            onClick = { onSave(name.trim().ifBlank { null }, birthday, anniversary) },
            label = stringResource(R.string.common_save),
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
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                tint = SalimTokens.TextPrimary,
            )
        }
        Text(stringResource(R.string.profile_title), style = SalimType.titleLg, color = SalimTokens.TextPrimary)
    }
}

/** 날짜 줄과 같은 모양을 유지하려고 값 자리에 우측 정렬 입력을 둔다. */
@Composable
private fun NameFieldRow(name: String, onNameChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.profile_name), style = SalimType.bodyMd, color = SalimTokens.TextMuted)
        BasicTextField(
            value = name,
            onValueChange = { onNameChange(it.take(NAME_MAX_LENGTH)) },
            textStyle = SalimType.bodyLg.copy(color = SalimTokens.TextPrimary, textAlign = TextAlign.End),
            singleLine = true,
            cursorBrush = SolidColor(SalimTokens.Accent),
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (name.isEmpty()) {
                        Text(
                            stringResource(R.string.profile_name_hint),
                            style = SalimType.bodyLg,
                            color = SalimTokens.TextMuted,
                        )
                    }
                    inner()
                }
            },
        )
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
            value = millis?.let(::formatDate) ?: stringResource(R.string.profile_empty),
            onClick = onClick,
        )
        if (millis != null) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.profile_clear), style = SalimType.labelSm, color = SalimTokens.TextMuted)
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
            initial = UserProfile(
                displayName = "해리",
                birthdayMillis = todayUtcMillis(),
                anniversaryMillis = null,
            ),
            onClose = {},
            onSave = { _, _, _ -> },
        )
    }
}
