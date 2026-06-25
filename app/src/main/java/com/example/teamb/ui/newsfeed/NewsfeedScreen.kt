package com.example.teamb.ui.newsfeed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.teamb.AppContainer
import com.example.teamb.data.model.Building
import com.example.teamb.data.model.FeedbackSentiment

@Composable
fun NewsfeedScreen(container: AppContainer) {
    val vm: NewsfeedViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                NewsfeedViewModel(container.community, container.desk) as T
        },
    )

    val profile by container.profileStore.profile.collectAsState(initial = null)
    val currentUserId = profile?.staffId
    vm.setCurrentUserId(currentUserId)

    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Community", style = MaterialTheme.typography.headlineSmall)

        FilterBar(
            buildings = state.buildings,
            selectedBuildingCode = state.filter.buildingCode,
            floorOptions = state.floorOptions,
            selectedFloor = state.filter.floor,
            onSelectBuilding = vm::selectBuilding,
            onSelectFloor = vm::selectFloor,
            onClear = vm::clearFilter,
        )

        if (state.rows.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("📭", style = MaterialTheme.typography.displaySmall)
                Text(
                    "No community feedback yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.rows, key = { it.item.id }) { row ->
                    FeedbackCard(
                        row = row,
                        canVote = currentUserId != null,
                        onVote = { vm.toggleVote(row.item.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBar(
    buildings: List<Building>,
    selectedBuildingCode: String?,
    floorOptions: List<Int>,
    selectedFloor: Int?,
    onSelectBuilding: (String?) -> Unit,
    onSelectFloor: (Int?) -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val buildingLabel =
            buildings.firstOrNull { it.code == selectedBuildingCode }?.label ?: "Building"
        Dropdown(
            label = buildingLabel,
            options = buildOptions(buildings),
            onSelect = { onSelectBuilding(it) },
        )

        Dropdown(
            label = selectedFloor?.let { "Floor $it" } ?: "Floor",
            enabled = selectedBuildingCode != null,
            options = buildFloorOptions(floorOptions),
            onSelect = { onSelectFloor(it) },
        )

        if (selectedBuildingCode != null || selectedFloor != null) {
            TextButton(onClick = onClear) { Text("Clear") }
        }
    }
}

private data class Option<T>(val label: String, val value: T?)

private fun buildOptions(buildings: List<Building>): List<Option<String>> =
    listOf(Option<String>("All buildings", null)) +
        buildings.map { Option(it.label, it.code) }

private fun buildFloorOptions(floors: List<Int>): List<Option<Int>> =
    listOf(Option<Int>("All floors", null)) + floors.map { Option("Floor $it", it) }

@Composable
private fun <T> Dropdown(
    label: String,
    options: List<Option<T>>,
    enabled: Boolean = true,
    onSelect: (T?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled) {
            Text(label)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option.value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FeedbackCard(
    row: NewsfeedRow,
    canVote: Boolean,
    onVote: () -> Unit,
) {
    val item = row.item
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item.category.label, style = MaterialTheme.typography.titleMedium)
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(
                            if (item.sentiment == FeedbackSentiment.POSITIVE) "👍 Positive"
                            else "⚠️ Issue",
                        )
                    },
                )
            }

            Text(item.message, style = MaterialTheme.typography.bodyMedium)

            locationLabel(item.building, item.floor, item.location)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }

            if (item.photoRef != null) {
                AsyncImage(
                    model = item.photoRef,
                    contentDescription = "Feedback photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.displayName, style = MaterialTheme.typography.labelMedium)
                OutlinedButton(onClick = onVote, enabled = canVote) {
                    Icon(
                        imageVector = if (item.votedByMe) Icons.Filled.ThumbUp
                        else Icons.Outlined.ThumbUp,
                        contentDescription = "Vote",
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "  ${item.votes}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

private fun locationLabel(building: String?, floor: Int?, location: String?): String? {
    val buildingLabel = building?.let { Building.fromCode(it)?.label ?: it }
    val parts = buildList {
        if (buildingLabel != null) add(buildingLabel)
        if (floor != null) add("Floor $floor")
    }
    return when {
        parts.isNotEmpty() -> parts.joinToString(" · ")
        else -> location
    }
}
