package com.chanbro.salim.ui.schedule

import java.util.Calendar
import java.util.TimeZone

// ---------------------------------------------------------------------------
// 월간 캘린더 격자 계산 (순수 함수)
// 날짜는 다른 기능과 동일하게 UTC 자정 millis로 다룬다.
// ---------------------------------------------------------------------------

const val DAYS_IN_WEEK = 7

/** 캘린더 한 칸. dateMillis가 null이면 격자를 채우기 위한 빈 칸. */
data class CalendarCell(
    val dateMillis: Long?,
    val dayOfMonth: Int?,
)

private fun utcCalendar(): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

/** 연/월(1-based)의 1일 UTC 자정 millis. */
fun startOfMonth(year: Int, month: Int): Long = utcCalendar().apply {
    clear()
    set(year, month - 1, 1)
}.timeInMillis

/** 해당 월의 일수. */
fun daysInMonth(year: Int, month: Int): Int = utcCalendar().apply {
    clear()
    set(year, month - 1, 1)
}.getActualMaximum(Calendar.DAY_OF_MONTH)

/** 해당 월 1일의 요일 인덱스 (0=일 ~ 6=토). */
fun firstDayOfWeekIndex(year: Int, month: Int): Int = utcCalendar().apply {
    clear()
    set(year, month - 1, 1)
}.get(Calendar.DAY_OF_WEEK) - 1

/**
 * 월간 격자를 만든다. 앞쪽은 1일의 요일만큼 빈 칸으로 채우고,
 * 마지막 주가 7칸이 되도록 뒤도 빈 칸으로 채운다.
 */
fun monthCells(year: Int, month: Int): List<CalendarCell> {
    val lead = firstDayOfWeekIndex(year, month)
    val days = daysInMonth(year, month)
    val first = startOfMonth(year, month)

    val cells = ArrayList<CalendarCell>(lead + days)
    repeat(lead) { cells += CalendarCell(null, null) }
    for (day in 1..days) {
        cells += CalendarCell(first + (day - 1) * DAY_MILLIS, day)
    }
    val trailing = (DAYS_IN_WEEK - cells.size % DAYS_IN_WEEK) % DAYS_IN_WEEK
    repeat(trailing) { cells += CalendarCell(null, null) }
    return cells
}

/** 격자를 주 단위로 자른다. */
fun monthWeeks(year: Int, month: Int): List<List<CalendarCell>> =
    monthCells(year, month).chunked(DAYS_IN_WEEK)

const val DAY_MILLIS = 24L * 60 * 60 * 1000
