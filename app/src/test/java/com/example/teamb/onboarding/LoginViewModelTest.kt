package com.example.teamb.onboarding

import com.example.teamb.ui.onboarding.LoginViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginViewModelTest {

    private fun store(password: String?) = FakeCredentialStore().apply {
        if (password != null) setPassword(password)
    }

    @Test
    fun `blank password shows error`() {
        val vm = LoginViewModel(store("secret"))
        vm.submit()
        assertEquals("Enter your password.", vm.state.value.error)
        assertFalse(vm.state.value.unlocked)
    }

    @Test
    fun `correct password unlocks and remembers the session`() {
        val credentials = store("secret")
        val vm = LoginViewModel(credentials)
        vm.onPasswordChange("secret")
        vm.submit()
        assertTrue(vm.state.value.unlocked)
        assertNull(vm.state.value.error)
        assertTrue(credentials.isLoggedIn()) // auto-login flag persisted
    }

    @Test
    fun `wrong password shows error, stays locked, and does not remember the session`() {
        val credentials = store("secret")
        val vm = LoginViewModel(credentials)
        vm.onPasswordChange("nope")
        vm.submit()
        assertFalse(vm.state.value.unlocked)
        assertEquals("Incorrect password.", vm.state.value.error)
        assertFalse(credentials.isLoggedIn())
    }

    @Test
    fun `editing password clears previous error`() {
        val vm = LoginViewModel(store("secret"))
        vm.onPasswordChange("nope")
        vm.submit()
        vm.onPasswordChange("s")
        assertNull(vm.state.value.error)
    }
}
