package com.dts.splitthebill.domain

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CalculationValidationTest {
    @Test
    fun rejectsInvalidPercentageWeightExactAndDuplicateInputs() {
        assertCode(CalculationErrorCode.INVALID_SPLIT) {
            SplitCalculator.allocate(
                100,
                SplitSpec.Percentage(listOf(PercentageEntry("p-a", 5_000), PercentageEntry("p-b", 4_999))),
            )
        }
        assertCode(CalculationErrorCode.INVALID_AMOUNT) {
            SplitCalculator.allocate(100, SplitSpec.Weighted(listOf(WeightedEntry("p-a", 0))))
        }
        assertCode(CalculationErrorCode.INVALID_SPLIT) {
            SplitCalculator.allocate(100, SplitSpec.Exact(listOf(ExactEntry("p-a", 99))))
        }
        assertCode(CalculationErrorCode.DUPLICATE_PARTICIPANT) {
            SplitCalculator.allocate(100, SplitSpec.Equal(listOf("p-a", "p-a")))
        }
    }

    @Test
    fun exactIntermediatesHandleSafeMaximumWithoutOverflow() {
        val allocations = SplitCalculator.allocate(
            MAX_SAFE_INTEGER,
            SplitSpec.Weighted(
                listOf(
                    WeightedEntry("p-a", MAX_SAFE_INTEGER - 1),
                    WeightedEntry("p-b", 1),
                ),
            ),
        )
        assertEquals(MAX_SAFE_INTEGER - 1, allocations[0].amountMinor)
        assertEquals(1, allocations[1].amountMinor)
    }

    @Test
    fun allocationIsConservedAndIndependentOfInputOrder() {
        val totals = listOf(1L, 2L, 7L, 101L, 10_001L, MAX_SAFE_INTEGER)
        totals.forEach { total ->
            val forward = SplitCalculator.allocate(
                total,
                SplitSpec.Weighted(
                    listOf(WeightedEntry("p-a", 7), WeightedEntry("p-b", 3), WeightedEntry("p-c", 11)),
                ),
            )
            val reversed = SplitCalculator.allocate(
                total,
                SplitSpec.Weighted(
                    listOf(WeightedEntry("p-c", 11), WeightedEntry("p-b", 3), WeightedEntry("p-a", 7)),
                ),
            )
            assertEquals(total, forward.sumOf(Allocation::amountMinor))
            assertEquals(forward, reversed)
        }
    }

    @Test
    fun rejectsUnsafeSumsAndOverPreciseText() {
        assertCode(CalculationErrorCode.OVERFLOW) {
            SplitCalculator.allocate(
                100,
                SplitSpec.Weighted(
                    listOf(WeightedEntry("p-a", MAX_SAFE_INTEGER), WeightedEntry("p-b", 1)),
                ),
            )
        }
        assertCode(CalculationErrorCode.INVALID_AMOUNT) {
            MoneyText.parseUnsignedMinor("1.001", CurrencyUnit("EUR", 2))
        }
        assertCode(CalculationErrorCode.OVERFLOW) {
            MoneyText.parseUnsignedMinor("90071992547410.00", CurrencyUnit("EUR", 2))
        }
    }

    @Test
    fun validatesContributionsAllocationsAndConservation() {
        assertCode(CalculationErrorCode.CONTRIBUTIONS_DO_NOT_SUM) {
            BalanceCalculator.expenseDeltas(
                ExpenseFinancials(100, listOf(Contribution("p-a", 99)), listOf(Allocation("p-a", 100))),
            )
        }
        assertCode(CalculationErrorCode.ALLOCATIONS_DO_NOT_SUM) {
            BalanceCalculator.expenseDeltas(
                ExpenseFinancials(100, listOf(Contribution("p-a", 100)), listOf(Allocation("p-a", 99))),
            )
        }
        assertCode(CalculationErrorCode.BALANCES_NOT_CONSERVED) {
            RepaymentSuggester.suggest(mapOf("p-a" to 10, "p-b" to -9))
        }
    }

    @Test
    fun replacingExpenseReversesOldDeltasBeforeApplyingNewOnes() {
        val oldExpense = ExpenseFinancials(
            100,
            listOf(Contribution("p-a", 100)),
            listOf(Allocation("p-a", 50), Allocation("p-b", 50)),
        )
        val newExpense = ExpenseFinancials(
            120,
            listOf(Contribution("p-b", 120)),
            listOf(Allocation("p-a", 40), Allocation("p-b", 80)),
        )
        val current = BalanceCalculator.applyDeltas(emptyMap(), BalanceCalculator.expenseDeltas(oldExpense))
        assertEquals(
            mapOf("p-a" to -40L, "p-b" to 40L),
            BalanceCalculator.replaceExpense(current, oldExpense, newExpense),
        )
        assertEquals(
            mapOf("p-a" to 0L, "p-b" to 0L),
            BalanceCalculator.applyDeltas(
                current,
                BalanceCalculator.expenseDeltas(oldExpense),
                DeltaDirection.REVERSE,
            ),
        )
    }

    private fun assertCode(code: CalculationErrorCode, block: () -> Unit) {
        assertEquals(code, assertFailsWith<CalculationException>(block = block).code)
    }
}
