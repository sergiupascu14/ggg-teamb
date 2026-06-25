package com.example.teamb.ui.newsfeed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamb.AppContainer
import com.example.teamb.data.model.Building
import com.example.teamb.data.model.FeedbackSentiment
import com.example.teamb.ui.components.GarminHeader
import com.example.teamb.ui.components.ScreenTitle
import com.example.teamb.ui.components.SurfaceCard
import com.example.teamb.ui.components.Tag
import com.example.teamb.ui.theme.AccentBlue
import com.example.teamb.ui.theme.CardBorder
import com.example.teamb.ui.theme.CardSurface
import com.example.teamb.ui.theme.GarminBlue
import com.example.teamb.ui.theme.IssueBg
import com.example.teamb.ui.theme.IssueText
import com.example.teamb.ui.theme.PositiveBg
import com.example.teamb.ui.theme.PositiveText
import com.example.teamb.ui.theme.TextMuted
import com.example.teamb.ui.theme.TextPrimary
import com.example.teamb.ui.theme.TextSecondary
import com.example.teamb.ui.util.toDisplayName

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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        GarminHeader()
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 20.dp)) {
            ScreenTitle("Community")

            Box(Modifier.padding(top = 16.dp)) {
                FilterChips(
                    buildings = state.buildings,
                    selectedBuildingCode = state.filter.buildingCode,
                    floorOptions = state.floorOptions,
                    selectedFloor = state.filter.floor,
                    onSelectBuilding = vm::selectBuilding,
                    onSelectFloor = vm::selectFloor,
                    onClear = vm::clearFilter,
                )
            }

            if (state.rows.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("📭", style = MaterialTheme.typography.displaySmall)
                    Text(
                        "No community feedback yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.rows.forEach { row ->
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
}

@Composable
private fun FilterChips(
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val buildingLabel =
            buildings.firstOrNull { it.code == selectedBuildingCode }?.label ?: "Building"
        FilterChip(
            label = buildingLabel,
            selected = selectedBuildingCode != null,
        ) { dismiss ->
            DropdownMenuItem(
                text = { Text("All buildings") },
                onClick = { onSelectBuilding(null); dismiss() },
            )
            buildings.forEach { b ->
                DropdownMenuItem(
                    text = { Text(b.label) },
                    onClick = { onSelectBuilding(b.code); dismiss() },
                )
            }
        }

        FilterChip(
            label = selectedFloor?.let { "Floor $it" } ?: "Floor",
            selected = selectedFloor != null,
            enabled = selectedBuildingCode != null,
        ) { dismiss ->
            DropdownMenuItem(
                text = { Text("All floors") },
                onClick = { onSelectFloor(null); dismiss() },
            )
            floorOptions.forEach { f ->
                DropdownMenuItem(
                    text = { Text("Floor $f") },
                    onClick = { onSelectFloor(f); dismiss() },
                )
            }
        }

        if (selectedBuildingCode != null || selectedFloor != null) {
            Text(
                "Clear",
                style = MaterialTheme.typography.labelLarge,
                color = GarminBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onClear() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    menuContent: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (selected) AccentBlue else CardSurface,
            border = BorderStroke(
                1.4.dp,
                if (selected) GarminBlue else CardBorder,
            ),
            modifier = Modifier.clickable(enabled = enabled) { expanded = true },
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                color = when {
                    !enabled -> TextMuted
                    selected -> GarminBlue
                    else -> TextSecondary
                },
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            menuContent { expanded = false }
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
    val positive = item.sentiment == FeedbackSentiment.POSITIVE
    val tagBg = if (positive) PositiveBg else IssueBg
    val tagFg = if (positive) PositiveText else IssueText
    val sentimentWord = if (positive) "Positive" else "Issue"
    // "Anonymous" stays as-is; real dataset names get title-cased.
    val displayName =
        if (row.displayName == "Anonymous") row.displayName else row.displayName.toDisplayName()

    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tag(item.category.label, bg = tagBg, fg = tagFg)
                    Text(
                        sentimentWord,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
                Text(
                    item.message,
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    subtitle(item.building, item.floor, displayName),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }

            VotePill(
                votedByMe = item.votedByMe,
                votes = item.votes,
                enabled = canVote,
                onVote = onVote,
            )
        }
    }
}

@Composable
private fun VotePill(votedByMe: Boolean, votes: Int, enabled: Boolean, onVote: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardSurface,
        border = BorderStroke(1.4.dp, GarminBlue),
        modifier = Modifier
            .padding(start = 8.dp)
            .clickable(enabled = enabled, onClick = onVote),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (votedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Vote",
                tint = GarminBlue,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "$votes",
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = GarminBlue,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** "<building><floor> · Floor <floor> · <name>", dropping the location parts when unknown. */
private fun subtitle(building: String?, floor: Int?, name: String): String {
    val head = buildString {
        if (building != null) append(building)
        if (floor != null) append(floor)
    }
    return buildList {
        if (head.isNotEmpty()) add(head)
        if (floor != null) add("Floor $floor")
        add(name)
    }.joinToString(" · ")
}
