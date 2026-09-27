package com.chanbro.salim.domain.model

import java.util.Calendar
import java.util.TimeZone

// 결혼 준비 (PRD 12, firestore-schema.md "결혼 준비")
// 가계부와 완전히 분리된 데이터다 — 월 예산·홈 통계 어디에도 섞이지 않는다.

/** 웨딩 항목. 1차는 고정 6개라 문서 없이 enum으로 둔다. 순서가 화면 순서다. */
enum class WeddingItem {
    VENUE,
    SDM,
    GIFTS,
    HOUSEHOLD,
    HONEYMOON,
    ETC,
    ;

    companion object {
        /** 모르는 키(다음 버전에서 추가된 항목 등)는 기타로 읽는다. */
        fun fromKey(key: String?): WeddingItem = entries.firstOrNull { it.name == key } ?: ETC
    }
}

/** 체크리스트 시기 그룹. 순서가 화면 순서다. (wedding.md 12-2) */
enum class WeddingPeriod {
    M12_6,
    M6_3,
    M3_1,
    M1_W1,
    W1,
    ;

    companion object {
        fun fromKey(key: String?): WeddingPeriod = entries.firstOrNull { it.name == key } ?: M12_6
    }
}

/** 설정 > 결혼 준비와 결혼 준비 화면 상단 값. 문서가 없으면 기본값(꺼짐). */
data class WeddingSettings(
    val enabled: Boolean = false,
    /** 예식일 (UTC 자정 millis). null이면 미정. */
    val weddingDateMillis: Long? = null,
    /** 총 웨딩 예산 (원). null이면 미설정. */
    val totalBudget: Long? = null,
    /** 체크리스트 기본 항목을 이미 채웠는지. */
    val templateSeeded: Boolean = false,
)

/** 체크리스트 항목. 담당자·완료 규칙은 할 일과 같다. */
data class WeddingTask(
    val id: String,
    val title: String,
    val period: WeddingPeriod,
    val assignee: TodoAssignee,
    val done: Boolean,
    val completedAtMillis: Long?,
    /** 그룹 안 미완료 정렬 키(등록순). 수정·시기 변경에도 유지한다. */
    val createdAtMillis: Long,
)

/** 웨딩 지출. 시간 없이 날짜만 받는다. */
data class WeddingExpense(
    val id: String,
    val amount: Long,
    /** 지출 날짜 (UTC 자정 millis). */
    val dateMillis: Long,
    val item: WeddingItem,
    val vendorId: String?,
    val spender: Spender,
    val memo: String?,
    val createdAtMillis: Long,
)

enum class VendorStatus {
    CONSULTING,
    CONTRACTED,
}

data class WeddingVendor(
    val id: String,
    val name: String,
    val item: WeddingItem,
    val status: VendorStatus,
    /** 숫자만. 하이픈은 표시할 때 붙인다. */
    val phone: String?,
    val contractAmount: Long?,
    /** 잔금일 (UTC 자정 millis). */
    val balanceDueMillis: Long?,
    val memo: String?,
    val createdAtMillis: Long,
) {
    companion object {
        const val NAME_MAX_LENGTH = 20
    }
}

/** 시기 그룹 하나. 미완료(등록순) 다음에 완료(최근 완료순). */
data class WeddingTaskGroup(
    val period: WeddingPeriod,
    val tasks: List<WeddingTask>,
) {
    val doneCount: Int get() = tasks.count { it.done }
    val openCount: Int get() = tasks.size - doneCount
}

