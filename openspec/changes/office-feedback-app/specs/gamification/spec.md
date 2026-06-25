## ADDED Requirements

### Requirement: Daily Pulse streak tracking
The system SHALL track the number of consecutive days a user has completed their Daily Pulse and SHALL reset the streak when a day is missed.

#### Scenario: Streak increments
- **WHEN** the user completes the Daily Pulse on a day immediately following their last check-in
- **THEN** the system increases the user's streak by one and displays the current streak

#### Scenario: Streak resets
- **WHEN** the user completes the Daily Pulse after missing one or more days
- **THEN** the system resets the streak to one

### Requirement: Leaderboard and ranking
The system SHALL provide a leaderboard that ranks users by the amount of public feedback they have given, and SHALL award the top-ranked user a recognition title such as "Office Champion".

#### Scenario: User views the leaderboard
- **WHEN** the user opens the leaderboard
- **THEN** the system shows users ranked by their public feedback count and highlights the current user's rank

#### Scenario: Top contributor is recognized
- **WHEN** a user holds the highest public feedback count on the leaderboard
- **THEN** the system displays the "Office Champion" title alongside that user

### Requirement: Public-only scoring
The system SHALL count only public (non-anonymous) feedback toward leaderboard ranking and points. Feedback submitted anonymously SHALL NOT earn the user any ranking credit.

#### Scenario: Public feedback earns ranking credit
- **WHEN** the user submits public (identified) feedback
- **THEN** the system attributes the points to the user and includes them in leaderboard ranking

#### Scenario: Anonymous feedback earns no ranking credit
- **WHEN** the user submits anonymous feedback
- **THEN** the system records the feedback but does not award the user ranking points or update their leaderboard standing for it

### Requirement: Rewards program
The system SHALL award points for qualifying actions (e.g. Daily Pulse check-ins, submitting public feedback) and SHALL surface rewards the user has earned or can earn.

#### Scenario: Points awarded for an action
- **WHEN** the user performs a qualifying action that is eligible for points
- **THEN** the system adds the corresponding points to the user's total

#### Scenario: Reward unlocked
- **WHEN** the user's points reach a reward threshold
- **THEN** the system marks the reward as unlocked and notifies the user
