> Status: implemented and verified on an emulator. 128 unit tests pass; JaCoCo line coverage 98.5%
> on the scoped logic (90% gate enforced via `jacocoCoverageVerification`, wired into `check`).
> Community data runs on a seeded in-memory `CommunityRepository` by default; the Firebase RTDB
> implementation (`FirebaseCommunityRepository`) is written and ready but needs a `google-services.json`
> to go live (see 7.1).

## 1. Project Foundation

- [x] 1.1 Add dependencies to the version catalog and `app/build.gradle.kts`: Navigation Compose, Room (runtime/ktx/compiler via KSP), DataStore Preferences, WorkManager, Coil, and lifecycle-viewmodel-compose
- [x] 1.2 Add required permissions to `AndroidManifest.xml`: `POST_NOTIFICATIONS`, `INTERNET`, and camera/media access
- [x] 1.3 Create the app package structure: `data` (db, repository, integration), `ui` (screens, components, theme), `notification`, and `model`
- [x] 1.4 Set up Room database (`AppDatabase`), DataStore for the user profile/settings, and a notification channel + helper
- [x] 1.5 Replace the placeholder `Greeting` in `MainActivity` with a `NavHost` + bottom navigation scaffold across the core destinations
- [x] 1.6 Add a seed/dev-data helper to populate sample newsfeed entries (linked only by `userId`) so voting/filtering are demoable — currently seeds the in-memory `CommunityRepository`; same seed shape applies to Firebase
- [x] 1.7 Convert `docs/desk-allocation/Desk_Allocation_Anonymized.xlsx` (per `AI_AGENT_GUIDE.md`) into the committed/bundled app asset (`app/src/main/assets/desk_allocation.json`, 648 employees / 754 desks); real `Desk_Allocation.xlsx` stays untracked
- [x] 1.8 Build a `DeskAllocationRepository`/lookup: parse the desk ID grammar `{Building}{Floor}-{Zone}{Row}-{DeskNum}` (zones A–H, rows 1–7 per real data), expose the canonical building/floor enumeration (Tower 3–6, Riviera 3–5), and provide Staff ID → profile and desk lookups

## 2. User Onboarding (MVP)

- [x] 2.1 Define the `UserProfile` model and DataStore persistence (staffId/userId, name, supervisor, building, floor, deskArea)
- [x] 2.2 Create `DirectoryService` interface with a `MockGarminAdDirectoryService` backed by the desk allocation dataset (employee list + Staff ID → name/supervisor/desk); GarminAD may pre-highlight the likely match
- [x] 2.3 Build the identity-selection screen: a searchable employee list (filter by name or Staff ID) where the user picks their own entry, populating name/staffId/supervisor/desk
- [x] 2.4 Add account password setup (enter + confirm) storing only a secure hash (EncryptedSharedPreferences + salted SHA-256), and a password login screen for returning/ signed-out users
- [x] 2.5 Add a "Sign out" action (in profile/settings) that ends the session and returns to the login screen while preserving the stored account
- [x] 2.6 Build the remaining onboarding steps around the selected employee as the source of truth: pre-fill desk area from the employee record, auto-derive building/floor/zone, and allow manual desk entry only when the selected employee has no assigned desk
- [x] 2.7 Gate the app on onboarding completion and login: returning users go straight to the password login, then their stored profile

## 3. Daily Pulse (MVP)

- [x] 3.1 Define the `DailyPulseEntry` entity/DAO and repository (response + date, "checked in today" query)
- [x] 3.2 Build the Daily Pulse prompt UI and submission, showing the completed state when already done today
- [x] 3.3 Schedule a daily reminder via WorkManager that posts a notification only when not yet checked in
- [x] 3.4 Request `POST_NOTIFICATIONS` permission (API 33+) with graceful degradation when denied

## 4. Freezer Management (MVP)

- [x] 4.1 Define the `FreezerItem` entity/DAO and repository (label, owner, checkInAt, checkOutAt, present flag)
- [x] 4.2 Build the freezer screen: check-in form, list of the user's in-freezer items with check-in dates, and check-out action

## 5. Feedback Form (MVP)

