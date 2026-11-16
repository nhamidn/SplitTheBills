package com.dts.splitthebill.ui

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.dts.splitthebill.R
import com.dts.splitthebill.domain.model.ActivityEvent
import com.dts.splitthebill.domain.model.GroupDetail
import com.dts.splitthebill.domain.model.GroupType
import com.dts.splitthebill.domain.model.SplitMode
import com.dts.splitthebill.domain.repository.ThemePreference
import java.math.BigDecimal
import java.text.DateFormat
import java.text.NumberFormat
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Currency
import java.util.Locale
import java.util.TimeZone

@Composable
internal fun uiStrings(): UiStrings {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) { UiStrings(context) }
}

/** Resource access also usable in event callbacks, semantics, and shared text. */
internal class UiStrings(private val context: Context) {
    val locale: Locale = context.resources.configuration.locales[0]

    fun get(@StringRes id: Int, vararg arguments: Any): String =
        if (arguments.isEmpty()) context.getString(id) else context.getString(id, *arguments)
    fun resolve(text: UiText): String = text.resolve(context)
    fun plural(@PluralsRes id: Int, count: Int): String = context.resources.getQuantityString(id, count, count)

    fun money(amount: Long, group: GroupDetail?): String = money(amount, group?.currency ?: "USD", group?.currencyExponent ?: 2)

    fun money(amount: Long, currency: String, exponent: Int): String {
        val number = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = exponent
            maximumFractionDigits = exponent
        }.format(BigDecimal.valueOf(amount, exponent))
        return "$currency $number"
    }

    fun signedMoney(amount: Long, group: GroupDetail?): String =
        (if (amount >= 0) "+" else "−") + money(kotlin.math.abs(amount), group)

    fun currencySymbol(currency: String): String = runCatching { Currency.getInstance(currency).getSymbol(locale) }.getOrDefault(currency)

    fun person(group: GroupDetail, id: String): String =
        group.participants.firstOrNull { it.id == id }?.displayName ?: get(R.string.former_participant)

    fun groupType(type: GroupType): String = get(when (type) {
        GroupType.TRIP -> R.string.trip
        GroupType.HOME -> R.string.group_type_home
        GroupType.FRIENDS -> R.string.friends
        GroupType.COUPLE -> R.string.couple
        GroupType.OTHER -> R.string.other
    })

    fun theme(preference: ThemePreference): String = get(when (preference) {
        ThemePreference.SYSTEM -> R.string.system
        ThemePreference.LIGHT -> R.string.light
        ThemePreference.DARK -> R.string.dark
    })

    fun role(role: String): String = get(if (role == "OWNER") R.string.owner else R.string.member)

    fun category(category: String): String = get(when (category) {
        "FOOD" -> R.string.food
        "GROCERIES" -> R.string.groceries
        "TRANSPORT" -> R.string.transport
        "LODGING" -> R.string.lodging
        "ENTERTAINMENT" -> R.string.entertainment
        else -> R.string.other
    })

    fun splitMode(mode: SplitMode): String = get(when (mode) {
        SplitMode.EQUAL -> R.string.equal
        SplitMode.EXACT -> R.string.exact
        SplitMode.PERCENTAGE -> R.string.percentage
        SplitMode.WEIGHTED -> R.string.shares
    })

    /** Filter values are stable tokens; only their displayed labels are translated. */
    fun filter(value: String): String = when (value) {
        "All" -> get(R.string.all)
        "Groups" -> get(R.string.groups)
        "Expenses" -> get(R.string.expenses)
        "Payments" -> get(R.string.payments)
        "Active" -> get(R.string.active)
        "Archived" -> get(R.string.archived)
        "All time" -> get(R.string.all_time)
        "This month" -> get(R.string.this_month)
        else -> GroupType.entries.firstOrNull { it.name == value }?.let(::groupType) ?: value
    }

    fun transactionTitle(transaction: GroupTransaction): String = transaction.expense?.title ?: with(transaction) {
        val payment = requireNotNull(settlement)
        get(R.string.value_paid_value, person(group, payment.fromParticipantId), person(group, payment.toParticipantId))
    }

    /** Event types are part of the API. Server-generated English summaries are never used as UI labels. */
    fun activityTitle(event: ActivityEvent): String = get(when (event.type) {
        "GROUP_CREATED" -> R.string.activity_group_created
        "GROUP_RENAMED", "GROUP_UPDATED" -> R.string.activity_group_updated
        "GROUP_ARCHIVED" -> R.string.activity_group_archived
        "PARTICIPANT_CREATED", "PARTICIPANT_ADDED" -> R.string.activity_participant_added
        "INVITATION_CREATED" -> R.string.activity_invitation_created
        "INVITATION_REVOKED" -> R.string.activity_invitation_revoked
        "MEMBER_JOINED", "INVITATION_REDEEMED" -> R.string.activity_member_joined
        "PARTICIPANT_LINKED", "PARTICIPANT_LINK_APPROVED" -> R.string.activity_participant_linked
        "EXPENSE_CREATED" -> R.string.activity_expense_created
        "EXPENSE_UPDATED" -> R.string.activity_expense_updated
        "EXPENSE_DELETED" -> R.string.activity_expense_deleted
        "SETTLEMENT_CREATED" -> R.string.activity_settlement_created
        "SETTLEMENT_UPDATED" -> R.string.activity_settlement_updated
        "SETTLEMENT_DELETED" -> R.string.activity_settlement_deleted
        "ACCOUNT_DELETED", "MEMBER_REMOVED_FOR_ACCOUNT_DELETION" -> R.string.activity_account_deleted
        "OWNERSHIP_TRANSFERRED" -> R.string.activity_ownership_transferred
        else -> R.string.activity_generic
    })

    fun activitySummary(event: ActivityEvent, group: GroupDetail): String {
        val title = event.subject ?: if (event.resourceType == "EXPENSE") group.expenses.firstOrNull { it.id == event.resourceId }?.title else null
        return listOfNotNull(activityTitle(event), title).joinToString(" · ")
    }

    fun activityTime(epochMillis: Long): String = if (epochMillis <= 0L) get(R.string.time_unavailable) else
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(epochMillis))

    fun date(value: String): String {
        if (value.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))) return OperationDate.display(value, locale)
        for (pattern in listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")) {
            val position = ParsePosition(0)
            val date = SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }.parse(value, position)
            if (date != null && position.index == value.length) return activityTime(date.time)
        }
        return value
    }

    fun reminder(reminder: Reminder): String = with(reminder) {
        get(R.string.hi_value_a_friendly_reminder_about_value_you_owe_value_value_plea,
            person(group, payment.fromParticipantId), group.name, person(group, payment.toParticipantId), money(payment.amountMinor, group))
    }
}
