package com.example.teamb.ui.feedback

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.teamb.AppContainer
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.FeedbackSentiment
import kotlinx.coroutines.launch

@Composable
fun FeedbackScreen(container: AppContainer) {
    val vm: FeedbackViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FeedbackViewModel(container.feedbackRepository, container.photoDetector) as T
        },
    )
    val state by vm.state.collectAsState()
    val profile by container.profileStore.profile.collectAsState(initial = null)

    // Prefill building/floor/location from the local profile once it's available.
    LaunchedEffect(profile) {
        profile?.let { p ->
            vm.prefillFromProfile(p.building.ifBlank { null }, p.floor.takeIf { it > 0 }, p.deskArea.ifBlank { null })
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> vm.onPhotoPicked(uri?.toString()) },
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Surface the one-shot submit result as a snackbar, then clear it.
    LaunchedEffect(state.result) {
        val result = state.result ?: return@LaunchedEffect
        val message = when {
            result.ticketSuppressed -> "Thanks for the positive feedback! (no ticket needed)"
            result.ticketCreated -> "Ticket ${result.ticketExternalId} created"
            else -> "Feedback submitted"
        }
        scope.launch { snackbarHostState.showSnackbar(message) }
        vm.consumeResult()
    }

    val currentUserId = profile?.staffId ?: "anonymous"

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Share Feedback", style = MaterialTheme.typography.headlineSmall)

            SentimentSelector(
                selected = state.form.sentiment,
                onSelect = vm::setSentiment,
            )

            CategoryDropdown(
                selected = state.form.category,
                onSelect = vm::setCategory,
            )

            OutlinedTextField(
                value = state.form.message,
                onValueChange = vm::setMessage,
                label = { Text("Your feedback") },
                isError = state.error != null,
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            PhotoSection(
                photoUri = state.form.photoUri,
                onPick = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemove = { vm.onPhotoPicked(null) },
            )

            state.suggestion?.let { suggestion ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(
                        onClick = vm::applySuggestion,
                        label = { Text("Suggested: ${suggestion.category.label} — ${suggestion.description}") },
                    )
                    TextButton(onClick = vm::dismissSuggestion) { Text("Dismiss") }
                }
                Button(onClick = vm::applySuggestion) { Text("Apply") }
            }

            ToggleRow(
                label = "Submit anonymously",
                checked = state.form.anonymous,
                onCheckedChange = vm::setAnonymous,
            )

            OutlinedTextField(
                value = state.form.location ?: "",
                onValueChange = vm::setLocation,
                label = { Text("Location (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            ToggleRow(
                label = "Share with the community",
                checked = state.form.communityVisible,
                onCheckedChange = vm::setCommunityVisible,
            )

            ToggleRow(
                label = "Create a ticket",
                checked = state.form.wantsTicket,
                onCheckedChange = vm::setWantsTicket,
            )

            FilledTonalButton(
                onClick = { vm.submit(currentUserId) },
                enabled = !state.submitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.submitting) "Submitting…" else "Submit")
            }
        }
    }
}

@Composable
private fun SentimentSelector(
    selected: FeedbackSentiment,
    onSelect: (FeedbackSentiment) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SentimentButton("👍 Positive", selected == FeedbackSentiment.POSITIVE) {
            onSelect(FeedbackSentiment.POSITIVE)
        }
        SentimentButton("⚠️ Issue", selected == FeedbackSentiment.ISSUE) {
            onSelect(FeedbackSentiment.ISSUE)
        }
    }
}

@Composable
private fun SentimentButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: FeedbackCategory,
    onSelect: (FeedbackCategory) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            FeedbackCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.label) },
                    onClick = {
                        onSelect(category)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PhotoSection(
    photoUri: String?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Attached photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            OutlinedButton(onClick = onRemove) { Text("Remove") }
        } else {
            OutlinedButton(onClick = onPick) { Text("Attach photo") }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
