package com.example.teamb.freezer

import com.example.teamb.data.db.FreezerDao
import com.example.teamb.data.db.FreezerItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [FreezerDao] with autoincrement ids. */
class FakeFreezerDao : FreezerDao {
    private val store = MutableStateFlow<Map<Long, FreezerItemEntity>>(emptyMap())
    private var nextId = 1L

    override suspend fun insert(item: FreezerItemEntity): Long {
        val id = nextId++
        store.value = store.value + (id to item.copy(id = id))
        return id
    }

    override suspend fun update(item: FreezerItemEntity) {
        store.value = store.value + (item.id to item)
    }

    override fun observePresent(ownerId: String): Flow<List<FreezerItemEntity>> =
        store.map { m ->
            m.values
                .filter { it.ownerId == ownerId && it.present }
                .sortedByDescending { it.checkInAt }
        }

    override suspend fun allPresent(): List<FreezerItemEntity> =
        store.value.values.filter { it.present }

    override suspend fun byId(id: Long): FreezerItemEntity? = store.value[id]
}
