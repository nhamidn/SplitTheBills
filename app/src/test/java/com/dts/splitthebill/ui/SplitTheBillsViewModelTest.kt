package com.dts.splitthebill.ui

import com.dts.splitthebill.domain.model.ExpenseInput
import com.dts.splitthebill.domain.model.ExpenseDraft
import com.dts.splitthebill.domain.model.Expense
import com.dts.splitthebill.domain.model.Contribution
import com.dts.splitthebill.domain.model.Allocation
import com.dts.splitthebill.domain.model.SplitEntryInput
import com.dts.splitthebill.domain.model.SplitMode
import com.dts.splitthebill.domain.model.GroupDetail
import com.dts.splitthebill.domain.model.GroupSummary
import com.dts.splitthebill.domain.model.GroupMember
import com.dts.splitthebill.domain.model.InvitationSecret
import com.dts.splitthebill.domain.model.InvitationSummary
import com.dts.splitthebill.domain.model.MutationResult
import com.dts.splitthebill.domain.model.Participant
import com.dts.splitthebill.domain.model.ParticipantBalance
import com.dts.splitthebill.domain.model.OwnershipCandidate
import com.dts.splitthebill.domain.model.OwnershipTransfer
import com.dts.splitthebill.domain.model.OwnershipOptions
import com.dts.splitthebill.domain.model.SettlementInput
import com.dts.splitthebill.domain.model.Settlement
import com.dts.splitthebill.domain.model.SignedInUser
import com.dts.splitthebill.domain.repository.AuthRepository
import com.dts.splitthebill.domain.repository.ExpenseDraftRepository
import com.dts.splitthebill.domain.repository.GroupsRepository
import com.dts.splitthebill.domain.repository.NetworkMonitor
import com.dts.splitthebill.domain.repository.BackendException
import com.dts.splitthebill.domain.repository.ThemePreference
import com.dts.splitthebill.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplitTheBillsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Test fun repaymentFromAnotherGroupKeepsFollowingThatGroupAndUsesItsCurrency() = runTest(dispatcher) {
        val groups = FakeGroupsRepository()
        groups.groups.value = listOf("g1", "g2").map { GroupSummary(it, it, if (it == "g1") "EUR" else "KWD", "OWNER", "p1", 1, false) }
        groups.details["g2"] = MutableStateFlow(sampleGroup().copy(id = "g2", currency = "KWD", currencyExponent = 3))
        val vm = SplitTheBillsViewModel(FakeAuthRepository(SignedInUser("alice", null, "Alice")), groups, FakeExpenseDraftRepository(), FakeNetworkMonitor(true))
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.OpenGroup("g1"))
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.PrepareSettlement("g2", "p2", "p1", 1234))
        advanceUntilIdle()
        groups.detail.value = sampleGroup().copy(name = "Old group changed")
        groups.details.getValue("g2").value = groups.details.getValue("g2").value.copy(name = "New group changed")
        advanceUntilIdle()
        assertEquals("g2", vm.state.value.selectedGroup?.id)
        assertEquals("New group changed", vm.state.value.selectedGroup?.name)
        assertEquals("1.234", vm.state.value.settlementEditor.amount)
        vm.onAction(SplitTheBillsAction.SubmitSettlement)
        advanceUntilIdle()
        assertEquals("g2", groups.lastSettlementGroupId)
        assertEquals("KWD", groups.lastSettlement?.currency)
        assertEquals(1234L, groups.lastSettlement?.amountMinor)
    }

    @Test fun percentagesUseOrdinaryUiValuesAndValidateBeforeCallingTheBackend() = runTest(dispatcher) {
        val groups = FakeGroupsRepository()
        val vm = SplitTheBillsViewModel(FakeAuthRepository(SignedInUser("alice", null, "Alice")), groups, FakeExpenseDraftRepository(), FakeNetworkMonitor(true))
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.OpenGroup("g1"))
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.OpenNewExpense)
        vm.onAction(SplitTheBillsAction.ExpenseTitleChanged("Train"))
        vm.onAction(SplitTheBillsAction.ExpenseAmountChanged("10.01"))
        vm.onAction(SplitTheBillsAction.ExpenseModeChanged(SplitMode.PERCENTAGE))
        vm.onAction(SplitTheBillsAction.ExpenseEntryValueChanged("p1", "33.33"))
        vm.onAction(SplitTheBillsAction.ExpenseEntryValueChanged("p2", "33.33"))
        vm.onAction(SplitTheBillsAction.SubmitExpense)
        advanceUntilIdle()
        assertEquals(null, groups.lastExpense)
        assertEquals(UiText(com.dts.splitthebill.R.string.percentages_must_add_up_to_exactly_100), vm.state.value.error)
        vm.onAction(SplitTheBillsAction.ExpenseEntryValueChanged("p2", "66.67"))
        vm.onAction(SplitTheBillsAction.SubmitExpense)
        advanceUntilIdle()
        assertEquals(listOf(3333L, 6667L), groups.lastExpense?.splitEntries?.map { it.value })
    }

    @Test fun editingPercentageExpenseRestoresPercentagesInsteadOfBasisPoints() = runTest(dispatcher) {
        val groups = FakeGroupsRepository()
        groups.detail.value = sampleGroup().copy(expenses = listOf(sampleExpense(2, "Train").copy(
            splitMode = SplitMode.PERCENTAGE, splitEntries = listOf(SplitEntryInput("p1", 3333), SplitEntryInput("p2", 6667)))))
        val vm = SplitTheBillsViewModel(FakeAuthRepository(SignedInUser("alice", null, "Alice")), groups, FakeExpenseDraftRepository(), FakeNetworkMonitor(true))
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.OpenGroupExpense("g1", "e1"))
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.EditSelectedExpense)
        assertEquals(mapOf("p1" to "33.33", "p2" to "66.67"), vm.state.value.expenseEditor.entryValues)
    }

    @Test fun retryingARepaymentReusesItsOperationId() = runTest(dispatcher) {
        val groups = FakeGroupsRepository()
        val vm = SplitTheBillsViewModel(FakeAuthRepository(SignedInUser("alice", null, "Alice")), groups, FakeExpenseDraftRepository(), FakeNetworkMonitor(true))
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.OpenGroup("g1"))
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.OpenSettlement)
        vm.onAction(SplitTheBillsAction.SettlementAmountChanged("5.00"))
        groups.settlementFailure = BackendException("UNAVAILABLE", "Connection interrupted")
        vm.onAction(SplitTheBillsAction.SubmitSettlement)
        advanceUntilIdle()
        groups.settlementFailure = null
        vm.onAction(SplitTheBillsAction.SubmitSettlement)
        advanceUntilIdle()
        assertEquals(2, groups.settlementOperations.size)
        assertEquals(1, groups.settlementOperations.distinct().size)
        assertEquals(AppScreen.SETTLEMENT_DETAIL, vm.state.value.screen)
    }

    @Test fun staleRepaymentKeepsChangesAndReloadsTheLatestRevisionBeforeRetry() = runTest(dispatcher) {
        val groups = FakeGroupsRepository()
        val payment = Settlement("s1", "p2", "p1", 500, "EUR", "2026-11-16", "Original", "alice", 1)
        groups.detail.value = sampleGroup().copy(settlements = listOf(payment))
        val vm = SplitTheBillsViewModel(FakeAuthRepository(SignedInUser("alice", null, "Alice")), groups, FakeExpenseDraftRepository(), FakeNetworkMonitor(true))
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.OpenGroupSettlement("g1", "s1"))
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.EditSelectedSettlement)
        vm.onAction(SplitTheBillsAction.SettlementAmountChanged("3.00"))
        groups.settlementFailure = BackendException("STALE_REVISION", "Changed", mapOf("actualRevision" to 2L))
        vm.onAction(SplitTheBillsAction.SubmitSettlement)
        advanceUntilIdle()
        assertEquals(ConflictTarget.SETTLEMENT, vm.state.value.conflictTarget)
        assertEquals("3.00", vm.state.value.settlementEditor.amount)
        groups.detail.value = sampleGroup().copy(settlements = listOf(payment.copy(revision = 2, amountMinor = 400)))
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.ConflictReloaded)
        assertEquals("4.00", vm.state.value.settlementEditor.amount)
        groups.settlementFailure = null
        vm.onAction(SplitTheBillsAction.SubmitSettlement)
        advanceUntilIdle()
        assertEquals(2L, groups.lastSettlementRevision)
        vm.onAction(SplitTheBillsAction.DeleteSelectedSettlement)
        advanceUntilIdle()
        assertEquals("s1", groups.deletedSettlementId)
        assertEquals(AppScreen.GROUP_DETAIL, vm.state.value.screen)
    }

    @Test fun openingGroupsFromFriendsLeavesTheProfileDestination() = runTest(dispatcher) {
        val vm = SplitTheBillsViewModel(FakeAuthRepository(SignedInUser("alice", null, "Alice")), FakeGroupsRepository(), FakeExpenseDraftRepository(), FakeNetworkMonitor(true))
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.Navigate(MainDestination.PROFILE))
        vm.onAction(SplitTheBillsAction.OpenFeature(AppScreen.FRIENDS))
        vm.onAction(SplitTheBillsAction.OpenGroups)
        assertEquals(MainDestination.GROUPS, vm.state.value.mainDestination)
        assertEquals(AppScreen.GROUPS, vm.state.value.screen)
    }

    @Test fun notificationReadStateIsScopedToTheSignedInAccount() = runTest(dispatcher) {
        val preferences = FakeUserPreferencesRepository()
        val groups = FakeGroupsRepository()
        groups.groups.value = listOf(GroupSummary("g1", "Weekend", "EUR", "OWNER", "p1", 1, false))
        groups.detail.value = sampleGroup().copy(activity = listOf(
            com.dts.splitthebill.domain.model.ActivityEvent("a1", "EXPENSE_CREATED", "EXPENSE", "Train", "bob", 1, 1000)))
        val vm = SplitTheBillsViewModel(FakeAuthRepository(SignedInUser("alice", null, "Alice")), groups, FakeExpenseDraftRepository(), FakeNetworkMonitor(true), preferences)
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.MarkNotificationsRead)
        advanceUntilIdle()
        assertEquals(1000L, vm.state.value.preferences.notificationReadAt["alice"])
        assertEquals(null, vm.state.value.preferences.notificationReadAt["bob"])
    }

    @Test
    fun overviewLoadsEveryGroupWithoutOpeningOneAndClearsAtSignOut() = runTest(dispatcher) {
        val auth = FakeAuthRepository(SignedInUser("alice", "alice@example.test", "Alice"))
        val groups = FakeGroupsRepository()
        groups.groups.value = listOf(
            GroupSummary("g1", "Weekend", "EUR", "OWNER", "p1", 1, false),
            GroupSummary("g2", "Home", "USD", "OWNER", "p1", 1, false),
        )
        groups.details["g2"] = MutableStateFlow(sampleGroup().copy(id = "g2", currency = "USD"))
        val vm = SplitTheBillsViewModel(auth, groups, FakeExpenseDraftRepository(), FakeNetworkMonitor(true))
        advanceUntilIdle()
        assertEquals(setOf("g1", "g2"), vm.state.value.groupDetails.keys)
        assertEquals(null, vm.state.value.selectedGroup)
        vm.onAction(SplitTheBillsAction.OpenGroupExpense("g2", "e1"))
        advanceUntilIdle()
        assertEquals("g2", vm.state.value.selectedGroup?.id)
        assertEquals(AppScreen.EXPENSE_DETAIL, vm.state.value.screen)
        vm.onAction(SplitTheBillsAction.SignOut)
        advanceUntilIdle()
        assertTrue(vm.state.value.groupDetails.isEmpty())
        assertEquals(null, vm.state.value.selectedGroup)
        assertEquals(null, vm.state.value.exportDocument)
    }

    @Test
    fun savedDefaultCurrencyAppliesToNewGroupsAndSimplificationPersists() = runTest(dispatcher) {
        val preferences = FakeUserPreferencesRepository()
        val vm = SplitTheBillsViewModel(FakeAuthRepository(SignedInUser("alice", null, "Alice")),
            FakeGroupsRepository(), FakeExpenseDraftRepository(), FakeNetworkMonitor(true), preferences)
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.DefaultCurrencyChanged("KWD"))
        vm.onAction(SplitTheBillsAction.SimplifyDebtsChanged(false))
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.OpenCreateGroup)
        assertEquals("KWD", vm.state.value.createGroupForm.currency)
        assertFalse(vm.state.value.preferences.simplifyDebts)
    }

    @Test
    fun cancelledExportDoesNotReportASuccessAndKeepsTheSnapshotWhilePickerIsOpen() = runTest(dispatcher) {
        val vm = SplitTheBillsViewModel(FakeAuthRepository(SignedInUser("alice", null, "Alice")),
            FakeGroupsRepository(), FakeExpenseDraftRepository(), FakeNetworkMonitor(true))
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.OpenGroup("g1"))
        advanceUntilIdle()
        vm.onAction(SplitTheBillsAction.ExportGroup)
        assertTrue(vm.state.value.exportRequested)
        vm.onAction(SplitTheBillsAction.ExportPickerOpened)
        assertNotNull(vm.state.value.exportDocument)
        assertFalse(vm.state.value.exportRequested)
        vm.onAction(SplitTheBillsAction.ExportFinished())
        assertEquals(null, vm.state.value.exportDocument)
        assertEquals(null, vm.state.value.message)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signedInUserObservesGroupsAndLeavesAuthScreen() = runTest(dispatcher) {
        val auth = FakeAuthRepository(SignedInUser("alice", "alice@example.test", null))
        val groups = FakeGroupsRepository()
        groups.groups.value = listOf(GroupSummary("g1", "Weekend", "EUR", "OWNER", "p1", 1, false))

        val viewModel = SplitTheBillsViewModel(auth, groups, FakeExpenseDraftRepository(), FakeNetworkMonitor(true))
        advanceUntilIdle()

        assertEquals(AppScreen.GROUPS, viewModel.state.value.screen)
        assertEquals("Weekend", viewModel.state.value.groups.single().name)
    }

    @Test
    fun offlineStateBlocksSharedMutationBeforeRepositoryCall() = runTest(dispatcher) {
        val groups = FakeGroupsRepository()
        val viewModel = SplitTheBillsViewModel(
            FakeAuthRepository(SignedInUser("alice", "alice@example.test", null)),
            groups,
            FakeExpenseDraftRepository(),
            FakeNetworkMonitor(false),
        )
        advanceUntilIdle()
        viewModel.onAction(SplitTheBillsAction.OpenCreateGroup)
        viewModel.onAction(SplitTheBillsAction.CreateGroupNameChanged("Trip"))
        viewModel.onAction(SplitTheBillsAction.CreateGroupParticipantChanged("Alice"))
        viewModel.onAction(SplitTheBillsAction.SubmitGroup)
        advanceUntilIdle()

        assertEquals(0, groups.createGroupCalls)
        assertEquals(UiText(com.dts.splitthebill.R.string.connect_to_the_internet_before_making_a_shared_change), viewModel.state.value.error)
    }

    @Test
    fun equalExpenseConvertsDecimalToMinorUnitsAndUsesSelectedPayer() = runTest(dispatcher) {
        val groups = FakeGroupsRepository()
        groups.detail.value = sampleGroup()
        val viewModel = SplitTheBillsViewModel(
            FakeAuthRepository(SignedInUser("alice", "alice@example.test", null)),
            groups,
            FakeExpenseDraftRepository(),
            FakeNetworkMonitor(true),
        )
        advanceUntilIdle()
        viewModel.onAction(SplitTheBillsAction.OpenGroup("g1"))
        advanceUntilIdle()
        viewModel.onAction(SplitTheBillsAction.OpenNewExpense)
        viewModel.onAction(SplitTheBillsAction.ExpenseTitleChanged("Lunch"))
        viewModel.onAction(SplitTheBillsAction.ExpenseAmountChanged("12.34"))
        viewModel.onAction(SplitTheBillsAction.SubmitExpense)
        advanceUntilIdle()

        val input = groups.lastExpense
        assertNotNull(input)
        assertEquals(1234L, input?.amountMinor)
        assertEquals("p1", input?.payerParticipantId)
        assertEquals(setOf("p1", "p2"), input?.splitEntries?.map { it.participantId }?.toSet())
        assertEquals(AppScreen.EXPENSE_DETAIL, viewModel.state.value.screen)
    }

    @Test
    fun localExpenseDraftIsRestoredForTheSameUserAndGroup() = runTest(dispatcher) {
        val drafts = FakeExpenseDraftRepository()
        val groups = FakeGroupsRepository().also { it.detail.value = sampleGroup() }
        val auth = FakeAuthRepository(SignedInUser("alice", "alice@example.test", null))
        val first = SplitTheBillsViewModel(auth, groups, drafts, FakeNetworkMonitor(true))
        advanceUntilIdle()
        first.onAction(SplitTheBillsAction.OpenGroup("g1"))
        advanceUntilIdle()
        first.onAction(SplitTheBillsAction.OpenNewExpense)
        first.onAction(SplitTheBillsAction.ExpenseTitleChanged("Saved locally"))
        advanceTimeBy(400)
        advanceUntilIdle()

        val restored = SplitTheBillsViewModel(auth, groups, drafts, FakeNetworkMonitor(true))
        advanceUntilIdle()
        restored.onAction(SplitTheBillsAction.OpenGroup("g1"))
        advanceUntilIdle()
        restored.onAction(SplitTheBillsAction.OpenNewExpense)
        advanceUntilIdle()

        assertEquals("Saved locally", restored.state.value.expenseEditor.title)
        assertEquals(UiText(com.dts.splitthebill.R.string.restored_local_draft), restored.state.value.draftNotice)
    }

    @Test
    fun invitationCodeIsRedeemedWithoutTreatingItAsAnIdentityLink() = runTest(dispatcher) {
        val groups = FakeGroupsRepository()
        val viewModel = SplitTheBillsViewModel(
            FakeAuthRepository(SignedInUser("alice", "alice@example.test", null)),
            groups,
            FakeExpenseDraftRepository(),
            FakeNetworkMonitor(true),
        )
        advanceUntilIdle()
        viewModel.onAction(SplitTheBillsAction.OpenInvitationRedemption)
        viewModel.onAction(SplitTheBillsAction.InvitationValueChanged("ABCDE-23456"))
        viewModel.onAction(SplitTheBillsAction.SubmitInvitation)
        advanceUntilIdle()

        assertEquals("ABCDE-23456", groups.lastRedeemCode)
        assertEquals(null, groups.lastRedeemToken)
        assertEquals(AppScreen.GROUP_DETAIL, viewModel.state.value.screen)
    }

    @Test
    fun staleExpenseKeepsDraftAndCanReloadNewServerVersion() = runTest(dispatcher) {
        val groups = FakeGroupsRepository()
        groups.detail.value = sampleGroup().copy(expenses = listOf(sampleExpense(2, "Old title")))
        groups.updateFailure = BackendException(
            "STALE_REVISION",
            "Changed elsewhere",
            mapOf("actualRevision" to 3L),
        )
        val viewModel = SplitTheBillsViewModel(
            FakeAuthRepository(SignedInUser("alice", "alice@example.test", null)),
            groups,
            FakeExpenseDraftRepository(),
            FakeNetworkMonitor(true),
        )
        advanceUntilIdle()
        viewModel.onAction(SplitTheBillsAction.OpenGroup("g1"))
        advanceUntilIdle()
        viewModel.onAction(SplitTheBillsAction.OpenExpense("e1"))
        viewModel.onAction(SplitTheBillsAction.EditSelectedExpense)
        viewModel.onAction(SplitTheBillsAction.ExpenseTitleChanged("My draft"))
        viewModel.onAction(SplitTheBillsAction.SubmitExpense)
        advanceUntilIdle()
        assertEquals(listOf(3L), viewModel.state.value.conflict?.arguments)
        assertEquals("My draft", viewModel.state.value.expenseEditor.title)

        groups.detail.value = sampleGroup().copy(expenses = listOf(sampleExpense(3, "Server title")))
        advanceUntilIdle()
        viewModel.onAction(SplitTheBillsAction.ConflictReloaded)
        advanceUntilIdle()
        assertEquals("Server title", viewModel.state.value.expenseEditor.title)
        assertEquals(null, viewModel.state.value.conflict)
    }

    @Test
    fun usedInvitationCanRefreshItsPrivateRevisionAndBeRevoked() = runTest(dispatcher) {
        val groups = FakeGroupsRepository()
        groups.revokeFailure = BackendException(
            "STALE_REVISION",
            "Invitation changed",
            mapOf("actualRevision" to 2L),
        )
        val viewModel = SplitTheBillsViewModel(
            FakeAuthRepository(SignedInUser("alice", "alice@example.test", null)),
            groups,
            FakeExpenseDraftRepository(),
            FakeNetworkMonitor(true),
        )
        advanceUntilIdle()
        viewModel.onAction(SplitTheBillsAction.OpenGroup("g1"))
        advanceUntilIdle()
        viewModel.onAction(SplitTheBillsAction.OpenGroupMembers)
        viewModel.onAction(SplitTheBillsAction.CreateInvitation)
        advanceUntilIdle()
        viewModel.onAction(SplitTheBillsAction.RevokeInvitation("i1", 1))
        advanceUntilIdle()
        assertEquals(ConflictTarget.INVITATION, viewModel.state.value.conflictTarget)

        viewModel.onAction(SplitTheBillsAction.ConflictReloaded)
        assertEquals(2L, viewModel.state.value.groupMembers.invitation?.revision)
        groups.revokeFailure = null
        viewModel.onAction(SplitTheBillsAction.RevokeInvitation("i1", 2))
        advanceUntilIdle()
        assertEquals(null, viewModel.state.value.groupMembers.invitation)
    }

    @Test
    fun themeAndProfileChangesFlowThroughRepositories() = runTest(dispatcher) {
        val auth = FakeAuthRepository(SignedInUser("alice", "alice@example.test", null))
        val preferences = FakeUserPreferencesRepository()
        val viewModel = SplitTheBillsViewModel(
            auth,
            FakeGroupsRepository(),
            FakeExpenseDraftRepository(),
            FakeNetworkMonitor(true),
            preferences,
        )
        advanceUntilIdle()

        viewModel.onAction(SplitTheBillsAction.ThemeChanged(ThemePreference.DARK))
        viewModel.onAction(SplitTheBillsAction.ProfileNameChanged("Alice A."))
        viewModel.onAction(SplitTheBillsAction.SaveProfile)
        advanceUntilIdle()

        assertEquals(ThemePreference.DARK, preferences.theme.value)
        assertEquals("Alice A.", viewModel.state.value.user?.displayName)
    }

    @Test
    fun accountDeletionTransfersOwnedGroupAndClearsLocalState() = runTest(dispatcher) {
        val auth = FakeAuthRepository(SignedInUser("alice", "alice@example.test", "Alice"))
        val groups = FakeGroupsRepository().also {
            it.groups.value = listOf(GroupSummary("g1", "Weekend", "EUR", "OWNER", "p1", 4, false))
        }
        val drafts = FakeExpenseDraftRepository()
        val viewModel = SplitTheBillsViewModel(
            auth,
            groups,
            drafts,
            FakeNetworkMonitor(true),
        )
        advanceUntilIdle()

        viewModel.onAction(SplitTheBillsAction.OpenAccountDeletion)
        advanceUntilIdle()
        viewModel.onAction(SplitTheBillsAction.AccountDeletionOwnerChanged("g1", "blair"))
        viewModel.onAction(SplitTheBillsAction.AccountDeletionConfirmationChanged("DELETE"))
        viewModel.onAction(SplitTheBillsAction.ConfirmAccountDeletion)
        advanceUntilIdle()

        assertEquals(OwnershipTransfer("g1", "blair", 4), groups.lastOwnershipTransfers.single())
        assertTrue(groups.localCacheCleared)
        assertTrue(viewModel.state.value.accountDeleted)
    }
}

