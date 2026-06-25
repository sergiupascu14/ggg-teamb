package com.example.teamb.ui.spaces

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.teamb.AppContainer
import com.example.teamb.ui.components.GarminHeader
import com.example.teamb.ui.components.ScreenTitle
import com.example.teamb.ui.components.SurfaceCard
import com.example.teamb.ui.theme.AccentBlue
import com.example.teamb.ui.theme.GarminBlue
import com.example.teamb.ui.theme.IssueText
import com.example.teamb.ui.theme.TextMuted
import com.example.teamb.ui.theme.TextPrimary
import com.example.teamb.ui.theme.TextSecondary
import kotlinx.coroutines.flow.emptyFlow

/**
 * Spaces hub (Screen 3): a directory of office areas with live observations. Kitchen is the only
 * area wired to a detail screen today; the rest preview upcoming insights.
 */
@Composable
fun SpacesScreen(container: AppContainer, onOpenKitchen: () -> Unit) {
    val profile by container.profileStore.profile.collectAsState(initial = null)
    val ownerId = profile?.staffId

    val frozenItems by remember(ownerId) {
        if (ownerId != null) container.freezerRepository.observeItems(ownerId) else emptyFlow()
    }.collectAsState(initial = emptyList())

    val kitchenStatus = when {
        ownerId == null -> "Sign in to see live status"
        frozenItems.isEmpty() -> "Freezer is empty"
        frozenItems.size >= 12 -> "Fridge almost full"
        else -> "${frozenItems.size} items in the freezer"
    }
    val kitchenAlert = frozenItems.size >= 12

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GarminHeader()
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 20.dp)) {
            ScreenTitle("Spaces", subtitle = "Office areas and live observations.")

            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SpaceCard(
                    icon = Icons.Filled.Apartment,
                    title = "Kitchen",
                    status = kitchenStatus,
                    statusColor = if (kitchenAlert) IssueText else TextSecondary,
                    onClick = onOpenKitchen,
                )
                SpaceCard(
                    icon = Icons.Filled.MeetingRoom,
                    title = "Meeting Rooms",
                    status = "Availability insights coming soon",
                    statusColor = TextMuted,
                    onClick = null,
                )
                SpaceCard(
                    icon = Icons.Filled.VolumeOff,
                    title = "Quiet Zones",
                    status = "Noise trends coming soon",
                    statusColor = TextMuted,
                    onClick = null,
                )
            }
        }
    }
}

@Composable
private fun SpaceCard(
    icon: ImageVector,
    title: String,
    status: String,
    statusColor: Color,
    onClick: (() -> Unit)?,
) {
    val cardModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    SurfaceCard(modifier = cardModifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = AccentBlue, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = GarminBlue, modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    status,
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (onClick != null) {
                Text("›", style = MaterialTheme.typography.headlineSmall, color = GarminBlue)
            }
        }
    }
}
