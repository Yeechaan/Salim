package com.chanbro.salim.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.chanbro.salim.domain.usecase.ObserveBudgetUseCase
import com.chanbro.salim.domain.usecase.ObserveDDaysUseCase
import com.chanbro.salim.domain.usecase.ObserveMonthExpensesUseCase
import com.chanbro.salim.domain.usecase.ObserveProfileUseCase
import com.chanbro.salim.domain.usecase.ObserveWidgetShowAmountUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 프로세스가 살아 있는 동안 데이터 변화를 위젯에 즉시 반영한다.
 * (내가 바꿨거나, 연결된 상대가 바꾼 것이 실시간 리스너로 들어온 경우)
 *
 * 놓여 있는 위젯 종류만 구독한다 — 쓰지도 않을 Firestore 리스너를 앱 수명 내내 붙들지 않기 위해서다.
 * 구독이 붙는 곳은 두 군데다:
 * - 앱 시작 시 [start] — 이미 놓여 있는 위젯
 * - 위젯이 그려질 때 [ensureWatching] — 새로 놓인 위젯. onEnabled 시점에는 Glance가 새 위젯을
 *   아직 모를 수 있어(getGlanceIds가 빈 목록) 거기서 확인하면 구독을 놓친다.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeDDays: ObserveDDaysUseCase,
    private val observeProfile: ObserveProfileUseCase,
    private val observeMonthExpenses: ObserveMonthExpensesUseCase,
    private val observeBudget: ObserveBudgetUseCase,
    private val observeWidgetShowAmount: ObserveWidgetShowAmountUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = mutableMapOf<Class<out GlanceAppWidget>, Job>()

    /** 위젯마다 "바뀌면 다시 그릴" 데이터. */
    private fun source(widget: GlanceAppWidget): Flow<Any?> = when (widget) {
        is DDayWidget -> nearestDDay(observeDDays, observeProfile)
        // 금액 표시 토글도 여기서 받는다 — 설정 화면은 저장만 하면 된다.
        is BudgetWidget -> combine(
            budgetSummary(observeMonthExpenses, observeBudget),
            observeWidgetShowAmount(),
        ) { summary, show -> summary to show }
        else -> error("알 수 없는 위젯: ${widget.javaClass.simpleName}")
    }

    fun start() {
        scope.launch {
            listOf(DDayWidget(), BudgetWidget()).forEach { widget ->
                val placed = GlanceAppWidgetManager(context).getGlanceIds(widget.javaClass).isNotEmpty()
                if (placed) ensureWatching(widget)
            }
        }
    }

    /** 날짜가 바뀌면 예산 위젯이 구독하는 "이번 달"이 달라지므로 새로 붙는다. */
    fun restart() {
        stopAll()
        start()
    }

    /** 마지막 위젯이 빠졌을 때 (각 리시버의 onDisabled). */
    @Synchronized
    fun stop(widget: GlanceAppWidget) {
        jobs.remove(widget.javaClass)?.cancel()
    }

    /** 이미 구독 중이면 아무것도 하지 않는다 — 위젯이 그려질 때마다 불러도 된다. */
    @Synchronized
    fun ensureWatching(widget: GlanceAppWidget) {
        if (jobs[widget.javaClass]?.isActive == true) return
        jobs[widget.javaClass] = scope.launch {
            source(widget)
                // 첫 값도 흘려보낸다 — 앱이 꺼져 있던 동안 상대가 바꾼 내용을 앱을 여는 순간 따라잡는다.
                .distinctUntilChanged()
                .catch { Log.w(TAG, "${widget.javaClass.simpleName} 구독 실패 — 다음 앱 실행 때 다시 붙는다", it) }
                .collect { widget.updateAll(context) }
        }
    }

    @Synchronized
    private fun stopAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }

    private companion object {
        const val TAG = "WidgetUpdater"
    }
}
