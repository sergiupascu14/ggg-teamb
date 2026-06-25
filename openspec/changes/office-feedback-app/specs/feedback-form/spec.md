## ADDED Requirements

### Requirement: Positive and issue feedback
The system SHALL support both positive (appreciation) feedback and issue feedback as first-class submission types, letting users spread love for things they enjoy in the office as well as raise problems. The user SHALL choose the feedback sentiment when submitting.

#### Scenario: User submits positive feedback
- **WHEN** the user selects the positive/appreciation sentiment, describes what they enjoy, and submits
- **THEN** the system records the feedback as positive and confirms it without prompting to create a ticket

#### Scenario: User submits an issue
- **WHEN** the user selects the issue sentiment, describes the problem, and submits
- **THEN** the system records the feedback as an issue and offers the ticketing options

### Requirement: Submit feedback with category
The system SHALL provide a feedback form with a category dropdown (e.g. Elevators, Kitchen, Desk Area) and SHALL require a category for every submission.

#### Scenario: User submits categorized feedback
- **WHEN** the user selects a category, enters a description, and submits
- **THEN** the system records the feedback with its category and submission time

#### Scenario: Category missing
- **WHEN** the user attempts to submit without selecting a category
- **THEN** the system blocks submission and shows a validation message

### Requirement: Photo upload
The system SHALL allow the user to attach one or more photos (from camera or gallery) to a feedback submission.

#### Scenario: User attaches a photo
- **WHEN** the user picks or captures a photo on the feedback form
- **THEN** the system attaches the image to the submission and shows a thumbnail preview

### Requirement: Anonymous or identified submission
The system SHALL provide a toggle that lets the user submit feedback anonymously or with their identity attached.

#### Scenario: Anonymous submission
- **WHEN** the user enables the anonymous toggle and submits
- **THEN** the system records the feedback without the user's identity

#### Scenario: Identified submission
- **WHEN** the anonymous toggle is off and the user submits
- **THEN** the system attaches the user's identity to the feedback

### Requirement: Optional issue location
The system SHALL provide an optional input field for the specific location of the issue.

#### Scenario: User specifies a location
- **WHEN** the user fills in the optional location field and submits
- **THEN** the system stores the location string with the feedback

#### Scenario: Location omitted
- **WHEN** the user leaves the location field empty and submits
- **THEN** the system accepts the submission without a location

### Requirement: Community visibility setting
The system SHALL provide a setting that controls whether a feedback submission is visible to other users in the community newsfeed.

#### Scenario: User makes feedback community-visible
- **WHEN** the user enables the community-visibility setting and submits
- **THEN** the system marks the feedback as eligible for the community newsfeed

#### Scenario: Private by default
- **WHEN** the user leaves community visibility off
- **THEN** the system keeps the feedback out of the community newsfeed
