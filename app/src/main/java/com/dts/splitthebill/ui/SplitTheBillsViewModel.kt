package com.dts.splitthebill.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dts.splitthebill.domain.CurrencyUnit
import com.dts.splitthebill.domain.MoneyText
import com.dts.splitthebill.domain.model.Expense
import com.dts.splitthebill.domain.model.ExpenseDraft
import com.dts.splitthebill.domain.model.ExpenseInput
import com.dts.splitthebill.domain.model.GroupDetail
import com.dts.splitthebill.domain.model.GroupSummary
import com.dts.splitthebill.domain.model.GroupType
import com.dts.splitthebill.domain.repository.FeaturePreferences
import com.dts.splitthebill.domain.model.InvitationSecret
import com.dts.splitthebill.domain.model.InvitationSummary
import com.dts.splitthebill.domain.model.OwnershipCandidate
import com.dts.splitthebill.domain.model.OwnershipTransfer
import com.dts.splitthebill.domain.model.SettlementInput
import com.dts.splitthebill.domain.model.Settlement
import com.dts.splitthebill.domain.model.SignedInUser
import com.dts.splitthebill.domain.model.SplitMode
import com.dts.splitthebill.domain.repository.AuthRepository
import com.dts.splitthebill.domain.repository.BackendException
import com.dts.splitthebill.domain.repository.ExpenseDraftRepository
import com.dts.splitthebill.domain.repository.GroupsRepository
import com.dts.splitthebill.domain.repository.NetworkMonitor
import com.dts.splitthebill.domain.repository.ThemePreference
import com.dts.splitthebill.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AppScreen {
    AUTH, GROUPS, CREATE_GROUP, GROUP_DETAIL, GROUP_MEMBERS, INVITATION_REDEMPTION,
    EXPENSE_EDITOR, EXPENSE_DETAIL, SETTLEMENT_EDITOR, SETTINGS,
    SEARCH, NOTIFICATIONS, REMINDERS, FRIENDS, HELP, GROUP_OPTIONS, SETTLEMENT_PICKER,
    EXPENSE_GROUP_PICKER, SETTLEMENT_DETAIL,
}
enum class MainDestination { HOME, GROUPS, ACTIVITY, PROFILE }
enum class AuthMode { SIGN_IN, CREATE_ACCOUNT }
enum class GroupTab { EXPENSES, BALANCES, MEMBERS, ACTIVITY }
enum class ConflictTarget { EXPENSE, SETTLEMENT, INVITATION, GROUP }

data class AuthFormState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val email: String = "",
    val password: String = "",
)

data class CreateGroupFormState(
    val name: String = "",
    val currency: String = "EUR",
    val participantName: String = "",
    val type: GroupType = GroupType.OTHER,
)

data class ExpenseEditorState(
    val expenseId: String? = null,
    val expectedRevision: Long? = null,
    val title: String = "",
    val amount: String = "",
    val payerParticipantId: String = "",
    val selectedParticipantIds: Set<String> = emptySet(),
    val splitMode: SplitMode = SplitMode.EQUAL,
    val entryValues: Map<String, String> = emptyMap(),
    val date: String = today(),
    val category: String = "FOOD",
    val note: String = "",
    val operationId: String = newOperationId(),
)

data class InvitationRedemptionState(
    val value: String = "",
    val fromLink: Boolean = false,
    val operationId: String = newOperationId(),
)

data class GroupMembersState(
    val expiresInSeconds: Long = 7 * 24 * 60 * 60,
    val maxUses: String = "1",
    val invitation: InvitationSecret? = null,
    val invitations: List<InvitationSummary> = emptyList(),
    val invitationsLoading: Boolean = false,
    val createOperationId: String = newOperationId(),
    val revokeOperationId: String = newOperationId(),
    val pendingRevokeInvitationId: String? = null,
    val selectedMemberUid: String = "",
    val selectedParticipantId: String = "",
    val linkOperationId: String = newOperationId(),
)

data class SettlementEditorState(
    val settlementId: String? = null,
    val expectedRevision: Long? = null,
    val operationId: String = newOperationId(),
    val fromParticipantId: String = "",
    val toParticipantId: String = "",
    val amount: String = "",
    val date: String = today(),
    val note: String = "",
)

data class AccountDeletionState(
    val open: Boolean = false,
    val confirmation: String = "",
    val loadingCandidates: Boolean = false,
    val candidates: Map<String, List<OwnershipCandidate>> = emptyMap(),
    val groupRevisions: Map<String, Long> = emptyMap(),
    val selectedNewOwners: Map<String, String> = emptyMap(),
    val operationId: String = newOperationId(),
)

data class SplitTheBillsUiState(
    val authResolved: Boolean = false,
    val user: SignedInUser? = null,
    val screen: AppScreen = AppScreen.AUTH,
    val mainDestination: MainDestination = MainDestination.HOME,
    val authForm: AuthFormState = AuthFormState(),
    val groups: List<GroupSummary> = emptyList(),
    val groupsLoading: Boolean = false,
    val groupDetails: Map<String, GroupDetail> = emptyMap(),
    val groupErrors: Map<String, String> = emptyMap(),
    val selectedGroup: GroupDetail? = null,
    val selectedGroupId: String? = null,
    val groupLoading: Boolean = false,
    val selectedTab: GroupTab = GroupTab.EXPENSES,
    val selectedExpenseId: String? = null,
    val selectedSettlementId: String? = null,
    val settlementDeleteOperationId: String = newOperationId(),
    val createGroupForm: CreateGroupFormState = CreateGroupFormState(),
    val expenseEditor: ExpenseEditorState = ExpenseEditorState(),
    val settlementEditor: SettlementEditorState = SettlementEditorState(),
    val invitationRedemption: InvitationRedemptionState = InvitationRedemptionState(),
    val groupMembers: GroupMembersState = GroupMembersState(),
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val preferences: FeaturePreferences = FeaturePreferences(),
    val preferencesResolved: Boolean = false,
    val searchScope: String = "All",
    val exportDocument: ExportDocument? = null,
    val exportRequested: Boolean = false,
    val profileName: String = "",
    val accountDeletion: AccountDeletionState = AccountDeletionState(),
    val accountDeleted: Boolean = false,
    val draftNotice: String? = null,
    val participantName: String = "",
    val online: Boolean = true,
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val conflict: String? = null,
    val conflictTarget: ConflictTarget? = null,
    val conflictActualRevision: Long? = null,
)

