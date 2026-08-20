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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.DDayBadge
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimType

// ---------------------------------------------------------------------------
// 디데이 리스트 (dday.md 6-1) — 탭 랜딩 화면
// 하단 탭바/FAB는 상위 Scaffold가 제공, 여기서는 콘텐츠만
// ---------------------------------------------------------------------------

@Composable
fun DDayScreen(
    modifier: Modifier = Modifier,
    onItemClick: (DDayRowUi) -> Unit = {},
    viewModel: DDayListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DDayContent(state = state, modifier = modifier, onItemClick = onItemClick)
}

@Composable
private fun DDayContent(
    state: DDayListUiState,
    modifier: Modifier = Modifier,
    onItemClick: (DDayRowUi) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        DDayTopBar()
        if (state.rows.isEmpty()) {
            EmptyState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
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
}

@Composable
private fun DDayTopBar() {
    Surface(color = SalimTokens.Background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("디데이", style = SalimType.headlineSm, color = SalimTokens.TextPrimary)
        }
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
        Text("디데이를 추가해보세요", style = SalimType.bodyLg, color = SalimTokens.TextMuted)
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
            onItemClick = {},
        )
    }
}
