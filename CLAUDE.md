# CLAUDE.md

Guidance for AI agents working in this repository.

## Project

Android app (Kotlin, Jetpack Compose, Material3) for office feedback at a Garmin office.
Package `com.example.teamb`; `minSdk 24`, `targetSdk 35`, JVM 17. Single `app/` module.

The active change being planned/implemented lives in OpenSpec:
- `openspec/changes/office-feedback-app/` — proposal, design, specs (8 capabilities), tasks.
- Use the `openspec` CLI for status/validation; use the `/opsx:*` skills to propose/apply/archive.

Scope summary: onboarding (identity/building/floor/desk), Daily Pulse + notifications,
freezer check-in/out, positive **and** issue feedback (photos, categories, anonymous-or-public,
location, community visibility), ticketing/routing (#CLU-Facilities email or mock Jira),
"My Tickets" tracker, community newsfeed (voting + location filters), and gamification
(streaks, leaderboard with an "Office Champion" title — public feedback only).

## Desk Allocation Dataset — keep in mind when relevant

The office desk layout lives in `docs/desk-allocation/`:
- `AI_AGENT_GUIDE.md` — full reference for the desk allocation system. **Read it before doing
  any work involving desks, buildings, floors, zones, desk IDs, or employee↔desk lookups.**
- `Desk_Allocation_Anonymized.xlsx` — anonymized data, safe for demos/seeding/tests.
  Never commit a real (non-anonymized) `Desk_Allocation.xlsx`.

Key facts to honor across the app:
- **Desk ID format**: `{Building}{Floor}-{Zone}{Row}-{DeskNum}` (e.g. `T6-C2-01`).
  - Building: `T` = Tower, `R` = Riviera. Floors: Tower 3–6, Riviera 3–5.
  - Zone: A–D. Row: 1–3. DeskNum: two digits `00`–`99`.
- A desk ID fully encodes building, floor, zone and row — derive these from the user's desk
  area instead of asking again where possible, and use them for newsfeed building/floor filters.
- The workbook maps **Staff ID → Name, Supervisor, Building, Floor, Desk** via the
  `All Associates` master sheet and per-floor sheets (`Tower_3`…`Riviera_5`). This is the
  local source of truth for identity/desk lookups and a stand-in/complement for GarminAD.
- Use the canonical building/floor enumeration (Tower 3–6, Riviera 3–5) and zones A–D wherever
  the UI offers building/floor/zone pickers or filters — don't hardcode different values.

When a task touches desks/locations/employees, consult the guide first and keep the app's
desk format, building/floor enumeration, and lookup behavior consistent with it.

## Newsfeed & Privacy — non-negotiable

- The community **newsfeed and its votes use Firebase Realtime Database** (the one shared,
  multi-device surface). All other data is local.
- **User identities never go to the cloud.** The user list/profile is stored only locally on
  each device (DataStore + the bundled desk allocation dataset). Records written to Firebase are
  linked **only by `userId`** (Staff ID) plus content (category, sentiment, location, photo ref,
  votes) — never names, supervisors, or emails. Anonymous submissions carry no `userId`.
- Display names are **resolved locally** from `userId` via the bundled desk allocation dataset
  (identical on every phone), so the feed shows names without storing PII in Firebase. Honor this
  in any code that reads/writes newsfeed, votes, or leaderboard data.

## Conventions

- Match the existing Compose/Material3 style. MVVM + repository layer; Room + DataStore for
  local persistence, Firebase Realtime Database for community data
  (see `openspec/changes/office-feedback-app/design.md`).
- External integrations (GarminAD, #CLU-Facilities email, Jira, AI vision) sit behind
  interfaces with mock implementations for the demo.
- **Testing: maintain ≥90% unit test coverage, enforced by JaCoCo as a build-failing gate**
  (`jacocoTestCoverageVerification`). Keep logic in ViewModels/repositories/use-cases so it's
  unit-testable with fakes; pure `@Composable`/UI, generated, and DI code are excluded from the
  coverage denominator. Add/adjust tests with every code change — don't let coverage regress.
- Don't commit build artifacts or secrets. Commit/push only when explicitly asked.
