# UI Design Audit — CLOOJ App

> **RESOLVED (this iteration).** Every screen was rebuilt onto a new Garmin design system
> (`ui/theme` + `ui/components`) matching the provided mockups (`garmin-office-collage.svg`,
> `daily-pulse-variants.svg`, screenshots). Status by issue: #2 status-bar padding ✔, #3 dead-space
> reduced to mockup parity ✔, #4/#15/#16-casing ALL-CAPS → Title Case via `toDisplayName()` ✔,
> #5/#7/#8 all primary buttons now solid `GarminBlue` ✔, #6/#7 Sign In branded + centred ✔,
> #9 post-submit success card (badge + banner) ✔, #10 Feedback defaults to **Positive** ✔,
> #11 freezer dates human-readable ✔, #12 "In the freezer" section header ✔, #13 leaderboard
> "posts" label ✔, #14 current-user row pinned ✔, #16 My Tickets/Leaderboard now filled vs outlined ✔,
> #18 onboarding "Step X of 3" indicator ✔, #19 picker uses `weight(1f)` + higher-contrast disabled
> button ✔, #20/#21 My Tickets empty state has an icon ✔, #22 edge-to-edge insets via Scaffold ✔.
> Issues #1/#10-hamburger and #11 referenced a drawer/hamburger that does not exist in this build
> (bottom-nav only) — no action needed.

Audited via ADB screenshots on emulator (1280×2856 @ 480dpi). All issues are actionable; severity ranked **Critical → Major → Minor**.

---

## Summary of Issues

| # | Severity | Area | Issue |
|---|----------|------|-------|
| 1 | Critical | Global | Floating hamburger button overlaps content on 5+ screens |
| 2 | Critical | Onboarding | Status bar overlap — title drawn behind clock/icons |
| 3 | Major | Global | Vast empty space on nearly every screen (content fills top 30–50%) |
| 4 | Major | Global | ALL CAPS names look shouted and outdated |
| 5 | Major | Global | Submit/action button color inconsistency (washed-out vs solid) |
| 6 | Major | Sign In | No branding — bare "Sign in" with no logo, massive dead space |
| 7 | Major | Sign In | Content block is below-center, not vertically balanced |
| 8 | Major | Daily Pulse | Selected mood Submit button color looks disabled |
| 9 | Major | Daily Pulse | Post-submit success state is just two lines of text — very sparse |
| 10 | Major | Feedback | "Issue" chip appears selected by default instead of "Positive" |
| 11 | Minor | Freezer | Hamburger button clips "F" off "Freezer" title |
| 12 | Minor | Freezer | Date shown as ISO format (`2026-06-25`) instead of human-readable |
| 13 | Minor | Freezer | No section header separating the input form from the items list |
| 14 | Minor | Leaderboard | Score column has no label — "1" is meaningless without context |
| 15 | Minor | Leaderboard | Logged-in user not highlighted when not in top 3 |
| 16 | Minor | Community | Names alternate between ALL CAPS and sentence case ("Anonymous") |
| 17 | Minor | Profile | Both "My Tickets" and "Leaderboard" buttons look identical |
| 18 | Minor | Profile | Scrollability not hinted — My Tickets/Leaderboard hidden below fold |
| 19 | Minor | Onboarding | No step progress indicator (step 1 of 3, etc.) |
| 20 | Minor | Onboarding | Last list item clipped; "Continue" disabled state has very low contrast |
| 21 | Minor | My Tickets | Empty state has no icon/illustration — just raw text |
| 22 | Minor | Global | Bottom nav gesture bar area not padded — home indicator overlaps |

---

## Issue Details

---

### 1. 🔴 CRITICAL — Floating hamburger button overlaps content

**Screens affected:** Login, Onboarding (all steps), Freezer (when text field is focused).

A circular floating `≡` button appears in the top-left corner of these screens, overlapping the page title and in one case clipping the "F" off "Freezer" completely. It appears to be a `DrawerButton` or FAB tied to a `ModalNavigationDrawer` state that is rendered at the wrong z-level or shown on screens where it should be hidden.

**What to fix:**
- Move the drawer trigger into a proper `TopAppBar` `navigationIcon` slot so it is bounded by the top bar's layout.
- Suppress it on screens where there is no drawer (Login, Onboarding).
- The floating circle with drop shadow on a plain background looks like a debug artifact, not an intentional control.

---

### 2. 🔴 CRITICAL — Status bar overlap on all Onboarding screens

**Screens affected:** Onboarding steps 1, 2, 3 (and the Sign In screen when it first loads before the profile is set).

The title "Welcome to CLOOJ" begins at `y=72` in pixel coords (confirmed via UIAutomator dump), which is exactly where the system status bar clock sits. The time "2:28" visibly collides with the "W" of "Welcome".

**Root cause:** The onboarding `Column`/`Box` does not apply `Modifier.statusBarsPadding()` or consume `WindowInsets.statusBars`. The main `Scaffold` in `MainScaffold` handles insets for the logged-in flow but the onboarding composable has its own layout without insets.

