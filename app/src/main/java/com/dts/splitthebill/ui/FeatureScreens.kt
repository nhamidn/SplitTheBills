package com.dts.splitthebill.ui

import com.dts.splitthebill.R
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dts.splitthebill.domain.model.GroupDetail
import com.dts.splitthebill.domain.model.GroupSummary
import com.dts.splitthebill.domain.model.GroupType

internal fun SplitTheBillsUiState.loadedGroups(): Collection<GroupDetail> =
    groupDetails.values.ifEmpty { listOfNotNull(selectedGroup) }

private fun SplitTheBillsUiState.loadingFeatures(): Boolean = groupsLoading ||
    groups.any { it.id !in groupDetails && it.id !in groupErrors }

@Composable
internal fun FeatureScreen(state: SplitTheBillsUiState, padding: PaddingValues, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
        key(state.screen) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth().fillMaxHeight()
                .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 24.dp)
                .testTag("feature_page_${state.screen.name.lowercase()}"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GroupLoadStatus(state, onAction)
                if (state.screen in setOf(AppScreen.GROUP_OPTIONS, AppScreen.SETTLEMENT_DETAIL) && state.selectedGroup == null) {
                    FeatureEmptyState(Icons.Outlined.Group, if (state.groupLoading) strings.get(R.string.finding_your_group) else strings.get(R.string.group_unavailable),
                        if (state.groupLoading) strings.get(R.string.your_shared_plans_will_be_here_in_a_moment) else strings.get(R.string.refresh_your_groups_to_try_again),
                        actionLabel = strings.get(R.string.retry_loading), onAction = { onAction(SplitTheBillsAction.Retry) })
                }
                when (state.screen) {
                    AppScreen.SEARCH -> SearchContent(state, onAction)
                    AppScreen.NOTIFICATIONS -> NotificationsContent(state, onAction)
                    AppScreen.REMINDERS -> RemindersContent(state, onAction)
                    AppScreen.SETTLEMENT_PICKER -> SettlePickerContent(state, onAction)
                    AppScreen.FRIENDS -> FriendsContent(state, onAction)
                    AppScreen.HELP -> HelpContent(onAction)
                    AppScreen.GROUP_OPTIONS -> GroupSettingsContent(state, onAction)
                    AppScreen.EXPENSE_GROUP_PICKER -> ExpenseGroupPickerContent(state, onAction)
                    AppScreen.SETTLEMENT_DETAIL -> SettlementDetailContent(state, onAction)
                    else -> Unit
                }
            }
        }
    }
}

