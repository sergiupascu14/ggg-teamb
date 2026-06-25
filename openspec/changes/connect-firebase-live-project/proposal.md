## Why

The community newsfeed, voting, and leaderboard are built and tested against `InMemoryCommunityRepository`. The Firebase RTDB SDK and `FirebaseCommunityRepository` implementation are done, but the app cannot connect to a real Firebase project without a `google-services.json`. This change provisions a Firebase project, writes the RTDB security rules, and flips the wiring so the community surface becomes live and multi-device.

## What Changes

- Create / connect a Firebase project and add `app/google-services.json` (gitignored).
- Commit `firebase.json` + `database.rules.json` with RTDB security rules.
- Provide an emulator-targeted `app/src/debug/google-services.json` template so CI and local emulator runs don't need a real project.
- Switch `AppContainer.community` from `InMemoryCommunityRepository` to `FirebaseCommunityRepository(FirebaseDatabase.getInstance().reference)`.
- Move the demo seed data out of `AppContainer` into a one-shot emulator import / dev-data script.

## Capabilities

### New Capabilities

- `firebase-live-connection`: Provision Firebase project, deploy RTDB rules, and wire the app to `FirebaseCommunityRepository`.

### Modified Capabilities

*(none — `CommunityRepository` interface and `CommunityFeedback` model are unchanged)*

## Impact

- **`AppContainer.kt`** — swap repository implementation.
- **`app/google-services.json`** — runtime requirement; stays gitignored.
- **New committed files**: `firebase.json`, `database.rules.json`, `app/src/debug/google-services.json` (emulator template).
- **`app/build.gradle.kts`** — `google-services` plugin already applied; no changes needed.
- **Tests** — `FirebaseCommunityRepository` is already excluded from JaCoCo; `InMemoryCommunityRepository` tests unaffected.
