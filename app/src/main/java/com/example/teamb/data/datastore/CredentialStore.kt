package com.example.teamb.data.datastore

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

/** Pure, testable salted-hash logic. No plaintext password is ever stored. */
object PasswordHasher {
    fun newSalt(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.toHex()
    }

    fun hash(password: String, saltHex: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(saltHex.hexToBytes())
        return md.digest(password.toByteArray(Charsets.UTF_8)).toHex()
    }

    /** Constant-time-ish comparison of a candidate against the stored salt+hash. */
    fun verify(candidate: String, saltHex: String, expectedHashHex: String): Boolean =
        hash(candidate, saltHex) == expectedHashHex

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

/** Stores the account password securely (salted hash) and verifies it. */
interface CredentialStore {
    fun hasPassword(): Boolean
    fun setPassword(password: String)
    fun verify(password: String): Boolean

    /**
     * Whether a logged-in session is remembered, so the app can skip the lock screen on relaunch.
     * Set after a successful unlock / onboarding; cleared by [clear] on sign out.
     */
    fun isLoggedIn(): Boolean
    fun setLoggedIn(loggedIn: Boolean)

    fun clear()
}

class EncryptedCredentialStore(context: Context) : CredentialStore {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "teamb_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun hasPassword(): Boolean = prefs.contains(KEY_HASH)

    override fun setPassword(password: String) {
        val salt = PasswordHasher.newSalt()
        prefs.edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, PasswordHasher.hash(password, salt))
            .apply()
    }

    override fun verify(password: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val hash = prefs.getString(KEY_HASH, null) ?: return false
        return PasswordHasher.verify(password, salt, hash)
    }

    override fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    override fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_SALT = "salt"
        const val KEY_HASH = "hash"
        const val KEY_LOGGED_IN = "logged_in"
    }
}
