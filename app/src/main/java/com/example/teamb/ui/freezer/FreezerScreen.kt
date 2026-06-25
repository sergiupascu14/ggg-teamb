package com.example.teamb.ui.freezer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.teamb.data.util.Dates
import kotlinx.coroutines.flow.emptyFlow

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
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Freezer", style = MaterialTheme.typography.headlineSmall)

        if (ownerId == null) {
            Text(
                "Sign in to use the freezer.",
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }

        CheckInForm(onCheckIn = vm::checkIn)

        if (frozenItems.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🧊", style = MaterialTheme.typography.displaySmall)
                Text(
                    "Nothing in the freezer yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(frozenItems, key = { it.id }) { item ->
                    FreezerRow(item = item, onCheckOut = { vm.checkOut(item.id) })
                }
            }
        }
    }
}

@Composable
private fun CheckInForm(onCheckIn: (String) -> Unit) {
    var label by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("What are you freezing?") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                onCheckIn(label)
                label = ""
            },
            enabled = label.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Check in")
        }
    }
}

@Composable
private fun FreezerRow(
    item: FreezerItemEntity,
    onCheckOut: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(item.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Since ${Dates.isoDate(item.checkInAt)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(onClick = onCheckOut) { Text("Check out") }
        }
    }
}
