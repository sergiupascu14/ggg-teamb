## Context

Reward cards currently display 4 lines: icon, title, description ("10 public feedbacks"), and progress ("7/10"). Too verbose for a small card. User wants just: name + "x/y feedbacks".

## Goals / Non-Goals

**Goals:**
- Reduce reward card to: icon + title + "x/y feedbacks" (or "✓ Unlocked")

**Non-Goals:**
- Changing reward thresholds or logic

## Decisions

1. **Remove `description` field from `Reward`** — the "x/y feedbacks" line already communicates what's needed.
2. **Change `progress` format** — locked: "7/10 feedbacks", unlocked: "✓ Unlocked".
3. **Remove one `Text` composable** from `RewardCard` — only icon, title, progress remain.
