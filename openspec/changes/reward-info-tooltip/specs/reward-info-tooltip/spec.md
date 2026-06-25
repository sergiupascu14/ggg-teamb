## ADDED Requirements

### Requirement: Reward card has info icon
Each reward card SHALL display a small info icon (ℹ️) that the user can tap.

#### Scenario: Info icon visible on card
- **WHEN** the reward card is displayed
- **THEN** an info icon is visible in the top-right area of the card

### Requirement: Tapping info shows pill with reward details
Tapping the info icon SHALL display a pill overlay with a text hint about the reward.

#### Scenario: Locked reward pill content
- **WHEN** the user taps the info icon on a locked reward with threshold 10
- **THEN** a pill appears showing "Submit 10 public feedbacks to unlock"

#### Scenario: Unlocked reward pill content
- **WHEN** the user taps the info icon on an unlocked reward with threshold 10
- **THEN** a pill appears showing "Earned! You reached 10 feedbacks"

### Requirement: Pill auto-dismisses after 1 second
The pill SHALL automatically disappear after 1 second without user interaction.

#### Scenario: Auto-dismiss timing
- **WHEN** the pill is shown
- **THEN** it disappears after 1 second automatically
