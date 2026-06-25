package com.example.teamb.ui.dailypulse

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamb.AppContainer
import com.example.teamb.data.repository.DailyPulseRepository

private val MOODS = listOf("😢", "🙁", "😐", "🙂", "😄")

@Composable
fun DailyPulseScreen(container: AppContainer) {
    val vm: DailyPulseViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DailyPulseViewModel(container.dailyPulseRepository) as T
        },
    )
    val state by vm.state.collectAsState()

    // Silently request notification permission once (API 33+). Denial must not crash or block.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* degrade gracefully; reminders simply won't fire if denied */ },
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Daily Pulse", style = MaterialTheme.typography.headlineSmall)
        Text(
            "🔥 ${state.streak} ${if (state.streak == 1) "day" else "days"}",
            style = MaterialTheme.typography.titleMedium,
        )

        when {
            state.loading -> CircularProgressIndicator()
            state.checkedInToday -> CheckedInState()
            else -> PulseForm(
                submitting = state.submitting,
                onSubmit = { mood, note -> vm.submit(mood, note) },
            )
        }
    }
}

@Composable
private fun CheckedInState() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("✅", style = MaterialTheme.typography.displaySmall)
        Text(
            "You've checked in today. See you tomorrow!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PulseForm(
    submitting: Boolean,
    onSubmit: (mood: Int, note: String?) -> Unit,
) {
    var mood by remember { mutableIntStateOf(0) } // 0 == none selected
    var note by remember { mutableStateOf("") }

    Text("How are you feeling today?", style = MaterialTheme.typography.bodyLarge)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        MOODS.forEachIndexed { index, emoji ->
            val value = index + 1
            val selected = mood == value
            val mod = Modifier.size(56.dp).clip(CircleShape)
            if (selected) {
                Button(onClick = { mood = value }, modifier = mod) { Text(emoji) }
            } else {
                OutlinedButton(onClick = { mood = value }, modifier = mod) { Text(emoji) }
            }
        }
    }

    OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("Add a note (optional)") },
        modifier = Modifier.fillMaxWidth(),
    )

    FilledTonalButton(
        onClick = { onSubmit(mood, note.ifBlank { null }) },
        enabled = mood in 1..5 && !submitting,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (submitting) "Submitting…" else "Submit")
    }
}
