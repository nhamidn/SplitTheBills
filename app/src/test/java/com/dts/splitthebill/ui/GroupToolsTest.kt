package com.dts.splitthebill.ui

import com.dts.splitthebill.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class GroupToolsTest {
    @Test fun currenciesStaySeparateAndCreditAndDebtBothSurviveNetting() {
        val totals = currencyBalances(listOf(group("a", 900), group("b", -300), group("c", 700, "USD")))
        assertEquals(CurrencyBalance("EUR", 2, 900, 300), totals.first())
        assertEquals(600L, totals.first().net)
        assertEquals(CurrencyBalance("USD", 2, 700, 0), totals.last())
    }

    @Test fun transactionSearchIncludesAllGroupsAndInterleavesRepayments() {
        val a = group("a", 500).copy(expenses = listOf(expense("Lunch", "2026-11-01")))
        val b = group("b", 0).copy(settlements = listOf(Settlement("s1", "p2", "p1", 500, "EUR", "2026-11-02", "Train fare", "bob", 1)))
        val records = transactions(listOf(a, b))
        assertEquals(listOf("s1", "e1"), records.map { it.settlement?.id ?: it.expense!!.id })
        assertTrue(records.first().matches("train"))
        assertTrue(records.last().matches("lunch"))
        assertFalse(records.last().matches("missing"))
    }

    @Test fun notificationsExcludeOwnWritesAndUseServerTimestampsAcrossGroups() {
        fun event(id: String, actor: String, at: Long) = ActivityEvent(id, "EXPENSE_CREATED", "EXPENSE", "Lunch", actor, 1, at)
        val a = group("a", 0).copy(activity = listOf(event("own", "alice", 30), event("old", "bob", 10)))
        val b = group("b", 0).copy(activity = listOf(event("new", "bob", 20), event("pending", "bob", 0)))
        assertEquals(listOf("new", "old"), notifications(listOf(a, b), "alice").map { it.event.id })
    }

    @Test fun remindersOnlyIncludeLiveAmountsOwedToThisUser() {
        val candidates = listOf(group("owed", 500), group("owe", -300), group("archived", 600).copy(archived = true), group("cached", 700).copy(fromCache = true))
        val reminder = reminders(candidates, true).single()
        assertEquals("owed", reminder.group.id)
        assertTrue(reminders(listOf(group("paid", 0)), true).isEmpty())
    }

    @Test fun csvQuotesNewlinesCommasAndQuotesAndNeutralizesSpreadsheetFormulas() {
        val record = expense("=HYPERLINK(\"bad\")", "2026-11-01").copy(note = "A, B\n\"quoted\"")
        val csv = groupCsv(group("a", 500).copy(name = "Trip, home", expenses = listOf(record)))
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"bad\"\")\""))
        assertTrue(csv.contains("\"A, B\n\"\"quoted\"\"\""))
        assertTrue(csv.contains("\"10.00\""))
        assertTrue(csv.contains("Alice: 10.00"))
        assertTrue(csv.endsWith("\r\n"))
    }

    @Test fun originalCounterpartiesAndSimplifiedTransfersProduceTheSameNetBalances() {
        val first = expense("Lunch", "2026-11-01") // Bob owes Alice 500.
        val second = expense("Taxi", "2026-11-02").copy(id = "e2", contributions = listOf(Contribution("p2", 1000)),
            allocations = listOf(Allocation("p2", 500), Allocation("p3", 500))) // Cara owes Bob 500.
        val g = group("a", 500).copy(participants = group("a", 0).participants + Participant("p3", "Cara", "cara", true),
            expenses = listOf(first, second), balances = listOf(ParticipantBalance("p1", 500), ParticipantBalance("p2", 0), ParticipantBalance("p3", -500)))
        val direct = repaymentPlan(g, false)
        assertEquals(setOf("p2" to "p1", "p3" to "p2"), direct.map { it.fromParticipantId to it.toParticipantId }.toSet())
        val simplified = repaymentPlan(g, true).single()
        assertEquals("p3", simplified.fromParticipantId)
        assertEquals("p1", simplified.toParticipantId)
        assertEquals(500L, simplified.amountMinor)
    }

    @Test fun recordedRepaymentReducesTheOriginalPair() {
        val g = group("a", 300).copy(expenses = listOf(expense("Lunch", "2026-11-01")),
            settlements = listOf(Settlement("s1", "p2", "p1", 200, "EUR", "2026-11-02", null, "bob", 1)))
        assertEquals(300L, repaymentPlan(g, false).single().amountMinor)
    }

    @Test fun inconsistentSnapshotsDoNotProduceActionableOriginalDebtReminders() {
        val g = group("a", 0).copy(expenses = listOf(expense("Lunch", "2026-11-01")))
        assertTrue(runCatching { repaymentPlan(g, false) }.isFailure)
        assertTrue(runCatching { repaymentPlan(g, true) }.isFailure)
        assertTrue(reminders(listOf(g), true).isEmpty())
        assertEquals("valid", reminders(listOf(g, group("valid", 500)), true).single().group.id)
    }

    private fun group(id: String, balance: Long, currency: String = "EUR") = GroupDetail(
        id, "Group $id", currency, 2, "alice", 1, false, "p1",
        listOf(GroupMember("alice", "OWNER", "p1"), GroupMember("bob", "MEMBER", "p2")),
        listOf(Participant("p1", "Alice", "alice", true), Participant("p2", "Bob", "bob", true)),
        if (balance == 0L) emptyList() else listOf(expense("Lunch", "2026-11-01").copy(
            amountMinor = kotlin.math.abs(balance) * 2,
            contributions = listOf(Contribution(if (balance > 0) "p1" else "p2", kotlin.math.abs(balance) * 2)),
            allocations = listOf(Allocation("p1", kotlin.math.abs(balance)), Allocation("p2", kotlin.math.abs(balance))),
        )), emptyList(), listOf(ParticipantBalance("p1", balance), ParticipantBalance("p2", -balance)), emptyList(), false,
    )
    private fun expense(title: String, date: String) = Expense("e1", title, 1000, "EUR",
        listOf(Contribution("p1", 1000)), listOf(Allocation("p1", 500), Allocation("p2", 500)), SplitMode.EQUAL,
        listOf(SplitEntryInput("p1"), SplitEntryInput("p2")), date, "FOOD", null, "alice", 1)
}
