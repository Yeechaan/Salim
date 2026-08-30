package com.chanbro.salim.data.repository

import com.chanbro.salim.domain.model.Expense
import com.chanbro.salim.domain.model.Spender
import com.chanbro.salim.domain.repository.ExpenseRepository
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
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Calendar
import java.util.TimeZone

/**
 * Firestore 기반 지출 저장소.
 *
 * 경로는 UserScope가 정한다 — 미연결은 `users/{uid}/expenses`, 연결은
 * `couples/{coupleId}/expenses`. 저장소는 어느 쪽인지 알 필요가 없다.
 */
@Singleton
class FirestoreExpenseRepository @Inject constructor(
    private val userScope: UserScope,
) : ExpenseRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMonth(year: Int, month: Int): Flow<List<Expense>> =
        userScope.scope.flatMapLatest { scope ->
            if (scope == null) return@flatMapLatest flowOf(emptyList())
            callbackFlow {
                val startMillis = startOfMonth(year, month)
                val endMillis = startOfMonth(year, month + 1)

                val listener = collection(scope.doc)
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
                            runCatching { doc.toExpense(scope.myUid) }.getOrNull()
                        } ?: emptyList()
                        trySend(expenses)
                    }
                awaitClose { listener.remove() }
            }
        }

    override suspend fun add(expense: Expense) {
        val scope = userScope.requireScope()
        val data = mapOf(
            "amount" to expense.amount,
            "spentAtMillis" to expense.spentAtMillis,
            // 지출자는 uid로 저장한다. "나/배우자"는 보는 사람에 따라 뒤집히는 값이라
            // 공동 경로에 그대로 넣으면 상대가 반대로 읽는다. (firestore-schema.md)
            "spenderId" to scope.spenderUid(expense.spender),
            "categoryName" to expense.categoryName,
            "memo" to expense.memo,
            "createdAtMillis" to expense.createdAtMillis,
        )
        collection(scope.doc).document(expense.id).set(data).await()
    }

    private fun collection(scopeDoc: DocumentReference): CollectionReference =
        scopeDoc.collection("expenses")

    private fun DocumentSnapshot.toExpense(myUid: String) = Expense(
        id = id,
        amount = getLong("amount") ?: 0L,
        spentAtMillis = getLong("spentAtMillis") ?: 0L,
        spender = readSpender(myUid),
        categoryName = getString("categoryName") ?: "기타",
        memo = getString("memo"),
        createdAtMillis = getLong("createdAtMillis") ?: 0L,
    )

    /**
     * spenderId가 없으면 연결 이전에 개인 경로로 쌓인 문서다. 그때의 `spender` 필드로
     * 폴백한다 — 개인 데이터는 본인만 열람하므로 폴백 결과가 항상 맞다.
     */
    private fun DocumentSnapshot.readSpender(myUid: String): Spender {
        getString("spenderId")?.let { return if (it == myUid) Spender.ME else Spender.PARTNER }
        return if (getString("spender") == Spender.PARTNER.name) Spender.PARTNER else Spender.ME
    }

    private fun startOfMonth(year: Int, month: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, 1)
        }.timeInMillis
}

/** 미연결 상태에서는 배우자를 고를 수 없으므로 언제나 본인으로 떨어진다. */
internal fun DataScope.spenderUid(spender: Spender): String =
    if (spender == Spender.PARTNER) partnerUid ?: myUid else myUid
