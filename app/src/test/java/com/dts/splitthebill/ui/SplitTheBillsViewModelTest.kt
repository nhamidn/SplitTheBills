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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplitTheBillsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

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
        assertTrue(viewModel.state.value.error.orEmpty().contains("connect", ignoreCase = true))
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
        assertEquals("Restored local draft", restored.state.value.draftNotice)
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
        assertTrue(viewModel.state.value.conflict.orEmpty().contains("server revision 3"))
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
    var createGroupCalls = 0
    var lastExpense: ExpenseInput? = null
    var lastRedeemToken: String? = null
    var lastRedeemCode: String? = null
    var updateFailure: BackendException? = null
    var revokeFailure: BackendException? = null
    var lastOwnershipTransfers: List<OwnershipTransfer> = emptyList()
    var localCacheCleared = false

    override fun observeGroups(uid: String): Flow<List<GroupSummary>> = groups
    override fun observeGroup(groupId: String, uid: String): Flow<GroupDetail> = detail
    override suspend fun createGroup(name: String, currency: String, participantName: String): MutationResult {
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
    override suspend fun createSettlement(groupId: String, settlement: SettlementInput) = MutationResult("s1", 1)
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
