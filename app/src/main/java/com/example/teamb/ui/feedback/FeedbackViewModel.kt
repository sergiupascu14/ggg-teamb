package com.example.teamb.ui.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamb.data.integration.PhotoIssueDetector
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.FeedbackSentiment
import com.example.teamb.data.model.PhotoSuggestion
import com.example.teamb.data.repository.FeedbackForm
import com.example.teamb.data.repository.FeedbackRepository
import com.example.teamb.data.repository.SubmitResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the feedback form. [form] is the live editable form; the rest is presentation. */
data class FeedbackUiState(
    val form: FeedbackForm = FeedbackForm(
        category = FeedbackCategory.OTHER,
        sentiment = FeedbackSentiment.POSITIVE,
        message = "",
    ),
    val error: String? = null,
    val submitting: Boolean = false,
    val suggestion: PhotoSuggestion? = null,
    val result: SubmitResult? = null,
)

/**
 * Drives [FeedbackScreen]. Holds the editable [FeedbackForm] in a [StateFlow] and runs photo
 * analysis + submission off the UI thread. Photo analysis is best-effort and never blocks submit.
 */
class FeedbackViewModel(
    private val repository: FeedbackRepository,
    private val photoDetector: PhotoIssueDetector,
) : ViewModel() {

    private val _state = MutableStateFlow(FeedbackUiState())
    val state: StateFlow<FeedbackUiState> = _state.asStateFlow()

    fun setSentiment(sentiment: FeedbackSentiment) =
        updateForm { it.copy(sentiment = sentiment) }

    fun setCategory(category: FeedbackCategory) =
        updateForm { it.copy(category = category) }

    fun setMessage(message: String) {
        _state.update { it.copy(form = it.form.copy(message = message), error = null) }
    }

    fun setAnonymous(anonymous: Boolean) =
        updateForm { it.copy(anonymous = anonymous) }

    fun setLocation(location: String) =
        updateForm { it.copy(location = location.ifBlank { null }) }

    fun setCommunityVisible(visible: Boolean) =
        updateForm { it.copy(communityVisible = visible) }

    fun setWantsTicket(wantsTicket: Boolean) =
        updateForm { it.copy(wantsTicket = wantsTicket) }

    /** Prefills building/floor (and a default location) from the user's profile. */
    fun prefillFromProfile(building: String?, floor: Int?, location: String?) {
        updateForm { current ->
            current.copy(
                building = building ?: current.building,
                floor = floor ?: current.floor,
                location = current.location ?: location,
            )
        }
    }

    /** Records the picked photo and kicks off best-effort, non-blocking analysis. */
    fun onPhotoPicked(uri: String?) {
        updateForm { it.copy(photoUri = uri) }
        _state.update { it.copy(suggestion = null) }
        if (uri == null) return
        viewModelScope.launch {
            val suggestion = runCatching { photoDetector.analyze(uri) }.getOrNull()
            if (suggestion != null && _state.value.form.photoUri == uri) {
                _state.update { it.copy(suggestion = suggestion) }
            }
        }
    }

    /** Applies the suggested category and dismisses the chip. */
    fun applySuggestion() {
        val suggestion = _state.value.suggestion ?: return
        _state.update { it.copy(form = it.form.copy(category = suggestion.category), suggestion = null) }
    }

    fun dismissSuggestion() {
        _state.update { it.copy(suggestion = null) }
    }

    /** Clears the one-shot submit result after the UI has shown it. */
    fun consumeResult() {
        _state.update { it.copy(result = null) }
    }

    /** Validates and submits. On success, resets the form and exposes the [SubmitResult]. */
    fun submit(currentUserId: String) {
        if (_state.value.submitting) return
        val form = _state.value.form
        val error = repository.validate(form)
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val result = repository.submit(form, currentUserId)
            _state.value = FeedbackUiState(
                form = FeedbackForm(
                    category = FeedbackCategory.OTHER,
                    sentiment = FeedbackSentiment.POSITIVE,
                    message = "",
                    building = form.building,
                    floor = form.floor,
                ),
                submitting = false,
                result = result,
            )
        }
    }

    private fun updateForm(transform: (FeedbackForm) -> FeedbackForm) {
        _state.update { it.copy(form = transform(it.form)) }
    }
}
