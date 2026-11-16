package com.dts.splitthebill

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.dts.splitthebill.domain.model.*
import com.dts.splitthebill.ui.*
import com.dts.splitthebill.ui.theme.SplitTheBillsTheme
import java.io.File
import org.junit.Rule
import org.junit.Test

/** Exercise scrolling and access to the styled pages' actions; export renders for visual review. */
@OptIn(ExperimentalTestApi::class)
class FeatureLayoutsTest {
    @get:Rule val compose = createComposeRule()
    private var state by mutableStateOf(fixture(AppScreen.REMINDERS))
    private var dark by mutableStateOf(false)
    private var fontScale by mutableFloatStateOf(1f)
    private var pageKey by mutableStateOf("")

    private fun showApp() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                key(pageKey) {
                    SplitTheBillsTheme(darkTheme = dark) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            Box(Modifier.widthIn(max = 360.dp).fillMaxSize()) {
                                SplitTheBillsApp(state, onAction = {}, onGoogleSignIn = {})
                            }
                        }
                    }
                }
            }
        }
    }

    @Test fun featureActionsRemainReachableInLightAndDarkThemes() {
        showApp()
        listOf(false, true).forEach { darkTheme ->
            pages.forEach { screen ->
                render(screen, darkTheme)
                capture("${screen.name.lowercase()}-${if (darkTheme) "dark" else "light"}")
                assertActionReachable(screen)
            }
            render(AppScreen.REMINDERS, darkTheme, empty = true)
            capture("reminders-empty-${if (darkTheme) "dark" else "light"}")
            compose.onNodeWithText("View your groups").performScrollTo().assertIsDisplayed()
            render(AppScreen.GROUPS, darkTheme)
            compose.onNodeWithTag("settlement_participants_s1", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            compose.onNode(
                hasScrollAction() and hasAnyDescendant(hasTestTag("settlement_participants_s1")),
                useUnmergedTree = true,
            ).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 10_000f) }
            capture("home-transactions-${if (darkTheme) "dark" else "light"}")
        }
    }

    @Test fun featureActionsRemainReachableAtTwoHundredPercentFontSize() {
        showApp()
        pages.forEach { screen ->
            render(screen, darkTheme = false, scale = 2f)
            capture("${screen.name.lowercase()}-large-text")
            assertActionReachable(screen)
        }
        render(AppScreen.REMINDERS, darkTheme = false, scale = 2f)
        compose.onNodeWithTag("reminder_trip_p2").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_message").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Share reminder").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        render(AppScreen.REMINDERS, darkTheme = false, scale = 2f, empty = true)
        compose.onNodeWithText("View your groups").performScrollTo().assertIsDisplayed()
        capture("reminders-empty-large-text-action")
    }

    private fun assertActionReachable(screen: AppScreen) {
        val node = when (screen) {
            AppScreen.SEARCH -> compose.onNodeWithTag("global_search")
            AppScreen.NOTIFICATIONS -> compose.onNodeWithText("Mark all as read")
            AppScreen.REMINDERS -> compose.onNodeWithTag("reminder_trip_p2")
            AppScreen.SETTLEMENT_PICKER -> compose.onNodeWithText("Bob → Alice")
            AppScreen.FRIENDS -> compose.onNodeWithText("Open your groups")
            AppScreen.HELP -> compose.onNodeWithText("Report an issue")
            AppScreen.GROUP_OPTIONS -> compose.onNodeWithText("Save group")
            AppScreen.EXPENSE_GROUP_PICKER -> compose.onNodeWithText("Weekend in Lisbon")
            AppScreen.SETTLEMENT_DETAIL -> compose.onNodeWithTag("settlement_edit")
            else -> error("Not a feature page")
        }
        node.performScrollTo().assertIsDisplayed()
    }

    private fun render(screen: AppScreen, darkTheme: Boolean, scale: Float = 1f, empty: Boolean = false) {
        compose.runOnIdle {
            state = fixture(screen, empty)
            dark = darkTheme
            fontScale = scale
            pageKey = "$screen/$darkTheme/$scale/$empty"
        }
    }

    private fun capture(name: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val folder = requireNotNull(context.getExternalFilesDir("feature-previews")).apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val pages = listOf(AppScreen.REMINDERS, AppScreen.SEARCH, AppScreen.NOTIFICATIONS,
        AppScreen.SETTLEMENT_PICKER, AppScreen.FRIENDS, AppScreen.HELP, AppScreen.GROUP_OPTIONS,
        AppScreen.EXPENSE_GROUP_PICKER, AppScreen.SETTLEMENT_DETAIL)

    private fun fixture(screen: AppScreen, empty: Boolean = false): SplitTheBillsUiState {
        val group = GroupDetail("trip", "Weekend in Lisbon", "EUR", 2, "alice", 3, false, "p1",
            listOf(GroupMember("alice", "OWNER", "p1"), GroupMember("bob", "MEMBER", "p2")),
            listOf(Participant("p1", "Alice", "alice", true), Participant("p2", "Bob", "bob", true)),
            listOf(Expense("e1", "Train tickets", 2600, "EUR", listOf(Contribution("p1", 2600)),
                listOf(Allocation("p1", 1300), Allocation("p2", 1300)), SplitMode.EQUAL,
                listOf(SplitEntryInput("p1"), SplitEntryInput("p2")), "2026-11-16", "TRANSPORT", "Two tickets to the coast", "alice", 1)),
            listOf(Settlement("s1", "p2", "p1", 400, "EUR", "2026-11-16", "Part of our train fare", "bob", 1)),
            listOf(ParticipantBalance("p1", 900), ParticipantBalance("p2", -900)),
            listOf(ActivityEvent("a1", "SETTLEMENT_CREATED", "SETTLEMENT", "Bob recorded a repayment", "bob", 1, System.currentTimeMillis(), "s1")), false, GroupType.TRIP)
        return SplitTheBillsUiState(authResolved = true, preferencesResolved = true, user = SignedInUser("alice", "alice@example.test", "Alice"),
            screen = screen, groups = if (empty) emptyList() else listOf(GroupSummary(group.id, group.name, group.currency, "OWNER", "p1", 3, false, group.type)),
            groupDetails = if (empty) emptyMap() else mapOf(group.id to group), selectedGroup = group.takeUnless { empty },
            selectedGroupId = group.id.takeUnless { empty }, selectedSettlementId = "s1")
    }
}
