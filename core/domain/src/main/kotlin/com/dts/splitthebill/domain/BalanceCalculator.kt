package com.dts.splitthebill.domain

import java.math.BigInteger

data class BalanceDelta(val participantId: String, val amountMinor: Long)

data class ExpenseFinancials(
    val totalMinor: Long,
    val contributions: List<Contribution>,
    val allocations: List<Allocation>,
)

data class SettlementFinancials(
    val fromParticipantId: String,
    val toParticipantId: String,
    val amountMinor: Long,
)

enum class DeltaDirection { APPLY, REVERSE }

object BalanceCalculator {
    fun expenseDeltas(expense: ExpenseFinancials): List<BalanceDelta> {
        CalculationChecks.safePositive(expense.totalMinor, "expense total")
        validateContributions(expense)
        validateAllocations(expense)

        val deltas = mutableMapOf<String, Long>()
        expense.contributions.forEach { contribution ->
            addDelta(deltas, contribution.participantId, contribution.amountMinor)
        }
        expense.allocations.forEach { allocation ->
            addDelta(deltas, allocation.participantId, -allocation.amountMinor)
        }
        return normalizedDeltas(deltas)
    }

    fun settlementDeltas(settlement: SettlementFinancials): List<BalanceDelta> {
        val from = CalculationChecks.participantId(settlement.fromParticipantId)
        val to = CalculationChecks.participantId(settlement.toParticipantId)
        if (from == to) {
            CalculationChecks.fail(CalculationErrorCode.INVALID_PARTICIPANT, "Settlement endpoints must differ")
        }
        CalculationChecks.safePositive(settlement.amountMinor, "settlement amount")
        return listOf(
            BalanceDelta(from, settlement.amountMinor),
            BalanceDelta(to, -settlement.amountMinor),
        ).sortedBy(BalanceDelta::participantId)
    }

    fun applyDeltas(
        balances: Map<String, Long>,
        deltas: List<BalanceDelta>,
        direction: DeltaDirection = DeltaDirection.APPLY,
    ): Map<String, Long> {
        assertConserved(balances)
        val updated = balances.toMutableMap()
        deltas.forEach { delta ->
            val participantId = CalculationChecks.participantId(delta.participantId)
            CalculationChecks.safeSigned(delta.amountMinor, "delta")
            val directed = if (direction == DeltaDirection.APPLY) {
                delta.amountMinor
            } else {
                CalculationChecks.negate(delta.amountMinor, "reversed delta")
            }
            updated[participantId] = CalculationChecks.add(
                updated[participantId] ?: 0,
                directed,
                "participant balance",
            )
        }
        assertConserved(updated)
        return updated.toSortedMap()
    }

    fun replaceExpense(
        balances: Map<String, Long>,
        previous: ExpenseFinancials,
        replacement: ExpenseFinancials,
    ): Map<String, Long> = applyDeltas(
        applyDeltas(balances, expenseDeltas(previous), DeltaDirection.REVERSE),
        expenseDeltas(replacement),
    )

    fun assertConserved(balances: Map<String, Long>) {
        var sum = BigInteger.ZERO
        balances.forEach { (participantId, amount) ->
            CalculationChecks.participantId(participantId)
            CalculationChecks.safeSigned(amount, "balance")
            sum = sum.add(BigInteger.valueOf(amount))
        }
        if (sum != BigInteger.ZERO) {
            CalculationChecks.fail(CalculationErrorCode.BALANCES_NOT_CONSERVED, "Group balances must sum to zero")
        }
    }

    private fun validateContributions(expense: ExpenseFinancials) {
        CalculationChecks.uniqueParticipantIds(expense.contributions.map(Contribution::participantId))
        var sum = 0L
        expense.contributions.forEach {
            CalculationChecks.safePositive(it.amountMinor, "contribution")
            sum = CalculationChecks.add(sum, it.amountMinor, "contribution sum")
        }
        if (sum != expense.totalMinor) {
            CalculationChecks.fail(
                CalculationErrorCode.CONTRIBUTIONS_DO_NOT_SUM,
                "Contributions must sum to the expense total",
            )
        }
    }

    private fun validateAllocations(expense: ExpenseFinancials) {
        CalculationChecks.uniqueParticipantIds(expense.allocations.map(Allocation::participantId))
        var sum = 0L
        expense.allocations.forEach {
            CalculationChecks.safeNonNegative(it.amountMinor, "allocation")
            sum = CalculationChecks.add(sum, it.amountMinor, "allocation sum")
        }
        if (sum != expense.totalMinor) {
            CalculationChecks.fail(
                CalculationErrorCode.ALLOCATIONS_DO_NOT_SUM,
                "Allocations must sum to the expense total",
            )
        }
    }

    private fun addDelta(target: MutableMap<String, Long>, participantId: String, amount: Long) {
        CalculationChecks.participantId(participantId)
        target[participantId] = CalculationChecks.add(target[participantId] ?: 0, amount, "expense delta")
    }

    private fun normalizedDeltas(deltas: Map<String, Long>): List<BalanceDelta> {
        assertConserved(deltas)
        return deltas.entries
            .filter { it.value != 0L }
            .sortedBy(Map.Entry<String, Long>::key)
            .map { BalanceDelta(it.key, it.value) }
    }
}
