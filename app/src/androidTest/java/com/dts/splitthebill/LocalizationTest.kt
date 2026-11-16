package com.dts.splitthebill

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.LocaleList
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.dts.splitthebill.domain.RepaymentSuggestion
import com.dts.splitthebill.domain.model.*
import com.dts.splitthebill.ui.*
import com.dts.splitthebill.ui.theme.SplitTheBillsTheme
import java.io.File
import java.util.Locale
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Resource overrides keep these checks independent of the user's saved app language. */
@OptIn(ExperimentalTestApi::class)
class LocalizationTest {
    @get:Rule val compose = createComposeRule()
    private var language by mutableStateOf("fr")
    private var state by mutableStateOf(fixture(AppScreen.GROUPS))
    private var dark by mutableStateOf(false)
    private var scale by mutableFloatStateOf(1f)
    private var pageKey by mutableIntStateOf(0)
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun localized(tag: String): Context = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocales(LocaleList(Locale.forLanguageTag(tag))) },
    )

    private fun showApp() {
        compose.setContent {
            val selected = remember(language) { localized(language) }
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalContext provides selected, LocalConfiguration provides selected.resources.configuration,
                LocalDensity provides Density(density, scale)) {
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

    @Test fun frenchAndSpanishCoverAllScreensWithoutChangingUserContent() {
        showApp()
        for (tag in listOf("fr", "es")) {
            for (screen in pages) {
                render(screen, tag)
                assertReachable(anchor(screen, tag))
                if (screen in previews) capturePage("${screen.name.lowercase()}-$tag-light")
            }
            render(AppScreen.AUTH, tag)
            compose.onAllNodesWithText(if (tag == "fr") "Se connecter" else "Iniciar sesión", useUnmergedTree = true).assertCountEquals(2)
            render(AppScreen.SETTINGS, tag)
            compose.onNodeWithText(localized(tag).getString(R.string.language)).performScrollTo().performClick()
            for (choice in listOf("system", "en", "fr", "es")) compose.onNodeWithTag("language_$choice").assertIsDisplayed()
            compose.onNodeWithText(localized(tag).getString(R.string.cancel)).performClick()
            for (tab in GroupTab.entries) {
                render(AppScreen.GROUP_DETAIL, tag, tab = tab)
                compose.onNodeWithText("Lisbon weekend").assertExists()
            }
            render(AppScreen.SEARCH, tag)
            compose.onNodeWithTag("global_search").performTextInput(if (tag == "fr") "rembours" else "reembolso")
            // A translated filter must keep the same stable route token.
            compose.onNodeWithText(localized(tag).getString(R.string.payments)).performClick()
            compose.onNodeWithTag("global_search").performTextClearance()
            compose.onNodeWithText(if (tag == "fr") "Bob a payé Alice" else "Bob pagó a Alice").performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun localizedRemindersErrorsPluralsAndMoneyUseTheSelectedLanguage() {
        showApp()
        val retainedMessage = UiText(R.string.percentages_must_add_up_to_exactly_100)
        for (tag in listOf("fr", "es")) {
            val strings = UiStrings(localized(tag))
            assertEquals("EUR 12,34", strings.money(1234, "EUR", 2))
            assertTrue(strings.money(123456789, "EUR", 2).endsWith(",89"))
            assertEquals("0", strings.plural(R.plurals.people_count, 0).substringBefore(' '))
            assertTrue(strings.plural(R.plurals.people_count, 1).startsWith("1 "))
            assertTrue(strings.plural(R.plurals.people_count, 2).startsWith("2 "))
            assertTrue(strings.get(R.string.enter_percentages_that_add_up_to_100_up_to_two_decimal_places).contains("100"))
            assertNotEquals(retainedMessage.resolve(localized("en")), retainedMessage.resolve(localized(tag)))
            val reminder = strings.reminder(Reminder(group(), RepaymentSuggestion("p2", "p1", 900)))
            assertTrue(reminder.startsWith(if (tag == "fr") "Bonjour Bob" else "Hola Bob"))
            assertTrue(reminder.contains("EUR 9,00"))
            assertTrue(reminder.contains("Lisbon weekend"))
            assertTrue(strings.date("2026-11-16").contains("2026"))
            val historical = ActivityEvent("old", "EXPENSE_UPDATED", "EXPENSE", "Original title", "bob", 1, 1000L,
                resourceId = "e1", subject = "Original title")
            assertTrue(strings.activitySummary(historical, group()).endsWith("Original title"))
            assertFalse(strings.activitySummary(historical, group()).contains("Train tickets"))
            render(AppScreen.REMINDERS, tag)
            compose.onNodeWithTag("reminder_trip_p2").performScrollTo().performClick()
            compose.onNodeWithTag("reminder_message").assertTextContains(reminder, substring = true)
            compose.onNodeWithText(strings.get(R.string.cancel)).performClick()
            render(AppScreen.AUTH, tag, error = retainedMessage)
            compose.onNodeWithText(retainedMessage.resolve(localized(tag))).assertIsDisplayed()
        }
    }

    @Test fun translationsRemainReachableInDarkThemeAndAtLargeFontSizes() {
        showApp()
        for (tag in listOf("fr", "es")) {
            for ((night, fontScale) in listOf(true to 1f, false to 2f)) {
                for (screen in pages) {
                    render(screen, tag, night = night, fontScale = fontScale)
                    assertReachable(anchor(screen, tag))
                    if (screen == AppScreen.GROUPS) {
                        compose.onNode(hasScrollAction() and hasAnyDescendant(hasTestTag("settlement_participants_s1")), useUnmergedTree = true)
                            .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 10_000f) }
                    }
                    if (screen in previews) capturePage("${screen.name.lowercase()}-$tag-${if (night) "dark" else "large-text"}")
                }
            }
        }
    }

    private fun assertReachable(node: SemanticsNodeInteraction) {
        val ancestors = generateSequence(node.fetchSemanticsNode().parent) { it.parent }
        if (ancestors.any { it.config.contains(SemanticsActions.ScrollBy) }) node.performScrollTo()
        node.assertIsDisplayed()
    }

    private fun anchor(screen: AppScreen, tag: String): SemanticsNodeInteraction = when (screen) {
        AppScreen.AUTH -> compose.onNodeWithTag("auth_submit")
        AppScreen.GROUPS -> compose.onNodeWithTag("balance_card")
        AppScreen.CREATE_GROUP -> compose.onNodeWithTag("group_submit")
        AppScreen.GROUP_DETAIL -> compose.onNodeWithText("Train tickets")
        AppScreen.GROUP_MEMBERS -> compose.onNodeWithTag("create_invitation")
        AppScreen.INVITATION_REDEMPTION -> compose.onNodeWithTag("invitation_submit")
        AppScreen.EXPENSE_EDITOR -> compose.onNodeWithTag("expense_submit")
        AppScreen.EXPENSE_DETAIL -> compose.onNodeWithTag("expense_detail_edit")
        AppScreen.SETTLEMENT_EDITOR -> compose.onNodeWithTag("settlement_submit")
        AppScreen.SETTINGS -> compose.onNodeWithText(localized(tag).getString(R.string.language))
        AppScreen.SEARCH -> compose.onNodeWithTag("global_search")
        AppScreen.NOTIFICATIONS -> compose.onNodeWithText(localized(tag).getString(R.string.mark_all_as_read))
        AppScreen.REMINDERS -> compose.onNodeWithTag("reminder_trip_p2")
        AppScreen.FRIENDS -> compose.onNodeWithText(localized(tag).getString(R.string.open_your_groups))
        AppScreen.HELP -> compose.onNodeWithText(localized(tag).getString(R.string.report_an_issue))
        AppScreen.GROUP_OPTIONS -> compose.onNodeWithText(localized(tag).getString(R.string.save_group))
        AppScreen.SETTLEMENT_PICKER -> compose.onNodeWithText("Bob → Alice")
        AppScreen.EXPENSE_GROUP_PICKER -> compose.onNodeWithText("Lisbon weekend")
        AppScreen.SETTLEMENT_DETAIL -> compose.onNodeWithTag("settlement_edit")
    }

    private fun render(screen: AppScreen, tag: String, night: Boolean = false, fontScale: Float = 1f,
        tab: GroupTab = GroupTab.EXPENSES, error: UiText? = null) {
        compose.runOnIdle {
            state = fixture(screen).copy(selectedTab = tab, error = error)
            language = tag
            dark = night
            scale = fontScale
            pageKey++
        }
    }

    private fun capture(name: String) {
        val folder = requireNotNull(context.getExternalFilesDir("localization-previews")).apply { mkdirs() }
        File(folder, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun capturePage(name: String) {
        capture(name)
        if (state.screen == AppScreen.EXPENSE_EDITOR) {
            compose.onNodeWithTag("expense_split_section").performScrollTo()
            compose.onNode(hasScrollAction() and hasAnyDescendant(hasTestTag("expense_split_section")), useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 160f * context.resources.displayMetrics.density) }
            capture("$name-split")
            val discard = compose.onNodeWithText(localized(language).getString(R.string.discard_draft)).performScrollTo()
            discard.assertIsDisplayed()
            val footer = compose.onNodeWithTag("expense_footer").fetchSemanticsNode().boundsInRoot
            val draft = discard.fetchSemanticsNode().boundsInRoot
            assertTrue("The save footer must not cover the draft action: draft=$draft, footer=$footer", draft.bottom <= footer.top + 1f)
        }
    }

    private val pages = AppScreen.entries
    private val previews = listOf(AppScreen.GROUPS, AppScreen.SETTINGS, AppScreen.REMINDERS,
        AppScreen.EXPENSE_EDITOR, AppScreen.SETTLEMENT_DETAIL)

    private fun fixture(screen: AppScreen): SplitTheBillsUiState {
        val group = group()
        return SplitTheBillsUiState(authResolved = true, preferencesResolved = true,
            user = if (screen == AppScreen.AUTH) null else SignedInUser("alice", "alice@example.test", "Alice"),
            screen = screen, groups = listOf(GroupSummary(group.id, group.name, "EUR", "OWNER", "p1", 3, false, GroupType.TRIP)),
            groupDetails = mapOf(group.id to group), selectedGroup = group, selectedGroupId = group.id,
            selectedExpenseId = "e1", selectedSettlementId = "s1", profileName = "Alice",
            expenseEditor = ExpenseEditorState(title = "Train tickets", amount = "26,00", payerParticipantId = "p1", selectedParticipantIds = setOf("p1", "p2")),
            settlementEditor = SettlementEditorState(fromParticipantId = "p2", toParticipantId = "p1", amount = "9,00"))
    }

    private fun group() = GroupDetail("trip", "Lisbon weekend", "EUR", 2, "alice", 3, false, "p1",
        listOf(GroupMember("alice", "OWNER", "p1"), GroupMember("bob", "MEMBER", "p2")),
        listOf(Participant("p1", "Alice", "alice", true), Participant("p2", "Bob", "bob", true), Participant("p3", "Cara", null, true)),
        listOf(Expense("e1", "Train tickets", 2600, "EUR", listOf(Contribution("p1", 2600)),
            listOf(Allocation("p1", 1300), Allocation("p2", 1300)), SplitMode.EQUAL,
            listOf(SplitEntryInput("p1"), SplitEntryInput("p2")), "2026-11-16", "TRANSPORT", "Two tickets", "alice", 1)),
        listOf(Settlement("s1", "p2", "p1", 400, "EUR", "2026-11-16", "Train fare", "bob", 1)),
        listOf(ParticipantBalance("p1", 900), ParticipantBalance("p2", -900), ParticipantBalance("p3", 0)),
        listOf(ActivityEvent("a1", "SETTLEMENT_CREATED", "SETTLEMENT", "Bob recorded a repayment", "bob", 1, 1000L, "s1")), false, GroupType.TRIP)
}
