## 1. Model Update

- [x] 1.1 Add `description: String` and `progress: String` fields to the `Reward` data class in Models.kt

## 2. Repository Logic

- [x] 2.1 Update `GamificationRepository.rewardsFor()` to populate `description` (e.g., "10 public feedbacks") and `progress` (e.g., "7/10" or "✓ Unlocked") for each reward
- [x] 2.2 Define descriptions alongside existing REWARD_TIERS companion data

## 3. UI Update

- [x] 3.1 Update `RewardCard` composable to display the description and progress text below the title

## 4. Tests

- [x] 4.1 Add/update unit tests in GamificationRepositoryTest to verify description and progress fields are correctly computed
