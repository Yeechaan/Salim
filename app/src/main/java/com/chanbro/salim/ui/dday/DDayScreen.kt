package com.chanbro.salim.ui.dday

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.DDayBadge
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimType

// ---------------------------------------------------------------------------
// 디데이 관리 (dday.md 6-1) — 설정 하위 화면. 진입: 설정 "디데이 관리", 홈 디데이 카드
// 탭 화면이 아니라 상위 Scaffold의 FAB가 없으므로 추가 버튼을 직접 그린다.
// ---------------------------------------------------------------------------

@Composable
fun DDayScreen(
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    onItemClick: (DDayRowUi) -> Unit = {},
    viewModel: DDayListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DDayContent(state = state, modifier = modifier, onBack = onBack, onAddClick = onAddClick, onItemClick = onItemClick)
}

@Composable
private fun DDayContent(
    state: DDayListUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (DDayRowUi) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DDayTopBar(onBack)
            if (state.rows.isEmpty()) {
                EmptyState()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        // FAB에 마지막 줄이 가리지 않게 아래 여백을 넉넉히 둔다.
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
                ) {
                    SalimCard(cornerRadius = 24.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            state.rows.forEachIndexed { index, row ->
                                DDayItemRow(row = row, onClick = { onItemClick(row) })
                                if (index != state.rows.lastIndex) {
                                    RowDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAddClick,
            containerColor = SalimTokens.Accent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.dday_add))
        }
    }
}

/** 설정 하위 화면 공통 상단 — 카테고리 수정과 같은 형태. */
@Composable
private fun DDayTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                tint = SalimTokens.TextPrimary,
            )
        }
        Text(stringResource(R.string.dday_manage_title), style = SalimType.titleLg, color = SalimTokens.TextPrimary)
    }
}

@Composable
private fun DDayItemRow(row: DDayRowUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DDayBadge(row.dDayText)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.title, style = SalimType.bodyLg, color = SalimTokens.TextPrimary)
                // 자동 반영 항목(생일/기념일)은 이 탭에서 수정 불가 — 출처를 라벨로 구분 (PRD 6.)
                if (row.isAuto) AutoTag()
            }
            Text(
                if (row.repeatYearly) "${row.dateText} · 매년 반복" else row.dateText,
                style = SalimType.bodySm,
                color = SalimTokens.TextMuted,
            )
        }
    }
}

/** 자동 반영 항목 표시용 옅은 태그. SalimChip은 clickable 전용이라 비클릭 축소판으로 둔다. */
@Composable
private fun AutoTag() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(SalimTokens.AccentSoft)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text("자동", style = SalimType.labelSm, color = SalimTokens.Accent)
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SalimTokens.Divider),
    )
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.dday_empty), style = SalimType.bodyLg, color = SalimTokens.TextMuted)
    }
}

// ---------------------------------------------------------------------------
// 프리뷰 (ViewModel 없이 DDayContent만)
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DDayScreenPreview() {
    SalimTheme {
        DDayContent(
            state = DDayListUiState(
                rows = listOf(
                    DDayRowUi("1", "결혼기념일", "2026.08.31", "D-11", isAuto = true, repeatYearly = true),
                    DDayRowUi("2", "제주 여행", "2026.09.15", "D-26", isAuto = false, repeatYearly = false),
                    DDayRowUi("3", "이사", "2026.11.20", "D-92", isAuto = false, repeatYearly = false),
                ),
            ),
            modifier = Modifier.background(SalimTokens.Background),
            onBack = {},
            onAddClick = {},
            onItemClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DDayScreenEmptyPreview() {
    SalimTheme {
        DDayContent(
            state = DDayListUiState(),
            modifier = Modifier.background(SalimTokens.Background),
            onBack = {},
            onAddClick = {},
            onItemClick = {},
        )
    }
}