private class FakeAuthRepository(initial: SignedInUser?) : AuthRepository {
    override val currentUser = MutableStateFlow(initial)
    override suspend fun signIn(email: String, password: String) = Unit
    override suspend fun createAccount(email: String, password: String) = Unit
    override suspend fun signInWithGoogle(idToken: String) = Unit
    override suspend fun sendPasswordReset(email: String) = Unit
    override suspend fun updateDisplayName(displayName: String) {
        currentUser.value = currentUser.value?.copy(displayName = displayName)
    }
    override suspend fun signOut() { currentUser.value = null }
}

private class FakeNetworkMonitor(initial: Boolean) : NetworkMonitor {
    override val isOnline: Flow<Boolean> = MutableStateFlow(initial)
}

private class FakeGroupsRepository : GroupsRepository {
    val groups = MutableStateFlow<List<GroupSummary>>(emptyList())
    val detail = MutableStateFlow(sampleGroup())
    val details = mutableMapOf<String, MutableStateFlow<GroupDetail>>()
    var createGroupCalls = 0
    var lastExpense: ExpenseInput? = null
    var lastRedeemToken: String? = null
    var lastRedeemCode: String? = null
    var updateFailure: BackendException? = null
    var revokeFailure: BackendException? = null
    var lastOwnershipTransfers: List<OwnershipTransfer> = emptyList()
    var localCacheCleared = false
    var lastSettlement: SettlementInput? = null
    var lastSettlementGroupId: String? = null
    var lastSettlementRevision: Long? = null
    var deletedSettlementId: String? = null
    var settlementFailure: BackendException? = null
    val settlementOperations = mutableListOf<String>()

