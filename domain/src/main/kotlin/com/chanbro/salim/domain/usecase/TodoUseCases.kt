package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.SubtaskDraft
import com.chanbro.salim.domain.model.TodoAssignee
import com.chanbro.salim.domain.model.TodoItem
import com.chanbro.salim.domain.model.TodoSections
import com.chanbro.salim.domain.model.planTodoSave
import com.chanbro.salim.domain.model.toSections
import com.chanbro.salim.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

// 할 일 (PRD 11)

/** 미완료 / 완료 구역으로 나누고 하위 항목을 붙인 목록을 관찰. */
class ObserveTodoSectionsUseCase @Inject constructor(
    private val repository: TodoRepository,
) {
    operator fun invoke(): Flow<TodoSections> = repository.observeAll().map { it.toSections() }
}

/**
 * 시트 저장 — 상위 항목 추가/수정과 하위 항목 추가·수정·삭제를 한 번에.
 * @param baseline 시트를 열 때의 항목. 추가면 null. ([planTodoSave] 참고)
 */
class SaveTodoUseCase @Inject constructor(
    private val repository: TodoRepository,
) {
    suspend operator fun invoke(
        baseline: TodoItem?,
        title: String,
        assignee: TodoAssignee,
        drafts: List<SubtaskDraft>,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val plan = planTodoSave(baseline, title, assignee, drafts, nowMillis) { UUID.randomUUID().toString() }
        if (plan == null || plan.isEmpty) return
        repository.apply(plan)
    }
}

/**
 * 상위 항목 완료 체크 / 해제. (PRD 11 하위 항목)
 * 체크하면 아직 안 끝난 하위 항목도 함께 완료한다 — 이미 끝난 하위 항목의 완료 시각은 그대로.
 * 해제는 상위 항목만 되돌리고 하위 항목은 그대로 둔다.
 */
class SetTodoDoneUseCase @Inject constructor(
    private val repository: TodoRepository,
) {
    suspend operator fun invoke(item: TodoItem, done: Boolean, nowMillis: Long = System.currentTimeMillis()) {
        val ids = if (done) {
            listOf(item.todo.id) + item.subtasks.filterNot { it.done }.map { it.id }
        } else {
            listOf(item.todo.id)
        }
        repository.setDone(ids, done, if (done) nowMillis else null)
    }
}

/** 하위 항목 하나만 완료 체크 / 해제. 모두 체크해도 상위 항목은 바뀌지 않는다. */
class SetSubtaskDoneUseCase @Inject constructor(
    private val repository: TodoRepository,
) {
    suspend operator fun invoke(id: String, done: Boolean, nowMillis: Long = System.currentTimeMillis()) =
        repository.setDone(listOf(id), done, if (done) nowMillis else null)
}

/** 상위 항목 삭제 — 하위 항목도 함께. */
class DeleteTodoUseCase @Inject constructor(
    private val repository: TodoRepository,
) {
    suspend operator fun invoke(item: TodoItem) =
        repository.delete(listOf(item.todo.id) + item.subtasks.map { it.id })
}
