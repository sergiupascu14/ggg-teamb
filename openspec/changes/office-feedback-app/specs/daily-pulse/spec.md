## ADDED Requirements

### Requirement: Daily Pulse prompt
The system SHALL present a "Daily Pulse" prompt that lets the user record a quick daily check-in about how the office feels.

#### Scenario: User completes the Daily Pulse
- **WHEN** the user opens the Daily Pulse prompt and submits a response
- **THEN** the system records the response with the current date and marks today as checked in

#### Scenario: Already completed today
- **WHEN** the user has already submitted a Daily Pulse for the current day
- **THEN** the system shows the completed state instead of prompting again

### Requirement: Daily push notification
The system SHALL deliver a daily push notification reminding the user to complete their Daily Pulse.

#### Scenario: Daily reminder fires
- **WHEN** the scheduled daily reminder time is reached and the user has not checked in
- **THEN** the system posts a notification that opens the Daily Pulse prompt when tapped

#### Scenario: Notification permission denied
- **WHEN** the user has not granted notification permission
- **THEN** the system requests permission and degrades gracefully without crashing if denied

#### Scenario: No reminder after check-in
- **WHEN** the user has already completed today's Daily Pulse before the reminder time
- **THEN** the system does not post the reminder notification for that day