**What to fix:**
```kotlin
// OnboardingScreen.kt — top-level composable
Column(
    modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()  // add this
        .padding(horizontal = 24.dp)
)
```

---

### 3. 🟠 MAJOR — Vast dead space on almost every screen

**Screens affected:** Sign In, Onboarding steps 1–3, Daily Pulse (all states), My Tickets, Leaderboard, Freezer (empty state).

Every screen has a content block that occupies the top 30–50% of the visible area and leaves the bottom half completely blank. On a tall phone this reads as an unfinished layout.

**Per-screen fixes:**

- **Sign In / Login:** Use `Arrangement.Center` on the column so the form sits vertically centred. Add the app logo/wordmark above the form to fill the top half purposefully.
- **Onboarding desk confirmation & password steps:** Wrap in a `Box(contentAlignment = Alignment.Center)` or add a descriptive illustration/icon above the form (e.g., a desk icon on the desk step, a lock icon on the password step).
- **Daily Pulse:** After submission, the success state ("You've checked in today. See you tomorrow!") could show a mood history strip, streak calendar, or a CTA to submit feedback — anything to fill the screen.
- **My Tickets / Leaderboard (empty):** Add an illustration or a "No items yet" state with a clear action button.
- **Freezer:** The ice-cube emoji + text empty state is charming but drops in the dead centre of a huge blank area. Pin the empty state to centre of the _list area only_ (below the form), not the whole screen.

---

### 4. 🟠 MAJOR — ALL CAPS names

**Screens affected:** Onboarding user picker, Community feed, Leaderboard, Profile header, Profile info card (Supervisor field).

Names from the dataset are stored in ALL CAPS (`AARON CRAWFORD`, `ALEXANDER BAUTISTA`) and displayed verbatim. This looks aggressive and dated.

**What to fix:** Apply a title-case transformation at the display layer — not at the data layer (the data is canonical).

```kotlin
fun String.toDisplayName(): String =
    split(" ").joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercaseChar) }
```

Note: "Anonymous" in the Community feed is already sentence case, making the inconsistency visible side by side.

---

### 5. 🟠 MAJOR — Action button colour inconsistency

Three button variants exist and only one looks intentional:

| Button | Colour | Looks |
|--------|--------|-------|
| Login "Unlock" | Dark blue-grey (solid) | ✅ Confident |
| Freezer "Check in" | Same dark blue (solid) | ✅ Confident |
| Daily Pulse "Submit" (enabled) | Pale washed-out blue | ❌ Looks disabled |
| Feedback "Submit" (enabled) | Same pale washed-out | ❌ Looks disabled |

The Daily Pulse and Feedback Submit buttons use a colour that is indistinguishable from the _disabled_ grey on a quick glance. Both use what appears to be `colorScheme.primaryContainer` or a heavily tinted surface instead of `colorScheme.primary`.

**What to fix:** Use the same `ButtonDefaults.buttonColors()` (defaults to `primary`) for all primary action buttons. The disabled state is already handled by the `enabled` param — you do not need to manually soften the colour.

---

### 6. 🟠 MAJOR — Sign In screen has no branding and is not vertically centred

The login screen shows "Sign in" text sitting at roughly 55% of the screen height with empty space both above and below. There is no app name, no logo, no Garmin branding.

**What to fix:**
- Vertically centre the form block.
- Add the app name / wordmark above the form in the top half of the screen.
- Even a simple large icon + "CLOOJ" headline above the form would anchor the layout.

---

### 7. 🟠 MAJOR — Daily Pulse: selected-mood Submit button looks passive

When the user selects a mood emoji, the Submit button transitions from grey to a very pale lavender-blue. It does not look "active" — users may think it is still disabled. See issue #5 for the colour fix; this is the most impactful place it manifests because submission is the primary CTA on this screen.

Additionally, the selected emoji circle gets a dark fill which is clear, but no scale animation or haptic feedback reinforces the tap. A subtle `animateFloatAsState` scale pulse on selection would polish it significantly.

---

### 8. 🟠 MAJOR — Post-submit Daily Pulse state is too sparse

After submitting, the screen shows:

```
[streak counter]
✅
You've checked in today. See you tomorrow!
```

…and then 70% blank screen. The iOS-style ✅ emoji also reads out of place on Android.

**What to fix:**
- Replace the ✅ emoji with a `CircularProgressIndicator` → `AnimatedVisibility` checkmark using Material Icons (`Icons.Default.CheckCircle` in green).
- Add contextual content below: a horizontal mood history for the last 5 days, or a nudge ("While you're here — seen anything worth reporting?") with a shortcut to the Feedback tab.

---

### 9. 🟠 MAJOR — Feedback: "Issue" chip appears selected by default

On opening the Feedback tab, "⚠️ Issue" has the dark filled background (selected state) and "👍 Positive" has the outline border (unselected state). For an office feedback app you want to encourage positive feedback first — "Positive" should be the default selected tab.

If the visual logic is actually inverted (outline = selected, filled = not selected), that is equally wrong because it contradicts Material3 chip conventions.

