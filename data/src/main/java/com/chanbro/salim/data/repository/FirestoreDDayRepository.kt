package com.chanbro.salim.data.repository

import com.chanbro.salim.domain.model.DDay
import com.chanbro.salim.domain.model.DDaySource
import com.chanbro.salim.domain.repository.DDayRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore 기반 디데이 저장소.
 *
 * 경로는 UserScope가 정한다:
 * - 미연결: users/{uid}/ddays
 * - 연결: couples/{coupleId}/ddays (PRD 9. 연결 도입 시 UserScope에서 분기)
 */
@Singleton
class FirestoreDDayRepository @Inject constructor(
    private val userScope: UserScope,
) : DDayRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<DDay>> = userScope.uid.flatMapLatest { uid ->
        if (uid == null) return@flatMapLatest flowOf(emptyList())
        callbackFlow {
            // 정렬은 표시 시점에 남은 일수로 계산하므로 여기서는 orderBy를 걸지 않는다
            // (매년 반복 항목은 저장된 날짜와 다음 기념일이 다르기 때문).
            val listener = collection(userScope.userDoc(uid))
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.documents?.mapNotNull { it.toDDay() } ?: emptyList())
                }
            awaitClose { listener.remove() }
        }
    }

    override suspend fun get(id: String): DDay? =
        collection().document(id).get().await().toDDay()

    override suspend fun save(dDay: DDay) {
        val data = mapOf(
            "title" to dDay.title,
            "dateMillis" to dDay.dateMillis,
            "repeatYearly" to dDay.repeatYearly,
            "source" to dDay.source.name,
            "createdAtMillis" to dDay.createdAtMillis,
        )
        collection().document(dDay.id).set(data).await()
    }

    override suspend fun delete(id: String) {
        collection().document(id).delete().await()
    }

    private fun collection(userDoc: DocumentReference = userScope.requireUserDoc()): CollectionReference =
        userDoc.collection("ddays")

    private fun DocumentSnapshot.toDDay(): DDay? {
        val title = getString("title") ?: return null
        val dateMillis = getLong("dateMillis") ?: return null
        return DDay(
            id = id,
            title = title,
            dateMillis = dateMillis,
            repeatYearly = getBoolean("repeatYearly") == true,
            source = if (getString("source") == DDaySource.AUTO.name) DDaySource.AUTO else DDaySource.MANUAL,
            createdAtMillis = getLong("createdAtMillis") ?: 0L,
        )
    }
}
