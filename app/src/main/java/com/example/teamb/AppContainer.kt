package com.example.teamb

import android.content.Context
import com.example.teamb.data.community.CommunityRepository
import com.example.teamb.data.community.FirebaseCommunityRepository
import com.example.teamb.data.datastore.CredentialStore
import com.example.teamb.data.datastore.DataStoreProfileStore
import com.example.teamb.data.datastore.DataStoreSettingsStore
import com.example.teamb.data.datastore.EncryptedCredentialStore
import com.example.teamb.data.datastore.ProfileStore
import com.example.teamb.data.datastore.SettingsStore
import com.example.teamb.data.sync.FirebaseFridgeRepository
import com.example.teamb.data.sync.FirebasePulseRepository
import com.example.teamb.data.sync.FridgeRepository
import com.example.teamb.data.sync.PulseRepository
import com.example.teamb.data.db.AppDatabase
import com.example.teamb.data.desk.DeskAllocationRepository
import com.example.teamb.data.integration.MockGarminAdDirectoryService
import com.example.teamb.data.integration.MockJiraTicketRouter
import com.example.teamb.data.integration.AndroidPhotoEncoder
import com.example.teamb.data.integration.MlKitPhotoIssueDetector
import com.example.teamb.data.integration.PhotoEncoder
import com.example.teamb.data.integration.PhotoIssueDetector
import com.example.teamb.data.integration.TicketRouter
import com.google.firebase.database.FirebaseDatabase
import com.example.teamb.data.repository.DailyPulseRepository
import com.example.teamb.data.repository.FeedbackRepository
import com.example.teamb.data.repository.FreezerRepository
import com.example.teamb.data.repository.GamificationRepository
import com.example.teamb.data.repository.TicketRepository
import com.example.teamb.data.util.Clock
import com.example.teamb.data.util.SystemClock

/** Manual dependency container. Held by [TeamBApp] and reachable from ViewModels. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val clock: Clock = SystemClock()

    val desk: DeskAllocationRepository by lazy { DeskAllocationRepository.fromAsset(appContext) }

    private val db by lazy { AppDatabase.get(appContext) }

    val profileStore: ProfileStore by lazy { DataStoreProfileStore(appContext) }
    val credentialStore: CredentialStore by lazy { EncryptedCredentialStore(appContext) }
    val settingsStore: SettingsStore by lazy { DataStoreSettingsStore(appContext) }

    val directory by lazy { MockGarminAdDirectoryService(desk) }
    val ticketRouter: TicketRouter by lazy { MockJiraTicketRouter() }
    val photoDetector: PhotoIssueDetector by lazy { MlKitPhotoIssueDetector(appContext) }
    val photoEncoder: PhotoEncoder by lazy { AndroidPhotoEncoder(appContext) }

    val community: CommunityRepository by lazy {
        FirebaseCommunityRepository(FirebaseDatabase.getInstance().reference)
    }
    val fridgeRepository: FridgeRepository by lazy {
        FirebaseFridgeRepository(FirebaseDatabase.getInstance().reference)
    }
    val pulseSyncRepository: PulseRepository by lazy {
        FirebasePulseRepository(FirebaseDatabase.getInstance().reference)
    }

    val dailyPulseRepository by lazy { DailyPulseRepository(db.dailyPulseDao(), clock) }
    val freezerRepository by lazy { FreezerRepository(db.freezerDao(), clock) }
    val feedbackRepository by lazy {
        FeedbackRepository(db.feedbackDao(), db.ticketDao(), community, ticketRouter, clock)
    }
    val ticketRepository by lazy { TicketRepository(db.ticketDao()) }
    val gamificationRepository by lazy { GamificationRepository(community, desk) }
}
