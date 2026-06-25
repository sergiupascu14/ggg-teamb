package com.example.teamb.integration

import com.example.teamb.data.integration.MockPhotoIssueDetector
import com.example.teamb.data.model.FeedbackCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoIssueDetectorTest {

    private val detector = MockPhotoIssueDetector()

    @Test
    fun `detects elevator from filename`() = runTest {
        assertEquals(FeedbackCategory.ELEVATORS, detector.analyze("file://broken-elevator.jpg")?.category)
    }

    @Test
    fun `detects kitchen and desk and room`() = runTest {
        assertEquals(FeedbackCategory.KITCHEN, detector.analyze("kitchen-sink.png")?.category)
        assertEquals(FeedbackCategory.DESK_AREA, detector.analyze("my-desk.png")?.category)
        assertEquals(FeedbackCategory.MEETING_ROOMS, detector.analyze("room-3.png")?.category)
    }

    @Test
    fun `returns null when nothing recognized`() = runTest {
        assertNull(detector.analyze("file://IMG_1234.jpg"))
    }

    @Test
    fun `suggestion includes a description`() = runTest {
        val s = detector.analyze("elevator.jpg")
        assertEquals(FeedbackCategory.ELEVATORS, s?.category)
        assert(s!!.description.isNotBlank())
    }
}
