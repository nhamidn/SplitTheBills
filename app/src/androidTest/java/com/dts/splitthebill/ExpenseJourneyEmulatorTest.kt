package com.dts.splitthebill

import android.view.View
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.After
import org.junit.Rule
import org.junit.Test

/** Requires Auth, Firestore, and Functions emulators plus adb reverse for ports 9099/8080/5001. */
@OptIn(ExperimentalTestApi::class)
class ExpenseJourneyEmulatorTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @After
    fun leaveDeviceSignedOut() {
        FirebaseAuth.getInstance().signOut()
    }

    @Test
    fun accountToConfirmedExpenseJourney() {
        // System password-manager save prompts can cover the app after account creation and
        // make physical-device runs nondeterministic. Release builds keep normal autofill.
        compose.activity.window.decorView.importantForAutofill =
            View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        // `connectedAndroidTest` may update an app that the developer has already used.
        // Start from Auth without deleting their groups, drafts, or emulator records.
        FirebaseAuth.getInstance().signOut()
        compose.waitUntilAtLeastOneExists(hasText("Create account"), timeoutMillis = 5_000)
        val suffix = System.currentTimeMillis()
        compose.onNodeWithText("Create account").performClick()
        compose.onNodeWithTag("auth_email").performTextInput("android-$suffix@example.test")
        compose.onNodeWithTag("auth_password").performTextInput("correcthorse")
        compose.onNodeWithTag("auth_submit").performClick()

        compose.waitUntilAtLeastOneExists(hasText("Groups"), timeoutMillis = 15_000)
        compose.onNodeWithContentDescription("Groups").performClick()
        compose.onNodeWithText("Create a group").performScrollTo().performClick()
        compose.onNodeWithTag("group_name").performTextInput("Android journey $suffix")
        compose.onNodeWithTag("participant_name").performTextInput("Alex")
        compose.onNodeWithTag("group_submit").performScrollTo().performClick()

        compose.waitUntilAtLeastOneExists(hasText("Add expense"), timeoutMillis = 15_000)
        compose.onNodeWithContentDescription("Manage people").performClick()
        compose.onNodeWithTag("group_participant_name").performScrollTo().performTextInput("Blair")
        compose.onNodeWithTag("group_participant_submit").performScrollTo().performClick()
        compose.waitUntilAtLeastOneExists(hasText("Blair"), timeoutMillis = 10_000)
        closeSoftKeyboard()
        compose.onNodeWithContentDescription("Back").performClick()
        // The confirmation snackbar temporarily covers the floating Add expense button.
        compose.waitUntilDoesNotExist(hasText("Participant added"), timeoutMillis = 10_000)
        compose.onNodeWithText("Add expense").performClick()

        compose.waitUntilAtLeastOneExists(hasTestTag("expense_title"), timeoutMillis = 10_000)
        compose.onNodeWithTag("expense_title").performTextInput("Shared lunch")
        compose.onNodeWithTag("expense_amount").performTextInput("12.00")
        closeSoftKeyboard()
        compose.onNodeWithTag("expense_submit").performClick()

        compose.waitUntilAtLeastOneExists(hasText("Confirmed shares"), timeoutMillis = 15_000)
        compose.onNodeWithText("Shared lunch").assertIsDisplayed()
        compose.onNodeWithText("Confirmed shares").assertIsDisplayed()
        compose.onAllNodes(hasText("EUR 12.00"))[0].assertIsDisplayed()

        val firestore = FirebaseFirestore.getInstance()
        val uid = requireNotNull(FirebaseAuth.getInstance().currentUser).uid
        val projection = Tasks.await(firestore.collection("users/$uid/groups").get(Source.SERVER), 10, TimeUnit.SECONDS).documents.single()
        val groupId = projection.id
        val ownerParticipant = requireNotNull(projection.getString("participantId"))
        val people = Tasks.await(firestore.collection("groups/$groupId/participants").get(Source.SERVER), 10, TimeUnit.SECONDS).documents

        // A real 50/50 form must send 5,000 basis points per participant and preserve its note.
        compose.waitUntilDoesNotExist(hasText("Expense saved"), timeoutMillis = 10_000)
        compose.onNodeWithTag("expense_detail_edit").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("expense_title"), timeoutMillis = 5_000)
        compose.onNodeWithText("Percent").performScrollTo().performClick()
        people.forEach { compose.onNodeWithTag("split_value_${it.id}").performScrollTo().performTextReplacement("50") }
        compose.onNodeWithTag("expense_note").performScrollTo().performTextInput("Receipt checked")
        closeSoftKeyboard()
        compose.onNodeWithTag("expense_submit").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Confirmed shares"), timeoutMillis = 15_000)
        compose.onNodeWithText("Receipt checked").performScrollTo().assertIsDisplayed()
        val expense = Tasks.await(firestore.collection("groups/$groupId/expenses").get(Source.SERVER), 10, TimeUnit.SECONDS).documents.single()
        assertEquals("PERCENTAGE", expense.getString("splitInput.mode"))
        assertEquals(600L, Tasks.await(firestore.document("groups/$groupId/balances/$ownerParticipant").get(Source.SERVER), 10, TimeUnit.SECONDS).getLong("amountMinor"))

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Settle up").performScrollTo().performClick()
        compose.onNodeWithText("Record").performScrollTo().performClick()
        compose.onNodeWithTag("settlement_amount").performTextReplacement("2.00")
        closeSoftKeyboard()
        compose.onNodeWithTag("settlement_submit").performScrollTo().performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("settlement_detail_total") and hasText("EUR 2.00"), timeoutMillis = 15_000)
        assertEquals(400L, Tasks.await(firestore.document("groups/$groupId/balances/$ownerParticipant").get(Source.SERVER), 10, TimeUnit.SECONDS).getLong("amountMinor"))

        compose.onNodeWithTag("settlement_edit").performScrollTo().performClick()
        compose.onNodeWithTag("settlement_amount").performTextReplacement("3.00")
        closeSoftKeyboard()
        compose.onNodeWithTag("settlement_submit").performScrollTo().performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("settlement_detail_total") and hasText("EUR 3.00"), timeoutMillis = 15_000)
        assertEquals(300L, Tasks.await(firestore.document("groups/$groupId/balances/$ownerParticipant").get(Source.SERVER), 10, TimeUnit.SECONDS).getLong("amountMinor"))

        compose.onNodeWithTag("settlement_remove").performScrollTo().performClick()
        compose.onNodeWithText("Confirm removal").performClick()
        compose.waitUntilAtLeastOneExists(hasText("WHO OWES WHOM"), timeoutMillis = 15_000)
        assertEquals(600L, Tasks.await(firestore.document("groups/$groupId/balances/$ownerParticipant").get(Source.SERVER), 10, TimeUnit.SECONDS).getLong("amountMinor"))
        compose.onNodeWithText("History").performScrollTo().performClick()
        compose.onNodeWithText("Settlement deleted").performScrollTo().assertIsDisplayed()

        // Supply a chosen document without driving another app's UI. This exercises the
        // real CreateDocument result callback, ContentResolver write, and success state.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val exported = File(compose.activity.cacheDir, "journey-export-$suffix.csv")
        val filter = IntentFilter(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            addDataType("text/csv")
        }
        val monitor = instrumentation.addMonitor(filter,
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(exported))), true)
        try {
            compose.onNodeWithText("Export").performScrollTo().performClick()
            compose.waitUntilAtLeastOneExists(hasText("CSV saved"), timeoutMillis = 10_000)
            assertEquals(1, monitor.hits)
            val csv = exported.readText()
            assertTrue(csv.contains("\"Shared lunch\""))
            assertTrue(csv.contains("\"Receipt checked\""))
            assertTrue(csv.contains("\"12.00\""))
            assertTrue(!csv.contains("\"repayment\"")) // Soft-deleted repayment is excluded.
        } finally {
            instrumentation.removeMonitor(monitor)
            exported.delete()
        }
    }
}