    override fun observeGroups(uid: String): Flow<List<GroupSummary>> = groups
    override fun observeGroup(groupId: String, uid: String): Flow<GroupDetail> = details[groupId] ?: detail
    override suspend fun renameGroup(groupId: String, expectedRevision: Long, name: String, type: com.dts.splitthebill.domain.model.GroupType) = MutationResult(groupId, expectedRevision + 1)
    override suspend fun archiveGroup(groupId: String, expectedRevision: Long) = MutationResult(groupId, expectedRevision + 1)
    override suspend fun createGroup(name: String, currency: String, participantName: String, type: com.dts.splitthebill.domain.model.GroupType): MutationResult {
        createGroupCalls++
        return MutationResult("g1", 1)
    }
    override suspend fun createParticipant(groupId: String, name: String) = MutationResult("p3", 1)
    override suspend fun createExpense(groupId: String, expense: ExpenseInput, operationId: String): MutationResult {
        lastExpense = expense
        return MutationResult("e1", 1)
    }
    override suspend fun updateExpense(
        groupId: String,
        expenseId: String,
        expectedRevision: Long,
        expense: ExpenseInput,
        operationId: String,
    ) =
        updateFailure?.let { throw it } ?: MutationResult(expenseId, expectedRevision + 1)
    override suspend fun deleteExpense(groupId: String, expenseId: String, expectedRevision: Long) =
        MutationResult(expenseId, expectedRevision + 1)
    override suspend fun createSettlement(groupId: String, settlement: SettlementInput, operationId: String): MutationResult {
        settlementOperations += operationId
        lastSettlement = settlement
        lastSettlementGroupId = groupId
        settlementFailure?.let { throw it }
        return MutationResult("s1", 1)
    }
    override suspend fun updateSettlement(groupId: String, settlementId: String, expectedRevision: Long, settlement: SettlementInput, operationId: String): MutationResult {
        settlementOperations += operationId
        lastSettlement = settlement
        lastSettlementRevision = expectedRevision
        settlementFailure?.let { throw it }
        return MutationResult(settlementId, expectedRevision + 1)
    }
    override suspend fun deleteSettlement(groupId: String, settlementId: String, expectedRevision: Long, operationId: String): MutationResult {
        settlementFailure?.let { throw it }
        deletedSettlementId = settlementId
        return MutationResult(settlementId, expectedRevision + 1)
    }
    override suspend fun createInvitation(
        groupId: String,
        expiresInSeconds: Long,
        maxUses: Long,
        operationId: String,
    ) = InvitationSecret("i1", 1, 2, "2099-01-01T00:00:00Z", "token", "ABCDE-23456", false)
    override suspend fun listInvitations(groupId: String) = listOf(
        InvitationSummary("i1", 1, "2026-09-08T00:00:00Z", "2099-01-01T00:00:00Z", null, 1, 0),
    )
    override suspend fun revokeInvitation(
        groupId: String,
        invitationId: String,
        expectedRevision: Long,
        operationId: String,
    ) = revokeFailure?.let { throw it } ?: MutationResult(invitationId, expectedRevision + 1)
    override suspend fun redeemInvitation(token: String?, code: String?, operationId: String): MutationResult {
        lastRedeemToken = token
        lastRedeemCode = code
        return MutationResult("g1", 2)
    }
    override suspend fun approveParticipantLink(
        groupId: String,
        participantId: String,
        memberUid: String,
        expectedRevision: Long,
        operationId: String,
    ) = MutationResult(participantId, 2)
    override suspend fun ownershipOptions(groupId: String) =
        OwnershipOptions(groups.value.firstOrNull { it.id == groupId }?.revision ?: 1, listOf(OwnershipCandidate("blair", "Blair")))
    override suspend fun deleteAccount(transfers: List<OwnershipTransfer>, operationId: String): MutationResult {
        lastOwnershipTransfers = transfers
        return MutationResult("alice", 1)
    }
    override suspend fun clearLocalCache() {
        localCacheCleared = true
    }
}

