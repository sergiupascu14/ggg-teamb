## Why

Office occupants have no quick, structured way to share how they feel about the office — both reporting facilities issues (broken elevators, dirty kitchens, desk problems) and celebrating the things they love — or to manage shared resources like the freezer, so problems go unreported, good things go unrecognized, and shared spaces degrade. This change introduces an Android app that captures everyday office signal ("Daily Pulse"), lets people both raise issues and spread love for what they enjoy, turns actionable feedback into facilities tickets, and builds a transparent, gamified community around keeping the office healthy.

## What Changes

The full feature set below is in scope for the MVP. The phase labels denote build *order*, not what ships — everything is implemented as part of the MVP.

- Bootstrap the existing empty Jetpack Compose app (`com.example.teamb`) into a multi-screen application with navigation, local persistence, and notifications.
- **Core (build order 1)** — onboarding that captures identity/location/desk, a Daily Pulse prompt with push notifications, freezer food check-in/check-out, an enhanced feedback form (positive and issue feedback with photos, categories, anonymity, location, visibility), and a ticketing/routing workflow with a personal "My Tickets" tracker.
- **Community & engagement (build order 2)** — a community newsfeed of shared feedback (backed by Firebase Realtime Database) with voting and location filtering, plus gamification (Daily Pulse streaks, leaderboard, ranking, rewards). For privacy, user identities stay on-device; cloud records link feedback only by `userId`, and names are resolved locally.
- **Advanced features (build order 3)** — AI detection of issues from uploaded photos and smart freezer-cleanup reminders.
- For the hackathon/demo scope, external integrations (GarminAD directory, #CLU-Facilities email, Jira) are implemented behind abstractions with mock implementations so the workflow is demoable without live credentials.

## Capabilities

### New Capabilities
- `user-onboarding`: Let the user select their identity from a searchable list of Garmin employees (loaded from the desk allocation dataset), set up an account password, and capture building/office location, floor number, and specific desk area (e.g. "T6-C2-01"); includes password login on return and a sign-out option.
- `daily-pulse`: Present the daily "Daily Pulse" prompt and deliver daily push notifications reminding users to check in.
- `freezer-management`: Track personal food items in the shared freezer via check-in/check-out, with smart reminders to clear out old food.
- `feedback-form`: Submit either positive (appreciation) or issue feedback with photo upload, category selection (Elevators, Kitchen, Desk Area, etc.), anonymous-or-identified toggle, optional location, and a community-visibility setting.
- `ticketing-system`: Optionally create a ticket from feedback, suppress tickets for positive feedback, route actionable feedback to #CLU-Facilities email or a mock Jira ticket, and let users track ticket status in a "My Tickets" dashboard.
- `community-newsfeed`: Display community-visible feedback in a newsfeed, allow voting on items, and filter by location (building/floor).
- `gamification`: Track Daily Pulse streaks and provide a leaderboard, ranking system, and rewards program.
- `ai-photo-detection`: Automatically detect and categorize issues from uploaded feedback photos.

### Modified Capabilities
<!-- None — this is a greenfield application; no existing specs. -->

## Impact

- **Affected code**: `app/` module — new screens, navigation graph, data layer, and notification scheduling under `com.example.teamb`. Replaces the placeholder `Greeting` content in `MainActivity.kt`.
- **Dependencies (new)**: Navigation Compose, a local persistence layer (Room/DataStore), Firebase Realtime Database (shared newsfeed feedback + votes), WorkManager + notifications for Daily Pulse/freezer reminders, an image picker/camera + image loading, and an HTTP/email client for ticket routing. AI photo detection adds an on-device or cloud vision dependency.
- **External systems**: GarminAD (user directory), #CLU-Facilities mailbox, and Jira — all integrated behind interfaces with mock implementations for the demo.
- **Reference data**: the desk allocation dataset in `docs/desk-allocation/` (`AI_AGENT_GUIDE.md` + anonymized `.xlsx`) defines the canonical desk ID format (`{Building}{Floor}-{Zone}{Row}-{DeskNum}`, e.g. `T6-C2-01`), the building/floor enumeration (Tower 3–6, Riviera 3–5), zones A–D, and the Staff ID → name/supervisor/desk mapping. It seeds desk/building/floor pickers, identity lookup, and newsfeed location filters.
- **Permissions**: camera/photo access, POST_NOTIFICATIONS, and (optionally) network access.
- **Risk**: low — greenfield app with no production users; external integrations are mocked.
