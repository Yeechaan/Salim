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
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.width
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.chanbro.salim.DEEP_LINK_DDAY_MANAGE
import com.chanbro.salim.R
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.dday.DDayRowUi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 홈 화면 디데이 위젯 — 소형 (PRD 10-1, wireframe/widget.md 10-1).
 * 가장 가까운 디데이 1건을 보여주고, 탭하면 설정 > 디데이 관리로 간다.
 */
class DDayWidget : GlanceAppWidget() {

    override val sizeMode = WIDGET_SIZE_MODE

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        scheduleMidnightRefresh(context)
        val entryPoint = context.widgetEntryPoint()
        entryPoint.updater().ensureWatching(this)
        val states = nearestDDay(entryPoint.observeDDays(), entryPoint.observeProfile())
            .map { row -> row?.let { DDayWidgetState.Upcoming(it) } ?: DDayWidgetState.Empty }
            .catch { emit(DDayWidgetState.Unavailable) }
        val initial = loadForWidget(TAG) { states.first() } ?: DDayWidgetState.Unavailable
        provideContent {
            // 세션이 열려 있는 동안 update()는 provideGlance를 다시 부르지 않는다 — 계속 구독해 반영한다 (BudgetWidget 참고)
            val state by remember { states }.collectAsState(initial)
            DDayWidgetContent(state)
        }
    }

    private companion object {
        const val TAG = "DDayWidget"
    }
}

class DDayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DDayWidget()

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.widgetEntryPoint().updater().stop(glanceAppWidget)
    }
}

internal sealed interface DDayWidgetState {
    data class Upcoming(val row: DDayRowUi) : DDayWidgetState
    data object Empty : DDayWidgetState
    data object Unavailable : DDayWidgetState
}

/** 당일은 Coral 카드에 흰 글자 (widget.md 10-1 "당일"). */
private class DDayColors(today: Boolean) {
    val background = if (today) R.drawable.widget_background_accent else R.drawable.widget_background
    val accent = if (today) Color.White else SalimTokens.Accent
    val primary = if (today) Color.White else SalimTokens.TextPrimary
    val muted = if (today) Color.White else SalimTokens.TextMuted
}

@Composable
private fun DDayWidgetContent(state: DDayWidgetState) {
    val context = LocalContext.current
    val today = (state as? DDayWidgetState.Upcoming)?.row?.isToday == true
    val colors = DDayColors(today)
    val onClick = openAppIntent(context, DEEP_LINK_DDAY_MANAGE)

    if (isCompact()) {
        WidgetCompactCard(colors.background, onClick) { DDayCompactBody(state, colors) }
        return
    }

    WidgetCard(
        background = colors.background,
        onClickIntent = onClick,
        header = { WidgetLabel(R.drawable.ic_widget_gift, context.getString(R.string.widget_dday_label), colors.accent, colors.muted) },
    ) {
        when (state) {
            is DDayWidgetState.Upcoming -> {
                Text(
                    text = state.row.dDayText,
                    style = TextStyle(color = ColorProvider(colors.accent), fontSize = 28.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = state.row.widgetTitle(context),
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(colors.primary), fontSize = 14.sp, fontWeight = FontWeight.Medium),
                )
                Text(
                    text = state.row.dateText,
                    style = TextStyle(color = ColorProvider(colors.muted), fontSize = 12.sp),
                )
            }
            DDayWidgetState.Empty -> WidgetMessage(context.getString(R.string.dday_empty), SalimTokens.TextMuted)
            DDayWidgetState.Unavailable -> WidgetMessage(context.widgetUnavailableText(), SalimTokens.TextMuted)
        }
    }
}

/**
 * 가로형 (widget.md 10-1 "가로형"). 2칸 폭은 좁아서 한 줄에 다 넣으면 제목이 잘린다 —
 * 윗줄에 아이콘 + 제목(전체 폭), 아랫줄에 날짜와 우측 D-숫자.
 */
@Composable
private fun DDayCompactBody(state: DDayWidgetState, colors: DDayColors) {
    val context = LocalContext.current
    val title = when (state) {
        is DDayWidgetState.Upcoming -> state.row.widgetTitle(context)
        DDayWidgetState.Empty -> context.getString(R.string.dday_empty)
        DDayWidgetState.Unavailable -> context.widgetUnavailableText()
    }
    val titleColor = if (state is DDayWidgetState.Upcoming) colors.primary else SalimTokens.TextMuted

    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        WidgetIcon(R.drawable.ic_widget_gift, colors.accent)
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = title,
            maxLines = 1,
            style = TextStyle(color = ColorProvider(titleColor), fontSize = 14.sp, fontWeight = FontWeight.Medium),
        )
    }
    if (state is DDayWidgetState.Upcoming) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                text = state.row.dateText,
                style = TextStyle(color = ColorProvider(colors.muted), fontSize = 12.sp),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = state.row.dDayText,
                style = TextStyle(color = ColorProvider(colors.accent), fontSize = 24.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

/** 당일이면 "오늘은 {제목}". */
private fun DDayRowUi.widgetTitle(context: Context): String =
    if (isToday) context.getString(R.string.widget_dday_today, title) else title
