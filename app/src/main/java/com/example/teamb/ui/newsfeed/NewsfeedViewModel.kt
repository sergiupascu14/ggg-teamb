package com.example.teamb.ui.newsfeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamb.data.community.CommunityRepository
import com.example.teamb.data.desk.DeskAllocationRepository
import com.example.teamb.data.model.Building
import com.example.teamb.data.model.CommunityFeedback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A row in the newsfeed: the raw feedback plus its locally-resolved submitter name. */
data class NewsfeedRow(
    val item: CommunityFeedback,
    val displayName: String,
)

/** Active location filter; null fields mean "no constraint". */
data class NewsfeedFilter(
    val buildingCode: String? = null,
    val floor: Int? = null,
)

data class NewsfeedUiState(
    val rows: List<NewsfeedRow> = emptyList(),
    val filter: NewsfeedFilter = NewsfeedFilter(),
    val buildings: List<Building> = emptyList(),
    val floorOptions: List<Int> = emptyList(),
    val currentUserId: String? = null,
)

/**
 * Drives the Community Newsfeed. Observes the shared community feed, resolves submitter
 * names locally from the desk dataset, and applies a client-side building/floor filter.
 * The composable stays thin: it renders [state] and calls [toggleVote]/filter mutators.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NewsfeedViewModel(
    private val community: CommunityRepository,
    private val desk: DeskAllocationRepository,
) : ViewModel() {

    private val currentUserId = MutableStateFlow<String?>(null)
    private val filter = MutableStateFlow(NewsfeedFilter())

    private val feed = currentUserId.flatMapLatest { uid ->
        community.observeFeedback(uid)
    }

    val state: StateFlow<NewsfeedUiState> =
        combine(feed, filter, currentUserId) { items, f, uid ->
            val filtered = items.filter { matches(it, f) }
            NewsfeedUiState(
                rows = filtered.map { NewsfeedRow(it, resolveName(it.userId)) },
                filter = f,
                buildings = desk.buildings(),
                floorOptions = f.buildingCode?.let { desk.floorsFor(it) } ?: emptyList(),
                currentUserId = uid,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, NewsfeedUiState())

    fun setCurrentUserId(userId: String?) {
        if (currentUserId.value != userId) currentUserId.value = userId
    }

    /** Picking a building resets the floor (floors differ per building). */
    fun selectBuilding(code: String?) {
        filter.value = NewsfeedFilter(buildingCode = code, floor = null)
    }

    fun selectFloor(floor: Int?) {
        filter.value = filter.value.copy(floor = floor)
    }

    fun clearFilter() {
        filter.value = NewsfeedFilter()
    }

    /** No-op when there is no signed-in user (anonymous users cannot vote). */
    fun toggleVote(itemId: String) {
        val voter = currentUserId.value ?: return
        viewModelScope.launch { community.toggleVote(itemId, voter) }
    }

    private fun matches(item: CommunityFeedback, f: NewsfeedFilter): Boolean {
        if (f.buildingCode != null && item.building != f.buildingCode) return false
        if (f.floor != null && item.floor != f.floor) return false
        return true
    }

    private fun resolveName(userId: String?): String =
        desk.displayName(userId) ?: "Anonymous"
}
