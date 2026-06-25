package com.example.teamb.ui.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.teamb.AppContainer
import com.example.teamb.data.db.TicketEntity
import com.example.teamb.data.model.TicketStatus
import com.example.teamb.data.repository.TicketRepository
import com.example.teamb.data.util.Dates
import kotlinx.coroutines.launch

@Composable
fun TicketsScreen(container: AppContainer) {
    val repository: TicketRepository = container.ticketRepository
    val tickets by repository.observeTickets().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("My Tickets", style = MaterialTheme.typography.headlineSmall)

        if (tickets.isEmpty()) {
            Text(
                "No tickets yet. Submit issue feedback with \"Create a ticket\" to raise one.",
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tickets, key = { it.id }) { ticket ->
                    TicketCard(
                        ticket = ticket,
                        onAdvance = {
                            scope.launch { repository.updateStatus(ticket, nextStatus(ticket.status)) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TicketCard(ticket: TicketEntity, onAdvance: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(ticket.externalId, style = MaterialTheme.typography.titleMedium)
                AssistChip(
                    onClick = {},
                    label = { Text(statusLabel(ticket.status)) },
                )
            }
            Text("Category: ${ticket.category}", style = MaterialTheme.typography.bodyMedium)
            Text("Route: ${ticket.route}", style = MaterialTheme.typography.bodyMedium)
            Text("Created: ${Dates.isoDate(ticket.createdAt)}", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onAdvance) { Text("Advance status") }
        }
    }
}

private fun statusLabel(raw: String): String =
    runCatching { TicketStatus.valueOf(raw).label }.getOrDefault(raw)

/** Cycles OPEN → IN_PROGRESS → RESOLVED → OPEN for the demo. */
private fun nextStatus(raw: String): TicketStatus =
    when (runCatching { TicketStatus.valueOf(raw) }.getOrNull()) {
        TicketStatus.OPEN -> TicketStatus.IN_PROGRESS
        TicketStatus.IN_PROGRESS -> TicketStatus.RESOLVED
        TicketStatus.RESOLVED -> TicketStatus.OPEN
        null -> TicketStatus.OPEN
    }