@Composable
internal fun GroupLoadStatus(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    if (!state.online || state.groupErrors.isNotEmpty() || state.loadingFeatures()) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!state.online) FeatureNotice(Icons.Outlined.CloudOff, strings.get(R.string.you_re_offline),
                strings.get(R.string.saved_records_are_still_here_connect_before_making_a_shared_chang))
            if (state.groupErrors.isNotEmpty()) {
                FeatureNotice(Icons.Outlined.Info, strings.get(R.string.some_groups_need_a_refresh),
                    strings.get(R.string.totals_and_results_may_be_incomplete_try_loading_your_groups_agai), error = true)
                TextButton(onClick = { onAction(SplitTheBillsAction.Retry) }) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp)); Text(strings.get(R.string.retry_loading))
                }
            }
            if (state.loadingFeatures()) DesignCard(Modifier.fillMaxWidth()) {
                Text(strings.get(R.string.loading_your_groups), style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun SearchContent(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    var query by rememberSaveable { mutableStateOf("") }
    var scope by rememberSaveable(state.searchScope) { mutableStateOf(state.searchScope) }
    val focus = LocalFocusManager.current
    FeatureTitle(Icons.Outlined.Search, strings.get(R.string.find_what_you_need), strings.get(R.string.groups_expenses_and_repayments_all_in_one_place))
    OutlinedTextField(query, { query = it }, placeholder = { Text(strings.get(R.string.search_anything)) },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, strings.get(R.string.clear_search)) } },
        shape = RoundedCornerShape(20.dp), singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedBorderColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().testTag("global_search"))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("All", "Groups", "Expenses", "Payments").forEach { item -> FeatureFilter(strings.filter(item), scope == item) { scope = item } }
    }
    val groups = state.loadedGroups()
    val matchedGroups = if (scope in listOf("All", "Groups")) groups.filter {
        listOf(it.name, it.currency, strings.groupType(it.type)).any { value -> value.contains(query.trim(), true) }
    } else emptyList()
    val records = if (scope == "Groups") emptyList() else transactions(groups).filter {
        (scope != "Expenses" || it.expense != null) && (scope != "Payments" || it.settlement != null) && (it.matches(query) || strings.transactionTitle(it).contains(query.trim(), true) || it.expense?.let { expense -> strings.category(expense.category).contains(query.trim(), true) } == true)
    }
    if (matchedGroups.isNotEmpty()) {
        FeatureSectionHeading(strings.get(R.string.groups), matchedGroups.size)
        matchedGroups.forEach { group ->
            FeatureListCard(group.name, "${group.currency} · ${strings.groupType(group.type)}${if (group.archived) " · ${strings.get(R.string.archived)}" else ""}",
                leading = { GroupTile(group.summary(), 48.dp) }) { onAction(SplitTheBillsAction.OpenGroup(group.id)) }
        }
    }
    if (records.isNotEmpty()) {
        FeatureSectionHeading(strings.get(R.string.transactions), records.size)
        records.forEach { TransactionCard(it, onAction) }
    }
    if (matchedGroups.isEmpty() && records.isEmpty() && !state.loadingFeatures()) {
        FeatureEmptyState(Icons.Outlined.Search, strings.get(R.string.no_matches_yet), strings.get(R.string.try_a_group_name_a_note_a_date_or_a_different_filter),
            actionLabel = if (query.isNotBlank()) strings.get(R.string.clear_search) else strings.get(R.string.view_your_groups),
            onAction = { if (query.isNotBlank()) query = "" else onAction(SplitTheBillsAction.OpenGroups) })
    }
}

@Composable
internal fun TransactionCard(transaction: GroupTransaction, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    val group = transaction.group
    FeatureListCard(strings.transactionTitle(transaction), "${group.name} · ${strings.date(transaction.date)}",
        amount = strings.money(transaction.expense?.amountMinor ?: transaction.settlement!!.amountMinor, group),
        supporting = if (transaction.expense != null) strings.get(R.string.shared_expense) else strings.get(R.string.repayment_recorded),
        leading = { FeatureIcon(if (transaction.expense != null) Icons.Outlined.ReceiptLong else Icons.Outlined.Handshake) }) {
        transaction.expense?.let { onAction(SplitTheBillsAction.OpenGroupExpense(group.id, it.id)) }
            ?: onAction(SplitTheBillsAction.OpenGroupSettlement(group.id, transaction.settlement!!.id))
    }
}

@Composable
private fun NotificationsContent(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    val uid = state.user?.uid.orEmpty()
    val events = notifications(state.loadedGroups(), uid)
    val readAt = state.preferences.notificationReadAt[uid] ?: 0L
    val unread = events.count { it.event.occurredAtEpochMillis > readAt }
    FeatureTitle(Icons.Outlined.Notifications, strings.get(R.string.your_group_updates), strings.get(R.string.the_latest_from_the_people_you_split_with))
    FeatureSectionHeading(if (unread > 0) strings.plural(R.plurals.unread_count, unread) else strings.get(R.string.recent_updates), events.size) {
        TextButton(onClick = { onAction(SplitTheBillsAction.MarkNotificationsRead) }, enabled = unread > 0) {
            Icon(Icons.Outlined.DoneAll, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp)); Text(strings.get(R.string.mark_all_as_read))
        }
    }
    events.forEach { notification ->
        val event = notification.event
        val group = notification.group
        val new = event.occurredAtEpochMillis > readAt
        FeatureListCard(strings.activitySummary(event, group), group.name,
            modifier = Modifier.semantics { stateDescription = if (new) strings.get(R.string.unread) else strings.get(R.string.read) },
            supporting = strings.activityTime(event.occurredAtEpochMillis).let { if (new) strings.get(R.string.new_value, it) else it },
            leading = { FeatureIcon(when (event.resourceType) {
                "EXPENSE" -> Icons.Outlined.ReceiptLong
                "SETTLEMENT" -> Icons.Outlined.Handshake
                else -> Icons.Outlined.Group
            }, container = if (new) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                tint = if (new) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }) {
            if (event.resourceType == "EXPENSE" && group.expenses.any { it.id == event.resourceId }) {
                onAction(SplitTheBillsAction.OpenGroupExpense(group.id, event.resourceId!!))
            } else if (event.resourceType == "SETTLEMENT" && group.settlements.any { it.id == event.resourceId }) {
                onAction(SplitTheBillsAction.OpenGroupSettlement(group.id, event.resourceId!!))
            } else onAction(SplitTheBillsAction.OpenGroup(group.id))
        }
    }
    if (events.isEmpty() && !state.loadingFeatures()) FeatureEmptyState(Icons.Outlined.DoneAll, strings.get(R.string.you_re_all_caught_up),
        strings.get(R.string.updates_from_other_members_will_appear_here_so_shared_plans_stay),
        actionLabel = strings.get(R.string.view_your_groups), onAction = { onAction(SplitTheBillsAction.OpenGroups) })
}

