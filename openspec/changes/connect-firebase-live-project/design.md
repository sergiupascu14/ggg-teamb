## Context

The app's community surface (newsfeed, votes, leaderboard) is driven by `CommunityRepository`. Two implementations exist: `InMemoryCommunityRepository` (current default — demo-only, no persistence, single-device) and `FirebaseCommunityRepository` (fully written, maps to the RTDB schema `feedback/{id}` / `votes/{id}/{voterId}`). The Firebase BOM + RTDB Kotlin SDK are already declared in `app/build.gradle.kts` and the Google Services Gradle plugin is applied, so the build is already Firebase-capable. The missing piece is `app/google-services.json`, which the plugin requires at compile time to inject the project's configuration.

`AppContainer` currently constructs `InMemoryCommunityRepository(seedCommunity())` and passes it throughout the app. No Hilt or other DI framework is used — it is a manual service locator held by `TeamBApp`.

Privacy contract (non-negotiable): Firebase records contain only `userId` (Staff ID) and content fields. Names, supervisors, and email are never written to Firebase; they are resolved locally from the bundled desk allocation dataset.

## Goals / Non-Goals

**Goals:**
- Provide a `google-services.json` workflow (real project for production, emulator template for CI/debug).
- Commit `firebase.json` + `database.rules.json` so security rules are version-controlled and deployable via `firebase deploy --only database`.
- Wire `AppContainer` to `FirebaseCommunityRepository` so the community surface is live and multi-device.
- Remove the in-app `seedCommunity()` from the production path; keep it available for the Firebase Emulator.

**Non-Goals:**
- Firebase Authentication — the app uses Staff ID (offline identity); RTDB rules will use `.validate` rather than `auth != null` for now (emulator-compatible open-read/authenticated-write relaxed rules suitable for a closed office demo).
- Firebase Storage for photos — photo references (`photoRef`) remain a future concern.
- Cloud Firestore migration — RTDB is the chosen store per project design.
- CI pipeline changes beyond the emulator template.

## Decisions

### 1. `google-services.json` stays gitignored; an emulator template is committed

**Decision**: Commit `app/src/debug/google-services.json` pointing at `localhost` emulator (project ID `clooj-ggg`). The real `app/google-services.json` (production project `clooj-ggg`, RTDB URL `https://clooj-ggg-default-rtdb.europe-west1.firebasedatabase.app`) is gitignored and must be placed manually or via CI secret injection.

**Rationale**: The `.gitignore` already excludes `google-services.json` at the project root. Committing a debug-flavored emulator config in `app/src/debug/` lets any developer run `./gradlew installDebug` against the Firebase Emulator without touching the real project. Production builds fail fast at Gradle configuration time if the real file is absent, which is the desired behaviour for a demo app with a known operator.

**Alternative considered**: Stub/no-op `google-services.json` committed — rejected because it produces a valid but broken Firebase connection rather than a clean build error, making misconfiguration harder to diagnose.

### 2. RTDB security rules: validate-only (no Firebase Auth)

**Decision**: Rules allow reads by anyone and writes that pass structural validation (required fields present, no PII field names). Voter nodes (`votes/{itemId}/{voterId}`) allow write only when `voterId` matches `newData.key` (self-keyed) — preventing vote-stuffing under one key.

**Rationale**: The app does not implement Firebase Auth. For a closed office demo the pragmatic trade-off is open reads + structurally-validated writes. A future `auth` gate can be layered on when Auth is introduced.

**Alternative considered**: Locked rules (`".read": false, ".write": false`) — rejected because it blocks all app operations until Auth is in place.

### 3. `AppContainer` swap: replace `InMemoryCommunityRepository` with `FirebaseCommunityRepository`

**Decision**: In `AppContainer`, replace:
```kotlin
val community: CommunityRepository by lazy { InMemoryCommunityRepository(seedCommunity()) }
```
with:
```kotlin
val community: CommunityRepository by lazy {
    FirebaseCommunityRepository(FirebaseDatabase.getInstance().reference)
}
```
Remove `seedCommunity()` from the production path. Keep the method (or a stripped version) as a utility for loading seed data into the emulator via a dev script.

**Rationale**: `FirebaseCommunityRepository` already satisfies the `CommunityRepository` interface fully. The swap is a one-line change; no other code needs to change because consumers reference the interface, not the implementation.

**Alternative considered**: Feature flag / build-variant split — rejected as over-engineering for a demo app with a single operator.

### 4. Seed data strategy

**Decision**: Provide a `scripts/seed_emulator.sh` (or equivalent `firebase database:set` command documented in `README-firebase.md`) to populate the emulator with the same four seed entries currently hard-coded in `AppContainer`. The seed JSON mirrors the RTDB schema.

**Rationale**: Keeping seed data in `AppContainer` couples app startup to demo state. Moving it outside keeps the production code clean and makes the seed step explicit for developers.

## Risks / Trade-offs

- **Build fails without `google-services.json`** → Mitigation: document the setup steps in `README-firebase.md`; the emulator template covers local dev.
- **Open RTDB rules in production** → Mitigation: acceptable for a closed office demo; document that rules must be hardened before wider rollout.
- **No offline support** → `FirebaseCommunityRepository` requires connectivity; the newsfeed shows an empty list when offline. Mitigation: the UI already handles empty states gracefully; offline caching is a future task.
- **Cold-start latency** → First `observeFeedback` call incurs a network round-trip. Mitigation: the existing loading state in `NewsfeedViewModel` covers this; no UI change needed.

## Migration Plan

1. **Firebase project `clooj-ggg` is set up** — `app/google-services.json` placed (gitignored). RTDB URL: `https://clooj-ggg-default-rtdb.europe-west1.firebasedatabase.app`.
2. **Deploy RTDB rules**: open Firebase Console → project `clooj-ggg` → Realtime Database → Rules tab, paste `database.rules.json`, and publish.
3. **Seed emulator** (dev only): run `firebase emulators:start`, then `firebase database:set / scripts/seed.json`.
4. **Swap repository in `AppContainer`** — the one-line code change.
5. **Smoke-test** on device: open newsfeed, post feedback, vote, verify multi-device sync.
6. **Rollback**: revert the `AppContainer` change to restore `InMemoryCommunityRepository`; no data migration needed (RTDB data persists independently).

## Open Questions

- Should the Firebase project be shared (one project for all team members) or per-developer? → **Resolved**: shared project `clooj-ggg` with `google-services.json` distributed out-of-band.
- Is a `README-firebase.md` the right place for onboarding docs, or should it go in the main `README.md`?
