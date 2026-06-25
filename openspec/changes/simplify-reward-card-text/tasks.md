## 1. Model Simplification

- [x] 1.1 Remove `description` field from `Reward` data class in Models.kt
- [x] 1.2 Update `GamificationRepository.rewardsFor()` to remove description and change progress format to "x/y feedbacks"
- [x] 1.3 Remove `RewardTier.description` field

## 2. UI Update

- [x] 2.1 Remove the description `Text` from `RewardCard`, keep only icon + title + progress

## 3. Tests

- [x] 3.1 Update unit tests to match new progress format ("x/y feedbacks") and remove description assertions
