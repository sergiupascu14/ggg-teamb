package com.example.teamb.ui.dailypulse

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamb.AppContainer
import com.example.teamb.ui.components.AppTextField
import com.example.teamb.ui.components.GarminHeader
import com.example.teamb.ui.components.InfoBanner
import com.example.teamb.ui.components.OutlinedPillButton
import com.example.teamb.ui.components.PrimaryButton
import com.example.teamb.ui.components.ScreenTitle
import com.example.teamb.ui.components.SurfaceCard
import com.example.teamb.ui.theme.AccentBlue
import com.example.teamb.ui.theme.CardSurface
import com.example.teamb.ui.theme.GarminBlue
import com.example.teamb.ui.theme.InputBorder
import com.example.teamb.ui.theme.TextMuted
import com.example.teamb.ui.theme.TextPrimary
import com.example.teamb.ui.theme.TextSecondary

private val MOODS = listOf("😞", "🙁", "😐", "🙂", "😄")

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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { },
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val streakLabel = "🔥 ${state.streak} ${if (state.streak == 1) "day" else "days"}"

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        GarminHeader()
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 20.dp)) {
            ScreenTitle("Daily Pulse", streakLabel)

            Box(Modifier.padding(top = 16.dp)) {
                when {
                    state.loading -> SurfaceCard {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = GarminBlue)
                        }
                    }
                    state.checkedInToday -> CheckedInCard()
                    else -> PulseForm(submitting = state.submitting, onSubmit = vm::submit)
                }
            }

            Box(Modifier.padding(top = 16.dp)) {
                InfoBanner(
                    if (state.checkedInToday) "Today's entry is saved. Keep the streak going."
                    else "Daily Pulse helps your workplace team spot trends. Your note is always optional."
                )
            }
        }
    }
}

@Composable
private fun PulseForm(submitting: Boolean, onSubmit: (Int, String?) -> Unit) {
    var mood by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
    SurfaceCard {
        Column {
            Text("How are you feeling today?", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                "Choose the mood that best matches your day.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MOODS.forEachIndexed { index, emoji ->
                    val value = index + 1
                    val selected = mood == value
                    Surface(
                        modifier = Modifier.size(48.dp).selectable(selected) { mood = value },
                        shape = CircleShape,
                        color = if (selected) AccentBlue else CardSurface,
                        border = BorderStroke(if (selected) 2.dp else 1.5.dp, if (selected) GarminBlue else InputBorder),
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = MaterialTheme.typography.titleLarge.fontSize) }
                    }
                }
            }
            Box(Modifier.padding(top = 18.dp)) {
                AppTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "Add a note (optional)",
                    singleLine = false,
                    minLines = 3,
                )
            }
            Box(Modifier.padding(top = 16.dp)) {
                PrimaryButton(
                    text = if (submitting) "Submitting…" else "Submit",
                    onClick = { onSubmit(mood, note.ifBlank { null }) },
                    enabled = mood in 1..5 && !submitting,
                )
            }
        }
    }
}

@Composable
private fun CheckedInCard() {
    SurfaceCard(padding = 28) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Success badge: blue check inside white tile inside accent square.
            Surface(
                modifier = Modifier.size(96.dp).padding(top = 12.dp),
                shape = RoundedCornerShape(28.dp),
                color = AccentBlue,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(shape = CircleShape, color = GarminBlue, modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = CardSurface, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
            Text(
                "You've checked in today.",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                "See you tomorrow!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            Box(Modifier.padding(top = 20.dp, bottom = 8.dp)) {
                OutlinedPillButton(text = "View history", onClick = { }, modifier = Modifier.fillMaxWidth(0.6f))
            }
        }
    }
}
