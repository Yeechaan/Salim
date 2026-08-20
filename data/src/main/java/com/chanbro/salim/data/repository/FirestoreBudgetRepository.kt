package com.chanbro.salim.data.repository

import com.chanbro.salim.domain.model.Budget
import com.chanbro.salim.domain.repository.BudgetRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore 기반 월 예산 저장소. (현재 시뮬레이션용 기본 경로 사용)
 *
 * 실제 배포:
 * - 미연결: users/{uid}/budget/{yyyy-MM}
 * - 연결: couples/{coupleId}/budget/{yyyy-MM}
 */
@Singleton
class FirestoreBudgetRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : BudgetRepository {

    override fun observe(year: Int, month: Int): Flow<Budget?> = callbackFlow {
        val listener = document(year, month).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val amount = snapshot?.getLong("amount")
            trySend(amount?.let { Budget(year, month, it) })
        }
        awaitClose { listener.remove() }
    }

    override suspend fun save(budget: Budget) {
        document(budget.year, budget.month)
            .set(mapOf("amount" to budget.amount))
            .await()
    }

    private fun document(year: Int, month: Int) = firestore
        .collection("users")
        .document(USER_ID)
        .collection("budget")
        .document(documentId(year, month))

    /** 문서 id는 정렬 가능하도록 yyyy-MM. */
    private fun documentId(year: Int, month: Int): String =
        "%04d-%02d".format(year, month)

    private companion object {
        // TODO: 실제로는 FirebaseAuth.currentUser?.uid (다른 리포지토리와 동일)
        const val USER_ID = "demo"
    }
}
