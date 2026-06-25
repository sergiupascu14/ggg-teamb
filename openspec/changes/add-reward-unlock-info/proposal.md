## Why

Users can see their rewards (First Steps, Office Regular, Office Champion) but have no visibility into what's needed to unlock each one. Adding unlock criteria info directly on each reward card helps motivate users by showing clear progress targets.

## What Changes

- Add a description/hint to each reward showing the unlock condition (e.g., "10 public feedbacks")
- Display this info on the `RewardCard` so users always know what's required
- Show progress toward the next locked reward (e.g., "7/10")

## Capabilities

### New Capabilities
- `reward-unlock-info`: Display unlock criteria and progress on each reward card in the profile screen

### Modified Capabilities
<!-- No existing specs to modify -->

## Impact

- `Reward` data class gains a `description` field
- `GamificationRepository.rewardsFor()` passes current points for progress calculation
- `RewardCard` composable updated to show unlock info text
- No breaking changes; purely additive UI enhancement
