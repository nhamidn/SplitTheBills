package com.dts.splitthebill.domain

import java.math.BigInteger

sealed interface SplitSpec {
    data class Equal(val participantIds: List<String>) : SplitSpec
    data class Exact(val entries: List<ExactEntry>) : SplitSpec
    data class Percentage(val entries: List<PercentageEntry>) : SplitSpec
    data class Weighted(val entries: List<WeightedEntry>) : SplitSpec
}

data class ExactEntry(val participantId: String, val amountMinor: Long)
data class PercentageEntry(val participantId: String, val basisPoints: Long)
data class WeightedEntry(val participantId: String, val shares: Long)
data class Allocation(val participantId: String, val amountMinor: Long)
data class Contribution(val participantId: String, val amountMinor: Long)

object SplitCalculator {
    fun allocate(totalMinor: Long, split: SplitSpec): List<Allocation> {
        CalculationChecks.safePositive(totalMinor, "totalMinor")
        return when (split) {
            is SplitSpec.Equal -> proportional(
                totalMinor,
                split.participantIds.map { WeightedEntry(it, 1) },
            )
            is SplitSpec.Exact -> exact(totalMinor, split.entries)
            is SplitSpec.Percentage -> percentage(totalMinor, split.entries)
            is SplitSpec.Weighted -> proportional(totalMinor, split.entries)
        }
    }

    private fun exact(totalMinor: Long, entries: List<ExactEntry>): List<Allocation> {
        CalculationChecks.uniqueParticipantIds(entries.map(ExactEntry::participantId))
        var sum = 0L
        val allocations = entries.map { entry ->
            CalculationChecks.safeNonNegative(entry.amountMinor, "exact amount")
            sum = CalculationChecks.add(sum, entry.amountMinor, "exact sum")
            Allocation(entry.participantId, entry.amountMinor)
        }
        if (sum != totalMinor) {
            CalculationChecks.fail(CalculationErrorCode.INVALID_SPLIT, "Exact amounts must sum to total")
        }
        return allocations.sortedBy(Allocation::participantId)
    }

    private fun percentage(totalMinor: Long, entries: List<PercentageEntry>): List<Allocation> {
        CalculationChecks.uniqueParticipantIds(entries.map(PercentageEntry::participantId))
        var sum = 0L
        val weights = entries.map { entry ->
            CalculationChecks.safePositive(entry.basisPoints, "basisPoints")
            sum = CalculationChecks.add(sum, entry.basisPoints, "basisPoints sum")
            WeightedEntry(entry.participantId, entry.basisPoints)
        }
        if (sum != 10_000L) {
            CalculationChecks.fail(CalculationErrorCode.INVALID_SPLIT, "Percentages must sum to 10,000 basis points")
        }
        return proportional(totalMinor, weights)
    }

    private fun proportional(totalMinor: Long, entries: List<WeightedEntry>): List<Allocation> {
        CalculationChecks.uniqueParticipantIds(entries.map(WeightedEntry::participantId))
        var denominator = 0L
        entries.forEach { entry ->
            CalculationChecks.safePositive(entry.shares, "shares")
            denominator = CalculationChecks.add(denominator, entry.shares, "shares sum")
        }

        val denominatorBig = BigInteger.valueOf(denominator)
        data class Row(val participantId: String, var amountMinor: Long, val remainder: BigInteger)

        val rows = entries.map { entry ->
            val product = BigInteger.valueOf(totalMinor).multiply(BigInteger.valueOf(entry.shares))
            val parts = product.divideAndRemainder(denominatorBig)
            Row(entry.participantId, parts[0].longValueExact(), parts[1])
        }
        var allocated = rows.fold(0L) { sum, row ->
            CalculationChecks.add(sum, row.amountMinor, "allocation sum")
        }
        var remainderUnits = totalMinor - allocated
        val remainderOrder = rows.sortedWith(
            compareByDescending<Row> { it.remainder }.thenBy { it.participantId },
        )
        var index = 0
        while (remainderUnits > 0) {
            val row = remainderOrder[index]
            row.amountMinor = CalculationChecks.add(row.amountMinor, 1, "allocation")
            allocated = CalculationChecks.add(allocated, 1, "allocation sum")
            remainderUnits--
            index++
        }
        check(allocated == totalMinor)
        return rows.sortedBy(Row::participantId).map { Allocation(it.participantId, it.amountMinor) }
    }
}
