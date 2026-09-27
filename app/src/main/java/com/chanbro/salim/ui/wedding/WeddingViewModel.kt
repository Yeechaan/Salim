package com.chanbro.salim.ui.wedding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.model.Spender
import com.chanbro.salim.domain.model.SpenderNames
import com.chanbro.salim.domain.model.TodoAssignee
import com.chanbro.salim.domain.model.VendorStatus
import com.chanbro.salim.domain.model.WeddingExpense
import com.chanbro.salim.domain.model.WeddingItem
import com.chanbro.salim.domain.model.WeddingOverview
import com.chanbro.salim.domain.model.WeddingPeriod
import com.chanbro.salim.domain.model.WeddingTask
import com.chanbro.salim.domain.model.WeddingVendor
import com.chanbro.salim.domain.usecase.DeleteWeddingExpenseUseCase
import com.chanbro.salim.domain.usecase.DeleteWeddingTaskUseCase
import com.chanbro.salim.domain.usecase.DeleteWeddingVendorUseCase
import com.chanbro.salim.domain.usecase.ObserveConnectionUseCase
import com.chanbro.salim.domain.usecase.ObserveSpenderNamesUseCase
import com.chanbro.salim.domain.usecase.ObserveWeddingOverviewUseCase
import com.chanbro.salim.domain.usecase.SaveWeddingExpenseUseCase
import com.chanbro.salim.domain.usecase.SaveWeddingTaskUseCase
import com.chanbro.salim.domain.usecase.SaveWeddingVendorUseCase
import com.chanbro.salim.domain.usecase.SetWeddingBudgetUseCase
import com.chanbro.salim.domain.usecase.SetWeddingDateUseCase
import com.chanbro.salim.domain.usecase.SetWeddingTaskDoneUseCase
import com.chanbro.salim.ui.common.todayUtcMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class WeddingUiState(
    /** 첫 스냅샷 전. 빈 상태 문구가 깜빡이지 않게 그리지 않는다. */
    val loading: Boolean = true,
    val overview: WeddingOverview = WeddingOverview(),
    /** 미연결이면 담당자·지출자를 숨기고 본인으로 저장한다. */
    val connected: Boolean = false,
    val names: SpenderNames = SpenderNames(),
    val todayMillis: Long = todayUtcMillis(),
) {
    val enabled: Boolean get() = overview.settings.enabled
}

/**
 * 결혼 준비 화면들(메인·지출 기록·업체 상세/추가)과 홈 카드가 함께 쓴다. (wedding.md)
 * 모두 같은 전체([WeddingOverview])를 보고, 화면마다 필요한 부분만 꺼낸다.
 */
@HiltViewModel
class WeddingViewModel @Inject constructor(
    observeOverview: ObserveWeddingOverviewUseCase,
    observeConnection: ObserveConnectionUseCase,
    observeSpenderNames: ObserveSpenderNamesUseCase,
    private val setWeddingDate: SetWeddingDateUseCase,
    private val setWeddingBudget: SetWeddingBudgetUseCase,
    private val saveTaskUseCase: SaveWeddingTaskUseCase,
    private val setTaskDoneUseCase: SetWeddingTaskDoneUseCase,
    private val deleteTaskUseCase: DeleteWeddingTaskUseCase,
    private val saveExpenseUseCase: SaveWeddingExpenseUseCase,
    private val deleteExpenseUseCase: DeleteWeddingExpenseUseCase,
    private val saveVendorUseCase: SaveWeddingVendorUseCase,
    private val deleteVendorUseCase: DeleteWeddingVendorUseCase,
) : ViewModel() {

    val uiState: StateFlow<WeddingUiState> = combine(
        observeOverview(),
        observeConnection(),
        observeSpenderNames(),
    ) { overview, connection, names ->
        WeddingUiState(
            loading = false,
            overview = overview,
            connected = connection is Connection.Connected,
            names = names,
            todayMillis = todayUtcMillis(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeddingUiState(),
    )

    // 쓰기는 기다리지 않는다 — Firestore는 오프라인에서도 로컬에 먼저 반영한다. (할 일과 같은 방식)

    private val connected: Boolean get() = uiState.value.connected

    fun setDate(dateMillis: Long?) = launch { setWeddingDate(dateMillis) }

    fun setBudget(amount: Long) = launch { setWeddingBudget(amount) }

    /** @param baseline 수정할 항목. 추가면 null. 미연결이면 본인으로 저장한다. */
    fun saveTask(baseline: WeddingTask?, title: String, period: WeddingPeriod, assignee: TodoAssignee) = launch {
        saveTaskUseCase(baseline, title, period, if (connected) assignee else TodoAssignee.ME)
    }

    fun setTaskDone(task: WeddingTask, done: Boolean) = launch { setTaskDoneUseCase(task.id, done) }

    fun deleteTask(task: WeddingTask) = launch { deleteTaskUseCase(task.id) }

    /** @param baseline 수정할 지출. 추가면 null — 등록 시각은 수정해도 원래 값을 유지한다. */
    fun saveExpense(
        baseline: WeddingExpense?,
        amount: Long,
        dateMillis: Long,
        item: WeddingItem,
        vendorId: String?,
        spender: Spender,
        memo: String,
    ) = launch {
        saveExpenseUseCase(
            WeddingExpense(
                id = baseline?.id ?: UUID.randomUUID().toString(),
                amount = amount,
                dateMillis = dateMillis,
                item = item,
                vendorId = vendorId,
                spender = if (connected) spender else Spender.ME,
                memo = memo.trim().ifBlank { null },
                createdAtMillis = baseline?.createdAtMillis ?: System.currentTimeMillis(),
            ),
            isNew = baseline == null,
        )
    }

    fun deleteExpense(id: String) = launch { deleteExpenseUseCase(id) }

    fun saveVendor(
        baseline: WeddingVendor?,
        name: String,
        item: WeddingItem,
        status: VendorStatus,
        phone: String,
        contractAmount: Long?,
        balanceDueMillis: Long?,
        memo: String,
    ) = launch {
        saveVendorUseCase(
            WeddingVendor(
                id = baseline?.id ?: UUID.randomUUID().toString(),
                name = name.trim().take(WeddingVendor.NAME_MAX_LENGTH),
                item = item,
                status = status,
                phone = phone.filter { it.isDigit() }.ifEmpty { null },
                contractAmount = contractAmount,
                balanceDueMillis = balanceDueMillis,
                memo = memo.trim().ifBlank { null },
                createdAtMillis = baseline?.createdAtMillis ?: System.currentTimeMillis(),
            ),
            isNew = baseline == null,
        )
    }

    fun deleteVendor(id: String) = launch { deleteVendorUseCase(id, uiState.value.overview) }

    /**
     * 저장·삭제 후 바로 화면을 닫는 흐름이 많다 — 화면이 닫혀 이 ViewModel이 정리돼도
     * 쓰기가 끝까지 가도록 취소되지 않게 돌린다.
     */
    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { withContext(NonCancellable) { runCatching { block() } } }
    }
}
