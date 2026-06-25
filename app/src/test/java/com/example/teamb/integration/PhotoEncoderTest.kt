package com.example.teamb.integration

import com.example.teamb.data.integration.NoopPhotoEncoder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoEncoderTest {

    @Test
    fun `noop encoder always returns null`() = runTest {
        val encoder = NoopPhotoEncoder()
        assertNull(encoder.encode("content://anything.jpg"))
        assertNull(encoder.encode(""))
    }
}
