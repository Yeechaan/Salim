package com.chanbro.salim.domain.model

/** 할 일 1건. 상위 항목과 하위 항목이 같은 모델이다. (PRD 11. 할 일, firestore-schema.md {todos}/{todoId}) */
data class Todo(
    val id: String,
    val title: String,
    val assignee: TodoAssignee,
    val done: Boolean,
    /** 완료 시각. 완료 섹션을 최근 완료순으로 세우는 키. 미완료면 null. */
    val completedAtMillis: Long?,
    /** 등록 시각. 미완료·하위 항목을 등록순으로 세우는 키 — 수정해도 바뀌지 않는다. */
    val createdAtMillis: Long,
    /** 하위 항목이면 상위 할 일 id. 한 단계만 있어서 하위 항목의 하위 항목은 없다. */
    val parentId: String? = null,
) {
    companion object {
        /** 한 줄 체크리스트라 짧게 제한한다. (todo.md 11-2) */
        const val TITLE_MAX_LENGTH = 30

        /** 상위 항목 하나에 둘 수 있는 하위 항목 수. 시트 한 화면에서 다룰 만큼만. */
        const val SUBTASK_MAX = 20
    }
}

/**
 * 담당자. 저장은 "함께 / 한 사람(uid)"으로 하고, 읽을 때 보는 사람 기준으로 나 / 상대방을 가른다
 * (지출자·일정 주인과 같은 방식). 미연결이면 언제나 [ME].
 */
enum class TodoAssignee {
    TOGETHER,
    ME,
    PARTNER,
}

/** 상위 할 일과 그 하위 항목들(추가한 순서). */
data class TodoItem(
    val todo: Todo,
    val subtasks: List<Todo> = emptyList(),
) {
    val doneSubtaskCount: Int get() = subtasks.count { it.done }
}

/** 목록 화면의 두 구역. (todo.md 11-1) 상위 항목의 완료 여부로만 나눈다. */
data class TodoSections(
    /** 미완료 — 등록순(먼저 적은 것이 위). */
    val open: List<TodoItem>,
    /** 완료 — 최근 완료순. */
    val done: List<TodoItem>,
) {
    fun find(id: String): TodoItem? = open.firstOrNull { it.todo.id == id } ?: done.firstOrNull { it.todo.id == id }
}

private val byCreated = compareBy<Todo> { it.createdAtMillis }.thenBy { it.id }

fun List<Todo>.toSections(): TodoSections {
    val (tops, subs) = partition { it.parentId == null }
    // 상위 문서가 없는 하위 항목(배치 도중 끊긴 경우 등)은 붙일 곳이 없어 보이지 않는다.
    val subsByParent = subs.groupBy { it.parentId }.mapValues { (_, list) -> list.sortedWith(byCreated) }
    val items = tops.map { TodoItem(it, subsByParent[it.id].orEmpty()) }
    val (done, open) = items.partition { it.todo.done }
    return TodoSections(
        open = open.sortedWith(compareBy(byCreated) { it.todo }),
        // 완료 시각이 비어 있는 완료 항목(다른 기기에서 필드 없이 쓴 경우)은 맨 아래로.
        done = done.sortedWith(
            compareByDescending<TodoItem> { it.todo.completedAtMillis ?: Long.MIN_VALUE }.thenBy { it.todo.id },
        ),
    )
}