- [x] 5.1 Define the `Feedback` entity/DAO and repository (category, description, sentiment, photos, anonymous flag, location, communityVisible, createdAt)
- [x] 5.2 Build the feedback form with a required category dropdown (Elevators, Kitchen, Desk Area, etc.) and a Positive/Issue sentiment selector
- [x] 5.3 Implement photo attachment (gallery via PickVisualMedia) with thumbnail previews using Coil
- [x] 5.4 Add the anonymous-or-identified toggle and the optional issue-location field
- [x] 5.5 Add the community-visibility setting (private by default) controlling newsfeed eligibility

## 6. Ticketing & Routing (MVP)

- [x] 6.1 Define the `Ticket` entity/DAO and repository (linked feedback, route, externalId, status, createdAt)
- [x] 6.2 Add the "create a ticket" checkbox to the feedback form
- [x] 6.3 Implement ticket-suppression logic: never create a ticket for positive feedback, and inform the user when suppressed
- [x] 6.4 Create the `TicketRouter` interface with `EmailTicketRouter` (mailto intent to #CLU-Facilities) and `MockJiraTicketRouter` (generates a `JIRA-####` id)
- [x] 6.5 Build the "My Tickets" dashboard listing the user's tickets with category, date, and status, including an empty state

## 7. Community Newsfeed (MVP)

- [ ] 7.1 Add Firebase to the project — RTDB SDK dependency + schema + `FirebaseCommunityRepository` are DONE; **pending a `google-services.json`** to connect a live project (app currently runs on the seeded in-memory repo)
- [x] 7.2 Implement a `NewsfeedRepository`/`CommunityRepository` that publishes community-visible feedback linking records only by `userId` (no name/supervisor/email) and observes them reactively; anonymous items carry no `userId`
- [x] 7.3 Build the newsfeed screen showing community-visible feedback (category, location, photo, locally-resolved name or "Anonymous")
- [x] 7.4 Implement voting: vote/un-vote toggle and per-item vote counts
- [x] 7.5 Add location filtering by building and floor (canonical enumeration), with a clear-filters action

## 8. Gamification (MVP)

- [x] 8.1 Implement Daily Pulse streak tracking (increment on consecutive days, reset on a missed day) and display the current streak
- [x] 8.2 Implement a points/rewards system counting only public, non-anonymous feedback toward ranking, and unlocking rewards at thresholds
- [x] 8.3 Build the leaderboard screen ranking users by public feedback count (by `userId`, names resolved locally), awarding the top user an "Office Champion" title, and highlighting the current user

## 9. Advanced Features (MVP)

- [x] 9.1 Create the `PhotoIssueDetector` interface and integrate it into the feedback form to suggest category/description from a photo, non-blocking with manual fallback and graceful unavailability handling
- [x] 9.2 Schedule smart freezer-cleanup reminders via WorkManager when an item exceeds the age threshold

## 10. Testing & Coverage (≥90%)

- [x] 10.1 Set up the unit test stack (JUnit, kotlinx-coroutines-test, Turbine) and a fake/in-memory layer (fake DAOs, fake DataStore/CredentialStore, in-memory `CommunityRepository`, fakes for `DirectoryService`/`PhotoIssueDetector`)
- [x] 10.2 Add JaCoCo with a `jacocoCoverageVerification` rule that **fails the build** below 90% line coverage, excluding generated code, DI, platform-bound integration, and pure `@Composable`/UI classes; wired into `check`
- [x] 10.3 Unit-test the data layer: repositories (feedback, tickets, freezer, daily-pulse), the desk allocation parser/lookup, and password hashing
- [x] 10.4 Unit-test the domain logic: ticket-suppression, ticket routing, streak increment/reset, points/rewards and leaderboard ranking (public-only, Office Champion), and feedback-form validation
- [x] 10.5 Unit-test the newsfeed/privacy behavior: records carry only `userId`, anonymous items carry no `userId`, local name resolution, and vote/un-vote counting
- [x] 10.6 Unit-test onboarding: employee search filtering, identity selection, desk-area pre-fill/derivation, and password setup/login/sign-out flows
- [x] 10.7 Unit-test the ViewModels (state transitions and error/empty states) using fakes and the test dispatcher
- [x] 10.8 Run `./gradlew jacocoCoverageVerification` — 128 tests pass, 98.5% line coverage, 90% gate green
- [x] 10.9 Build and run the app on an emulator and walk the MVP flow (onboarding → unlock → daily pulse → community newsfeed with name resolution/Anonymous → profile with derived location/streak/rewards/sign-out)
