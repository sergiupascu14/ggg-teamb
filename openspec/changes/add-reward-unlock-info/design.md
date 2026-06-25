## Context

The profile screen shows up to 3 reward cards (First Steps, Office Regular, Office Champion) with a lock/medal icon and title. Currently users cannot see what's needed to unlock a reward or how close they are. The `Reward` model holds `id`, `title`, `threshold`, `unlocked`.

## Goals / Non-Goals

**Goals:**
- Show the unlock condition on every reward card (locked and unlocked)
- Show progress fraction for locked rewards (e.g., "7/10")

**Non-Goals:**
- Animated progress bars or fancy UI effects
- Push notifications when close to unlocking
- Changing the reward tier thresholds

## Decisions

1. **Add `description` and `progress` to `Reward` model** — a `description: String` field (e.g., "10 public feedbacks") plus `progress: String?` (null when unlocked, e.g., "7/10" when locked). Computed in `rewardsFor()` since it already receives `points`. Simple and no new state needed.

2. **Display below the title in `RewardCard`** — add a small muted-text line under the reward title. For unlocked rewards show "✓ Unlocked", for locked show the progress string. The description (unlock criteria) always shows.

3. **No data model or DB changes** — rewards are computed on-the-fly from public feedback count; this only adds derived display strings.

## Risks / Trade-offs

- [Risk] Card height may vary with longer descriptions → Mitigation: Keep descriptions very short (< 20 chars) and use `labelSmall` typography.
- [Risk] Tight card width on small screens → Mitigation: Already using `weight(1f)` distribution; text will wrap naturally.