---

### 10. 🟡 MINOR — Freezer: hamburger clips the page title

When the Freezer text field is focused, the floating `≡` button (same as issue #1) overlaps the title, hiding the "F" of "Freezer". This makes the title read as "eezer". See fix in issue #1.

---

### 11. 🟡 MINOR — Freezer: ISO date format

Checked-in items show `Since 2026-06-25`. For an internal office app this should be human-readable: `Since Jun 25` or `Since today` (when it was checked in the same calendar day).

```kotlin
val label = if (item.since == LocalDate.now()) "Since today"
            else "Since ${item.since.format(DateTimeFormatter.ofPattern("MMM d"))}"
```

---

### 12. 🟡 MINOR — Freezer: no visual section divider between form and items list

After a check-in, the item card (`Lunchbox / Since…`) appears directly below the form with no separator or heading. Adding a `Text("In the freezer", style = MaterialTheme.typography.labelMedium)` divider above the list makes the screen structure clearer.

---

### 13. 🟡 MINOR — Leaderboard: score column has no label

Each row shows a number (`1`) flush-right with no column header or unit label. It's unclear if this is "posts this week", "total posts", or "points". A small `Text("posts", style = labelSmall)` alongside the number, or a column header, would remove the ambiguity.

---

### 14. 🟡 MINOR — Leaderboard: logged-in user not shown or highlighted

If the current user is not in the top 3, they have no way to see their own rank. Standard leaderboard pattern: show top N rows, then a divider, then the current user's row pinned at the bottom ("You — #47 — 3 posts").

---

### 15. 🟡 MINOR — Community feed: name casing inconsistency

`MARIA WARREN`, `FRANK CARLSON` (ALL CAPS) vs `Anonymous` (sentence case). These appear side-by-side in the same list. Fix with the `toDisplayName()` transformation from issue #4; `Anonymous` stays as-is.

---

### 16. 🟡 MINOR — Profile: "My Tickets" and "Leaderboard" buttons look identical

Both are dark blue filled pills of the same size and weight. There is no visual distinction between a "navigate to my data" action (My Tickets) and a "navigate to community ranking" action (Leaderboard). Consider making one outlined:

```
[My Tickets]       ← filled primary (personal action)
○ Leaderboard ○   ← outlined secondary (browse action)
```

---

### 17. 🟡 MINOR — Profile: scroll affordance hidden

The screen appears complete when loaded (shows AARON CRAWFORD, desk info, streak, rewards), but My Tickets and Leaderboard buttons are 200+ px below the fold. Nothing hints that the screen is scrollable. Fix: reduce vertical spacing between reward cards, or add a subtle fade/shadow at the bottom of the visible area.

---

### 18. 🟡 MINOR — Onboarding: no step progress indicator

Three steps (who are you → your desk → create password) with no "Step 2 of 3" indicator or dot pager. First-time users don't know how much is left.

---

### 19. 🟡 MINOR — Onboarding: list clips last item and disabled Continue has low contrast

In the user picker, "ADAM TANNER" is partially clipped at the bottom of the list (staff ID cut off). The list should have a defined `height` or `weight(1f)` in the column so it fills the available space rather than ending at a fixed number of items.

The disabled "Continue" button (before a user is selected) is near-invisible — very light grey text on light grey background. While `enabled = false` handles the semantics, the visual contrast could be raised slightly without misleading the user.

---

### 20. 🟡 MINOR — My Tickets empty state

The empty state is plain text only:

> "No tickets yet. Submit issue feedback with 'Create a ticket' to raise one."

No icon, no illustration. Add `Icons.Default.ConfirmationNumber` or similar above the text, tinted in `colorScheme.secondary`, and make "Create a ticket" a tappable hyperlink or button that navigates directly to Feedback.

---

### 21. 🟡 MINOR — Bottom navigation bar / gesture area padding

The bottom nav bar extends to `y=2784` and the system home indicator sits at `y=2784–2856`. On this emulator the two coexist without overlap because gesture navigation is configured correctly, but the `NavigationBar` background does not extend behind the gesture bar, leaving a thin unstyled strip at the very bottom. Use `Modifier.navigationBarsPadding()` on the `Scaffold` content or ensure `WindowCompat.setDecorFitsSystemWindows(window, false)` is set with proper inset handling so the nav bar background draws edge-to-edge.

---

## Quick-win priority list

If you want to tackle these in order of visual impact per effort:

1. **Fix status bar padding on Onboarding** (one `statusBarsPadding()` modifier)
2. **Fix floating hamburger button z-ordering / hide on wrong screens**
3. **Fix primary button colour on Daily Pulse and Feedback Submit**
4. **Apply `toDisplayName()` to all name display sites**
5. **Add branding + centre the Sign In form**
6. **Switch default Feedback tab to Positive**
7. **Add context/content to the Daily Pulse post-submit screen**
8. **Fix Freezer date formatting**
9. **Remaining minor polish (Leaderboard label, Profile button distinction, scroll hint)**
