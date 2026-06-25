package com.example.teamb.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.teamb.AppContainer
import com.example.teamb.data.model.Building
import com.example.teamb.data.model.Reward
import com.example.teamb.data.model.UserProfile
import com.example.teamb.ui.components.FieldLabel
import com.example.teamb.ui.components.GarminLogo
import com.example.teamb.ui.components.OutlinedPillButton
import com.example.teamb.ui.components.PrimaryButton
import com.example.teamb.ui.components.SurfaceCard
import com.example.teamb.ui.theme.AccentBlue
import com.example.teamb.ui.theme.CardBorder
import com.example.teamb.ui.theme.CardSurface
import com.example.teamb.ui.theme.GarminBlue
import com.example.teamb.ui.theme.GarminBlueLight
import com.example.teamb.ui.theme.TextMuted
import com.example.teamb.ui.theme.TextPrimary
import com.example.teamb.ui.util.toDisplayName

@Composable
fun ProfileScreen(
    container: AppContainer,
    onSignOut: () -> Unit,
    onOpenTickets: () -> Unit,
    onOpenLeaderboard: () -> Unit,
) {
    val profile by container.profileStore.profile.collectAsState(initial = null)
    val staffId = profile?.staffId

    var streak by remember { mutableIntStateOf(0) }
    // Reward "points" = the user's public feedback count (drives the 10/50/100 tiers,
    // whose top tier is literally "Office Champion"). Derived from the leaderboard so it
    // matches what the Leaderboard screen shows.
    var points by remember { mutableIntStateOf(0) }
    var rewards by remember { mutableStateOf<List<Reward>>(emptyList()) }

    LaunchedEffect(staffId) {
        streak = container.dailyPulseRepository.currentStreak()
        points = staffId?.let { id ->
            container.gamificationRepository.leaderboard(id)
                .firstOrNull { it.userId == id }
                ?.publicFeedbackCount
        } ?: 0
        rewards = container.gamificationRepository.rewardsFor(points)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        GradientHeader(name = profile?.name ?: "Profile")

        Column(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 20.dp)) {
            profile?.let { WorkplaceInfoCard(it) }

            Box(Modifier.padding(top = 16.dp)) {
                StreakCard(streak = streak)
            }

            Box(Modifier.padding(top = 20.dp)) {
                FieldLabel("Rewards")
            }
            if (rewards.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rewards.take(3).forEach { reward ->
                        RewardCard(reward, modifier = Modifier.weight(1f))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrimaryButton("My Tickets", onOpenTickets, Modifier.weight(1f))
                OutlinedPillButton("Leaderboard", onOpenLeaderboard, Modifier.weight(1f))
            }

            Box(Modifier.padding(top = 12.dp)) {
                OutlinedPillButton("Sign out", onSignOut, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GradientHeader(name: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GarminBlue,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        shadowElevation = 1.dp,
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(listOf(GarminBlue, GarminBlueLight)),
            ),
        ) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)) {
                GarminLogo(onDark = true)
                Row(
                    modifier = Modifier.padding(top = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(shape = CircleShape, color = CardSurface, modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("👤", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Text(
                        name.toDisplayName(),
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        color = CardSurface,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkplaceInfoCard(profile: UserProfile) {
    val buildingLabel = Building.fromCode(profile.building)?.label
        ?: profile.building.ifBlank { "—" }
    SurfaceCard {
        Column(Modifier.fillMaxWidth()) {
            Text(
                "Workplace info",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    InfoPair("Desk area", profile.deskArea.ifBlank { "—" })
                    InfoPair("Building", buildingLabel)
                    InfoPair("Floor", if (profile.floor > 0) "${profile.floor}" else "—")
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    InfoPair("Zone", profile.zone.ifBlank { "—" })
                    InfoPair(
                        "Supervisor",
                        profile.supervisor.ifBlank { "—" }.let {
                            if (it == "—") it else it.toDisplayName()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoPair(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun StreakCard(streak: Int) {
    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = AccentBlue, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🔥", style = MaterialTheme.typography.titleMedium)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    "Daily Pulse streak",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    "$streak ${if (streak == 1) "day" else "days"} strong",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                "$streak",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun RewardCard(reward: Reward, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, CardBorder),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (reward.unlocked) "🏅" else "🔒",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                reward.title,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
