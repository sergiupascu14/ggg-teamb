package com.example.teamb.data.integration

import android.content.Context
import android.net.Uri
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.PhotoAnalysisFailure
import com.example.teamb.data.model.PhotoCategorizationResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * On-device image analysis using Google ML Kit image labeling.
 * Maps detected labels to office feedback categories.
 */
class MlKitPhotoIssueDetector(
    private val context: Context,
    private val mapper: PhotoIssueCategoryMapper = PhotoIssueCategoryMapper(categoryConfidenceThreshold = 0.4f),
) : PhotoIssueDetector {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build()
    )

    override suspend fun analyze(photoUri: String): PhotoCategorizationResult {
        val uri = Uri.parse(photoUri)
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            return PhotoCategorizationResult(failure = PhotoAnalysisFailure.UNAVAILABLE)
        }

        val labels: List<com.google.mlkit.vision.label.ImageLabel> = withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine<List<com.google.mlkit.vision.label.ImageLabel>> { cont ->
                labeler.process(image)
                    .addOnSuccessListener { result -> cont.resume(result) }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
            }
        } ?: return PhotoCategorizationResult(failure = PhotoAnalysisFailure.TIMEOUT)

        if (labels.isEmpty()) {
            return PhotoCategorizationResult(
                detectedIssue = "No objects recognized",
                description = "The image could not be classified. Please describe the issue manually.",
                confidence = 0f,
            )
        }

        val topLabel = labels.maxBy { it.confidence }
        val issueLabel = mapLabelToIssue(topLabel.text)
        val confidence = topLabel.confidence
        val allLabelNames = labels.sortedByDescending { it.confidence }.take(3).joinToString(", ") { it.text }

        return PhotoCategorizationResult(
            detectedIssue = issueLabel,
            description = "Detected: $allLabelNames. Review and adjust the category if needed.",
            suggestedCategory = mapper.map(issueLabel, confidence),
            confidence = confidence,
        )
    }

    /** Maps ML Kit's generic object labels to office-specific issue descriptions. */
    private fun mapLabelToIssue(label: String): String {
        val lower = label.lowercase()
        return when {
            lower in ELEVATOR_LABELS -> "Elevator access issue"
            lower in KITCHEN_LABELS -> "Kitchen maintenance issue"
            lower in DESK_LABELS -> "Desk area issue"
            lower in ROOM_LABELS -> "Meeting room issue"
            lower in BATHROOM_LABELS -> "Bathroom maintenance issue"
            lower in PARKING_LABELS -> "Parking issue"
            lower in TEMPERATURE_LABELS -> "Temperature / A/C issue"
            else -> "Facilities issue: $label"
        }
    }

    private companion object {
        val ELEVATOR_LABELS = setOf("elevator", "lift", "escalator")
        val KITCHEN_LABELS = setOf(
            "kitchen", "sink", "coffee", "food", "microwave", "refrigerator",
            "coffee maker", "cup", "mug", "drink", "countertop", "tableware",
            "kitchen appliance", "kitchen utensil", "cookware and bakeware",
        )
        val DESK_LABELS = setOf(
            "desk", "chair", "office", "table", "computer", "monitor", "keyboard",
            "laptop", "mouse", "furniture", "office supplies", "office equipment",
            "personal computer", "output device", "peripheral",
        )
        val ROOM_LABELS = setOf(
            "meeting", "room", "conference", "whiteboard", "projector",
            "presentation", "display device",
        )
        val BATHROOM_LABELS = setOf(
            "bathroom", "restroom", "toilet", "dryer", "hand dryer", "faucet",
            "soap", "plumbing fixture", "tap", "bathroom accessory",
        )
        val PARKING_LABELS = setOf(
            "parking", "car", "vehicle", "automobile", "motor vehicle",
            "parking lot", "wheel", "tire", "bumper",
        )
        val TEMPERATURE_LABELS = setOf(
            "air conditioning", "ac", "thermometer", "vent", "heater",
            "hvac", "mechanical fan",
        )
    }
}
