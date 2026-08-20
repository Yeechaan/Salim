package com.chanbro.salim.domain.repository

import com.chanbro.salim.domain.model.Schedule
import kotlinx.coroutines.flow.Flow

/**
 * 일정 저장소.
 * (firestore-schema.md: 미연결=users/{uid}, 연결=couples/{coupleId} 이중 경로)
 */
interface ScheduleRepository {
    /** 해당 연월의 일정을 관찰. 캘린더가 월 단위로 그려지므로 월이 조회 단위. */
    fun observeMonth(year: Int, month: Int): Flow<List<Schedule>>

    /** 단건 조회. 없으면 null. */
    suspend fun get(id: String): Schedule?

    /** 신규 등록과 수정을 겸한다(같은 id면 덮어씀). */
    suspend fun save(schedule: Schedule)

    /** 삭제. */
    suspend fun delete(id: String)
}
