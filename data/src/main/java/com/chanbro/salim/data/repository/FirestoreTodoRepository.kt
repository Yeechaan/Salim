package com.chanbro.salim.data.repository

import com.chanbro.salim.domain.model.Todo
import com.chanbro.salim.domain.model.TodoAssignee
import com.chanbro.salim.domain.model.TodoWritePlan
import com.chanbro.salim.domain.repository.TodoRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
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
 * Firestore 기반 할 일 저장소. (firestore-schema.md {todos}/{todoId})
 *
 * 경로는 UserScope가 정한다 — 미연결은 `users/{uid}/todos`, 연결은 `couples/{coupleId}/todos`.
 * 하위 항목도 같은 컬렉션에 `parentId`를 달아 둔다 — 리스너 하나로 전부 받고, 체크가 필드 단위로 갈린다.
 * 컬렉션 전체를 리스너 하나로 구독한다 — 완료 여부별로 쿼리를 나누면 체크하는 순간
 * 항목이 한 리스너에서 빠지고 다른 리스너에 들어오기 전까지 깜빡인다.
 */
@Singleton
class FirestoreTodoRepository @Inject constructor(
    private val userScope: UserScope,
) : TodoRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<Todo>> =
        userScope.scope.flatMapLatest { scope ->
            if (scope == null) return@flatMapLatest flowOf(emptyList())
            callbackFlow {
                val listener = collection(scope.doc).addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.documents?.mapNotNull { it.toTodo(scope.myUid) } ?: emptyList())
                }
                awaitClose { listener.remove() }
            }
        }

    override suspend fun apply(plan: TodoWritePlan) {
        val scope = userScope.requireScope()
        val todos = collection(scope.doc)
        // 상위 수정 + 하위 추가·수정·삭제를 한 배치로 — 중간에 끊겨 하위 항목만 반쯤 저장되지 않게.
        val batch = firestore(scope).batch()
        plan.adds.forEach { todo ->
            val data = mapOf(
                "title" to todo.title,
                "done" to todo.done,
                "completedAtMillis" to todo.completedAtMillis,
                "createdAtMillis" to todo.createdAtMillis,
                "parentId" to todo.parentId,
            ) + assigneeFields(scope, todo.assignee, deleteOwner = false)
            batch.set(todos.document(todo.id), data)
        }
        plan.patches.forEach { patch ->
            // 제목·담당자만 고친다 — 그사이 상대가 바꾼 완료 여부를 덮어쓰지 않게.
            val data = mapOf(
                "title" to patch.title,
                "updatedAtMillis" to patch.updatedAtMillis,
            ) + assigneeFields(scope, patch.assignee, deleteOwner = true)
            batch.update(todos.document(patch.id), data)
        }
        plan.deletes.forEach { batch.delete(todos.document(it)) }
        batch.commit().await()
    }

    override suspend fun setDone(ids: List<String>, done: Boolean, completedAtMillis: Long?) {
        if (ids.isEmpty()) return
        val scope = userScope.requireScope()
        val batch = firestore(scope).batch()
        // 두 필드만 고친다 — 상대가 같은 순간 제목을 고쳐도 서로 덮어쓰지 않게.
        ids.forEach { id ->
            batch.update(
                collection(scope.doc).document(id),
                mapOf("done" to done, "completedAtMillis" to completedAtMillis),
            )
        }
        batch.commit().await()
    }

    override suspend fun delete(ids: List<String>) {
        if (ids.isEmpty()) return
        val scope = userScope.requireScope()
        val batch = firestore(scope).batch()
        ids.forEach { batch.delete(collection(scope.doc).document(it)) }
        batch.commit().await()
    }

    private fun firestore(scope: DataScope) = scope.doc.firestore

    private fun collection(scopeDoc: DocumentReference): CollectionReference =
        scopeDoc.collection("todos")

    /**
     * 지출자·일정 주인과 같은 이유로 "나/상대" 대신 SHARED 또는 PERSONAL + 담당자 uid로 저장한다.
     * @param deleteOwner 수정일 때 "함께"로 바꾸면 남아 있던 ownerId를 지운다.
     */
    private fun assigneeFields(scope: DataScope, assignee: TodoAssignee, deleteOwner: Boolean): Map<String, Any?> =
        when (assignee) {
            TodoAssignee.TOGETHER -> buildMap {
                put("assignee", ASSIGNEE_SHARED)
                if (deleteOwner) put("ownerId", FieldValue.delete())
            }
            TodoAssignee.ME -> mapOf("assignee" to ASSIGNEE_PERSONAL, "ownerId" to scope.myUid)
            // 미연결이면 상대가 없으므로 본인으로 떨어진다.
            TodoAssignee.PARTNER -> mapOf("assignee" to ASSIGNEE_PERSONAL, "ownerId" to (scope.partnerUid ?: scope.myUid))
        }

    private fun DocumentSnapshot.toTodo(myUid: String): Todo? {
        val title = getString("title") ?: return null
        val assignee = when {
            getString("assignee") != ASSIGNEE_PERSONAL -> TodoAssignee.TOGETHER
            getString("ownerId") == myUid -> TodoAssignee.ME
            else -> TodoAssignee.PARTNER
        }
        return Todo(
            id = id,
            title = title,
            assignee = assignee,
            done = getBoolean("done") ?: false,
            completedAtMillis = getLong("completedAtMillis"),
            createdAtMillis = getLong("createdAtMillis") ?: 0L,
            // 없으면 하위 항목 기능 이전 문서 — 모두 상위 항목이다.
            parentId = getString("parentId"),
        )
    }

    private companion object {
        const val ASSIGNEE_SHARED = "SHARED"
        const val ASSIGNEE_PERSONAL = "PERSONAL"
    }
}
