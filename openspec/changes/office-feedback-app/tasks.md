## 1. Project Foundation

- [ ] 1.1 Add dependencies to the version catalog and `app/build.gradle.kts`: Navigation Compose, Room (runtime/ktx/compiler via KSP), DataStore Preferences, WorkManager, Coil, and lifecycle-viewmodel-compose
- [ ] 1.2 Add required permissions to `AndroidManifest.xml`: `POST_NOTIFICATIONS`, `INTERNET`, and camera/media access
- [ ] 1.3 Create the app package structure: `data` (db, repository, integration), `ui` (screens, components, theme), `notification`, and `model`
- [ ] 1.4 Set up Room database (`AppDatabase`), DataStore for the user profile/settings, and a notification channel + helper
- [ ] 1.5 Replace the placeholder `Greeting` in `MainActivity` with a `NavHost` + bottom navigation scaffold across the core destinations
- [ ] 1.6 Add a seed/dev-data helper to populate sample newsfeed and leaderboard entries for demos
- [ ] 1.7 Convert the desk allocation dataset (`docs/desk-allocation/Desk_Allocation_Anonymized.xlsx`, per `AI_AGENT_GUIDE.md`) into a bundled app asset (e.g. JSON in `app/src/main/assets`) covering desks (id, building, floor, zone) and associates (staff ID → name, supervisor)
- [ ] 1.8 Build a `DeskAllocationRepository`/lookup: parse the desk ID grammar `{Building}{Floor}-{Zone}{Row}-{DeskNum}`, expose the canonical building/floor enumeration (Tower 3–6, Riviera 3–5) and zones A–D, and provide Staff ID → profile and desk lookups

## 2. User Onboarding (MVP)

- [ ] 2.1 Define the `UserProfile` model and DataStore persistence (staffId/userId, name, supervisor, building, floor, deskArea)
- [ ] 2.2 Create `DirectoryService` interface with a `MockGarminAdDirectoryService` backed by the desk allocation dataset (employee list + Staff ID → name/supervisor/desk); GarminAD may pre-highlight the likely match
- [ ] 2.3 Build the identity-selection screen: a searchable employee list (filter by name or Staff ID) where the user picks their own entry, populating name/staffId/supervisor/desk
- [ ] 2.4 Add account password setup (enter + confirm) storing only a secure hash (e.g. EncryptedSharedPreferences / salted hash), and a password login screen for returning/ signed-out users
- [ ] 2.5 Add a "Sign out" action (in profile/settings) that ends the session and returns to the login screen while preserving the stored account
- [ ] 2.6 Build the remaining onboarding steps: location step (building + floor from the canonical enumeration) and desk-area step that pre-fills from the selected employee, validates against the desk ID grammar, and derives building/floor/zone
- [ ] 2.7 Gate the app on onboarding completion and login: returning users go straight to the password login, then their stored profile

## 3. Daily Pulse (MVP)

- [ ] 3.1 Define the `DailyPulseEntry` entity/DAO and repository (response + date, "checked in today" query)
- [ ] 3.2 Build the Daily Pulse prompt UI and submission, showing the completed state when already done today
- [ ] 3.3 Schedule a daily reminder via WorkManager that posts a notification only when not yet checked in, deep-linking to the prompt
- [ ] 3.4 Request `POST_NOTIFICATIONS` permission (API 33+) with rationale and graceful degradation when denied

## 4. Freezer Management (MVP)

- [ ] 4.1 Define the `FreezerItem` entity/DAO and repository (label, owner, checkInAt, checkOutAt, present flag)
- [ ] 4.2 Build the freezer screen: check-in form, list of the user's in-freezer items with check-in dates, and check-out action

## 5. Feedback Form (MVP)

- [ ] 5.1 Define the `Feedback` entity/DAO and repository (category, description, sentiment, photos, anonymous flag, location, communityVisible, createdAt)
- [ ] 5.2 Build the feedback form with a required category dropdown (Elevators, Kitchen, Desk Area, etc.) and a Positive/Issue sentiment selector
- [ ] 5.3 Implement photo attachment (camera + gallery) with thumbnail previews using Coil
- [ ] 5.4 Add the anonymous-or-identified toggle and the optional issue-location field
- [ ] 5.5 Add the community-visibility setting (private by default) controlling newsfeed eligibility

## 6. Ticketing & Routing (MVP)

