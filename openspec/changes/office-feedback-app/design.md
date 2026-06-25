## Context

The app is a greenfield Android project (`com.example.teamb`) bootstrapped with Jetpack Compose, Material3, Kotlin, `minSdk 24`, `targetSdk 35`, JVM 17. `MainActivity` currently shows a placeholder `Greeting`. This is a hackathon/demo build: the priority is a convincing, end-to-end demoable workflow over production-grade backend integration. External systems named in the requirements — GarminAD (directory), #CLU-Facilities mailbox, and Jira — are not available with live credentials in the demo environment, so they must be abstracted and mockable. Stakeholders: office occupants (end users), facilities team (ticket recipients), and the hackathon demo audience.

## Goals / Non-Goals

**Goals:**
- A single-module Compose app with bottom-navigation across the core surfaces (Home/Daily Pulse, Feedback, Freezer, Newsfeed, My Tickets, Profile).
- Local-first persistence so the full workflow works offline and demos deterministically.
- Clean abstraction boundaries for the three external integrations (directory, ticket routing, AI vision) so each has a mock implementation today and a real one later.
- Daily reminders and freezer-cleanup reminders via scheduled background work + notifications.
- **≥90% unit test coverage**, enforced as a build-failing gate.

**Non-Goals:**
- A custom backend server. Community data (newsfeed feedback + votes) uses Firebase Realtime Database; everything else is local. No bespoke API tier.
- Storing user identities (names, supervisors, profiles) in the cloud. Identity stays on-device.
- Production GarminAD SSO, real SMTP delivery, or a live Jira connection.
- Account management beyond a single local profile per device.

## Decisions

