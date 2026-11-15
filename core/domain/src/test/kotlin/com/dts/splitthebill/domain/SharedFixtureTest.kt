package com.dts.splitthebill.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import kotlin.test.assertEquals

class SharedFixtureTest {
    private val fixture: JsonObject = requireNotNull(
        javaClass.classLoader?.getResourceAsStream("fixtures/splits-v1.json"),
    ).bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject }

    @Test
    fun sharedMoneyTextFixturesMatch() {
        fixture.array("moneyTextCases").forEach { element ->
            val case = element.jsonObject
            val currency = CurrencyUnit(case.string("code"), case.int("exponent"))
            val minor = MoneyText.parseUnsignedMinor(case.string("input"), currency)
            assertEquals(case.long("expectedMinor"), minor, case.string("id"))
            assertEquals(case.string("expectedFormatted"), MoneyText.formatMinor(minor, currency), case.string("id"))
        }
    }

    @Test
    fun sharedSplitFixturesMatch() {
        fixture.array("cases").forEach { element ->
            val case = element.jsonObject
            val splitObject = case.objectValue("split")
            val entries = splitObject.array("entries").map { it.jsonObject }
            val split = when (splitObject.string("mode")) {
                "EQUAL" -> SplitSpec.Equal(entries.map { it.string("participantId") })
                "EXACT" -> SplitSpec.Exact(entries.map { ExactEntry(it.string("participantId"), it.long("amountMinor")) })
                "PERCENTAGE" -> SplitSpec.Percentage(
                    entries.map { PercentageEntry(it.string("participantId"), it.long("basisPoints")) },
                )
                "WEIGHTED" -> SplitSpec.Weighted(
                    entries.map { WeightedEntry(it.string("participantId"), it.long("shares")) },
                )
                else -> error("Unknown fixture mode")
            }
            val expected = case.array("expectedAllocations").map {
                Allocation(it.jsonObject.string("participantId"), it.jsonObject.long("amountMinor"))
            }
            assertEquals(expected, SplitCalculator.allocate(case.long("amountMinor"), split), case.string("id"))
        }
    }

    @Test
    fun sharedBalanceAndRepaymentFixturesMatch() {
        fixture.array("balanceCases").forEach { element ->
            val case = element.jsonObject
            val expenseObject = case.objectValue("expense")
            val expense = ExpenseFinancials(
                totalMinor = expenseObject.long("totalMinor"),
                contributions = expenseObject.array("contributions").map {
                    Contribution(it.jsonObject.string("participantId"), it.jsonObject.long("amountMinor"))
                },
                allocations = expenseObject.array("allocations").map {
                    Allocation(it.jsonObject.string("participantId"), it.jsonObject.long("amountMinor"))
                },
            )
            val expenseBalances = BalanceCalculator.applyDeltas(emptyMap(), BalanceCalculator.expenseDeltas(expense))
            assertEquals(case.longMap("expectedExpenseBalances"), expenseBalances, case.string("id"))

            val settlementObject = case.objectValue("settlement")
            val settlement = SettlementFinancials(
                fromParticipantId = settlementObject.string("fromParticipantId"),
                toParticipantId = settlementObject.string("toParticipantId"),
                amountMinor = settlementObject.long("amountMinor"),
            )
            val afterSettlement = BalanceCalculator.applyDeltas(
                expenseBalances,
                BalanceCalculator.settlementDeltas(settlement),
            )
            assertEquals(case.longMap("expectedAfterSettlement"), afterSettlement, case.string("id"))
            assertEquals(
                expenseBalances,
                BalanceCalculator.applyDeltas(
                    afterSettlement,
                    BalanceCalculator.settlementDeltas(settlement),
                    DeltaDirection.REVERSE,
                ),
                case.string("id"),
            )
        }

        fixture.array("repaymentCases").forEach { element ->
            val case = element.jsonObject
            val expected = case.array("expected").map {
                RepaymentSuggestion(
                    it.jsonObject.string("fromParticipantId"),
                    it.jsonObject.string("toParticipantId"),
                    it.jsonObject.long("amountMinor"),
                )
            }
            assertEquals(expected, RepaymentSuggester.suggest(case.longMap("balances")), case.string("id"))
        }
    }

    private fun JsonObject.array(key: String): JsonArray = getValue(key).jsonArray
    private fun JsonObject.objectValue(key: String): JsonObject = getValue(key).jsonObject
    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content
    private fun JsonObject.long(key: String): Long = getValue(key).jsonPrimitive.content.toLong()
    private fun JsonObject.int(key: String): Int = getValue(key).jsonPrimitive.content.toInt()
    private fun JsonObject.longMap(key: String): Map<String, Long> =
        objectValue(key).mapValues { (_, value) -> (value as JsonPrimitive).content.toLong() }
}
