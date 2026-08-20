package com.chanbro.salim.ui.dday

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ---------------------------------------------------------------------------
// D-day 계산
//
// 날짜는 UTC 자정 기준 millis로 다룬다 (Material3 DatePicker 규약이자
// SalimUi.todayUtcMillis()와 동일한 표현). UTC로 고정하므로 DST 영향이 없고,
// 두 자정 사이의 차이는 항상 하루의 정확한 배수가 된다.
// ---------------------------------------------------------------------------

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

private fun utcCalendar(millis: Long): Calendar =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }

/** 연/월/일(1-based month)로 UTC 자정 millis를 만든다. */
fun utcDate(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month - 1, day)
    }.timeInMillis

/** 해당 연/월에 없는 날짜는 그 달 마지막 날로 당긴다 (2월 29일 → 평년 2월 28일). */
private fun utcDateClamped(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month - 1, 1)
        set(Calendar.DAY_OF_MONTH, day.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
    }.timeInMillis

/**
 * 화면에 표시할 실제 대상 날짜.
 * 매년 반복이면 오늘(당일 포함) 이후로 돌아오는 가장 가까운 기념일, 아니면 원래 날짜 그대로.
 *
 * 2월 29일 기념일은 평년에 2월 28일로 당긴다. java.time(LocalDate.withYear)이 쓰는 방식과
 * 같아 나중에 java.time으로 옮겨도 동작이 바뀌지 않고, 기념일이 2월 안에 남는다.
 * (iCalendar 표준처럼 건너뛰면 4년에 한 번만 떠서 디데이로는 쓸 수 없다.)
 */
fun nextOccurrence(dateMillis: Long, todayMillis: Long, repeatYearly: Boolean): Long {
    if (!repeatYearly) return dateMillis

    val target = utcCalendar(dateMillis)
    val today = utcCalendar(todayMillis)
    val month = target.get(Calendar.MONTH) + 1
    val day = target.get(Calendar.DAY_OF_MONTH)
    val thisYear = utcDateClamped(today.get(Calendar.YEAR), month, day)
    if (thisYear >= todayMillis) return thisYear
    return utcDateClamped(today.get(Calendar.YEAR) + 1, month, day)
}

/** 오늘부터 대상 날짜까지 남은 일수. 0이면 당일, 음수면 이미 지난 날. */
fun daysUntil(targetMillis: Long, todayMillis: Long): Long =
    (targetMillis - todayMillis) / DAY_MILLIS

/** 남은 일수를 배지 문구로. 당일 D-DAY, 미래 D-12, 지난 날 D+3. */
fun dDayLabel(days: Long): String = when {
    days == 0L -> "D-DAY"
    days > 0 -> "D-$days"
    else -> "D+${-days}"
}

/** 리스트 표기용 "2026.08.31". */
fun formatDotDate(millis: Long): String =
    SimpleDateFormat("yyyy.MM.dd", Locale.KOREAN).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(millis))