@Composable
private fun RemindersContent(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    var selected by remember { mutableStateOf<Reminder?>(null) }
    val result = runCatching { reminders(state.loadedGroups(), state.preferences.simplifyDebts) }
    val values = result.getOrDefault(emptyList())
    FeatureHero(Icons.Outlined.NotificationsActive, strings.get(R.string.keep_it_friendly), strings.get(R.string.a_friendly_nudge),
        strings.get(R.string.follow_up_on_shared_balances_with_a_simple_thoughtful_message), modifier = Modifier.testTag("reminder_hero"),
        detail = if (values.isEmpty()) strings.get(R.string.review_first_share_when_ready)
        else strings.plural(R.plurals.balances_ready, values.size))
    val updating = result.isFailure || state.loadedGroups().any {
        !it.archived && !it.fromCache && runCatching { repaymentPlan(it, state.preferences.simplifyDebts) }.isFailure
    }
    if (updating) FeatureNotice(Icons.Outlined.Sync, strings.get(R.string.refreshing_balances),
        strings.get(R.string.reminders_appear_when_the_latest_records_are_ready_that_keeps_eve))
    FeatureSectionHeading(strings.get(R.string.ready_to_remind), values.size)
    values.forEach { reminder ->
        val group = reminder.group
        val name = strings.person(group, reminder.payment.fromParticipantId)
        FeatureListCard(name, group.name, amount = strings.money(reminder.payment.amountMinor, group), supporting = strings.get(R.string.owes_you_review_message),
            enabled = state.online, modifier = Modifier.testTag("reminder_${group.id}_${reminder.payment.fromParticipantId}"),
            leading = { Avatar(name, 48.dp) }) { selected = reminder }
    }
    if (values.isEmpty() && !state.loadingFeatures()) FeatureEmptyState(Icons.Outlined.NotificationsNone, strings.get(R.string.no_reminders_yet),
        if (!state.online) strings.get(R.string.reconnect_to_see_the_latest_amounts_owed_to_you)
        else strings.get(R.string.when_someone_owes_you_their_balance_will_appear_here_ready_for_a),
        modifier = Modifier.testTag("reminder_empty"), actionLabel = strings.get(R.string.view_your_groups),
        onAction = { onAction(SplitTheBillsAction.OpenGroups) })
    FeatureNotice(Icons.Outlined.ChatBubbleOutline, strings.get(R.string.your_words_your_choice),
        strings.get(R.string.review_your_message_then_choose_an_app_and_a_recipient_nothing_is))
    selected?.let { original ->
        // Keep the selected message tied to the current live balance.
        val current = result.getOrNull()?.firstOrNull {
            it.group.id == original.group.id && it.payment.fromParticipantId == original.payment.fromParticipantId
        }
        var message by remember(current, strings.locale) { mutableStateOf(current?.let(strings::reminder).orEmpty()) }
        val context = LocalContext.current
        AlertDialog(onDismissRequest = { selected = null }, icon = { FeatureIcon(Icons.Outlined.NotificationsActive) },
            title = { Text(strings.get(R.string.review_reminder), style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (current == null) Text(strings.get(R.string.this_balance_changed_close_this_reminder_and_check_the_latest_amo))
                    else {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(strings.person(current.group, current.payment.fromParticipantId), style = MaterialTheme.typography.labelLarge)
                                Text("${current.group.name} · ${strings.money(current.payment.amountMinor, current.group)}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        OutlinedTextField(message, { message = it }, label = { Text(strings.get(R.string.message)) }, minLines = 4,
                            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().testTag("reminder_message"))
                    }
                }
            }, confirmButton = {
                Button(enabled = current != null && message.isNotBlank() && state.online, shape = CircleShape, onClick = {
                    shareText(context, strings.get(R.string.share_reminder), message, onAction); selected = null
                }) {
                    Icon(Icons.AutoMirrored.Outlined.Send, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp)); Text(strings.get(R.string.share_reminder))
                }
            }, dismissButton = { TextButton(onClick = { selected = null }) { Text(strings.get(R.string.cancel)) } })
    }
}

