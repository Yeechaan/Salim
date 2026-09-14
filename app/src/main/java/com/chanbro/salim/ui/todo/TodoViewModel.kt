package com.chanbro.salim.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.model.SpenderNames
import com.chanbro.salim.domain.model.SubtaskDraft
import com.chanbro.salim.domain.model.TodoAssignee
import com.chanbro.salim.domain.model.TodoItem
import com.chanbro.salim.domain.model.TodoSections
import com.chanbro.salim.domain.usecase.DeleteTodoUseCase
import com.chanbro.salim.domain.usecase.ObserveConnectionUseCase
import com.chanbro.salim.domain.usecase.ObserveSpenderNamesUseCase
import com.chanbro.salim.domain.usecase.ObserveTodoSectionsUseCase
import com.chanbro.salim.domain.usecase.SaveTodoUseCase
import com.chanbro.salim.domain.usecase.SetSubtaskDoneUseCase
import com.chanbro.salim.domain.usecase.SetTodoDoneUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodoUiState(
    /** 첫 스냅샷 전. 빈 상태 문구가 잠깐 깜빡이지 않게 목록을 그리지 않는다. */
    val loading: Boolean = true,
    val sections: TodoSections = TodoSections(emptyList(), emptyList()),
    /** 미연결이면 담당자 메타와 시트의 담당자 줄을 숨긴다. (todo.md 11-1 상태 분기) */
    val connected: Boolean = false,
    /** 담당자 칩·메타에 쓸 이름. 가계부 지출자와 같은 규칙. */
    val names: SpenderNames = SpenderNames(),
)

@HiltViewModel
class TodoViewModel @Inject constructor(
    observeSections: ObserveTodoSectionsUseCase,
    observeConnection: ObserveConnectionUseCase,
    observeSpenderNames: ObserveSpenderNamesUseCase,
    private val saveTodo: SaveTodoUseCase,
    private val setTodoDone: SetTodoDoneUseCase,
    private val setSubtaskDoneUseCase: SetSubtaskDoneUseCase,
    private val deleteTodo: DeleteTodoUseCase,
) : ViewModel() {

    val uiState: StateFlow<TodoUiState> = combine(
        observeSections(),
        observeConnection(),
        observeSpenderNames(),
    ) { sections, connection, names ->
        TodoUiState(
            loading = false,
            sections = sections,
            connected = connection is Connection.Connected,
            names = names,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodoUiState(),
    )

    // 쓰기는 기다리지 않는다 — Firestore는 오프라인에서도 로컬에 먼저 반영하고,
    // 서버 확인까지 시트를 붙잡아 두면 체크리스트다운 가벼움이 사라진다.

    /**
     * 시트 저장. 미연결이면 담당자를 고를 수 없으므로 상위·하위 항목 모두 본인으로 저장한다.
     * @param baseline 시트를 열 때의 항목. 추가면 null.
     */
    fun save(baseline: TodoItem?, title: String, assignee: TodoAssignee, drafts: List<SubtaskDraft>) {
        val connected = uiState.value.connected
        val resolve = { a: TodoAssignee -> if (connected) a else TodoAssignee.ME }
        viewModelScope.launch {
            runCatching { saveTodo(baseline, title, resolve(assignee), drafts.map { it.copy(assignee = resolve(it.assignee)) }) }
        }
    }

    /** 상위 항목 체크 — 체크하면 하위 항목도 함께 완료된다. */
    fun setDone(item: TodoItem, done: Boolean) {
        viewModelScope.launch { runCatching { setTodoDone(item, done) } }
    }

    fun setSubtaskDone(id: String, done: Boolean) {
        viewModelScope.launch { runCatching { setSubtaskDoneUseCase(id, done) } }
    }

    /** 상위 항목 삭제 — 하위 항목도 함께. */
    fun delete(item: TodoItem) {
        viewModelScope.launch { runCatching { deleteTodo(item) } }
    }
}
