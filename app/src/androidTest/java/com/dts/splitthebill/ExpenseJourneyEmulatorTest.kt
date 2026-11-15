package com.dts.splitthebill

import android.view.View
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.google.firebase.auth.FirebaseAuth
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

        compose.waitUntilAtLeastOneExists(hasText("New group"), timeoutMillis = 15_000)
        compose.onNodeWithText("New group").performClick()
        compose.onNodeWithTag("group_name").performTextInput("Android journey $suffix")
        compose.onNodeWithTag("participant_name").performTextInput("Alex")
        compose.onNodeWithTag("group_submit").performScrollTo().performClick()

        compose.waitUntilAtLeastOneExists(hasText("Add expense"), timeoutMillis = 15_000)
        compose.onNodeWithTag("group_participant_name").performTextInput("Blair")
        compose.onNodeWithTag("group_participant_submit").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Blair"), timeoutMillis = 10_000)
        compose.onNodeWithText("Add expense").performClick()

        compose.onNodeWithTag("expense_title").performTextInput("Shared lunch")
        compose.onNodeWithTag("expense_amount").performTextInput("12.00")
        compose.onNodeWithTag("expense_submit").performScrollTo().performClick()

        compose.waitUntilAtLeastOneExists(hasText("Confirmed shares"), timeoutMillis = 15_000)
        compose.onNodeWithText("Shared lunch").assertIsDisplayed()
        compose.onNodeWithText("Confirmed shares").assertIsDisplayed()
        compose.onAllNodes(hasText("EUR 12.00"))[0].assertIsDisplayed()
    }
}
