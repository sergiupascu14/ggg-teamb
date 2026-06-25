## 1. Firebase Project Setup (manual / operator steps)

- [x] 1.1 Create (or reuse) a Firebase project in the Firebase Console and enable the Realtime Database — **project `clooj-ggg`**
- [x] 1.2 Download `google-services.json` from the Firebase Console and place it at `app/google-services.json` (already gitignored) — **done**
- [x] 1.3 Note the RTDB URL — **`https://clooj-ggg-default-rtdb.europe-west1.firebasedatabase.app`**

## 2. Emulator Config & Version-Controlled Files

- [x] 2.1 Create emulator-targeted `google-services.json` template (project `clooj-ggg`, `localhost:9000`) at `scripts/firebase/google-services.emulator.json` — copy to `app/src/debug/google-services.json` (gitignored) when running against the emulator
- [x] 2.2 Commit `database.rules.json` at the repo root as a reference copy of the RTDB security rules (public reads, validated writes on `feedback/`, self-keyed votes, no PII fields)

## 3. Seed Data Script

- [x] 3.1 Create `scripts/seed.json` containing the four canonical demo feedback entries in RTDB JSON format (matching the `feedback/{id}` schema, using anonymized `userId` values, no PII)
- [x] 3.2 Document the seed command (`firebase database:set / scripts/seed.json --project clooj-ggg`) in a `README-firebase.md`

## 4. AppContainer Wiring

- [x] 4.1 In `AppContainer.kt`, replace `InMemoryCommunityRepository(seedCommunity())` with `FirebaseCommunityRepository(FirebaseDatabase.getInstance().reference)`
- [x] 4.2 Remove (or move to a dev-only utility) the `seedCommunity()` method from `AppContainer` — it is no longer called in the production path
- [x] 4.3 Add the `com.google.firebase.database.FirebaseDatabase` import; verify the build compiles cleanly

## 5. RTDB Security Rules Deployment

- [ ] 5.1 Open Firebase Console → project `clooj-ggg` → Realtime Database → **Rules** tab
- [ ] 5.2 Paste the contents of `database.rules.json` and click **Publish**

## 6. Documentation

- [x] 6.1 Create `README-firebase.md` covering: (a) how to create/join the Firebase project and place `google-services.json`, (b) how to deploy RTDB rules, (c) how to run the Firebase Emulator and seed it for local dev
- [x] 6.2 Add a short "Firebase Setup" section to the main `README.md` (or existing docs) linking to `README-firebase.md`

## 7. Smoke Test

- [ ] 7.1 Run the app on a physical device (or emulator with the Firebase Emulator backend) and verify the newsfeed loads data from RTDB
- [ ] 7.2 Post a feedback item and confirm it appears in the Firebase Console → Realtime Database viewer
- [ ] 7.3 Toggle a vote from two separate devices/emulators and confirm the vote count syncs in real time
- [x] 7.4 Verify the build still passes JaCoCo coverage gate (`./gradlew jacocoTestCoverageVerification`) — `FirebaseCommunityRepository` is excluded from the denominator
