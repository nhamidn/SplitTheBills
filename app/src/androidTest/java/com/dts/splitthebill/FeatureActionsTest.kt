package com.dts.splitthebill

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.dts.splitthebill.domain.model.*
import com.dts.splitthebill.ui.*
import com.dts.splitthebill.ui.theme.SplitTheBillsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class FeatureActionsTest {
    @get:Rule val compose = createComposeRule()
    private var state by mutableStateOf(fixture())

    private fun showApp(context: Context? = null) {
        compose.setContent {
            CompositionLocalProvider(LocalContext provides (context ?: LocalContext.current)) {
              SplitTheBillsTheme {
                SplitTheBillsApp(state, onAction = { action ->
                    state = when (action) {
                        is SplitTheBillsAction.Navigate -> state.copy(mainDestination = action.destination,
                            screen = if (action.destination == MainDestination.PROFILE) AppScreen.SETTINGS else AppScreen.GROUPS)
                        is SplitTheBillsAction.OpenFeature -> state.copy(screen = action.screen, searchScope = action.scope)
                        SplitTheBillsAction.OpenGroups -> state.copy(screen = AppScreen.GROUPS, mainDestination = MainDestination.GROUPS)
                        SplitTheBillsAction.OpenSettings -> state.copy(screen = AppScreen.SETTINGS, mainDestination = MainDestination.PROFILE)
                        is SplitTheBillsAction.OpenGroupExpense -> state.copy(screen = AppScreen.EXPENSE_DETAIL,
                            selectedGroup = state.groupDetails[action.groupId], selectedExpenseId = action.expenseId)
                        is SplitTheBillsAction.DefaultCurrencyChanged -> state.copy(preferences = state.preferences.copy(defaultCurrency = action.value))
                        is SplitTheBillsAction.SimplifyDebtsChanged -> state.copy(preferences = state.preferences.copy(simplifyDebts = action.value))
                        is SplitTheBillsAction.AddExpenseToGroup -> state.copy(screen = AppScreen.EXPENSE_EDITOR,
                            selectedGroup = state.groupDetails[action.groupId], expenseEditor = ExpenseEditorState(selectedParticipantIds = setOf("p1", "p2")))
                        is SplitTheBillsAction.OpenGroupSettlement -> state.copy(screen = AppScreen.SETTLEMENT_DETAIL,
                            selectedGroup = state.groupDetails[action.groupId], selectedSettlementId = action.settlementId)
                        SplitTheBillsAction.MarkNotificationsRead -> state.copy(preferences = state.preferences.copy(notificationReadAt = mapOf("alice" to 1000L)))
                        else -> state
                    }
                }, onGoogleSignIn = {})
              }
            }
        }
    }

    @Test fun remindOpensAReviewableMessageWithTheLiveAmount() {
        showApp()
        compose.onNodeWithText("Remind").performClick()
        compose.onNodeWithTag("reminder_trip_p2").performScrollTo().performClick()
        compose.onNodeWithText("Review reminder").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertTextContains("you owe Alice EUR 5.00", substring = true)
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Review reminder").assertDoesNotExist()
    }

    @Test fun reminderSharingUsesTheReviewedMessageAndTheAndroidChooser() {
        val intents = mutableListOf<Intent>()
        val context = object : ContextWrapper(androidx.test.core.app.ApplicationProvider.getApplicationContext()) {
            override fun startActivity(intent: Intent) { intents += intent }
        }
        state = state.copy(screen = AppScreen.REMINDERS)
        showApp(context)
        compose.onNodeWithTag("reminder_trip_p2").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Hello Bob, please check our shared lunch.")
        compose.onNodeWithText("Share reminder").performClick()
        compose.runOnIdle {
            assertEquals(Intent.ACTION_CHOOSER, intents.single().action)
            @Suppress("DEPRECATION")
            val send = intents.single().getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
            assertEquals(Intent.ACTION_SEND, send.action)
            assertEquals("text/plain", send.type)
            assertEquals("Hello Bob, please check our shared lunch.", send.getStringExtra(Intent.EXTRA_TEXT))
        }
    }

    @Test fun aChangedBalanceInvalidatesTheOpenReminder() {
        state = state.copy(screen = AppScreen.REMINDERS)
        showApp()
        compose.onNodeWithTag("reminder_trip_p2").performScrollTo().performClick()
        compose.runOnIdle {
            val group = state.groupDetails.getValue("trip").copy(expenses = emptyList(),
                balances = listOf(ParticipantBalance("p1", 0), ParticipantBalance("p2", 0)))
            state = state.copy(groupDetails = state.groupDetails + (group.id to group))
        }
        compose.onNodeWithText("Share reminder").assertIsNotEnabled()
        compose.onNodeWithText("This balance changed.", substring = true).assertIsDisplayed()
    }

    @Test fun searchFindsAndOpensAnExpenseFromAnotherGroup() {
        showApp()
        compose.onNodeWithContentDescription("Search").performClick()
        compose.onNodeWithTag("global_search").performTextInput("Train tickets")
        compose.onNode(hasText("Train tickets") and !hasSetTextAction()).performClick()
        compose.runOnIdle {
            assertEquals("home", state.selectedGroup?.id)
            assertEquals(AppScreen.EXPENSE_DETAIL, state.screen)
        }
    }

    @Test fun groupTypeFiltersActuallyChangeTheVisibleGroups() {
        showApp()
        compose.onNodeWithContentDescription("Groups").performClick()
        compose.onNodeWithText("Trip").performClick()
        compose.onNodeWithText("Trip group").assertExists()
        compose.onNodeWithText("Home group").assertDoesNotExist()
    }

    @Test fun profileCurrencyAndDebtPreferenceHaveWorkingControls() {
        showApp()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.onNodeWithText("Default currency").performScrollTo().performClick()
        compose.onNodeWithTag("group_currency").performClick()
        compose.onNodeWithTag("currency_search").performTextInput("KWD")
        compose.onNodeWithTag("currency_KWD").performClick()
        compose.runOnIdle { assertEquals("KWD", state.preferences.defaultCurrency) }
        compose.onNodeWithTag("simplify_debts").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(false, state.preferences.simplifyDebts) }
        compose.onNodeWithText("Help & support").performScrollTo().performClick()
        compose.onNodeWithText("How do reminders work?").performScrollTo().performClick()
        compose.onNodeWithText("Remind shows current amounts owed to you.", substring = true).assertIsDisplayed()
    }

    @Test fun addExpenseChoosesAGroupAndOpensTheForm() {
        showApp()
        compose.onNodeWithText("Add expense").performClick()
        compose.onNodeWithText("Home group").performClick()
        compose.onNodeWithTag("expense_title").assertExists()
        compose.onNodeWithText("Shares").performScrollTo().assertExists()
        compose.onNodeWithTag("expense_note").performScrollTo().assertExists()
        compose.runOnIdle { assertEquals("home", state.selectedGroup?.id) }
    }

    @Test fun notificationsOpenRepaymentsAndCanBeMarkedRead() {
        val trip = state.groupDetails.getValue("trip").copy(
            settlements = listOf(Settlement("s1", "p2", "p1", 200, "EUR", "2026-11-16", "Train fare", "bob", 1)),
            activity = listOf(ActivityEvent("a1", "SETTLEMENT_CREATED", "SETTLEMENT", "Repayment recorded", "bob", 1, 1000L, "s1")))
        state = state.copy(screen = AppScreen.NOTIFICATIONS, groupDetails = state.groupDetails + (trip.id to trip))
        showApp()
        compose.onNodeWithText("Mark all as read").performClick()
        compose.onNodeWithText("Mark all as read").assertIsNotEnabled()
        compose.onNodeWithText("Repayment recorded", substring = true).performClick()
        compose.onNodeWithTag("settlement_detail_total").assertTextEquals("EUR 2.00")
        compose.onNodeWithText("Train fare").performScrollTo().assertIsDisplayed()
    }

    @Test fun expenseAndRepaymentActionsRespectCreatorPermissions() {
        val trip = state.groupDetails.getValue("trip").copy(ownerUid = "bob",
            settlements = listOf(Settlement("s1", "p2", "p1", 200, "EUR", "2026-11-16", null, "bob", 1)))
        state = state.copy(screen = AppScreen.SETTLEMENT_DETAIL, selectedGroup = trip, selectedSettlementId = "s1")
        showApp()
        compose.onNodeWithTag("settlement_edit").assertIsNotEnabled()
        compose.onNodeWithTag("settlement_remove").assertIsNotEnabled()
        compose.runOnIdle { assertTrue(state.user?.uid != trip.ownerUid) }
    }

    private fun fixture(): SplitTheBillsUiState {
        val trip = detail("trip", "Trip group", GroupType.TRIP, "Lunch")
        val home = detail("home", "Home group", GroupType.HOME, "Train tickets").copy(
            balances = listOf(ParticipantBalance("p1", 0), ParticipantBalance("p2", 0)))
        return SplitTheBillsUiState(authResolved = true, preferencesResolved = true,
            user = SignedInUser("alice", "alice@example.test", "Alice"), screen = AppScreen.GROUPS,
            groups = listOf(trip, home).map { GroupSummary(it.id, it.name, it.currency, "OWNER", "p1", 1, false, it.type) },
            groupDetails = mapOf(trip.id to trip, home.id to home))
    }

    private fun detail(id: String, name: String, type: GroupType, title: String) = GroupDetail(
        id, name, "EUR", 2, "alice", 1, false, "p1",
        listOf(GroupMember("alice", "OWNER", "p1"), GroupMember("bob", "MEMBER", "p2")),
        listOf(Participant("p1", "Alice", "alice", true), Participant("p2", "Bob", "bob", true)),
        listOf(Expense("e1", title, 1000, "EUR", listOf(Contribution("p1", 1000)),
            listOf(Allocation("p1", 500), Allocation("p2", 500)), SplitMode.EQUAL,
            listOf(SplitEntryInput("p1"), SplitEntryInput("p2")), "2026-11-16", "OTHER", null, "alice", 1)),
        emptyList(), listOf(ParticipantBalance("p1", 500), ParticipantBalance("p2", -500)), emptyList(), false, type,
    )
}
