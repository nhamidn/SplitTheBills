package com.dts.splitthebill.ui

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.dts.splitthebill.R
import com.dts.splitthebill.domain.CurrencyUnit
import com.dts.splitthebill.domain.MoneyText
import com.dts.splitthebill.domain.RepaymentSuggester
import com.dts.splitthebill.domain.model.Expense
import com.dts.splitthebill.domain.model.GroupDetail
import com.dts.splitthebill.domain.model.GroupSummary
import com.dts.splitthebill.domain.model.GroupType
import com.dts.splitthebill.domain.model.Participant
import com.dts.splitthebill.domain.model.Settlement
import com.dts.splitthebill.domain.model.SplitMode
import com.dts.splitthebill.domain.repository.ThemePreference
import com.dts.splitthebill.ui.theme.LocalBalanceCardColors
import com.dts.splitthebill.ui.theme.SplitTheBillsTheme
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTheBillsApp(
    state: SplitTheBillsUiState,
    onAction: (SplitTheBillsAction) -> Unit,
    onGoogleSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    onBiometricLockChange: ((Boolean) -> Unit)? = null,
) {
    val strings = uiStrings()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(strings.resolve(it))
            onAction(SplitTheBillsAction.MessageShown)
        }
    }
    if (!state.authResolved) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (state.user == null) {
        AuthScreen(state, onAction, onGoogleSignIn, modifier)
        ErrorDialogs(state, onAction)
        return
    }

    val destination = if (state.screen == AppScreen.SETTINGS) MainDestination.PROFILE else state.mainDestination
    val nested = state.screen !in setOf(AppScreen.GROUPS, AppScreen.SETTINGS)
    BackHandler(enabled = nested) { if (!state.saving) onAction(SplitTheBillsAction.Back) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (nested) {
                Column {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(enabled = !state.saving, onClick = { onAction(SplitTheBillsAction.Back) }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = strings.get(R.string.back))
                            }
                        },
                        title = { Text(nestedTitle(state, strings), fontWeight = FontWeight.ExtraBold) },
                        actions = {
                            if (state.screen == AppScreen.GROUP_DETAIL) {
                                IconButton(enabled = !state.saving, onClick = { onAction(SplitTheBillsAction.OpenFeature(AppScreen.GROUP_OPTIONS)) }) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = strings.get(R.string.group_options))
                                }
                                if (state.selectedGroup?.ownerUid == state.user.uid) {
                                    IconButton(enabled = !state.saving, onClick = { onAction(SplitTheBillsAction.OpenGroupMembers) }) {
                                        Icon(Icons.Outlined.PersonAdd, contentDescription = strings.get(R.string.manage_people))
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    )
                    if (!state.online) OfflineBanner()
                }
            }
        },
        bottomBar = {
            if (!nested) {
                DesignBottomNavigation(destination, enabled = !state.saving) { selected ->
                    onAction(SplitTheBillsAction.Navigate(selected))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (!nested) {
            when (destination) {
                MainDestination.HOME -> HomeScreen(state, padding, onAction) {
                    onAction(SplitTheBillsAction.OpenGroups)
                }
                MainDestination.GROUPS -> GroupsScreen(state, padding, onAction)
                MainDestination.ACTIVITY -> GlobalActivityScreen(state, padding, onAction)
                MainDestination.PROFILE -> SettingsScreen(state, padding, onAction, onBiometricLockChange)
            }
        } else {
            when (state.screen) {
                AppScreen.CREATE_GROUP -> CreateGroupScreen(state, padding, onAction)
                AppScreen.GROUP_DETAIL -> GroupScreen(state, padding, onAction)
                AppScreen.GROUP_MEMBERS -> GroupMembersScreen(state, padding, onAction)
                AppScreen.INVITATION_REDEMPTION -> InvitationRedemptionScreen(state, padding, onAction)
                AppScreen.EXPENSE_EDITOR -> ExpenseEditorScreen(state, padding, onAction)
                AppScreen.EXPENSE_DETAIL -> ExpenseDetailScreen(state, padding, onAction)
                AppScreen.SETTLEMENT_EDITOR -> SettlementEditorScreen(state, padding, onAction)
                AppScreen.SEARCH, AppScreen.NOTIFICATIONS, AppScreen.REMINDERS, AppScreen.FRIENDS,
                AppScreen.HELP, AppScreen.GROUP_OPTIONS, AppScreen.SETTLEMENT_PICKER,
                AppScreen.EXPENSE_GROUP_PICKER, AppScreen.SETTLEMENT_DETAIL -> FeatureScreen(state, padding, onAction)
                else -> Unit
            }
        }
        if (state.saving) SavingOverlay()
    }
    ErrorDialogs(state, onAction)
    AccountDeletionDialog(state, onAction)
}

private fun nestedTitle(state: SplitTheBillsUiState, strings: UiStrings): String = when (state.screen) {
    AppScreen.EXPENSE_EDITOR -> if (state.expenseEditor.expenseId == null) strings.get(R.string.add_expense) else strings.get(R.string.edit_expense)
    AppScreen.EXPENSE_DETAIL -> strings.get(R.string.expense)
    AppScreen.SETTLEMENT_EDITOR -> if (state.settlementEditor.settlementId == null) strings.get(R.string.settle_up) else strings.get(R.string.edit_repayment)
    AppScreen.SETTLEMENT_DETAIL -> strings.get(R.string.repayment)
    AppScreen.EXPENSE_GROUP_PICKER -> strings.get(R.string.choose_a_group)
    AppScreen.CREATE_GROUP -> strings.get(R.string.create_group)
    AppScreen.GROUP_MEMBERS -> strings.get(R.string.group_members)
    AppScreen.INVITATION_REDEMPTION -> strings.get(R.string.join_a_group)
    AppScreen.SEARCH -> strings.get(R.string.search)
    AppScreen.NOTIFICATIONS -> strings.get(R.string.notifications)
    AppScreen.REMINDERS -> strings.get(R.string.reminders_title)
    AppScreen.FRIENDS -> strings.get(R.string.friends)
    AppScreen.HELP -> strings.get(R.string.help_support)
    AppScreen.GROUP_OPTIONS -> strings.get(R.string.group_settings)
    AppScreen.SETTLEMENT_PICKER -> strings.get(R.string.settle_up)
    else -> ""
}

@Composable
private fun DesignBottomNavigation(selected: MainDestination, enabled: Boolean, onSelected: (MainDestination) -> Unit) {
    val strings = uiStrings()
    val items: List<Triple<MainDestination, String, ImageVector>> = listOf(
        Triple(MainDestination.HOME, strings.get(R.string.home), Icons.Filled.Home),
        Triple(MainDestination.GROUPS, strings.get(R.string.groups), Icons.Filled.Group),
        Triple(MainDestination.ACTIVITY, strings.get(R.string.activity), Icons.Filled.ReceiptLong),
        Triple(MainDestination.PROFILE, strings.get(R.string.profile), Icons.Filled.Person),
    )
    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f), shadowElevation = 1.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().heightIn(min = 76.dp).padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { (destination, label, icon) ->
                val active = selected == destination
                Column(
                    modifier = Modifier.weight(1f).clickable(enabled = enabled) { onSelected(destination) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Surface(
                        modifier = Modifier.width(58.dp).height(32.dp),
                        shape = CircleShape,
                        color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = label, tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(21.dp))
                        }
                    }
                    Text(label, fontSize = 11.sp, fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold, color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun bottomNavigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun BrandTitle(state: SplitTheBillsUiState) {
    val strings = uiStrings()
    val nestedGroupName = state.selectedGroup?.name?.takeIf {
        state.screen !in setOf(AppScreen.GROUPS, AppScreen.SETTINGS)
    }
    if (nestedGroupName != null) {
        Text(nestedGroupName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(modifier = Modifier.size(36.dp), color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("S", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
            }
        }
        Column {
            Text(state.selectedGroup?.name?.takeIf { state.screen != AppScreen.GROUPS } ?: "Split the Bills", fontWeight = FontWeight.Bold)
            Text(strings.get(R.string.clear_shares_calmer_plans), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AuthScreen(
    state: SplitTheBillsUiState,
    onAction: (SplitTheBillsAction) -> Unit,
    onGoogleSignIn: () -> Unit,
    modifier: Modifier,
) {
    val strings = uiStrings()
    Box(
        modifier.fillMaxSize().imePadding().padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("S", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
                }
            }
            Text(
                if (state.authForm.mode == AuthMode.SIGN_IN) strings.get(R.string.welcome_back) else strings.get(R.string.create_your_account),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                strings.get(R.string.shared_expenses_clearly_organized),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Column(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FlowRow(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.authForm.mode == AuthMode.SIGN_IN,
                        onClick = { onAction(SplitTheBillsAction.AuthModeChanged(AuthMode.SIGN_IN)) },
                        label = { Text(strings.get(R.string.sign_in)) },
                    )
                    FilterChip(
                        selected = state.authForm.mode == AuthMode.CREATE_ACCOUNT,
                        onClick = { onAction(SplitTheBillsAction.AuthModeChanged(AuthMode.CREATE_ACCOUNT)) },
                        label = { Text(strings.get(R.string.create_account)) },
                    )
                }
                OutlinedTextField(
                    value = state.authForm.email,
                    onValueChange = { onAction(SplitTheBillsAction.AuthEmailChanged(it)) },
                    label = { Text(strings.get(R.string.email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("auth_email"),
                )
                OutlinedTextField(
                    value = state.authForm.password,
                    onValueChange = { onAction(SplitTheBillsAction.AuthPasswordChanged(it)) },
                    label = { Text(strings.get(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("auth_password"),
                )
                Button(
                    onClick = { onAction(SplitTheBillsAction.SubmitEmailAuth) },
                    enabled = !state.saving && state.online,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("auth_submit"),
                ) {
                    Text(if (state.authForm.mode == AuthMode.SIGN_IN) strings.get(R.string.sign_in) else strings.get(R.string.create_account))
                }
                OutlinedButton(
                    onClick = onGoogleSignIn,
                    enabled = !state.saving && state.online,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text(strings.get(R.string.continue_with_google)) }
                TextButton(
                    onClick = { onAction(SplitTheBillsAction.ResetPassword) },
                    enabled = !state.saving,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text(strings.get(R.string.forgot_password)) }
            }
            if (!state.online) OfflineBanner()
        }
        if (state.saving) SavingOverlay()
    }
}

@Composable
private fun HomeScreen(
    state: SplitTheBillsUiState,
    padding: PaddingValues,
    onAction: (SplitTheBillsAction) -> Unit,
    onSeeAll: () -> Unit,
) {
    val strings = uiStrings()
    val loaded = state.loadedGroups()
    val totals = currencyBalances(loaded)
    var selectedCurrency by rememberSaveable { mutableStateOf(state.preferences.defaultCurrency) }
    val total = totals.firstOrNull { it.currency == selectedCurrency } ?: totals.firstOrNull()
    val detail = loaded.firstOrNull { it.currency == total?.currency }
    val myBalance = total?.net ?: 0L
    val owed = total?.owed ?: 0L
    val owe = total?.owe ?: 0L
    val name = state.user?.displayName?.substringBefore(' ')?.takeIf(String::isNotBlank) ?: strings.get(R.string.you)
    Box(Modifier.fillMaxSize().padding(padding)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Avatar(name = state.user?.displayName ?: strings.get(R.string.you), size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(strings.get(R.string.good_morning), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                IconButton(onClick = { onAction(SplitTheBillsAction.OpenFeature(AppScreen.SEARCH)) }) { Icon(Icons.Outlined.Search, contentDescription = strings.get(R.string.search)) }
                val unread = notifications(loaded, state.user!!.uid).count { it.event.occurredAtEpochMillis > (state.preferences.notificationReadAt[state.user.uid] ?: 0L) }
                IconButton(onClick = { onAction(SplitTheBillsAction.OpenFeature(AppScreen.NOTIFICATIONS)) }) {
                    BadgedBox(badge = { if (unread > 0) Badge { Text(if (unread > 99) "99+" else unread.toString()) } }) {
                        Icon(Icons.Outlined.Notifications, contentDescription = if (unread > 0) strings.plural(R.plurals.notifications_unread, unread) else strings.get(R.string.notifications))
                    }
                }
            }
            GroupLoadStatus(state, onAction)
            BalanceHero(myBalance, owed, owe, detail,
                onRemind = { onAction(SplitTheBillsAction.OpenFeature(AppScreen.REMINDERS)) },
                onSettle = { onAction(SplitTheBillsAction.OpenFeature(AppScreen.SETTLEMENT_PICKER)) })
            if (totals.size > 1) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    totals.forEach { balance -> DesignChip(balance.currency, balance.currency == total?.currency) { selectedCurrency = balance.currency } }
                }
                Text(strings.get(R.string.currencies_are_shown_separately), modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SectionHeader(strings.get(R.string.active_groups), strings.get(R.string.see_all), onSeeAll, Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.groups.isEmpty() && !state.groupsLoading) {
                    DesignCard(Modifier.width(230.dp).clickable { onSeeAll() }) {
                        GroupTile(null, 44.dp)
                        Text(strings.get(R.string.create_your_first_group), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                        Text(strings.get(R.string.trips_homes_and_shared_plans), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
                state.groups.filterNot { it.archived }.forEach { group ->
                    DesignCard(
                        Modifier.width(178.dp).clickable { onAction(SplitTheBillsAction.OpenGroup(group.id)) },
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            GroupTile(group, 44.dp)
                            Avatar(group.name, 24.dp)
                        }
                        Text(group.name, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                        Text(if (group.archived) strings.get(R.string.status_archived) else strings.get(R.string.status_active), color = if (group.archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(group.currency, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = if (group.archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                    }
                }
            }
            SectionHeader(strings.get(R.string.recent_transactions), strings.get(R.string.view_all), { onAction(SplitTheBillsAction.OpenFeature(AppScreen.SEARCH, "All")) }, Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 10.dp))
            DesignCard(Modifier.padding(horizontal = 16.dp), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                val recent = transactions(loaded).take(4)
                if (recent.isEmpty()) {
                    Text(strings.get(R.string.your_latest_shared_expenses_will_appear_here), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(14.dp))
                } else {
                    recent.forEachIndexed { index, transaction ->
                        if (transaction.expense != null) ExpenseDesignRow(transaction.expense, transaction.group, index > 0) {
                            onAction(SplitTheBillsAction.OpenGroupExpense(transaction.group.id, transaction.expense.id))
                        } else {
                            val settlement = requireNotNull(transaction.settlement)
                            SettlementDesignRow(settlement, transaction.group, index > 0) {
                                onAction(SplitTheBillsAction.OpenGroupSettlement(transaction.group.id, settlement.id))
                            }
                        }
                    }
                }
            }
        }
        DesignFab(strings.get(R.string.add_expense), Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            onAction(SplitTheBillsAction.OpenFeature(AppScreen.EXPENSE_GROUP_PICKER))
        }
    }
}

@Composable
private fun BalanceHero(balance: Long, owed: Long, owe: Long, group: GroupDetail?, onRemind: () -> Unit, onSettle: () -> Unit) {
    val strings = uiStrings()
    val colors = LocalBalanceCardColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("balance_card"),
        shape = RoundedCornerShape(28.dp),
        color = colors.container,
        contentColor = colors.content,
    ) {
        Column(Modifier.padding(22.dp)) {
            Text(strings.get(R.string.total_balance), color = colors.supporting, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(strings.signedMoney(balance, group), modifier = Modifier.testTag("balance_total"), color = if (balance >= 0) colors.content else colors.negative, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.2).sp)
            Text(if (owed == 0L && owe == 0L) strings.get(R.string.you_re_all_settled_up) else if (balance >= 0) strings.get(R.string.overall_friends_owe_you) else strings.get(R.string.overall_you_owe_friends), color = colors.supporting, fontSize = 13.sp)
            val totalShares = owed.toDouble() + owe.toDouble()
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(8.dp)
                    .clip(CircleShape)
                    .background(colors.content.copy(alpha = 0.12f))
                    .semantics {
                        contentDescription = strings.get(R.string.you_are_owed_value_you_owe_value, strings.money(owed, group), strings.money(owe, group))
                    },
            ) {
                if (owed > 0L) {
                    Box(Modifier.weight((owed / totalShares).toFloat()).fillMaxHeight().background(colors.positive))
                }
                if (owe > 0L) {
                    Box(Modifier.weight((owe / totalShares).toFloat()).fillMaxHeight().background(colors.negative))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DarkMetric(strings.get(R.string.you_are_owed), owed, colors.positive, group, Modifier.weight(1f))
                DarkMetric(strings.get(R.string.you_owe), owe, colors.negative, group, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onSettle, modifier = Modifier.weight(1f).fillMaxHeight(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.content, contentColor = colors.container), shape = CircleShape) {
                    Icon(Icons.Outlined.Handshake, contentDescription = null, Modifier.size(19.dp)); Spacer(Modifier.width(7.dp)); Text(strings.get(R.string.settle_up))
                }
                OutlinedButton(onClick = onRemind, modifier = Modifier.weight(1f).fillMaxHeight(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), border = BorderStroke(1.dp, colors.supporting), colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.content), shape = CircleShape) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text(strings.get(R.string.remind))
                }
            }
        }
    }
}

@Composable
private fun DarkMetric(label: String, amount: Long, color: Color, group: GroupDetail?, modifier: Modifier = Modifier) {
    val strings = uiStrings()
    val colors = LocalBalanceCardColors.current
    Surface(modifier, color = colors.content.copy(alpha = 0.07f), contentColor = colors.content, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(6.dp).background(color, CircleShape))
                Text(label, color = colors.supporting, fontSize = 12.sp)
            }
            Text(strings.money(amount, group), color = color, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun GroupsScreen(state: SplitTheBillsUiState, padding: PaddingValues, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    var menu by remember { mutableStateOf(false) }
    var newestFirst by rememberSaveable { mutableStateOf(false) }
    val filters = listOf("All", "Active") + GroupType.entries.map { it.name } + "Archived"
    val groups = state.groups.filter { group ->
        when (selectedFilter) {
            "All" -> true
            "Active" -> !group.archived
            "Archived" -> group.archived
            else -> group.type.name == selectedFilter
        }
    }.let { values -> if (newestFirst) values.sortedByDescending { state.groupDetails[it.id]?.activity?.maxOfOrNull { event -> event.occurredAtEpochMillis } ?: 0L } else values.sortedBy { it.name.lowercase() } }
    Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(strings.get(R.string.groups), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { onAction(SplitTheBillsAction.OpenFeature(AppScreen.SEARCH, "Groups")) }) { Icon(Icons.Outlined.Search, contentDescription = strings.get(R.string.search_groups)) }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = strings.get(R.string.more)) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text(if (newestFirst) strings.get(R.string.sort_by_name) else strings.get(R.string.sort_by_latest_activity)) }, onClick = { newestFirst = !newestFirst; menu = false })
                        DropdownMenuItem(text = { Text(strings.get(R.string.create_a_group)) }, onClick = { menu = false; onAction(SplitTheBillsAction.OpenCreateGroup) })
                        DropdownMenuItem(text = { Text(strings.get(R.string.join_with_a_code)) }, onClick = { menu = false; onAction(SplitTheBillsAction.OpenInvitationRedemption) })
                    }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEach { filter -> DesignChip(if (filter == "All") strings.get(R.string.all_value, state.groups.size) else strings.filter(filter), selectedFilter == filter) { selectedFilter = filter } }
            }
            GroupLoadStatus(state, onAction)
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    state.groupsLoading -> LoadingBlock()
                    state.groups.isEmpty() -> EmptyCard(strings.get(R.string.nothing_split_yet), strings.get(R.string.create_a_group_for_a_trip_home_or_shared_plan))
                    groups.isEmpty() -> EmptyCard(strings.get(R.string.no_matching_groups), strings.get(R.string.choose_another_filter_or_create_a_group_of_this_type))
                    else -> groups.forEach { group -> GroupCard(group, onAction, state.groupDetails[group.id]) }
                }
                OutlinedButton(onClick = { onAction(SplitTheBillsAction.OpenCreateGroup) }, modifier = Modifier.fillMaxWidth()) { Text(strings.get(R.string.create_a_group)) }
                TextButton(onClick = { onAction(SplitTheBillsAction.OpenInvitationRedemption) }, modifier = Modifier.fillMaxWidth().testTag("open_invitation")) { Text(strings.get(R.string.have_an_invitation_join_a_group)) }
            }
        }
        DesignFab(strings.get(R.string.add_expense), Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            onAction(SplitTheBillsAction.OpenFeature(AppScreen.EXPENSE_GROUP_PICKER))
        }
    }
}

@Composable
private fun GroupCard(group: GroupSummary, onAction: (SplitTheBillsAction) -> Unit, detail: GroupDetail? = null) {
    val strings = uiStrings()
    DesignCard(Modifier.fillMaxWidth().clickable { onAction(SplitTheBillsAction.OpenGroup(group.id)) }, contentPadding = PaddingValues(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GroupTile(group, 52.dp)
            Column(Modifier.weight(1f)) {
                Text(group.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text("${group.currency} · ${strings.groupType(group.type)} · ${strings.role(group.role)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Text(if (group.archived) strings.get(R.string.status_archived) else strings.get(R.string.status_active), color = if (group.archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary, fontSize = 11.sp)
        }
        if (detail == null) Text(strings.get(R.string.loading_balance), modifier = Modifier.padding(top = 12.dp))
        else {
            val balance = detail.balances.firstOrNull { it.participantId == detail.currentMemberParticipantId }?.amountMinor ?: 0L
            Text(if (detail.currentMemberParticipantId == null) strings.get(R.string.participant_link_pending) else if (balance == 0L) strings.get(R.string.settled) else if (balance > 0) strings.get(R.string.you_are_owed_value, strings.money(balance, detail)) else strings.get(R.string.you_owe_value, strings.money(-balance, detail)),
                color = if (balance > 0) MaterialTheme.colorScheme.primary else if (balance < 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.SemiBold)
            Text("${strings.plural(R.plurals.people_count, detail.participants.count { it.active })} · ${strings.plural(R.plurals.expenses_count, detail.expenses.size)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun GlobalActivityScreen(state: SplitTheBillsUiState, padding: PaddingValues, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    var filter by rememberSaveable { mutableStateOf("All") }
    var period by rememberSaveable { mutableStateOf("All time") }
    var menu by remember { mutableStateOf(false) }
    var selectedCurrency by rememberSaveable { mutableStateOf(state.preferences.defaultCurrency) }
    val month = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(Date())
    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(Date())
    val groups = state.loadedGroups()
    val records = transactions(groups).filter {
        (filter != "Expenses" || it.expense != null) && (filter != "Payments" || it.settlement != null) &&
            (period != "This month" || (it.date.startsWith(month) && it.date <= today))
    }
    val currencies = groups.map { it.currency }.distinct().sorted()
    val currency = selectedCurrency.takeIf { it in currencies } ?: currencies.firstOrNull() ?: state.preferences.defaultCurrency
    val currencyGroup = groups.firstOrNull { it.currency == currency }
    val weekly = LongArray(5)
    transactions(groups).filter { it.expense != null && it.group.currency == currency && it.date.startsWith(month) && it.date <= today }.forEach { record ->
        val share = record.expense!!.allocations.firstOrNull { it.participantId == record.group.currentMemberParticipantId }?.amountMinor ?: 0L
        val week = ((record.date.takeLast(2).toIntOrNull() ?: 1) - 1) / 7
        weekly[week.coerceIn(0, 4)] += share
    }
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(strings.get(R.string.activity), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { onAction(SplitTheBillsAction.OpenFeature(AppScreen.SEARCH, "All")) }) { Icon(Icons.Outlined.Search, contentDescription = strings.get(R.string.search_activity)) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = strings.get(R.string.filter_activity)) }
                DropdownMenu(menu, { menu = false }) {
                    listOf("All time", "This month").forEach { value -> DropdownMenuItem(text = { Text(strings.filter(value)) }, onClick = { period = value; menu = false }) }
                }
            }
        }
        DesignCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), contentPadding = PaddingValues(18.dp)) {
            Text(strings.get(R.string.your_share_this_month), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(currencyGroup?.let { strings.money(weekly.sum(), it) } ?: "$currency 0", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                weekly.forEachIndexed { index, amount ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics { contentDescription = strings.get(R.string.week_value_value, index + 1, currencyGroup?.let { strings.money(amount, it) } ?: "$currency 0") }) {
                        Surface(Modifier.width(20.dp).height((4 + 44 * amount.toDouble() / maxOf(1L, weekly.maxOrNull() ?: 1L)).toFloat().dp), color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp)) {}
                        Text(strings.get(R.string.wvalue, index + 1), fontSize = 10.sp)
                    }
                }
            }
            if (currencies.size > 1) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                currencies.forEach { code -> DesignChip(code, code == currency) { selectedCurrency = code } }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Expenses", "Payments").forEach { item -> DesignChip(strings.filter(item), filter == item) { filter = item } }
        }
        GroupLoadStatus(state, onAction)
        Text(strings.filter(period), modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.groupsLoading) LoadingBlock()
            records.forEach { TransactionCard(it, onAction) }
            if (records.isEmpty()) EmptyCard(strings.get(R.string.quiet_for_now), strings.get(R.string.no_transactions_match_these_filters))
        }
    }
}

@Composable
internal fun DesignCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), shadowElevation = 0.5.dp) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
internal fun GroupTile(group: GroupSummary?, size: androidx.compose.ui.unit.Dp) {
    val icon = when {
        group == null -> Icons.Outlined.GroupAdd
        group.type == GroupType.TRIP -> Icons.Outlined.Flight
        group.type == GroupType.HOME -> Icons.Outlined.Home
        group.type == GroupType.FRIENDS -> Icons.Outlined.Restaurant
        else -> Icons.Outlined.FavoriteBorder
    }
    val color = when {
        group == null -> MaterialTheme.colorScheme.surfaceContainer
        group.type == GroupType.TRIP -> MaterialTheme.colorScheme.tertiaryContainer
        group.type == GroupType.HOME -> MaterialTheme.colorScheme.primaryContainer
        group.type == GroupType.FRIENDS -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    Surface(Modifier.size(size), color = color, shape = RoundedCornerShape(size / 3)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(size / 2), tint = MaterialTheme.colorScheme.contentColorFor(color)) }
    }
}

@Composable
internal fun Avatar(name: String, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val initials = name.trim().split(" ").filter(String::isNotBlank).take(2).joinToString("") { it.take(1).uppercase() }.ifBlank { "?" }
    val initialsSize = with(LocalDensity.current) { (size * 0.34f).toSp() }
    Surface(modifier.size(size), color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
        Box(contentAlignment = Alignment.Center) {
            Text(initials, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = initialsSize, lineHeight = initialsSize, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DesignChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.heightIn(min = 36.dp).clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface,
        shape = CircleShape,
        shadowElevation = if (selected) 0.dp else 0.5.dp,
    ) { Box(Modifier.padding(horizontal = 15.dp), contentAlignment = Alignment.Center) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
}

@Composable
private fun DesignFab(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 18.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp,
            focusedElevation = 1.dp,
            hoveredElevation = 1.dp,
            disabledElevation = 0.dp,
        ),
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(20.dp)); Spacer(Modifier.width(7.dp)); Text(label, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun SectionHeader(title: String, action: String?, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        action?.let { TextButton(onClick = onAction) { Text(it, fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
    }
}

@Composable
private fun ExpenseDesignRow(expense: Expense, group: GroupDetail, divider: Boolean, onClick: () -> Unit) {
    val strings = uiStrings()
    val myId = group.currentMemberParticipantId
    val paid = expense.contributions.any { it.participantId == myId }
    TransactionDesignRow(
        icon = categoryIcon(expense.category),
        title = expense.title,
        subtitle = strings.get(R.string.value_paid_value_value, strings.person(group, expense.contributions.firstOrNull()?.participantId.orEmpty()), strings.money(expense.amountMinor, group), strings.date(expense.date)),
        amountLabel = if (paid) strings.get(R.string.you_lent) else strings.get(R.string.you_owe_short),
        amount = strings.money(expense.allocations.firstOrNull { it.participantId == myId }?.amountMinor ?: 0L, group),
        amountColor = if (paid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        divider = divider,
        onClick = onClick,
    ) { ExpenseParticipants(expense, group, Modifier.padding(top = 8.dp)) }
}

@Composable
private fun SettlementDesignRow(settlement: Settlement, group: GroupDetail, divider: Boolean, onClick: () -> Unit) {
    val strings = uiStrings()
    val myId = group.currentMemberParticipantId
    val received = myId == settlement.toParticipantId
    val sent = myId == settlement.fromParticipantId
    TransactionDesignRow(
        icon = Icons.Outlined.Handshake,
        title = strings.get(R.string.value_paid_value, strings.person(group, settlement.fromParticipantId), strings.person(group, settlement.toParticipantId)),
        subtitle = "${group.name} · ${strings.date(settlement.date)}",
        amountLabel = when { received -> strings.get(R.string.you_received); sent -> strings.get(R.string.you_paid); else -> strings.get(R.string.repayment_short) },
        amount = strings.money(settlement.amountMinor, group),
        amountColor = when { received -> MaterialTheme.colorScheme.primary; sent -> MaterialTheme.colorScheme.secondary; else -> MaterialTheme.colorScheme.onSurfaceVariant },
        divider = divider,
        onClick = onClick,
    ) {
        ParticipantSummary(listOf(settlement.fromParticipantId, settlement.toParticipantId).distinct().map { strings.person(group, it) },
            Modifier.padding(top = 8.dp).testTag("settlement_participants_${settlement.id}"))
    }
}

@Composable
private fun TransactionDesignRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    amountLabel: String,
    amount: String,
    amountColor: Color,
    divider: Boolean,
    onClick: () -> Unit,
    participants: @Composable () -> Unit,
) {
    if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(Modifier.size(44.dp), color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(14.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, modifier = Modifier.size(21.dp)) }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(amountLabel, color = amountColor, fontSize = 10.sp)
                    Text(amount, color = amountColor, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }
            participants()
        }
    }
}

@Composable
internal fun ExpenseParticipants(expense: Expense, group: GroupDetail, modifier: Modifier = Modifier) {
    val strings = uiStrings()
    val participantIds = (expense.contributions.map { it.participantId } +
        expense.allocations.map { it.participantId }).distinct()
    ParticipantSummary(participantIds.map { strings.person(group, it) }, modifier.testTag("expense_participants_${expense.id}"))
}

@Composable
private fun ParticipantSummary(names: List<String>, modifier: Modifier = Modifier) {
    val strings = uiStrings()
    if (names.isEmpty()) return
    val shown = names.take(4)
    val remaining = names.size - shown.size
    val circles = shown.size + if (remaining > 0) 1 else 0
    Row(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = strings.get(R.string.participants_value, names.joinToString())
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.width((26 + (circles - 1) * 18).dp).height(26.dp)) {
            shown.forEachIndexed { index, name ->
                Avatar(
                    name,
                    26.dp,
                    Modifier.offset(x = (index * 18).dp)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                )
            }
            if (remaining > 0) {
                val countSize = with(LocalDensity.current) { 9.dp.toSp() }
                Surface(
                    modifier = Modifier.offset(x = (shown.size * 18).dp).size(26.dp)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("+$remaining", fontSize = countSize, lineHeight = countSize, maxLines = 1, fontWeight = FontWeight.Bold) }
                }
            }
        }
        Text(
            strings.plural(R.plurals.people_count, names.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun categoryIcon(category: String): ImageVector = when (category.uppercase()) {
    "FOOD" -> Icons.Outlined.Restaurant
    "GROCERIES" -> Icons.Outlined.LocalGroceryStore
    "TRANSPORT" -> Icons.Outlined.LocalTaxi
    else -> Icons.Outlined.ReceiptLong
}


@Composable
private fun InvitationRedemptionScreen(
    state: SplitTheBillsUiState,
    padding: PaddingValues,
    onAction: (SplitTheBillsAction) -> Unit,
) {
    val strings = uiStrings()
    val form = state.invitationRedemption
    FormPage(
        padding,
        strings.get(R.string.join_a_shared_space),
        strings.get(R.string.invitation_access_and_participant_identity_are_separate_the_owner),
    ) {
        if (form.fromLink) {
            EmptyCard(strings.get(R.string.invitation_link_ready), strings.get(R.string.sign_in_and_confirm_below_the_raw_token_stays_hidden))
        } else {
            OutlinedTextField(
                value = form.value,
                onValueChange = { onAction(SplitTheBillsAction.InvitationValueChanged(it)) },
                label = { Text(strings.get(R.string.invitation_code)) },
                supportingText = { Text(strings.get(R.string.example_abcde_23456)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("invitation_code"),
            )
        }
        Button(
            onClick = { onAction(SplitTheBillsAction.SubmitInvitation) },
            enabled = state.online && !state.saving && form.value.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("invitation_submit"),
        ) { Text(if (state.saving) strings.get(R.string.checking_with_server) else strings.get(R.string.accept_invitation)) }
    }
}

@Composable
private fun CreateGroupScreen(state: SplitTheBillsUiState, padding: PaddingValues, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    FormPage(padding, strings.get(R.string.create_a_shared_space), strings.get(R.string.one_currency_keeps_every_balance_understandable)) {
        FormSection(strings.get(R.string.group_details), strings.get(R.string.you_can_add_more_people_after_creating_the_group)) {
            OutlinedTextField(
                state.createGroupForm.name,
                { onAction(SplitTheBillsAction.CreateGroupNameChanged(it)) },
                label = { Text(strings.get(R.string.group_name)) },
                placeholder = { Text(strings.get(R.string.weekend_away)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("group_name"),
            )
            GroupTypePicker(state.createGroupForm.type) { onAction(SplitTheBillsAction.CreateGroupTypeChanged(it)) }
            CurrencyPickerField(
                currencyCode = state.createGroupForm.currency,
                onCurrencySelected = { onAction(SplitTheBillsAction.CreateGroupCurrencyChanged(it)) },
            )
            Text(
                strings.get(R.string.this_currency_applies_to_every_expense_and_repayment_in_the_group),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                state.createGroupForm.participantName,
                { onAction(SplitTheBillsAction.CreateGroupParticipantChanged(it)) },
                label = { Text(strings.get(R.string.your_participant_name)) },
                supportingText = { Text(strings.get(R.string.names_never_determine_account_identity)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("participant_name"),
            )
        }
        Button(
            onClick = { onAction(SplitTheBillsAction.SubmitGroup) },
            enabled = !state.saving && state.online,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("group_submit"),
        ) {
            Text(if (state.saving) strings.get(R.string.creating) else strings.get(R.string.create_group))
        }
    }
}

@Composable
private fun GroupScreen(state: SplitTheBillsUiState, padding: PaddingValues, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    val group = state.selectedGroup
    if (state.groupLoading || group == null) {
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.groupLoading) CircularProgressIndicator()
                else Text(strings.get(R.string.this_group_is_unavailable))
                TextButton(onClick = { onAction(SplitTheBillsAction.Retry) }) { Text(strings.get(R.string.retry_loading)) }
            }
        }
        return
    }
    val totalSpend = group.expenses.sumOf(Expense::amountMinor)
    val myBalance = group.balances.firstOrNull { it.participantId == group.currentMemberParticipantId }?.amountMinor ?: 0L
    Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp).padding(bottom = 94.dp)) {
            if (group.fromCache) Text(strings.get(R.string.cached_copy_balances_will_refresh_when_connected), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GroupTile(GroupSummary(group.id, group.name, group.currency, "", group.currentMemberParticipantId, group.revision, group.archived, group.type), 64.dp)
                Column(Modifier.weight(1f)) {
                    Text(group.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                    Text("${group.currency} · ${strings.plural(R.plurals.members_count, group.participants.count(Participant::active))}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            DesignCard(Modifier.fillMaxWidth().padding(top = 14.dp), contentPadding = PaddingValues(18.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) { Text(strings.get(R.string.group_spend), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); Text(strings.money(totalSpend, group), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold) }
                    Surface(Modifier.width(1.dp).heightIn(min = 48.dp), color = MaterialTheme.colorScheme.outlineVariant) {}
                    Column(Modifier.weight(1f).padding(start = 14.dp)) { Text(strings.get(R.string.your_balance), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); Text(strings.signedMoney(myBalance, group), color = if (myBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold) }
                }
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAction(SplitTheBillsAction.SelectGroupTab(GroupTab.BALANCES)) }, modifier = Modifier.weight(1f).heightIn(min = 44.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface), shape = CircleShape) { Icon(Icons.Outlined.Handshake, null, Modifier.size(19.dp)); Spacer(Modifier.width(6.dp)); Text(strings.get(R.string.settle_up)) }
                    Button(onClick = { onAction(SplitTheBillsAction.ExportGroup) }, modifier = Modifier.weight(1f).heightIn(min = 44.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface), shape = CircleShape) { Text(strings.get(R.string.export)) }
                }
            }
            DesignSegmented(
                selected = state.selectedTab,
                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                onSelected = { onAction(SplitTheBillsAction.SelectGroupTab(it)) },
            )
            when (state.selectedTab) {
                GroupTab.EXPENSES -> ExpenseList(group, onAction)
                GroupTab.BALANCES -> BalanceList(group, onAction, state.preferences.simplifyDebts, state.online)
                GroupTab.MEMBERS -> GroupMembersSummary(group)
                GroupTab.ACTIVITY -> ActivityList(group)
            }
        }
        if (state.selectedTab == GroupTab.EXPENSES && !group.archived) {
            DesignFab(strings.get(R.string.add_expense), Modifier.align(Alignment.BottomEnd).padding(16.dp)) { onAction(SplitTheBillsAction.OpenNewExpense) }
        }
    }
}

@Composable
private fun DesignSegmented(selected: GroupTab, modifier: Modifier = Modifier, onSelected: (GroupTab) -> Unit) {
    val strings = uiStrings()
    val tabs = listOf(GroupTab.EXPENSES to strings.get(R.string.expenses), GroupTab.BALANCES to strings.get(R.string.balances), GroupTab.MEMBERS to strings.get(R.string.members), GroupTab.ACTIVITY to strings.get(R.string.history))
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainer, shape = CircleShape) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(4.dp)) {
            tabs.forEach { (tab, label) ->
                Surface(
                    modifier = Modifier.widthIn(min = 88.dp).heightIn(min = 40.dp).clickable { onSelected(tab) },
                    color = if (selected == tab) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shape = CircleShape,
                    shadowElevation = if (selected == tab) 0.5.dp else 0.dp,
                ) { Box(contentAlignment = Alignment.Center) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected == tab) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
        }
    }
}

@Composable
private fun GroupMembersSummary(group: GroupDetail) {
    val strings = uiStrings()
    DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
        group.participants.filter(Participant::active).forEachIndexed { index, participant ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            val balance = group.balances.firstOrNull { it.participantId == participant.id }?.amountMinor ?: 0L
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(participant.displayName, 38.dp)
                Column(Modifier.weight(1f)) { Text(if (participant.id == group.currentMemberParticipantId) strings.get(R.string.you) else participant.displayName, fontWeight = FontWeight.Bold); Text(if (participant.linkedUid == null) strings.get(R.string.invitation_pending) else strings.get(R.string.group_member), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
                Column(horizontalAlignment = Alignment.End) { Text(strings.signedMoney(balance, group), color = if (balance > 0) MaterialTheme.colorScheme.primary else if (balance < 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.ExtraBold); Text(if (balance > 0) strings.get(R.string.gets_back) else if (balance < 0) strings.get(R.string.owes) else strings.get(R.string.even), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) }
            }
        }
    }
}

@Composable
private fun ParticipantAdder(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    // Outlined fields reserve half a label line above the visible border.
    val labelInset = with(LocalDensity.current) {
        MaterialTheme.typography.bodySmall.lineHeight.toDp() / 2
    }
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = state.participantName,
            onValueChange = { onAction(SplitTheBillsAction.ParticipantNameChanged(it)) },
            label = { Text(strings.get(R.string.add_named_participant)) },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.weight(1f).testTag("group_participant_name"),
        )
        OutlinedButton(
            onClick = { onAction(SplitTheBillsAction.AddParticipant) },
            enabled = state.online && !state.saving,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .padding(top = labelInset)
                .fillMaxHeight()
                .widthIn(min = 80.dp)
                .testTag("group_participant_submit"),
        ) { Text(strings.get(R.string.add)) }
    }
}

@Composable
private fun GroupMembersScreen(
    state: SplitTheBillsUiState,
    padding: PaddingValues,
    onAction: (SplitTheBillsAction) -> Unit,
) {
    val strings = uiStrings()
    val group = state.selectedGroup ?: return
    val form = state.groupMembers
    val context = LocalContext.current
    val unlinkedMembers = group.members.filter { it.participantId == null }
    val unlinkedParticipants = group.participants.filter { it.active && it.linkedUid == null }
    val activeParticipantCount = group.participants.count(Participant::active)
    val availableAccountPlaces = minOf(
        unlinkedParticipants.size,
        (activeParticipantCount - group.members.size).coerceAtLeast(0),
    )
    FormPage(
        padding,
        strings.get(R.string.people_access),
        strings.get(R.string.manage_participants_invitations_and_account_links_for_this_group),
    ) {
        FormSection(strings.get(R.string.participants), strings.get(R.string.people_included_in_expenses_and_balances)) {
            ParticipantAdder(state, onAction)
            group.participants.forEachIndexed { index, participant ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(Modifier.size(36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(participant.displayName.take(1).uppercase(), fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(participant.displayName, fontWeight = FontWeight.SemiBold)
                        Text(
                            participant.linkedUid?.let { strings.get(R.string.account_linked_value, shortUid(it)) } ?: strings.get(R.string.participant_only),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        FormSection(
            strings.get(R.string.create_an_invitation),
            if (availableAccountPlaces > 0) {
                strings.get(R.string.value_of_value_account_places_available_share_the_link_or_its_fal, availableAccountPlaces, activeParticipantCount)
            } else {
                strings.get(R.string.the_group_is_full_add_a_participant_before_inviting_another_accou)
            },
        ) {
            Text(strings.get(R.string.expires_after), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3600L to strings.get(R.string.text_1_hour), 86400L to strings.get(R.string.text_1_day), 604800L to strings.get(R.string.text_7_days), 2592000L to strings.get(R.string.text_30_days))
                    .forEach { (seconds, label) ->
                        FilterChip(
                            selected = form.expiresInSeconds == seconds,
                            onClick = { onAction(SplitTheBillsAction.InvitationExpiryChanged(seconds)) },
                            label = { Text(label) },
                        )
                    }
            }
            OutlinedTextField(
                value = form.maxUses,
                onValueChange = { onAction(SplitTheBillsAction.InvitationMaxUsesChanged(it.filter(Char::isDigit))) },
                label = { Text(strings.get(R.string.maximum_uses)) },
                supportingText = { Text(strings.get(R.string.invitation_use_limit_1_20_group_capacity_still_applies)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onAction(SplitTheBillsAction.CreateInvitation) },
                enabled = state.online && !state.saving && form.invitation == null && availableAccountPlaces > 0,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("create_invitation"),
            ) { Text(strings.get(R.string.create_invitation)) }
        }

        form.invitation?.let { invitation ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.get(R.string.invitation_ready), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (invitation.secretAlreadyIssued || invitation.token.isBlank()) {
                        Text(strings.get(R.string.this_secret_was_already_displayed_revoke_it_and_create_another_in))
                    } else {
                        Text(
                            invitation.code,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(strings.get(R.string.code_fallback), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        val link = invitationLink(invitation.token)
                        Button(
                            onClick = {
                                val share = Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(
                                        Intent.EXTRA_TEXT,
                                        strings.get(R.string.join_value_in_split_the_bills_nvalue_ncode_value, group.name, link, invitation.code),
                                    )
                                context.startActivity(Intent.createChooser(share, strings.get(R.string.share_invitation)))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(strings.get(R.string.share_invitation)) }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(strings.get(R.string.expires_value, strings.date(invitation.expiresAt)), style = MaterialTheme.typography.bodySmall)
                        TextButton(
                            onClick = { onAction(SplitTheBillsAction.RevokeInvitation(invitation.id, invitation.revision)) },
                            enabled = state.online && !state.saving,
                        ) { Text(strings.get(R.string.revoke)) }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        FormSection(strings.get(R.string.issued_invitations), strings.get(R.string.only_metadata_remains_after_an_invitation_secret_is_shown)) {
            when {
                form.invitationsLoading -> CircularProgressIndicator(Modifier.size(28.dp))
                form.invitations.isEmpty() -> Text(strings.get(R.string.no_invitations_created_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> form.invitations.forEachIndexed { index, invitation ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val revocable = invitation.revokedAt == null && invitation.useCount < invitation.maxUses
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                when {
                                    invitation.revokedAt != null -> strings.get(R.string.revoked_invitation)
                                    invitation.useCount >= invitation.maxUses -> strings.get(R.string.usage_limit_reached)
                                    else -> strings.get(R.string.active_invitation)
                                },
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                strings.get(R.string.value_value_uses_expires_value, invitation.useCount, invitation.maxUses, strings.date(invitation.expiresAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (revocable) {
                            TextButton(
                                onClick = { onAction(SplitTheBillsAction.RevokeInvitation(invitation.id, invitation.revision)) },
                                enabled = state.online && !state.saving,
                            ) { Text(strings.get(R.string.revoke)) }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        FormSection(strings.get(R.string.link_an_account), strings.get(R.string.after_someone_joins_confirm_which_participant_represents_them)) {
            when {
                unlinkedMembers.isEmpty() -> Text(strings.get(R.string.no_member_is_waiting_to_be_linked), color = MaterialTheme.colorScheme.onSurfaceVariant)
                unlinkedParticipants.isEmpty() -> Text(strings.get(R.string.no_unlinked_participant_is_available), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> {
                    Text(strings.get(R.string.member_account), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        unlinkedMembers.forEach { member ->
                            FilterChip(
                                selected = form.selectedMemberUid == member.uid,
                                onClick = { onAction(SplitTheBillsAction.LinkMemberChanged(member.uid)) },
                                label = { Text(shortUid(member.uid)) },
                            )
                        }
                    }
                    Text(strings.get(R.string.participant), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        unlinkedParticipants.forEach { participant ->
                            FilterChip(
                                selected = form.selectedParticipantId == participant.id,
                                onClick = { onAction(SplitTheBillsAction.LinkParticipantChanged(participant.id)) },
                                label = { Text(participant.displayName) },
                            )
                        }
                    }
                    Text(
                        strings.get(R.string.confirm_the_match_with_the_invited_person_names_are_never_used_to),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { onAction(SplitTheBillsAction.ApproveParticipantLink) },
                        enabled = state.online && !state.saving && form.selectedMemberUid.isNotBlank() &&
                            form.selectedParticipantId.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(strings.get(R.string.approve_account_link)) }
                }
            }
        }
    }
}

@Composable
private fun ExpenseList(group: GroupDetail, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    if (group.expenses.isEmpty() && group.settlements.isEmpty()) {
        EmptyCard(strings.get(R.string.no_activity_yet), strings.get(R.string.add_an_expense_when_everyone_can_see_who_paid_and_how_it_is_split))
    }
    if (group.expenses.isNotEmpty()) {
        DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
            group.expenses.sortedByDescending { it.date }.forEachIndexed { index, expense ->
                ExpenseDesignRow(expense, group, index > 0) { onAction(SplitTheBillsAction.OpenExpense(expense.id)) }
            }
        }
    }
}

@Composable
private fun BalanceList(group: GroupDetail, onAction: (SplitTheBillsAction) -> Unit, simplified: Boolean, online: Boolean) {
    val strings = uiStrings()
    val suggestions = remember(group, simplified) {
        runCatching { repaymentPlan(group, simplified) }
    }

    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("✦", color = MaterialTheme.colorScheme.primary, fontSize = 22.sp)
            Text(if (simplified) strings.get(R.string.balances_are_combined_to_reduce_transfers) else strings.get(R.string.repayments_keep_the_counterparties_from_your_expenses), color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    Spacer(Modifier.height(12.dp))
    when {
        suggestions.isFailure -> Text(
            strings.get(R.string.suggestions_are_unavailable_because_the_current_balances_are_inco),
            color = MaterialTheme.colorScheme.error,
        )
        suggestions.getOrThrow().isEmpty() -> EmptyCard(strings.get(R.string.everyone_is_settled), strings.get(R.string.no_repayment_is_currently_needed))
        else -> DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
            Text(strings.get(R.string.who_owes_whom), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp))
            suggestions.getOrThrow().forEachIndexed { index, suggestion ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Avatar(strings.person(group, suggestion.fromParticipantId), 36.dp)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(strings.money(suggestion.amountMinor, group), color = if (suggestion.toParticipantId == group.currentMemberParticipantId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        Text("- - - - - - →", color = if (suggestion.toParticipantId == group.currentMemberParticipantId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Avatar(strings.person(group, suggestion.toParticipantId), 36.dp)
                    Surface(modifier = Modifier.clickable(enabled = online && !group.fromCache && !group.archived) {
                        onAction(SplitTheBillsAction.PrepareSettlement(group.id, suggestion.fromParticipantId, suggestion.toParticipantId, suggestion.amountMinor))
                    }, color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) { Text(if (suggestion.toParticipantId == group.currentMemberParticipantId) strings.get(R.string.record) else strings.get(R.string.settle), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)) }
                }
            }
        }
    }
    if (group.settlements.isNotEmpty()) {
        SectionHeader(strings.get(R.string.payment_history), null, {}, Modifier.padding(top = 20.dp, bottom = 8.dp))
        DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
            group.settlements.forEachIndexed { index, settlement ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Row(Modifier.fillMaxWidth().clickable {
                    onAction(SplitTheBillsAction.OpenGroupSettlement(group.id, settlement.id))
                }.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Avatar(strings.person(group, settlement.fromParticipantId), 38.dp)
                    Column(Modifier.weight(1f)) { Text(strings.get(R.string.value_paid_value, strings.person(group, settlement.fromParticipantId), strings.person(group, settlement.toParticipantId)), fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(strings.date(settlement.date), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }
                    Text(strings.money(settlement.amountMinor, group), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
    OutlinedButton(onClick = { onAction(SplitTheBillsAction.OpenSettlement) },
        enabled = online && !group.fromCache && !group.archived && group.participants.count { it.active } >= 2,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text(strings.get(R.string.record_a_repayment)) }
}

@Composable
private fun ActivityList(group: GroupDetail) {
    val strings = uiStrings()
    if (group.activity.isEmpty()) EmptyCard(strings.get(R.string.quiet_for_now), strings.get(R.string.server_confirmed_changes_will_appear_here))
    group.activity.forEach { event ->
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(modifier = Modifier.padding(top = 5.dp).size(10.dp), color = MaterialTheme.colorScheme.primary, shape = CircleShape) {}
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(strings.activityTitle(event), fontWeight = FontWeight.SemiBold)
                Text(strings.activitySummary(event, group), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    buildString {
                        append(strings.activityTime(event.occurredAtEpochMillis))
                        event.actorUid?.let { append(strings.get(R.string.account_value, shortUid(it))) }
                        if (event.resourceRevision > 0) append(strings.get(R.string.revision_value, event.resourceRevision))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ExpenseEditorScreen(state: SplitTheBillsUiState, padding: PaddingValues, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    val density = LocalDensity.current
    var footerHeight by remember { mutableStateOf(88.dp) }
    val group = state.selectedGroup ?: return
    val editor = state.expenseEditor
    val selectedCount = editor.selectedParticipantIds.size.coerceAtLeast(1)
    val amountMinor = runCatching {
        MoneyText.parseUnsignedMinor(canonicalDecimal(editor.amount), CurrencyUnit(group.currency, group.currencyExponent))
    }.getOrDefault(0L)
    val preview = remember(editor, group) { runCatching { splitAllocations(editor, group) } }
    val allocations = preview.getOrNull()?.associate { it.participantId to it.amountMinor }.orEmpty()
    Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
        Column(Modifier.fillMaxSize().padding(bottom = footerHeight).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Surface(Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.surface, shape = CircleShape, shadowElevation = 0.5.dp) {
                Row(Modifier.padding(horizontal = 7.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GroupTile(GroupSummary(group.id, group.name, group.currency, "", group.currentMemberParticipantId, group.revision, group.archived, group.type), 30.dp)
                    Text(group.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("⌄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(
                value = editor.amount,
                onValueChange = { onAction(SplitTheBillsAction.ExpenseAmountChanged(it)) },
                prefix = { Text(strings.currencySymbol(group.currency), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 30.sp, fontWeight = FontWeight.Bold) },
                textStyle = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp).testTag("expense_amount"),
            )
            OutlinedTextField(
                value = editor.title,
                onValueChange = { onAction(SplitTheBillsAction.ExpenseTitleChanged(it)) },
                placeholder = { Text(strings.get(R.string.what_was_it_for)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("expense_title"),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("FOOD", "GROCERIES", "TRANSPORT", "LODGING", "ENTERTAINMENT", "OTHER").forEach { category ->
                    val selected = editor.category == category
                    Surface(
                        modifier = Modifier.widthIn(min = 76.dp).heightIn(min = 72.dp).clickable { onAction(SplitTheBillsAction.ExpenseCategoryChanged(category)) },
                        color = if (selected) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface,
                        contentColor = if (selected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = if (selected) 0.dp else 0.5.dp,
                    ) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(categoryIcon(category), null, Modifier.size(22.dp)); Text(strings.category(category), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
                OperationDateField(
                    date = editor.date,
                    onDateSelected = { onAction(SplitTheBillsAction.ExpenseDateChanged(it)) },
                    modifier = Modifier.testTag("expense_date"),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Text(strings.get(R.string.paid_by), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(start = 12.dp, top = 10.dp))
                ParticipantChips(group.participants, editor.payerParticipantId) { onAction(SplitTheBillsAction.ExpensePayerChanged(it)) }
            }
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 18.dp).testTag("expense_split_section"),
                horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(strings.get(R.string.split), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(strings.get(R.string.value_of_value_people, editor.selectedParticipantIds.size, group.participants.count(Participant::active)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            ExpenseSplitSegmented(editor.splitMode) { onAction(SplitTheBillsAction.ExpenseModeChanged(it)) }
            Surface(Modifier.fillMaxWidth().height(9.dp).padding(horizontal = 2.dp, vertical = 1.dp), color = MaterialTheme.colorScheme.surfaceContainer, shape = CircleShape) {
                Row { repeat(selectedCount) { index -> Surface(Modifier.weight(1f).fillMaxHeight(), color = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.outline)[index % 4]) {} } }
            }
            Text(when {
                preview.isSuccess -> strings.get(R.string.shares_total_value, strings.money(allocations.values.sum(), group))
                editor.splitMode == SplitMode.PERCENTAGE -> strings.get(R.string.enter_percentages_that_add_up_to_100_up_to_two_decimal_places)
                editor.splitMode == SplitMode.WEIGHTED -> strings.get(R.string.enter_positive_whole_number_shares_amounts_are_divided_in_that_ra)
                editor.splitMode == SplitMode.EXACT -> strings.get(R.string.enter_amounts_that_add_up_to_the_expense_total)
                else -> strings.get(R.string.enter_an_amount_to_preview_each_person_s_share)
            }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 2.dp, vertical = 5.dp))
            DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
                group.participants.filter(Participant::active).forEachIndexed { index, participant ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                    val selected = participant.id in editor.selectedParticipantIds
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = selected, onCheckedChange = { onAction(SplitTheBillsAction.ExpenseParticipantToggled(participant.id)) })
                        Avatar(participant.displayName, 34.dp)
                        Text(if (participant.id == group.currentMemberParticipantId) strings.get(R.string.you) else participant.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 9.dp).weight(1f))
                        if (editor.splitMode == SplitMode.EQUAL) {
                            Text(if (selected && preview.isFailure) "—" else strings.money(allocations[participant.id] ?: 0, group), color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        } else {
                            OutlinedTextField(
                                value = editor.entryValues[participant.id].orEmpty(),
                                onValueChange = { onAction(SplitTheBillsAction.ExpenseEntryValueChanged(participant.id, it)) },
                                enabled = selected,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = if (editor.splitMode == SplitMode.WEIGHTED) KeyboardType.Number else KeyboardType.Decimal),
                                modifier = Modifier.width(108.dp).testTag("split_value_${participant.id}"),
                                supportingText = { allocations[participant.id]?.let { Text(strings.money(it, group)) } },
                                suffix = { Text(when (editor.splitMode) { SplitMode.PERCENTAGE -> "%"; SplitMode.WEIGHTED -> strings.get(R.string.shares_short); else -> group.currency }, fontSize = 10.sp) },
                            )
                        }
                    }
                }
            }
            OutlinedTextField(editor.note, { onAction(SplitTheBillsAction.ExpenseNoteChanged(it)) },
                label = { Text(strings.get(R.string.note_optional)) }, minLines = 2,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("expense_note"))
            state.draftNotice?.let { Text(strings.resolve(it), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)) }
            TextButton(onClick = { onAction(SplitTheBillsAction.DiscardExpenseDraft) }, enabled = !state.saving) {
                Text(if (editor.expenseId == null) strings.get(R.string.discard_draft) else strings.get(R.string.discard_changes))
            }
        }
        Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().testTag("expense_footer")
            .onSizeChanged { footerHeight = with(density) { it.height.toDp() } }, color = MaterialTheme.colorScheme.background, shadowElevation = 1.dp) {
            Button(
                onClick = { onAction(SplitTheBillsAction.SubmitExpense) },
                enabled = !state.saving && state.online,
                modifier = Modifier.fillMaxWidth().padding(12.dp).heightIn(min = 56.dp).testTag("expense_submit"),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Filled.Check, null, Modifier.size(21.dp)); Spacer(Modifier.width(8.dp)); Text(if (editor.expenseId == null) strings.get(R.string.save_expense_value, strings.money(amountMinor, group)) else strings.get(R.string.save_changes), fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun ExpenseSplitSegmented(mode: SplitMode, onSelected: (SplitMode) -> Unit) {
    val strings = uiStrings()
    val options = listOf(SplitMode.EQUAL to strings.get(R.string.equally), SplitMode.PERCENTAGE to strings.get(R.string.percent), SplitMode.EXACT to strings.get(R.string.custom), SplitMode.WEIGHTED to strings.get(R.string.shares))
    val columns = if (LocalDensity.current.fontScale > 1.3f) 2 else 4
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(4.dp).selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { (value, label) ->
                        Surface(
                            Modifier.weight(1f).fillMaxHeight().heightIn(min = 40.dp)
                                .selectable(selected = mode == value, role = androidx.compose.ui.semantics.Role.RadioButton) { onSelected(value) }
                                .semantics { contentDescription = strings.splitMode(value) },
                            color = if (mode == value) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shape = CircleShape, shadowElevation = if (mode == value) 0.5.dp else 0.dp,
                        ) {
                            Box(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                                Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center,
                                    color = if (mode == value) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseDetailScreen(state: SplitTheBillsUiState, padding: PaddingValues, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    val group = state.selectedGroup ?: return
    val expense = group.expenses.firstOrNull { it.id == state.selectedExpenseId }
    if (expense == null) {
        Page(padding) { EmptyCard(strings.get(R.string.expense_unavailable), strings.get(R.string.it_may_have_been_removed_on_another_device)) }
        return
    }
    var confirmRemove by rememberSaveable(expense.id) { mutableStateOf(false) }
    val canEdit = !group.archived && (state.user?.uid == group.ownerUid || state.user?.uid == expense.createdByUid)
    if (confirmRemove) AlertDialog(onDismissRequest = { confirmRemove = false }, title = { Text(strings.get(R.string.remove_this_expense)) },
        text = { Text(strings.get(R.string.the_expense_will_be_removed_and_its_effect_on_everyone_s_balance)) },
        confirmButton = { TextButton(onClick = { confirmRemove = false; onAction(SplitTheBillsAction.DeleteSelectedExpense) }) { Text(strings.get(R.string.remove_expense)) } },
        dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(strings.get(R.string.cancel)) } })
    val locale = LocalConfiguration.current.locales[0]
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 680.dp).fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(Modifier.size(48.dp), color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(16.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(categoryIcon(expense.category), null, Modifier.size(24.dp)) }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            expense.title,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            group.name,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(strings.get(R.string.total_expense), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    StatusPill(strings.category(expense.category), MaterialTheme.colorScheme.surfaceContainer)
                }
                Text(
                    strings.money(expense.amountMinor, group),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp,
                    modifier = Modifier.testTag("expense_detail_total"),
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(modifier = Modifier.align(Alignment.CenterVertically), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(OperationDate.display(expense.date, locale), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        color = if (group.fromCache || group.archived) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (group.fromCache || group.archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Check, null, Modifier.size(13.dp))
                            Text(when { group.archived -> strings.get(R.string.archived); group.fromCache -> strings.get(R.string.cached_copy); else -> strings.get(R.string.saved) }, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                ExpenseParticipants(expense, group, Modifier.padding(top = 12.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(strings.get(R.string.paid_by), null, {})
                DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                    expense.contributions.forEachIndexed { index, contribution ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ExpensePersonRow(
                            name = strings.person(group, contribution.participantId),
                            caption = if (contribution.participantId == group.currentMemberParticipantId) strings.get(R.string.paid_by_you) else strings.get(R.string.paid_toward_this_expense),
                            amount = strings.money(contribution.amountMinor, group),
                            accent = contribution.participantId == group.currentMemberParticipantId,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.get(R.string.confirmed_shares), fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).semantics { heading() })
                    StatusPill(strings.get(R.string.value_split, strings.splitMode(expense.splitMode)), MaterialTheme.colorScheme.surfaceContainer)
                }
                DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                    expense.allocations.forEachIndexed { index, allocation ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ExpensePersonRow(
                            name = strings.person(group, allocation.participantId),
                            caption = if (allocation.participantId == group.currentMemberParticipantId) strings.get(R.string.your_share) else strings.get(R.string.share_of_expense),
                            amount = strings.money(allocation.amountMinor, group),
                            accent = allocation.participantId == group.currentMemberParticipantId,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(strings.get(R.string.total_shares), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Text(strings.money(expense.allocations.sumOf { it.amountMinor }, group), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            expense.note?.takeIf(String::isNotBlank)?.let { note ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(strings.get(R.string.note), null, {})
                    DesignCard(Modifier.fillMaxWidth()) { Text(note, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 1.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { onAction(SplitTheBillsAction.EditSelectedExpense) },
                        enabled = canEdit && !state.saving,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("expense_detail_edit"),
                    ) {
                        Icon(Icons.Outlined.Edit, null, Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.online) strings.get(R.string.edit) else strings.get(R.string.edit_draft), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { confirmRemove = true },
                        enabled = state.online && canEdit && !state.saving,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("expense_detail_remove"),
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.get(R.string.remove), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpensePersonRow(name: String, caption: String, amount: String, accent: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Avatar(name, 38.dp)
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(caption, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            amount,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SettlementEditorScreen(state: SplitTheBillsUiState, padding: PaddingValues, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    val group = state.selectedGroup ?: return
    val form = state.settlementEditor
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(group.name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.padding(vertical = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Avatar(strings.person(group, form.fromParticipantId), 58.dp); Text(strings.person(group, form.fromParticipantId), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp)) }
            Surface(Modifier.size(44.dp), color = MaterialTheme.colorScheme.surface, shape = CircleShape) { Box(contentAlignment = Alignment.Center) { Text("→", fontSize = 22.sp) } }
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Avatar(strings.person(group, form.toParticipantId), 58.dp); Text(strings.person(group, form.toParticipantId), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp)) }
        }
        OutlinedTextField(
            form.amount,
            { onAction(SplitTheBillsAction.SettlementAmountChanged(it)) },
            prefix = { Text(strings.currencySymbol(group.currency), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 24.sp) },
            textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().testTag("settlement_amount"),
        )
        DesignCard(Modifier.fillMaxWidth().padding(top = 18.dp), contentPadding = PaddingValues(14.dp)) {
            Text(strings.get(R.string.paid_by), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            ParticipantChips(group.participants, form.fromParticipantId) { onAction(SplitTheBillsAction.SettlementFromChanged(it)) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 8.dp))
            Text(strings.get(R.string.paid_to), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            ParticipantChips(group.participants, form.toParticipantId) { onAction(SplitTheBillsAction.SettlementToChanged(it)) }
        }
        OperationDateField(
            date = form.date,
            onDateSelected = { onAction(SplitTheBillsAction.SettlementDateChanged(it)) },
            modifier = Modifier.padding(top = 14.dp).testTag("settlement_date"),
        )
        OutlinedTextField(form.note, { onAction(SplitTheBillsAction.SettlementNoteChanged(it)) }, label = { Text(strings.get(R.string.note_optional)) }, minLines = 2, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(16.dp))
        Text(strings.get(R.string.this_records_money_already_paid_outside_split_the_bills_the_app_d), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
        Button(onClick = { onAction(SplitTheBillsAction.SubmitSettlement) }, enabled = !group.archived && !state.saving && state.online, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("settlement_submit"), shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Filled.Check, null, Modifier.size(20.dp)); Spacer(Modifier.width(7.dp)); Text(if (form.settlementId == null) strings.get(R.string.mark_as_settled) else strings.get(R.string.save_repayment), fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SettingsScreen(state: SplitTheBillsUiState, padding: PaddingValues, onAction: (SplitTheBillsAction) -> Unit, onBiometricLockChange: ((Boolean) -> Unit)?) {
    val strings = uiStrings()
    var choosingLanguage by rememberSaveable { mutableStateOf(false) }
    var editingName by rememberSaveable { mutableStateOf(false) }
    var editingCurrency by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val allGroups = state.loadedGroups()
    val peopleCount = allGroups.flatMap { group -> group.participants.filter { it.active && it.id != group.currentMemberParticipantId }
        .map { it.linkedUid ?: "${group.id}/${it.id}" } }.distinct().size
    val profileDisplayName = state.user?.displayName?.takeIf(String::isNotBlank)
        ?: state.profileName.takeIf(String::isNotBlank)
        ?: state.user?.email?.substringBefore('@')
        ?: strings.get(R.string.you)
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        GroupLoadStatus(state, onAction)
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(strings.get(R.string.profile), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = strings.get(R.string.profile_options)) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text(strings.get(R.string.edit_display_name)) }, onClick = { menu = false; editingName = true })
                    DropdownMenuItem(text = { Text(strings.get(R.string.view_friends)) }, onClick = { menu = false; onAction(SplitTheBillsAction.OpenFeature(AppScreen.FRIENDS)) })
                    DropdownMenuItem(text = { Text(strings.get(R.string.help_support)) }, onClick = { menu = false; onAction(SplitTheBillsAction.OpenFeature(AppScreen.HELP)) })
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(profileDisplayName, 88.dp)
            Text(profileDisplayName, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp))
            Text(state.user?.email ?: strings.get(R.string.signed_in), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileMetric(state.groups.size.toString(), strings.get(R.string.groups), Modifier.weight(1f))
                ProfileMetric(peopleCount.toString(), strings.get(R.string.friends), Modifier.weight(1f))
                ProfileMetric(allGroups.sumOf { it.settlements.size }.toString(), strings.get(R.string.settled), Modifier.weight(1f))
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
                ProfileActionRow(strings.get(R.string.display_name), profileDisplayName, onClick = { editingName = !editingName }) {
                    if (editingName) {
                        OutlinedTextField(state.profileName, { onAction(SplitTheBillsAction.ProfileNameChanged(it)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        Button(onClick = { onAction(SplitTheBillsAction.SaveProfile); editingName = false }, enabled = !state.saving && state.online && state.profileName.isNotBlank(), modifier = Modifier.padding(top = 8.dp)) { Text(strings.get(R.string.save_profile)) }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ProfileActionRow(strings.get(R.string.default_currency), state.preferences.defaultCurrency, onClick = { editingCurrency = !editingCurrency }) {
                    if (editingCurrency) CurrencyPickerField(state.preferences.defaultCurrency, {
                        onAction(SplitTheBillsAction.DefaultCurrencyChanged(it)); editingCurrency = false
                    }, Modifier.fillMaxWidth())
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ProfileActionRow(
                    strings.get(R.string.language),
                    strings.languageLabel(androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()[0]?.language.orEmpty()),
                    onClick = { choosingLanguage = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ProfileActionRow(strings.get(R.string.friends), strings.plural(R.plurals.people_count, peopleCount), onClick = { onAction(SplitTheBillsAction.OpenFeature(AppScreen.FRIENDS)) })
            }
            DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
                Text(strings.get(R.string.appearance), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePreference.entries.forEach { preference ->
                        DesignChip(strings.theme(preference), state.themePreference == preference) { onAction(SplitTheBillsAction.ThemeChanged(preference)) }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), modifier = Modifier.padding(top = 8.dp))
                ProfileActionRow(strings.get(R.string.simplify_debts), if (state.preferences.simplifyDebts) strings.get(R.string.combine_balances_to_reduce_transfers) else strings.get(R.string.keep_original_expense_counterparties), showChevron = false) {
                    Switch(checked = state.preferences.simplifyDebts, onCheckedChange = { onAction(SplitTheBillsAction.SimplifyDebtsChanged(it)) }, modifier = Modifier.testTag("simplify_debts"))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ProfileActionRow(strings.get(R.string.biometric_lock), if (state.preferences.biometricLock) strings.get(R.string.on_authentication_required_when_reopened) else strings.get(R.string.off_secure_this_app_on_your_device), showChevron = false) {
                    Switch(checked = state.preferences.biometricLock, enabled = onBiometricLockChange != null,
                        onCheckedChange = { onBiometricLockChange?.invoke(it) }, modifier = Modifier.testTag("biometric_lock"))
                }
            }
            DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
                ProfileActionRow(strings.get(R.string.help_support), strings.get(R.string.get_answers_and_report_an_issue), onClick = { onAction(SplitTheBillsAction.OpenFeature(AppScreen.HELP)) })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Row(Modifier.fillMaxWidth().clickable { onAction(SplitTheBillsAction.SignOut) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(strings.get(R.string.sign_out), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                TextButton(onClick = { onAction(SplitTheBillsAction.OpenAccountDeletion) }, enabled = !state.saving && state.online, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary), modifier = Modifier.fillMaxWidth()) { Text(strings.get(R.string.delete_account)) }
            }
        }
    }
    if (choosingLanguage) LanguagePicker { choosingLanguage = false }
}

@Composable
private fun ProfileMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold); Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) } }
}

@Composable
private fun ProfileActionRow(title: String, subtitle: String, showChevron: Boolean = true, onClick: (() -> Unit)? = null, extra: (@Composable ColumnScope.() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 10.dp, vertical = 11.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(40.dp), color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(12.dp)) { Box(contentAlignment = Alignment.Center) { Text(title.take(1), fontWeight = FontWeight.ExtraBold) } }
            Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
            if (showChevron) Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 22.sp)
        }
        extra?.invoke(this)
    }
}

@Composable
private fun AccountDeletionDialog(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    val deletion = state.accountDeletion
    if (!deletion.open) return
    val ownedGroups = state.groups.filter { it.role == "OWNER" }
    AlertDialog(
        onDismissRequest = { if (!state.saving) onAction(SplitTheBillsAction.CloseAccountDeletion) },
        title = { Text(strings.get(R.string.delete_your_account)) },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(strings.get(R.string.this_cannot_be_undone_memberships_are_revoked_profile_data_is_rem))
                if (deletion.loadingCandidates) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                        Text(strings.get(R.string.loading_ownership_choices))
                    }
                } else {
                    ownedGroups.forEach { group ->
                        Text(strings.get(R.string.new_owner_for_value, group.name), fontWeight = FontWeight.SemiBold)
                        val candidates = deletion.candidates[group.id].orEmpty()
                        if (candidates.isEmpty()) {
                            Text(
                                strings.get(R.string.no_other_member_is_available_invite_someone_to_this_group_before),
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                candidates.forEach { candidate ->
                                    FilterChip(
                                        selected = deletion.selectedNewOwners[group.id] == candidate.uid,
                                        onClick = {
                                            onAction(SplitTheBillsAction.AccountDeletionOwnerChanged(group.id, candidate.uid))
                                        },
                                        label = { Text(candidate.displayName ?: strings.get(R.string.member_value, candidate.uid.take(8))) },
                                    )
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = deletion.confirmation,
                    onValueChange = { onAction(SplitTheBillsAction.AccountDeletionConfirmationChanged(it)) },
                    label = { Text(strings.get(R.string.type_delete_to_confirm)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(SplitTheBillsAction.ConfirmAccountDeletion) },
                enabled = !state.saving && !deletion.loadingCandidates && deletion.confirmation == "DELETE",
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(strings.get(R.string.delete_permanently)) }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(SplitTheBillsAction.CloseAccountDeletion) },
                enabled = !state.saving,
            ) { Text(strings.get(R.string.cancel)) }
        },
    )
}

@Composable
private fun ParticipantChips(participants: List<Participant>, selectedId: String, onSelected: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        participants.filter(Participant::active).forEach { participant ->
            FilterChip(selected = selectedId == participant.id, onClick = { onSelected(participant.id) }, label = { Text(participant.displayName) })
        }
    }
}

@Composable
private fun ErrorDialogs(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
    val strings = uiStrings()
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = { onAction(SplitTheBillsAction.ErrorShown) },
            title = { Text(strings.get(R.string.couldn_t_complete_that)) },
            text = { Text(strings.resolve(error)) },
            confirmButton = { TextButton(onClick = { onAction(SplitTheBillsAction.ErrorShown) }) { Text(strings.get(R.string.ok)) } },
        )
    }
    state.conflict?.let { conflict ->
        AlertDialog(
            onDismissRequest = { onAction(SplitTheBillsAction.ConflictDismissed) },
            title = { Text(strings.get(R.string.newer_version_found)) },
            text = { Text(strings.resolve(conflict)) },
            confirmButton = {
                TextButton(onClick = { onAction(SplitTheBillsAction.ConflictReloaded) }) {
                    Text(
                        when (state.conflictTarget) {
                            ConflictTarget.INVITATION -> strings.get(R.string.refresh_invitation)
                            ConflictTarget.GROUP -> strings.get(R.string.reload_group)
                            else -> strings.get(R.string.reload_server_version)
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(SplitTheBillsAction.ConflictDismissed) }) { Text(strings.get(R.string.keep_draft)) }
            },
        )
    }
}

@Composable
private fun Page(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 900.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun FormPage(padding: PaddingValues, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EmptyCard(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun SectionLabel(title: String, detail: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        detail?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusPill(label: String, containerColor: Color) {
    Surface(color = containerColor, shape = CircleShape) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FormSection(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        content()
    }
}

@Composable
private fun LoadingBlock() {
    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun OfflineBanner() {
    val strings = uiStrings()
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(strings.get(R.string.offline_cached_data_remains_available_shared_changes_are_disabled), modifier = Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun SavingOverlay() {
    val strings = uiStrings()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), shape = RoundedCornerShape(18.dp), tonalElevation = 8.dp) {
            Row(Modifier.padding(22.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(28.dp))
                Text(strings.get(R.string.waiting_for_server_confirmation))
            }
        }
    }
}

@Composable
private fun MoneyText(amountMinor: Long, group: GroupDetail) {
    val strings = uiStrings()
    Text(strings.money(amountMinor, group), fontWeight = FontWeight.Bold)
}


private fun invitationLink(token: String): String =
    "https://links.example.invalid/invite?token=${Uri.encode(token)}"

private fun shortUid(uid: String): String = if (uid.length <= 10) uid else "${uid.take(6)}…${uid.takeLast(4)}"


@PreviewScreenSizes
@Composable
private fun AuthPreview() {
    SplitTheBillsTheme(dynamicColor = false) {
        SplitTheBillsApp(
            state = SplitTheBillsUiState(authResolved = true),
            onAction = {},
            onGoogleSignIn = {},
        )
    }
}

@Preview(name = "Settings · 200% font", device = Devices.PHONE, fontScale = 2f, showSystemUi = true)
@Preview(name = "Settings · tablet", device = Devices.TABLET, showSystemUi = true)
@Preview(
    name = "Settings · dark",
    device = Devices.PHONE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
)
@Composable
private fun SettingsAdaptivePreview() {
    SplitTheBillsTheme(dynamicColor = false) {
        SplitTheBillsApp(
            state = SplitTheBillsUiState(
                authResolved = true,
                user = com.dts.splitthebill.domain.model.SignedInUser(
                    uid = "preview-user",
                    email = "alex@example.test",
                    displayName = "Alex",
                ),
                screen = AppScreen.SETTINGS,
                profileName = "Alex",
                groups = listOf(
                    GroupSummary("preview-group", "Coast weekend", "EUR", "OWNER", "p1", 4, false),
                ),
            ),
            onAction = {},
            onGoogleSignIn = {},
        )
    }
}
