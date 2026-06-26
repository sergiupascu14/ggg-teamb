# Agent Context — CLOOJ Android App

## What is this project?

**CLOOJ** = Cluj Location Office Optimization Journey  
Android app (Kotlin, Jetpack Compose, Material3) for workplace feedback at the Garmin Cluj office.  
Package: `com.example.teamb` | minSdk 24, targetSdk 35, JVM 17 | Single `app/` module.

## Recent commits (newest first)

```
1b007fb docs: expand CLOOJ acronym in README
e65392b feat: Hub rename, ice cube icon, mood order, toggle visibility, password eye icon + graph fix
b3696a3 fix: add navigationBarsPadding to onboarding and login screens
2f603cd feat: kitchen auto-fill, community photos, auto-login, skeleton loaders + docs
9e5be82 feat: rebrand to CLOOJ + official Garmin logo
```

## Architecture

- **MVVM** + repository layer
- **Room + DataStore** for all local persistence (pulse check-ins, profile, credentials)
- **Firebase Realtime Database** (`clooj-ggg`) for shared surfaces only: community newsfeed/votes, fridge occupancy, and weekly pulse
- **EncryptedSharedPreferences** for salted-hash password storage (`teamb_credentials`)
- Manual DI via `AppContainer`
- External integrations (GarminAD, Jira, email, AI vision) are behind interfaces with mock implementations

## Key file map

```
app/src/main/java/com/example/teamb/
  AppContainer.kt                          # manual DI — all singletons
  ui/navigation/
    Destinations.kt                        # Tab enum (Pulse/Spaces/Hub/Report/Profile) + Routes
    AppNav.kt                              # root composable: onboarding vs login vs main
  ui/onboarding/
    OnboardingScreen.kt                    # 3-step onboarding + LoginScreen
    OnboardingViewModel.kt
    LoginViewModel.kt
  ui/components/
    Fields.kt                              # AppTextField + PasswordTextField (eye-icon toggle)
    Buttons.kt, Cards.kt, Headers.kt, ...
  ui/dailypulse/
    DailyPulseScreen.kt                    # mood picker, weekly graph, skeleton loaders
    DailyPulseViewModel.kt                 # local check-in + Firebase weekly sync
  ui/feedback/
    FeedbackScreen.kt                      # positive/issue feedback, photo, toggles, ticket
    FeedbackViewModel.kt
  ui/newsfeed/
    NewsfeedScreen.kt                      # Hub (community) feed, votes, photos, filters
  ui/spaces/
    KitchenDetailScreen.kt                 # fridge occupancy, freezer check-in/out
  ui/profile/
    ProfileScreen.kt                       # streak, rewards, leaderboard, sign out
  data/
    sync/
      FirebasePulseRepository.kt           # pulse/{date}/{userId} → {mood, building, floor}
      FirebaseCommunityRepository.kt       # community feed + votes
      FirebaseFridgeRepository.kt          # shared fridge occupancy
      PulseSync.kt                         # PulseRepository interface + InMemoryPulseRepository
      PulseAggregator.kt                   # weekly() — builds WeeklyPulse from records
    repository/
      Repositories.kt                      # DailyPulseRepository (Room), FreezerRepository
    datastore/
      ProfileStore.kt                      # DataStore<Profile>
      CredentialStore.kt                   # EncryptedSharedPreferences password + session flag
    model/Models.kt                        # all data classes (Profile, Employee, PulseRecord, etc.)
    util/Time.kt                           # Dates utility (isoDate, lastSevenDays, weekdayInitial)
  ui/theme/Color.kt                        # all theme colors (Canvas, Navy, GarminBlue, TextMuted, etc.)
```

## Important conventions

### Building
Always use Android Studio's JDK (Java 25 system JDK breaks the build):
```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug
```
Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

### Privacy rule (non-negotiable)
- Firebase only stores: `userId` (Staff ID string), content fields, location codes — never names, emails, supervisors
- Display names resolved locally from bundled desk allocation dataset
- Anonymous submissions carry no `userId`

### Newsfeed is "Hub" (not "Community")
The community tab was renamed from "Community" to "Hub" in `Destinations.kt` and `NewsfeedScreen.kt`.

### Password fields
Use `PasswordTextField` from `Fields.kt` (not raw `AppTextField` + `PasswordVisualTransformation`).  
`PasswordTextField` includes the built-in eye-icon toggle. Used in `OnboardingScreen.kt` (step 3) and `LoginScreen`.

### Mood picker
`MOODS = listOf("😄", "🙂", "😐", "🙁", "😞")` — happy→sad left-to-right.  
Value mapping: `val value = MOODS.size - index` so 😄=5, 😞=1.  
File: `DailyPulseScreen.kt` line ~74.

### Freezer icon
Freezer items use 🧊 (ice cube) not 🥚. `KitchenDetailScreen.kt` `FreezerRow()`.

### Toggles (FeedbackScreen)
`SwitchDefaults.colors(checkedTrackColor = Navy, uncheckedTrackColor = InputFill, uncheckedBorderColor = TextMuted, uncheckedThumbColor = TextMuted)` — makes the OFF state clearly visible on the dark theme.

### Graph / check-in sync bug fix
`DailyPulseViewModel.observeWeek()` now also sets `checkedInToday = true` when Firebase has a record for today's date + this user, preventing the form and graph from contradicting each other (e.g. after reinstall or cross-device scenario).

## Sign-out behaviour
`signOut()` in `AppNav.kt` calls both `credentialStore.clear()` AND `profileStore.clear()`.  
This wipes password + session, so the app returns to **Onboarding** (not Login).  
The **Login** screen only appears when `hasPassword() && !isLoggedIn()` — a narrow window (e.g. between password creation and first unlock).

## Desk allocation dataset
Lives in `docs/desk-allocation/`. Key guide: `docs/desk-allocation/AI_AGENT_GUIDE.md`.  
Format: `{Building}{Floor}-{Zone}{Row}-{DeskNum}` e.g. `T6-C2-01`, `R4-F6-04`.  
Buildings: Tower (T, floors 3–6), Riviera (R, floors 3–5).

## Testing
- ≥90% unit test coverage enforced by JaCoCo (`jacocoTestCoverageVerification`)
- Run tests separately from assemble: `./gradlew jacocoCoverageVerification`
- Pure `@Composable`, generated, and DI code excluded from denominator

## Firebase
- Project: `clooj-ggg`
- Config committed at `app/google-services.json`
- RTDB rules at `database.rules.json`
- Layout:
  - `pulse/{date}/{userId}` → `{mood, building, floor, updatedAt}`
  - `community/{id}` → feedback posts
  - `votes/{postId}/{userId}` → vote records
  - `fridges/{location}` → fridge occupancy

## OpenSpec
Active change spec lives in `openspec/changes/office-feedback-app/`.  
Use `/opsx:*` skills to propose/apply/archive changes.

## ADB device (emulator)
- Device: `emulator-5554`
- Screen: 1280×2856 px, 480 dpi
- Bottom nav tap coordinates (y≈2700): Pulse x=128, Spaces x=384, Hub x=640, Report x=896, Profile x=1152
