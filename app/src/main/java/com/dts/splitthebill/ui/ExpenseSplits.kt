package com.dts.splitthebill.ui

import com.dts.splitthebill.domain.Allocation
import com.dts.splitthebill.domain.CurrencyUnit
import com.dts.splitthebill.domain.ExactEntry
import com.dts.splitthebill.domain.MoneyText
import com.dts.splitthebill.domain.PercentageEntry
import com.dts.splitthebill.domain.SplitCalculator
import com.dts.splitthebill.domain.SplitSpec
import com.dts.splitthebill.domain.WeightedEntry
import com.dts.splitthebill.domain.model.GroupDetail
import com.dts.splitthebill.domain.model.SplitEntryInput
import com.dts.splitthebill.domain.model.SplitMode

// Percent fields use ordinary percentages (50 or 33.33); the API uses basis points.
internal fun percentageText(basisPoints: Long): String =
    MoneyText.formatMinor(basisPoints, CurrencyUnit("PCT", 2)).trimEnd('0').trimEnd('.')

internal fun splitEntries(editor: ExpenseEditorState, group: GroupDetail): List<SplitEntryInput> {
    require(editor.selectedParticipantIds.isNotEmpty()) { "Select at least one participant." }
    val active = group.participants.filter { it.active }.map { it.id }.toSet()
    require(active.containsAll(editor.selectedParticipantIds)) { "A selected participant is no longer active. Review the split." }
    return editor.selectedParticipantIds.sorted().map { id ->
        val text = editor.entryValues[id].orEmpty().trim()
        val value = when (editor.splitMode) {
            SplitMode.EQUAL -> null
            SplitMode.EXACT -> MoneyText.parseUnsignedMinor(text, CurrencyUnit(group.currency, group.currencyExponent))
            SplitMode.PERCENTAGE -> {
                val basisPoints = runCatching { MoneyText.parseUnsignedMinor(text, CurrencyUnit("PCT", 2)) }
                    .getOrElse { throw IllegalArgumentException("Enter a percentage with at most two decimal places for each person.") }
                require(basisPoints in 1..10_000) { "Each percentage must be greater than 0 and at most 100." }
                basisPoints
            }
            SplitMode.WEIGHTED -> text.toLongOrNull()?.takeIf { it > 0 }
                ?: throw IllegalArgumentException("Enter a positive whole number of shares for each person.")
        }
        SplitEntryInput(id, value)
    }.also { entries ->
        if (editor.splitMode == SplitMode.PERCENTAGE) {
            require(entries.sumOf { requireNotNull(it.value) } == 10_000L) { "Percentages must add up to 100%." }
        }
    }
}

internal fun splitAllocations(editor: ExpenseEditorState, group: GroupDetail): List<Allocation> {
    val entries = splitEntries(editor, group)
    val total = MoneyText.parseUnsignedMinor(editor.amount.trim(), CurrencyUnit(group.currency, group.currencyExponent))
    val split = when (editor.splitMode) {
        SplitMode.EQUAL -> SplitSpec.Equal(entries.map { it.participantId })
        SplitMode.EXACT -> SplitSpec.Exact(entries.map { ExactEntry(it.participantId, requireNotNull(it.value)) })
        SplitMode.PERCENTAGE -> SplitSpec.Percentage(entries.map { PercentageEntry(it.participantId, requireNotNull(it.value)) })
        SplitMode.WEIGHTED -> SplitSpec.Weighted(entries.map { WeightedEntry(it.participantId, requireNotNull(it.value)) })
    }
    return SplitCalculator.allocate(total, split)
}
