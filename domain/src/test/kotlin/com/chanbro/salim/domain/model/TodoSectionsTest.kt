package com.chanbro.salim.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodoSectionsTest {

    @Test
    fun `미완료는 등록순, 완료는 최근 완료순으로 나뉜다`() {
        val sections = listOf(
            todo("b", createdAt = 2),
            todo("done-old", createdAt = 1, completedAt = 10),
            todo("a", createdAt = 1),
            todo("done-new", createdAt = 3, completedAt = 20),
        ).toSections()

        assertEquals(listOf("a", "b"), sections.open.map { it.todo.id })
        assertEquals(listOf("done-new", "done-old"), sections.done.map { it.todo.id })
    }

    @Test
    fun `체크를 풀면 등록순 자리로 돌아간다`() {
        val sections = listOf(
            todo("first", createdAt = 1),
            todo("third", createdAt = 3),
            todo("second", createdAt = 2),
        ).toSections()

        assertEquals(listOf("first", "second", "third"), sections.open.map { it.todo.id })
    }

    @Test
    fun `완료 시각이 없는 완료 항목은 완료 구역 맨 아래로 간다`() {
        val sections = listOf(
            todo("unknown", createdAt = 1, done = true),
            todo("known", createdAt = 1, completedAt = 5),
        ).toSections()

        assertEquals(listOf("known", "unknown"), sections.done.map { it.todo.id })
    }

    @Test
    fun `하위 항목은 상위 항목 아래에 추가한 순서로 붙고 구역 나누기에 끼지 않는다`() {
        val sections = listOf(
            todo("trip", createdAt = 1),
            todo("passport", createdAt = 3, parentId = "trip", completedAt = 9),
            todo("bag", createdAt = 2, parentId = "trip"),
        ).toSections()

        val trip = sections.open.single()
        assertEquals("trip", trip.todo.id)
        assertEquals(listOf("bag", "passport"), trip.subtasks.map { it.id })
        assertEquals(1, trip.doneSubtaskCount)
        assertEquals(emptyList<TodoItem>(), sections.done)
    }

    @Test
    fun `상위 항목이 없는 하위 항목은 보이지 않는다`() {
        val sections = listOf(todo("orphan", createdAt = 1, parentId = "gone")).toSections()

        assertEquals(emptyList<TodoItem>(), sections.open)
        assertNull(sections.find("orphan"))
    }

    private fun todo(
        id: String,
        createdAt: Long,
        completedAt: Long? = null,
        done: Boolean = completedAt != null,
        parentId: String? = null,
    ) = Todo(
        id = id,
        title = id,
        assignee = TodoAssignee.TOGETHER,
        done = done,
        completedAtMillis = completedAt,
        createdAtMillis = createdAt,
        parentId = parentId,
    )
}
