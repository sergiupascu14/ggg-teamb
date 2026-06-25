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
    fun `correct password unlocks`() {
        val vm = LoginViewModel(store("secret"))
        vm.onPasswordChange("secret")
        vm.submit()
        assertTrue(vm.state.value.unlocked)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `wrong password shows error and stays locked`() {
        val vm = LoginViewModel(store("secret"))
        vm.onPasswordChange("nope")
        vm.submit()
        assertFalse(vm.state.value.unlocked)
        assertEquals("Incorrect password.", vm.state.value.error)
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
