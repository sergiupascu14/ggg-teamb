package com.example.teamb.ui.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.teamb.AppContainer
import com.example.teamb.data.model.LeaderboardEntry

@Composable
fun LeaderboardScreen(container: AppContainer) {
    val profile by container.profileStore.profile.collectAsState(initial = null)
    val currentUserId = profile?.staffId

    var entries by remember { mutableStateOf<List<LeaderboardEntry>?>(null) }
    LaunchedEffect(currentUserId) {
        entries = container.gamificationRepository.leaderboard(currentUserId)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Leaderboard", style = MaterialTheme.typography.headlineSmall)

        val rows = entries
        when {
            rows == null -> Unit // loading; render nothing until the first result lands
            rows.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🏆", style = MaterialTheme.typography.displaySmall)
                Text(
                    "No public feedback yet — be the first!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEachIndexed { index, entry ->
                    LeaderboardRow(rank = index + 1, entry = entry)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, entry: LeaderboardEntry) {
    val containerColor =
        if (entry.isCurrentUser) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$rank",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.displayName + if (entry.isCurrentUser) " (you)" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (entry.isOfficeChampion) {
                    Text(
                        "Office Champion 👑",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(
                "${entry.publicFeedbackCount}",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}
