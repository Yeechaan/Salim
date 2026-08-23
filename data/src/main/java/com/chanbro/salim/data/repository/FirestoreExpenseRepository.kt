package com.chanbro.salim.data.repository

import com.chanbro.salim.domain.model.Expense
import com.chanbro.salim.domain.model.Spender
import com.chanbro.salim.domain.repository.ExpenseRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Calendar
import java.util.TimeZone

/**
 * Firestore 기반 지출 저장소.
 *
 * 경로는 UserScope가 정한다:
 * - 미연결: users/{uid}/expenses
 * - 연결: couples/{coupleId}/expenses (PRD 9. 연결 도입 시 UserScope에서 분기)
 */
@Singleton
class FirestoreExpenseRepository @Inject constructor(
    private val userScope: UserScope,
) : ExpenseRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMonth(year: Int, month: Int): Flow<List<Expense>> =
        userScope.uid.flatMapLatest { uid ->
            if (uid == null) return@flatMapLatest flowOf(emptyList())
            callbackFlow {
                val startMillis = startOfMonth(year, month)
                val endMillis = startOfMonth(year, month + 1)

                val listener = collection(userScope.userDoc(uid))
                    .whereGreaterThanOrEqualTo("spentAtMillis", startMillis)
                    .whereLessThan("spentAtMillis", endMillis)
                    .orderBy("spentAtMillis", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        // 필드 타입이 어긋난 문서 하나 때문에 목록 전체가 죽지 않도록 건별로 흘린다.
                        val expenses = snapshot?.documents?.mapNotNull { doc ->
                            runCatching {
                                Expense(
                                    id = doc.id,
                                    amount = doc.getLong("amount") ?: 0L,
                                    spentAtMillis = doc.getLong("spentAtMillis") ?: 0L,
                                    spender = when (doc.getString("spender")) {
                                        "PARTNER" -> Spender.PARTNER
                                        else -> Spender.ME
                                    },
                                    categoryName = doc.getString("categoryName") ?: "기타",
                                    memo = doc.getString("memo"),
                                    createdAtMillis = doc.getLong("createdAtMillis") ?: 0L,
                                )
                            }.getOrNull()
                        } ?: emptyList()
                        trySend(expenses)
                    }
                awaitClose { listener.remove() }
            }
        }

    override suspend fun add(expense: Expense) {
        val data = mapOf(
            "amount" to expense.amount,
            "spentAtMillis" to expense.spentAtMillis,
            "spender" to expense.spender.name,
            "categoryName" to expense.categoryName,
            "memo" to expense.memo,
            "createdAtMillis" to expense.createdAtMillis,
        )
        collection().document(expense.id).set(data).await()
    }

    private fun collection(userDoc: DocumentReference = userScope.requireUserDoc()): CollectionReference =
        userDoc.collection("expenses")

    private fun startOfMonth(year: Int, month: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, 1)
        }.timeInMillis
}
