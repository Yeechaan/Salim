package com.chanbro.salim.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.width
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.chanbro.salim.DEEP_LINK_HOME
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.currentYearMonth
import com.chanbro.salim.ui.common.formatWon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * 홈 화면 예산 위젯 — 소형 (PRD 10-2, wireframe/widget.md 10-2).
 * 이번 달 남은 예산을 보여주고, 탭하면 홈 탭으로 간다.
 */
class BudgetWidget : GlanceAppWidget() {

    override val sizeMode = WIDGET_SIZE_MODE

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        scheduleMidnightRefresh(context)
        val entryPoint = context.widgetEntryPoint()
        entryPoint.updater().ensureWatching(this)
        val states = stateFlow(entryPoint)
        val initial = loadForWidget(TAG) { states.first() } ?: unavailable()
        provideContent {
            // 세션이 열려 있는 동안(최대 수십 초) update()는 provideGlance를 다시 부르지 않고 이 컴포지션만
            // 다시 그린다. 그래서 첫 값만 읽지 않고 계속 구독해야 그 사이의 변경(토글 등)이 반영된다.
            val state by remember { states }.collectAsState(initial)
            BudgetWidgetContent(state)
        }
    }

    private fun stateFlow(entryPoint: WidgetEntryPoint): Flow<BudgetWidgetState> =
        combine(
            budgetSummary(entryPoint.observeMonthExpenses(), entryPoint.observeBudget()),
            entryPoint.observeWidgetShowAmount()(),
        ) { summary, showAmount -> BudgetWidgetState.Loaded(summary, showAmount) as BudgetWidgetState }
            .catch { emit(unavailable()) }

    private fun unavailable() = BudgetWidgetState.Unavailable(month = currentYearMonth().second)

    private companion object {
        const val TAG = "BudgetWidget"
    }
}

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.widgetEntryPoint().updater().stop(glanceAppWidget)
    }
}

internal sealed interface BudgetWidgetState {
    val month: Int

    data class Loaded(val summary: BudgetSummary, val showAmount: Boolean) : BudgetWidgetState {
        override val month: Int get() = summary.month
    }

    data class Unavailable(override val month: Int) : BudgetWidgetState
}

@Composable
private fun BudgetWidgetContent(state: BudgetWidgetState) {
    val context = LocalContext.current
    val onClick = openAppIntent(context, DEEP_LINK_HOME)
    val title = context.getString(R.string.widget_budget_title, state.month)
    // 표시할 요약. null이면 대신 띄울 문구가 있다 (불러오기 실패 / 예산 미설정)
    val summary = (state as? BudgetWidgetState.Loaded)?.summary?.takeIf { it.hasBudget }
    val message = when {
        state is BudgetWidgetState.Unavailable -> context.widgetUnavailableText()
        summary == null -> context.getString(R.string.widget_budget_empty)
        else -> null
    }
    val showAmount = (state as? BudgetWidgetState.Loaded)?.showAmount ?: true

    if (isCompact()) {
        // 가로형 (widget.md 10-2 "가로형"). 2칸 폭은 좁아 라벨과 금액을 한 줄에 두면 라벨이 잘린다 —
        // 라벨 / 금액 + 상태 / 진행률 바 세 줄로 쌓는다 (한 칸 높이에 들어간다).
        WidgetCompactCard(R.drawable.widget_background, onClick) {
            WidgetLabel(R.drawable.ic_widget_wallet, title, SalimTokens.Accent, SalimTokens.TextMuted)
            Spacer(GlanceModifier.height(4.dp))
            if (summary == null) {
                WidgetMessage(message.orEmpty(), SalimTokens.TextMuted)
            } else {
                val texts = budgetTexts(context, summary, showAmount)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = texts.headline,
                        maxLines = 1,
                        style = TextStyle(color = ColorProvider(texts.headlineColor), fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = texts.status,
                        style = TextStyle(color = ColorProvider(texts.statusColor), fontSize = 12.sp),
                    )
                }
                Spacer(GlanceModifier.height(6.dp))
                BudgetProgress(summary)
            }
        }
        return
    }

    WidgetCard(
        background = R.drawable.widget_background,
        onClickIntent = onClick,
        header = {
            WidgetLabel(
                icon = R.drawable.ic_widget_wallet,
                text = title,
                iconColor = SalimTokens.Accent,
                textColor = SalimTokens.TextMuted,
            )
        },
    ) {
        if (summary == null) {
            WidgetMessage(message.orEmpty(), SalimTokens.TextMuted)
        } else {
            BudgetBody(summary, showAmount)
        }
    }
}

@Composable
private fun BudgetBody(summary: BudgetSummary, showAmount: Boolean) {
    val context = LocalContext.current
    val texts = budgetTexts(context, summary, showAmount)
    Text(
        text = texts.headline,
        maxLines = 1,
        style = TextStyle(color = ColorProvider(texts.headlineColor), fontSize = 22.sp, fontWeight = FontWeight.Bold),
    )
    Text(
        text = texts.status,
        style = TextStyle(color = ColorProvider(texts.statusColor), fontSize = 12.sp),
    )
    Spacer(GlanceModifier.height(8.dp))
    BudgetProgress(summary)
    if (showAmount) {
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = context.getString(R.string.widget_budget_used_percent, summary.usedPercent),
            style = TextStyle(color = ColorProvider(SalimTokens.TextMuted), fontSize = 12.sp),
        )
    }
}

/** Glance 진행률 바는 그라데이션을 못 그려 단색이다. 초과면 경고색으로 가득. */
@Composable
private fun BudgetProgress(summary: BudgetSummary) {
    LinearProgressIndicator(
        progress = summary.progress,
        color = ColorProvider(if (summary.isOver) SalimTokens.Warning else SalimTokens.Accent),
        backgroundColor = ColorProvider(SalimTokens.ProgressTrack),
        modifier = GlanceModifier.fillMaxWidth().height(6.dp),
    )
}

/** 대표 숫자 + 상태 문구. 두 크기가 같은 규칙을 쓴다. */
private class BudgetTexts(
    val headline: String,
    val headlineColor: Color,
    val status: String,
    val statusColor: Color,
)

private fun budgetTexts(context: Context, summary: BudgetSummary, showAmount: Boolean): BudgetTexts {
    val over = summary.isOver
    return BudgetTexts(
        // 금액 가리기면 사용률이 대표 숫자 자리를 차지한다 (widget.md 10-2 "금액 가리기")
        headline = if (showAmount) formatWon(summary.remainOrOver)
        else context.getString(R.string.widget_budget_percent, summary.usedPercent),
        headlineColor = when {
            over -> SalimTokens.Warning
            showAmount -> SalimTokens.TextPrimary
            else -> SalimTokens.Accent
        },
        status = context.getString(
            when {
                over -> R.string.widget_budget_over
                showAmount -> R.string.widget_budget_remain
                else -> R.string.widget_budget_used
            },
        ),
        statusColor = if (over) SalimTokens.Warning else SalimTokens.TextMuted,
    )
}
