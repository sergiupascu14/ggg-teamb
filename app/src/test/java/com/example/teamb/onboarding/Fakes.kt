package com.example.teamb.onboarding

import com.example.teamb.data.datastore.CredentialStore
import com.example.teamb.data.datastore.ProfileStore
import com.example.teamb.data.integration.DirectoryService
import com.example.teamb.data.model.Employee
import com.example.teamb.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [ProfileStore] backed by a [MutableStateFlow]. */
class FakeProfileStore : ProfileStore {
    private val _profile = MutableStateFlow<UserProfile?>(null)
    override val profile: Flow<UserProfile?> = _profile.asStateFlow()

    val saved: UserProfile? get() = _profile.value

    override suspend fun save(profile: UserProfile) {
        _profile.value = profile
    }

    override suspend fun clear() {
        _profile.value = null
    }
}

/** In-memory [CredentialStore] storing the password in plaintext (test-only). */
class FakeCredentialStore : CredentialStore {
    var storedPassword: String? = null
        private set

    override fun hasPassword(): Boolean = storedPassword != null

    override fun setPassword(password: String) {
        storedPassword = password
    }

    override fun verify(password: String): Boolean = storedPassword == password

    override fun clear() {
        storedPassword = null
    }
}

/** Directory that suggests a fixed staff id (or none). */
class FakeDirectoryService(
    private val suggested: Employee? = null,
    private val all: List<Employee> = emptyList(),
) : DirectoryService {
    override fun suggestedProfile(): Employee? = suggested
    override fun allEmployees(): List<Employee> = all
}
