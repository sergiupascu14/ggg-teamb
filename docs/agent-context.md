# Agent Context — CLOOJ Office (Garmin) App

Compact handoff. Android Jetpack Compose office-feedback app for the Garmin **Cluj ("CLOOJ")**
office. Brand: Garmin. Package `com.example.teamb`.

## Stack / build
- Kotlin, Compose, Material3. minSdk 24, targetSdk 35, JVM 17. Gradle 8.11.1, AGP 8.7.3, Kotlin 2.0.21.
- **MUST build with JDK 21** (system Java is too new): `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- Build: `./gradlew :app:assembleDebug`. Tests + 90% gate: `./gradlew :app:jacocoCoverageVerification` (wired into `check`).
- **Run these two in SEPARATE Gradle invocations** — running `assembleDebug` + `jacoco*` together trips a Gradle task-ordering validation (a committed APK now lives under `app/build/`). Emulator: `emulator-5554`.
- ADB text input is hijacked by a stylus-handwriting promo; disable: `adb shell settings put secure stylus_handwriting_enabled 0`.
- Firebase is **LIVE**: `google-services.json` IS committed (project `clooj-ggg`, RTDB `europe-west1`). Don't commit `gradle.properties` `org.gradle.java.home`. `.gitignore` excludes `build/`, `node_modules/` (but an `app/build/...apk` got tracked historically).
- Shareable debug APK lives at `~/Desktop/clooj-teamb-debug.apk` (~64 MB, self-signed; release would need a signing keystore).

## Architecture
- MVVM + repositories + integration interfaces (mocks). Manual DI: `AppContainer` (held by `TeamBApp`; reach via `(application as TeamBApp).container`).
- Persistence: Room (`AppDatabase` **v3**, `fallbackToDestructiveMigration`) + DataStore (`ProfileStore`→`UserProfile`, `SettingsStore`→`ThemeMode`) + `EncryptedCredentialStore` (salted hash; `PasswordHasher` pure/testable).
- Local repos: `DailyPulseRepository`, `FreezerRepository`, `FeedbackRepository` (+`FeedbackForm`/`SubmitResult`), `TicketRepository`, `GamificationRepository`. Util `Clock`/`Dates`/`StreakCalculator` (`data/util`).
- **Firebase-synced repos** (`data/sync/`, interface + `InMemory*` + `Firebase*`): `CommunityRepository` (newsfeed+votes, actually `data/community/`), `FridgeRepository`, `PulseRepository`. Pure logic: `PulseAggregator`, `Fridges` helper.
- Keep logic in repos/ViewModels/use-cases (unit-testable). JaCoCo excludes pure `*Screen*`, `ui/theme`, `ui/components`, `ui/navigation`, DI, and Android/Firebase-bound impls (`Firebase*Repository`, DataStore stores, ML Kit, notifications). **ViewModels are NOT excluded → they need tests.** Add the new `Firebase*`/`DataStoreSettingsStore` to `coverageExclusions` in `app/build.gradle.kts` when adding more.

## Privacy model (non-negotiable)
- Identities never leave device. Firebase records link **only by `userId` (= Staff ID)** plus content (mood, category, sentiment, **building/floor**, votes, occupancy) — never names/supervisors/emails. Anonymous = `null` userId. Display names resolved **locally** via `desk.displayName(id)?.toDisplayName()`. `database.rules.json` validates the no-PII shape for `feedback`/`votes`/`pulse`/`fridges`.

## Desk allocation dataset
- Bundled asset `app/src/main/assets/desk_allocation.json` (648 employees, 754 desks), from `docs/desk-allocation/Desk_Allocation_Anonymized.xlsx`. Never commit real `Desk_Allocation.xlsx`.
- `DeskAllocationRepository`: `employees`, `desks` (each has `building`/`floor`/`staffId`), `searchEmployees`, `employeeById`, `deskForStaff`, `displayName`, `buildings()`, `floorsFor`, `parseDeskId`.
- Desk ID: `{T|R}{floor}-{Zone}{Row}-{DeskNum}` e.g. `T6-C2-01`, `R4-F6-04`. Zones A–H, rows 1–7, 1–2 digit deskNum. Tower floors 3–6, Riviera 3–5.

## Design system + dark mode — `ui/theme`, `ui/components`
- **Brand color is `BrandSky #6DCFF6`** (logo accent, hero gradients, identity). Solid fills use `Navy #1E3A8A` with `OnBrand #FFFFFF` text. Also `BrandCyan #19A9E5`, `GarminBlueMid #005BAC`. `BrandGradient` = navy→blue→sky.
- **Dark mode is real**: `Color.kt` defines `AppColors` (data class) with `LightAppColors`/`DarkAppColors` and `LocalAppColors`. Semantic tokens (`Canvas`, `CardSurface`, `CardBorder`, `InputFill`, `InputBorder`, `AccentBlue`, `TextPrimary/Secondary/Muted/Disabled`, `Positive/Issue/Warning*`, and **`GarminBlue`** = foreground accent) are **`@Composable` getter `val`s** that read `LocalAppColors`.
  - GOTCHA: those tokens can only be used in `@Composable` scope. For solid fills carrying white text use the constant `Navy`/`OnBrand`, NOT `GarminBlue`/`CardSurface`.
