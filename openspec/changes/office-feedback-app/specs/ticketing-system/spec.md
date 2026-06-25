## ADDED Requirements

### Requirement: Opt-in ticket creation
The system SHALL include a checkbox on the feedback form letting the user choose whether to create a ticket from their feedback.

#### Scenario: User opts to create a ticket
- **WHEN** the user checks "create a ticket" and submits actionable feedback
- **THEN** the system creates a ticket linked to that feedback

#### Scenario: User opts out
- **WHEN** the user leaves the ticket checkbox unchecked
- **THEN** the system records the feedback without creating a ticket

### Requirement: No ticket for positive feedback
The system SHALL NOT generate a ticket for feedback classified as positive, even when ticket creation is requested.

#### Scenario: Positive feedback suppresses ticket
- **WHEN** the user submits positive feedback with the ticket checkbox checked
- **THEN** the system records the feedback but does not create a ticket and informs the user

### Requirement: Route actionable feedback
The system SHALL route actionable feedback to facilities by composing an email to #CLU-Facilities or generating a mock Jira ticket.

#### Scenario: Email route
- **WHEN** a ticket is created and the email route is configured
- **THEN** the system composes an email to #CLU-Facilities containing the feedback details, category, location, and any photos

#### Scenario: Jira route
- **WHEN** a ticket is created and the Jira route is configured
- **THEN** the system generates a mock Jira ticket with an identifier and stores its reference on the feedback

### Requirement: My Tickets tracker
The system SHALL provide a "My Tickets" dashboard where the user can view the status of tickets created from their feedback.

#### Scenario: User views ticket statuses
- **WHEN** the user opens the My Tickets dashboard
- **THEN** the system lists the user's tickets with their category, creation date, and current status

#### Scenario: Empty tracker
- **WHEN** the user has not created any tickets
- **THEN** the system shows an empty state inviting the user to submit feedback
