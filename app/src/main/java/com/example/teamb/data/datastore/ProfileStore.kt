package com.example.teamb.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.teamb.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Local-only persistence of the user profile. Identity never leaves the device. */
interface ProfileStore {
    val profile: Flow<UserProfile?>
    suspend fun save(profile: UserProfile)
    suspend fun clear()
}

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore("user_profile")

class DataStoreProfileStore(private val context: Context) : ProfileStore {
    override val profile: Flow<UserProfile?> =
        context.profileDataStore.data.map { prefs -> prefs.toProfile() }

    override suspend fun save(profile: UserProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[STAFF_ID] = profile.staffId
            prefs[NAME] = profile.name
            prefs[SUPERVISOR] = profile.supervisor
            prefs[BUILDING] = profile.building
            prefs[FLOOR] = profile.floor
            prefs[ZONE] = profile.zone
            prefs[DESK] = profile.deskArea
        }
    }

    override suspend fun clear() {
        context.profileDataStore.edit { it.clear() }
    }

    private fun Preferences.toProfile(): UserProfile? {
        val staffId = this[STAFF_ID] ?: return null
        return UserProfile(
            staffId = staffId,
            name = this[NAME] ?: "",
            supervisor = this[SUPERVISOR] ?: "",
            building = this[BUILDING] ?: "",
            floor = this[FLOOR] ?: 0,
            zone = this[ZONE] ?: "",
            deskArea = this[DESK] ?: "",
        )
    }

    private companion object {
        val STAFF_ID = stringPreferencesKey("staffId")
        val NAME = stringPreferencesKey("name")
        val SUPERVISOR = stringPreferencesKey("supervisor")
        val BUILDING = stringPreferencesKey("building")
        val FLOOR = intPreferencesKey("floor")
        val ZONE = stringPreferencesKey("zone")
        val DESK = stringPreferencesKey("desk")
    }
}