@Composable
private fun SettlePickerContent(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    val choices = state.loadedGroups().filter { !it.archived }.flatMap { group ->
        runCatching { repaymentPlan(group, state.preferences.simplifyDebts) }.getOrDefault(emptyList())
            .filter { it.fromParticipantId == group.currentMemberParticipantId || it.toParticipantId == group.currentMemberParticipantId }
            .map { group to it }
    }
    FeatureHero(Icons.Outlined.Handshake, strings.get(R.string.clear_shares_calmer_plans_2), strings.get(R.string.settle_up_simply),
        strings.get(R.string.choose_a_balance_and_record_a_payment_you_ve_made), detail = strings.plural(R.plurals.balances_to_settle, choices.size))
    FeatureSectionHeading(strings.get(R.string.your_balances), choices.size)
    choices.forEach { (group, payment) ->
        FeatureListCard("${strings.person(group, payment.fromParticipantId)} → ${strings.person(group, payment.toParticipantId)}", group.name,
            amount = strings.money(payment.amountMinor, group), enabled = state.online && !group.fromCache,
            amountColor = if (payment.fromParticipantId == group.currentMemberParticipantId) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            supporting = if (payment.fromParticipantId == group.currentMemberParticipantId) strings.get(R.string.you_pay) else strings.get(R.string.you_receive),
            leading = { Avatar(strings.person(group, payment.fromParticipantId), 48.dp) }) {
            onAction(SplitTheBillsAction.PrepareSettlement(group.id, payment.fromParticipantId, payment.toParticipantId, payment.amountMinor))
        }
    }
    if (choices.isEmpty() && !state.loadingFeatures()) FeatureEmptyState(Icons.Outlined.TaskAlt, strings.get(R.string.nothing_to_settle_here),
        strings.get(R.string.repayments_due_to_or_from_you_will_appear_as_your_groups_share_ex),
        actionLabel = strings.get(R.string.view_your_groups), onAction = { onAction(SplitTheBillsAction.OpenGroups) })
    FeatureNotice(Icons.Outlined.AccountBalanceWallet, strings.get(R.string.a_record_of_your_repayment),
        strings.get(R.string.pay_outside_split_the_bills_then_record_it_here_the_app_updates_b))
}

