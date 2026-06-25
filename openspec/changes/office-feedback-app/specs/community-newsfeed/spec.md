## ADDED Requirements

### Requirement: Firebase-backed shared newsfeed
The system SHALL store community-visible feedback and its votes in Firebase Realtime Database so the newsfeed is shared across devices and updates in real time.

#### Scenario: Community-visible feedback is published
- **WHEN** a user submits feedback marked community-visible
- **THEN** the system writes the feedback to Firebase Realtime Database where other devices can read it

#### Scenario: Newsfeed updates in real time
- **WHEN** another user publishes or votes on community-visible feedback while the newsfeed is open
- **THEN** the system reflects the change without requiring a manual refresh

### Requirement: Privacy — link feedback by userId only
The system SHALL link feedback stored in Firebase only by an opaque `userId` (the Staff ID) and content fields, and SHALL NOT store any user identity (name, supervisor, email) in Firebase. The list of users SHALL be stored only locally on each device. Display names SHALL be resolved locally from the bundled desk allocation dataset.

#### Scenario: No identity stored in the cloud
- **WHEN** identified feedback is published to Firebase
- **THEN** the stored record contains the `userId` and content but no name, supervisor, or email

#### Scenario: Name resolved locally
- **WHEN** the newsfeed displays an identified item
- **THEN** the system resolves the submitter's display name locally from the `userId` via the bundled desk allocation dataset

#### Scenario: Anonymous item carries no userId
- **WHEN** anonymous feedback is published to Firebase
- **THEN** the stored record carries no `userId` and the newsfeed shows it as "Anonymous"

### Requirement: Community newsfeed
The system SHALL display a newsfeed of feedback that other users have marked as community-visible.

#### Scenario: User browses the newsfeed
- **WHEN** the user opens the newsfeed
- **THEN** the system shows community-visible feedback items with category, location, photo (if any), and the locally-resolved submitter name or "Anonymous"

#### Scenario: Private feedback excluded
- **WHEN** feedback is not marked community-visible
- **THEN** the system does not publish it to Firebase and does not show it in the newsfeed

### Requirement: Voting on feedback
The system SHALL let users vote on newsfeed items and SHALL reflect each item's vote count.

#### Scenario: User votes on an item
- **WHEN** the user votes on a newsfeed item
- **THEN** the system increments that item's vote count and marks it as voted by the user

#### Scenario: User removes a vote
- **WHEN** the user votes again on an item they already voted on
- **THEN** the system removes the user's vote and decrements the count

### Requirement: Location filtering
The system SHALL allow filtering the newsfeed by location, including by building and floor. The available building and floor options SHALL come from the canonical desk allocation enumeration (Tower floors 3–6, Riviera floors 3–5).

#### Scenario: Filter by building and floor
- **WHEN** the user selects a building and floor from the canonical options
- **THEN** the system shows only newsfeed items matching the selected location

#### Scenario: Clear filters
- **WHEN** the user clears the location filters
- **THEN** the system shows all community-visible items again