sealed interface SplitTheBillsAction {
    data class Navigate(val destination: MainDestination) : SplitTheBillsAction
    data class OpenFeature(val screen: AppScreen, val scope: String = "All") : SplitTheBillsAction
    data class DefaultCurrencyChanged(val value: String) : SplitTheBillsAction
    data class SimplifyDebtsChanged(val value: Boolean) : SplitTheBillsAction
    data class BiometricLockChanged(val value: Boolean) : SplitTheBillsAction
    data object MarkNotificationsRead : SplitTheBillsAction
    data object ExportGroup : SplitTheBillsAction
    data object ExportPickerOpened : SplitTheBillsAction
    data class ExportFinished(val error: String? = null, val saved: Boolean = false) : SplitTheBillsAction
    data class OpenGroupExpense(val groupId: String, val expenseId: String) : SplitTheBillsAction
    data class AddExpenseToGroup(val groupId: String) : SplitTheBillsAction
    data class OpenGroupSettlement(val groupId: String, val settlementId: String) : SplitTheBillsAction
    data object EditSelectedSettlement : SplitTheBillsAction
    data object DeleteSelectedSettlement : SplitTheBillsAction
    data class PrepareSettlement(val groupId: String, val from: String, val to: String, val amount: Long) : SplitTheBillsAction
    data class SaveGroupSettings(val name: String, val type: GroupType, val expectedRevision: Long) : SplitTheBillsAction
    data class ReportError(val message: String) : SplitTheBillsAction
    data object ArchiveGroup : SplitTheBillsAction
    data class CreateGroupTypeChanged(val value: GroupType) : SplitTheBillsAction
    data class AuthEmailChanged(val value: String) : SplitTheBillsAction
    data class AuthPasswordChanged(val value: String) : SplitTheBillsAction
    data class AuthModeChanged(val value: AuthMode) : SplitTheBillsAction
    data object SubmitEmailAuth : SplitTheBillsAction
    data object ResetPassword : SplitTheBillsAction
    data class GoogleTokenReceived(val token: String) : SplitTheBillsAction
    data class GoogleSignInFailed(val reason: String) : SplitTheBillsAction
    data object GoogleUnavailable : SplitTheBillsAction
    data object SignOut : SplitTheBillsAction
    data class ThemeChanged(val value: ThemePreference) : SplitTheBillsAction
    data class ProfileNameChanged(val value: String) : SplitTheBillsAction
    data object SaveProfile : SplitTheBillsAction
    data object OpenAccountDeletion : SplitTheBillsAction
    data object CloseAccountDeletion : SplitTheBillsAction
    data class AccountDeletionConfirmationChanged(val value: String) : SplitTheBillsAction
    data class AccountDeletionOwnerChanged(val groupId: String, val uid: String) : SplitTheBillsAction
    data object ConfirmAccountDeletion : SplitTheBillsAction
    data object OpenGroups : SplitTheBillsAction
    data object OpenSettings : SplitTheBillsAction
    data object OpenCreateGroup : SplitTheBillsAction
    data object OpenInvitationRedemption : SplitTheBillsAction
    data class InvitationValueChanged(val value: String) : SplitTheBillsAction
    data class InvitationLinkReceived(val token: String) : SplitTheBillsAction
    data object SubmitInvitation : SplitTheBillsAction
    data class CreateGroupNameChanged(val value: String) : SplitTheBillsAction
    data class CreateGroupCurrencyChanged(val value: String) : SplitTheBillsAction
    data class CreateGroupParticipantChanged(val value: String) : SplitTheBillsAction
    data object SubmitGroup : SplitTheBillsAction
    data class OpenGroup(val groupId: String) : SplitTheBillsAction
    data class SelectGroupTab(val tab: GroupTab) : SplitTheBillsAction
    data object OpenGroupMembers : SplitTheBillsAction
    data class InvitationExpiryChanged(val seconds: Long) : SplitTheBillsAction
    data class InvitationMaxUsesChanged(val value: String) : SplitTheBillsAction
    data object CreateInvitation : SplitTheBillsAction
    data class RevokeInvitation(val invitationId: String, val expectedRevision: Long) : SplitTheBillsAction
    data class LinkMemberChanged(val uid: String) : SplitTheBillsAction
    data class LinkParticipantChanged(val participantId: String) : SplitTheBillsAction
    data object ApproveParticipantLink : SplitTheBillsAction
    data object OpenNewExpense : SplitTheBillsAction
    data class OpenExpense(val expenseId: String) : SplitTheBillsAction
    data object EditSelectedExpense : SplitTheBillsAction
    data object DeleteSelectedExpense : SplitTheBillsAction
    data class ExpenseTitleChanged(val value: String) : SplitTheBillsAction
    data class ExpenseAmountChanged(val value: String) : SplitTheBillsAction
    data class ExpensePayerChanged(val participantId: String) : SplitTheBillsAction
    data class ExpenseParticipantToggled(val participantId: String) : SplitTheBillsAction
    data class ExpenseModeChanged(val mode: SplitMode) : SplitTheBillsAction
    data class ExpenseEntryValueChanged(val participantId: String, val value: String) : SplitTheBillsAction
    data class ExpenseDateChanged(val value: String) : SplitTheBillsAction
    data class ExpenseCategoryChanged(val value: String) : SplitTheBillsAction
    data class ExpenseNoteChanged(val value: String) : SplitTheBillsAction
    data object SubmitExpense : SplitTheBillsAction
    data object DiscardExpenseDraft : SplitTheBillsAction
    data class ParticipantNameChanged(val value: String) : SplitTheBillsAction
    data object AddParticipant : SplitTheBillsAction
    data object OpenSettlement : SplitTheBillsAction
    data class SettlementFromChanged(val value: String) : SplitTheBillsAction
    data class SettlementToChanged(val value: String) : SplitTheBillsAction
    data class SettlementAmountChanged(val value: String) : SplitTheBillsAction
    data class SettlementDateChanged(val value: String) : SplitTheBillsAction
    data class SettlementNoteChanged(val value: String) : SplitTheBillsAction
    data object SubmitSettlement : SplitTheBillsAction
    data object Back : SplitTheBillsAction
    data object Retry : SplitTheBillsAction
    data object MessageShown : SplitTheBillsAction
    data object ErrorShown : SplitTheBillsAction
    data object ConflictDismissed : SplitTheBillsAction
    data object ConflictReloaded : SplitTheBillsAction
}