- [ ] 6.1 Define the `Ticket` entity/DAO and repository (linked feedback, route, externalId, status, createdAt)
- [ ] 6.2 Add the "create a ticket" checkbox to the feedback form
- [ ] 6.3 Implement ticket-suppression logic: never create a ticket for positive feedback, and inform the user when suppressed
- [ ] 6.4 Create the `TicketRouter` interface with `EmailTicketRouter` (mailto intent to #CLU-Facilities) and `MockJiraTicketRouter` (generates a `JIRA-####` id)
- [ ] 6.5 Build the "My Tickets" dashboard listing the user's tickets with category, date, and status, including an empty state

## 7. Community Newsfeed (MVP)

- [ ] 7.1 Add Firebase to the project (Gradle plugin, `google-services.json`, Realtime Database SDK) and define the RTDB schema for community feedback + votes
- [ ] 7.2 Implement a `NewsfeedRepository` that publishes community-visible feedback to Firebase RTDB linking records only by `userId` (no name/supervisor/email), and observes them in real time; anonymous items carry no `userId`
- [ ] 7.3 Build the newsfeed screen showing community-visible feedback (category, location, photo, locally-resolved name or "Anonymous"), resolving names on-device from the desk allocation dataset
- [ ] 7.4 Implement voting in Firebase RTDB: vote/un-vote toggle and per-item vote counts synced across devices
- [ ] 7.5 Add location filtering by building and floor (canonical enumeration), with a clear-filters action

## 8. Gamification (MVP)

- [ ] 8.1 Implement Daily Pulse streak tracking (increment on consecutive days, reset on a missed day) and display the current streak
- [ ] 8.2 Implement a points/rewards system awarding points for qualifying actions (counting only public, non-anonymous feedback toward ranking) and unlocking rewards at thresholds with notification
- [ ] 8.3 Build the leaderboard screen ranking users by public feedback count (derived from Firebase records by `userId`, names resolved locally), awarding the top user an "Office Champion" title, and highlighting the current user

## 9. Advanced Features (MVP)

- [ ] 9.1 Create the `PhotoIssueDetector` interface and integrate it into the feedback form to suggest category/description from a photo, non-blocking with manual fallback and graceful unavailability handling
- [ ] 9.2 Schedule smart freezer-cleanup reminders via WorkManager when an item exceeds the age threshold

## 10. Testing & Coverage (≥90%)

- [ ] 10.1 Set up the unit test stack (JUnit, MockK/Mockito, kotlinx-coroutines-test, Turbine for Flows) and a fake/in-memory layer (in-memory Room, fake DataStore, fake Firebase RTDB, fake `DirectoryService`/`TicketRouter`/`PhotoIssueDetector`)
- [ ] 10.2 Add JaCoCo with a `coverageVerification` rule that **fails the build** below 90% line coverage on the testable code, excluding generated code, DI, and pure `@Composable`/UI classes
- [ ] 10.3 Unit-test the data layer: repositories and DAOs (feedback, tickets, freezer, daily-pulse), the desk allocation parser/lookup (desk ID grammar, building/floor enumeration, Staff ID lookup), and DataStore profile/password (hash set/verify) logic
- [ ] 10.4 Unit-test the domain logic: ticket-suppression (no ticket for positive feedback), ticket routing selection (email vs mock Jira), streak increment/reset, points/rewards and leaderboard ranking (public-only, Office Champion), and feedback-form validation
- [ ] 10.5 Unit-test the newsfeed/privacy behavior against a fake Firebase RTDB: records carry only `userId` (never name/supervisor/email), anonymous items carry no `userId`, local name resolution, and vote/un-vote counting
- [ ] 10.6 Unit-test onboarding: employee search filtering, identity selection populating the profile, desk-area pre-fill/derivation, and password setup/login/sign-out flows
- [ ] 10.7 Unit-test the ViewModels for each screen (state transitions and error/empty states) using fakes and the test dispatcher
- [ ] 10.8 Run `./gradlew testDebugUnitTest jacocoTestCoverageVerification`, confirm the 90% gate passes, and close any gaps
- [ ] 10.9 Build and run the app on an emulator/device and walk the full MVP demo flow (onboarding → daily pulse → freezer → feedback with AI photo suggestion → ticket → tracker → community newsfeed with voting/filtering → leaderboard/Office Champion)
