package com.chanbro.salim.ui.wedding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.DDayBadge
import com.chanbro.salim.ui.common.SalimCard
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.dday.dDayLabel
import com.chanbro.salim.ui.dday.daysUntil

/**
 * 홈 결혼 준비 카드 (home.md 2-2). 설정 > 결혼 준비를 켠 경우에만 보인다.
 * 상단 월 선택과 무관하게 항상 전체 기준이다.
 */
@Composable
fun WeddingHomeCard(onClick: () -> Unit, viewModel: WeddingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.loading || !state.enabled) return
    val overview = state.overview
    val date = overview.settings.weddingDateMillis

    SalimCard(cornerRadius = 24.dp, contentPadding = 0.dp, modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = SalimTokens.Accent, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.wedding_title),
                style = SalimType.headlineSm,
                color = SalimTokens.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (date != null) {
                DDayBadge(dDayLabel(daysUntil(date, state.todayMillis)))
            } else {
                Text(stringResource(R.string.wedding_date_undecided), style = SalimType.labelMd, color = SalimTokens.TextMuted)
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryCell(
                label = stringResource(R.string.wedding_checklist),
                value = stringResource(R.string.wedding_checklist_count, overview.doneTaskCount, overview.taskCount),
                ratio = if (overview.taskCount > 0) overview.doneTaskCount.toFloat() / overview.taskCount else 0f,
                modifier = Modifier.weight(1f),
            )
            // 가계부 예산과 헷갈리지 않게 라벨은 반드시 "웨딩 예산" (home.md 2-2)
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
            )
        }
    }
}
