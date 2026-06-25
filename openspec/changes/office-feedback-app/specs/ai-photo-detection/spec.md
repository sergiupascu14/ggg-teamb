## ADDED Requirements

### Requirement: Detect issues from photos
The system SHALL analyze photos attached to feedback and automatically suggest a detected issue and category.

#### Scenario: Issue detected from photo
- **WHEN** the user attaches a photo to the feedback form
- **THEN** the system analyzes the image and suggests a category and short issue description the user can accept or override

#### Scenario: No confident detection
- **WHEN** the analysis cannot confidently detect an issue
- **THEN** the system leaves the category unset and lets the user fill it in manually

#### Scenario: Analysis unavailable
- **WHEN** the AI analysis service is unavailable
- **THEN** the system allows the user to submit feedback normally without suggestions and does not block submission
