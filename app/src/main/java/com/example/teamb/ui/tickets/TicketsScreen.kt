package com.example.teamb.ui.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.teamb.AppContainer
import com.example.teamb.data.db.TicketEntity
import com.example.teamb.data.model.TicketStatus
import com.example.teamb.data.repository.TicketRepository
import com.example.teamb.ui.components.GarminHeader
import com.example.teamb.ui.components.OutlinedPillButton
import com.example.teamb.ui.components.ScreenTitle
import com.example.teamb.ui.components.SurfaceCard
import com.example.teamb.ui.components.Tag
import com.example.teamb.ui.theme.AccentBlue
import com.example.teamb.ui.theme.GarminBlue
import com.example.teamb.ui.theme.IssueBg
import com.example.teamb.ui.theme.IssueText
import com.example.teamb.ui.theme.PositiveBg
import com.example.teamb.ui.theme.PositiveText
import com.example.teamb.ui.theme.TextMuted
import com.example.teamb.ui.theme.TextPrimary
import com.example.teamb.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TicketsScreen(container: AppContainer) {
    val repository: TicketRepository = container.ticketRepository
    val tickets by repository.observeTickets().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        GarminHeader()
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 20.dp)) {
            ScreenTitle("My Tickets", subtitle = "Track submitted issue requests.")

            Box(Modifier.padding(top = 16.dp)) {
                if (tickets.isEmpty()) {
                    EmptyState()
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        tickets.forEach { ticket ->
                            TicketCard(
                                ticket = ticket,
                                onAdvance = {
                                    scope.launch {
                                        repository.updateStatus(ticket, nextStatus(ticket.status))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.ConfirmationNumber,
            contentDescription = null,
            tint = GarminBlue,
            modifier = Modifier.size(48.dp),
        )
        Text(
            "No tickets yet.",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Submit issue feedback with \"Create a ticket\" to raise one.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun TicketCard(ticket: TicketEntity, onAdvance: () -> Unit) {
    SurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    ticket.externalId,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                StatusTag(ticket.status)
            }
            Text(
                "Category: ${ticket.category}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Text(
                "Route: ${ticket.route}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Text(
                "Created ${formatDate(ticket.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            Box(Modifier.padding(top = 4.dp)) {
                OutlinedPillButton(text = "Advance status", onClick = onAdvance)
            }
        }
    }
}

@Composable
private fun StatusTag(rawStatus: String) {
    val status = runCatching { TicketStatus.valueOf(rawStatus) }.getOrNull()
    val (bg, fg) = when (status) {
        TicketStatus.OPEN -> AccentBlue to GarminBlue
        TicketStatus.IN_PROGRESS -> IssueBg to IssueText
        TicketStatus.RESOLVED -> PositiveBg to PositiveText
        null -> AccentBlue to GarminBlue
    }
    Tag(text = status?.label ?: rawStatus, bg = bg, fg = fg)
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(millis)

/** Cycles OPEN → IN_PROGRESS → RESOLVED → OPEN for the demo. */
private fun nextStatus(raw: String): TicketStatus =
    when (runCatching { TicketStatus.valueOf(raw) }.getOrNull()) {
        TicketStatus.OPEN -> TicketStatus.IN_PROGRESS
        TicketStatus.IN_PROGRESS -> TicketStatus.RESOLVED
        TicketStatus.RESOLVED -> TicketStatus.OPEN
        null -> TicketStatus.OPEN
    }