private class FakeUserPreferencesRepository : UserPreferencesRepository {
    override val theme = MutableStateFlow(ThemePreference.SYSTEM)
    override val features = MutableStateFlow(com.dts.splitthebill.domain.repository.FeaturePreferences())
    override suspend fun setDefaultCurrency(value: String) { features.value = features.value.copy(defaultCurrency = value) }
    override suspend fun setSimplifyDebts(value: Boolean) { features.value = features.value.copy(simplifyDebts = value) }
    override suspend fun setBiometricLock(value: Boolean) { features.value = features.value.copy(biometricLock = value) }
    override suspend fun markNotificationsRead(uid: String, throughEpochMillis: Long) {
        features.value = features.value.copy(notificationReadAt = features.value.notificationReadAt + (uid to throughEpochMillis))
    }
    override suspend fun setTheme(value: ThemePreference) {
        theme.value = value
    }
}

private class FakeExpenseDraftRepository : ExpenseDraftRepository {
    private val drafts = mutableListOf<ExpenseDraft>()
    override suspend fun load(ownerUid: String, groupId: String, expenseId: String?): ExpenseDraft? =
        drafts.lastOrNull { it.ownerUid == ownerUid && it.groupId == groupId && it.expenseId == expenseId }
    override suspend fun save(draft: ExpenseDraft) {
        drafts.removeAll { it.ownerUid == draft.ownerUid && it.groupId == draft.groupId && it.expenseId == draft.expenseId }
        drafts += draft
    }
    override suspend fun delete(ownerUid: String, groupId: String, expenseId: String?) {
        drafts.removeAll { it.ownerUid == ownerUid && it.groupId == groupId && it.expenseId == expenseId }
    }
    override suspend fun deleteForUser(ownerUid: String) {
        drafts.removeAll { it.ownerUid == ownerUid }
    }
}

private fun sampleGroup() = GroupDetail(
    id = "g1",
    name = "Weekend",
    currency = "EUR",
    currencyExponent = 2,
    ownerUid = "alice",
    revision = 1,
    archived = false,
    currentMemberParticipantId = "p1",
    members = listOf(GroupMember("alice", "OWNER", "p1")),
    participants = listOf(
        Participant("p1", "Alice", "alice", true),
        Participant("p2", "Blair", null, true),
    ),
    expenses = emptyList(),
    settlements = emptyList(),
    balances = listOf(ParticipantBalance("p1", 0), ParticipantBalance("p2", 0)),
    activity = emptyList(),
    fromCache = false,
)

private fun sampleExpense(revision: Long, title: String) = Expense(
    id = "e1",
    title = title,
    amountMinor = 1000,
    currency = "EUR",
    contributions = listOf(Contribution("p1", 1000)),
    allocations = listOf(Allocation("p1", 500), Allocation("p2", 500)),
    splitMode = SplitMode.EQUAL,
    splitEntries = listOf(SplitEntryInput("p1"), SplitEntryInput("p2")),
    date = "2026-09-08",
    category = "FOOD",
    note = null,
    createdByUid = "alice",
    revision = revision,
)
