package com.chanbro.salim.data.repository

import android.util.Log
import com.chanbro.salim.domain.model.DefaultWeddingTasks
import com.chanbro.salim.domain.model.Spender
import com.chanbro.salim.domain.model.VendorStatus
import com.chanbro.salim.domain.model.WeddingExpense
import com.chanbro.salim.domain.model.WeddingItem
import com.chanbro.salim.domain.model.WeddingPeriod
import com.chanbro.salim.domain.model.WeddingSettings
import com.chanbro.salim.domain.model.WeddingTask
import com.chanbro.salim.domain.model.WeddingVendor
import com.chanbro.salim.domain.repository.WeddingRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore 기반 결혼 준비 저장소. (firestore-schema.md "결혼 준비")
 *
 * 가계부 `expenses`와 섞지 않고 별도 컬렉션에 둔다 — 월 예산·홈 통계가 `expenses`를 읽기 때문에
 * 섞으면 모든 곳에 "웨딩 제외"를 빠짐없이 붙여야 한다.
 * 목록은 컬렉션 전체를 리스너 하나로 받고 정렬·합계는 도메인([com.chanbro.salim.domain.model.WeddingOverview])이 한다.
 */
@Singleton
class FirestoreWeddingRepository @Inject constructor(
    private val userScope: UserScope,
) : WeddingRepository {

    /** 기본 항목 시드를 진행 중인 경로. 스냅샷이 연달아 와도 트랜잭션을 한 번만 돌린다. */
    private val seeding: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSettings(): Flow<WeddingSettings> = userScope.scope.flatMapLatest { scope ->
        if (scope == null) return@flatMapLatest flowOf(WeddingSettings())
        callbackFlow {
            // 메타데이터 변경도 받는다 — "서버가 확인했다"는 신호가 문서 변화 없이 메타데이터로만 올 수 있다.
            val listener = settingsDoc(scope.doc).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val settings = snapshot?.toSettings() ?: WeddingSettings()
                trySend(settings)
                // 캐시 기준으로 판단하면, 기본 항목을 지운 뒤 오프라인에서 껐다 켤 때 지운 항목이 되살아난다.
                // 서버가 확인해 준 스냅샷에서만 시드를 시도하고, 트랜잭션 안에서 한 번 더 확인한다.
                if (settings.enabled && !settings.templateSeeded && snapshot?.metadata?.isFromCache == false) {
                    seed(scope.doc)
                }
            }
            awaitClose { listener.remove() }
        }
    }.distinctUntilChanged()

    override suspend fun setEnabled(enabled: Boolean) = mergeSettings("enabled" to enabled)

    override suspend fun setWeddingDate(dateMillis: Long?) = mergeSettings("weddingDateMillis" to dateMillis)

    override suspend fun setTotalBudget(amount: Long) = mergeSettings("totalBudget" to amount)

    /** 바꾼 필드만 merge — 한 사람은 예식일, 다른 사람은 예산을 동시에 고쳐도 서로 덮어쓰지 않게. */
    private suspend fun mergeSettings(field: Pair<String, Any?>) {
        settingsDoc(userScope.requireScope().doc).set(mapOf(field), SetOptions.merge()).await()
    }

    private fun seed(scopeDoc: DocumentReference) {
        val key = scopeDoc.path
        if (!seeding.add(key)) return
        val settingsRef = settingsDoc(scopeDoc)
        val scope = DataScope(scopeDoc, myUid = userScope.currentUid().orEmpty(), partnerUid = null)
        scopeDoc.firestore.runTransaction { tx ->
            if (tx.get(settingsRef).getBoolean("templateSeeded") == true) return@runTransaction
            DefaultWeddingTasks.tasks(System.currentTimeMillis()).forEach { task ->
                tx.set(tasks(scopeDoc).document(task.id), task.toData(scope))
            }
            tx.set(settingsRef, mapOf("templateSeeded" to true), SetOptions.merge())
        }
            .addOnFailureListener { Log.w(TAG, "결혼 준비 기본 항목 시드 실패", it) }
            .addOnCompleteListener { seeding.remove(key) }
    }

    // --- 체크리스트 ---

    override fun observeTasks(): Flow<List<WeddingTask>> =
        observeCollection(::tasks) { doc, myUid -> doc.toTask(myUid) }

    override suspend fun saveTask(task: WeddingTask, isNew: Boolean) {
        val scope = userScope.requireScope()
        val ref = tasks(scope.doc).document(task.id)
        if (isNew) {
            ref.set(task.toData(scope)).await()
        } else {
            // 제목·시기·담당자만 고친다 — 그사이 상대가 바꾼 완료 여부를 덮어쓰지 않게.
            val data = mapOf(
                "title" to task.title,
                "period" to task.period.name,
                "updatedAtMillis" to System.currentTimeMillis(),
            ) + scope.assigneeFields(task.assignee, deleteOwner = true)
            ref.update(data).await()
        }
    }

    override suspend fun setTaskDone(id: String, done: Boolean, completedAtMillis: Long?) {
        tasks(userScope.requireScope().doc).document(id)
            .update(mapOf("done" to done, "completedAtMillis" to completedAtMillis))
            .await()
    }

    override suspend fun deleteTask(id: String) {
        tasks(userScope.requireScope().doc).document(id).delete().await()
    }

    // --- 지출 ---

    override fun observeExpenses(): Flow<List<WeddingExpense>> =
        observeCollection(::expenses) { doc, myUid -> doc.toExpense(myUid) }

    override suspend fun saveExpense(expense: WeddingExpense, isNew: Boolean) {
        val scope = userScope.requireScope()
        val spenderUid = scope.spenderUid(expense.spender)
        // 문서를 통째로 쓴다. 수정이면 등록 시각은 호출부가 원래 값으로 넘기고 수정 시각을 남긴다.
        val data = mapOf(
            "amount" to expense.amount,
            "dateMillis" to expense.dateMillis,
            "item" to expense.item.name,
            "vendorId" to expense.vendorId,
            "spenderType" to if (spenderUid == null) SPENDER_SHARED else SPENDER_PERSONAL,
            "spenderId" to spenderUid,
            "memo" to expense.memo,
            "createdAtMillis" to expense.createdAtMillis,
            "updatedAtMillis" to if (isNew) null else System.currentTimeMillis(),
        )
        expenses(scope.doc).document(expense.id).set(data).await()
    }

    override suspend fun deleteExpense(id: String) {
        expenses(userScope.requireScope().doc).document(id).delete().await()
    }

    // --- 업체 ---

    override fun observeVendors(): Flow<List<WeddingVendor>> =
        observeCollection(::vendors) { doc, _ -> doc.toVendor() }

    override suspend fun saveVendor(vendor: WeddingVendor, isNew: Boolean) {
        val data = mapOf(
            "name" to vendor.name,
            "item" to vendor.item.name,
            "status" to vendor.status.name,
            "phone" to vendor.phone,
            "contractAmount" to vendor.contractAmount,
            "balanceDueMillis" to vendor.balanceDueMillis,
            "memo" to vendor.memo,
            "createdAtMillis" to vendor.createdAtMillis,
            "updatedAtMillis" to if (isNew) null else System.currentTimeMillis(),
        )
        vendors(userScope.requireScope().doc).document(vendor.id).set(data).await()
    }

    override suspend fun deleteVendor(id: String, linkedExpenseIds: List<String>) {
        val scope = userScope.requireScope()
        val batch = scope.doc.firestore.batch()
        batch.delete(vendors(scope.doc).document(id))
        // 지출은 남기고 업체 표시만 뗀다 (PRD 12-5). 배치가 끊겨도 읽는 쪽이 없는 업체를 "선택 안 함"으로 본다.
        linkedExpenseIds.forEach { batch.update(expenses(scope.doc).document(it), "vendorId", null) }
        batch.commit().await()
    }

    // --- 공통 ---

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> observeCollection(
        collection: (DocumentReference) -> CollectionReference,
        map: (DocumentSnapshot, String) -> T?,
    ): Flow<List<T>> = userScope.scope.flatMapLatest { scope ->
        if (scope == null) return@flatMapLatest flowOf(emptyList())
        callbackFlow {
            val listener = collection(scope.doc).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { map(it, scope.myUid) } ?: emptyList())
            }
            awaitClose { listener.remove() }
        }
    }

    private fun settingsDoc(scopeDoc: DocumentReference) = scopeDoc.collection("wedding").document("settings")
    private fun tasks(scopeDoc: DocumentReference) = scopeDoc.collection("weddingTasks")
    private fun expenses(scopeDoc: DocumentReference) = scopeDoc.collection("weddingExpenses")
    private fun vendors(scopeDoc: DocumentReference) = scopeDoc.collection("weddingVendors")

    private fun DocumentSnapshot.toSettings() = WeddingSettings(
        enabled = getBoolean("enabled") ?: false,
        weddingDateMillis = getLong("weddingDateMillis"),
        totalBudget = getLong("totalBudget"),
        templateSeeded = getBoolean("templateSeeded") ?: false,
    )

    private fun WeddingTask.toData(scope: DataScope): Map<String, Any?> = mapOf(
        "title" to title,
        "period" to period.name,
        "done" to done,
        "completedAtMillis" to completedAtMillis,
        "createdAtMillis" to createdAtMillis,
    ) + scope.assigneeFields(assignee, deleteOwner = false)

    private fun DocumentSnapshot.toTask(myUid: String): WeddingTask? {
        val title = getString("title") ?: return null
        return WeddingTask(
            id = id,
            title = title,
            period = WeddingPeriod.fromKey(getString("period")),
            assignee = readAssignee(myUid),
            done = getBoolean("done") ?: false,
            completedAtMillis = getLong("completedAtMillis"),
            createdAtMillis = getLong("createdAtMillis") ?: 0L,
        )
    }

    private fun DocumentSnapshot.toExpense(myUid: String): WeddingExpense? {
        val amount = getLong("amount") ?: return null
        // 새 컬렉션이라 처음부터 spenderType/spenderId로 쓴다 — 예전 `spender` 폴백은 없다.
        val spender = when {
            getString("spenderType") == SPENDER_SHARED -> Spender.SHARED
            getString("spenderId") == myUid -> Spender.ME
            else -> Spender.PARTNER
        }
        return WeddingExpense(
            id = id,
            amount = amount,
            dateMillis = getLong("dateMillis") ?: 0L,
            item = WeddingItem.fromKey(getString("item")),
            vendorId = getString("vendorId"),
            spender = spender,
            memo = getString("memo"),
            createdAtMillis = getLong("createdAtMillis") ?: 0L,
        )
    }

    private fun DocumentSnapshot.toVendor(): WeddingVendor? {
        val name = getString("name") ?: return null
        return WeddingVendor(
            id = id,
            name = name,
            item = WeddingItem.fromKey(getString("item")),
            status = if (getString("status") == VendorStatus.CONTRACTED.name) VendorStatus.CONTRACTED else VendorStatus.CONSULTING,
            phone = getString("phone"),
            contractAmount = getLong("contractAmount"),
            balanceDueMillis = getLong("balanceDueMillis"),
            memo = getString("memo"),
            createdAtMillis = getLong("createdAtMillis") ?: 0L,
        )
    }

    private companion object {
        const val TAG = "SalimWedding"
        const val SPENDER_SHARED = "SHARED"
        const val SPENDER_PERSONAL = "PERSONAL"
    }
}
