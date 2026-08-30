package com.chanbro.salim.ui.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.ui.common.BudgetInputSheet
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// 설정 (settings.md 7) — 탭 랜딩 화면
//
// 로그아웃은 구글 로그인(PRD 1)이, 연결 상태 카드는 상대방 연결(PRD 9)이 붙으면서 동작한다.
// 연결 해제와 회원탈퇴는 30일 유예 삭제에 서버 작업이 필요해 아직 비활성으로만 노출한다.
// 동작하는 것처럼 보이지 않도록 muted 처리하고 탭도 막는다.
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {},
    onConnectClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showBudgetSheet by rememberSaveable { mutableStateOf(false) }
    var confirmingSignOut by rememberSaveable { mutableStateOf(false) }

    SettingsContent(
        state = state,
        modifier = modifier,
        onProfileClick = onProfileClick,
        onConnectClick = onConnectClick,
        onBudgetClick = { showBudgetSheet = true },
        onSignOutClick = { confirmingSignOut = true },
    )

    if (confirmingSignOut) {
        SignOutConfirmDialog(
            onConfirm = {
                confirmingSignOut = false
                viewModel.onSignOut()
            },
            onDismiss = { confirmingSignOut = false },
        )
    }

    if (showBudgetSheet) {
        BudgetInputSheet(
            year = state.year,
            month = state.month,
            initialAmount = state.budgetAmount,
            onDismiss = { showBudgetSheet = false },
            onConfirm = { amount ->
                viewModel.setBudget(amount)
                showBudgetSheet = false
            },
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit,
    onConnectClick: () -> Unit,
    onBudgetClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SettingsTopBar()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ConnectionCard(state.connection, onConnectClick)

            SettingsGroup("일반") {
                SettingsRow(
                    label = "프로필 수정",
                    value = state.profileText,
                    onClick = onProfileClick,
                )
                RowDivider()
                SettingsRow(
                    label = "가계부 카테고리 수정",
                    value = "준비 중",
                    enabled = false,
                )
                RowDivider()
                SettingsRow(
                    label = "달별 예산 설정",
                    value = state.budgetText,
                    onClick = onBudgetClick,
                )
                RowDivider()
                SettingsRow(
                    label = "알림 설정",
                    value = "준비 중",
                    enabled = false,
                )
            }

            SettingsGroup("연결") {
                SettingsRow(label = "연결 해제", value = "준비 중", enabled = false)
            }

            // 실수로 누르지 않도록 목록과 시각적으로 분리 (settings.md 5.)
            Column(
                // fillMaxWidth가 없으면 Column이 콘텐츠 폭으로 줄어들어 가운데 정렬이 먹지 않는다.
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.settings_sign_out),
                    style = SalimType.bodyMd,
                    color = SalimTokens.TextPrimary,
                    modifier = Modifier
                        .clickable(onClick = onSignOutClick)
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                )
                // 회원탈퇴는 유예기간(30일) 처리가 필요해 아직 비활성 (PRD 9.)
                Text(
                    text = stringResource(R.string.settings_withdraw),
                    style = SalimType.labelSm,
                    color = SalimTokens.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun SignOutConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.settings_sign_out_confirm), style = SalimType.titleLg)
        },
        text = {
            Text(
                stringResource(R.string.settings_sign_out_confirm_body),
                style = SalimType.bodyMd,
                color = SalimTokens.TextMuted,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.common_confirm), color = SalimTokens.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel), color = SalimTokens.TextMuted)
            }
        },
        containerColor = SalimTokens.CardSurface,
    )
}

@Composable
private fun SettingsTopBar() {
    Surface(color = SalimTokens.Background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.settings_title),
                style = SalimType.headlineSm,
                color = SalimTokens.TextPrimary,
            )
        }
    }
}

/** 연결 상태 카드. 탭하면 연결 관리 화면으로 간다. (settings.md 2 / connect.md 9-1) */
@Composable
private fun ConnectionCard(connection: Connection, onClick: () -> Unit) {
    val connected = connection as? Connection.Connected
    SalimCard(modifier = Modifier.clickable(onClick = onClick), cornerRadius = 24.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SalimTokens.AccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.People,
                    contentDescription = null,
                    tint = SalimTokens.Accent,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = connected?.let {
                        stringResource(R.string.connect_connected, it.partner.nameOrDefault)
                    } ?: stringResource(R.string.home_connect_banner_title),
                    style = SalimType.bodyLg,
                    color = SalimTokens.TextPrimary,
                )
                Text(
                    text = connected?.let {
                        stringResource(R.string.connect_connected_since, formatConnectedAt(it.connectedAtMillis))
                    } ?: stringResource(R.string.home_connect_banner_body),
                    style = SalimType.bodySm,
                    color = SalimTokens.TextMuted,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = SalimTokens.TextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun formatConnectedAt(millis: Long): String =
    SimpleDateFormat("yyyy.MM.dd", Locale.KOREAN).format(Date(millis))

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = SalimType.labelMd, color = SalimTokens.TextMuted)
        SalimCard(cornerRadius = 20.dp, contentPadding = 4.dp) { content() }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = SalimType.bodyLg,
            color = if (enabled) SalimTokens.TextPrimary else SalimTokens.TextMuted,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, style = SalimType.bodyMd, color = SalimTokens.TextMuted)
            if (enabled) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = SalimTokens.TextMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(SalimTokens.Divider),
    )
}

// ---------------------------------------------------------------------------
// 프리뷰
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SettingsScreenPreview() {
    SalimTheme {
        SettingsContent(
            state = SettingsUiState(year = 2026, month = 8, budgetAmount = 1_200_000),
            modifier = Modifier.background(SalimTokens.Background),
            onProfileClick = {},
            onConnectClick = {},
            onBudgetClick = {},
            onSignOutClick = {},
        )
    }
}
