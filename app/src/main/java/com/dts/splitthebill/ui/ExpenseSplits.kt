package com.dts.splitthebill.ui

import com.dts.splitthebill.R
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
    requireUi(editor.selectedParticipantIds.isNotEmpty()) { uiText(R.string.select_at_least_one_participant) }
    val active = group.participants.filter { it.active }.map { it.id }.toSet()
    requireUi(active.containsAll(editor.selectedParticipantIds)) { uiText(R.string.a_selected_participant_is_no_longer_active_review_the_split) }
    return editor.selectedParticipantIds.sorted().map { id ->
        val text = canonicalDecimal(editor.entryValues[id].orEmpty())
        val value = when (editor.splitMode) {
            SplitMode.EQUAL -> null
            SplitMode.EXACT -> MoneyText.parseUnsignedMinor(text, CurrencyUnit(group.currency, group.currencyExponent))
            SplitMode.PERCENTAGE -> {
                val basisPoints = runCatching { MoneyText.parseUnsignedMinor(text, CurrencyUnit("PCT", 2)) }
                    .getOrElse { throw UiTextException(uiText(R.string.enter_a_percentage_with_at_most_two_decimal_places_for_each_perso)) }
                requireUi(basisPoints in 1..10_000) { uiText(R.string.each_percentage_must_be_greater_than_0_and_at_most_100) }
                basisPoints
            }
            SplitMode.WEIGHTED -> text.toLongOrNull()?.takeIf { it > 0 }
                ?: throw UiTextException(uiText(R.string.enter_a_positive_whole_number_of_shares_for_each_person))
        }
        SplitEntryInput(id, value)
    }.also { entries ->
        if (editor.splitMode == SplitMode.PERCENTAGE) {
            requireUi(entries.sumOf { requireNotNull(it.value) } == 10_000L) { uiText(R.string.percentages_must_add_up_to_exactly_100) }
        }
    }
}

internal fun splitAllocations(editor: ExpenseEditorState, group: GroupDetail): List<Allocation> {
    val entries = splitEntries(editor, group)
    val total = MoneyText.parseUnsignedMinor(canonicalDecimal(editor.amount), CurrencyUnit(group.currency, group.currencyExponent))
    val split = when (editor.splitMode) {
        SplitMode.EQUAL -> SplitSpec.Equal(entries.map { it.participantId })
        SplitMode.EXACT -> SplitSpec.Exact(entries.map { ExactEntry(it.participantId, requireNotNull(it.value)) })
        SplitMode.PERCENTAGE -> SplitSpec.Percentage(entries.map { PercentageEntry(it.participantId, requireNotNull(it.value)) })
        SplitMode.WEIGHTED -> SplitSpec.Weighted(entries.map { WeightedEntry(it.participantId, requireNotNull(it.value)) })
    }
    return SplitCalculator.allocate(total, split)
}
