## ADDED Requirements

### Requirement: Reward displays unlock criteria
Each reward card SHALL display a short description of the unlock condition (e.g., "10 public feedbacks") regardless of whether the reward is locked or unlocked.

#### Scenario: Locked reward shows criteria
- **WHEN** a reward is locked (user points below threshold)
- **THEN** the reward card displays the unlock description text below the title

#### Scenario: Unlocked reward shows criteria
- **WHEN** a reward is unlocked (user points at or above threshold)
- **THEN** the reward card still displays the unlock description text below the title

### Requirement: Locked reward displays progress
Each locked reward card SHALL display the user's current progress toward unlocking (format: "currentPoints/threshold").

#### Scenario: Progress shown on locked reward
- **WHEN** a user has 7 public feedbacks and the reward threshold is 10
- **THEN** the reward card displays "7/10" as progress

#### Scenario: No progress on unlocked reward
- **WHEN** a reward is already unlocked
- **THEN** the reward card displays "✓ Unlocked" instead of a progress fraction

### Requirement: Reward model includes description and progress
The `Reward` data class SHALL include a `description: String` field with the human-readable unlock condition and a `progress: String` field with either the fraction or "✓ Unlocked".

#### Scenario: rewardsFor returns enriched rewards
- **WHEN** `rewardsFor(points=7)` is called
- **THEN** it returns rewards with `description` populated (e.g., "10 public feedbacks") and `progress` set to "7/10" for locked rewards or "✓ Unlocked" for unlocked rewards
