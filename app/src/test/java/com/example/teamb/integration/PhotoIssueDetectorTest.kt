package com.example.teamb.integration

import com.example.teamb.data.integration.MockPhotoIssueDetector
import com.example.teamb.data.integration.PhotoIssueCategoryMapper
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.PhotoAnalysisFailure
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoIssueDetectorTest {

    private val detector = MockPhotoIssueDetector()
    private val mapper = PhotoIssueCategoryMapper()

    @Test
    fun `detects elevator from filename`() = runTest {
        assertEquals(FeedbackCategory.ELEVATORS, detector.analyze("file://broken-elevator.jpg").suggestedCategory)
    }

    @Test
    fun `detects kitchen and desk and room`() = runTest {
        assertEquals(FeedbackCategory.KITCHEN, detector.analyze("kitchen-sink.png").suggestedCategory)
        assertEquals(FeedbackCategory.DESK_AREA, detector.analyze("my-desk.png").suggestedCategory)
        assertEquals(FeedbackCategory.MEETING_ROOMS, detector.analyze("room-3.png").suggestedCategory)
    }

    @Test
    fun `returns low confidence result when nothing recognized`() = runTest {
        val result = detector.analyze("file://IMG_1234.jpg")
        assertNull(result.suggestedCategory)
        assertTrue(result.confidence < 0.6f)
        assertEquals("Unclear facilities issue", result.detectedIssue)
    }

    @Test
    fun `suggestion includes a description`() = runTest {
        val result = detector.analyze("elevator.jpg")
        assertEquals(FeedbackCategory.ELEVATORS, result.suggestedCategory)
        assertTrue(result.description!!.isNotBlank())
    }

    @Test
    fun `returns failure state when analysis is unavailable`() = runTest {
        assertEquals(PhotoAnalysisFailure.UNAVAILABLE, detector.analyze("offline-photo.jpg").failure)
    }

    @Test
    fun `mapper suppresses category when confidence is too low`() {
        assertNull(mapper.map("Kitchen maintenance issue", 0.35f))
    }

    @Test
    fun `mapper handles ML Kit object labels`() {
        assertEquals(FeedbackCategory.DESK_AREA, mapper.map("Facilities issue: Computer", 0.8f))
        assertEquals(FeedbackCategory.KITCHEN, mapper.map("Facilities issue: Refrigerator", 0.75f))
        assertEquals(FeedbackCategory.PARKING, mapper.map("Facilities issue: Vehicle", 0.9f))
        assertEquals(FeedbackCategory.BATHROOMS, mapper.map("Facilities issue: Toilet", 0.85f))
    }
}
