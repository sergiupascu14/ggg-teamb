package com.example.teamb.ui.onboarding

import com.example.teamb.data.datastore.CredentialStore
import com.example.teamb.data.datastore.ProfileStore
import com.example.teamb.data.desk.DeskAllocationRepository
import com.example.teamb.data.integration.DirectoryService
import com.example.teamb.data.model.DeskId
import com.example.teamb.data.model.Employee
import com.example.teamb.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The three sequential steps of the onboarding flow. */
enum class OnboardingStep { IDENTITY, LOCATION, PASSWORD }

/**
 * Immutable UI state for the onboarding flow. The composable is a thin renderer over this.
 */
data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.IDENTITY,
    // Identity step
    val query: String = "",
    val results: List<Employee> = emptyList(),
    val suggestedStaffId: String? = null,
    val selected: Employee? = null,
    // Location step
    /** Desk assigned to the selected employee in the dataset, if any. */
    val hasAssignedDesk: Boolean = false,
    val deskCode: String = "",
    val deskError: String? = null,
    val derivedDeskId: DeskId? = null,
    // Password step
    val password: String = "",
    val confirm: String = "",
    val passwordError: String? = null,
    val saving: Boolean = false,
    val completed: Boolean = false,
) {
    val canProceedFromIdentity: Boolean get() = selected != null
    val canProceedFromLocation: Boolean get() = derivedDeskId != null
}

/**
 * Plain (non-Android) onboarding logic. State exposed via [StateFlow]; suspend persistence is
 * delegated to the caller's coroutine scope through [complete].
 */
class OnboardingViewModel(
    private val desk: DeskAllocationRepository,
    private val profileStore: ProfileStore,
    private val credentialStore: CredentialStore,
    private val directory: DirectoryService,
) {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        val suggested = directory.suggestedProfile()
        _state.update {
            it.copy(
                results = desk.searchEmployees(""),
                suggestedStaffId = suggested?.staffId,
                selected = suggested,
            )
        }
    }

    fun onSearch(query: String) {
        _state.update { it.copy(query = query, results = desk.searchEmployees(query)) }
    }

    fun selectEmployee(employee: Employee) {
        _state.update { it.copy(selected = employee) }
    }

    /** Advance from the identity step. No-op if nothing is selected. */
    fun confirmIdentity() {
        val current = _state.value
        val selected = current.selected ?: return
        val assigned = desk.deskForStaff(selected.staffId)
        if (assigned != null) {
            val parsed = DeskId.parse(assigned.deskId)
            _state.update {
                it.copy(
                    step = OnboardingStep.LOCATION,
                    hasAssignedDesk = true,
                    deskCode = assigned.deskId,
                    derivedDeskId = parsed,
                    deskError = null,
                )
            }
        } else {
            _state.update {
                it.copy(
                    step = OnboardingStep.LOCATION,
                    hasAssignedDesk = false,
                    deskCode = "",
                    derivedDeskId = null,
                    deskError = null,
                )
            }
        }
    }

    /** Manual desk-code entry (only relevant when the employee has no assigned desk). */
    fun enterDeskCode(raw: String) {
        val parsed = DeskId.parse(raw)
        _state.update {
            it.copy(
                deskCode = raw,
                derivedDeskId = parsed,
                deskError = if (raw.isBlank() || parsed != null) null
                else "Enter a valid desk code (e.g. T6-C2-01).",
            )
        }
    }

    /** Advance from the location step. No-op if no valid desk id is derived. */
    fun confirmLocation() {
        if (_state.value.derivedDeskId == null) {
            _state.update { it.copy(deskError = "A valid desk is required to continue.") }
            return
        }
        _state.update { it.copy(step = OnboardingStep.PASSWORD) }
    }

    fun setPasswords(password: String, confirm: String) {
        _state.update {
            it.copy(password = password, confirm = confirm, passwordError = null)
        }
    }

    /** Returns the [UserProfile] to persist when the form is valid, or null with an error set. */
    fun buildProfile(): UserProfile? {
        val s = _state.value
        val selected = s.selected ?: return null
        val deskId = s.derivedDeskId ?: return null
        return UserProfile(
            staffId = selected.staffId,
            name = selected.name,
            supervisor = selected.supervisor,
            building = deskId.building,
            floor = deskId.floor,
            zone = deskId.zone,
            deskArea = s.deskCode.ifBlank { deskId.canonical }.trim(),
        )
    }

    /** Validates the password pair; on success sets the credential, saves the profile, marks completed. */
    suspend fun complete(): Boolean {
        val s = _state.value
        if (s.password.isBlank()) {
            _state.update { it.copy(passwordError = "Enter a password.") }
            return false
        }
        if (s.password != s.confirm) {
            _state.update { it.copy(passwordError = "Passwords do not match.") }
            return false
        }
        val profile = buildProfile()
        if (profile == null) {
            _state.update { it.copy(passwordError = "Profile is incomplete.") }
            return false
        }
        _state.update { it.copy(saving = true) }
        credentialStore.setPassword(s.password)
        credentialStore.setLoggedIn(true) // first-time users are signed in; remember it for relaunch
        profileStore.save(profile)
        _state.update { it.copy(saving = false, completed = true) }
        return true
    }

    fun back() {
        _state.update {
            when (it.step) {
                OnboardingStep.PASSWORD -> it.copy(step = OnboardingStep.LOCATION)
                OnboardingStep.LOCATION -> it.copy(step = OnboardingStep.IDENTITY)
                OnboardingStep.IDENTITY -> it
            }
        }
    }
}
