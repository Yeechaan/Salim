package com.chanbro.salim.domain.model

/** 일정 1건. (firestore-schema.md {schedules}/{scheduleId} — PRD 5. 일정) */
data class Schedule(
    val id: String,
    val title: String,
    val dateMillis: Long,        // 날짜 (UTC 자정 millis)
    val minuteOfDay: Int?,       // 시작 시각 (0~1439). null이면 종일 일정
    val type: ScheduleType,
    val createdAtMillis: Long,
) {
    val isAllDay: Boolean get() = minuteOfDay == null
}

/** 일정 유형. (PRD 5: 우리 일정 / 개인(나, 상대방)) */
enum class ScheduleType(val label: String) {
    SHARED("우리 일정"),
    MINE("개인(나)"),
    PARTNER("개인(배우자)"),
}
