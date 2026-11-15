package com.dts.splitthebill.ui

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.dts.splitthebill.domain.model.Participant
import com.dts.splitthebill.domain.model.SplitMode
import com.dts.splitthebill.domain.repository.ThemePreference
import com.dts.splitthebill.ui.theme.LocalBalanceCardColors
import com.dts.splitthebill.ui.theme.SplitTheBillsTheme
import java.text.DateFormat
import java.util.Date

private enum class MainDestination { HOME, GROUPS, ACTIVITY, PROFILE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTheBillsApp(
    state: SplitTheBillsUiState,
    onAction: (SplitTheBillsAction) -> Unit,
    onGoogleSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
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

    var destination by rememberSaveable { mutableStateOf(MainDestination.HOME) }
    val nested = state.screen !in setOf(AppScreen.GROUPS, AppScreen.SETTINGS)
    if (state.screen == AppScreen.SETTINGS) destination = MainDestination.PROFILE

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (nested) {
                Column {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { onAction(SplitTheBillsAction.Back) }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        title = { Text(nestedTitle(state), fontWeight = FontWeight.ExtraBold) },
                        actions = {
                            if (state.screen == AppScreen.GROUP_DETAIL) {
                                IconButton(onClick = { onAction(SplitTheBillsAction.OpenGroupMembers) }) {
                                    Icon(Icons.Outlined.PersonAdd, contentDescription = "Manage people")
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
                DesignBottomNavigation(destination) { selected ->
                    destination = selected
                    when (selected) {
                        MainDestination.GROUPS -> onAction(SplitTheBillsAction.OpenGroups)
                        MainDestination.PROFILE -> onAction(SplitTheBillsAction.OpenSettings)
                        MainDestination.HOME, MainDestination.ACTIVITY -> if (state.screen == AppScreen.SETTINGS) {
                            onAction(SplitTheBillsAction.OpenGroups)
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (!nested) {
            when (destination) {
                MainDestination.HOME -> HomeScreen(state, padding, onAction) {
                    destination = MainDestination.GROUPS
                    onAction(SplitTheBillsAction.OpenGroups)
                }
                MainDestination.GROUPS -> GroupsScreen(state, padding, onAction)
                MainDestination.ACTIVITY -> GlobalActivityScreen(state, padding, onAction)
                MainDestination.PROFILE -> SettingsScreen(state, padding, onAction)
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
                else -> Unit
            }
        }
        if (state.saving) SavingOverlay()
    }
    ErrorDialogs(state, onAction)
    AccountDeletionDialog(state, onAction)
}

private fun nestedTitle(state: SplitTheBillsUiState): String = when (state.screen) {
    AppScreen.EXPENSE_EDITOR -> if (state.expenseEditor.expenseId == null) "Add expense" else "Edit expense"
    AppScreen.EXPENSE_DETAIL -> "Expense"
    AppScreen.SETTLEMENT_EDITOR -> "Settle up"
    AppScreen.CREATE_GROUP -> "Create group"
    AppScreen.GROUP_MEMBERS -> "Group members"
    AppScreen.INVITATION_REDEMPTION -> "Join a group"
    else -> ""
}

@Composable
private fun DesignBottomNavigation(selected: MainDestination, onSelected: (MainDestination) -> Unit) {
    val items: List<Triple<MainDestination, String, ImageVector>> = listOf(
        Triple(MainDestination.HOME, "Home", Icons.Filled.Home),
        Triple(MainDestination.GROUPS, "Groups", Icons.Filled.Group),
        Triple(MainDestination.ACTIVITY, "Activity", Icons.Filled.ReceiptLong),
        Triple(MainDestination.PROFILE, "Profile", Icons.Filled.Person),
    )
    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f), shadowElevation = 1.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(76.dp).padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { (destination, label, icon) ->
                val active = selected == destination
                Column(
                    modifier = Modifier.weight(1f).clickable { onSelected(destination) },
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
            Text("Clear shares, calmer plans", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                if (state.authForm.mode == AuthMode.SIGN_IN) "Welcome back" else "Create your account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                "Shared expenses, clearly organized.",
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
                        label = { Text("Sign in") },
                    )
                    FilterChip(
                        selected = state.authForm.mode == AuthMode.CREATE_ACCOUNT,
                        onClick = { onAction(SplitTheBillsAction.AuthModeChanged(AuthMode.CREATE_ACCOUNT)) },
                        label = { Text("Create account") },
                    )
                }
                OutlinedTextField(
                    value = state.authForm.email,
                    onValueChange = { onAction(SplitTheBillsAction.AuthEmailChanged(it)) },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("auth_email"),
                )
                OutlinedTextField(
                    value = state.authForm.password,
                    onValueChange = { onAction(SplitTheBillsAction.AuthPasswordChanged(it)) },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("auth_password"),
                )
                Button(
                    onClick = { onAction(SplitTheBillsAction.SubmitEmailAuth) },
                    enabled = !state.saving && state.online,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("auth_submit"),
                ) {
                    Text(if (state.authForm.mode == AuthMode.SIGN_IN) "Sign in" else "Create account")
                }
                OutlinedButton(
                    onClick = onGoogleSignIn,
                    enabled = !state.saving && state.online,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Continue with Google") }
                TextButton(
                    onClick = { onAction(SplitTheBillsAction.ResetPassword) },
                    enabled = !state.saving,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Forgot password?") }
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
    val detail = state.selectedGroup
    val myBalance = detail?.balances?.firstOrNull { it.participantId == detail.currentMemberParticipantId }?.amountMinor ?: 0L
    val owed = myBalance.coerceAtLeast(0)
    val owe = (-myBalance).coerceAtLeast(0)
    val name = state.user?.displayName?.substringBefore(' ')?.takeIf(String::isNotBlank) ?: "there"
    Box(Modifier.fillMaxSize().padding(padding)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Avatar(name = state.user?.displayName ?: "You", size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text("Good morning", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                IconButton(onClick = {}) { Icon(Icons.Outlined.Search, contentDescription = "Search") }
                IconButton(onClick = {}) { Icon(Icons.Outlined.Notifications, contentDescription = "Notifications") }
            }
            BalanceHero(myBalance, owed, owe, detail) { detail?.let { onAction(SplitTheBillsAction.OpenSettlement) } }
            SectionHeader("Active groups", "See all", onSeeAll, Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.groups.isEmpty() && !state.groupsLoading) {
                    DesignCard(Modifier.width(230.dp).clickable { onSeeAll() }) {
                        GroupTile(null, 44.dp)
                        Text("Create your first group", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                        Text("Trips, homes, and shared plans", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
                state.groups.forEach { group ->
                    DesignCard(
                        Modifier.width(178.dp).clickable { onAction(SplitTheBillsAction.OpenGroup(group.id)) },
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            GroupTile(group, 44.dp)
                            Avatar(group.name, 24.dp)
                        }
                        Text(group.name, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                        Text(if (group.archived) "archived" else "active", color = if (group.archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(group.currency, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = if (group.archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                    }
                }
            }
            SectionHeader("Recent transactions", "View all", {}, Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 10.dp))
            DesignCard(Modifier.padding(horizontal = 16.dp), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                val recent = detail?.expenses.orEmpty().sortedByDescending { it.date }.take(4)
                if (recent.isEmpty()) {
                    Text("Your latest shared expenses will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(14.dp))
                } else {
                    recent.forEachIndexed { index, expense ->
                        ExpenseDesignRow(expense, detail!!, index > 0) { onAction(SplitTheBillsAction.OpenExpense(expense.id)) }
                    }
                }
            }
        }
        DesignFab("Add expense", Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            if (detail != null) onAction(SplitTheBillsAction.OpenNewExpense) else onSeeAll()
        }
    }
}

@Composable
private fun BalanceHero(balance: Long, owed: Long, owe: Long, group: GroupDetail?, onSettle: () -> Unit) {
    val colors = LocalBalanceCardColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("balance_card"),
        shape = RoundedCornerShape(28.dp),
        color = colors.container,
        contentColor = colors.content,
    ) {
        Column(Modifier.padding(22.dp)) {
            Text("Total balance", color = colors.supporting, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(formatMinorSigned(balance, group), color = if (balance >= 0) colors.content else colors.negative, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.2).sp)
            Text(if (balance >= 0) "Overall, friends owe you" else "Overall, you owe friends", color = colors.supporting, fontSize = 13.sp)
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DarkMetric("You are owed", owed, colors.positive, group, Modifier.weight(1f))
                DarkMetric("You owe", owe, colors.negative, group, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onSettle, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = colors.content, contentColor = colors.container), shape = CircleShape) {
                    Icon(Icons.Outlined.Handshake, contentDescription = null, Modifier.size(19.dp)); Spacer(Modifier.width(7.dp)); Text("Settle up")
                }
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, colors.supporting), colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.content), shape = CircleShape) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Remind")
                }
            }
        }
    }
}

@Composable
private fun DarkMetric(label: String, amount: Long, color: Color, group: GroupDetail?, modifier: Modifier = Modifier) {
    val colors = LocalBalanceCardColors.current
    Surface(modifier, color = colors.content.copy(alpha = 0.07f), contentColor = colors.content, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(13.dp)) {
            Text(label, color = colors.supporting, fontSize = 12.sp)
            Text(formatMinor(amount, group), color = color, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun GroupsScreen(state: SplitTheBillsUiState, padding: PaddingValues, onAction: (SplitTheBillsAction) -> Unit) {
    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    val filters = listOf("All", "Trip", "Home", "Friends", "Couple")
    Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Groups", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = {}) { Icon(Icons.Outlined.Search, contentDescription = "Search groups") }
                IconButton(onClick = {}) { Icon(Icons.Outlined.MoreVert, contentDescription = "More") }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEach { filter ->
                    DesignChip(if (filter == "All") "All · ${state.groups.size}" else filter, selectedFilter == filter) { selectedFilter = filter }
                }
            }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    state.groupsLoading -> LoadingBlock()
                    state.groups.isEmpty() -> EmptyCard("Nothing split yet", "Create a group for a trip, home, or shared plan.")
                    else -> state.groups.forEach { group -> GroupCard(group, onAction) }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp)).clickable { onAction(SplitTheBillsAction.OpenCreateGroup) },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(Modifier.size(44.dp), color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(14.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.GroupAdd, null) } }
                        Column(Modifier.weight(1f)) { Text("Create a group", fontWeight = FontWeight.Bold); Text("For a trip, a flat, or a night out", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
                    }
                }
                TextButton(onClick = { onAction(SplitTheBillsAction.OpenInvitationRedemption) }, modifier = Modifier.fillMaxWidth().testTag("open_invitation")) { Text("Have an invitation? Join a group") }
            }
        }
        DesignFab("Add expense", Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            if (state.selectedGroup != null) onAction(SplitTheBillsAction.OpenNewExpense) else onAction(SplitTheBillsAction.OpenCreateGroup)
        }
    }
}

@Composable
private fun GroupCard(group: GroupSummary, onAction: (SplitTheBillsAction) -> Unit) {
    DesignCard(Modifier.fillMaxWidth().clickable { onAction(SplitTheBillsAction.OpenGroup(group.id)) }, contentPadding = PaddingValues(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GroupTile(group, 52.dp)
            Column(Modifier.weight(1f)) {
                Text(group.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text("${group.currency} · ${group.role.lowercase().replaceFirstChar(Char::uppercase)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (group.archived) "archived" else "active", color = if (group.archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(group.currency, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (group.archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(group.name, 26.dp)
            Text("Shared group", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
            Text("Open details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text("Settled", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
        Surface(Modifier.fillMaxWidth().height(6.dp).padding(top = 2.dp), color = MaterialTheme.colorScheme.surfaceContainer, shape = CircleShape) {}
    }
}

@Composable
private fun GlobalActivityScreen(state: SplitTheBillsUiState, padding: PaddingValues, onAction: (SplitTheBillsAction) -> Unit) {
    val group = state.selectedGroup
    var filter by rememberSaveable { mutableStateOf("All") }
    val expenses = if (filter == "Payments") emptyList() else group?.expenses.orEmpty().sortedByDescending { it.date }
    val settlements = if (filter == "Expenses") emptyList() else group?.settlements.orEmpty().sortedByDescending { it.date }
    val currentParticipantId = group?.currentMemberParticipantId
    val yourShare = group?.expenses.orEmpty().sumOf { expense ->
        expense.allocations.firstOrNull { it.participantId == currentParticipantId }?.amountMinor ?: 0L
    }
    Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Activity", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = {}) { Icon(Icons.Outlined.Search, contentDescription = "Search activity") }
                IconButton(onClick = {}) { Icon(Icons.Outlined.MoreVert, contentDescription = "Filter activity") }
            }
            DesignCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), contentPadding = PaddingValues(18.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("Your share this month", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(formatMinor(yourShare, group), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(18.dp, 32.dp, 42.dp).forEachIndexed { index, height ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(Modifier.width(14.dp).height(height), color = if (index == 2) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(4.dp)) {}
                                Text("W${index + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Expenses", "Payments").forEach { item -> DesignChip(item, filter == item) { filter = item } }
            }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (group == null) {
                    EmptyCard("No activity selected", "Open a group to load its latest expenses and repayments.")
                } else {
                    Text(group.name.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                    expenses.forEach { expense ->
                        DesignCard(Modifier.fillMaxWidth().clickable { onAction(SplitTheBillsAction.OpenExpense(expense.id)) }, contentPadding = PaddingValues(14.dp)) {
                            ExpenseDesignRow(expense, group, false) { onAction(SplitTheBillsAction.OpenExpense(expense.id)) }
                        }
                    }
                    settlements.forEach { settlement ->
                        DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(Modifier.size(44.dp), color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Handshake, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) } }
                                Column(Modifier.weight(1f)) {
                                    Text("${nameOf(group, settlement.fromParticipantId)} paid ${nameOf(group, settlement.toParticipantId)}", fontWeight = FontWeight.Bold)
                                    Text(settlement.date, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                                Text(format(settlement.amountMinor, group), fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    if (expenses.isEmpty() && settlements.isEmpty()) EmptyCard("Quiet for now", "Confirmed group changes will appear here.")
                }
            }
        }
        DesignFab("Add expense", Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            if (group != null) onAction(SplitTheBillsAction.OpenNewExpense)
        }
    }
}

@Composable
private fun DesignCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), shadowElevation = 0.5.dp) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
private fun GroupTile(group: GroupSummary?, size: androidx.compose.ui.unit.Dp) {
    val icon = when {
        group == null -> Icons.Outlined.GroupAdd
        group.name.contains("trip", true) || group.name.contains("travel", true) -> Icons.Outlined.Flight
        group.name.contains("home", true) || group.name.contains("street", true) || group.name.contains("flat", true) -> Icons.Outlined.Home
        group.name.contains("dinner", true) || group.name.contains("food", true) -> Icons.Outlined.Restaurant
        else -> Icons.Outlined.FavoriteBorder
    }
    val color = when {
        group == null -> MaterialTheme.colorScheme.surfaceContainer
        group.name.contains("trip", true) || group.name.contains("travel", true) -> MaterialTheme.colorScheme.tertiaryContainer
        group.name.contains("home", true) || group.name.contains("street", true) || group.name.contains("flat", true) -> MaterialTheme.colorScheme.primaryContainer
        group.name.contains("dinner", true) || group.name.contains("food", true) -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    Surface(Modifier.size(size), color = color, shape = RoundedCornerShape(size / 3)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(size / 2), tint = MaterialTheme.colorScheme.contentColorFor(color)) }
    }
}

@Composable
private fun Avatar(name: String, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
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
        modifier = Modifier.height(36.dp).clickable(onClick = onClick),
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
        modifier = modifier.height(56.dp),
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
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        action?.let { TextButton(onClick = onAction) { Text(it, fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
    }
}

@Composable
private fun ExpenseDesignRow(expense: Expense, group: GroupDetail, divider: Boolean, onClick: () -> Unit) {
    if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(Modifier.size(44.dp), color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(14.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(categoryIcon(expense.category), null, modifier = Modifier.size(21.dp)) }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(expense.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${nameOf(group, expense.contributions.firstOrNull()?.participantId.orEmpty())} paid ${format(expense.amountMinor, group)} · ${expense.date}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                }
                val myId = group.currentMemberParticipantId
                val share = expense.allocations.firstOrNull { it.participantId == myId }?.amountMinor ?: 0L
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (expense.contributions.any { it.participantId == myId }) "you lent" else "you owe", color = if (expense.contributions.any { it.participantId == myId }) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, fontSize = 10.sp)
                    Text(format(share, group), color = if (expense.contributions.any { it.participantId == myId }) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }
            ExpenseParticipants(expense, group, Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
internal fun ExpenseParticipants(expense: Expense, group: GroupDetail, modifier: Modifier = Modifier) {
    val participantIds = (expense.contributions.map { it.participantId } +
        expense.allocations.map { it.participantId }).distinct()
    if (participantIds.isEmpty()) return
    val names = participantIds.map { nameOf(group, it) }
    val shown = names.take(4)
    val remaining = names.size - shown.size
    val circles = shown.size + if (remaining > 0) 1 else 0
    Row(
        modifier = modifier.testTag("expense_participants_${expense.id}").clearAndSetSemantics {
            contentDescription = "Participants: ${names.joinToString()}"
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
            if (names.size == 1) "1 person" else "${names.size} people",
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

private fun formatMinor(amountMinor: Long, group: GroupDetail?): String =
    if (group != null) format(amountMinor, group)
    else "$" + MoneyText.formatMinor(amountMinor, CurrencyUnit("USD", 2))

private fun formatMinorSigned(amountMinor: Long, group: GroupDetail?): String =
    (if (amountMinor >= 0) "+" else "−") + formatMinor(kotlin.math.abs(amountMinor), group)

@Composable
private fun InvitationRedemptionScreen(
    state: SplitTheBillsUiState,
    padding: PaddingValues,
    onAction: (SplitTheBillsAction) -> Unit,
) {
    val form = state.invitationRedemption
    FormPage(
        padding,
        "Join a shared space",
        "Invitation access and participant identity are separate. The owner must approve any participant link.",
    ) {
        if (form.fromLink) {
            EmptyCard("Invitation link ready", "Sign in and confirm below. The raw token stays hidden.")
        } else {
            OutlinedTextField(
                value = form.value,
                onValueChange = { onAction(SplitTheBillsAction.InvitationValueChanged(it)) },
                label = { Text("Invitation code") },
                supportingText = { Text("Example: ABCDE-23456") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("invitation_code"),
            )
        }
        Button(
            onClick = { onAction(SplitTheBillsAction.SubmitInvitation) },
            enabled = state.online && !state.saving && form.value.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("invitation_submit"),
        ) { Text(if (state.saving) "Checking with server…" else "Accept invitation") }
    }
}

@Composable
private fun CreateGroupScreen(state: SplitTheBillsUiState, padding: PaddingValues, onAction: (SplitTheBillsAction) -> Unit) {
    FormPage(padding, "Create a shared space", "One currency keeps every balance understandable.") {
        FormSection("Group details", "You can add more people after creating the group.") {
            OutlinedTextField(
                state.createGroupForm.name,
                { onAction(SplitTheBillsAction.CreateGroupNameChanged(it)) },
                label = { Text("Group name") },
                placeholder = { Text("Weekend away") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("group_name"),
            )
            CurrencyPickerField(
                currencyCode = state.createGroupForm.currency,
                onCurrencySelected = { onAction(SplitTheBillsAction.CreateGroupCurrencyChanged(it)) },
            )
            Text(
                "Currency is locked after the first expense or repayment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                state.createGroupForm.participantName,
                { onAction(SplitTheBillsAction.CreateGroupParticipantChanged(it)) },
                label = { Text("Your participant name") },
                supportingText = { Text("Names never determine account identity.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("participant_name"),
            )
        }
        Button(
            onClick = { onAction(SplitTheBillsAction.SubmitGroup) },
            enabled = !state.saving && state.online,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("group_submit"),
        ) {
            Text(if (state.saving) "Creating…" else "Create group")
        }
    }
}

@Composable
private fun GroupScreen(state: SplitTheBillsUiState, padding: PaddingValues, onAction: (SplitTheBillsAction) -> Unit) {
    val group = state.selectedGroup
    if (state.groupLoading || group == null) {
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val totalSpend = group.expenses.sumOf(Expense::amountMinor)
    val myBalance = group.balances.firstOrNull { it.participantId == group.currentMemberParticipantId }?.amountMinor ?: 0L
    Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp).padding(bottom = 94.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GroupTile(GroupSummary(group.id, group.name, group.currency, "", group.currentMemberParticipantId, group.revision, group.archived), 64.dp)
                Column(Modifier.weight(1f)) {
                    Text(group.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                    Text("${group.currency} · ${group.participants.count(Participant::active)} members", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            DesignCard(Modifier.fillMaxWidth().padding(top = 14.dp), contentPadding = PaddingValues(18.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) { Text("Group spend", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); Text(format(totalSpend, group), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold) }
                    Surface(Modifier.width(1.dp).height(48.dp), color = MaterialTheme.colorScheme.outlineVariant) {}
                    Column(Modifier.weight(1f).padding(start = 14.dp)) { Text("Your balance", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); Text(formatMinorSigned(myBalance, group), color = if (myBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold) }
                }
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAction(SplitTheBillsAction.SelectGroupTab(GroupTab.BALANCES)) }, modifier = Modifier.weight(1f).height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface), shape = CircleShape) { Icon(Icons.Outlined.Handshake, null, Modifier.size(19.dp)); Spacer(Modifier.width(6.dp)); Text("Settle up") }
                    Button(onClick = {}, modifier = Modifier.weight(1f).height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface), shape = CircleShape) { Text("Export") }
                }
            }
            DesignSegmented(
                selected = state.selectedTab,
                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                onSelected = { onAction(SplitTheBillsAction.SelectGroupTab(it)) },
            )
            when (state.selectedTab) {
                GroupTab.EXPENSES -> ExpenseList(group, onAction)
                GroupTab.BALANCES -> BalanceList(group, onAction)
                GroupTab.ACTIVITY -> GroupMembersSummary(group)
            }
        }
        if (state.selectedTab == GroupTab.EXPENSES) {
            DesignFab("Add expense", Modifier.align(Alignment.BottomEnd).padding(16.dp)) { onAction(SplitTheBillsAction.OpenNewExpense) }
        }
    }
}

@Composable
private fun DesignSegmented(selected: GroupTab, modifier: Modifier = Modifier, onSelected: (GroupTab) -> Unit) {
    val tabs = listOf(GroupTab.EXPENSES to "Expenses", GroupTab.BALANCES to "Balances", GroupTab.ACTIVITY to "Members")
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainer, shape = CircleShape) {
        Row(Modifier.fillMaxWidth().padding(4.dp)) {
            tabs.forEach { (tab, label) ->
                Surface(
                    modifier = Modifier.weight(1f).height(36.dp).clickable { onSelected(tab) },
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
    DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
        group.participants.filter(Participant::active).forEachIndexed { index, participant ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            val balance = group.balances.firstOrNull { it.participantId == participant.id }?.amountMinor ?: 0L
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(participant.displayName, 38.dp)
                Column(Modifier.weight(1f)) { Text(if (participant.id == group.currentMemberParticipantId) "You" else participant.displayName, fontWeight = FontWeight.Bold); Text(if (participant.linkedUid == null) "Invitation pending" else "Group member", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
                Column(horizontalAlignment = Alignment.End) { Text(formatMinorSigned(balance, group), color = if (balance > 0) MaterialTheme.colorScheme.primary else if (balance < 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.ExtraBold); Text(if (balance > 0) "gets back" else if (balance < 0) "owes" else "even", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) }
            }
        }
    }
}

@Composable
private fun ParticipantAdder(state: SplitTheBillsUiState, onAction: (SplitTheBillsAction) -> Unit) {
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
            label = { Text("Add named participant") },
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
        ) { Text("Add") }
    }
}

@Composable
private fun GroupMembersScreen(
    state: SplitTheBillsUiState,
    padding: PaddingValues,
    onAction: (SplitTheBillsAction) -> Unit,
) {
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
        "People & access",
        "Manage participants, invitations, and account links for this group.",
    ) {
        FormSection("Participants", "People included in expenses and balances.") {
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
                            participant.linkedUid?.let { "Account linked · ${shortUid(it)}" } ?: "Participant only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        FormSection(
            "Create an invitation",
            if (availableAccountPlaces > 0) {
                "$availableAccountPlaces of $activeParticipantCount account places available. Share the link or its fallback code."
            } else {
                "The group is full. Add a participant before inviting another account."
            },
        ) {
            Text("Expires after", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3600L to "1 hour", 86400L to "1 day", 604800L to "7 days", 2592000L to "30 days")
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
                label = { Text("Maximum uses") },
                supportingText = { Text("Invitation use limit: 1–20. Group capacity still applies.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onAction(SplitTheBillsAction.CreateInvitation) },
                enabled = state.online && !state.saving && form.invitation == null && availableAccountPlaces > 0,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_invitation"),
            ) { Text("Create invitation") }
        }

        form.invitation?.let { invitation ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Invitation ready", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (invitation.secretAlreadyIssued || invitation.token.isBlank()) {
                        Text("This secret was already displayed. Revoke it and create another invitation if it was not shared.")
                    } else {
                        Text(
                            invitation.code,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text("Code fallback", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        val link = invitationLink(invitation.token)
                        Button(
                            onClick = {
                                val share = Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Join ${group.name} in Split the Bills:\n$link\nCode: ${invitation.code}",
                                    )
                                context.startActivity(Intent.createChooser(share, "Share invitation"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Share invitation") }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Expires ${invitation.expiresAt}", style = MaterialTheme.typography.bodySmall)
                        TextButton(
                            onClick = { onAction(SplitTheBillsAction.RevokeInvitation(invitation.id, invitation.revision)) },
                            enabled = state.online && !state.saving,
                        ) { Text("Revoke") }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        FormSection("Issued invitations", "Only metadata remains after an invitation secret is shown.") {
            when {
                form.invitationsLoading -> CircularProgressIndicator(Modifier.size(28.dp))
                form.invitations.isEmpty() -> Text("No invitations created yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    invitation.revokedAt != null -> "Revoked invitation"
                                    invitation.useCount >= invitation.maxUses -> "Usage limit reached"
                                    else -> "Active invitation"
                                },
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${invitation.useCount}/${invitation.maxUses} uses · expires ${invitation.expiresAt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (revocable) {
                            TextButton(
                                onClick = { onAction(SplitTheBillsAction.RevokeInvitation(invitation.id, invitation.revision)) },
                                enabled = state.online && !state.saving,
                            ) { Text("Revoke") }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        FormSection("Link an account", "After someone joins, confirm which participant represents them.") {
            when {
                unlinkedMembers.isEmpty() -> Text("No member is waiting to be linked.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                unlinkedParticipants.isEmpty() -> Text("No unlinked participant is available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> {
                    Text("Member account", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        unlinkedMembers.forEach { member ->
                            FilterChip(
                                selected = form.selectedMemberUid == member.uid,
                                onClick = { onAction(SplitTheBillsAction.LinkMemberChanged(member.uid)) },
                                label = { Text(shortUid(member.uid)) },
                            )
                        }
                    }
                    Text("Participant", style = MaterialTheme.typography.labelLarge)
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
                        "Confirm the match with the invited person. Names are never used to infer identity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { onAction(SplitTheBillsAction.ApproveParticipantLink) },
                        enabled = state.online && !state.saving && form.selectedMemberUid.isNotBlank() &&
                            form.selectedParticipantId.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Approve account link") }
                }
            }
        }
    }
}

@Composable
private fun ExpenseList(group: GroupDetail, onAction: (SplitTheBillsAction) -> Unit) {
    if (group.expenses.isEmpty() && group.settlements.isEmpty()) {
        EmptyCard("No activity yet", "Add an expense when everyone can see who paid and how it is split.")
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
private fun BalanceList(group: GroupDetail, onAction: (SplitTheBillsAction) -> Unit) {
    val suggestions = remember(group.balances) {
        runCatching {
            RepaymentSuggester.suggest(group.balances.associate { it.participantId to it.amountMinor })
        }
    }

    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("✦", color = MaterialTheme.colorScheme.primary, fontSize = 22.sp)
            Text("Balances are simplified into fewer transfers, so fewer payments change hands.", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    Spacer(Modifier.height(12.dp))
    when {
        suggestions.isFailure -> Text(
            "Suggestions are unavailable because the current balances are inconsistent.",
            color = MaterialTheme.colorScheme.error,
        )
        suggestions.getOrThrow().isEmpty() -> EmptyCard("Everyone is settled", "No repayment is currently needed.")
        else -> DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
            Text("WHO OWES WHOM", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp))
            suggestions.getOrThrow().forEachIndexed { index, suggestion ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Avatar(nameOf(group, suggestion.fromParticipantId), 36.dp)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(format(suggestion.amountMinor, group), color = if (suggestion.toParticipantId == group.currentMemberParticipantId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        Text("- - - - - - →", color = if (suggestion.toParticipantId == group.currentMemberParticipantId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Avatar(nameOf(group, suggestion.toParticipantId), 36.dp)
                    Surface(modifier = Modifier.clickable { onAction(SplitTheBillsAction.OpenSettlement) }, color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) { Text(if (suggestion.toParticipantId == group.currentMemberParticipantId) "Record" else "Settle", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)) }
                }
            }
        }
    }
    if (group.settlements.isNotEmpty()) {
        SectionHeader("Payment history", null, {}, Modifier.padding(top = 20.dp, bottom = 8.dp))
        DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
            group.settlements.forEachIndexed { index, settlement ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Avatar(nameOf(group, settlement.fromParticipantId), 38.dp)
                    Column(Modifier.weight(1f)) { Text("${nameOf(group, settlement.fromParticipantId)} paid ${nameOf(group, settlement.toParticipantId)}", fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(settlement.date, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }
                    Text(format(settlement.amountMinor, group), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun ActivityList(group: GroupDetail) {
    if (group.activity.isEmpty()) EmptyCard("Quiet for now", "Server-confirmed changes will appear here.")
    group.activity.forEach { event ->
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(modifier = Modifier.padding(top = 5.dp).size(10.dp), color = MaterialTheme.colorScheme.primary, shape = CircleShape) {}
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(event.type.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase), fontWeight = FontWeight.SemiBold)
                Text(event.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    buildString {
                        append(formatActivityTime(event.occurredAtEpochMillis))
                        event.actorUid?.let { append(" · account ${shortUid(it)}") }
                        if (event.resourceRevision > 0) append(" · revision ${event.resourceRevision}")
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
    val group = state.selectedGroup ?: return
    val editor = state.expenseEditor
    val selectedCount = editor.selectedParticipantIds.size.coerceAtLeast(1)
    val amountMinor = runCatching {
        MoneyText.parseUnsignedMinor(editor.amount, CurrencyUnit(group.currency, group.currencyExponent))
    }.getOrDefault(0L)
    Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 88.dp)) {
            Surface(Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.surface, shape = CircleShape, shadowElevation = 0.5.dp) {
                Row(Modifier.padding(horizontal = 7.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GroupTile(GroupSummary(group.id, group.name, group.currency, "", group.currentMemberParticipantId, group.revision, group.archived), 30.dp)
                    Text(group.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("⌄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(
                value = editor.amount,
                onValueChange = { onAction(SplitTheBillsAction.ExpenseAmountChanged(it)) },
                prefix = { Text(currencySymbol(group.currency), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 30.sp, fontWeight = FontWeight.Bold) },
                textStyle = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp).testTag("expense_amount"),
            )
            OutlinedTextField(
                value = editor.title,
                onValueChange = { onAction(SplitTheBillsAction.ExpenseTitleChanged(it)) },
                placeholder = { Text("What was it for?") },
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("expense_title"),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("FOOD", "GROCERIES", "TRANSPORT", "LODGING", "ENTERTAINMENT", "OTHER").forEach { category ->
                    val selected = editor.category == category
                    Surface(
                        modifier = Modifier.width(76.dp).height(72.dp).clickable { onAction(SplitTheBillsAction.ExpenseCategoryChanged(category)) },
                        color = if (selected) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface,
                        contentColor = if (selected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = if (selected) 0.dp else 0.5.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(categoryIcon(category), null, Modifier.size(22.dp)); Text(category.lowercase().replaceFirstChar(Char::uppercase), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
                Text("Paid by", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(start = 12.dp, top = 10.dp))
                ParticipantChips(group.participants, editor.payerParticipantId) { onAction(SplitTheBillsAction.ExpensePayerChanged(it)) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 18.dp), verticalAlignment = Alignment.Bottom) {
                Text("Split", fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${editor.selectedParticipantIds.size} of ${group.participants.count(Participant::active)} people", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            ExpenseSplitSegmented(editor.splitMode) { onAction(SplitTheBillsAction.ExpenseModeChanged(it)) }
            Surface(Modifier.fillMaxWidth().height(9.dp).padding(horizontal = 2.dp, vertical = 1.dp), color = MaterialTheme.colorScheme.surfaceContainer, shape = CircleShape) {
                Row { repeat(selectedCount) { index -> Surface(Modifier.weight(1f).fillMaxHeight(), color = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.outline)[index % 4]) {} } }
            }
            Text(if (editor.splitMode == SplitMode.EQUAL) "${formatMinor(amountMinor / selectedCount, group)} each" else "Allocated", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 2.dp, vertical = 5.dp))
            DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
                group.participants.filter(Participant::active).forEachIndexed { index, participant ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                    val selected = participant.id in editor.selectedParticipantIds
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = selected, onCheckedChange = { onAction(SplitTheBillsAction.ExpenseParticipantToggled(participant.id)) })
                        Avatar(participant.displayName, 34.dp)
                        Text(if (participant.id == group.currentMemberParticipantId) "You" else participant.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 9.dp).weight(1f))
                        if (editor.splitMode == SplitMode.EQUAL) {
                            Text(formatMinor(if (selected) amountMinor / selectedCount else 0, group), color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        } else {
                            OutlinedTextField(
                                value = editor.entryValues[participant.id].orEmpty(),
                                onValueChange = { onAction(SplitTheBillsAction.ExpenseEntryValueChanged(participant.id, it)) },
                                enabled = selected,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(96.dp),
                                suffix = { Text(if (editor.splitMode == SplitMode.PERCENTAGE) "%" else group.currency, fontSize = 10.sp) },
                            )
                        }
                    }
                }
            }
            state.draftNotice?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)) }
        }
        Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = MaterialTheme.colorScheme.background, shadowElevation = 1.dp) {
            Button(
                onClick = { onAction(SplitTheBillsAction.SubmitExpense) },
                enabled = !state.saving && state.online,
                modifier = Modifier.fillMaxWidth().padding(12.dp).height(56.dp).testTag("expense_submit"),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Filled.Check, null, Modifier.size(21.dp)); Spacer(Modifier.width(8.dp)); Text(if (editor.expenseId == null) "Save expense · ${formatMinor(amountMinor, group)}" else "Save changes", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun ExpenseSplitSegmented(mode: SplitMode, onSelected: (SplitMode) -> Unit) {
    val options = listOf(SplitMode.EQUAL to "Equally", SplitMode.PERCENTAGE to "Percent", SplitMode.EXACT to "Custom")
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer, shape = CircleShape) {
        Row(Modifier.padding(4.dp)) {
            options.forEach { (value, label) ->
                Surface(Modifier.weight(1f).height(36.dp).clickable { onSelected(value) }, color = if (mode == value) MaterialTheme.colorScheme.surface else Color.Transparent, shape = CircleShape, shadowElevation = if (mode == value) 0.5.dp else 0.dp) {
                    Box(contentAlignment = Alignment.Center) { Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (mode == value) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

private fun currencySymbol(currency: String): String =
    runCatching { java.util.Currency.getInstance(currency).symbol }.getOrDefault(currency)

@Composable
private fun ExpenseDetailScreen(state: SplitTheBillsUiState, padding: PaddingValues, onAction: (SplitTheBillsAction) -> Unit) {
    val group = state.selectedGroup ?: return
    val expense = group.expenses.firstOrNull { it.id == state.selectedExpenseId }
    if (expense == null) {
        Page(padding) { EmptyCard("Expense unavailable", "It may have been removed on another device.") }
        return
    }
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
                    Text("Total expense", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    StatusPill(expense.category.lowercase().replaceFirstChar(Char::uppercase), MaterialTheme.colorScheme.surfaceContainer)
                }
                Text(
                    format(expense.amountMinor, group),
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
                            Text(when { group.archived -> "Archived"; group.fromCache -> "Cached copy"; else -> "Saved" }, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                ExpenseParticipants(expense, group, Modifier.padding(top = 12.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("Paid by", null, {})
                DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                    expense.contributions.forEachIndexed { index, contribution ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ExpensePersonRow(
                            name = nameOf(group, contribution.participantId),
                            caption = if (contribution.participantId == group.currentMemberParticipantId) "Paid by you" else "Paid toward this expense",
                            amount = format(contribution.amountMinor, group),
                            accent = contribution.participantId == group.currentMemberParticipantId,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Confirmed shares", fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).semantics { heading() })
                    StatusPill("${expense.splitMode.label()} split", MaterialTheme.colorScheme.surfaceContainer)
                }
                DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                    expense.allocations.forEachIndexed { index, allocation ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ExpensePersonRow(
                            name = nameOf(group, allocation.participantId),
                            caption = if (allocation.participantId == group.currentMemberParticipantId) "Your share" else "Share of expense",
                            amount = format(allocation.amountMinor, group),
                            accent = allocation.participantId == group.currentMemberParticipantId,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Total shares", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Text(format(expense.allocations.sumOf { it.amountMinor }, group), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            expense.note?.takeIf(String::isNotBlank)?.let { note ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader("Note", null, {})
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
                        enabled = !group.archived && !state.saving,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("expense_detail_edit"),
                    ) {
                        Icon(Icons.Outlined.Edit, null, Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.online) "Edit" else "Edit draft", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onAction(SplitTheBillsAction.DeleteSelectedExpense) },
                        enabled = state.online && !group.archived && !state.saving,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("expense_detail_remove"),
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Remove", fontWeight = FontWeight.Bold)
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
    val group = state.selectedGroup ?: return
    val form = state.settlementEditor
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(group.name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.padding(vertical = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Avatar(nameOf(group, form.fromParticipantId), 58.dp); Text(nameOf(group, form.fromParticipantId), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp)) }
            Surface(Modifier.size(44.dp), color = MaterialTheme.colorScheme.surface, shape = CircleShape) { Box(contentAlignment = Alignment.Center) { Text("→", fontSize = 22.sp) } }
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Avatar(nameOf(group, form.toParticipantId), 58.dp); Text(nameOf(group, form.toParticipantId), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp)) }
        }
        OutlinedTextField(
            form.amount,
            { onAction(SplitTheBillsAction.SettlementAmountChanged(it)) },
            prefix = { Text(currencySymbol(group.currency), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 24.sp) },
            textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        DesignCard(Modifier.fillMaxWidth().padding(top = 18.dp), contentPadding = PaddingValues(14.dp)) {
            Text("Paid by", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            ParticipantChips(group.participants, form.fromParticipantId) { onAction(SplitTheBillsAction.SettlementFromChanged(it)) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 8.dp))
            Text("Paid to", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            ParticipantChips(group.participants, form.toParticipantId) { onAction(SplitTheBillsAction.SettlementToChanged(it)) }
        }
        OperationDateField(
            date = form.date,
            onDateSelected = { onAction(SplitTheBillsAction.SettlementDateChanged(it)) },
            modifier = Modifier.padding(top = 14.dp).testTag("settlement_date"),
        )
        OutlinedTextField(form.note, { onAction(SplitTheBillsAction.SettlementNoteChanged(it)) }, label = { Text("Note (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(16.dp))
        Text("This records money already paid outside Split the Bills. The app does not transfer funds.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
        Button(onClick = { onAction(SplitTheBillsAction.SubmitSettlement) }, enabled = !state.saving && state.online, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Filled.Check, null, Modifier.size(20.dp)); Spacer(Modifier.width(7.dp)); Text("Mark as settled", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SettingsScreen(state: SplitTheBillsUiState, padding: PaddingValues, onAction: (SplitTheBillsAction) -> Unit) {
    var editingName by rememberSaveable { mutableStateOf(false) }
    val profileDisplayName = state.user?.displayName?.takeIf(String::isNotBlank)
        ?: state.profileName.takeIf(String::isNotBlank)
        ?: state.user?.email?.substringBefore('@')
        ?: "You"
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Profile", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Outlined.MoreVert, contentDescription = "Profile options") }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(profileDisplayName, 88.dp)
            Text(profileDisplayName, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp))
            Text(state.user?.email ?: "Signed in", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileMetric(state.groups.size.toString(), "Groups", Modifier.weight(1f))
                ProfileMetric(state.selectedGroup?.participants?.size?.toString() ?: "0", "Friends", Modifier.weight(1f))
                ProfileMetric(state.selectedGroup?.settlements?.size?.toString() ?: "0", "Settled", Modifier.weight(1f))
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
                ProfileActionRow("Display name", profileDisplayName, onClick = { editingName = !editingName }) {
                    if (editingName) {
                        OutlinedTextField(state.profileName, { onAction(SplitTheBillsAction.ProfileNameChanged(it)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        Button(onClick = { onAction(SplitTheBillsAction.SaveProfile); editingName = false }, enabled = !state.saving && state.online && state.profileName.isNotBlank(), modifier = Modifier.padding(top = 8.dp)) { Text("Save profile") }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ProfileActionRow("Default currency", state.selectedGroup?.currency ?: "USD · US Dollar")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ProfileActionRow("Friends", "${state.selectedGroup?.participants?.size ?: 0} people")
            }
            DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
                Text("APPEARANCE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePreference.entries.forEach { preference ->
                        DesignChip(preference.name.lowercase().replaceFirstChar(Char::uppercase), state.themePreference == preference) { onAction(SplitTheBillsAction.ThemeChanged(preference)) }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), modifier = Modifier.padding(top = 8.dp))
                ProfileActionRow("Simplify debts", "Minimise transfers in groups")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ProfileActionRow("Biometric lock", "Secure this app")
            }
            DesignCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
                ProfileActionRow("Help & support", "Get answers and contact us")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Row(Modifier.fillMaxWidth().clickable { onAction(SplitTheBillsAction.SignOut) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text("Sign out", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                TextButton(onClick = { onAction(SplitTheBillsAction.OpenAccountDeletion) }, enabled = !state.saving && state.online, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary), modifier = Modifier.fillMaxWidth()) { Text("Delete account") }
            }
        }
    }
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
    val deletion = state.accountDeletion
    if (!deletion.open) return
    val ownedGroups = state.groups.filter { it.role == "OWNER" }
    AlertDialog(
        onDismissRequest = { if (!state.saving) onAction(SplitTheBillsAction.CloseAccountDeletion) },
        title = { Text("Delete your account?") },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("This cannot be undone. Memberships are revoked, profile data is removed, local drafts are erased, and retained participant history is anonymized.")
                if (deletion.loadingCandidates) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                        Text("Loading ownership choices…")
                    }
                } else {
                    ownedGroups.forEach { group ->
                        Text("New owner for ${group.name}", fontWeight = FontWeight.SemiBold)
                        val candidates = deletion.candidates[group.id].orEmpty()
                        if (candidates.isEmpty()) {
                            Text(
                                "No other member is available. Invite someone to this group before deleting your account.",
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
                                        label = { Text(candidate.displayName ?: "Member ${candidate.uid.take(8)}") },
                                    )
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = deletion.confirmation,
                    onValueChange = { onAction(SplitTheBillsAction.AccountDeletionConfirmationChanged(it)) },
                    label = { Text("Type DELETE to confirm") },
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
            ) { Text("Delete permanently") }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(SplitTheBillsAction.CloseAccountDeletion) },
                enabled = !state.saving,
            ) { Text("Cancel") }
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
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = { onAction(SplitTheBillsAction.ErrorShown) },
            title = { Text("Couldn’t complete that") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = { onAction(SplitTheBillsAction.ErrorShown) }) { Text("OK") } },
        )
    }
    state.conflict?.let { conflict ->
        AlertDialog(
            onDismissRequest = { onAction(SplitTheBillsAction.ConflictDismissed) },
            title = { Text("Newer version found") },
            text = { Text(conflict) },
            confirmButton = {
                TextButton(onClick = { onAction(SplitTheBillsAction.ConflictReloaded) }) {
                    Text(
                        when (state.conflictTarget) {
                            ConflictTarget.INVITATION -> "Refresh invitation"
                            ConflictTarget.GROUP -> "Reload group"
                            else -> "Reload server version"
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(SplitTheBillsAction.ConflictDismissed) }) { Text("Keep draft") }
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
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text("Offline · cached data remains available; shared changes are disabled", modifier = Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun SavingOverlay() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), shape = RoundedCornerShape(18.dp), tonalElevation = 8.dp) {
            Row(Modifier.padding(22.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(28.dp))
                Text("Waiting for server confirmation…")
            }
        }
    }
}

@Composable
private fun MoneyText(amountMinor: Long, group: GroupDetail) {
    Text(format(amountMinor, group), fontWeight = FontWeight.Bold)
}

private fun nameOf(group: GroupDetail, participantId: String): String =
    group.participants.firstOrNull { it.id == participantId }?.displayName ?: "Unknown participant"

private fun format(amountMinor: Long, group: GroupDetail): String =
    "${group.currency} ${MoneyText.formatMinor(amountMinor, CurrencyUnit(group.currency, group.currencyExponent))}"

private fun invitationLink(token: String): String =
    "https://links.example.invalid/invite?token=${Uri.encode(token)}"

private fun shortUid(uid: String): String = if (uid.length <= 10) uid else "${uid.take(6)}…${uid.takeLast(4)}"

private fun formatActivityTime(epochMillis: Long): String = if (epochMillis <= 0L) {
    "Time unavailable"
} else {
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))
}

private fun SplitMode.label(): String = when (this) {
    SplitMode.EQUAL -> "Equal"
    SplitMode.EXACT -> "Exact"
    SplitMode.PERCENTAGE -> "Percentage"
    SplitMode.WEIGHTED -> "Shares"
}

private fun SplitMode.entryLabel(currency: String): String = when (this) {
    SplitMode.EXACT -> currency
    SplitMode.PERCENTAGE -> "basis points (10000 total)"
    SplitMode.WEIGHTED -> "shares"
    SplitMode.EQUAL -> "equal"
}

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
