package com.chanbro.salim.domain.model

/** 시트에서 편집 중인 하위 항목 한 줄. [id]가 null이면 새로 추가한 줄. */
data class SubtaskDraft(
    val id: String?,
    val title: String,
    val assignee: TodoAssignee,
)

/** 제목·담당자만 고치는 부분 수정 — 완료 여부는 건드리지 않는다. */
data class TodoPatch(
    val id: String,
    val title: String,
    val assignee: TodoAssignee,
    val updatedAtMillis: Long,
)

/** 시트 저장 한 번에 일어나는 쓰기 묶음. 저장소가 한 배치로 반영한다. */
data class TodoWritePlan(
    val adds: List<Todo> = emptyList(),
    val patches: List<TodoPatch> = emptyList(),
    val deletes: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = adds.isEmpty() && patches.isEmpty() && deletes.isEmpty()
}

/**
 * 시트 저장 내용을 쓰기 묶음으로 바꾼다. (todo.md 11-2 동작)
 *
 * @param baseline 시트를 열 때의 상위 항목과 하위 항목. 추가면 null.
 *   삭제·변경 판단을 **지금 목록이 아니라 시트를 연 시점** 기준으로 한다 — 시트가 열려 있는 동안
 *   상대가 새로 붙인 하위 항목을 "시트에 없으니 지운 것"으로 오해해 지워버리지 않게.
 * @return 상위 항목 제목이 비어 있으면 null (저장하지 않음).
 */
fun planTodoSave(
    baseline: TodoItem?,
    title: String,
    assignee: TodoAssignee,
    drafts: List<SubtaskDraft>,
    nowMillis: Long,
    newId: () -> String,
): TodoWritePlan? {
    val parentTitle = normalizeTodoTitle(title) ?: return null
    val adds = mutableListOf<Todo>()
    val patches = mutableListOf<TodoPatch>()

    val parentId = baseline?.todo?.id ?: newId()
    if (baseline == null) {
        adds += Todo(parentId, parentTitle, assignee, done = false, completedAtMillis = null, createdAtMillis = nowMillis)
    } else if (baseline.todo.title != parentTitle || baseline.todo.assignee != assignee) {
        patches += TodoPatch(parentId, parentTitle, assignee, nowMillis)
    }

    val original = baseline?.subtasks.orEmpty().associateBy { it.id }
    // 제목을 비운 줄은 버린다 — 기존 줄이었다면 삭제가 된다.
    val kept = drafts
        .mapNotNull { draft -> normalizeTodoTitle(draft.title)?.let { draft.copy(title = it) } }
        .take(Todo.SUBTASK_MAX)

    kept.forEachIndexed { index, draft ->
        val before = draft.id?.let(original::get)
        when {
            before == null -> adds += Todo(
                id = newId(),
                title = draft.title,
                assignee = draft.assignee,
                done = false,
                completedAtMillis = null,
                // 같은 순간 여러 줄을 추가해도 시트에 적은 순서가 유지되게 1ms씩 벌린다.
                createdAtMillis = nowMillis + index,
                parentId = parentId,
            )
            before.title != draft.title || before.assignee != draft.assignee ->
                patches += TodoPatch(before.id, draft.title, draft.assignee, nowMillis)
        }
    }

    val keptIds = kept.mapNotNull { it.id }.toSet()
    val deletes = original.keys.filterNot { it in keptIds }

    return TodoWritePlan(adds, patches, deletes)
}

/** 저장할 제목. 앞뒤 공백을 자르고 최대 길이로 자른다. 비어 있으면 null. */
fun normalizeTodoTitle(title: String): String? =
    title.trim().take(Todo.TITLE_MAX_LENGTH).takeIf { it.isNotEmpty() }
