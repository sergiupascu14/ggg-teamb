# CLOOJ — QA Test Cases

Manual test cases for the CLOOJ Android app (Garmin Cluj office feedback app).

**How to use:** Execute each case on a debug build (`./gradlew installDebug`) on an Android device or
emulator (minSdk 24 / Android 7.0+). Record **Pass / Fail** and attach a screenshot for any failure.
Priority: **P1** = critical path / must pass before release, **P2** = important, **P3** = nice-to-have.

**Legend:** "fresh install" = app data cleared (Settings → Apps → CLOOJ → Clear storage, or
`adb shell pm clear com.example.teamb`).

---

## 1. Onboarding & Identity

| ID | Title | Pri | Preconditions | Steps | Expected result |
|----|-------|-----|---------------|-------|-----------------|
| TC-ONB-01 | First launch shows onboarding | P1 | Fresh install | Launch the app | "Welcome to CLOOJ", Step 1 of 3 "Who are you?" with a searchable employee list is shown |
| TC-ONB-02 | Search filters the directory | P2 | On Step 1 | Type a name or staff ID in the search box | The list narrows to matching employees in real time |
| TC-ONB-03 | Continue is blocked until an identity is chosen | P1 | On Step 1, nothing selected | Tap **Continue** without selecting | Cannot proceed; an employee must be selected first |
| TC-ONB-04 | Desk is auto-detected | P1 | Selected an employee with an assigned desk | Tap **Continue** | Step 2 shows the assigned desk (e.g. `R4-F6-04`) and "Building R · Floor 4 · Zone F" |
| TC-ONB-05 | Password mismatch blocked | P1 | On Step 3 | Enter two different passwords, tap **Finish** | Inline error "Passwords do not match"; account not created |
| TC-ONB-06 | Blank password blocked | P2 | On Step 3 | Leave password blank, tap **Finish** | Inline error "Enter a password." |
| TC-ONB-07 | Successful onboarding enters the app | P1 | On Step 3 | Enter matching passwords, tap **Finish** | App goes **straight to the main app** (Daily Pulse), no separate login prompt |

## 2. Auto-login & Sign out

| ID | Title | Pri | Preconditions | Steps | Expected result |
|----|-------|-----|---------------|-------|-----------------|
| TC-AUTH-01 | Session is remembered across relaunch | P1 | Onboarded / logged in | Fully close the app, relaunch it | App opens **directly to the main app** — no lock screen |
| TC-AUTH-02 | Sign out returns to onboarding | P1 | Signed in | Profile → **Sign out** | App returns to onboarding (Step 1) |
| TC-AUTH-03 | Sign out clears the remembered session | P1 | Just signed out | Fully close and relaunch the app | App shows onboarding, **not** the main app (no stale auto-login) |
| TC-AUTH-04 | Sign out wipes local identity | P2 | After sign out, re-onboard as a different employee | Complete onboarding | New identity is shown in Profile; previous user's data is gone |

## 3. Daily Pulse

| ID | Title | Pri | Preconditions | Steps | Expected result |
|----|-------|-----|---------------|-------|-----------------|
| TC-PULSE-01 | Skeleton shows while loading | P3 | Signed in, slow/again-loading network | Open the Pulse tab | Shimmer skeleton placeholders appear briefly, then real content |
| TC-PULSE-02 | Submit a mood check-in | P1 | Not yet checked in today | Pick a mood, optionally add a note, tap **Submit** | Check-in is saved; the card switches to a "checked in" confirmation |
| TC-PULSE-03 | Only one check-in per day | P1 | Already checked in today | Reopen the Pulse tab | The form is replaced by the checked-in state; cannot submit again |
| TC-PULSE-04 | Streak increments | P2 | Checked in today | Observe the streak under the title | Streak shows "🔥 N days" reflecting consecutive check-ins |
| TC-PULSE-05 | Weekly graph renders | P2 | Pulse data exists for the week | View "This week" card | Company / Floor / You averages + a rolling 7-day line graph (You=orange, Floor=cyan, Company=sky) with weekday labels |

## 4. Spaces & Kitchen