@HiltViewModel
class SplitTheBillsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val groupsRepository: GroupsRepository,
    private val expenseDraftRepository: ExpenseDraftRepository,
    networkMonitor: NetworkMonitor,
    private val userPreferencesRepository: UserPreferencesRepository = DefaultUserPreferencesRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SplitTheBillsUiState())
    val state: StateFlow<SplitTheBillsUiState> = mutableState.asStateFlow()
    private var groupsJob: Job? = null
    private var groupJob: Job? = null
    private var draftJob: Job? = null
    private val overviewJobs = mutableMapOf<String, Job>()
    private var observedUid: String? = null

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online -> mutableState.update { it.copy(online = online) } }
        }
        viewModelScope.launch {
            userPreferencesRepository.theme.collect { theme ->
                mutableState.update { it.copy(themePreference = theme) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.features.catch { error("Could not load preferences. Please restart the app.") }
                .collect { preferences -> mutableState.update { it.copy(preferences = preferences, preferencesResolved = true) } }
        }
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user == null) signedOut() else signedIn(user)
            }
        }
    }

    fun onAction(action: SplitTheBillsAction) {
        when (action) {
            is SplitTheBillsAction.Navigate -> mutableState.update { it.copy(
                mainDestination = action.destination,
                screen = if (action.destination == MainDestination.PROFILE) AppScreen.SETTINGS else AppScreen.GROUPS,
            ) }
            is SplitTheBillsAction.OpenFeature -> mutableState.update { it.copy(screen = action.screen, searchScope = action.scope) }
            is SplitTheBillsAction.DefaultCurrencyChanged -> savePreference { userPreferencesRepository.setDefaultCurrency(action.value) }
            is SplitTheBillsAction.SimplifyDebtsChanged -> savePreference { userPreferencesRepository.setSimplifyDebts(action.value) }
            is SplitTheBillsAction.BiometricLockChanged -> savePreference { userPreferencesRepository.setBiometricLock(action.value) }
            SplitTheBillsAction.MarkNotificationsRead -> {
                val state = mutableState.value
                val uid = state.user?.uid ?: return
                val latest = notifications(state.groupDetails.values, uid).maxOfOrNull { it.event.occurredAtEpochMillis } ?: return
                savePreference { userPreferencesRepository.markNotificationsRead(uid, latest) }
            }
            SplitTheBillsAction.ExportGroup -> mutableState.value.selectedGroup?.let { group ->
                mutableState.update { it.copy(exportDocument = ExportDocument("split-the-bills-${group.id}.csv", groupCsv(group)), exportRequested = true) }
            }
            SplitTheBillsAction.ExportPickerOpened -> mutableState.update { it.copy(exportRequested = false) }
            is SplitTheBillsAction.ExportFinished -> mutableState.update {
                it.copy(exportDocument = null, exportRequested = false, error = action.error,
                    message = if (action.saved) "CSV saved" else it.message)
            }
            is SplitTheBillsAction.OpenGroupExpense -> openGroup(action.groupId, AppScreen.EXPENSE_DETAIL, action.expenseId)
            is SplitTheBillsAction.OpenGroupSettlement -> {
                openGroup(action.groupId, AppScreen.SETTLEMENT_DETAIL, settlementId = action.settlementId)
                mutableState.update { it.copy(settlementDeleteOperationId = newOperationId()) }
            }
            is SplitTheBillsAction.AddExpenseToGroup -> {
                val group = mutableState.value.groupDetails[action.groupId] ?: return error("This group is still loading. Please try again.")
                if (group.archived) return error("This group is archived.")
                openGroup(group.id)
                openExpenseEditor(null)
            }
            SplitTheBillsAction.EditSelectedSettlement -> selectedSettlement()?.let { settlement ->
                val group = mutableState.value.selectedGroup ?: return
                if (!canChangeRecord(group, settlement.createdByUid)) return error("Only the creator or group owner can edit this repayment.")
                mutableState.update { it.copy(screen = AppScreen.SETTLEMENT_EDITOR, settlementEditor = settlementEditorFor(settlement, group)) }
            }
            SplitTheBillsAction.DeleteSelectedSettlement -> deleteSelectedSettlement()
            is SplitTheBillsAction.PrepareSettlement -> {
                val group = mutableState.value.groupDetails[action.groupId] ?: mutableState.value.selectedGroup?.takeIf { it.id == action.groupId } ?: return
                if (group.archived || group.fromCache || !mutableState.value.online) return error("Connect and refresh this group before recording a repayment.")
                openGroup(group.id, AppScreen.SETTLEMENT_EDITOR)
                mutableState.update { it.copy(settlementEditor = SettlementEditorState(
                    fromParticipantId = action.from, toParticipantId = action.to,
                    amount = MoneyText.formatMinor(action.amount, CurrencyUnit(group.currency, group.currencyExponent)),
                )) }
            }
            is SplitTheBillsAction.SaveGroupSettings -> saveGroupSettings(action.name, action.type, action.expectedRevision)
            is SplitTheBillsAction.ReportError -> error(action.message)
            SplitTheBillsAction.ArchiveGroup -> archiveSelectedGroup()
            is SplitTheBillsAction.CreateGroupTypeChanged -> updateCreateGroup { copy(type = action.value) }
            is SplitTheBillsAction.AuthEmailChanged -> updateAuth { copy(email = action.value) }
            is SplitTheBillsAction.AuthPasswordChanged -> updateAuth { copy(password = action.value) }
            is SplitTheBillsAction.AuthModeChanged -> updateAuth { copy(mode = action.value) }
            SplitTheBillsAction.SubmitEmailAuth -> submitEmailAuth()
            SplitTheBillsAction.ResetPassword -> resetPassword()
            is SplitTheBillsAction.GoogleTokenReceived -> authenticate { authRepository.signInWithGoogle(action.token) }
            is SplitTheBillsAction.GoogleSignInFailed -> error(action.reason)
            SplitTheBillsAction.GoogleUnavailable -> message("Configure googleWebClientId to enable Google sign-in.")
            SplitTheBillsAction.SignOut -> launchSaving(requireOnline = false) { authRepository.signOut() }
            is SplitTheBillsAction.ThemeChanged -> changeTheme(action.value)
            is SplitTheBillsAction.ProfileNameChanged -> mutableState.update { it.copy(profileName = action.value) }
            SplitTheBillsAction.SaveProfile -> saveProfile()
            SplitTheBillsAction.OpenAccountDeletion -> openAccountDeletion()
            SplitTheBillsAction.CloseAccountDeletion -> mutableState.update {
                it.copy(accountDeletion = AccountDeletionState())
            }
            is SplitTheBillsAction.AccountDeletionConfirmationChanged -> mutableState.update {
                it.copy(accountDeletion = it.accountDeletion.copy(confirmation = action.value))
            }
            is SplitTheBillsAction.AccountDeletionOwnerChanged -> mutableState.update {
                it.copy(
                    accountDeletion = it.accountDeletion.copy(
                        selectedNewOwners = it.accountDeletion.selectedNewOwners + (action.groupId to action.uid),
                    ),
                )
            }
            SplitTheBillsAction.ConfirmAccountDeletion -> confirmAccountDeletion()
            SplitTheBillsAction.OpenGroups -> mutableState.update { it.copy(screen = AppScreen.GROUPS, mainDestination = MainDestination.GROUPS) }
            SplitTheBillsAction.OpenSettings -> mutableState.update { it.copy(screen = AppScreen.SETTINGS, mainDestination = MainDestination.PROFILE) }
            SplitTheBillsAction.OpenCreateGroup -> mutableState.update {
                it.copy(screen = AppScreen.CREATE_GROUP, createGroupForm = CreateGroupFormState(currency = it.preferences.defaultCurrency))
            }
            SplitTheBillsAction.OpenInvitationRedemption -> mutableState.update {
                it.copy(screen = AppScreen.INVITATION_REDEMPTION, invitationRedemption = InvitationRedemptionState())
            }
            is SplitTheBillsAction.InvitationValueChanged -> mutableState.update {
                it.copy(invitationRedemption = it.invitationRedemption.copy(value = action.value, fromLink = false))
            }
            is SplitTheBillsAction.InvitationLinkReceived -> receiveInvitationLink(action.token)
            SplitTheBillsAction.SubmitInvitation -> redeemInvitation()
            is SplitTheBillsAction.CreateGroupNameChanged -> updateCreateGroup { copy(name = action.value) }
            is SplitTheBillsAction.CreateGroupCurrencyChanged -> updateCreateGroup { copy(currency = action.value) }
            is SplitTheBillsAction.CreateGroupParticipantChanged -> updateCreateGroup { copy(participantName = action.value) }
            SplitTheBillsAction.SubmitGroup -> submitGroup()
            is SplitTheBillsAction.OpenGroup -> openGroup(action.groupId)
            is SplitTheBillsAction.SelectGroupTab -> mutableState.update { it.copy(selectedTab = action.tab) }
            SplitTheBillsAction.OpenGroupMembers -> openGroupMembers()
            is SplitTheBillsAction.InvitationExpiryChanged -> updateGroupMembers {
                copy(expiresInSeconds = action.seconds, invitation = null, createOperationId = newOperationId())
            }
            is SplitTheBillsAction.InvitationMaxUsesChanged -> updateGroupMembers {
                copy(maxUses = action.value, invitation = null, createOperationId = newOperationId())
            }
            SplitTheBillsAction.CreateInvitation -> createInvitation()
            is SplitTheBillsAction.RevokeInvitation -> revokeInvitation(action.invitationId, action.expectedRevision)
            is SplitTheBillsAction.LinkMemberChanged -> updateGroupMembers {
                copy(selectedMemberUid = action.uid, linkOperationId = newOperationId())
            }
            is SplitTheBillsAction.LinkParticipantChanged -> updateGroupMembers {
                copy(selectedParticipantId = action.participantId, linkOperationId = newOperationId())
            }
            SplitTheBillsAction.ApproveParticipantLink -> approveParticipantLink()
            SplitTheBillsAction.OpenNewExpense -> openExpenseEditor(null)
            is SplitTheBillsAction.OpenExpense -> mutableState.update {
                it.copy(screen = AppScreen.EXPENSE_DETAIL, selectedExpenseId = action.expenseId)
            }
            SplitTheBillsAction.EditSelectedExpense -> selectedExpense()?.let { expense ->
                val group = mutableState.value.selectedGroup ?: return
                if (!canChangeRecord(group, expense.createdByUid)) return error("Only the creator or group owner can edit this expense.")
                openExpenseEditor(expense)
            }
            SplitTheBillsAction.DeleteSelectedExpense -> deleteSelectedExpense()
            is SplitTheBillsAction.ExpenseTitleChanged -> updateExpense { copy(title = action.value) }
            is SplitTheBillsAction.ExpenseAmountChanged -> updateExpense { copy(amount = action.value) }
            is SplitTheBillsAction.ExpensePayerChanged -> updateExpense { copy(payerParticipantId = action.participantId) }
            is SplitTheBillsAction.ExpenseParticipantToggled -> toggleExpenseParticipant(action.participantId)
            is SplitTheBillsAction.ExpenseModeChanged -> updateExpense { copy(splitMode = action.mode, entryValues = emptyMap()) }
            is SplitTheBillsAction.ExpenseEntryValueChanged -> updateExpense {
                copy(entryValues = entryValues + (action.participantId to action.value))
            }
            is SplitTheBillsAction.ExpenseDateChanged -> updateExpense { copy(date = action.value) }
            is SplitTheBillsAction.ExpenseCategoryChanged -> updateExpense { copy(category = action.value) }
            is SplitTheBillsAction.ExpenseNoteChanged -> updateExpense { copy(note = action.value) }
            SplitTheBillsAction.SubmitExpense -> submitExpense()
            SplitTheBillsAction.DiscardExpenseDraft -> discardExpenseDraft()
            is SplitTheBillsAction.ParticipantNameChanged -> mutableState.update { it.copy(participantName = action.value) }
            SplitTheBillsAction.AddParticipant -> addParticipant()
            SplitTheBillsAction.OpenSettlement -> openSettlement()
            is SplitTheBillsAction.SettlementFromChanged -> updateSettlement { copy(fromParticipantId = action.value) }
            is SplitTheBillsAction.SettlementToChanged -> updateSettlement { copy(toParticipantId = action.value) }
            is SplitTheBillsAction.SettlementAmountChanged -> updateSettlement { copy(amount = action.value) }
            is SplitTheBillsAction.SettlementDateChanged -> updateSettlement { copy(date = action.value) }
            is SplitTheBillsAction.SettlementNoteChanged -> updateSettlement { copy(note = action.value) }
            SplitTheBillsAction.SubmitSettlement -> submitSettlement()
            SplitTheBillsAction.Back -> back()
            SplitTheBillsAction.Retry -> retry()
            SplitTheBillsAction.MessageShown -> mutableState.update { it.copy(message = null) }
            SplitTheBillsAction.ErrorShown -> mutableState.update { it.copy(error = null) }
            SplitTheBillsAction.ConflictDismissed -> mutableState.update {
                it.copy(conflict = null, conflictTarget = null, conflictActualRevision = null)
            }
            SplitTheBillsAction.ConflictReloaded -> reloadConflict()
        }
    }

    private fun signedOut() {
        groupsJob?.cancel()
        groupJob?.cancel()
        draftJob?.cancel()
        overviewJobs.values.forEach { it.cancel() }
        overviewJobs.clear()
        observedUid = null
        mutableState.update {
            SplitTheBillsUiState(
                authResolved = true,
                themePreference = it.themePreference,
                preferences = it.preferences,
                preferencesResolved = it.preferencesResolved,
                accountDeleted = it.accountDeleted,
            )
        }
    }

    private fun signedIn(user: SignedInUser) {
        if (mutableState.value.user?.uid != null && mutableState.value.user?.uid != user.uid) signedOut()
        mutableState.update { current ->
            current.copy(
                authResolved = true,
                user = user,
                profileName = user.displayName.orEmpty(),
                screen = if (current.invitationRedemption.value.isNotBlank()) {
                    AppScreen.INVITATION_REDEMPTION
                } else if (current.screen == AppScreen.AUTH) {
                    AppScreen.GROUPS
                } else {
                    current.screen
                },
            )
        }
        observeGroups(user)
    }

    private fun changeTheme(theme: ThemePreference) {
        mutableState.update { it.copy(themePreference = theme) }
        viewModelScope.launch {
            runCatching { userPreferencesRepository.setTheme(theme) }
                .onFailure { error("Could not save the appearance setting.") }
        }
    }

    private fun savePreference(block: suspend () -> Unit) {
        viewModelScope.launch { runCatching { block() }.onFailure { error("Could not save this setting. Please try again.") } }
    }

    private fun saveGroupSettings(name: String, type: GroupType, expectedRevision: Long) {
        val group = mutableState.value.selectedGroup ?: return
        if (group.ownerUid != mutableState.value.user?.uid) return error("Only the owner can change group settings.")
        val trimmed = name.trim()
        if (trimmed.codePointCount(0, trimmed.length) !in 1..80) return error("Use a group name between 1 and 80 characters.")
        launchSaving(success = "Group updated", conflictTarget = ConflictTarget.GROUP) {
            groupsRepository.renameGroup(group.id, expectedRevision, trimmed, type)
            mutableState.update { it.copy(screen = AppScreen.GROUP_DETAIL) }
        }
    }

    private fun archiveSelectedGroup() {
        val group = mutableState.value.selectedGroup ?: return
        if (group.ownerUid != mutableState.value.user?.uid) return error("Only the owner can archive this group.")
        launchSaving(success = "Group archived", conflictTarget = ConflictTarget.GROUP) {
            groupsRepository.archiveGroup(group.id, group.revision)
            mutableState.update { it.copy(screen = AppScreen.GROUPS) }
        }
    }

    private fun saveProfile() {
        val displayName = mutableState.value.profileName.trim()
        if (displayName.codePointCount(0, displayName.length) !in 1..80) {
            return error("Display name must be between 1 and 80 characters.")
        }
        launchSaving(success = "Profile updated") {
            authRepository.updateDisplayName(displayName)
            mutableState.update { state ->
                state.copy(user = state.user?.copy(displayName = displayName), profileName = displayName)
            }
        }
    }

    private fun openAccountDeletion() {
        if (!mutableState.value.online) return error("Connect to the internet before deleting your account.")
        val uid = mutableState.value.user?.uid ?: return
        val ownedGroups = mutableState.value.groups.filter { it.role == "OWNER" }
        mutableState.update {
            it.copy(accountDeletion = AccountDeletionState(open = true, loadingCandidates = ownedGroups.isNotEmpty()))
        }
        if (ownedGroups.isEmpty()) return
        viewModelScope.launch {
            try {
                val options = ownedGroups.associate { group ->
                    group.id to groupsRepository.ownershipOptions(group.id)
                }
                val candidates = options.mapValues { (_, value) -> value.candidates.filterNot { it.uid == uid } }
                val defaults = candidates.mapNotNull { (groupId, values) ->
                    values.singleOrNull()?.let { groupId to it.uid }
                }.toMap()
                mutableState.update {
                    it.copy(
                        accountDeletion = it.accountDeletion.copy(
                            loadingCandidates = false,
                            candidates = candidates,
                            groupRevisions = options.mapValues { it.value.groupRevision },
                            selectedNewOwners = defaults,
                        ),
                    )
                }
            } catch (failure: Throwable) {
                mutableState.update {
                    it.copy(
                        accountDeletion = it.accountDeletion.copy(loadingCandidates = false),
                        error = readable(failure),
                    )
                }
            }
        }
    }

    private fun confirmAccountDeletion() {
        val state = mutableState.value
        val uid = state.user?.uid ?: return
        val ownedGroups = state.groups.filter { it.role == "OWNER" }
        if (state.accountDeletion.confirmation != "DELETE") {
            return error("Type DELETE exactly to confirm account deletion.")
        }
        val unavailable = ownedGroups.firstOrNull { state.accountDeletion.candidates[it.id].isNullOrEmpty() }
        if (unavailable != null) {
            return error("Invite another member to ${unavailable.name} before deleting your account.")
        }
        val missing = ownedGroups.firstOrNull { state.accountDeletion.selectedNewOwners[it.id].isNullOrBlank() }
        if (missing != null) return error("Choose a new owner for ${missing.name}.")
        val transfers = ownedGroups.map { group ->
            OwnershipTransfer(
                groupId = group.id,
                newOwnerUid = requireNotNull(state.accountDeletion.selectedNewOwners[group.id]),
                expectedRevision = state.accountDeletion.groupRevisions[group.id] ?: group.revision,
            )
        }
        if (state.saving) return
        viewModelScope.launch {
            mutableState.update { it.copy(saving = true, error = null) }
            try {
                groupsRepository.deleteAccount(transfers, state.accountDeletion.operationId)
                groupsJob?.cancel()
                groupJob?.cancel()
                draftJob?.cancel()
                overviewJobs.values.forEach { it.cancel() }
                overviewJobs.clear()
                runCatching { expenseDraftRepository.deleteForUser(uid) }
                runCatching { authRepository.signOut() }
                runCatching { groupsRepository.clearLocalCache() }
                mutableState.update {
                    it.copy(
                        saving = false,
                        accountDeletion = AccountDeletionState(),
                        accountDeleted = true,
                    )
                }
            } catch (failure: Throwable) {
                mutableState.update { it.copy(saving = false, error = readable(failure)) }
            }
        }
    }

    private fun observeGroups(user: SignedInUser) {
        if (observedUid == user.uid && groupsJob?.isActive == true) return
        observedUid = user.uid
        groupsJob?.cancel()
        groupsJob = viewModelScope.launch {
            mutableState.update { it.copy(groupsLoading = true) }
            groupsRepository.observeGroups(user.uid)
                .catch { failure -> mutableState.update { it.copy(groupsLoading = false, error = readable(failure)) } }
                .collect { groups ->
                    val ids = groups.map { it.id }.toSet()
                    (overviewJobs.keys - ids).forEach { overviewJobs.remove(it)?.cancel() }
                    val lostSelection = mutableState.value.let { state ->
                        state.groups.any { it.id == state.selectedGroupId } && state.selectedGroupId !in ids
                    }
                    if (lostSelection) groupJob?.cancel()
                    mutableState.update { state -> state.copy(
                        groups = groups, groupsLoading = false,
                        groupDetails = state.groupDetails.filterKeys { it in ids },
                        groupErrors = state.groupErrors.filterKeys { it in ids },
                        selectedGroup = state.selectedGroup.takeUnless { lostSelection },
                        selectedGroupId = state.selectedGroupId.takeUnless { lostSelection },
                        groupLoading = if (lostSelection) false else state.groupLoading,
                        screen = if (lostSelection) AppScreen.GROUPS else state.screen,
                    ) }
                    groups.filterNot { it.id in overviewJobs }.forEach { summary ->
                        overviewJobs[summary.id] = viewModelScope.launch {
                            groupsRepository.observeGroup(summary.id, user.uid)
                                .catch { failure -> mutableState.update { it.copy(
                                    groupDetails = it.groupDetails - summary.id,
                                    groupErrors = it.groupErrors + (summary.id to readable(failure)),
                                ) } }
                                .collect { group ->
                                    if (mutableState.value.user?.uid == user.uid) mutableState.update { state -> state.copy(
                                        groupDetails = state.groupDetails + (group.id to group),
                                        groupErrors = state.groupErrors - group.id,
                                        selectedGroup = if (state.selectedGroupId == group.id) group else state.selectedGroup,
                                    ) }
                                }
                        }
                    }
                }
        }
    }

    private fun openGroup(
        groupId: String,
        screen: AppScreen = AppScreen.GROUP_DETAIL,
        expenseId: String? = null,
        settlementId: String? = null,
    ) {
        val uid = mutableState.value.user?.uid ?: return
        val loaded = mutableState.value.groupDetails[groupId]
        mutableState.update {
            it.copy(screen = screen, selectedGroup = loaded, selectedGroupId = groupId, groupLoading = loaded == null,
                selectedTab = if (it.selectedGroupId == groupId) it.selectedTab else GroupTab.EXPENSES,
                selectedExpenseId = expenseId, selectedSettlementId = settlementId)
        }
        groupJob?.cancel()
        groupJob = viewModelScope.launch {
            groupsRepository.observeGroup(groupId, uid)
                .catch { failure -> mutableState.update { it.copy(groupLoading = false, error = readable(failure)) } }
                .collect { group ->
                    if (mutableState.value.user?.uid == uid && mutableState.value.selectedGroupId == groupId) {
                        mutableState.update { it.copy(selectedGroup = group, groupLoading = false) }
                    }
                }
        }
    }

    private fun receiveInvitationLink(token: String) {
        if (token.isBlank()) return error("This invitation link is missing its token.")
        mutableState.update {
            it.copy(
                screen = if (it.user == null) AppScreen.AUTH else AppScreen.INVITATION_REDEMPTION,
                invitationRedemption = InvitationRedemptionState(value = token, fromLink = true),
                message = if (it.user == null) "Sign in to accept the invitation." else null,
            )
        }
    }

    private fun redeemInvitation() {
        val form = mutableState.value.invitationRedemption
        val value = form.value.trim()
        if (value.isEmpty()) return error("Enter an invitation code.")
        launchSaving(success = "Invitation accepted") {
            val result = groupsRepository.redeemInvitation(
                token = value.takeIf { form.fromLink },
                code = value.takeUnless { form.fromLink },
                operationId = form.operationId,
            )
            mutableState.update { it.copy(invitationRedemption = InvitationRedemptionState()) }
            openGroup(result.resourceId)
        }
    }

    private fun openGroupMembers() {
        val group = mutableState.value.selectedGroup ?: return
        if (group.ownerUid != mutableState.value.user?.uid) return error("Only the group owner can manage invitations and links.")
        val unlinkedMember = group.members.firstOrNull { it.participantId == null }?.uid.orEmpty()
        val unlinkedParticipant = group.participants.firstOrNull { it.active && it.linkedUid == null }?.id.orEmpty()
        mutableState.update {
            it.copy(
                screen = AppScreen.GROUP_MEMBERS,
                groupMembers = GroupMembersState(
                    invitationsLoading = true,
                    selectedMemberUid = unlinkedMember,
                    selectedParticipantId = unlinkedParticipant,
                ),
            )
        }
        loadInvitations(group.id)
    }

    private fun createInvitation() {
        val group = mutableState.value.selectedGroup ?: return
        val form = mutableState.value.groupMembers
        val maxUses = form.maxUses.toLongOrNull()
        if (maxUses == null || maxUses !in 1L..20L) return error("Invitation uses must be between 1 and 20.")
        launchSaving(success = "Invitation created — share it before leaving this screen") {
            val invitation = groupsRepository.createInvitation(
                groupId = group.id,
                expiresInSeconds = form.expiresInSeconds,
                maxUses = maxUses,
                operationId = form.createOperationId,
            )
            mutableState.update { state ->
                state.copy(groupMembers = state.groupMembers.copy(invitation = invitation))
            }
            refreshInvitations(group.id)
        }
    }

    private fun revokeInvitation(invitationId: String, expectedRevision: Long) {
        val group = mutableState.value.selectedGroup ?: return
        val form = mutableState.value.groupMembers
        mutableState.update { state ->
            state.copy(groupMembers = state.groupMembers.copy(pendingRevokeInvitationId = invitationId))
        }
        launchSaving(success = "Invitation revoked", conflictTarget = ConflictTarget.INVITATION) {
            groupsRepository.revokeInvitation(
                groupId = group.id,
                invitationId = invitationId,
                expectedRevision = expectedRevision,
                operationId = form.revokeOperationId,
            )
            refreshInvitations(group.id)
            mutableState.update { state ->
                state.copy(
                    groupMembers = state.groupMembers.copy(
                        invitation = state.groupMembers.invitation?.takeUnless { it.id == invitationId },
                        createOperationId = newOperationId(),
                        revokeOperationId = newOperationId(),
                        pendingRevokeInvitationId = null,
                    ),
                )
            }
        }
    }

    private fun loadInvitations(groupId: String) {
        viewModelScope.launch {
            runCatching { groupsRepository.listInvitations(groupId) }
                .onSuccess { invitations ->
                    mutableState.update {
                        it.copy(groupMembers = it.groupMembers.copy(invitations = invitations, invitationsLoading = false))
                    }
                }
                .onFailure { failure ->
                    mutableState.update {
                        it.copy(
                            groupMembers = it.groupMembers.copy(invitationsLoading = false),
                            error = readable(failure),
                        )
                    }
                }
        }
    }

    private suspend fun refreshInvitations(groupId: String) {
        val invitations = groupsRepository.listInvitations(groupId)
        mutableState.update {
            it.copy(groupMembers = it.groupMembers.copy(invitations = invitations, invitationsLoading = false))
        }
    }

    private fun approveParticipantLink() {
        val group = mutableState.value.selectedGroup ?: return
        val form = mutableState.value.groupMembers
        if (form.selectedMemberUid.isBlank() || form.selectedParticipantId.isBlank()) {
            return error("Choose both an unlinked member and an unlinked participant.")
        }
        launchSaving(success = "Account linked to participant", conflictTarget = ConflictTarget.GROUP) {
            groupsRepository.approveParticipantLink(
                groupId = group.id,
                participantId = form.selectedParticipantId,
                memberUid = form.selectedMemberUid,
                expectedRevision = group.revision,
                operationId = form.linkOperationId,
            )
            mutableState.update { state ->
                state.copy(
                    groupMembers = state.groupMembers.copy(
                        selectedMemberUid = "",
                        selectedParticipantId = "",
                        linkOperationId = newOperationId(),
                    ),
                )
            }
        }
    }

    private fun submitEmailAuth() {
        val form = mutableState.value.authForm
        if (!form.email.contains('@') || form.password.length < 6) {
            return error("Enter a valid email and a password of at least 6 characters.")
        }
        authenticate {
            if (form.mode == AuthMode.SIGN_IN) authRepository.signIn(form.email, form.password)
            else authRepository.createAccount(form.email, form.password)
        }
    }

    private fun resetPassword() {
        val email = mutableState.value.authForm.email
        if (!email.contains('@')) return error("Enter your email first.")
        launchSaving(success = "If the account exists, a reset email has been requested.") {
            authRepository.sendPasswordReset(email)
        }
    }

    private fun authenticate(block: suspend () -> Unit) = launchSaving(block = block)

    private fun submitGroup() {
        val form = mutableState.value.createGroupForm
        if (form.name.isBlank() || form.participantName.isBlank()) return error("Group and participant names are required.")
        launchSaving(success = "Group created") {
            val result = groupsRepository.createGroup(form.name.trim(), form.currency, form.participantName.trim(), form.type)
            openGroup(result.resourceId)
        }
    }

    private fun openExpenseEditor(expense: Expense?) {
        val group = mutableState.value.selectedGroup ?: return
        if (group.archived) return error("This group is archived and no longer accepts financial changes.")
        val editor = editorFor(expense, group)
        mutableState.update {
            it.copy(
                screen = AppScreen.EXPENSE_EDITOR,
                expenseEditor = editor,
                conflict = null,
                draftNotice = null,
            )
        }
        loadExpenseDraft(group, expense?.id)
    }

    private fun editorFor(expense: Expense?, group: GroupDetail): ExpenseEditorState {
        val activeIds = group.participants.filter { it.active }.map { it.id }
        if (expense == null) {
            return ExpenseEditorState(
                payerParticipantId = group.currentMemberParticipantId ?: activeIds.firstOrNull().orEmpty(),
                selectedParticipantIds = activeIds.toSet(),
            )
        }
        return ExpenseEditorState(
            expenseId = expense.id,
            expectedRevision = expense.revision,
            title = expense.title,
            amount = formatMinor(expense.amountMinor, group.currencyExponent),
            payerParticipantId = expense.contributions.singleOrNull()?.participantId.orEmpty(),
            selectedParticipantIds = expense.splitEntries.map { it.participantId }.toSet(),
            splitMode = expense.splitMode,
            entryValues = expense.splitEntries.mapNotNull { entry ->
                val value = entry.value ?: return@mapNotNull null
                entry.participantId to when (expense.splitMode) {
                    SplitMode.EXACT -> formatMinor(value, group.currencyExponent)
                    SplitMode.PERCENTAGE -> percentageText(value)
                    else -> value.toString()
                }
            }.toMap(),
            date = expense.date,
            category = expense.category,
            note = expense.note.orEmpty(),
        )
    }

    private fun toggleExpenseParticipant(participantId: String) = updateExpense {
        val selected = selectedParticipantIds.toMutableSet()
        if (!selected.add(participantId)) selected.remove(participantId)
        copy(selectedParticipantIds = selected, entryValues = entryValues.filterKeys(selected::contains))
    }

    private fun submitExpense() {
        val group = mutableState.value.selectedGroup ?: return
        val editor = mutableState.value.expenseEditor
        val input = runCatching { expenseInput(editor, group) }.getOrElse { return error(readable(it)) }
        launchSaving(
            success = if (editor.expenseId == null) "Expense saved" else "Expense updated",
            conflictTarget = ConflictTarget.EXPENSE,
        ) {
            val result = if (editor.expenseId == null) {
                groupsRepository.createExpense(group.id, input, editor.operationId)
            } else {
                groupsRepository.updateExpense(
                    group.id,
                    editor.expenseId,
                    editor.expectedRevision ?: 0L,
                    input,
                    editor.operationId,
                )
            }
            expenseDraftRepository.delete(requireNotNull(mutableState.value.user).uid, group.id, editor.expenseId)
            draftJob?.cancel()
            mutableState.update {
                it.copy(
                    screen = AppScreen.EXPENSE_DETAIL,
                    selectedExpenseId = result.resourceId,
                    draftNotice = null,
                )
            }
        }
    }

    private fun expenseInput(editor: ExpenseEditorState, group: GroupDetail): ExpenseInput {
        require(editor.title.isNotBlank()) { "Title is required." }
        require(editor.payerParticipantId.isNotBlank()) { "Choose who paid." }
        require(editor.selectedParticipantIds.isNotEmpty()) { "Select at least one participant." }
        val currency = CurrencyUnit(group.currency, group.currencyExponent)
        val amount = MoneyText.parseUnsignedMinor(editor.amount.trim(), currency)
        require(amount > 0L) { "Amount must be greater than zero." }
        require(group.participants.any { it.id == editor.payerParticipantId && it.active }) { "Choose an active payer." }
        val entries = splitEntries(editor, group)
        splitAllocations(editor, group) // Validate the entire split with the same calculation used by the backend.
        return ExpenseInput(
            title = editor.title.trim(),
            amountMinor = amount,
            currency = group.currency,
            payerParticipantId = editor.payerParticipantId,
            splitMode = editor.splitMode,
            splitEntries = entries,
            date = editor.date,
            category = editor.category,
            note = editor.note.trim().ifEmpty { null },
        )
    }

    private fun deleteSelectedExpense() {
        val group = mutableState.value.selectedGroup ?: return
        val expense = selectedExpense() ?: return
        if (!canChangeRecord(group, expense.createdByUid)) return error("Only the creator or group owner can remove this expense.")
        launchSaving(success = "Expense removed", conflictTarget = ConflictTarget.EXPENSE) {
            groupsRepository.deleteExpense(group.id, expense.id, expense.revision)
            mutableState.update { it.copy(screen = AppScreen.GROUP_DETAIL, selectedExpenseId = null) }
        }
    }

    private fun addParticipant() {
        val group = mutableState.value.selectedGroup ?: return
        val name = mutableState.value.participantName.trim()
        if (name.isEmpty()) return error("Enter a participant name.")
        launchSaving(success = "Participant added") {
            groupsRepository.createParticipant(group.id, name)
            mutableState.update { it.copy(participantName = "") }
        }
    }

    private fun openSettlement() {
        val group = mutableState.value.selectedGroup ?: return
        if (group.archived) return error("This group is archived.")
        val active = group.participants.filter { it.active }
        mutableState.update {
            it.copy(
                screen = AppScreen.SETTLEMENT_EDITOR,
                settlementEditor = SettlementEditorState(
                    fromParticipantId = active.firstOrNull()?.id.orEmpty(),
                    toParticipantId = active.drop(1).firstOrNull()?.id.orEmpty(),
                ),
            )
        }
    }

    private fun submitSettlement() {
        val group = mutableState.value.selectedGroup ?: return
        val form = mutableState.value.settlementEditor
        val amount = runCatching {
            MoneyText.parseUnsignedMinor(form.amount, CurrencyUnit(group.currency, group.currencyExponent))
        }.getOrElse { return error(readable(it)) }
        if (amount <= 0L || form.fromParticipantId.isBlank() || form.toParticipantId.isBlank() ||
            form.fromParticipantId == form.toParticipantId
        ) return error("Choose two different participants and a positive amount.")
        if (group.archived) return error("This group is archived.")
        if (form.settlementId != null && selectedSettlement()?.let { !canChangeRecord(group, it.createdByUid) } != false) {
            return error("This repayment is no longer available for you to edit.")
        }
        val input = SettlementInput(
            fromParticipantId = form.fromParticipantId,
            toParticipantId = form.toParticipantId,
            amountMinor = amount,
            currency = group.currency,
            date = form.date,
            note = form.note.trim().ifEmpty { null },
        )
        launchSaving(success = if (form.settlementId == null) "Repayment recorded — no money was transferred" else "Repayment updated",
            conflictTarget = ConflictTarget.SETTLEMENT) {
            val result = if (form.settlementId == null) {
                groupsRepository.createSettlement(group.id, input, form.operationId)
            } else {
                groupsRepository.updateSettlement(group.id, form.settlementId, requireNotNull(form.expectedRevision), input, form.operationId)
            }
            mutableState.update { it.copy(screen = AppScreen.SETTLEMENT_DETAIL, selectedSettlementId = result.resourceId,
                selectedTab = GroupTab.BALANCES, settlementDeleteOperationId = newOperationId()) }
        }
    }

    private fun selectedSettlement(): Settlement? = mutableState.value.let { state ->
        state.selectedGroup?.settlements?.firstOrNull { it.id == state.selectedSettlementId }
    }

    private fun settlementEditorFor(settlement: Settlement, group: GroupDetail) = SettlementEditorState(
        settlementId = settlement.id, expectedRevision = settlement.revision,
        fromParticipantId = settlement.fromParticipantId, toParticipantId = settlement.toParticipantId,
        amount = formatMinor(settlement.amountMinor, group.currencyExponent), date = settlement.date, note = settlement.note.orEmpty(),
    )

    private fun canChangeRecord(group: GroupDetail, creatorUid: String): Boolean =
        !group.archived && mutableState.value.user?.uid.let { it == group.ownerUid || it == creatorUid }

    private fun deleteSelectedSettlement() {
        val state = mutableState.value
        val group = state.selectedGroup ?: return
        val settlement = selectedSettlement() ?: return
        if (!canChangeRecord(group, settlement.createdByUid)) return error("Only the creator or group owner can remove this repayment.")
        launchSaving(success = "Repayment removed and balances restored", conflictTarget = ConflictTarget.SETTLEMENT) {
            groupsRepository.deleteSettlement(group.id, settlement.id, settlement.revision, state.settlementDeleteOperationId)
            mutableState.update { it.copy(screen = AppScreen.GROUP_DETAIL, selectedTab = GroupTab.BALANCES, selectedSettlementId = null) }
        }
    }

    private fun retry() {
        overviewJobs.values.forEach { it.cancel() }
        overviewJobs.clear()
        groupsJob?.cancel()
        observedUid = null
        mutableState.value.user?.let(::observeGroups)
        val state = mutableState.value
        state.selectedGroupId?.let { openGroup(it, state.screen, state.selectedExpenseId, state.selectedSettlementId) }
    }

    private fun back() {
        if (mutableState.value.saving) return
        if (mutableState.value.screen == AppScreen.EXPENSE_EDITOR) persistExpenseDraftImmediately()
        val target = when (mutableState.value.screen) {
            AppScreen.CREATE_GROUP, AppScreen.SETTINGS, AppScreen.INVITATION_REDEMPTION,
            AppScreen.SEARCH, AppScreen.NOTIFICATIONS, AppScreen.REMINDERS, AppScreen.FRIENDS,
            AppScreen.HELP, AppScreen.SETTLEMENT_PICKER, AppScreen.EXPENSE_GROUP_PICKER -> AppScreen.GROUPS
            AppScreen.EXPENSE_EDITOR, AppScreen.EXPENSE_DETAIL, AppScreen.SETTLEMENT_EDITOR,
            AppScreen.SETTLEMENT_DETAIL,
            AppScreen.GROUP_MEMBERS, AppScreen.GROUP_OPTIONS -> AppScreen.GROUP_DETAIL
            AppScreen.GROUP_DETAIL -> AppScreen.GROUPS
            else -> mutableState.value.screen
        }
        mutableState.update { it.copy(screen = target, conflict = null, draftNotice = null) }
    }

    private fun selectedExpense(): Expense? = mutableState.value.selectedGroup?.expenses
        ?.firstOrNull { it.id == mutableState.value.selectedExpenseId }

    private fun launchSaving(
        requireOnline: Boolean = true,
        success: String? = null,
        conflictTarget: ConflictTarget? = null,
        block: suspend () -> Unit,
    ) {
        if (mutableState.value.saving) return
        if (requireOnline && !mutableState.value.online) return error("Connect to the internet before making a shared change.")
        mutableState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                block()
                mutableState.update { it.copy(saving = false, message = success ?: it.message) }
            } catch (failure: BackendException) {
                val conflict = if (failure.errorCode == "STALE_REVISION") {
                    val actual = (failure.details["actualRevision"] as? Number)?.toLong()
                    if (conflictTarget == ConflictTarget.EXPENSE || conflictTarget == ConflictTarget.SETTLEMENT) {
                        "This record changed elsewhere${actual?.let { " (server revision $it)" }.orEmpty()}. " +
                            "Your changes are still available. Reload the server version or keep editing."
                    } else {
                        "This group changed elsewhere${actual?.let { " (server revision $it)" }.orEmpty()}. " +
                            "Reload the group before trying the management change again."
                    }
                } else null
                mutableState.update {
                    it.copy(
                        saving = false,
                        error = if (conflict == null) readable(failure) else null,
                        conflict = conflict,
                        conflictTarget = conflictTarget.takeIf { conflict != null },
                        conflictActualRevision = (failure.details["actualRevision"] as? Number)?.toLong()
                            .takeIf { conflict != null },
                    )
                }
            } catch (failure: Throwable) {
                mutableState.update { it.copy(saving = false, error = readable(failure)) }
            }
        }
    }

    private fun loadExpenseDraft(group: GroupDetail, expenseId: String?) {
        val uid = mutableState.value.user?.uid ?: return
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            val draft = expenseDraftRepository.load(uid, group.id, expenseId) ?: return@launch
            if (mutableState.value.screen != AppScreen.EXPENSE_EDITOR ||
                mutableState.value.selectedGroup?.id != group.id ||
                mutableState.value.expenseEditor.expenseId != expenseId
            ) return@launch
            val serverRevision = group.expenses.firstOrNull { it.id == expenseId }?.revision
            mutableState.update {
                it.copy(
                    expenseEditor = draft.toEditor(),
                    draftNotice = "Restored local draft",
                    conflict = if (serverRevision != null && draft.expectedRevision != serverRevision) {
                        "This draft started from revision ${draft.expectedRevision}; the server now has revision " +
                            "$serverRevision. Reload the server version or keep this draft and review it carefully."
                    } else {
                        null
                    },
                )
            }
        }
    }

    private fun scheduleExpenseDraftSave() {
        if (mutableState.value.screen != AppScreen.EXPENSE_EDITOR) return
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            delay(350)
            saveCurrentExpenseDraft()
        }
    }

    private fun persistExpenseDraftImmediately() {
        draftJob?.cancel()
        draftJob = viewModelScope.launch { saveCurrentExpenseDraft() }
    }

    private suspend fun saveCurrentExpenseDraft() {
        val state = mutableState.value
        val uid = state.user?.uid ?: return
        val group = state.selectedGroup ?: return
        val editor = state.expenseEditor
        if (!editor.hasMeaningfulDraft()) {
            expenseDraftRepository.delete(uid, group.id, editor.expenseId)
            return
        }
        expenseDraftRepository.save(editor.toDraft(uid, group.id))
        if (mutableState.value.screen == AppScreen.EXPENSE_EDITOR) {
            mutableState.update { it.copy(draftNotice = "Draft saved on this device") }
        }
    }

    private fun discardExpenseDraft() {
        val state = mutableState.value
        val uid = state.user?.uid ?: return
        val group = state.selectedGroup ?: return
        val expenseId = state.expenseEditor.expenseId
        draftJob?.cancel()
        viewModelScope.launch { expenseDraftRepository.delete(uid, group.id, expenseId) }
        if (expenseId == null) {
            mutableState.update {
                it.copy(screen = AppScreen.GROUP_DETAIL, expenseEditor = ExpenseEditorState(), draftNotice = null, conflict = null)
            }
        } else {
            reloadConflictedExpense()
        }
    }

    private fun reloadConflictedExpense() {
        val group = mutableState.value.selectedGroup ?: return
        val expenseId = mutableState.value.expenseEditor.expenseId ?: run {
            mutableState.update { it.copy(conflict = null) }
            return
        }
        val expense = group.expenses.firstOrNull { it.id == expenseId }
            ?: return error("The expense is no longer available on the server.")
        val uid = mutableState.value.user?.uid ?: return
        draftJob?.cancel()
        viewModelScope.launch { expenseDraftRepository.delete(uid, group.id, expenseId) }
        mutableState.update {
            it.copy(expenseEditor = editorFor(expense, group), conflict = null, draftNotice = "Loaded server version")
        }
    }

    private fun reloadConflict() {
        val state = mutableState.value
        when (state.conflictTarget) {
            ConflictTarget.EXPENSE -> reloadConflictedExpense()
            ConflictTarget.SETTLEMENT -> {
                val group = state.selectedGroup ?: return
                val settlement = selectedSettlement()
                    ?: return error("This repayment was removed. Return to the group's payment history.")
                mutableState.update { it.copy(
                    settlementEditor = settlementEditorFor(settlement, group),
                    settlementDeleteOperationId = newOperationId(),
                    conflict = null, conflictTarget = null, conflictActualRevision = null,
                    message = "Loaded the current repayment. Review it before saving or removing it.",
                ) }
            }
            ConflictTarget.INVITATION -> {
                val actualRevision = state.conflictActualRevision ?: return
                val invitationId = state.groupMembers.pendingRevokeInvitationId ?: return
                mutableState.update {
                    it.copy(
                        conflict = null,
                        conflictTarget = null,
                        conflictActualRevision = null,
                        message = "Invitation status refreshed. Tap revoke again to confirm.",
                        groupMembers = it.groupMembers.copy(
                            invitation = it.groupMembers.invitation?.let { invitation ->
                                if (invitation.id == invitationId) invitation.copy(revision = actualRevision) else invitation
                            },
                            invitations = it.groupMembers.invitations.map { invitation ->
                                if (invitation.id == invitationId) invitation.copy(revision = actualRevision) else invitation
                            },
                            revokeOperationId = newOperationId(),
                            pendingRevokeInvitationId = null,
                        ),
                    )
                }
            }
            ConflictTarget.GROUP, null -> {
                val groupId = state.selectedGroup?.id ?: return
                mutableState.update {
                    it.copy(conflict = null, conflictTarget = null, conflictActualRevision = null)
                }
                openGroup(groupId)
            }
        }
    }

    private fun updateAuth(block: AuthFormState.() -> AuthFormState) = mutableState.update {
        it.copy(authForm = it.authForm.block())
    }
    private fun updateCreateGroup(block: CreateGroupFormState.() -> CreateGroupFormState) = mutableState.update {
        it.copy(createGroupForm = it.createGroupForm.block())
    }
    private fun updateExpense(block: ExpenseEditorState.() -> ExpenseEditorState) {
        mutableState.update { it.copy(expenseEditor = it.expenseEditor.block()) }
        scheduleExpenseDraftSave()
    }
    private fun updateGroupMembers(block: GroupMembersState.() -> GroupMembersState) = mutableState.update {
        it.copy(groupMembers = it.groupMembers.block())
    }
    private fun updateSettlement(block: SettlementEditorState.() -> SettlementEditorState) = mutableState.update {
        it.copy(settlementEditor = it.settlementEditor.block())
    }
    private fun message(value: String) = mutableState.update { it.copy(message = value) }
    private fun error(value: String) = mutableState.update { it.copy(error = value) }
}

