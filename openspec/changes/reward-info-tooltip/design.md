## Context

Reward cards show icon + title + "x/y feedbacks". The user wants an info icon that, when tapped, shows a pill with more context about the reward. The pill auto-dismisses after 1 second.

## Goals / Non-Goals

**Goals:**
- Add an ℹ️ icon button to each `RewardCard`
- Show a pill (small rounded chip/snackbar-like overlay) with reward info on tap
- Auto-dismiss the pill after 1 second

**Non-Goals:**
- Persistent tooltip or bottom sheet
- Animation beyond simple fade in/out

## Decisions

1. **Use a `Popup` composable** for the pill — lightweight overlay that doesn't affect layout, positioned above/below the card. Dismissed via `LaunchedEffect` with 1-second delay.

2. **Add `hint: String` to `Reward` model** — computed in `rewardsFor()`. Locked: "Submit X public feedbacks to unlock". Unlocked: "Earned! You reached X feedbacks".

3. **Info icon placement** — small `IconButton` with `Icons.Outlined.Info` at the top-end corner of the card, overlaid.

4. **Pill styling** — `Surface` with `RoundedCornerShape(20.dp)`, dark background, white text, `labelSmall` typography. Appears centered above the card.

## Risks / Trade-offs

- [Risk] 1 second may feel too fast → easy to adjust later
- [Risk] Popup may clip on screen edges → use `Alignment.TopCenter` with offset