- **UI: Jetpack Compose + Material3 with Navigation Compose and a bottom nav bar.** Already the project's stack; fastest path to multiple screens. Alternative (Fragments/Views) rejected as heavier and off-stack.
- **Architecture: MVVM with a repository layer.** Screens observe `ViewModel` state (`StateFlow`); ViewModels call repositories; repositories own persistence and integration interfaces. Keeps the demo logic (positive-feedback ticket suppression, streak math) testable and out of composables.
- **Persistence: Room for local structured entities (own feedback, tickets, freezer items, daily-pulse log) + DataStore for the single user profile and settings.** Room gives queryable local history for the tracker; DataStore is simpler for the singleton profile. Alternative (everything in DataStore/JSON) rejected because the tracker needs filtering/sorting queries.
- **Community data: Firebase Realtime Database for the newsfeed (community-visible feedback + votes).** This is the one shared, multi-device surface, so it needs a live backend; Firebase RTDB gives real-time sync and free-tier hosting without a custom server. Alternatives (Firestore, custom REST API) rejected — RTDB is simplest for this small, list-shaped, real-time data and the hackathon timeline.
- **Privacy: user identities never leave the device.** The user list/profile is stored only locally (DataStore + the bundled desk allocation dataset, which exists identically on every phone). Records published to Firebase are linked **only by `userId`** (the Staff ID) plus content fields (category, sentiment, location, photo ref, votes) — never names, supervisors, or emails. The newsfeed resolves `userId` → display name **locally** via the bundled desk allocation dataset; anonymous submissions carry no `userId` at all. This keeps PII off the cloud while still showing names in-app. Alternative (storing display names in Firebase) rejected for privacy.
- **External integrations behind interfaces with mock impls:**
  - `DirectoryService.fetchProfile()` — mock returns a sample Garmin user; real impl would call GarminAD. Onboarding always falls back to manual entry.
  - `TicketRouter` with `EmailTicketRouter` (composes an `ACTION_SENDTO` mailto intent to #CLU-Facilities) and `MockJiraTicketRouter` (generates a fake `JIRA-####` id and persists status). Route is config/toggle-selectable.
  - `PhotoIssueDetector.analyze(image)` — built last in the sequence; mock/heuristic to start, swappable for on-device ML Kit or a cloud vision call. Always optional and non-blocking.
- **Notifications: WorkManager periodic work + `NotificationManager`.** Daily Pulse reminder and freezer-cleanup reminder are scheduled workers that check state before posting. Handles `POST_NOTIFICATIONS` runtime permission (API 33+). Alternative (`AlarmManager`) rejected; WorkManager is more robust to reboots/Doze.
- **Photos: Activity Result APIs (`PickVisualMedia` / camera) + Coil for loading; store image URIs/copied files locally.** Standard, lightweight.
- **Positive-vs-actionable classification: explicit sentiment field on the feedback form (e.g. Positive / Issue) drives ticket suppression**, rather than inferring tone from free text. Deterministic and demo-safe; AI can augment later.
- **Identity by employee selection, not free text.** Onboarding shows a searchable list of Garmin employees from the bundled desk allocation dataset; the user picks themselves, which fixes their Staff ID (the `userId` used everywhere, incl. Firebase) and pulls name/supervisor/desk. The selected employee's desk assignment is the onboarding source of truth for `deskArea`, `building`, `floor`, and derived `zone`, so the app auto-fills location from the dataset instead of asking the user to enter building/floor separately. If the selected employee has no assigned desk in the dataset, the app falls back to manual desk entry and derives location from the entered desk code. This avoids typos, guarantees a valid `userId`, and keeps identity resolution local. GarminAD, when available, only pre-highlights the likely match.
- **Local account password + sign-out.** The app stores a password hash (e.g. via `EncryptedSharedPreferences` / a salted hash in DataStore) to gate access on return; there is no auth server. Sign-out clears the in-memory session and returns to a password login screen while preserving the stored account. This is device-local protection of the profile, not federated auth — a real build would layer GarminAD SSO on top. Plaintext passwords are never stored.
- **Desk allocation dataset as canonical location reference.** `AI_AGENT_GUIDE.md` in `docs/desk-allocation/` defines the desk ID grammar (`{Building}{Floor}-{Zone}{Row}-{DeskNum}`), the building/floor enumeration (Tower 3–6, Riviera 3–5), zones A–D, and Staff ID → name/supervisor/desk mappings. Repo, CI, and demo builds convert only `Desk_Allocation_Anonymized.xlsx` into the bundled app asset and may commit only anonymized/generated-safe data. If a private internal build ever needs the real `Desk_Allocation.xlsx`, that file must live outside git or in an ignored path, and any generated asset derived from it must also be written only to ignored output (for example under `build/`), never under committed source paths. Onboarding resolves identity and auto-fills desk/location from the bundled dataset, while manual desk entry remains a fallback only when a selected employee lacks an assigned desk. The newsfeed building/floor filters draw their options from the same canonical enumeration. Alternative (hardcoding building/floor lists in the UI) rejected to avoid drift from the real layout.
- **Testing strategy targeting a 90% coverage gate.** MVVM + repository + integration-interface boundaries exist partly so business logic is unit-testable without a device: ViewModels, repositories, mappers, validators, the desk-allocation parser, ticket-suppression/routing, streak/points/leaderboard math, and the privacy rules are covered with JUnit + fakes (in-memory Room, fake DataStore, fake Firebase RTDB, fake directory/router/detector) and coroutine/Flow test utilities. JaCoCo enforces ≥90% line coverage and **fails the build** below it. Pure `@Composable`/UI, generated, and DI glue are excluded from the denominator (UI is exercised by the manual demo walkthrough and optional instrumentation tests) so the 90% reflects real logic coverage rather than being gamed or blocked by hard-to-unit-test UI. Alternative (coverage-as-report-only, no enforced gate) rejected because the requirement is a hard 90%.
- **Everything is in the MVP; phases are build order, not scope.** The whole feature set ships in the MVP: onboarding, daily-pulse, freezer (incl. smart cleanup reminders), feedback-form, ticketing, the Firebase newsfeed, gamification, and AI photo detection. The build sequence is core → community/engagement → advanced (AI, smart reminders) to manage dependencies, but no capability is deferred or optional. The demo walkthrough exercises all of it.

## Risks / Trade-offs

- **Mock integrations may feel non-real in the demo** → keep mocks realistic (sample directory user, visible generated Jira id, an actual draft email via mailto intent) and clearly labeled.
- **Community data must be genuinely shared across users, not local-only** → the newsfeed and leaderboard are backed by Firebase Realtime Database so feedback and votes sync across devices in real time; seed Firebase with believable sample entries so voting and filtering are demoable even before multiple real devices are connected.
- **Notification permission denial / OEM Doze behavior could suppress reminders** → request permission with rationale, degrade gracefully, and provide an in-app way to open the Daily Pulse.
- **Scope is large for a hackathon, yet all of it is in the MVP** → manage with strict build ordering (core → community → advanced) so each layer is demoable as it lands and integration risk is incremental; the full set still ships in the MVP.
- **AI photo detection latency/cost** → run async, never block submission, and fall back silently when unavailable.

## Migration Plan

Greenfield — no data migration. Deploy by building the debug APK and running on a device/emulator. "Rollback" is reverting to the placeholder `MainActivity`. The full MVP is landed in build-order layers (core → community → advanced) so each layer is independently revertible, but all layers are part of the MVP.

## Open Questions

- Is real GarminAD access (SSO/Graph-style API) available for the final build, or is the mock sufficient for the demo?
- Should ticket routing default to the email (mailto) path or the mock Jira path for the demo?
- For AI photo detection, is on-device ML Kit acceptable, or is a cloud vision model (e.g. Claude vision) preferred for richer issue descriptions?
- What is the freezer "old food" age threshold (e.g. 7 days) and the Daily Pulse reminder time of day?
