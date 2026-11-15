package com.dts.splitthebill

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.dts.splitthebill.domain.model.Allocation
import com.dts.splitthebill.domain.model.Contribution
import com.dts.splitthebill.domain.model.Expense
import com.dts.splitthebill.domain.model.GroupDetail
import com.dts.splitthebill.domain.model.GroupMember
import com.dts.splitthebill.domain.model.Participant
import com.dts.splitthebill.domain.model.SignedInUser
import com.dts.splitthebill.domain.model.SplitEntryInput
import com.dts.splitthebill.domain.model.SplitMode
import com.dts.splitthebill.ui.AppScreen
import com.dts.splitthebill.ui.SplitTheBillsApp
import com.dts.splitthebill.ui.SplitTheBillsUiState
import com.dts.splitthebill.ui.theme.SplitTheBillsTheme
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ExpenseParticipantsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsPayerAndSplitParticipantsWithoutDuplicatingOrIncludingOtherGroupMembers() {
        val names = listOf("Alex", "Blair", "Casey", "Drew", "Emery", "Finley", "Gray", "Harper")
        val participants = names.mapIndexed { index, name -> Participant("p$index", name, null, true) }
        val expense = Expense(
            id = "preview-expense", title = "Shared dinner", amountMinor = 21000, currency = "MAD",
            contributions = listOf(Contribution("p0", 21000)),
            allocations = (1..6).map { Allocation("p$it", 3500) },
            splitMode = SplitMode.EQUAL,
            splitEntries = (1..6).map { SplitEntryInput("p$it") },
            date = "2026-11-15", category = "FOOD", note = null, createdByUid = "preview", revision = 1,
        )
        val group = GroupDetail(
            id = "preview", name = "Dinner with friends", currency = "MAD", currencyExponent = 2,
            ownerUid = "preview", revision = 1, archived = false, currentMemberParticipantId = "p1",
            members = listOf(GroupMember("preview", "OWNER", "p1")), participants = participants,
            expenses = listOf(expense), settlements = emptyList(), balances = emptyList(),
            activity = emptyList(), fromCache = false,
        )
        compose.setContent {
            SplitTheBillsTheme(darkTheme = false) {
                SplitTheBillsApp(
                    SplitTheBillsUiState(
                        authResolved = true, user = SignedInUser("preview", "blair@example.test", "Blair"),
                        screen = AppScreen.GROUP_DETAIL, selectedGroup = group,
                    ),
                    onAction = {}, onGoogleSignIn = {},
                )
            }
        }
        compose.onNodeWithTag("expense_participants_preview-expense", useUnmergedTree = true)
            .assertContentDescriptionEquals("Participants: Alex, Blair, Casey, Drew, Emery, Finley, Gray")

        // Keep a local preview for visual review without creating backend test records.
        val preview = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
            .resolve("expense-participants-preview.png")
        preview.outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
