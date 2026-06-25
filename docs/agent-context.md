# Agent Context — TeamB Office (Garmin) App

Compact handoff. Android Jetpack Compose office-feedback app for a Garmin office.

## Stack / build
- Kotlin, Compose, Material3, `com.example.teamb`. minSdk 24, targetSdk 35, JVM 17. Gradle 8.11.1, AGP 8.7.3, Kotlin 2.0.21.
- **MUST build with JDK 21** (system Java 25 is too new): `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- Build: `./gradlew :app:assembleDebug`. Tests + 90% gate: `./gradlew :app:jacocoCoverageVerification` (wired into `check`). Emulator: `emulator-5554`.
- ADB text input is hijacked by a stylus-handwriting promo; disable: `adb shell settings put secure stylus_handwriting_enabled 0`.
- Never commit `gradle.properties` `org.gradle.java.home` (machine-specific) or `google-services.json`. `.gitignore` excludes `build/`, `app/build/`, `node_modules/`.

## Architecture
- MVVM + repositories + integration interfaces (mocks). Manual DI: `AppContainer` (held by `TeamBApp` Application; reach via `(application as TeamBApp).container`).
- Persistence: Room (`AppDatabase` v2; entities `DailyPulseEntity`, `FreezerItemEntity`, `FeedbackEntity`, `TicketEntity`) + DataStore (`ProfileStore` → `UserProfile`) + `EncryptedCredentialStore` (salted password hash; `PasswordHasher` is pure/testable).
- Repos: `DailyPulseRepository`, `FreezerRepository`, `FeedbackRepository` (+ `FeedbackForm`/`SubmitResult`), `TicketRepository`, `GamificationRepository`. Util: `Clock`/`Dates`/`StreakCalculator` (`data/util`).
- Keep logic in repositories/ViewModels (unit-testable); JaCoCo gate excludes pure UI screens, DI, and Android-bound integration (DataStore/Encrypted/Firebase/notifications/ML Kit).

## Privacy model (non-negotiable)
- Identities never leave device. Community/newsfeed records link **only by `userId` (= Staff ID)**; anonymous = `null` userId; display names resolved **locally** from the desk dataset via `desk.displayName(id)?.toDisplayName()`.

## Desk allocation dataset
- Bundled asset `app/src/main/assets/desk_allocation.json` (648 employees, 754 desks), generated from `docs/desk-allocation/Desk_Allocation_Anonymized.xlsx` (safe/anonymized). Never commit real `Desk_Allocation.xlsx`.
- `DeskAllocationRepository`: `employees`, `desks`, `searchEmployees(q)`, `employeeById`, `deskForStaff`, `displayName`, `buildings()`, `floorsFor(code)`, `parseDeskId`.
- Desk ID grammar: `{T|R}{floor}-{Zone}{Row}-{DeskNum}` e.g. `T6-C2-01`, `R4-F6-04`. **Zones A–H, rows 1–7, 1–2 digit deskNum** (the AI_AGENT_GUIDE says A–D/1–3 but the real data is wider — `DeskId.parse` matches the data). Tower floors 3–6, Riviera 3–5.

## Design system (Garmin) — `ui/theme` + `ui/components`
- Tokens: `GarminBlue #005BAC`, `Canvas #F6F8FC`, `AccentBlue #EAF4FF`, `CardSurface #FFF`, `CardBorder #E7EDF6`, `InputFill #F9FBFE`, `InputBorder #DDE6F2`, `TextPrimary #172033`, `TextSecondary`, `TextMuted #7A869A`, `PositiveBg/Text #E8F7EF/#1E8E5A`, `IssueBg/Text #FFF2E6/#C56A00`. Light theme only (no dark/dynamic).
- Components: `GarminHeader`, `GarminLogo(onDark)`, `ScreenTitle(title, streak?)`, `SurfaceCard(padding=20){}`, `PrimaryButton`, `OutlinedPillButton(leadingIcon?)`, `AppTextField`, `InfoBanner`, `Tag(text,bg,fg)`, `FieldLabel`. Name helper: `ui.util.toDisplayName()` (ALL-CAPS → Title Case).
- **Screen pattern** (main tabs): `Column(fillMaxSize().verticalScroll){ GarminHeader(); Column(padding(horizontal=20.dp).padding(top=16,bottom=20)){ ScreenTitle(...); cards } }`. Bottom nav + Canvas bg + insets come from `MainScaffold` (`ui/navigation/AppNav.kt`) — do NOT add Scaffold/bottom-nav/status-bar padding in tab screens.
- Onboarding/Login are standalone (outside MainScaffold): add `.background(Canvas).statusBarsPadding().imePadding()`.
- Reference screen: `ui/dailypulse/DailyPulseScreen.kt`. Design source of truth: `daily-pulse-variants.svg`, `garmin-office-collage.svg`, and `docs/ui-design-audit.md` (all audit items resolved).

## Features / screens
Onboarding (searchable employee picker → desk-derived location → password; "Step X of 3"), Login (password unlock + sign out), Daily Pulse (mood + note + streak + notification perm), Freezer (check-in/out, human dates, cleanup reminders), Share Feedback (Positive default; AI photo categorization), My Tickets, Community newsfeed (vote + building/floor filters), Profile (gradient header, workplace info, streak, rewards, My Tickets/Leaderboard), Leaderboard (Office Champion 👑, pinned current-user row).
- **Ticketing**: positive feedback never creates a ticket; `EmailTicketRouter` (mailto) / `MockJiraTicketRouter` (`JIRA-####`).
- **AI photo**: `PhotoIssueDetector.analyze(uri): PhotoCategorizationResult(detectedIssue, description, suggestedCategory, confidence, failure)`. Impls: `MlKitPhotoIssueDetector` (on-device) + `MockPhotoIssueDetector` + `PhotoIssueCategoryMapper`. `FeedbackForm.category` is nullable; `issueLabel` auto-filled from the draft; submit blocks while `PhotoDraftStatus.ANALYZING`. Camera capture uses FileProvider (`file_paths.xml`).

## Integrations (mocked; swap later)
- `DirectoryService` (GarminAD mock, backed by dataset), `TicketRouter` (email/mock-Jira), `CommunityRepository` (seeded in-memory default; `FirebaseCommunityRepository` ready but **needs `google-services.json`** to go live), `PhotoIssueDetector` (ML Kit / mock).

## Git / state
- Remote `origin git@github.com:sergiupascu14/ggg-teamb.git`. Work branch `feat/office-feedback-mvp`. Latest = merge `b6db990` (UI redesign + AI photo categorization). `main` has prior MVP+AI.
- OpenSpec planning in `openspec/changes/office-feedback-app/` (proposal/design/specs/tasks). Validate: `openspec validate "office-feedback-app"`.
- Status: builds clean, ~136 unit tests pass, ~97% line coverage. Firebase not live; AI photo uses ML Kit on-device.
