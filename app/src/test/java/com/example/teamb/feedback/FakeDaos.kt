package com.example.teamb.feedback

import com.example.teamb.data.db.FeedbackDao
import com.example.teamb.data.db.FeedbackEntity
import com.example.teamb.data.db.TicketDao
import com.example.teamb.data.db.TicketEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [FeedbackDao] with autoincrement ids. */
class FakeFeedbackDao : FeedbackDao {
    private val store = MutableStateFlow<Map<Long, FeedbackEntity>>(emptyMap())
    private var nextId = 1L

    val all: List<FeedbackEntity> get() = store.value.values.toList()

    override suspend fun insert(item: FeedbackEntity): Long {
        val id = nextId++
        store.value = store.value + (id to item.copy(id = id))
        return id
    }

    override fun observeAll(): Flow<List<FeedbackEntity>> =
        store.map { m -> m.values.sortedByDescending { it.createdAt } }

    override suspend fun byId(id: Long): FeedbackEntity? = store.value[id]
}

/** In-memory [TicketDao] with autoincrement ids. */
class FakeTicketDao : TicketDao {
    private val store = MutableStateFlow<Map<Long, TicketEntity>>(emptyMap())
    private var nextId = 1L

    val all: List<TicketEntity> get() = store.value.values.toList()

    override suspend fun insert(item: TicketEntity): Long {
        val id = nextId++
        store.value = store.value + (id to item.copy(id = id))
        return id
    }

    override suspend fun update(item: TicketEntity) {
        store.value = store.value + (item.id to item)
    }

    override fun observeAll(): Flow<List<TicketEntity>> =
        store.map { m -> m.values.sortedByDescending { it.createdAt } }
}
