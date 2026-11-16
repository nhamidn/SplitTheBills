package com.dts.splitthebill.ui

import com.dts.splitthebill.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ExpenseSplitsTest {
    private val group = GroupDetail("g", "Trip", "EUR", 2, "alice", 1, false, "a", emptyList(),
        listOf(Participant("a", "Alice", "alice", true), Participant("b", "Bob", "bob", true), Participant("c", "Cara", null, true)),
        emptyList(), emptyList(), emptyList(), emptyList(), false)

    @Test fun equalPreviewDistributesEveryCentUsingTheServerTieBreak() {
        val editor = ExpenseEditorState(amount = "10.00", selectedParticipantIds = setOf("c", "b", "a"))
        assertEquals(mapOf("a" to 334L, "b" to 333L, "c" to 333L), splitAllocations(editor, group).associate { it.participantId to it.amountMinor })
    }

    @Test fun percentagesAcceptTwoDecimalsAndPreviewConservesTheTotal() {
        val editor = ExpenseEditorState(amount = "10.01", selectedParticipantIds = setOf("a", "b"),
            splitMode = SplitMode.PERCENTAGE, entryValues = mapOf("a" to "33.33", "b" to "66.67"))
        assertEquals(listOf(3333L, 6667L), splitEntries(editor, group).map { it.value })
        assertEquals(listOf(334L, 667L), splitAllocations(editor, group).map { it.amountMinor })
        assertEquals("50", percentageText(5000))
        assertEquals("33.33", percentageText(3333))
        assertEquals("100", percentageText(10000))
    }

    @Test fun weightedAndExactSplitsUseTheirOwnUnits() {
        val editor = ExpenseEditorState(amount = "10.00", selectedParticipantIds = setOf("a", "b"),
            splitMode = SplitMode.WEIGHTED, entryValues = mapOf("a" to "1", "b" to "3"))
        assertEquals(listOf(250L, 750L), splitAllocations(editor, group).map { it.amountMinor })
        assertEquals(listOf(100L, 900L), splitAllocations(editor.copy(splitMode = SplitMode.EXACT,
            entryValues = mapOf("a" to "1.00", "b" to "9.00")), group).map { it.amountMinor })
    }

    @Test fun invalidPercentagesWeightsAndTotalsCannotBeSubmitted() {
        val editor = ExpenseEditorState(amount = "10.00", selectedParticipantIds = setOf("a", "b"), splitMode = SplitMode.PERCENTAGE)
        listOf(mapOf("a" to "50", "b" to "49"), mapOf("a" to "50.001", "b" to "49.999"), mapOf("a" to "100", "b" to "0")).forEach {
            assertTrue(runCatching { splitAllocations(editor.copy(entryValues = it), group) }.isFailure)
        }
        assertTrue(runCatching { splitAllocations(editor.copy(splitMode = SplitMode.WEIGHTED,
            entryValues = mapOf("a" to "1.5", "b" to "1")), group) }.isFailure)
        assertTrue(runCatching { splitAllocations(editor.copy(splitMode = SplitMode.EXACT,
            entryValues = mapOf("a" to "3", "b" to "3")), group) }.isFailure)
    }
}
