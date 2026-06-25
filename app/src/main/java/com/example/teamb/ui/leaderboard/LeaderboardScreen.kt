package com.example.teamb.ui.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
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
import com.example.teamb.ui.components.GarminHeader
import com.example.teamb.ui.components.ScreenTitle
import com.example.teamb.ui.components.SurfaceCard
import com.example.teamb.ui.theme.AccentBlue
import com.example.teamb.ui.theme.CardBorder
import com.example.teamb.ui.theme.CardSurface
import com.example.teamb.ui.theme.GarminBlue
import com.example.teamb.ui.theme.Navy
import com.example.teamb.ui.theme.OnBrand
import com.example.teamb.ui.theme.TextMuted
import com.example.teamb.ui.theme.TextPrimary
import com.example.teamb.ui.util.toDisplayName

@Composable
fun LeaderboardScreen(container: AppContainer) {
    val profile by container.profileStore.profile.collectAsState(initial = null)
    val currentUserId = profile?.staffId

    var entries by remember { mutableStateOf<List<LeaderboardEntry>?>(null) }
    LaunchedEffect(currentUserId) {
        entries = container.gamificationRepository.leaderboard(currentUserId)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        GarminHeader()
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 20.dp)) {
            ScreenTitle("Leaderboard", subtitle = "Recognizing helpful contributions.")

            val rows = entries
            Box(Modifier.padding(top = 16.dp)) {
                when {
                    rows == null -> Unit // loading; render nothing until the first result lands
                    rows.isEmpty() -> Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🏆", style = MaterialTheme.typography.displaySmall)
                        Text(
                            "No public feedback yet — be the first!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEachIndexed { index, entry ->
                            LeaderboardRow(rank = index + 1, entry = entry)
                        }
                        if (rows.none { it.isCurrentUser }) {
                            Divider(
                                color = CardBorder,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                            PinnedYouRow()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, entry: LeaderboardEntry) {
    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RankBadge(rank.toString())

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.displayName.toDisplayName() + if (entry.isCurrentUser) " (you)" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                if (entry.isOfficeChampion) {
                    Text(
                        "Office Champion 👑",
                        style = MaterialTheme.typography.labelMedium,
                        color = GarminBlue,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            CountColumn(count = entry.publicFeedbackCount.toString())
        }
    }

    if (entry.isCurrentUser) {
        // Current user's row highlighted with an accent-blue card background.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AccentBlue,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, CardBorder),
            shadowElevation = 3.dp,
        ) {
            Box(Modifier.padding(16.dp)) { rowContent() }
        }
    } else {
        SurfaceCard(padding = 16) { rowContent() }
    }
}

/** Pinned row shown when the current user is not in the returned list. */
@Composable
private fun PinnedYouRow() {
    SurfaceCard(padding = 16) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RankBadge("—")
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "You",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Text(
                    "#—",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            CountColumn(count = "—")
        }
    }
}

@Composable
private fun RankBadge(label: String) {
    Surface(
        shape = CircleShape,
        color = Navy,
        modifier = Modifier.size(32.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = OnBrand,
            )
        }
    }
}

@Composable
private fun CountColumn(count: String) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            count,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        Text(
            "posts",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
    }
}