private fun readable(error: Throwable): String = when (error) {
    is BackendException -> when (error.errorCode) {
        "GROUP_ARCHIVED" -> "This group is archived and no longer accepts financial changes."
        "NOT_A_MEMBER" -> "You no longer have access to this group."
        "OWNER_REQUIRED" -> "Only the group owner can make this change."
        "RECORD_EDIT_FORBIDDEN" -> "Only the creator or group owner can change this record."
        "INVITATION_EXPIRED" -> "This invitation expired. Ask the owner for a new one."
        "INVITATION_REVOKED" -> "This invitation was revoked. Ask the owner for a new one."
        "INVITATION_EXHAUSTED" -> "This invitation has reached its usage limit."
        "GROUP_FULL" -> "This group has no available participant place. Ask the owner to add a participant first."
        "RATE_LIMITED" -> "Too many invitation attempts. Wait ten minutes before trying again."
        "OPERATION_ID_REUSED" -> "The server already handled an earlier version of this change. Reload before trying again."
        else -> error.message ?: "The server rejected this change."
    }
    else -> error.message ?: "Something went wrong. Try again."
}

private fun formatMinor(amountMinor: Long, exponent: Int): String =
    MoneyText.formatMinor(amountMinor, CurrencyUnit("XXX", exponent))

