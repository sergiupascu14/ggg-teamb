## Why

Users need a way to learn more about each reward (what it is, how to unlock it) without cluttering the card permanently. An info icon with an auto-dismissing pill tooltip provides contextual help on demand.

## What Changes

- Add a small ℹ️ info icon on each `RewardCard`
- Tapping the icon shows a pill/tooltip with reward details (e.g., "Submit 10 public feedbacks to unlock")
- The pill auto-dismisses after 1 second
- Works for both locked and unlocked rewards

## Capabilities

### New Capabilities
- `reward-info-tooltip`: Info icon on reward cards that shows an auto-dismissing pill with reward details

### Modified Capabilities

## Impact

- `RewardCard` composable gains an info icon and tooltip state
- `Reward` model gains a `hint: String` field with the info text
- No backend/database changes — purely UI + computed string
