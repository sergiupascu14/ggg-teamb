## MODIFIED Requirements

### Requirement: Reward model includes description and progress
The `Reward` data class SHALL include a `progress: String` field with either "x/y feedbacks" or "✓ Unlocked". The `description` field is removed.

#### Scenario: rewardsFor returns simplified rewards
- **WHEN** `rewardsFor(points=7)` is called
- **THEN** it returns rewards with `progress` set to "7/10 feedbacks" for locked rewards or "✓ Unlocked" for unlocked rewards

### Requirement: Reward card displays compact info
Each reward card SHALL display only the icon, title, and progress line ("x/y feedbacks" or "✓ Unlocked").

#### Scenario: Locked reward shows compact progress
- **WHEN** a reward is locked with user at 7 points and threshold 10
- **THEN** the card shows: 🔒, title, "7/10 feedbacks"

#### Scenario: Unlocked reward shows compact status
- **WHEN** a reward is unlocked
- **THEN** the card shows: 🏅, title, "✓ Unlocked"
