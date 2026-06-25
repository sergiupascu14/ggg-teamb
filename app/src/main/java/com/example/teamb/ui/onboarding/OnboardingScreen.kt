package com.example.teamb.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.teamb.AppContainer
import com.example.teamb.data.model.Employee
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

    LaunchedEffect(state.completed) {
        if (state.completed) onCompleted()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Welcome to TeamB Office", style = MaterialTheme.typography.headlineSmall)
        when (state.step) {
            OnboardingStep.IDENTITY -> IdentityStep(state, vm)
            OnboardingStep.LOCATION -> LocationStep(state, vm)
            OnboardingStep.PASSWORD -> PasswordStep(state, vm, scope)
        }
    }
}

@Composable
private fun IdentityStep(state: OnboardingUiState, vm: OnboardingViewModel) {
    Text("Who are you?", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = state.query,
        onValueChange = vm::onSearch,
        label = { Text("Search by name or staff id") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    val listState = rememberLazyListState()
    // Pre-scroll to the directory's suggested employee, if present in the current results.
    LaunchedEffect(state.suggestedStaffId, state.results) {
        val id = state.suggestedStaffId ?: return@LaunchedEffect
        val index = state.results.indexOfFirst { it.staffId == id }
        if (index >= 0) listState.scrollToItem(index)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth().height(360.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(state.results, key = { it.staffId }) { emp ->
            EmployeeRow(
                employee = emp,
                selected = state.selected?.staffId == emp.staffId,
                suggested = state.suggestedStaffId == emp.staffId,
                onClick = { vm.selectEmployee(emp) },
            )
        }
    }

    Button(
        onClick = vm::confirmIdentity,
        enabled = state.canProceedFromIdentity,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Continue") }
}

@Composable
private fun EmployeeRow(
    employee: Employee,
    selected: Boolean,
    suggested: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(employee.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                buildString {
                    append(employee.staffId)
                    if (suggested) append("  (suggested)")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LocationStep(state: OnboardingUiState, vm: OnboardingViewModel) {
    Text("Your desk", style = MaterialTheme.typography.titleMedium)
    val derived = state.derivedDeskId
    if (state.hasAssignedDesk && derived != null) {
        Text("We found your assigned desk:", style = MaterialTheme.typography.bodyMedium)
        Text(derived.canonical, style = MaterialTheme.typography.headlineSmall)
        Text(
            "Building ${derived.building} · Floor ${derived.floor} · Zone ${derived.zone}",
            style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        Text("Enter your desk code to set your location.", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = state.deskCode,
            onValueChange = vm::enterDeskCode,
            label = { Text("Desk code (e.g. T6-C2-01)") },
            singleLine = true,
            isError = state.deskError != null,
            modifier = Modifier.fillMaxWidth(),
        )
        state.deskError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        derived?.let {
            Text(
                "Building ${it.building} · Floor ${it.floor} · Zone ${it.zone}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = vm::back) { Text("Back") }
        Button(
            onClick = vm::confirmLocation,
            enabled = state.canProceedFromLocation,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continue") }
    }
}

@Composable
private fun PasswordStep(
    state: OnboardingUiState,
    vm: OnboardingViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    Text("Create a password", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = state.password,
        onValueChange = { vm.setPasswords(it, state.confirm) },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.confirm,
        onValueChange = { vm.setPasswords(state.password, it) },
        label = { Text("Confirm password") },
        singleLine = true,
        isError = state.passwordError != null,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
    state.passwordError?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = vm::back, enabled = !state.saving) { Text("Back") }
        Button(
            onClick = { scope.launch { vm.complete() } },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Finish") }
    }
}

@Composable
fun LoginScreen(container: AppContainer, onUnlocked: () -> Unit) {
    val vm = remember { LoginViewModel(container.credentialStore) }
    val state by vm.state.collectAsState()

    LaunchedEffect(state.unlocked) {
        if (state.unlocked) onUnlocked()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Sign in", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = vm::onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            isError = state.error != null,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = vm::submit, modifier = Modifier.fillMaxWidth()) {
            Text("Unlock")
        }
    }
}
