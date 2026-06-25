package com.example.teamb.ticketing

import app.cash.turbine.test
import com.example.teamb.data.db.TicketEntity
import com.example.teamb.data.model.TicketStatus
import com.example.teamb.data.repository.TicketRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TicketRepositoryTest {

    private fun ticket() = TicketEntity(
        feedbackId = 7L,
        category = "KITCHEN",
        route = "JIRA",
        externalId = "JIRA-1234",
        status = TicketStatus.OPEN.name,
        createdAt = 100L,
    )

    @Test
    fun updateStatus_persists_new_status() = runTest {
        val dao = FakeTicketDao()
        val id = dao.insert(ticket())
        val stored = dao.all.single { it.id == id }
        val repo = TicketRepository(dao)

        repo.updateStatus(stored, TicketStatus.IN_PROGRESS)

        assertEquals(TicketStatus.IN_PROGRESS.name, dao.all.single { it.id == id }.status)
    }

    @Test
    fun observeTickets_emits_updated_status() = runTest {
        val dao = FakeTicketDao()
        val id = dao.insert(ticket())
        val repo = TicketRepository(dao)

        repo.observeTickets().test {
            assertEquals(TicketStatus.OPEN.name, awaitItem().single().status)
            repo.updateStatus(dao.all.single { it.id == id }, TicketStatus.RESOLVED)
            assertEquals(TicketStatus.RESOLVED.name, awaitItem().single().status)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
