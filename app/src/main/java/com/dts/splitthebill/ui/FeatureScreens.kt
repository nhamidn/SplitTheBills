package com.dts.splitthebill.ui

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
import java.text.DateFormat
import java.util.Date

internal fun SplitTheBillsUiState.loadedGroups(): Collection<GroupDetail> =
    groupDetails.values.ifEmpty { listOfNotNull(selectedGroup) }

private fun SplitTheBillsUiState.loadingFeatures(): Boolean = groupsLoading ||
    groups.any { it.id !in groupDetails && it.id !in groupErrors }

@Composable
internal fun FeatureScreen(state: SplitTheBillsUiState, padding: PaddingValues, onAction: (SplitTheBillsAction) -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
        key(state.screen) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth().fillMaxHeight()
                .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 24.dp)
                .testTag("feature_page_${state.screen.name.lowercase()}"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GroupLoadStatus(state, onAction)
                if (state.screen in setOf(AppScreen.GROUP_OPTIONS, AppScreen.SETTLEMENT_DETAIL) && state.selectedGroup == null) {
                    FeatureEmptyState(Icons.Outlined.Group, if (state.groupLoading) "Finding your group" else "Group unavailable",
                        if (state.groupLoading) "Your shared plans will be here in a moment." else "Refresh your groups to try again.",
                        actionLabel = "Retry loading", onAction = { onAction(SplitTheBillsAction.Retry) })
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
    if (!state.online || state.groupErrors.isNotEmpty() || state.loadingFeatures()) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!state.online) FeatureNotice(Icons.Outlined.CloudOff, "You're offline",
                "Saved records are still here. Connect before making a shared change.")
            if (state.groupErrors.isNotEmpty()) {
                FeatureNotice(Icons.Outlined.Info, "Some groups need a refresh",
                    "Totals and results may be incomplete. Try loading your groups again.", error = true)
                TextButton(onClick = { onAction(SplitTheBillsAction.Retry) }) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp)); Text("Retry loading")
                }
            }
            if (state.loadingFeatures()) DesignCard(Modifier.fillMaxWidth()) {
                Text("Loading your groups…", style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun SearchContent(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var scope by rememberSaveable(state.searchScope) { mutableStateOf(state.searchScope) }
    val focus = LocalFocusManager.current
    FeatureTitle(Icons.Outlined.Search, "Find what you need", "Groups, expenses and repayments, all in one place.")
    OutlinedTextField(query, { query = it }, placeholder = { Text("Search anything") },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear search") } },
        shape = RoundedCornerShape(20.dp), singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedBorderColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().testTag("global_search"))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("All", "Groups", "Expenses", "Payments").forEach { item -> FeatureFilter(item, scope == item) { scope = item } }
    }
    val groups = state.loadedGroups()
    val matchedGroups = if (scope in listOf("All", "Groups")) groups.filter {
        listOf(it.name, it.currency, it.type.label).any { value -> value.contains(query.trim(), true) }
    } else emptyList()
    val records = if (scope == "Groups") emptyList() else transactions(groups).filter {
        (scope != "Expenses" || it.expense != null) && (scope != "Payments" || it.settlement != null) && it.matches(query)
    }
    if (matchedGroups.isNotEmpty()) {
        FeatureSectionHeading("Groups", matchedGroups.size)
        matchedGroups.forEach { group ->
            FeatureListCard(group.name, "${group.currency} · ${group.type.label}${if (group.archived) " · Archived" else ""}",
                leading = { GroupTile(group.summary(), 48.dp) }) { onAction(SplitTheBillsAction.OpenGroup(group.id)) }
        }
    }
    if (records.isNotEmpty()) {
        FeatureSectionHeading("Transactions", records.size)
        records.forEach { TransactionCard(it, onAction) }
    }
    if (matchedGroups.isEmpty() && records.isEmpty() && !state.loadingFeatures()) {
        FeatureEmptyState(Icons.Outlined.Search, "No matches yet", "Try a group name, a note, a date or a different filter.",
            actionLabel = if (query.isNotBlank()) "Clear search" else "View your groups",
            onAction = { if (query.isNotBlank()) query = "" else onAction(SplitTheBillsAction.OpenGroups) })
    }
}

@Composable
internal fun TransactionCard(transaction: GroupTransaction, onAction: (SplitTheBillsAction) -> Unit) {
    val group = transaction.group
    FeatureListCard(transaction.title, "${group.name} · ${transaction.date}",
        amount = group.money(transaction.expense?.amountMinor ?: transaction.settlement!!.amountMinor),
        supporting = if (transaction.expense != null) "Shared expense" else "Repayment recorded",
        leading = { FeatureIcon(if (transaction.expense != null) Icons.Outlined.ReceiptLong else Icons.Outlined.Handshake) }) {
        transaction.expense?.let { onAction(SplitTheBillsAction.OpenGroupExpense(group.id, it.id)) }
            ?: onAction(SplitTheBillsAction.OpenGroupSettlement(group.id, transaction.settlement!!.id))
    }
}

@Composable
private fun NotificationsContent(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    val uid = state.user?.uid.orEmpty()
    val events = notifications(state.loadedGroups(), uid)
    val readAt = state.preferences.notificationReadAt[uid] ?: 0L
    val unread = events.count { it.event.occurredAtEpochMillis > readAt }
    FeatureTitle(Icons.Outlined.Notifications, "Your group updates", "The latest from the people you split with.")
    FeatureSectionHeading(if (unread > 0) "$unread unread" else "Recent updates", events.size) {
        TextButton(onClick = { onAction(SplitTheBillsAction.MarkNotificationsRead) }, enabled = unread > 0) {
            Icon(Icons.Outlined.DoneAll, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp)); Text("Mark all as read")
        }
    }
    events.forEach { notification ->
        val event = notification.event
        val group = notification.group
        val new = event.occurredAtEpochMillis > readAt
        FeatureListCard(event.summary.replaceFirstChar(Char::uppercase), group.name,
            modifier = Modifier.semantics { stateDescription = if (new) "Unread" else "Read" },
            supporting = "${if (new) "New · " else ""}${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(event.occurredAtEpochMillis))}",
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
    if (events.isEmpty() && !state.loadingFeatures()) FeatureEmptyState(Icons.Outlined.DoneAll, "You're all caught up",
        "Updates from other members will appear here, so shared plans stay easy to follow.",
        actionLabel = "View your groups", onAction = { onAction(SplitTheBillsAction.OpenGroups) })
}

@Composable
private fun RemindersContent(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    var selected by remember { mutableStateOf<Reminder?>(null) }
    val result = runCatching { reminders(state.loadedGroups(), state.preferences.simplifyDebts) }
    val values = result.getOrDefault(emptyList())
    FeatureHero(Icons.Outlined.NotificationsActive, "KEEP IT FRIENDLY", "A friendly nudge",
        "Follow up on shared balances with a simple, thoughtful message.", modifier = Modifier.testTag("reminder_hero"),
        detail = if (values.isEmpty()) "Review first. Share when ready."
        else "${values.size} ${if (values.size == 1) "balance" else "balances"} ready to review")
    val updating = result.isFailure || state.loadedGroups().any {
        !it.archived && !it.fromCache && runCatching { repaymentPlan(it, state.preferences.simplifyDebts) }.isFailure
    }
    if (updating) FeatureNotice(Icons.Outlined.Sync, "Refreshing balances",
        "Reminders appear when the latest records are ready. That keeps every message accurate.")
    FeatureSectionHeading("Ready to remind", values.size)
    values.forEach { reminder ->
        val group = reminder.group
        val name = group.person(reminder.payment.fromParticipantId)
        FeatureListCard(name, group.name, amount = group.money(reminder.payment.amountMinor), supporting = "Owes you · Review message",
            enabled = state.online, modifier = Modifier.testTag("reminder_${group.id}_${reminder.payment.fromParticipantId}"),
            leading = { Avatar(name, 48.dp) }) { selected = reminder }
    }
    if (values.isEmpty() && !state.loadingFeatures()) FeatureEmptyState(Icons.Outlined.NotificationsNone, "No reminders yet",
        if (!state.online) "Reconnect to see the latest amounts owed to you."
        else "When someone owes you, their balance will appear here, ready for a friendly follow-up.",
        modifier = Modifier.testTag("reminder_empty"), actionLabel = "View your groups",
        onAction = { onAction(SplitTheBillsAction.OpenGroups) })
    FeatureNotice(Icons.Outlined.ChatBubbleOutline, "Your words, your choice",
        "Review your message, then choose an app and a recipient. Nothing is sent automatically.")
    selected?.let { original ->
        // Keep the selected message tied to the current live balance.
        val current = result.getOrNull()?.firstOrNull {
            it.group.id == original.group.id && it.payment.fromParticipantId == original.payment.fromParticipantId
        }
        var message by remember(current) { mutableStateOf(current?.let(::reminderText).orEmpty()) }
        val context = LocalContext.current
        AlertDialog(onDismissRequest = { selected = null }, icon = { FeatureIcon(Icons.Outlined.NotificationsActive) },
            title = { Text("Review reminder", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (current == null) Text("This balance changed. Close this reminder and check the latest amounts.")
                    else {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(current.group.person(current.payment.fromParticipantId), style = MaterialTheme.typography.labelLarge)
                                Text("${current.group.name} · ${current.group.money(current.payment.amountMinor)}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        OutlinedTextField(message, { message = it }, label = { Text("Message") }, minLines = 4,
                            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().testTag("reminder_message"))
                    }
                }
            }, confirmButton = {
                Button(enabled = current != null && message.isNotBlank() && state.online, shape = CircleShape, onClick = {
                    shareText(context, "Share reminder", message, onAction); selected = null
                }) {
                    Icon(Icons.AutoMirrored.Outlined.Send, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp)); Text("Share reminder")
                }
            }, dismissButton = { TextButton(onClick = { selected = null }) { Text("Cancel") } })
    }
}

@Composable
private fun SettlePickerContent(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    val choices = state.loadedGroups().filter { !it.archived }.flatMap { group ->
        runCatching { repaymentPlan(group, state.preferences.simplifyDebts) }.getOrDefault(emptyList())
            .filter { it.fromParticipantId == group.currentMemberParticipantId || it.toParticipantId == group.currentMemberParticipantId }
            .map { group to it }
    }
    FeatureHero(Icons.Outlined.Handshake, "CLEAR SHARES, CALMER PLANS", "Settle up, simply",
        "Choose a balance and record a payment you've made.", detail = "${choices.size} ${if (choices.size == 1) "balance" else "balances"} to settle")
    FeatureSectionHeading("Your balances", choices.size)
    choices.forEach { (group, payment) ->
        FeatureListCard("${group.person(payment.fromParticipantId)} → ${group.person(payment.toParticipantId)}", group.name,
            amount = group.money(payment.amountMinor), enabled = state.online && !group.fromCache,
            amountColor = if (payment.fromParticipantId == group.currentMemberParticipantId) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            supporting = if (payment.fromParticipantId == group.currentMemberParticipantId) "You pay" else "You receive",
            leading = { Avatar(group.person(payment.fromParticipantId), 48.dp) }) {
            onAction(SplitTheBillsAction.PrepareSettlement(group.id, payment.fromParticipantId, payment.toParticipantId, payment.amountMinor))
        }
    }
    if (choices.isEmpty() && !state.loadingFeatures()) FeatureEmptyState(Icons.Outlined.TaskAlt, "Nothing to settle here",
        "Repayments due to or from you will appear as your groups share expenses.",
        actionLabel = "View your groups", onAction = { onAction(SplitTheBillsAction.OpenGroups) })
    FeatureNotice(Icons.Outlined.AccountBalanceWallet, "A record of your repayment",
        "Pay outside Split the Bills, then record it here. The app updates balances without moving money.")
}

@Composable
private fun FriendsContent(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    val people = state.loadedGroups().flatMap { group ->
        group.participants.filter { it.active && it.id != group.currentMemberParticipantId }.map { group to it }
    }.groupBy { (group, person) -> person.linkedUid ?: "${group.id}/${person.id}" }
    FeatureTitle(Icons.Outlined.PeopleOutline, "Your people", "The friends and familiar faces you share plans with.")
    FeatureSectionHeading("People you split with", people.size)
    people.values.forEach { memberships ->
        DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
            val name = memberships.first().second.displayName
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(name, 48.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text("${memberships.size} shared ${if (memberships.size == 1) "group" else "groups"}",
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
                            Text(if (person.linkedUid == null) "Participant only · ${group.currency}" else group.currency,
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    if (people.isEmpty() && !state.loadingFeatures()) FeatureEmptyState(Icons.Outlined.PeopleOutline, "Better together",
        "Add people to a group or share an invitation. Your shared connections will appear here.")
    FeatureSecondaryButton("Open your groups", icon = Icons.Outlined.Group, onClick = { onAction(SplitTheBillsAction.OpenGroups) })
}

@Composable
private fun HelpContent(onAction: (SplitTheBillsAction) -> Unit) {
    FeatureHero(Icons.Outlined.HelpOutline, "A LITTLE CLARITY", "Here to help", "A few answers to keep sharing simple.")
    val questions = listOf(
        Triple(Icons.Outlined.NotificationsActive, "How do reminders work?", "Remind shows current amounts owed to you. Review the message and choose a messaging app. You decide whether to send it."),
        Triple(Icons.Outlined.Handshake, "Does Settle up transfer money?", "No. Pay outside the app, then record the repayment so everyone sees the updated balances."),
        Triple(Icons.Outlined.PersonAdd, "How do I invite someone?", "Open a group, tap Manage people, add a named participant, and create an invitation. Your friend joins with the code; the owner then approves their participant link."),
        Triple(Icons.Outlined.Payments, "Can I use different currencies?", "Each group keeps its own currency. Home shows separate totals for each currency; amounts are never converted or added across currencies. Default currency applies to new groups."),
        Triple(Icons.Outlined.CloudOff, "What happens offline?", "You can read cached records and keep expense drafts on this device. Shared changes and reminders need a connection. Export can save a copy of the records currently loaded."),
        Triple(Icons.Outlined.SwapHoriz, "What does Simplify debts change?", "When enabled, repayment suggestions combine group balances to reduce transfers. When disabled, suggestions keep counterparties from individual expenses. Recorded expenses and balances stay authoritative."),
        Triple(Icons.Outlined.Fingerprint, "How does biometric lock work?", "When enabled, the app asks for your biometric or device screen lock when reopened. This protects access on this device and is separate from your account password."),
    )
    FeatureSectionHeading("Good to know")
    DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(4.dp)) {
        questions.forEachIndexed { index, (icon, question, answer) ->
            var open by rememberSaveable(question) { mutableStateOf(false) }
            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { open = !open }
                .semantics { stateDescription = if (open) "Expanded" else "Collapsed" }.padding(14.dp)) {
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
        Text("Still need a hand?", style = MaterialTheme.typography.titleMedium)
        Text("Tell us what happened and how we can reproduce it.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
        FeaturePrimaryButton("Report an issue", icon = Icons.Outlined.OpenInNew, onClick = {
            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nhamidn/SplitTheBills/issues/new"))) }
            catch (_: ActivityNotFoundException) { onAction(SplitTheBillsAction.ReportError("No browser is available to open support.")) }
        })
        Text("Keep passwords, invitation codes and private financial details out of public reports.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun GroupSettingsContent(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
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
            Text("${group.currency} · ${group.participants.count { it.active }} people", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("The details", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(name, { name = it }, label = { Text("Group name") }, enabled = owner,
                singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
            if (owner) GroupTypePicker(type) { type = it }
            else Text("${group.type.label} · Only the owner can change group settings.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FeaturePrimaryButton("Save group", enabled = owner && state.online && !state.saving && name.isNotBlank(), icon = Icons.Outlined.Check, onClick = {
                onAction(SplitTheBillsAction.SaveGroupSettings(name, type, revision))
            })
        }
    }
    if (owner) {
        FeatureSectionHeading("People & access")
        FeatureListCard("Manage people", "Participants, invitations and account links", leading = { FeatureIcon(Icons.Outlined.PersonAdd) }) {
            onAction(SplitTheBillsAction.OpenGroupMembers)
        }
    }
    if (group.archived) FeatureNotice(Icons.Outlined.Archive, "This group is archived", "Its history and balances stay available to your group.")
    if (owner && !group.archived) DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FeatureIcon(Icons.Outlined.Archive, size = 40.dp, container = MaterialTheme.colorScheme.secondaryContainer, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text("Close this chapter", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        Text("Archive the group when its shared plans are complete. The history stays here.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp, bottom = 16.dp))
        FeatureSecondaryButton("Archive group", enabled = state.online && !state.saving, destructive = true, icon = Icons.Outlined.Archive,
            onClick = { confirmArchive = true })
    }
    if (confirmArchive) AlertDialog(onDismissRequest = { confirmArchive = false }, icon = { FeatureIcon(Icons.Outlined.Archive) },
        title = { Text("Archive ${group.name}?") },
        text = { Text("Its history and balances stay available, but no new expenses or repayments can be recorded. Archiving cannot currently be reversed.") },
        confirmButton = { Button(onClick = { confirmArchive = false; onAction(SplitTheBillsAction.ArchiveGroup) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Archive") } },
        dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancel") } })
}

@Composable
private fun ExpenseGroupPickerContent(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    FeatureTitle(Icons.Outlined.AddCircleOutline, "Start with a group", "Keep every expense with the right people.")
    val groups = state.groups.filterNot { it.archived }
    if (groups.isNotEmpty()) FeatureSectionHeading("Your shared spaces", groups.size)
    groups.forEach { group ->
        val detail = state.groupDetails[group.id]
        FeatureListCard(group.name, "${group.currency} · ${group.type.label}",
            supporting = detail?.let { "${it.participants.count { person -> person.active }} people" } ?: "Loading group…",
            enabled = detail != null, leading = { GroupTile(group, 48.dp) }) { onAction(SplitTheBillsAction.AddExpenseToGroup(group.id)) }
    }
    if (groups.isEmpty() && !state.loadingFeatures()) FeatureEmptyState(Icons.Outlined.GroupAdd, "Your first shared plan",
        "Create a group for a trip, your home or a little bit of everyday life. Then add your first expense.")
    FeatureSecondaryButton("Create a group", icon = Icons.Outlined.Add, onClick = { onAction(SplitTheBillsAction.OpenCreateGroup) })
}

@Composable
private fun SettlementDetailContent(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    val group = state.selectedGroup ?: return
    val payment = group.settlements.firstOrNull { it.id == state.selectedSettlementId }
    if (payment == null) {
        FeatureEmptyState(Icons.Outlined.Handshake, "Repayment unavailable", "It may have been removed on another device.",
            actionLabel = "Back to the group", onAction = { onAction(SplitTheBillsAction.Back) })
        return
    }
    var confirmRemove by rememberSaveable(payment.id) { mutableStateOf(false) }
    val canEdit = !group.archived && (state.user?.uid == group.ownerUid || state.user?.uid == payment.createdByUid)
    FeatureHero(Icons.Outlined.TaskAlt, "REPAYMENT RECORDED", group.money(payment.amountMinor), group.name,
        titleModifier = Modifier.testTag("settlement_detail_total"),
        detail = when { group.archived -> "Archived group"; group.fromCache -> "Cached copy"; else -> "Saved" })
    FeatureSectionHeading("Payment details")
    DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            PaymentPerson("Paid by", group.person(payment.fromParticipantId))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            PaymentPerson("Paid to", group.person(payment.toParticipantId))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureIcon(Icons.Outlined.CalendarMonth, size = 40.dp)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(OperationDate.display(payment.date, LocalConfiguration.current.locales[0]), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
    payment.note?.takeIf { it.isNotBlank() }?.let { note ->
        DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
            Text("Note", style = MaterialTheme.typography.titleMedium)
            Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
    FeatureNotice(Icons.Outlined.VerifiedUser, "Balances brought up to date", "This records a payment made outside the app.")
    FeaturePrimaryButton("Edit repayment", enabled = canEdit && state.online && !state.saving, icon = Icons.Outlined.Edit,
        onClick = { onAction(SplitTheBillsAction.EditSelectedSettlement) }, modifier = Modifier.testTag("settlement_edit"))
    FeatureSecondaryButton("Remove repayment", enabled = canEdit && state.online && !state.saving, icon = Icons.Outlined.DeleteOutline, destructive = true,
        onClick = { confirmRemove = true }, modifier = Modifier.testTag("settlement_remove"))
    if (!canEdit && !group.archived) FeatureNotice(Icons.Outlined.Lock, "Shared record", "Only the creator or group owner can change this repayment.")
    if (confirmRemove) AlertDialog(onDismissRequest = { confirmRemove = false }, icon = { FeatureIcon(Icons.Outlined.DeleteOutline) },
        title = { Text("Remove this repayment?") },
        text = { Text("This reverses the repayment's effect on balances. No money is transferred or refunded.") },
        confirmButton = { Button(onClick = { confirmRemove = false; onAction(SplitTheBillsAction.DeleteSelectedSettlement) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Confirm removal") } },
        dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } })
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Group type", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            GroupType.entries.forEach { type ->
                FeatureFilter(type.label, selected == type, icon = when (type) {
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
    catch (_: ActivityNotFoundException) { onAction(SplitTheBillsAction.ReportError("No messaging app is available. Install one and try again.")) }
}
