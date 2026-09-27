package com.chanbro.salim.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.chanbro.salim.domain.usecase.ObserveBudgetUseCase
import com.chanbro.salim.domain.usecase.ObserveDDaysUseCase
import com.chanbro.salim.domain.usecase.ObserveMonthExpensesUseCase
import com.chanbro.salim.domain.usecase.ObserveProfileUseCase
import com.chanbro.salim.domain.usecase.ObserveWidgetShowAmountUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

/** 위젯(Hilt 주입 대상이 아님)이 싱글턴 그래프에서 꺼내 쓰는 것들. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun observeDDays(): ObserveDDaysUseCase
    fun observeProfile(): ObserveProfileUseCase
    fun observeMonthExpenses(): ObserveMonthExpensesUseCase
    fun observeBudget(): ObserveBudgetUseCase
    fun observeWidgetShowAmount(): ObserveWidgetShowAmountUseCase
    fun updater(): WidgetUpdater
}

internal fun Context.widgetEntryPoint(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)

/** 모든 위젯을 다시 그린다. 놓인 위젯이 없는 종류는 아무 일도 하지 않는다. */
internal suspend fun updateAllWidgets(context: Context) {
    DDayWidget().updateAll(context)
    BudgetWidget().updateAll(context)
}

// ---------------------------------------------------------------------------
// 자정 갱신 (widget.md "갱신 시점")
//
// 데이터가 그대로여도 날짜가 넘어가면 D-숫자와 "이번 달"이 바뀐다. 다음 로컬 자정에 위젯을 다시
// 그리도록 알람을 건다. 비-wakeup(RTC) 알람이라 기기가 잠들어 있으면 깨어날 때 전달된다 —
// 화면이 꺼져 있는 동안에는 위젯을 볼 일도 없으니 굳이 깨우지 않는다.
//
// 알람은 위젯이 그려질 때마다 다시 건다. 위젯이 모두 빠지면 더는 그려지지 않으므로
// 마지막 알람 한 번이 헛돌고 사슬이 저절로 끊긴다 — 따로 취소하지 않는다.
// ---------------------------------------------------------------------------

internal const val ACTION_WIDGET_REFRESH = "com.chanbro.salim.widget.action.REFRESH"

/** 다음 로컬 자정 millis. 정확히 자정인 순간이면 그다음 날 자정. */
internal fun nextLocalMidnight(nowMillis: Long, calendar: Calendar = Calendar.getInstance()): Long =
    calendar.apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis

/** 같은 PendingIntent를 덮어쓰므로 여러 번 불러도 알람은 하나만 남는다. */
internal fun scheduleMidnightRefresh(context: Context) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    val intent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION_WIDGET_REFRESH),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    alarmManager.set(AlarmManager.RTC, nextLocalMidnight(System.currentTimeMillis()), intent)
}

/**
 * 자정 알람과 시간/시간대 변경을 받아 모든 위젯을 다시 그린다.
 * TIME_SET / TIMEZONE_CHANGED는 암시적 브로드캐스트 제한의 예외라 매니페스트로 받을 수 있다.
 */
class WidgetRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val pending = goAsync()
        scope.launch {
            try {
                // 앱이 살아 있으면 구독 중인 "이번 달"도 새 날짜로 갈아 끼운다
                context.widgetEntryPoint().updater().restart()
                updateAllWidgets(context)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val HANDLED_ACTIONS = setOf(
            ACTION_WIDGET_REFRESH,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
