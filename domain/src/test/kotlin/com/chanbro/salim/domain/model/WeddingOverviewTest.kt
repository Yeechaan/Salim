package com.chanbro.salim.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class WeddingOverviewTest {

    @Test
    fun `지출 예정은 계약 완료 업체의 남은 금액만 더한다`() {
        val overview = WeddingOverview(
            settings = WeddingSettings(enabled = true, totalBudget = 10_000_000),
            vendors = listOf(
                vendor("hall", VendorStatus.CONTRACTED, contract = 5_000_000),
                vendor("studio", VendorStatus.CONSULTING, contract = 3_000_000),
            ),
            expenses = listOf(expense("e1", 2_000_000, vendorId = "hall")),
        )

        assertEquals(2_000_000L, overview.spentTotal)
        assertEquals(3_000_000L, overview.plannedTotal)
        assertEquals(5_000_000L, overview.remainingBudget)
    }

    @Test
    fun `계약보다 더 쓴 업체는 초과로 보이고 지출 예정은 0이다`() {
        val hall = vendor("hall", VendorStatus.CONTRACTED, contract = 1_000_000)
        val overview = WeddingOverview(
            vendors = listOf(hall),
            expenses = listOf(expense("e1", 1_200_000, vendorId = "hall")),
        )

        assertEquals(-200_000L, overview.remainingFor(hall))
        assertEquals(0L, overview.plannedTotal)
    }

    @Test
    fun `삭제된 업체를 가리키는 지출은 업체 없음으로 본다`() {
        val overview = WeddingOverview(expenses = listOf(expense("e1", 100, vendorId = "gone")))

        assertNull(overview.vendorOf(overview.expenses.single()))
        assertEquals(0L, overview.spentFor("gone"))
    }

    @Test
    fun `예산이 없으면 남은 금액도 없다`() {
        assertNull(WeddingOverview().remainingBudget)
    }

    @Test
    fun `시기 그룹은 항목이 있는 것만, 안에서는 미완료 등록순 다음 완료 최근순`() {
        val overview = WeddingOverview(
            tasks = listOf(
                task("b", WeddingPeriod.M6_3, createdAt = 2),
                task("done-old", WeddingPeriod.M6_3, createdAt = 0, completedAt = 10),
                task("a", WeddingPeriod.M6_3, createdAt = 1),
                task("done-new", WeddingPeriod.M6_3, createdAt = 3, completedAt = 20),
                task("x", WeddingPeriod.M12_6, createdAt = 5),
            ),
        )

        assertEquals(listOf(WeddingPeriod.M12_6, WeddingPeriod.M6_3), overview.taskGroups.map { it.period })
        assertEquals(listOf("a", "b", "done-new", "done-old"), overview.taskGroups[1].tasks.map { it.id })
    }

    @Test
    fun `업체는 계약 완료가 먼저, 같은 상태끼리는 등록순`() {
        val overview = WeddingOverview(
            vendors = listOf(
                vendor("consult", VendorStatus.CONSULTING, createdAt = 1),
                vendor("contract-late", VendorStatus.CONTRACTED, createdAt = 3),
                vendor("contract-early", VendorStatus.CONTRACTED, createdAt = 2),
            ),
        )

        assertEquals(
            listOf("contract-early", "contract-late", "consult"),
            overview.vendorGroups.single().second.map { it.id },
        )
    }

    @Test
    fun `지출은 최근 날짜가 위, 같은 날짜는 나중에 적은 것이 위`() {
        val overview = WeddingOverview(
            expenses = listOf(
                expense("old", 1, date = 1, createdAt = 9),
                expense("today-first", 1, date = 2, createdAt = 1),
                expense("today-second", 1, date = 2, createdAt = 2),
            ),
        )

        assertEquals(listOf("today-second", "today-first", "old"), overview.sortedExpenses.map { it.id })
    }

    @Test
    fun `현재 시기 그룹은 예식일에서 달력으로 거슬러 센다`() {
        val wedding = date(2027, 5, 22)

        assertEquals(WeddingPeriod.M12_6, currentWeddingPeriod(wedding, date(2026, 1, 1)))
        assertEquals(WeddingPeriod.M12_6, currentWeddingPeriod(wedding, date(2026, 11, 21)))
        assertEquals(WeddingPeriod.M6_3, currentWeddingPeriod(wedding, date(2026, 11, 22)))
        assertEquals(WeddingPeriod.M3_1, currentWeddingPeriod(wedding, date(2027, 2, 22)))
        assertEquals(WeddingPeriod.M1_W1, currentWeddingPeriod(wedding, date(2027, 4, 22)))
        assertEquals(WeddingPeriod.M1_W1, currentWeddingPeriod(wedding, date(2027, 5, 14)))
        assertEquals(WeddingPeriod.W1, currentWeddingPeriod(wedding, date(2027, 5, 15)))
        // 예식일이 지난 뒤도 "1주 이내"
        assertEquals(WeddingPeriod.W1, currentWeddingPeriod(wedding, date(2027, 6, 1)))
    }

    @Test
    fun `기본 항목은 고정 id에 목록 순서대로 등록 시각이 벌어진다`() {
        val tasks = DefaultWeddingTasks.tasks(nowMillis = 100)

        assertEquals(DefaultWeddingTasks.all.map { it.id }, tasks.map { it.id })
        assertEquals((100L until 100L + tasks.size).toList(), tasks.map { it.createdAtMillis })
        assertEquals(setOf(TodoAssignee.TOGETHER), tasks.map { it.assignee }.toSet())
    }

    private fun date(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day)
        }.timeInMillis

    private fun vendor(
        id: String,
        status: VendorStatus,
        contract: Long? = null,
        createdAt: Long = 0,
    ) = WeddingVendor(id, id, WeddingItem.VENUE, status, null, contract, null, null, createdAt)

    private fun expense(id: String, amount: Long, vendorId: String? = null, date: Long = 0, createdAt: Long = 0) =
        WeddingExpense(id, amount, date, WeddingItem.VENUE, vendorId, Spender.SHARED, null, createdAt)

    private fun task(id: String, period: WeddingPeriod, createdAt: Long, completedAt: Long? = null) =
        WeddingTask(id, id, period, TodoAssignee.TOGETHER, completedAt != null, completedAt, createdAt)
}