private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun newOperationId(): String = UUID.randomUUID().toString()

private object DefaultUserPreferencesRepository : UserPreferencesRepository {
    private val value = MutableStateFlow(ThemePreference.SYSTEM)
    override val theme = value.asStateFlow()
    override val features = MutableStateFlow(FeaturePreferences())
    override suspend fun setDefaultCurrency(value: String) { features.value = features.value.copy(defaultCurrency = value) }
    override suspend fun setSimplifyDebts(value: Boolean) { features.value = features.value.copy(simplifyDebts = value) }
    override suspend fun setBiometricLock(value: Boolean) { features.value = features.value.copy(biometricLock = value) }
    override suspend fun markNotificationsRead(uid: String, throughEpochMillis: Long) {
        features.value = features.value.copy(notificationReadAt = features.value.notificationReadAt + (uid to throughEpochMillis))
    }
    override suspend fun setTheme(value: ThemePreference) {
        this.value.value = value
    }
}

private fun ExpenseEditorState.hasMeaningfulDraft(): Boolean =
    expenseId != null || title.isNotBlank() || amount.isNotBlank() || note.isNotBlank() ||
        entryValues.values.any(String::isNotBlank)

private fun ExpenseEditorState.toDraft(ownerUid: String, groupId: String) = ExpenseDraft(
    ownerUid = ownerUid,
    groupId = groupId,
    expenseId = expenseId,
    operationId = operationId,
    expectedRevision = expectedRevision,
    title = title,
    amount = amount,
    payerParticipantId = payerParticipantId,
    selectedParticipantIds = selectedParticipantIds,
    splitMode = splitMode,
    entryValues = entryValues,
    date = date,
    category = category,
    note = note,
    updatedAtEpochMillis = System.currentTimeMillis(),
)

private fun ExpenseDraft.toEditor() = ExpenseEditorState(
    expenseId = expenseId,
    expectedRevision = expectedRevision,
    title = title,
    amount = amount,
    payerParticipantId = payerParticipantId,
    selectedParticipantIds = selectedParticipantIds,
    splitMode = splitMode,
    entryValues = entryValues,
    date = date,
    category = category,
    note = note,
    operationId = operationId,
)
