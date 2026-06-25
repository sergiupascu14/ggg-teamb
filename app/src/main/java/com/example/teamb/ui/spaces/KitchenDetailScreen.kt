package com.example.teamb.ui.spaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamb.AppContainer
import com.example.teamb.data.db.FreezerItemEntity
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.FridgeOccupancy
import com.example.teamb.data.sync.Fridges
import com.example.teamb.ui.components.AppTextField
import com.example.teamb.ui.components.FieldLabel
import com.example.teamb.ui.components.GarminHeader
import com.example.teamb.ui.components.OutlinedPillButton
import com.example.teamb.ui.components.PrimaryButton
import com.example.teamb.ui.components.ScreenTitle
import com.example.teamb.ui.components.SurfaceCard
import com.example.teamb.ui.freezer.FreezerViewModel
import com.example.teamb.ui.theme.AccentBlue
import com.example.teamb.ui.theme.BrandCyan
import com.example.teamb.ui.theme.GarminBlue
import com.example.teamb.ui.theme.InputBorder
import com.example.teamb.ui.theme.Navy
import com.example.teamb.ui.theme.TextMuted
import com.example.teamb.ui.theme.TextPrimary
import com.example.teamb.ui.theme.TextSecondary
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Quick-report shortcuts shown on the Kitchen detail (Screen 4), all routed as Kitchen issues. */
private val KITCHEN_QUICK_ACTIONS = listOf(
    "Expired food", "Fridge full", "Dirty shelves", "Unknown items",
)

/**
 * Kitchen detail (Screen 4): food-storage and cleanliness insights, with the office freezer
 * check-in/out living here. Fridge occupancy is derived from the number of items checked in.
 */
@Composable
fun KitchenDetailScreen(container: AppContainer, onReport: (categoryName: String) -> Unit) {
    val vm: FreezerViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FreezerViewModel(container.freezerRepository) as T
        },
    )
    val profile by container.profileStore.profile.collectAsState(initial = null)
    val ownerId = profile?.staffId
    if (ownerId != null) vm.setOwner(ownerId)
    val scope = rememberCoroutineScope()

    val frozenItems by remember(ownerId) {
        if (ownerId != null) container.freezerRepository.observeItems(ownerId) else emptyFlow()
    }.collectAsState(initial = emptyList())

    // The two shared fridges on this user's floor, synced live across devices.
    val floorKey = Fridges.floorKey(profile?.building, profile?.floor)
    val fridges by remember(floorKey) {
        if (floorKey != null) container.fridgeRepository.observeFloor(floorKey) else emptyFlow()
    }.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GarminHeader()
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 20.dp)) {
            ScreenTitle("Kitchen", subtitle = "Food storage and cleanliness insights.")

            Box(Modifier.padding(top = 16.dp)) {
                FloorFridgesCard(
                    floorKey = floorKey,
                    fridges = fridges,
                    canEdit = ownerId != null,
                    onAdjust = { fridgeId, newOccupancy ->
                        if (floorKey != null) {
                            scope.launch {
                                container.fridgeRepository.setOccupancy(
                                    floorKey, fridgeId, newOccupancy, ownerId, container.clock.nowMillis(),
                                )
                            }
                        }
                    },
                    onReport = { onReport(FeedbackCategory.KITCHEN.name) },
                )
            }

            FieldLabel("Quick actions", modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
            QuickActions(onAction = { onReport(FeedbackCategory.KITCHEN.name) })

            Box(Modifier.padding(top = 24.dp)) {
                if (ownerId == null) {
                    SurfaceCard {
                        Text(
                            "Sign in to manage the office freezer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                } else {
                    FreezerSection(
                        items = frozenItems,
                        onCheckIn = vm::checkIn,
                        onCheckOut = { vm.checkOut(it) },
                    )
                }
            }

            Box(Modifier.padding(top = 24.dp)) { KitchenPulseCard(itemCount = frozenItems.size) }
            Box(Modifier.padding(top = 16.dp)) { YouSaidWeDidCard() }
        }
    }
}

/** The two shared fridges on the user's floor, with live occupancy synced across devices. */
@Composable
private fun FloorFridgesCard(
    floorKey: String?,
    fridges: List<FridgeOccupancy>,
    canEdit: Boolean,
    onAdjust: (fridgeId: String, newOccupancy: Int) -> Unit,
    onReport: () -> Unit,
) {
    SurfaceCard {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Fridge occupancy", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        if (floorKey != null) "Shared live with floor $floorKey" else "Set your desk to see your floor's fridges",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                OutlinedPillButton(text = "Report", onClick = onReport)
            }

            if (floorKey != null) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val display = if (fridges.isEmpty()) Fridges.normalize(emptyList()) else fridges
                    display.forEachIndexed { index, fridge ->
                        FridgeRow(
                            name = "Fridge ${index + 1}",
                            fridge = fridge,
                            canEdit = canEdit,
                            onAdjust = { delta ->
                                onAdjust(fridge.fridgeId, (fridge.occupancy + delta).coerceIn(0, 100))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FridgeRow(
    name: String,
    fridge: FridgeOccupancy,
    canEdit: Boolean,
    onAdjust: (delta: Int) -> Unit,
) {
    val barColor = when {
        fridge.occupancy >= 80 -> Navy
        fridge.occupancy >= 50 -> BrandCyan
        else -> GarminBlue
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(
                "${fridge.occupancy}%",
                style = MaterialTheme.typography.titleMedium,
                color = GarminBlue,
                fontWeight = FontWeight.Black,
            )
        }
        // Occupancy bar.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AccentBlue),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fridge.occupancy / 100f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(barColor),
            )
        }
        if (canEdit) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AdjustButton(label = "−10%", modifier = Modifier.weight(1f)) { onAdjust(-10) }
                AdjustButton(label = "+10%", modifier = Modifier.weight(1f)) { onAdjust(+10) }
            }
        }
    }
}

