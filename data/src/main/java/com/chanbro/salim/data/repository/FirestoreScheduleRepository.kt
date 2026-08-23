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
 * 경로는 UserScope가 정한다:
 * - 미연결: users/{uid}/schedules
 * - 연결: couples/{coupleId}/schedules (PRD 9. 연결 도입 시 UserScope에서 분기)
 */
@Singleton
class FirestoreScheduleRepository @Inject constructor(
    private val userScope: UserScope,
) : ScheduleRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMonth(year: Int, month: Int): Flow<List<Schedule>> =
        userScope.uid.flatMapLatest { uid ->
            if (uid == null) return@flatMapLatest flowOf(emptyList())
            callbackFlow {
                val startMillis = startOfMonth(year, month)
                val endMillis = startOfMonth(year, month + 1)

                val listener = collection(userScope.userDoc(uid))
                    .whereGreaterThanOrEqualTo("dateMillis", startMillis)
                    .whereLessThan("dateMillis", endMillis)
                    .orderBy("dateMillis", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.documents?.mapNotNull { it.toSchedule() } ?: emptyList())
                    }
                awaitClose { listener.remove() }
            }
        }

    override suspend fun get(id: String): Schedule? =
        collection().document(id).get().await().toSchedule()

    override suspend fun save(schedule: Schedule) {
        val data = mapOf(
            "title" to schedule.title,
            "dateMillis" to schedule.dateMillis,
            // 종일 일정은 minuteOfDay를 비워 구분한다.
            "minuteOfDay" to schedule.minuteOfDay,
            "type" to schedule.type.name,
            "createdAtMillis" to schedule.createdAtMillis,
        )
        collection().document(schedule.id).set(data).await()
    }

    override suspend fun delete(id: String) {
        collection().document(id).delete().await()
    }

    private fun collection(userDoc: DocumentReference = userScope.requireUserDoc()): CollectionReference =
        userDoc.collection("schedules")

    private fun DocumentSnapshot.toSchedule(): Schedule? {
        val title = getString("title") ?: return null
        val dateMillis = getLong("dateMillis") ?: return null
        return Schedule(
            id = id,
            title = title,
            dateMillis = dateMillis,
            minuteOfDay = getLong("minuteOfDay")?.toInt(),
            type = runCatching { ScheduleType.valueOf(getString("type").orEmpty()) }
                .getOrDefault(ScheduleType.SHARED),
            createdAtMillis = getLong("createdAtMillis") ?: 0L,
        )
    }

    /** month가 13이면 다음 해 1월로 넘어간다(Calendar 기본 동작) — 월말 경계 계산에 사용. */
    private fun startOfMonth(year: Int, month: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, 1)
        }.timeInMillis
}
