package com.dts.splitthebill.domain

data class RepaymentSuggestion(
    val fromParticipantId: String,
    val toParticipantId: String,
    val amountMinor: Long,
)

object RepaymentSuggester {
    /** Deterministic largest-balance-first suggestions; no minimum-count claim is made. */
    fun suggest(balances: Map<String, Long>): List<RepaymentSuggestion> {
        BalanceCalculator.assertConserved(balances)
        val creditors = balances.filterValues { it > 0 }.toMutableMap()
        val debtors = balances.filterValues { it < 0 }
            .mapValues { (_, value) -> CalculationChecks.negate(value, "debt") }
            .toMutableMap()
        val suggestions = mutableListOf<RepaymentSuggestion>()

        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            val creditor = largestThenId(creditors)
            val debtor = largestThenId(debtors)
            val amount = minOf(creditor.value, debtor.value)
            suggestions += RepaymentSuggestion(debtor.key, creditor.key, amount)

            val creditLeft = creditor.value - amount
            val debtLeft = debtor.value - amount
            if (creditLeft == 0L) creditors.remove(creditor.key) else creditors[creditor.key] = creditLeft
            if (debtLeft == 0L) debtors.remove(debtor.key) else debtors[debtor.key] = debtLeft
        }
        check(creditors.isEmpty() && debtors.isEmpty())
        return suggestions
    }

    private fun largestThenId(values: Map<String, Long>): Map.Entry<String, Long> =
        values.entries.minWith(
            compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key },
        )
}
