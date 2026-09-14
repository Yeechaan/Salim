package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.Todo
import com.chanbro.salim.domain.model.TodoAssignee
import com.chanbro.salim.domain.model.TodoItem
import com.chanbro.salim.domain.model.TodoWritePlan
import com.chanbro.salim.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoDoneUseCasesTest {

    private val repo = RecordingRepository()
    private val item = TodoItem(
        todo = todo("trip"),
        subtasks = listOf(todo("s1", parentId = "trip", done = true), todo("s2", parentId = "trip")),
    )

    @Test
    fun `상위 항목을 체크하면 안 끝난 하위 항목도 함께 완료한다`() = runBlocking {
        SetTodoDoneUseCase(repo)(item, done = true, nowMillis = 5)

        assertEquals(listOf(DoneCall(listOf("trip", "s2"), true, 5L)), repo.doneCalls)
    }

    @Test
    fun `상위 항목 체크를 풀면 하위 항목은 그대로 둔다`() = runBlocking {
        SetTodoDoneUseCase(repo)(item, done = false, nowMillis = 5)

        assertEquals(listOf(DoneCall(listOf("trip"), false, null)), repo.doneCalls)
    }

    @Test
    fun `상위 항목을 지우면 하위 항목도 함께 지운다`() = runBlocking {
        DeleteTodoUseCase(repo)(item)

        assertEquals(listOf(listOf("trip", "s1", "s2")), repo.deleteCalls)
    }

    private fun todo(id: String, parentId: String? = null, done: Boolean = false) =
        Todo(id, id, TodoAssignee.TOGETHER, done, if (done) 1 else null, 0, parentId)

    private data class DoneCall(val ids: List<String>, val done: Boolean, val completedAt: Long?)

    private class RecordingRepository : TodoRepository {
        val doneCalls = mutableListOf<DoneCall>()
        val deleteCalls = mutableListOf<List<String>>()
        override fun observeAll(): Flow<List<Todo>> = emptyFlow()
        override suspend fun apply(plan: TodoWritePlan) = Unit
        override suspend fun setDone(ids: List<String>, done: Boolean, completedAtMillis: Long?) {
            doneCalls += DoneCall(ids, done, completedAtMillis)
        }
        override suspend fun delete(ids: List<String>) {
            deleteCalls += ids
        }
    }
}
