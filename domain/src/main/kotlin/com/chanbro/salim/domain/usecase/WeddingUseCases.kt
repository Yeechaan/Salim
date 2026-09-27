package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.TodoAssignee
import com.chanbro.salim.domain.model.WeddingExpense
import com.chanbro.salim.domain.model.WeddingOverview
import com.chanbro.salim.domain.model.WeddingPeriod
import com.chanbro.salim.domain.model.WeddingSettings
import com.chanbro.salim.domain.model.WeddingTask
import com.chanbro.salim.domain.model.WeddingVendor
import com.chanbro.salim.domain.model.normalizeWeddingTaskTitle
import com.chanbro.salim.domain.repository.WeddingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

// 결혼 준비 (PRD 12)

class ObserveWeddingSettingsUseCase @Inject constructor(
    private val repository: WeddingRepository,
) {
    operator fun invoke(): Flow<WeddingSettings> = repository.observeSettings()
}

/**
 * 설정 + 체크리스트 + 지출 + 업체. 꺼져 있으면 목록 리스너를 붙이지 않는다 —
 * 홈 카드가 없으니 읽을 이유가 없다. (firestore-schema.md "결혼 준비")
 */
class ObserveWeddingOverviewUseCase @Inject constructor(
    private val repository: WeddingRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<WeddingOverview> {
        val settings = repository.observeSettings()
        // 켜짐 여부로만 리스너를 갈아탄다 — 예식일·예산을 고칠 때마다 목록 리스너를 다시 붙이지 않게.
        val lists = settings.map { it.enabled }.distinctUntilChanged().flatMapLatest { enabled ->
            if (!enabled) {
                flowOf(WeddingOverview())
            } else {
                combine(repository.observeTasks(), repository.observeExpenses(), repository.observeVendors()) { t, e, v ->
                    WeddingOverview(tasks = t, expenses = e, vendors = v)
                }
            }
        }
        return combine(settings, lists) { s, l -> l.copy(settings = s) }
    }
}

class SetWeddingEnabledUseCase @Inject constructor(
    private val repository: WeddingRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setEnabled(enabled)
}

class SetWeddingDateUseCase @Inject constructor(
    private val repository: WeddingRepository,
) {
    suspend operator fun invoke(dateMillis: Long?) = repository.setWeddingDate(dateMillis)
}

class SetWeddingBudgetUseCase @Inject constructor(
    private val repository: WeddingRepository,
) {
    suspend operator fun invoke(amount: Long) = repository.setTotalBudget(amount)
}

/**
 * 체크리스트 항목 추가 / 수정. 제목이 비어 있으면 저장하지 않는다.
 * @param baseline 수정할 항목. 추가면 null.
 */
class SaveWeddingTaskUseCase @Inject constructor(
    private val repository: WeddingRepository,
) {
    suspend operator fun invoke(
        baseline: WeddingTask?,
        title: String,
        period: WeddingPeriod,
        assignee: TodoAssignee,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val normalized = normalizeWeddingTaskTitle(title) ?: return
        if (baseline == null) {
            repository.saveTask(
                WeddingTask(
                    id = UUID.randomUUID().toString(),
                    title = normalized,
                    period = period,
                    assignee = assignee,
                    done = false,
                    completedAtMillis = null,
                    createdAtMillis = nowMillis,
                ),
                isNew = true,
            )
        } else if (baseline.title != normalized || baseline.period != period || baseline.assignee != assignee) {
            repository.saveTask(baseline.copy(title = normalized, period = period, assignee = assignee), isNew = false)
        }
    }
}

class SetWeddingTaskDoneUseCase @Inject constructor(
    private val repository: WeddingRepository,
) {
    suspend operator fun invoke(id: String, done: Boolean, nowMillis: Long = System.currentTimeMillis()) =
        repository.setTaskDone(id, done, if (done) nowMillis else null)
}

class DeleteWeddingTaskUseCase @Inject constructor(
    private val repository: WeddingRepository,
) {
    suspend operator fun invoke(id: String) = repository.deleteTask(id)
}

class SaveWeddingExpenseUseCase @Inject constructor(
    private val repository: WeddingRepository,
) {
    suspend operator fun invoke(expense: WeddingExpense, isNew: Boolean) = repository.saveExpense(expense, isNew)
}

class DeleteWeddingExpenseUseCase @Inject constructor(
    private val repository: WeddingRepository,
) {
    suspend operator fun invoke(id: String) = repository.deleteExpense(id)
}

class SaveWeddingVendorUseCase @Inject constructor(
    private val repository: WeddingRepository,
) {
    suspend operator fun invoke(vendor: WeddingVendor, isNew: Boolean) = repository.saveVendor(vendor, isNew)
}

/**
 * 업체 삭제. 그 업체로 기록한 지출은 남기고 업체 표시만 뗀다. (PRD 12-5)
 * @param overview 지금 보고 있는 전체 — 지출 목록을 이미 구독 중이라 따로 조회하지 않는다.
 */
class DeleteWeddingVendorUseCase @Inject constructor(
    private val repository: WeddingRepository,
) {
    suspend operator fun invoke(vendorId: String, overview: WeddingOverview) =
        repository.deleteVendor(vendorId, overview.expenses.filter { it.vendorId == vendorId }.map { it.id })
}
