package com.example.teamb.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.teamb.AppContainer
import com.example.teamb.R
import com.example.teamb.data.model.Employee
import com.example.teamb.ui.components.AppTextField
import com.example.teamb.ui.components.FieldLabel
import com.example.teamb.ui.components.GarminLogo
import com.example.teamb.ui.components.OutlinedPillButton
import com.example.teamb.ui.components.PrimaryButton
import com.example.teamb.ui.components.SurfaceCard
import com.example.teamb.ui.theme.AccentBlue
import com.example.teamb.ui.theme.Canvas
import com.example.teamb.ui.theme.CardBorder
import com.example.teamb.ui.theme.CardSurface
import com.example.teamb.ui.theme.GarminBlue
import com.example.teamb.ui.theme.InputBorder
import com.example.teamb.ui.theme.IssueText
import com.example.teamb.ui.theme.Navy
import com.example.teamb.ui.theme.OnBrand
import com.example.teamb.ui.theme.TextMuted
import com.example.teamb.ui.theme.TextPrimary
import com.example.teamb.ui.theme.TextSecondary
import com.example.teamb.ui.util.toDisplayName
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(container: AppContainer, onCompleted: () -> Unit) {
    val vm = remember {
        OnboardingViewModel(
            desk = container.desk,
            profileStore = container.profileStore,
            credentialStore = container.credentialStore,
            directory = container.directory,
        )
    }
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.completed) { if (state.completed) onCompleted() }

    val stepIndex = when (state.step) {
        OnboardingStep.IDENTITY -> 1
        OnboardingStep.LOCATION -> 2
        OnboardingStep.PASSWORD -> 3
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 20.dp),
    ) {
        GarminLogo()
        StepDots(stepIndex, modifier = Modifier.padding(top = 20.dp))
        Text(
            "Welcome to CLOOJ",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(16.dp))
        when (state.step) {
            OnboardingStep.IDENTITY -> IdentityStep(state, vm, Modifier.weight(1f))
            OnboardingStep.LOCATION -> LocationStep(state, vm)
            OnboardingStep.PASSWORD -> PasswordStep(state, vm, scope)
        }
    }
}

@Composable
private fun StepDots(step: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            Box(
                Modifier
                    .padding(end = 6.dp)
                    .size(if (i + 1 == step) 22.dp else 8.dp, 8.dp)
                    .background(if (i + 1 <= step) GarminBlue else InputBorder, RoundedCornerShape(4.dp))
            )
        }
        Text(
            "Step $step of 3",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun IdentityStep(state: OnboardingUiState, vm: OnboardingViewModel, modifier: Modifier = Modifier) {
    Text("Who are you?", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    Spacer(Modifier.height(10.dp))
    AppTextField(state.query, vm::onSearch, "Search by name or staff id")

    val listState = rememberLazyListState()
    LaunchedEffect(state.suggestedStaffId, state.results) {
        val id = state.suggestedStaffId ?: return@LaunchedEffect
        val index = state.results.indexOfFirst { it.staffId == id }
        if (index >= 0) listState.scrollToItem(index)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth().padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.results, key = { it.staffId }) { emp ->
            EmployeeRow(emp, state.selected?.staffId == emp.staffId, state.suggestedStaffId == emp.staffId) {
                vm.selectEmployee(emp)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    PrimaryButton("Continue", vm::confirmIdentity, enabled = state.canProceedFromIdentity)
}

@Composable
private fun EmployeeRow(employee: Employee, selected: Boolean, suggested: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) AccentBlue else CardSurface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) GarminBlue else CardBorder),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(employee.name.toDisplayName(), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(
                if (suggested) "${employee.staffId} · suggested" else employee.staffId,
                style = MaterialTheme.typography.labelSmall,
                color = if (suggested) GarminBlue else TextMuted,
            )
        }
    }
}

@Composable
private fun LocationStep(state: OnboardingUiState, vm: OnboardingViewModel) {
    Text("Your desk", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    Spacer(Modifier.height(12.dp))
    val derived = state.derivedDeskId
    SurfaceCard {
        Column {
            if (state.hasAssignedDesk && derived != null) {
                Text("We found your assigned desk", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(derived.canonical, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, modifier = Modifier.padding(vertical = 6.dp))
                Text(
                    "Building ${derived.building} · Floor ${derived.floor} · Zone ${derived.zone}",
                    style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
                )
            } else {
                Text("Enter your desk code to set your location.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                AppTextField(state.deskCode, vm::enterDeskCode, "Desk code (e.g. T6-C2-01)", isError = state.deskError != null)
                state.deskError?.let { Text(it, color = IssueText, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp)) }
                derived?.let {
                    Text(
                        "Building ${it.building} · Floor ${it.floor} · Zone ${it.zone}",
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(20.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = vm::back) { Text("Back", color = GarminBlue) }
        PrimaryButton("Continue", vm::confirmLocation, modifier = Modifier.weight(1f), enabled = state.canProceedFromLocation)
    }
}

@Composable
private fun PasswordStep(state: OnboardingUiState, vm: OnboardingViewModel, scope: kotlinx.coroutines.CoroutineScope) {
    Text("Create a password", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    Spacer(Modifier.height(12.dp))
    SurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(state.password, { vm.setPasswords(it, state.confirm) }, "Password", visualTransformation = PasswordVisualTransformation())
            AppTextField(state.confirm, { vm.setPasswords(state.password, it) }, "Confirm password", isError = state.passwordError != null, visualTransformation = PasswordVisualTransformation())
            state.passwordError?.let { Text(it, color = IssueText, style = MaterialTheme.typography.labelSmall) }
        }
    }
    Spacer(Modifier.height(20.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = vm::back, enabled = !state.saving) { Text("Back", color = GarminBlue) }
        PrimaryButton(if (state.saving) "Saving…" else "Finish", { scope.launch { vm.complete() } }, modifier = Modifier.weight(1f), enabled = !state.saving)
    }
}

@Composable
fun LoginScreen(container: AppContainer, onUnlocked: () -> Unit) {
    val vm = remember { LoginViewModel(container.credentialStore) }
    val state by vm.state.collectAsState()

    LaunchedEffect(state.unlocked) { if (state.unlocked) onUnlocked() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.clooj_logo),
            contentDescription = "CLOOJ",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(28.dp)),
        )
        Spacer(Modifier.height(20.dp))
        Box { GarminLogo() }
        Text("CLOOJ", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
        Text("Welcome back — sign in to continue.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        SurfaceCard {
            Column {
                AppTextField(
                    state.password, vm::onPasswordChange, "Password",
                    isError = state.error != null, visualTransformation = PasswordVisualTransformation(),
                )
                state.error?.let { Text(it, color = IssueText, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp)) }
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Unlock", vm::submit)
            }
        }
    }
}