- Theme: `TeamBTheme(darkTheme)` provides `LocalAppColors` + builds the M3 scheme. `MainActivity` resolves `darkTheme` from `SettingsStore.themeMode` (`ThemeMode.SYSTEM/LIGHT/DARK`, `isDark(systemDark)`); toggle lives in Profile ("Appearance").
- Components: `GarminHeader`, `GarminLogo(onDark)`, `ScreenTitle(title, streak?, subtitle?)`, `SurfaceCard(modifier?, padding=20){}`, `PrimaryButton`, `OutlinedPillButton(leadingIcon?)`, `AppTextField`, `InfoBanner`, `Tag`, `FieldLabel`, `BrandGradient`. Name helper `ui.util.toDisplayName()`.
- **Screen pattern** (tabs): `Column(fillMaxSize().verticalScroll){ GarminHeader(); Column(padding(horizontal=20).padding(top=16,bottom=20)){ ScreenTitle(...); cards } }`. Bottom nav + Canvas bg + insets come from `MainScaffold` — do NOT add Scaffold/nav/status-bar padding in tab screens. Onboarding/Login are standalone (`.background(Canvas).statusBarsPadding().imePadding()`).
- 12-screen design source: `~/Downloads/clooj-garmin-building-icon-12-screens.svg` (visually faithful, not pixel-identical).

## Navigation (`ui/navigation/AppNav.kt`, `Destinations.kt`)
- Tabs (order): **Pulse · Spaces · Community (middle) · Report · Profile**. Routes: `pulse`, `spaces`, `kitchen`, `report`, `feedback?category={category}`, `newsfeed`, `profile`, `tickets`, `leaderboard`.
- **Landing**: `MainScaffold` starts on **Community** if the current user already checked in today, else **Pulse** (per-user `checkedInToday`).
- `AppRoot` routes onboarding → login → main. **Sign out fully clears** profile + credential (→ onboarding), not just re-lock.

## Features / screens
- **Daily Pulse** — per-user mood check-in (once/day). `DailyPulseViewModel(repository, pulseSync, clock)` + `configure(userId, building, floor)`; `submit` no-ops if already checked in or no user. Shows a **rolling last-7-days line graph** (You=orange / Floor=cyan / Company=sky) + company & floor averages, drawn on a Compose `Canvas`. `Dates.lastSevenDays` window; `Dates.weekdayInitial` labels. No "View trends" button.
- **Spaces** — `SpacesScreen` (Kitchen / Meeting Rooms / Quiet Zones). `KitchenDetailScreen`: **2 shared fridges per floor** with live synced occupancy + ±10% controls (`FridgeRepository`, keyed by `Fridges.floorKey(building,floor)` e.g. "R4"), plus the personal **office freezer** check-in/out (`FreezerViewModel`, local), Kitchen Pulse, "You said → we did".
- **Report** — `ReportCategoryScreen` (category grid + quick chips) → `FeedbackScreen(initialCategoryName)`. Positive (default) vs Issue; AI photo categorization; toggles (anonymous/community/ticket); location prefilled from profile.
- **Community** — newsfeed (vote + building/floor filters), "What's happening around CLOOJ."
- **Profile** — gradient hero (sky subtitle "{Building} · Cluj"), workplace info, streak, rewards (with info tooltip + progress), Appearance toggle, My Tickets / Leaderboard, Sign out.
- **My Tickets** + **Leaderboard** (Office Champion 👑, pinned current-user row).
- Ticketing: positive feedback never tickets; `MockJiraTicketRouter` (`JIRA-####`). AI photo: `PhotoIssueDetector.analyze` (ML Kit on-device + mock), camera via FileProvider.

## Data sync details
- Firebase RTDB layout: `feedback/{id}`, `votes/{id}/{voterId}`, `pulse/{date}/{userId}={mood,building,floor,updatedAt}`, `fridges/{floorKey}/{fridgeId}={occupancy,updatedBy,updatedAt}`.
- Pulse seeded for demo via REST `PATCH` (multi-path, merges): see throwaway script logic that maps real staff IDs→their building/floor and writes moods for a date range. The graph is data-only for the rolling 7-day window — re-seed when the week moves.
- Models: `FridgeOccupancy`, `PulseRecord`, `WeeklyPulse`/`WeeklyPulsePoint`, `ThemeMode`. `Dates`: `isoDate`, `epochDay`, `currentWeekDates`, `currentWeekToDate`, `lastSevenDays`, `weekdayInitial`.

## ADB / emulator tips
- Device: `emulator-5554`. **A physical device (RZCX51LJTNP) is also connected** — always pass `-s emulator-5554` to every `adb` command.
- Screen: 1280×2856 px @ 480 dpi. Bottom nav bounds (actual): Pulse x=118 y=2664, Spaces x=378, Hub x=640, Report x=900, Profile x=1162.
- Content cards start at ~y=566 (first card center ~y=698). **Do not guess y-coords from image proportions** — use `adb -s emulator-5554 shell uiautomator dump` to get exact element bounds.
- Screenshots: `adb -s emulator-5554 shell screencap -p /sdcard/screen.png && adb -s emulator-5554 pull /sdcard/screen.png /tmp/screen.png`. Resize with `sips -Z 400` before reading images (raw 1280×2856 exceeds API limits).
- Disable stylus promo once per session: `adb -s emulator-5554 shell settings put secure stylus_handwriting_enabled 0`.

## Git / state
- Remote `origin git@github.com:sergiupascu14/ggg-teamb.git`. **`main` is the integration branch and is current**. All session work (CLOOJ redesign, Firebase fridge+pulse sync, dark mode, nav reorder + Hub landing, per-user check-in fix, full sign-out) is merged into `main`.
- OpenSpec plans in `openspec/changes/` (validate with `openspec validate "<change>"`).
- Status: builds clean (JDK 21), unit tests pass, **90% JaCoCo gate green**, Firebase live, verified on `emulator-5554` (incl. dark mode).
