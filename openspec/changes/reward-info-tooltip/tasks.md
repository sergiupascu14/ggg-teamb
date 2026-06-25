## 1. Model Update

- [x] 1.1 Add `hint: String` field to `Reward` data class
- [x] 1.2 Compute hint text in `GamificationRepository.rewardsFor()` — locked: "Submit X public feedbacks to unlock", unlocked: "Earned! You reached X feedbacks"

## 2. UI Implementation

- [x] 2.1 Add info `IconButton` to `RewardCard` composable (top-end corner)
- [x] 2.2 Add pill Popup that shows on tap with `reward.hint` text, auto-dismisses after 1 second via `LaunchedEffect`

## 3. Tests

- [x] 3.1 Add unit tests verifying hint text generation for locked and unlocked rewards
