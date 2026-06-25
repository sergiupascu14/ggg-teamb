package com.example.teamb.ui.freezer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamb.AppContainer
import com.example.teamb.data.db.FreezerItemEntity
import com.example.teamb.ui.components.AppTextField
import com.example.teamb.ui.components.FieldLabel
import com.example.teamb.ui.components.GarminHeader
import com.example.teamb.ui.components.OutlinedPillButton
import com.example.teamb.ui.components.PrimaryButton
import com.example.teamb.ui.components.ScreenTitle
import com.example.teamb.ui.components.SurfaceCard
import com.example.teamb.ui.theme.AccentBlue
import com.example.teamb.ui.theme.TextMuted
import com.example.teamb.ui.theme.TextPrimary
import com.example.teamb.ui.theme.TextSecondary
import kotlinx.coroutines.flow.emptyFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun FreezerScreen(container: AppContainer) {
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

    // Re-subscribe whenever the owner becomes known.
    val frozenItems by remember(ownerId) {
        if (ownerId != null) container.freezerRepository.observeItems(ownerId) else emptyFlow()
    }.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        GarminHeader()
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 20.dp)) {
            ScreenTitle("Freezer")

            if (ownerId == null) {
                Box(Modifier.padding(top = 16.dp)) {
                    SurfaceCard {
                        Text(
                            "Sign in to use the freezer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
                return@Column
            }

            Box(Modifier.padding(top = 16.dp)) {
                CheckInForm(onCheckIn = vm::checkIn)
            }

            if (frozenItems.isEmpty()) {
                Box(Modifier.padding(top = 16.dp)) {
                    EmptyState()
                }
            } else {
                FieldLabel("In the freezer", modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    frozenItems.forEach { item ->
                        FreezerRow(item = item, onCheckOut = { vm.checkOut(item.id) })
                    }
                }
            }

            Box(Modifier.padding(top = 16.dp)) {
                StorageTipCard()
            }
        }
    }
}

@Composable
private fun CheckInForm(onCheckIn: (String) -> Unit) {
    var label by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppTextField(
            value = label,
            onValueChange = { label = it },
            placeholder = "What are you freezing?",
        )
        PrimaryButton(
            text = "Check in",
            onClick = {
                onCheckIn(label)
                label = ""
            },
            enabled = label.isNotBlank(),
        )
    }
}

@Composable
private fun FreezerRow(
    item: FreezerItemEntity,
    onCheckOut: () -> Unit,
) {
    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = AccentBlue, modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🥚", style = MaterialTheme.typography.titleLarge)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(item.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "Since ${sinceLabel(item.checkInAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
            OutlinedPillButton(text = "Check out", onClick = onCheckOut)
        }
    }
}

@Composable
private fun StorageTipCard() {
    SurfaceCard {
        Column {
            FieldLabel("Storage tip")
            Text(
                "Label items before placing them in the office freezer.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    SurfaceCard(padding = 28) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🧊", style = MaterialTheme.typography.displaySmall)
            Text(
                "Nothing in the freezer yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
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
