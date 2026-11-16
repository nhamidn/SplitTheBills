package com.dts.splitthebill.ui

import com.dts.splitthebill.domain.BalanceCalculator
import com.dts.splitthebill.domain.CurrencyUnit
import com.dts.splitthebill.domain.ExpenseFinancials
import com.dts.splitthebill.domain.MoneyText
import com.dts.splitthebill.domain.RepaymentSuggestion
import com.dts.splitthebill.domain.RepaymentSuggester
import com.dts.splitthebill.domain.model.ActivityEvent
import com.dts.splitthebill.domain.model.Expense
import com.dts.splitthebill.domain.model.GroupDetail
import com.dts.splitthebill.domain.model.Settlement

internal data class CurrencyBalance(val currency: String, val exponent: Int, val owed: Long, val owe: Long) {
    val net: Long get() = owed - owe
}

internal data class GroupTransaction(val group: GroupDetail, val expense: Expense? = null, val settlement: Settlement? = null) {
    val date: String get() = expense?.date ?: settlement!!.date
    val id: String get() = "${group.id}/${expense?.let { "expense/${it.id}" } ?: "settlement/${settlement!!.id}"}"
    val title: String get() = expense?.title ?: "${group.person(settlement!!.fromParticipantId)} paid ${group.person(settlement.toParticipantId)}"
    fun matches(query: String): Boolean = query.isBlank() ||
        listOf(title, group.name, group.currency, date, expense?.category.orEmpty(), expense?.note.orEmpty(), settlement?.note.orEmpty())
            .any { it.contains(query.trim(), ignoreCase = true) }
}

internal data class GroupNotification(val group: GroupDetail, val event: ActivityEvent)
internal data class Reminder(val group: GroupDetail, val payment: RepaymentSuggestion)
data class ExportDocument(val fileName: String, val contents: String)

internal fun GroupDetail.person(id: String): String = participants.firstOrNull { it.id == id }?.displayName ?: "Former participant"
internal fun GroupDetail.money(amount: Long): String = "$currency ${MoneyText.formatMinor(amount, CurrencyUnit(currency, currencyExponent))}"

internal fun currencyBalances(groups: Collection<GroupDetail>): List<CurrencyBalance> =
    groups.groupBy { it.currency to it.currencyExponent }.map { (currency, records) ->
        val balances = records.map { group ->
            group.balances.firstOrNull { it.participantId == group.currentMemberParticipantId }?.amountMinor ?: 0L
        }
        CurrencyBalance(currency.first, currency.second,
            balances.filter { it > 0 }.fold(0L, Math::addExact),
            balances.filter { it < 0 }.fold(0L) { sum, value -> Math.addExact(sum, -value) })
    }.sortedBy { it.currency }

internal fun transactions(groups: Collection<GroupDetail>): List<GroupTransaction> = groups.flatMap { group ->
    group.expenses.map { GroupTransaction(group, expense = it) } + group.settlements.map { GroupTransaction(group, settlement = it) }
}.sortedWith(compareByDescending<GroupTransaction> { it.date }.thenBy { it.id })

internal fun notifications(groups: Collection<GroupDetail>, uid: String): List<GroupNotification> = groups.flatMap { group ->
    group.activity.filter { it.actorUid != uid && it.occurredAtEpochMillis > 0 }.map { GroupNotification(group, it) }
}.sortedWith(compareByDescending<GroupNotification> { it.event.occurredAtEpochMillis }.thenBy { "${it.group.id}/${it.event.id}" })