| ID | Title | Pri | Preconditions | Steps | Expected result |
|----|-------|-----|---------------|-------|-----------------|
| TC-KIT-01 | Open Kitchen detail | P2 | Signed in | Spaces tab → tap **Kitchen** | Kitchen detail with two shared fridges for the user's floor (e.g. "Shared live with floor R4") |
| TC-KIT-02 | Adjust fridge occupancy | P1 | On Kitchen detail | Tap **+10% / −10%** on a fridge | The percentage and bar update immediately and persist (synced) |
| TC-KIT-03 | Kitchen Pulse is dynamic | P2 | On Kitchen detail | Raise both fridges ≥ 80%, then lower below 50% | Top issue changes: ≥80% → "Fridge full"; ≥50% → "Limited fridge space"; else "No major issues right now 🎉" |
| TC-KIT-04 | Fridge "Report" pre-fills + shares | P1 | On Kitchen detail | Tap **Report** next to fridge occupancy | Feedback form opens pre-filled: "The fridges on floor X of the Y building are currently about Z% full.", category Kitchen, **Share with the community = ON** |
| TC-KIT-05 | Quick action pre-fills | P2 | On Kitchen detail | Tap a Quick action (Expired food / Fridge full / Dirty shelves / Unknown items) | Feedback form opens pre-filled with the matching message, category Kitchen, community share ON |
| TC-KIT-06 | Office freezer check-in/out | P2 | On Kitchen detail | Type an item, **Check in**; then **Check out** | Item appears in the freezer list with "Since today"; checking out removes it |

## 5. Report & Feedback

| ID | Title | Pri | Preconditions | Steps | Expected result |
|----|-------|-----|---------------|-------|-----------------|
| TC-FB-01 | Category grid → form | P1 | Report tab | Pick a category, tap **Continue** | Feedback form opens with that category preselected |
| TC-FB-02 | Quick chip pre-fills + shares | P2 | Report tab | Tap a quick chip (Too noisy / Too hot / No rooms) | Form opens with category + a starter message pre-filled and community share ON |
| TC-FB-03 | Validation: message required | P1 | On the form | Clear the message, tap **Submit feedback** | Inline error "Please describe your feedback"; nothing submitted |
| TC-FB-04 | Submit positive feedback | P1 | On the form | Select 👍 Positive, fill message, submit | "Thanks for the positive feedback! (no ticket needed)"; **no ticket** created |
| TC-FB-05 | Submit issue + ticket | P1 | On the form | Select ⚠️ Issue, enable **Create a ticket**, submit | Confirmation "Ticket JIRA-#### created"; ticket appears in My Tickets |
| TC-FB-06 | Attach photo from gallery | P1 | On the form | **Attach photo** → pick an image | Thumbnail appears; on-device AI may draft an issue/category (non-blocking) |
| TC-FB-07 | Take photo with camera | P2 | On the form, camera permission | **Take photo** → capture | Photo attaches; AI draft runs |
| TC-FB-08 | Anonymous submission | P1 | On the form | Enable **Submit anonymously**, share to community, submit | In Community the post shows **"Anonymous"** (no name) |
| TC-FB-09 | Instant return after submit | P1 | On the form | Submit any feedback | A confirmation toast appears and the form **returns to the previous screen immediately** (no delay) |
| TC-FB-10 | Positive feedback never tickets | P2 | On the form | Select Positive, enable Create a ticket, submit | Result indicates the ticket was suppressed; no ticket created |

## 6. Community newsfeed

