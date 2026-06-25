# CLOOJ — Test & Feature Report

_Android app for the Garmin Cluj office. Package `com.example.teamb`._
_Built with JDK 21; verified on Android emulator (`emulator-5554`)._

## Summary

| Check | Result |
|---|---|
| Unit tests | **174 passed**, 0 failed, 0 skipped (27 test classes) |
| Line coverage | **~97%** (90% JaCoCo gate enforced — passing) |
| Instruction coverage | ~92% |
| Branch coverage | ~72% |
| Debug build | ✅ `assembleDebug` succeeds |
| On-device smoke test | ✅ all key flows verified |

Excluded from the coverage denominator (by design): pure Compose UI/screens, theme, navigation,
DI/`AppContainer`, and platform-bound integrations (Firebase, DataStore/EncryptedSharedPreferences,
ML Kit, photo encoder, notifications). ViewModels, repositories and pure logic are **in** scope.

## Features (current)

- **Onboarding & auto-login** — searchable directory, auto-detected desk, local password. The session
  is **remembered**, so the app logs in automatically on relaunch; **sign out** clears it.
- **Daily Pulse** — daily mood check-in, streaks, a rolling 7-day trend graph, reminders, skeleton
  loaders while loading.
- **Spaces & Kitchen** — live per-floor shared-fridge occupancy (synced), office-freezer check-in/out,
  a **dynamic** Kitchen Pulse driven by real fridge fullness, and quick-report shortcuts that open the
  feedback form **pre-filled** and shared to the community.
- **Report & Feedback** — positive/issue feedback, categories, on-device AI photo categorization,
  anonymous/public, facilities ticketing (mock Jira / email; positive never tickets). Submitting shows
  a confirmation toast and **returns instantly**.
- **Community newsfeed** — shared feedback with voting, **redesigned Building/Floor filter dropdowns**
  (chevron affordance; Floor disabled until a Building is picked), **inline photos** with a
  **full-screen viewer**, and skeleton loaders.
- **Profile & Gamification** — streaks, rewards, leaderboard ("Office Champion"), and a light/dark/
  system appearance toggle.
- **Branding** — CLOOJ app name + launcher icon; official Garmin wordmark in headers.

## Notable changes this cycle

1. **Kitchen auto-filled reports** — fridge "Report" and quick actions pre-fill a tailored message
   (with live fridge %) and share to the community.
2. **Report quick chips** — preselect a category, pre-fill a message, and share to the community.
3. **Dynamic Kitchen Pulse** — reflects live fridge fullness instead of a hardcoded value.
4. **Auto-back / instant return (bug fix)** — after a successful submit the form returns to the
   previous screen immediately (confirmation via toast). The earlier failure was a navigation effect
   cancelling itself when the result was consumed; fixed by reordering, then made instant.
5. **Photos in the community feed (new)** — photos are downscaled, JPEG-compressed and base64-encoded,
   stored with the feedback record in Firebase RTDB (no Firebase Storage needed; no-PII), and rendered
   inline; tapping opens a **full-screen viewer**.
6. **Auto-login (new)** — a remembered-session flag in encrypted prefs skips the lock screen on
   relaunch and is cleared on sign out.
7. **Skeleton loaders (new)** — shimmer placeholders for the Firebase-backed surfaces (Community,
   Leaderboard, Daily Pulse).
8. **Filter dropdown redesign** — chevron icons, on-theme menus with a check on the selected option,
   and a clearly-disabled Floor chip.

## New-logic test coverage

- `FeedbackViewModelTest` — community submit with a photo encodes once and publishes the base64;
  non-community submit never encodes/publishes.
- `LoginViewModelTest` / `OnboardingViewModelTest` — session is remembered on successful login /
  onboarding, and **not** on failure / password mismatch.
- `PhotoEncoderTest` — the no-op (test/in-memory) encoder returns null.
- Existing suites (feedback, community, newsfeed, gamification, pulse, fridge, tickets, desk, auth,
  dates/streak) continue to pass.

## On-device verification

| Flow | Result |
|---|---|
| Onboarding → desk auto-detected → password → **straight to main** | ✅ |
| Relaunch → **auto-logs in** (no lock screen) | ✅ |
| Sign out → onboarding; relaunch → onboarding (no stale auto-login) | ✅ |
| Report quick chip → form pre-filled, community share ON | ✅ |
| Submit feedback → confirmation → **instant return** | ✅ |
| Submitted post appears in Community with locally-resolved name | ✅ |
| Fridge "Report" → message names floor/building + live % + community ON | ✅ |
| Kitchen Pulse changes with fridge occupancy (20% → 90%) | ✅ |
| Attach photo → share → photo renders inline → **full-screen** on tap | ✅ |
| Community filters → Floor disabled until Building chosen → floors list | ✅ |
| Skeleton loaders show on Community & Leaderboard while loading | ✅ |

## How to reproduce the checks

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"   # or Android Studio's bundled JBR
./gradlew :app:assembleDebug                 # build
./gradlew :app:testDebugUnitTest             # unit tests
./gradlew :app:jacocoCoverageVerification    # tests + 90% coverage gate
```
> Run `assembleDebug` and the `jacoco*` tasks as **separate** Gradle invocations.

## Notes / limitations

- Photos are base64 in the shared feed, kept small on purpose — sized for demo volumes, not a
  high-traffic photo service (Firebase Storage would be the next step at scale).
- Auto-filled feedback messages are templates with live values injected, not AI-generated.
- The dark-mode Garmin reverse logo tints the whole mark white (single flattened asset); a dedicated
  reverse asset would preserve the blue triangle on dark surfaces.
- On Daily Pulse the skeleton is brief (check-in status loads from local Room); the Community and
  Leaderboard skeletons cover the real Firebase latency.
