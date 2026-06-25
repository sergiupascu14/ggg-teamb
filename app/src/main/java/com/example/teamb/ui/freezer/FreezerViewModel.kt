package com.example.teamb.ui.freezer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamb.data.db.FreezerItemEntity
import com.example.teamb.data.repository.FreezerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

/**
 * Drives the Freezer screen for a single owner. The ownerId is resolved from the user
 * profile after it loads, so it is supplied via [setOwner] rather than the constructor.
 * Composables observe items directly from the repository (keyed by ownerId); this VM
 * owns the mutating actions and the current owner.
 */
class FreezerViewModel(
    private val repository: FreezerRepository,
) : ViewModel() {

    private val _ownerId = MutableStateFlow<String?>(null)
    val ownerId: StateFlow<String?> = _ownerId.asStateFlow()

    fun setOwner(ownerId: String) {
        if (_ownerId.value != ownerId) _ownerId.value = ownerId
    }

    /** Present items for the current owner; empty until an owner is set. */
    fun items(): Flow<List<FreezerItemEntity>> =
        _ownerId.value?.let { repository.observeItems(it) } ?: emptyFlow()

    fun checkIn(label: String) {
        val owner = _ownerId.value ?: return
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.checkIn(trimmed, owner) }
    }

    fun checkOut(id: Long) {
        viewModelScope.launch { repository.checkOut(id) }
    }
}
