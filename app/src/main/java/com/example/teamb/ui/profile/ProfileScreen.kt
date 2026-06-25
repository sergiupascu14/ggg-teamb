package com.example.teamb.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.teamb.AppContainer
import com.example.teamb.data.model.Building
import com.example.teamb.data.model.Reward
import com.example.teamb.data.model.UserProfile

@Composable
fun ProfileScreen(
    container: AppContainer,
    onSignOut: () -> Unit,
    onOpenTickets: () -> Unit,
    onOpenLeaderboard: () -> Unit,
) {
    val profile by container.profileStore.profile.collectAsState(initial = null)
    val staffId = profile?.staffId

    var streak by remember { mutableStateOf(0) }
    // Reward "points" = the user's public feedback count (drives the 10/50/100 tiers,
    // whose top tier is literally "Office Champion"). Derived from the leaderboard so it
    // matches what the Leaderboard screen shows.
    var points by remember { mutableStateOf(0) }
    var rewards by remember { mutableStateOf<List<Reward>>(emptyList()) }

    LaunchedEffect(staffId) {
        runCatching {
            streak = container.dailyPulseRepository.currentStreak()
            points = staffId?.let { id ->
                container.gamificationRepository.leaderboard(id)
                    .firstOrNull { it.userId == id }
                    ?.publicFeedbackCount
            } ?: 0
            rewards = container.gamificationRepository.rewardsFor(points)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            profile?.name ?: "Profile",
            style = MaterialTheme.typography.headlineSmall,
        )

        profile?.let { ProfileDetails(it) }

        StatCard(label = "Daily Pulse streak", value = "$streak 🔥")

        if (rewards.isNotEmpty()) {
            Text("Rewards", style = MaterialTheme.typography.titleMedium)
            rewards.forEach { RewardRow(it) }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = onOpenTickets, modifier = Modifier.fillMaxWidth()) {
                Text("My Tickets")
            }
            Button(onClick = onOpenLeaderboard, modifier = Modifier.fillMaxWidth()) {
                Text("Leaderboard")
            }
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text("Sign out")
            }
        }
    }
}

@Composable
private fun ProfileDetails(profile: UserProfile) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val buildingLabel = Building.fromCode(profile.building)?.label
                ?: profile.building.ifBlank { "—" }
            DetailRow("Desk area", profile.deskArea.ifBlank { "—" })
            DetailRow("Building", buildingLabel)
            DetailRow("Floor", if (profile.floor > 0) "${profile.floor}" else "—")
            DetailRow("Zone", profile.zone.ifBlank { "—" })
            DetailRow("Supervisor", profile.supervisor.ifBlank { "—" })
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun RewardRow(reward: Reward) {
    val containerColor =
        if (reward.unlocked) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(reward.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${reward.threshold} public posts",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(if (reward.unlocked) "Unlocked ✓" else "🔒")
        }
    }
}
