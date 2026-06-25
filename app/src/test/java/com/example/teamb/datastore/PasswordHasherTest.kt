package com.example.teamb.datastore

import com.example.teamb.data.datastore.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class PasswordHasherTest {

    @Test
    fun hashIsDeterministicForSameSalt() {
        val salt = "00112233445566778899aabbccddeeff"
        assertEquals(
            PasswordHasher.hash("hunter2", salt),
            PasswordHasher.hash("hunter2", salt),
        )
    }

    @Test
    fun verifyTrueForCorrectPassword() {
        val salt = PasswordHasher.newSalt(SecureRandom().apply { setSeed(42L) })
        val hash = PasswordHasher.hash("correct horse", salt)
        assertTrue(PasswordHasher.verify("correct horse", salt, hash))
    }

    @Test
    fun verifyFalseForWrongPassword() {
        val salt = "ffeeddccbbaa99887766554433221100"
        val hash = PasswordHasher.hash("right", salt)
        assertFalse(PasswordHasher.verify("wrong", salt, hash))
    }

    @Test
    fun differentSaltsProduceDifferentHashes() {
        val saltA = "0000000000000000000000000000000a"
        val saltB = "0000000000000000000000000000000b"
        assertNotEquals(
            PasswordHasher.hash("samepw", saltA),
            PasswordHasher.hash("samepw", saltB),
        )
    }

    @Test
    fun newSaltIsThirtyTwoHexChars() {
        val salt = PasswordHasher.newSalt()
        assertEquals(32, salt.length)
        assertTrue(salt.all { it in "0123456789abcdef" })
    }

    @Test
    fun newSaltIsRandomAcrossCalls() {
        assertNotEquals(PasswordHasher.newSalt(), PasswordHasher.newSalt())
    }

    @Test
    fun hashIsSha256HexLength() {
        // SHA-256 -> 32 bytes -> 64 hex chars.
        assertEquals(64, PasswordHasher.hash("x", "ab").length)
    }
}
