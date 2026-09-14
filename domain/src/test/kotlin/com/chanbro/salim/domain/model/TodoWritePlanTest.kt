package com.chanbro.salim.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoWritePlanTest {

    private val now = 1_000L

    @Test
    fun `새 할 일은 상위와 하위 항목을 함께 추가하고 빈 줄은 버린다`() {
        val plan = plan(
            baseline = null,
            drafts = listOf(draft(null, "여권"), draft(null, "   "), draft(null, "가방", TodoAssignee.PARTNER)),
        )!!

        val parent = plan.adds.first()
        assertEquals("여행 준비", parent.title)
        assertNull(parent.parentId)
        val subs = plan.adds.drop(1)
        assertEquals(listOf("여권", "가방"), subs.map { it.title })
        assertTrue(subs.all { it.parentId == parent.id })
        assertEquals(TodoAssignee.PARTNER, subs[1].assignee)
        // 시트에 적은 순서가 등록순으로 유지된다.
        assertTrue(subs[0].createdAtMillis < subs[1].createdAtMillis)
    }

    @Test
    fun `상위 항목 제목이 비면 저장하지 않는다`() {
        assertNull(plan(baseline = null, title = "  ", drafts = listOf(draft(null, "여권"))))
    }

    @Test
    fun `바뀐 것만 고치고 시트에서 지운 하위 항목은 삭제한다`() {
        val baseline = TodoItem(
            todo = todo("trip", "여행 준비"),
            subtasks = listOf(todo("s1", "여권", parentId = "trip"), todo("s2", "가방", parentId = "trip")),
        )

        val plan = plan(
            baseline = baseline,
            drafts = listOf(draft("s1", "여권"), draft(null, "환전")),
        )!!

        assertEquals(emptyList<TodoPatch>(), plan.patches)
        assertEquals(listOf("환전"), plan.adds.map { it.title })
        assertEquals(listOf("s2"), plan.deletes)
    }

    @Test
    fun `제목이나 담당자가 바뀐 항목만 부분 수정한다`() {
        val baseline = TodoItem(
            todo = todo("trip", "여행 준비"),
            subtasks = listOf(todo("s1", "여권", parentId = "trip")),
        )

        val plan = plan(
            baseline = baseline,
            title = "제주 여행 준비",
            drafts = listOf(draft("s1", "여권", TodoAssignee.ME)),
        )!!

        assertEquals(
            listOf(
                TodoPatch("trip", "제주 여행 준비", TodoAssignee.TOGETHER, now),
                TodoPatch("s1", "여권", TodoAssignee.ME, now),
            ),
            plan.patches,
        )
    }

    @Test
    fun `시트를 연 뒤 상대가 붙인 하위 항목은 지우지 않는다`() {
        // baseline에는 s1만 있었다. 그사이 상대가 s9를 붙였어도 baseline 기준으로만 삭제를 판단한다.
        val baseline = TodoItem(todo("trip", "여행 준비"), listOf(todo("s1", "여권", parentId = "trip")))

        val plan = plan(baseline = baseline, drafts = listOf(draft("s1", "여권")))!!

        assertTrue(plan.isEmpty)
    }

    @Test
    fun `하위 항목은 최대 개수까지만 저장한다`() {
        val drafts = (1..Todo.SUBTASK_MAX + 5).map { draft(null, "항목$it") }

        val plan = plan(baseline = null, drafts = drafts)!!

        assertEquals(Todo.SUBTASK_MAX, plan.adds.count { it.parentId != null })
    }

    @Test
    fun `제목은 앞뒤 공백을 자르고 최대 길이로 자른다`() {
        assertEquals("우유 사기", normalizeTodoTitle("  우유 사기 "))
        assertEquals(Todo.TITLE_MAX_LENGTH, normalizeTodoTitle("가".repeat(40))?.length)
        assertNull(normalizeTodoTitle("   "))
    }

    private var seq = 0

    private fun plan(
        baseline: TodoItem?,
        title: String = "여행 준비",
        assignee: TodoAssignee = TodoAssignee.TOGETHER,
        drafts: List<SubtaskDraft>,
    ) = planTodoSave(baseline, title, assignee, drafts, now) { "new${seq++}" }

    private fun draft(id: String?, title: String, assignee: TodoAssignee = TodoAssignee.TOGETHER) =
        SubtaskDraft(id, title, assignee)

    private fun todo(id: String, title: String, parentId: String? = null) =
        Todo(id, title, TodoAssignee.TOGETHER, done = false, completedAtMillis = null, createdAtMillis = 0, parentId = parentId)
}