/** Per-expense counterparties are retained when simplification is off; repayments reduce that pair's debt. */
internal fun repaymentPlan(group: GroupDetail, simplified: Boolean): List<RepaymentSuggestion> {
    val pairs = mutableMapOf<Pair<String, String>, Long>()
    fun add(from: String, to: String, amount: Long) {
        val key = if (from < to) Pair(from, to) else Pair(to, from)
        val signed = if (from < to) amount else -amount
        pairs[key] = Math.addExact(pairs[key] ?: 0, signed)
    }
    group.expenses.forEach { expense ->
        val deltas = BalanceCalculator.expenseDeltas(ExpenseFinancials(
            expense.amountMinor,
            expense.contributions.map { com.dts.splitthebill.domain.Contribution(it.participantId, it.amountMinor) },
            expense.allocations.map { com.dts.splitthebill.domain.Allocation(it.participantId, it.amountMinor) },
        ))
        RepaymentSuggester.suggest(deltas.associate { it.participantId to it.amountMinor })
            .forEach { add(it.fromParticipantId, it.toParticipantId, it.amountMinor) }
    }
    group.settlements.forEach { add(it.fromParticipantId, it.toParticipantId, -it.amountMinor) }
    val plan = pairs.filterValues { it != 0L }.map { (pair, value) ->
        if (value > 0) RepaymentSuggestion(pair.first, pair.second, value)
        else RepaymentSuggestion(pair.second, pair.first, -value)
    }.sortedWith(compareBy<RepaymentSuggestion> { it.fromParticipantId }.thenBy { it.toParticipantId })
    val reconstructed = mutableMapOf<String, Long>()
    plan.forEach {
        reconstructed[it.fromParticipantId] = Math.addExact(reconstructed[it.fromParticipantId] ?: 0, -it.amountMinor)
        reconstructed[it.toParticipantId] = Math.addExact(reconstructed[it.toParticipantId] ?: 0, it.amountMinor)
    }
    val balances = group.balances.associate { it.participantId to it.amountMinor }
    check((balances.keys + reconstructed.keys).all { (reconstructed[it] ?: 0) == (balances[it] ?: 0) }) {
        "Balances are still updating. Try again shortly."
    }
    return if (simplified) RepaymentSuggester.suggest(balances) else plan
}

internal fun reminders(groups: Collection<GroupDetail>, simplified: Boolean): List<Reminder> = groups
    .filter { !it.archived && !it.fromCache && it.currentMemberParticipantId != null }
    .flatMap { group -> runCatching { repaymentPlan(group, simplified) }.getOrDefault(emptyList())
        .filter { it.toParticipantId == group.currentMemberParticipantId }.map { Reminder(group, it) } }

internal fun reminderText(reminder: Reminder): String = with(reminder) {
    "Hi ${group.person(payment.fromParticipantId)}, a friendly reminder about ${group.name}: " +
        "you owe ${group.person(payment.toParticipantId)} ${group.money(payment.amountMinor)}. " +
        "Please check Split the Bills when you have a moment. Thanks!"
}

/** RFC 4180 quoting plus spreadsheet-formula neutralization for user-provided text. */
internal fun groupCsv(group: GroupDetail): String = buildString {
    fun row(vararg cells: String) {
        append(cells.joinToString(",") { original ->
            val safe = if (original.trimStart().firstOrNull() in listOf('=', '+', '-', '@') || original.startsWith('\t') || original.startsWith('\r')) "'$original" else original
            "\"${safe.replace("\"", "\"\"")}\""
        })
        append("\r\n")
    }
    fun amount(value: Long) = MoneyText.formatMinor(value, CurrencyUnit(group.currency, group.currencyExponent))
    row("record_type", "id", "group", "date", "title", "currency", "amount", "paid_by", "shares_or_payee", "category", "note")
    group.expenses.sortedWith(compareBy<Expense> { it.date }.thenBy { it.id }).forEach { expense ->
        row("expense", expense.id, group.name, expense.date, expense.title, expense.currency, amount(expense.amountMinor),
            expense.contributions.joinToString("; ") { "${group.person(it.participantId)}: ${amount(it.amountMinor)}" },
            expense.allocations.joinToString("; ") { "${group.person(it.participantId)}: ${amount(it.amountMinor)}" }, expense.category, expense.note.orEmpty())
    }
    group.settlements.sortedWith(compareBy<Settlement> { it.date }.thenBy { it.id }).forEach { payment ->
        row("repayment", payment.id, group.name, payment.date, "Repayment", payment.currency, amount(payment.amountMinor),
            group.person(payment.fromParticipantId), group.person(payment.toParticipantId), "", payment.note.orEmpty())
    }
}