@Composable
private fun FriendsContent(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    val people = state.loadedGroups().flatMap { group ->
        group.participants.filter { it.active && it.id != group.currentMemberParticipantId }.map { group to it }
    }.groupBy { (group, person) -> person.linkedUid ?: "${group.id}/${person.id}" }
    FeatureTitle(Icons.Outlined.PeopleOutline, strings.get(R.string.your_people), strings.get(R.string.the_friends_and_familiar_faces_you_share_plans_with))
    FeatureSectionHeading(strings.get(R.string.people_you_split_with), people.size)
    people.values.forEach { memberships ->
        DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
            val name = memberships.first().second.displayName
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(name, 48.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(strings.plural(R.plurals.shared_groups_count, memberships.size),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            memberships.forEach { (group, person) ->
                Surface(Modifier.fillMaxWidth().padding(top = 12.dp).clickable { onAction(SplitTheBillsAction.OpenGroup(group.id)) },
                    shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GroupTile(group.summary(), 34.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(group.name, style = MaterialTheme.typography.labelLarge)
                            Text(if (person.linkedUid == null) strings.get(R.string.participant_only_value, group.currency) else group.currency,
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    if (people.isEmpty() && !state.loadingFeatures()) FeatureEmptyState(Icons.Outlined.PeopleOutline, strings.get(R.string.better_together),
        strings.get(R.string.add_people_to_a_group_or_share_an_invitation_your_shared_connecti))
    FeatureSecondaryButton(strings.get(R.string.open_your_groups), icon = Icons.Outlined.Group, onClick = { onAction(SplitTheBillsAction.OpenGroups) })
}

@Composable
private fun HelpContent(onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    FeatureHero(Icons.Outlined.HelpOutline, strings.get(R.string.a_little_clarity), strings.get(R.string.here_to_help), strings.get(R.string.a_few_answers_to_keep_sharing_simple))
    val questions = listOf(
        Triple(Icons.Outlined.NotificationsActive, strings.get(R.string.how_do_reminders_work), strings.get(R.string.remind_shows_current_amounts_owed_to_you_review_the_message_and_c)),
        Triple(Icons.Outlined.Handshake, strings.get(R.string.does_settle_up_transfer_money), strings.get(R.string.no_pay_outside_the_app_then_record_the_repayment_so_everyone_sees)),
        Triple(Icons.Outlined.PersonAdd, strings.get(R.string.how_do_i_invite_someone), strings.get(R.string.open_a_group_tap_manage_people_add_a_named_participant_and_create)),
        Triple(Icons.Outlined.Payments, strings.get(R.string.can_i_use_different_currencies), strings.get(R.string.each_group_keeps_its_own_currency_home_shows_separate_totals_for)),
        Triple(Icons.Outlined.CloudOff, strings.get(R.string.what_happens_offline), strings.get(R.string.you_can_read_cached_records_and_keep_expense_drafts_on_this_devic)),
        Triple(Icons.Outlined.SwapHoriz, strings.get(R.string.what_does_simplify_debts_change), strings.get(R.string.when_enabled_repayment_suggestions_combine_group_balances_to_redu)),
        Triple(Icons.Outlined.Fingerprint, strings.get(R.string.how_does_biometric_lock_work), strings.get(R.string.when_enabled_the_app_asks_for_your_biometric_or_device_screen_loc)),
    )
    FeatureSectionHeading(strings.get(R.string.good_to_know))
    DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(4.dp)) {
        questions.forEachIndexed { index, (icon, question, answer) ->
            var open by rememberSaveable(question) { mutableStateOf(false) }
            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { open = !open }
                .semantics { stateDescription = if (open) strings.get(R.string.expanded) else strings.get(R.string.collapsed) }.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureIcon(icon, size = 36.dp)
                    Text(question, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                if (open) Text(answer, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 14.dp))
            }
        }
    }
    val context = LocalContext.current
    DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Text(strings.get(R.string.still_need_a_hand), style = MaterialTheme.typography.titleMedium)
        Text(strings.get(R.string.tell_us_what_happened_and_how_we_can_reproduce_it), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
        FeaturePrimaryButton(strings.get(R.string.report_an_issue), icon = Icons.Outlined.OpenInNew, onClick = {
            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nhamidn/SplitTheBills/issues/new"))) }
            catch (_: ActivityNotFoundException) { onAction(SplitTheBillsAction.ReportError(uiText(R.string.no_browser_is_available_to_open_support))) }
        })
        Text(strings.get(R.string.keep_passwords_invitation_codes_and_private_financial_details_out),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun GroupSettingsContent(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    val group = state.selectedGroup ?: return
    var name by rememberSaveable(group.id) { mutableStateOf(group.name) }
    var type by rememberSaveable(group.id) { mutableStateOf(group.type) }
    val revision = rememberSaveable(group.id) { group.revision }
    var confirmArchive by rememberSaveable { mutableStateOf(false) }
    val owner = group.ownerUid == state.user?.uid
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        GroupTile(group.summary(), 60.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(group.name, style = MaterialTheme.typography.headlineMedium)
            Text("${group.currency} · ${strings.plural(R.plurals.people_count, group.participants.count { it.active })}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(strings.get(R.string.the_details), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(name, { name = it }, label = { Text(strings.get(R.string.group_name)) }, enabled = owner,
                singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
            if (owner) GroupTypePicker(type) { type = it }
            else Text(strings.get(R.string.value_only_the_owner_can_change_group_settings, strings.groupType(group.type)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FeaturePrimaryButton(strings.get(R.string.save_group), enabled = owner && state.online && !state.saving && name.isNotBlank(), icon = Icons.Outlined.Check, onClick = {
                onAction(SplitTheBillsAction.SaveGroupSettings(name, type, revision))
            })
        }
    }
    if (owner) {
        FeatureSectionHeading(strings.get(R.string.people_access))
        FeatureListCard(strings.get(R.string.manage_people), strings.get(R.string.participants_invitations_and_account_links), leading = { FeatureIcon(Icons.Outlined.PersonAdd) }) {
            onAction(SplitTheBillsAction.OpenGroupMembers)
        }
    }
    if (group.archived) FeatureNotice(Icons.Outlined.Archive, strings.get(R.string.this_group_is_archived), strings.get(R.string.its_history_and_balances_stay_available_to_your_group))
    if (owner && !group.archived) DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FeatureIcon(Icons.Outlined.Archive, size = 40.dp, container = MaterialTheme.colorScheme.secondaryContainer, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(strings.get(R.string.close_this_chapter), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        Text(strings.get(R.string.archive_the_group_when_its_shared_plans_are_complete_the_history), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp, bottom = 16.dp))
        FeatureSecondaryButton(strings.get(R.string.archive_group), enabled = state.online && !state.saving, destructive = true, icon = Icons.Outlined.Archive,
            onClick = { confirmArchive = true })
    }
    if (confirmArchive) AlertDialog(onDismissRequest = { confirmArchive = false }, icon = { FeatureIcon(Icons.Outlined.Archive) },
        title = { Text(strings.get(R.string.archive_value, group.name)) },
        text = { Text(strings.get(R.string.its_history_and_balances_stay_available_but_no_new_expenses_or_re)) },
        confirmButton = { Button(onClick = { confirmArchive = false; onAction(SplitTheBillsAction.ArchiveGroup) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(strings.get(R.string.archive)) } },
        dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text(strings.get(R.string.cancel)) } })
}

@Composable
private fun ExpenseGroupPickerContent(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    FeatureTitle(Icons.Outlined.AddCircleOutline, strings.get(R.string.start_with_a_group), strings.get(R.string.keep_every_expense_with_the_right_people))
    val groups = state.groups.filterNot { it.archived }
    if (groups.isNotEmpty()) FeatureSectionHeading(strings.get(R.string.your_shared_spaces), groups.size)
    groups.forEach { group ->
        val detail = state.groupDetails[group.id]
        FeatureListCard(group.name, "${group.currency} · ${strings.groupType(group.type)}",
            supporting = detail?.let { strings.plural(R.plurals.people_count, it.participants.count { person -> person.active }) } ?: strings.get(R.string.loading_group),
            enabled = detail != null, leading = { GroupTile(group, 48.dp) }) { onAction(SplitTheBillsAction.AddExpenseToGroup(group.id)) }
    }
    if (groups.isEmpty() && !state.loadingFeatures()) FeatureEmptyState(Icons.Outlined.GroupAdd, strings.get(R.string.your_first_shared_plan),
        strings.get(R.string.create_a_group_for_a_trip_your_home_or_a_little_bit_of_everyday_l))
    FeatureSecondaryButton(strings.get(R.string.create_a_group), icon = Icons.Outlined.Add, onClick = { onAction(SplitTheBillsAction.OpenCreateGroup) })
}

@Composable
private fun SettlementDetailContent(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    val group = state.selectedGroup ?: return
    val payment = group.settlements.firstOrNull { it.id == state.selectedSettlementId }
    if (payment == null) {
        FeatureEmptyState(Icons.Outlined.Handshake, strings.get(R.string.repayment_unavailable), strings.get(R.string.it_may_have_been_removed_on_another_device),
            actionLabel = strings.get(R.string.back_to_the_group), onAction = { onAction(SplitTheBillsAction.Back) })
        return
    }
    var confirmRemove by rememberSaveable(payment.id) { mutableStateOf(false) }
    val canEdit = !group.archived && (state.user?.uid == group.ownerUid || state.user?.uid == payment.createdByUid)
    FeatureHero(Icons.Outlined.TaskAlt, strings.get(R.string.repayment_recorded_2), strings.money(payment.amountMinor, group), group.name,
        titleModifier = Modifier.testTag("settlement_detail_total"),
        detail = when { group.archived -> strings.get(R.string.archived_group); group.fromCache -> strings.get(R.string.cached_copy); else -> strings.get(R.string.saved) })
    FeatureSectionHeading(strings.get(R.string.payment_details))
    DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            PaymentPerson(strings.get(R.string.paid_by), strings.person(group, payment.fromParticipantId))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            PaymentPerson(strings.get(R.string.paid_to), strings.person(group, payment.toParticipantId))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureIcon(Icons.Outlined.CalendarMonth, size = 40.dp)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(strings.get(R.string.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(OperationDate.display(payment.date, LocalConfiguration.current.locales[0]), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
    payment.note?.takeIf { it.isNotBlank() }?.let { note ->
        DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
            Text(strings.get(R.string.note), style = MaterialTheme.typography.titleMedium)
            Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
    FeatureNotice(Icons.Outlined.VerifiedUser, strings.get(R.string.balances_brought_up_to_date), strings.get(R.string.this_records_a_payment_made_outside_the_app))
    FeaturePrimaryButton(strings.get(R.string.edit_repayment), enabled = canEdit && state.online && !state.saving, icon = Icons.Outlined.Edit,
        onClick = { onAction(SplitTheBillsAction.EditSelectedSettlement) }, modifier = Modifier.testTag("settlement_edit"))
    FeatureSecondaryButton(strings.get(R.string.remove_repayment), enabled = canEdit && state.online && !state.saving, icon = Icons.Outlined.DeleteOutline, destructive = true,
        onClick = { confirmRemove = true }, modifier = Modifier.testTag("settlement_remove"))
    if (!canEdit && !group.archived) FeatureNotice(Icons.Outlined.Lock, strings.get(R.string.shared_record), strings.get(R.string.only_the_creator_or_group_owner_can_change_this_repayment))
    if (confirmRemove) AlertDialog(onDismissRequest = { confirmRemove = false }, icon = { FeatureIcon(Icons.Outlined.DeleteOutline) },
        title = { Text(strings.get(R.string.remove_this_repayment)) },
        text = { Text(strings.get(R.string.this_reverses_the_repayment_s_effect_on_balances_no_money_is_tran)) },
        confirmButton = { Button(onClick = { confirmRemove = false; onAction(SplitTheBillsAction.DeleteSelectedSettlement) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(strings.get(R.string.confirm_removal)) } },
        dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(strings.get(R.string.cancel)) } })
}

@Composable
private fun PaymentPerson(label: String, name: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Avatar(name, 44.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(name, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
internal fun GroupTypePicker(selected: GroupType, onSelected: (GroupType) -> Unit) {
    val strings = uiStrings()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.get(R.string.group_type), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            GroupType.entries.forEach { type ->
                FeatureFilter(strings.groupType(type), selected == type, icon = when (type) {
                    GroupType.TRIP -> Icons.Outlined.Flight
                    GroupType.HOME -> Icons.Outlined.Home
                    GroupType.FRIENDS -> Icons.Outlined.Restaurant
                    GroupType.COUPLE -> Icons.Outlined.FavoriteBorder
                    GroupType.OTHER -> Icons.Outlined.Group
                }) { onSelected(type) }
            }
        }
    }
}

@Composable
private fun FeatureFilter(label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    FilterChip(selected, onClick, label = { Text(label, fontWeight = FontWeight.Bold) }, shape = CircleShape, border = null,
        modifier = Modifier.heightIn(min = 42.dp),
        leadingIcon = icon?.let { { Icon(it, null, Modifier.size(17.dp)) } },
        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface, selectedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            selectedLabelColor = MaterialTheme.colorScheme.inverseOnSurface,
            selectedLeadingIconColor = MaterialTheme.colorScheme.inverseOnSurface))
}

private fun GroupDetail.summary() = GroupSummary(id, name, currency, "", currentMemberParticipantId, revision, archived, type)

private fun shareText(context: android.content.Context, title: String, text: String, onAction: (SplitTheBillsAction) -> Unit) {
    try { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, title)) }
    catch (_: ActivityNotFoundException) { onAction(SplitTheBillsAction.ReportError(uiText(R.string.no_messaging_app_is_available_install_one_and_try_again))) }
}
