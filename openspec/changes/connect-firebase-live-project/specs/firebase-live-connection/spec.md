## ADDED Requirements

### Requirement: Firebase project configuration is present at build time
The build system SHALL resolve a `google-services.json` at Gradle configuration time. For debug/CI builds the file SHALL be an emulator-targeted template committed at `app/src/debug/google-services.json`. For release builds the file SHALL be the real project config placed at `app/google-services.json` (gitignored).

#### Scenario: Debug build uses emulator config
- **WHEN** a developer runs `./gradlew assembleDebug` without a real `google-services.json`
- **THEN** Gradle resolves `app/src/debug/google-services.json` and the build succeeds

#### Scenario: Release build fails without real config
- **WHEN** `app/google-services.json` is absent during a release build
- **THEN** the Gradle Google Services plugin fails with a clear missing-file error

---

### Requirement: RTDB security rules are version-controlled and deployable
The repository SHALL contain `firebase.json` and `database.rules.json` defining RTDB security rules. Rules SHALL allow:
- Public reads on `feedback/` and `votes/`.
- Writes to `feedback/{id}` only when required fields (`category`, `sentiment`, `createdAt`) are present.
- Writes to `votes/{itemId}/{voterId}` only when `$voterId` equals the key being written (self-keyed, prevents vote-stuffing under an arbitrary key).
- No PII field names (`name`, `email`, `supervisor`) SHALL be writable.

#### Scenario: Valid feedback write is accepted
- **WHEN** a client writes a feedback node with `category`, `sentiment`, and `createdAt` fields
- **THEN** the RTDB rules permit the write

#### Scenario: Feedback write missing required fields is rejected
- **WHEN** a client writes a feedback node without `category` or `sentiment`
- **THEN** the RTDB rules reject the write

#### Scenario: Vote write with mismatched voterId is rejected
- **WHEN** a client writes `votes/{itemId}/someOtherId` with a key that differs from `$voterId`
- **THEN** the RTDB rules reject the write

#### Scenario: Rules deploy without error
- **WHEN** a developer runs `firebase deploy --only database`
- **THEN** the deployment succeeds using the committed `firebase.json` and `database.rules.json`

---

### Requirement: AppContainer wires FirebaseCommunityRepository for production
`AppContainer` SHALL construct `FirebaseCommunityRepository(FirebaseDatabase.getInstance().reference)` as the `community` binding. The `InMemoryCommunityRepository` SHALL NOT be used in the production build path.

#### Scenario: Community data persists across app restarts
- **WHEN** a user posts feedback and restarts the app
- **THEN** the feedback item appears in the newsfeed (retrieved from Firebase RTDB)

#### Scenario: Multi-device vote sync
- **WHEN** user A votes on an item on Device 1
- **THEN** the updated vote count is reflected on Device 2 within the next RTDB update cycle

---

### Requirement: Seed data is available for the Firebase Emulator
A committed seed JSON file (`scripts/seed.json`) and an accompanying shell script or documented `firebase database:set` command SHALL allow developers to pre-populate the Firebase Emulator with the four canonical demo feedback entries. Seed entries SHALL mirror the `CommunityFeedback` RTDB schema and SHALL NOT include PII (names/emails); they use anonymized `userId` values from the desk allocation dataset.

#### Scenario: Developer seeds the emulator
- **WHEN** a developer runs the seed script against a running emulator
- **THEN** the newsfeed shows four pre-populated feedback entries matching the demo scenario

---

### Requirement: Firebase onboarding is documented
A `README-firebase.md` (or equivalent section in the main README) SHALL document:
1. How to create / join the Firebase project and download `google-services.json`.
2. How to deploy RTDB security rules.
3. How to run the Firebase Emulator and seed it for local development.

#### Scenario: New developer can run the app against the emulator
- **WHEN** a developer follows the README-firebase.md instructions from a clean checkout
- **THEN** they can run the debug build connected to the local emulator with seeded data, without access to the real Firebase project
