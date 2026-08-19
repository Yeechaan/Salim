package com.chanbro.salim.data.repository

import com.chanbro.salim.domain.model.Expense
import com.chanbro.salim.domain.model.Spender
import com.chanbro.salim.domain.repository.ExpenseRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Calendar
import java.util.TimeZone

/**
 * Firestore 기반 지출 저장소. (현재 시뮬레이션용 기본 경로 사용)
 *
 * 실제 배포:
 * - 미연결: users/{uid}/expenses
 * - 연결: couples/{coupleId}/expenses (Firestore Rules로 관리)
 */
@Singleton
class FirestoreExpenseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ExpenseRepository {

    override fun observeMonth(year: Int, month: Int): Flow<List<Expense>> = callbackFlow {
        // 기본 경로: users/demo/expenses (시뮬레이션용)
        val userId = "demo"  // TODO: 실제로는 FirebaseAuth.currentUser?.uid
        val startMillis = startOfMonth(year, month)
        val endMillis = startOfMonth(year, month + 1)

        val listener = firestore
            .collection("users")
            .document(userId)
            .collection("expenses")
            .whereGreaterThanOrEqualTo("spentAtMillis", startMillis)
            .whereLessThan("spentAtMillis", endMillis)
            .orderBy("spentAtMillis", Query.Direction.DESCENDING)
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    try {
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
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(expenses)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun add(expense: Expense) {
        val userId = "demo"  // TODO: 실제로는 FirebaseAuth.currentUser?.uid
        val data = mapOf(
            "amount" to expense.amount,
            "spentAtMillis" to expense.spentAtMillis,
            "spender" to expense.spender.name,
            "categoryName" to expense.categoryName,
            "memo" to expense.memo,
            "createdAtMillis" to expense.createdAtMillis,
        )
        firestore
            .collection("users")
            .document(userId)
            .collection("expenses")
            .document(expense.id)
            .set(data)
            .await()
    }

    private fun startOfMonth(year: Int, month: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, 1)
        }.timeInMillis

    private companion object {
        init {
            // Firestore 오프라인 캐시 활성화 (기본적으로 활성, 명시적 설정 선택사항)
            // FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings {
            //     isPersistenceEnabled = true
            // }
        }
    }
}
