package com.chanbro.salim.data.repository

import com.chanbro.salim.domain.model.Schedule
import com.chanbro.salim.domain.model.ScheduleType
import com.chanbro.salim.domain.repository.ScheduleRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore 기반 일정 저장소.
 *
 * 경로는 UserScope가 정한다 — 미연결은 `users/{uid}/schedules`, 연결은
 * `couples/{coupleId}/schedules`.
 */
@Singleton
class FirestoreScheduleRepository @Inject constructor(
    private val userScope: UserScope,
) : ScheduleRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMonth(year: Int, month: Int): Flow<List<Schedule>> =
        userScope.scope.flatMapLatest { scope ->
            if (scope == null) return@flatMapLatest flowOf(emptyList())
            callbackFlow {
                val startMillis = startOfMonth(year, month)
                val endMillis = startOfMonth(year, month + 1)

                val listener = collection(scope.doc)
                    .whereGreaterThanOrEqualTo("dateMillis", startMillis)
                    .whereLessThan("dateMillis", endMillis)
                    .orderBy("dateMillis", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        trySend(
                            snapshot?.documents?.mapNotNull { it.toSchedule(scope.myUid) } ?: emptyList(),
                        )
                    }
                awaitClose { listener.remove() }
            }
        }

    override suspend fun get(id: String): Schedule? {
        val scope = userScope.requireScope()
        return collection(scope.doc).document(id).get().await().toSchedule(scope.myUid)
    }

    override suspend fun save(schedule: Schedule) {
        val scope = userScope.requireScope()
        val data = mapOf(
            "title" to schedule.title,
            "dateMillis" to schedule.dateMillis,
            // 종일 일정은 minuteOfDay를 비워 구분한다.
            "minuteOfDay" to schedule.minuteOfDay,
            // 지출자와 같은 이유로, "개인(나)/개인(배우자)" 대신 PERSONAL + 주인 uid로 저장한다.
            "type" to if (schedule.type == ScheduleType.SHARED) TYPE_SHARED else TYPE_PERSONAL,
            "ownerId" to scope.ownerUid(schedule.type),
            "createdAtMillis" to schedule.createdAtMillis,
        )
        collection(scope.doc).document(schedule.id).set(data).await()
    }

    override suspend fun delete(id: String) {
        collection(userScope.requireScope().doc).document(id).delete().await()
    }

    private fun collection(scopeDoc: DocumentReference): CollectionReference =
        scopeDoc.collection("schedules")

    private fun DocumentSnapshot.toSchedule(myUid: String): Schedule? {
        val title = getString("title") ?: return null
        val dateMillis = getLong("dateMillis") ?: return null
        return Schedule(
            id = id,
            title = title,
            dateMillis = dateMillis,
            minuteOfDay = getLong("minuteOfDay")?.toInt(),
            type = readType(myUid),
            createdAtMillis = getLong("createdAtMillis") ?: 0L,
        )
    }

    /**
     * ownerId가 없으면 연결 이전에 개인 경로로 쌓인 문서다. 그때의 `type`
     * (SHARED/MINE/PARTNER)으로 폴백한다.
     */
    private fun DocumentSnapshot.readType(myUid: String): ScheduleType {
        val raw = getString("type")
        if (raw == TYPE_SHARED) return ScheduleType.SHARED
        val ownerId = getString("ownerId")
        if (ownerId != null) {
            return if (ownerId == myUid) ScheduleType.MINE else ScheduleType.PARTNER
        }
        return runCatching { ScheduleType.valueOf(raw.orEmpty()) }.getOrDefault(ScheduleType.SHARED)
    }

    /** month가 13이면 다음 해 1월로 넘어간다(Calendar 기본 동작) — 월말 경계 계산에 사용. */
    private fun startOfMonth(year: Int, month: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, 1)
        }.timeInMillis

    private companion object {
        const val TYPE_SHARED = "SHARED"
        const val TYPE_PERSONAL = "PERSONAL"
    }
}

/** 미연결 상태에서는 배우자 일정을 만들 수 없으므로 언제나 본인으로 떨어진다. */
internal fun DataScope.ownerUid(type: ScheduleType): String =
    if (type == ScheduleType.PARTNER) partnerUid ?: myUid else myUid
