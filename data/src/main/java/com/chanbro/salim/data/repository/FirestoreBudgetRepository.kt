package com.chanbro.salim.data.repository

import com.chanbro.salim.domain.model.Budget
import com.chanbro.salim.domain.repository.BudgetRepository
import com.google.firebase.firestore.DocumentReference
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
 * Firestore 기반 월 예산 저장소.
 *
 * 경로는 UserScope가 정한다:
 * - 미연결: users/{uid}/budget/{yyyy-MM}
 * - 연결: couples/{coupleId}/budget/{yyyy-MM} (PRD 9. 연결 도입 시 UserScope에서 분기)
 */
@Singleton
class FirestoreBudgetRepository @Inject constructor(
    private val userScope: UserScope,
) : BudgetRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(year: Int, month: Int): Flow<Budget?> =
        userScope.scope.flatMapLatest { scope ->
            if (scope == null) return@flatMapLatest flowOf(null)
            callbackFlow {
                val listener = document(scope.doc, year, month)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val amount = snapshot?.getLong("amount")
                        trySend(amount?.let { Budget(year, month, it) })
                    }
                awaitClose { listener.remove() }
            }
        }

    override suspend fun save(budget: Budget) {
        document(userScope.requireScope().doc, budget.year, budget.month)
            .set(mapOf("amount" to budget.amount))
            .await()
    }

    private fun document(scopeDoc: DocumentReference, year: Int, month: Int) = scopeDoc
        .collection("budget")
        .document(documentId(year, month))

    /** 문서 id는 정렬 가능하도록 yyyy-MM. */
    private fun documentId(year: Int, month: Int): String =
        "%04d-%02d".format(year, month)
}