/** 결혼 준비 화면·홈 카드가 보는 전체. 합계는 저장하지 않고 여기서 계산한다. */
data class WeddingOverview(
    val settings: WeddingSettings = WeddingSettings(),
    val tasks: List<WeddingTask> = emptyList(),
    val expenses: List<WeddingExpense> = emptyList(),
    val vendors: List<WeddingVendor> = emptyList(),
) {
    private val vendorById: Map<String, WeddingVendor> by lazy { vendors.associateBy { it.id } }

    /** 업체별 기록한 지출 합. 없는 업체를 가리키는 지출은 업체 없음으로 본다. */
    private val spentByVendor: Map<String, Long> by lazy {
        expenses.filter { it.vendorId != null && it.vendorId in vendorById }
            .groupBy { it.vendorId!! }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    val taskCount: Int get() = tasks.size
    val doneTaskCount: Int get() = tasks.count { it.done }

    /** 지출이 가리키는 업체. 삭제됐으면 null. */
    fun vendorOf(expense: WeddingExpense): WeddingVendor? = expense.vendorId?.let(vendorById::get)

    fun vendor(id: String): WeddingVendor? = vendorById[id]

    fun spentFor(vendorId: String): Long = spentByVendor[vendorId] ?: 0L

    /** 계약 금액 − 기록한 지출. 계약 금액이 없으면 null. 음수면 초과. */
    fun remainingFor(vendor: WeddingVendor): Long? = vendor.contractAmount?.let { it - spentFor(vendor.id) }

    /** 지출 예정에 더하는 금액 — 계약 완료 업체의 남은 금액만, 0 미만은 0. (wedding.md 12-7) */
    fun plannedFor(vendor: WeddingVendor): Long =
        if (vendor.status == VendorStatus.CONTRACTED) (remainingFor(vendor) ?: 0L).coerceAtLeast(0L) else 0L

    val spentTotal: Long get() = expenses.sumOf { it.amount }
    val plannedTotal: Long get() = vendors.sumOf { plannedFor(it) }

    fun spentBy(item: WeddingItem): Long = expenses.filter { it.item == item }.sumOf { it.amount }
    fun plannedBy(item: WeddingItem): Long = vendors.filter { it.item == item }.sumOf { plannedFor(it) }

    /** 총 예산 − 지출 완료 − 지출 예정. 예산이 없으면 null. */
    val remainingBudget: Long? get() = settings.totalBudget?.let { it - spentTotal - plannedTotal }

    /** 최근 날짜가 위, 같은 날짜 안에서는 나중에 적은 것이 위. */
    val sortedExpenses: List<WeddingExpense>
        get() = expenses.sortedWith(
            compareByDescending<WeddingExpense> { it.dateMillis }.thenByDescending { it.createdAtMillis }.thenBy { it.id },
        )

    /** 항목이 있는 시기 그룹만, 시기 순서대로. */
    val taskGroups: List<WeddingTaskGroup>
        get() = tasks.groupBy { it.period }.let { byPeriod ->
            WeddingPeriod.entries.mapNotNull { period ->
                val list = byPeriod[period] ?: return@mapNotNull null
                val (done, open) = list.partition { it.done }
                WeddingTaskGroup(
                    period,
                    open.sortedWith(compareBy<WeddingTask> { it.createdAtMillis }.thenBy { it.id }) +
                        done.sortedWith(
                            compareByDescending<WeddingTask> { it.completedAtMillis ?: Long.MIN_VALUE }.thenBy { it.id },
                        ),
                )
            }
        }

    /** 업체가 있는 웨딩 항목만. 그룹 안은 계약 완료 → 상담 중, 각각 등록순. */
    val vendorGroups: List<Pair<WeddingItem, List<WeddingVendor>>>
        get() = vendors.groupBy { it.item }.let { byItem ->
            WeddingItem.entries.mapNotNull { item ->
                byItem[item]?.let { list ->
                    item to list.sortedWith(
                        compareBy<WeddingVendor> { if (it.status == VendorStatus.CONTRACTED) 0 else 1 }
                            .thenBy { it.createdAtMillis }
                            .thenBy { it.id },
                    )
                }
            }
        }

    /** 이 업체로 기록한 지출. 최근 날짜가 위. */
    fun expensesOf(vendorId: String): List<WeddingExpense> = sortedExpenses.filter { it.vendorId == vendorId }
}

/**
 * 오늘이 속한 시기 그룹. 예식일에서 달력으로 거슬러 센다. (wedding.md 12-2 판정표)
 * 예식일이 지난 뒤는 "1주 이내"로 본다.
 */
fun currentWeddingPeriod(weddingDateMillis: Long, todayMillis: Long): WeddingPeriod {
    fun before(months: Int = 0, days: Int = 0): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = weddingDateMillis
            add(Calendar.MONTH, -months)
            add(Calendar.DAY_OF_MONTH, -days)
        }.timeInMillis
    return when {
        todayMillis < before(months = 6) -> WeddingPeriod.M12_6
        todayMillis < before(months = 3) -> WeddingPeriod.M6_3
        todayMillis < before(months = 1) -> WeddingPeriod.M3_1
        todayMillis < before(days = 7) -> WeddingPeriod.M1_W1
        else -> WeddingPeriod.W1
    }
}

/** 저장할 제목. 할 일과 같은 규칙(앞뒤 공백 제거, 30자). 비어 있으면 null. */
fun normalizeWeddingTaskTitle(title: String): String? = normalizeTodoTitle(title)

/**
 * 처음 켤 때 한 번 채우는 체크리스트. (wedding.md 12-2 기본 항목 — 초안)
 * id를 고정해 두 사람이 동시에 켜도 같은 문서를 덮어쓸 뿐 중복되지 않는다.
 */
object DefaultWeddingTasks {
    data class Entry(val id: String, val title: String, val period: WeddingPeriod)

    val all: List<Entry> = listOf(
        Entry("default_meeting", "상견례", WeddingPeriod.M12_6),
        Entry("default_venue", "예식장 계약", WeddingPeriod.M12_6),
        Entry("default_sdm", "스드메 계약", WeddingPeriod.M12_6),
        Entry("default_honeymoon", "신혼여행 예약", WeddingPeriod.M6_3),
        Entry("default_gifts", "예물·예단 준비", WeddingPeriod.M6_3),
        Entry("default_attire", "혼주 한복/예복", WeddingPeriod.M6_3),
        Entry("default_invitation", "청첩장 제작", WeddingPeriod.M3_1),
        Entry("default_program", "식순·사회자 섭외", WeddingPeriod.M3_1),
        Entry("default_invitation_send", "청첩장 발송", WeddingPeriod.M1_W1),
        Entry("default_headcount", "최종 인원 확정", WeddingPeriod.M1_W1),
    )

    /** 목록 순서대로 보이도록 등록 시각을 1ms씩 벌린다. 담당자는 "우리". */
    fun tasks(nowMillis: Long): List<WeddingTask> = all.mapIndexed { index, entry ->
        WeddingTask(
            id = entry.id,
            title = entry.title,
            period = entry.period,
            assignee = TodoAssignee.TOGETHER,
            done = false,
            completedAtMillis = null,
            createdAtMillis = nowMillis + index,
        )
    }
}