| ID | Title | Pri | Preconditions | Steps | Expected result |
|----|-------|-----|---------------|-------|-----------------|
| TC-COM-01 | Skeleton while loading | P2 | Open Community on a cold/slow network | Open the Community tab | Shimmer card skeletons show, then real posts load |
| TC-COM-02 | Shared feedback appears | P1 | A community post was submitted | Open Community | The post is listed with category, sentiment, message, location and submitter name |
| TC-COM-03 | Name resolved locally | P1 | A non-anonymous post exists | View the post subtitle | Shows the submitter's real name (resolved on-device), never an email/ID |
| TC-COM-04 | Vote toggle | P1 | Signed in | Tap the heart/vote pill on a post | Vote count increments and the heart fills; tapping again removes the vote |
| TC-COM-05 | Building filter | P2 | Posts from multiple buildings | Tap **Building** → choose one | Only posts for that building remain; chip turns blue with a ✓ in the menu |
| TC-COM-06 | Floor disabled until building chosen | P1 | No building selected | Observe the **Floor** chip | Floor is **visibly greyed/faded and not tappable**; it enables only after a building is picked |
| TC-COM-07 | Floor filter | P2 | Building selected | Tap **Floor** → choose one | List narrows to that floor; **Clear** resets both filters |
| TC-COM-08 | Dropdown affordance | P3 | On Community | Observe the filter chips | Each chip shows a **▾ chevron** that rotates up when its menu opens |
| TC-COM-09 | Photo appears inline | P1 | A post with a photo exists | View that post | The attached photo renders inside the card |
| TC-COM-10 | Full-screen photo | P1 | A post with a photo exists | Tap the photo | It opens **full-screen** on a black backdrop; tapping again dismisses it |

## 7. Leaderboard & Gamification

| ID | Title | Pri | Preconditions | Steps | Expected result |
|----|-------|-----|---------------|-------|-----------------|
| TC-LB-01 | Skeleton while loading | P3 | Open Leaderboard on a slow network | Profile → **Leaderboard** | Shimmer row skeletons show, then the ranked list loads |
| TC-LB-02 | Ranking by public feedback | P1 | Several public posts exist | Open Leaderboard | Users ranked by public (non-anonymous) feedback count; top user marked **Office Champion 👑** |
| TC-LB-03 | Anonymous excluded | P2 | Anonymous posts exist | Open Leaderboard | Anonymous submissions do **not** add to anyone's count |
| TC-LB-04 | Current user pinned | P2 | Current user has/has no rank | Open Leaderboard | The current user's row is highlighted (or pinned at the bottom if outside the list) |
| TC-LB-05 | Rewards progress | P3 | On Profile | View Rewards | Tiers (First Steps / Office Regular / Office Champion) show locked/unlocked + progress |

## 8. Profile, Theme & Branding

| ID | Title | Pri | Preconditions | Steps | Expected result |
|----|-------|-----|---------------|-------|-----------------|
| TC-PRO-01 | Profile shows workplace info | P2 | Signed in | Open Profile | Name, "{Building} · Cluj", desk/zone/building/floor, supervisor, streak |
| TC-PRO-02 | Dark / Light / System toggle | P1 | On Profile | Appearance → toggle Light / Dark / System | The whole app re-themes accordingly and persists across relaunch |
| TC-PRO-03 | App icon & name | P2 | Home screen / launcher | View the installed app | Launcher shows the **CLOOJ** icon and the name "CLOOJ" |
| TC-PRO-04 | Garmin logo in header | P3 | Any tab | View the top header | The official Garmin wordmark shows (white on dark/branded surfaces, dark on light) |

## 9. My Tickets

| ID | Title | Pri | Preconditions | Steps | Expected result |
|----|-------|-----|---------------|-------|-----------------|
| TC-TKT-01 | Empty state | P3 | No tickets raised | Profile → My Tickets | "No tickets yet" empty state |
| TC-TKT-02 | Ticket tracked | P1 | Submitted an issue with a ticket | Open My Tickets | The ticket is listed with its category, id and status |

## 10. Privacy (verification)

| ID | Title | Pri | Preconditions | Steps | Expected result |
|----|-------|-----|---------------|-------|-----------------|
| TC-PRIV-01 | No names in the cloud | P1 | Access to the Firebase console (optional) | Inspect a `feedback/{id}` record | Contains only userId (Staff ID), category, sentiment, message, building/floor, photo, createdAt — **never** name/email/supervisor |
| TC-PRIV-02 | Anonymous carries no id | P1 | An anonymous post exists | Inspect its record | No `userId`; the feed shows "Anonymous" |

---

### Regression checklist (smoke test before release)
TC-ONB-07 · TC-AUTH-01 · TC-AUTH-03 · TC-PULSE-02 · TC-KIT-02 · TC-KIT-04 · TC-FB-05 · TC-FB-09 ·
TC-COM-04 · TC-COM-06 · TC-COM-10 · TC-PRO-02
