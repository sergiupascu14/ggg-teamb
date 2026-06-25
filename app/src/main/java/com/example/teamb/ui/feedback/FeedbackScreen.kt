package com.example.teamb.ui.feedback

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.teamb.AppContainer
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.data.model.FeedbackSentiment
import com.example.teamb.data.model.PhotoCategorizationResult
import com.example.teamb.ui.components.AppTextField
import com.example.teamb.ui.components.FieldLabel
import com.example.teamb.ui.components.GarminHeader
import com.example.teamb.ui.components.OutlinedPillButton
import com.example.teamb.ui.components.PrimaryButton
import com.example.teamb.ui.components.ScreenTitle
import com.example.teamb.ui.components.SurfaceCard
import com.example.teamb.ui.theme.AccentBlue
import com.example.teamb.ui.theme.CardBorder
import com.example.teamb.ui.theme.CardSurface
import com.example.teamb.ui.theme.GarminBlue
import com.example.teamb.ui.theme.InputBorder
import com.example.teamb.ui.theme.InputFill
import com.example.teamb.ui.theme.IssueText
import com.example.teamb.ui.theme.TextMuted
import com.example.teamb.ui.theme.TextPrimary
import com.example.teamb.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.io.File

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
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

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
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            val uri = pendingCameraUri
            pendingCameraUri = null
            vm.onPhotoPicked(if (success) uri?.toString() else null)
        },
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun launchCameraCapture() {
        val uri = createCameraImageUri(context)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                launchCameraCapture()
            } else {
                pendingCameraUri = null
                scope.launch { snackbarHostState.showSnackbar("Camera permission is required to take a photo") }
            }
        },
    )

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

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            GarminHeader()
            Column(
                Modifier.padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ScreenTitle("Share Feedback")

                SentimentSelector(
                    selected = state.form.sentiment,
                    onSelect = vm::setSentiment,
                )

                Column {
                    FieldLabel("Category", modifier = Modifier.padding(bottom = 6.dp))
                    CategoryDropdown(
                        selected = state.form.category,
                        isError = state.error == "Please choose a category",
                        onSelect = vm::setCategory,
                    )
                }

                // AI-detected issue label (editable) — shown once a photo is attached/analyzed.
                if (state.form.photoUri != null || !state.form.issueLabel.isNullOrBlank()) {
                    Column {
                        FieldLabel("Detected issue", modifier = Modifier.padding(bottom = 6.dp))
                        AppTextField(
                            value = state.form.issueLabel.orEmpty(),
                            onValueChange = vm::setIssueLabel,
                            placeholder = "What's the issue?",
                        )
                    }
                }

                Column {
                    FieldLabel("Feedback", modifier = Modifier.padding(bottom = 6.dp))
                    AppTextField(
                        value = state.form.message,
                        onValueChange = vm::setMessage,
                        placeholder = "Tell us what happened or what you enjoyed…",
                        singleLine = false,
                        minLines = 3,
                        isError = state.error == "Please describe your feedback",
                    )
                    state.error?.let { error ->
                        Text(
                            error,
                            color = IssueText,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                        )
                    }
                }

                // Gallery + camera capture (camera feeds the on-device AI categorizer).
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedPillButton(
                        text = "Attach photo",
                        onClick = {
                            photoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        modifier = Modifier.weight(1f),
                        leadingIcon = Icons.Filled.PhotoLibrary,
                    )
                    OutlinedPillButton(
                        text = "Take photo",
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                launchCameraCapture()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        leadingIcon = Icons.Filled.PhotoCamera,
                    )
                }

                state.form.photoUri?.let { uri ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Attached photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(16.dp)),
                        )
                        OutlinedPillButton(text = "Remove", onClick = { vm.onPhotoPicked(null) })
                    }
                }

                PhotoDraftCard(status = state.photoDraftStatus, draft = state.photoDraft)

                ToggleRow("Submit anonymously", state.form.anonymous, vm::setAnonymous)
                ToggleRow("Share with the community", state.form.communityVisible, vm::setCommunityVisible)
                ToggleRow("Create a ticket", state.form.wantsTicket, vm::setWantsTicket)

                AppTextField(
                    value = state.form.location ?: "",
                    onValueChange = vm::setLocation,
                    placeholder = "Location (optional)",
                )

                val analyzing = state.photoDraftStatus == PhotoDraftStatus.ANALYZING
                PrimaryButton(
                    text = when {
                        state.submitting -> "Submitting…"
                        analyzing -> "Generating draft…"
                        else -> "Submit feedback"
                    },
                    onClick = { vm.submit(currentUserId) },
                    enabled = !state.submitting && !analyzing,
                )
            }
        }
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** Styled summary of the on-device photo categorization draft. */
@Composable
private fun PhotoDraftCard(status: PhotoDraftStatus, draft: PhotoCategorizationResult?) {
    if (status == PhotoDraftStatus.IDLE) return
    val headline = when (status) {
        PhotoDraftStatus.ANALYZING -> "Analyzing your photo…"
        PhotoDraftStatus.READY -> "Draft generated from your photo"
        PhotoDraftStatus.LOW_CONFIDENCE -> "Draft generated — please review the category"
        PhotoDraftStatus.UNAVAILABLE -> "Couldn't analyze the photo — continue manually"
        PhotoDraftStatus.IDLE -> return
    }
    SurfaceCard(padding = 16) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(headline, style = MaterialTheme.typography.titleSmall, color = GarminBlue)
            draft?.detectedIssue?.let {
                Text("Issue: $it", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            draft?.suggestedCategory?.let {
                Text("Suggested category: ${it.label}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            if (draft != null && status != PhotoDraftStatus.UNAVAILABLE && status != PhotoDraftStatus.ANALYZING) {
                Text(
                    "Confidence: ${(draft.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
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
        SentimentPill(
            label = "👍 Positive",
            selected = selected == FeedbackSentiment.POSITIVE,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(FeedbackSentiment.POSITIVE) },
        )
        SentimentPill(
            label = "⚠️ Issue",
            selected = selected == FeedbackSentiment.ISSUE,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(FeedbackSentiment.ISSUE) },
        )
    }
}

@Composable
private fun SentimentPill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) GarminBlue else CardSurface,
        border = if (selected) null else BorderStroke(1.4.dp, InputBorder),
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) CardSurface else TextSecondary,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: FeedbackCategory?,
    isError: Boolean,
    onSelect: (FeedbackCategory) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        // Read-only anchor styled to the Garmin input spec (AppTextField can't be readOnly).
        Surface(
            modifier = Modifier.fillMaxWidth().height(56.dp).menuAnchor(),
            shape = RoundedCornerShape(16.dp),
            color = InputFill,
            border = BorderStroke(1.2.dp, if (isError) IssueText else InputBorder),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    selected?.label ?: "Select a category",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected == null) TextMuted else TextPrimary,
                )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextMuted,
                )
            }
        }
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
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, CardBorder),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = GarminBlue),
            )
        }
    }
}

private fun createCameraImageUri(context: Context): Uri {
    val photosDir = File(context.cacheDir, "feedback-photos").apply { mkdirs() }
    val imageFile = File(photosDir, "feedback-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile,
    )
}
