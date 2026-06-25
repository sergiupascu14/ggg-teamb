package com.example.teamb.onboarding

import com.example.teamb.data.desk.DeskAllocationRepository
import com.example.teamb.data.model.Desk
import com.example.teamb.data.model.Employee
import com.example.teamb.ui.onboarding.OnboardingStep
import com.example.teamb.ui.onboarding.OnboardingViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingViewModelTest {

    private val alice = Employee("1001", "Alice Anderson", "active", "Carol Chief")
    private val bob = Employee("1002", "Bob Brown", "active", "Carol Chief")
    private val dave = Employee("3003", "Dave Deskless", "active", "Carol Chief")
    private val employees = listOf(alice, bob, dave)

    private val desks = listOf(
        Desk("T6-C2-01", "T", 6, "C", 2, "01", "1001"),
        Desk("R3-A1-05", "R", 3, "A", 1, "05", "1002"),
        // dave (3003) has no desk
    )

    private fun repo() = DeskAllocationRepository(employees, desks)

    private fun vm(
        directory: FakeDirectoryService = FakeDirectoryService(all = employees),
        profileStore: FakeProfileStore = FakeProfileStore(),
        credentialStore: FakeCredentialStore = FakeCredentialStore(),
    ) = OnboardingViewModel(repo(), profileStore, credentialStore, directory)

    @Test
    fun startsOnIdentityWithAllEmployees() {
        val vm = vm()
        val s = vm.state.value
        assertEquals(OnboardingStep.IDENTITY, s.step)
        assertEquals(employees, s.results)
        assertNull(s.selected)
        assertFalse(s.canProceedFromIdentity)
    }

    @Test
    fun suggestedProfilePreselectsAndHighlights() {
        val vm = vm(directory = FakeDirectoryService(suggested = bob, all = employees))
        val s = vm.state.value
        assertEquals(bob, s.selected)
        assertEquals("1002", s.suggestedStaffId)
        assertTrue(s.canProceedFromIdentity)
    }

    @Test
    fun searchFiltersResults() {
        val vm = vm()
        vm.onSearch("alice")
        assertEquals(listOf(alice), vm.state.value.results)
        vm.onSearch("1002")
        assertEquals(listOf(bob), vm.state.value.results)
        vm.onSearch("nobody")
        assertTrue(vm.state.value.results.isEmpty())
        vm.onSearch("")
        assertEquals(employees, vm.state.value.results)
    }

    @Test
    fun selectionRequiredToProceed() {
        val vm = vm()
        assertFalse(vm.state.value.canProceedFromIdentity)
        vm.confirmIdentity() // no-op without a selection
        assertEquals(OnboardingStep.IDENTITY, vm.state.value.step)

        vm.selectEmployee(alice)
        assertTrue(vm.state.value.canProceedFromIdentity)
        vm.confirmIdentity()
        assertEquals(OnboardingStep.LOCATION, vm.state.value.step)
    }

    @Test
    fun deskPrefilledFromSelectedEmployee() {
        val vm = vm()
        vm.selectEmployee(alice)
        vm.confirmIdentity()
        val s = vm.state.value
        assertTrue(s.hasAssignedDesk)
        assertEquals("T6-C2-01", s.deskCode)
        assertNotNull(s.derivedDeskId)
        assertEquals("T", s.derivedDeskId!!.building)
        assertEquals(6, s.derivedDeskId!!.floor)
        assertEquals("C", s.derivedDeskId!!.zone)
        assertTrue(s.canProceedFromLocation)
    }

    @Test
    fun deskMissingRequiresManualEntryValidation() {
        val vm = vm()
        vm.selectEmployee(dave)
        vm.confirmIdentity()
        var s = vm.state.value
        assertFalse(s.hasAssignedDesk)
        assertEquals("", s.deskCode)
        assertNull(s.derivedDeskId)
        assertFalse(s.canProceedFromLocation)

        // Invalid code -> error, blocked.
        vm.enterDeskCode("R6-A1-01")
        s = vm.state.value
        assertNotNull(s.deskError)
        assertNull(s.derivedDeskId)
        assertFalse(s.canProceedFromLocation)
        vm.confirmLocation()
        assertEquals(OnboardingStep.LOCATION, vm.state.value.step)

        // Valid code -> derives location, unblocked.
        vm.enterDeskCode("t4-b1-09")
        s = vm.state.value
        assertNull(s.deskError)
        assertEquals("T4-B1-09", s.derivedDeskId!!.canonical)
        assertTrue(s.canProceedFromLocation)
        vm.confirmLocation()
        assertEquals(OnboardingStep.PASSWORD, vm.state.value.step)
    }

    @Test
    fun passwordMismatchBlocksAndDoesNotPersist() = runTest {
        val profileStore = FakeProfileStore()
        val credentialStore = FakeCredentialStore()
        val vm = vm(profileStore = profileStore, credentialStore = credentialStore)
        vm.selectEmployee(alice)
        vm.confirmIdentity()
        vm.confirmLocation()

        vm.setPasswords("secret1", "secret2")
        val ok = vm.complete()
        assertFalse(ok)
        assertNotNull(vm.state.value.passwordError)
        assertFalse(vm.state.value.completed)
        assertNull(profileStore.saved)
        assertFalse(credentialStore.hasPassword())
        assertFalse(credentialStore.isLoggedIn())
    }

    @Test
    fun blankPasswordBlocks() = runTest {
        val vm = vm()
        vm.selectEmployee(alice)
        vm.confirmIdentity()
        vm.confirmLocation()
        vm.setPasswords("", "")
        assertFalse(vm.complete())
        assertNotNull(vm.state.value.passwordError)
    }

    @Test
    fun happyPathSavesProfileAndSetsPassword() = runTest {
        val profileStore = FakeProfileStore()
        val credentialStore = FakeCredentialStore()
        val vm = vm(profileStore = profileStore, credentialStore = credentialStore)

        vm.selectEmployee(alice)
        vm.confirmIdentity()
        vm.confirmLocation()
        vm.setPasswords("matching", "matching")
        val ok = vm.complete()

        assertTrue(ok)
        assertTrue(vm.state.value.completed)
        assertTrue(credentialStore.verify("matching"))
        assertTrue(credentialStore.isLoggedIn()) // first-time users are remembered for auto-login

        val saved = profileStore.saved
        assertNotNull(saved)
        assertEquals("1001", saved!!.staffId)
        assertEquals("Alice Anderson", saved.name)
        assertEquals("Carol Chief", saved.supervisor)
        assertEquals("T", saved.building)
        assertEquals(6, saved.floor)
        assertEquals("C", saved.zone)
        assertEquals("T6-C2-01", saved.deskArea)
        assertTrue(saved.isComplete)
    }

    @Test
    fun happyPathWithManualDeskBuildsProfile() = runTest {
        val profileStore = FakeProfileStore()
        val vm = vm(profileStore = profileStore)
        vm.selectEmployee(dave)
        vm.confirmIdentity()
        vm.enterDeskCode("R5-D2-07")
        vm.confirmLocation()
        vm.setPasswords("pw", "pw")
        assertTrue(vm.complete())

        val saved = profileStore.saved!!
        assertEquals("3003", saved.staffId)
        assertEquals("R", saved.building)
        assertEquals(5, saved.floor)
        assertEquals("D", saved.zone)
        assertEquals("R5-D2-07", saved.deskArea)
    }

    @Test
    fun backNavigatesThroughSteps() {
        val vm = vm()
        vm.selectEmployee(alice)
        vm.confirmIdentity()
        vm.confirmLocation()
        assertEquals(OnboardingStep.PASSWORD, vm.state.value.step)
        vm.back()
        assertEquals(OnboardingStep.LOCATION, vm.state.value.step)
        vm.back()
        assertEquals(OnboardingStep.IDENTITY, vm.state.value.step)
        vm.back() // no-op at first step
        assertEquals(OnboardingStep.IDENTITY, vm.state.value.step)
    }
}