@Composable
private fun AdjustButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = AccentBlue,
        border = androidx.compose.foundation.BorderStroke(1.dp, InputBorder),
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = GarminBlue, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuickActions(onAction: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KITCHEN_QUICK_ACTIONS.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { label ->
                    OutlinedPillButton(text = label, onClick = onAction, modifier = Modifier.weight(1f))
                }
                if (rowItems.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FreezerSection(
    items: List<FreezerItemEntity>,
    onCheckIn: (String) -> Unit,
    onCheckOut: (Long) -> Unit,
) {
    var label by remember { mutableStateOf("") }
    SurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Office freezer", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            AppTextField(
                value = label,
                onValueChange = { label = it },
                placeholder = "What are you freezing?",
            )
            PrimaryButton(
                text = "Check in",
                onClick = { onCheckIn(label); label = "" },
                enabled = label.isNotBlank(),
            )
            if (items.isEmpty()) {
                Text(
                    "Nothing in the freezer yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            } else {
                items.forEach { item -> FreezerRow(item = item, onCheckOut = { onCheckOut(item.id) }) }
            }
        }
    }
}

@Composable
private fun FreezerRow(item: FreezerItemEntity, onCheckOut: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = AccentBlue, modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("🥚", style = MaterialTheme.typography.titleMedium) }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(
                "Since ${sinceLabel(item.checkInAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
        OutlinedPillButton(text = "Check out", onClick = onCheckOut)
    }
}

@Composable
private fun KitchenPulseCard(itemCount: Int) {
    SurfaceCard {
        Column {
            Text("Kitchen Pulse", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                "$itemCount items stored · Top issue: Fridge full",
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun YouSaidWeDidCard() {
    SurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("You said → we did", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("• Added weekly fridge cleanout reminder", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Text("• Labels restocked near pantry shelves", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        }
    }
}

/** Human-readable check-in date: "today" when same calendar day, else "MMM d, yyyy". */
private fun sinceLabel(checkInAt: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = checkInAt }
    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    return if (sameDay) "today" else SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(checkInAt)
}
