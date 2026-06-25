## Why

The reward cards currently show too much text (title + description + progress). It clutters the UI. Simplify to just show the reward name and a compact "x/y feedbacks" line.

## What Changes

- Remove the separate `description` line from `RewardCard`
- Change progress format from "7/10" to "7/10 feedbacks" for locked rewards
- Keep "✓ Unlocked" for unlocked rewards

## Capabilities

### New Capabilities

### Modified Capabilities
- `reward-unlock-info`: Simplify display to title + "x/y feedbacks" only

## Impact

- `RewardCard` composable simplified (remove one Text element)
- `Reward.description` field removed (no longer needed)
- `Reward.progress` format changed to include "feedbacks" suffix
